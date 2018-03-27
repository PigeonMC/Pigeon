package io.jadon.pigeon

import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.objectweb.asm.*
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.ZipOutputStream

object JarManager {

    val DOWNLOAD_URL = URL("http://s3.amazonaws.com/Minecraft.Download/versions/b1.7.3/b1.7.3.jar")

    fun downloadVanillaClient(destinationFile: String) {
        val path = Paths.get(destinationFile)
        path.parent.toFile().mkdirs()
        if (!path.toFile().isFile) {
            DOWNLOAD_URL.openStream().use { Files.copy(it, path) }
        }
    }

    fun mergeJars(client: File, server: File, merged: File, serverToClientMappings: Map<String, String>, serverOnlyClasses: List<String>) {
        val clientJar = JarFile(client)
        val serverJar = JarFile(server)
        val remapper = SimpleRemapper(serverToClientMappings)
        val mappedServerOnlyClasses = serverOnlyClasses.map { serverToClientMappings[it] }
        val serverClassesToMove = mutableListOf<Pair<String, ByteArray>>()

        /**
         * Transform the access for all protected/package-private entities to public.
         */
        fun checkAccess(classNode: ClassNode) {
            val acc = classNode.access
            classNode.access = acc or Opcodes.ACC_PUBLIC
            classNode.methods.filter { it.name == "<init>" }.forEach {
                if (it.access and Opcodes.ACC_PUBLIC != Opcodes.ACC_PUBLIC && it.access and Opcodes.ACC_PRIVATE != Opcodes.ACC_PRIVATE) {
                    it.access = (it.access and 0xFFFF8) or Opcodes.ACC_PUBLIC
                }
            }
            classNode.fields.forEach {
                if (it.access and Opcodes.ACC_PUBLIC != Opcodes.ACC_PUBLIC && it.access and Opcodes.ACC_PRIVATE != Opcodes.ACC_PRIVATE) {
                    it.access = (it.access and 0xFFFF8) or Opcodes.ACC_PUBLIC
                }
            }
        }

        val entries = serverJar.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.name.endsWith(".class")
                    && (serverToClientMappings.contains(entry.name.substring(0, entry.name.length - 6))
                    || entry.name == "net/minecraft/server/MinecraftServer.class")) {
                // Read class from jar
                val inputStream = serverJar.getInputStream(entry)
                val classNode = ClassNode()
                val classReader = ClassReader(inputStream)
                classReader.accept(classNode, 0)
                // Remap the classes
                val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
                val classRemapper = ClassRemapper(classWriter, remapper)
                classNode.accept(classRemapper)

                // Add the class to the list to move
                if (classNode.name == "net/minecraft/server/MinecraftServer") {
                    serverClassesToMove.add("net/minecraft/server/MinecraftServer.class" to classWriter.toByteArray())
                } else {
                    val mappedName = serverToClientMappings[classNode.name]
                    if (mappedServerOnlyClasses.contains(mappedName)) {
                        serverClassesToMove.add("$mappedName.class" to classWriter.toByteArray())
                    }
                }
            }
        }

        //  Get client classes
        val mergedFiles = clientJar.entries().toList().filter { !it.name.contains("META-INF") }.map {
            val inputStream = clientJar.getInputStream(it)
            it.name to inputStream.readBytes()
        }.toMutableList()

        clientJar.close()

        // Add remapped server classes
        mergedFiles.addAll(serverClassesToMove)

        val mergedClassNodes = mergedFiles.mapNotNull {
            if (it.first.endsWith(".class")) {
                val classNode = ClassNode()
                val classReader = ClassReader(it.second)
                classReader.accept(classNode, 0)
                it.first to classNode
            } else null
        }

        val mergedClassBytes = mergedClassNodes.map { (name, classNode) ->
            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
            // Make all the classes public
            checkAccess(classNode)
            // Transform constructor calls
            val constructorRemapper = ConstructorRemapper(mergedClassNodes.map { it.second }, classNode.name, serverToClientMappings, classWriter)
            classNode.accept(constructorRemapper)
            name to classWriter.toByteArray()
        }.toMap()

        // Modify classes to play nicely together
        val mergedClasses = mergedFiles.map {
            if (it.first.endsWith(".class"))
                it.first to mergedClassBytes[it.first]
            else it
        }

        val mergedJarStream = ZipOutputStream(FileOutputStream(merged))
        val output = BufferedOutputStream(mergedJarStream)

        mergedClasses.forEach {
            mergedJarStream.putNextEntry(JarEntry(it.first))
            output.write(it.second)
            output.flush()
            mergedJarStream.closeEntry()
        }
        output.close()
        mergedJarStream.close()
    }

    class ConstructorRemapper(val classNodes: List<ClassNode>, val className: String, val serverToClientMappings: Map<String, String>,
                              cv: ClassVisitor) : ClassVisitor(Opcodes.ASM6, cv) {

        override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
            return ConstructorCallTransformer(
                    classNodes, className, serverToClientMappings,
                    super.visitMethod(access, name, desc, signature, exceptions),
                    access, name, desc, signature, exceptions)
        }

    }

    class ConstructorCallTransformer(val classNodes: List<ClassNode>, val className: String, val serverToClientMappings: Map<String, String>,
                                     delegate: MethodVisitor, access: Int, val name: String?,
                                     desc: String?, signature: String?, exceptions: Array<out String>?)
        : GeneratorAdapter(Opcodes.ASM6, delegate, access, name, desc) {

        private val sharedClasses = serverToClientMappings.filter { !(it.value.startsWith("s_")) }
        private val allocationQueue = mutableListOf<String>()
        private var found = false

        override fun visitTypeInsn(opcode: Int, type: String?) {
            if (opcode == Opcodes.NEW && type!!.length > 2 && sharedClasses.containsValue(type.substring(1, type.length - 2))) {
                allocationQueue.add(type)
            } else {
                super.visitTypeInsn(opcode, type)
            }
        }

        override fun visitInsn(opcode: Int) {
//            if (!(opcode == Opcodes.DUP && allocationQueue.isNotEmpty())) {
//                if (found) {
            found = false
//                } else {
            super.visitInsn(opcode)
//                }
//            }
        }

        override fun visitMethodInsn(opcode: Int, owner: String?, methodCalled: String?, desc: String?, itf: Boolean) {
            val debug = className.contains("MinecraftServer") && methodCalled == "<init>" && name == "<init>"
            if (debug) {
                print("$className#$name: $owner $methodCalled $desc isShared=${sharedClasses.containsValue(owner)} ")
                if (opcode and Opcodes.INVOKEVIRTUAL == Opcodes.INVOKEVIRTUAL) print("VIRTUAL")
                if (opcode and Opcodes.INVOKESTATIC == Opcodes.INVOKESTATIC) print("STATIC")
                println()
            }

            if (classNodes.filter { it.name == owner }.filter { it.methods.filter { it.name == "<init>" && it.desc == desc!! }.isEmpty() }.isNotEmpty()
                    && sharedClasses.containsValue(owner)
                    && opcode and Opcodes.INVOKEVIRTUAL == Opcodes.INVOKEVIRTUAL
                    && methodCalled == "<init>") {
                if (debug) {
                    println("Found it")
                }

                if (allocationQueue.isNotEmpty()) {
                    super.visitTypeInsn(Opcodes.NEW, allocationQueue.removeAt(0))
                    super.visitInsn(Opcodes.DUP)
                }
                super.visitMethodInsn(opcode, owner, methodCalled, desc, itf)
//                super.visitLdcInsn(Type.getType("L$owner;"))
//                super.visitMethodInsn(Opcodes.INVOKESTATIC, "io/jadon/pigeon/mixin/JvmUtil", "getUnsafeInstance", "(Ljava/lang/Class;)Ljava/lang/Object;", false)
//                super.visitTypeInsn(Opcodes.CHECKCAST, owner)
//                super.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "serverInit", "(L$owner;${desc!!.substring(1)}", itf)
                found = true
            } else {
                if (allocationQueue.isNotEmpty()) {
                    super.visitTypeInsn(Opcodes.NEW, allocationQueue.removeAt(0))
                    super.visitInsn(Opcodes.DUP)
                }
                super.visitMethodInsn(opcode, owner, methodCalled, desc, itf)
            }
        }

    }

    fun remapJar(vanillaFile: String, mappedFile: String, srgFile: String) {
        val destination = File(mappedFile)
        if (destination.exists()) destination.delete()

        val jarMapping = JarMapping()
        jarMapping.loadMappings(srgFile, false, false, null, null)

        val inheritanceProviders = JointProvider()
        jarMapping.setFallbackInheritanceProvider(inheritanceProviders)

        val jar = Jar.init(File(vanillaFile))

        inheritanceProviders.add(JarProvider(jar))
        val jarRemapper = JarRemapper(jarMapping)
        jarRemapper.remapJar(jar, destination)
    }

}
