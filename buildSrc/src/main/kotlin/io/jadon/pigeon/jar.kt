package io.jadon.pigeon

import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
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
        val mergedClasses = clientJar.entries().toList().filter { !it.name.contains("META-INF") }.map {
            it.name to clientJar.getInputStream(it).readBytes()
        }.toMutableList()

        clientJar.close()

        // Add remapped server classes
        mergedClasses.addAll(serverClassesToMove)

        val mergedJarStream = ZipOutputStream(FileOutputStream(merged))
        val output = BufferedOutputStream(mergedJarStream)

        mergedClasses.forEach {
            //            println("Creating zip entry for ${it.first}")
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
