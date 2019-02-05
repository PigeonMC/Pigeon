package io.jadon.pigeon

import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
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

    fun mergeJars(client: File, server: File, merged: File,
                  serverToClientMappings: File, serverOnlyClasses: List<String>, clientClassOverrides: List<String>) {
        val clientJar = JarFile(client)
        // Remap the server jar to the client obfuscations
        val remappedServer = merged.absolutePath.substring(0, merged.absolutePath.lastIndexOf('/')) + "/minecraft_server_merobf.jar"
        remapJar(
                server.absolutePath,
                remappedServer,
                serverToClientMappings.absolutePath
        )
        val remappedServerJar = JarFile(File(remappedServer))

        /**
         * Transform the access for all protected/package-private entities to public.
         */
        fun checkAccess(classNode: ClassNode) {
            val acc = classNode.access
            classNode.access = acc or Opcodes.ACC_PUBLIC
            classNode.methods.forEach {
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

        //  Get client classes
        val mergedFiles = clientJar.entries().toList().filter { !it.name.contains("META-INF") }.filter {
            // Remove client class overrides
            if (it.name.endsWith(".class")) {
                !clientClassOverrides.contains(it.name.removeSuffix(".class"))
            } else true
        }.map {
            val inputStream = clientJar.getInputStream(it)
            val r = it.name to inputStream.readBytes()
            inputStream.close()
            r
        }.toMutableList()

        clientJar.close()

        // Add remapped server classes
        mergedFiles.addAll(remappedServerJar.entries().toList().filter {
            (it.name.endsWith(".class")
                    && (serverOnlyClasses.contains(it.name.removeSuffix(".class"))
                    || clientClassOverrides.contains(it.name.removeSuffix(".class"))))
                    || it.name == "net/minecraft/server/MinecraftServer.class"
        }.map {
            val inputStream = remappedServerJar.getInputStream(it)
            val r = it.name to inputStream.readBytes()
            inputStream.close()
            r
        })

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
            classNode.accept(classWriter)
            name to classWriter.toByteArray()
        }.toMap()

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
