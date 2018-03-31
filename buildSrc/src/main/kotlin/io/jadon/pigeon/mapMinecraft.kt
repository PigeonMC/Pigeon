package io.jadon.pigeon

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.File

open class MapMinecraftPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.add("minecraftData", MapMinecraftExtension::class.java)
        project.tasks.create("mapMinecraft", MapMinecraftTask::class.java)
    }
}

open class MapMinecraftExtension {
    var tsrgFile: String = "null"
    var serverToClientObf: String = "null"
    var clientClassOverride: String = "null"
    var clientJar: String = "build/minecraft/minecraft_client.jar"
    var serverJar: String = "build/minecraft/minecraft_server.jar"
    var mergedJar: String = "build/minecraft/minecraft_merged.jar"
    var mappedJar: String = "build/minecraft/minecraft_merged_mapped.jar"
    var force: Boolean = false
}

open class MapMinecraftTask : DefaultTask() {
    @TaskAction
    fun mapMinecraft() {
//        println("Generating")
//        val classFile = File("mappings/mcp/classes.csv")
//        val (serverOnlyClasses, serverToClientMappings) = MappingsGenerator.getClassMappings(classFile)
//        val combinedMappings: MutableMap<String, String> = serverOnlyClasses.mapNotNull {
//            if (it == "MinecraftServer") null else it to "s_$it"
//        }.toMap().toMutableMap()
//        combinedMappings.putAll(serverToClientMappings)

//        val fieldFile = File("mappings/mcp/fields.csv")
//        val mappings = MappingsGenerator.generateFieldMappings(fieldFile)
//
//        val seenClasses = mutableListOf<String>()
//        mappings.forEach { serverObf, clientObf ->
//            val serverClass = serverObf.split(".")[0]
//            val clientClass = clientObf.split(".")[0]
//            val serverField = serverObf.split(".")[1]
//            val clientField = clientObf.split(".")[1]
//            if (!seenClasses.contains(serverClass)) {
//                println("$serverClass $clientClass")
//                seenClasses.add(serverClass)
//            }
//            println("    $serverField $clientField")
//        }

//        val methodFile = File("mappings/mcp/methods.csv")
//        val mappings = MappingsGenerator.generateMethodMappings(methodFile)
//
//        val seenClasses = mutableListOf<String>()
//        mappings.forEach { serverObf, clientObf ->
//            val serverClass = serverObf.split(".")[0]
//            val clientClass = clientObf.split(".")[0]
//            val serverMethodName = serverObf.split(".")[1]
//            val clientMethodName = clientObf.split(".")[1]
//            val serverMethodSig = serverObf.split(".")[2]
//            val clientMethodSig = clientObf.split(".")[2]
//            if (!seenClasses.contains(serverClass)) {
//                println("$serverClass $clientClass")
//                seenClasses.add(serverClass)
//            }
//            println("    $serverMethodName $serverMethodSig $clientMethodName")
//        }
//        System.exit(-1)

        println("Generating Merged & Mapped Minecraft Jar")
        val extension = project.extensions.getByType(MapMinecraftExtension::class.java)
        println("Using mappings: ${extension.tsrgFile}")
        println("Vanilla Client Jar: ${extension.clientJar}")
        println("Vanilla Server Jar: ${extension.serverJar}")
        println("Merged Jar: ${extension.mergedJar}")
        println("Mapped Jar: ${extension.mappedJar}")

        val serverToClientObfFile = File(extension.serverToClientObf)
        val clientFile = File(extension.clientJar)
        val serverFile = File(extension.serverJar)
        val mergedFile = File(extension.mergedJar)
        val mappedFile = File(extension.mappedJar)

        if (!clientFile.exists()) {
            println("Downloading Vanilla Jar")
            JarManager.downloadVanillaClient(clientFile.path)
        }

        if (!serverFile.exists()) {
            println("ERROR: You're missing a Beta 1.7.3 Server jar!")
            System.exit(-1)
        }

        val combinedMappings = TSrgUtil.parseTSrg(serverToClientObfFile.readLines())
        val serverOnlyClasses = combinedMappings.filter { it.deobf.startsWith("s_") }.map { it.deobf }
        val serverToClientMappingsFile = File.createTempFile("tempMinecraftMappings", ".srg")
        TSrgUtil.toSrg(combinedMappings, serverToClientMappingsFile)

        // Get Client Class Overrides
        val clientClassOverrideFile = File(extension.clientClassOverride)
        val clientClassOverrides: List<String> = if (clientClassOverrideFile.exists() && clientClassOverrideFile.isFile) {
            val overrides = mutableListOf<String>()
            val lines = clientClassOverrideFile.readLines()
            lines.forEach {
                if (it.trim().startsWith("#") || it.trim().isEmpty()) { // comment
                } else overrides.add(it.split(" ")[0])
            }
            overrides
        } else listOf()

        println("Merging Jars")
        JarManager.mergeJars(
                clientFile,
                serverFile,
                mergedFile,
                serverToClientMappingsFile,
                serverOnlyClasses,
                clientClassOverrides
        )

        val tsrgFile = File(extension.tsrgFile)
        if (!tsrgFile.exists()) throw IllegalArgumentException("Can't find mappings file: ${tsrgFile.path}")

        val tempSrgFile = File.createTempFile("tempMinecraftMappings", ".srg")

        println("Converting TSRG to SRG")
        TSrgUtil.toSrg(tsrgFile, tempSrgFile)

        println("Mapping Jar (might take a long time)")
        JarManager.remapJar(mergedFile.path, mappedFile.path, tempSrgFile.path)

        println("Mapping completed")
    }
}
