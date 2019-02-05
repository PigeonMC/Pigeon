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

    private val CLIENT_URL = URL("http://s3.amazonaws.com/Minecraft.Download/versions/b1.7.3/b1.7.3.jar")
    private val SERVER_URL = URL("https://betacraft.ovh/server-archive/minecraft/b1.7.3.jar")

    fun downloadVanillaFiles(client: String, server: String) {
        downloadFile(client, CLIENT_URL)
        downloadFile(server, SERVER_URL)
    }

    fun downloadFile(destinationFile: String, url: URL) {
        val path = Paths.get(destinationFile)
        path.parent.toFile().mkdirs()
        if (!path.toFile().isFile) {
            url.openStream().use { Files.copy(it, path) }
        }
    }

    fun transformJar(inFile: File, outFile: File) {
        val inJar = JarFile(inFile)

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

        //  Get classes
        val files: List<Pair<String, ByteArray>> = inJar.entries().toList()
                .filter { !it.name.contains("META-INF") }
                .map {
                    val inputStream = inJar.getInputStream(it)
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    it.name to if (it.name.endsWith(".class")) {
                        val classNode = ClassNode()
                        val classReader = ClassReader(bytes)
                        classReader.accept(classNode, 0)
                        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
                        // transform classes
                        checkAccess(classNode)
                        classNode.accept(classWriter)
                        classWriter.toByteArray()
                    } else bytes
                }.toList()
        inJar.close()

        val outJarStream = ZipOutputStream(FileOutputStream(outFile))
        val output = BufferedOutputStream(outJarStream)

        files.forEach {
            outJarStream.putNextEntry(JarEntry(it.first))
            output.write(it.second)
            output.flush()
            outJarStream.closeEntry()
        }
        output.close()
        outJarStream.close()
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
