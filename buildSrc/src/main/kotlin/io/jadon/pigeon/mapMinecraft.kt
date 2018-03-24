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
    var tempSrgFile: String? = null
    var unmappedJar: String = "null"
    var mappedJar: String = "null"
    var force: Boolean = false
}

open class MapMinecraftTask : DefaultTask() {
    @TaskAction
    fun mapMinecraft() {
        println("Generating Mapped Minecraft Jar")
        val extension = project.extensions.getByType(MapMinecraftExtension::class.java)
        println("Using mappings: ${extension.tsrgFile}")
        println("Downloading Vanilla Jar to: ${extension.unmappedJar}")
        println("Mapped File will be located at: ${extension.mappedJar}")

        val tsrgFile = File(extension.tsrgFile)
        if (!tsrgFile.exists()) throw IllegalArgumentException("Can't find mappings file: ${tsrgFile.path}")

        val tempSrgFile = if (extension.tempSrgFile == null) File.createTempFile("temporaryMinecraftMappings", ".srg")
        else File(extension.tempSrgFile)

        println("Converting TSRG to SRG")
        TSrgUtil.toSrg(tsrgFile, tempSrgFile)

        val minecraftFile = File(extension.unmappedJar)
        val mappedFile = File(extension.mappedJar)

        if (!minecraftFile.exists()) {
            println("Downloading Vanilla Jar")
            JarManager.downloadVanilla(minecraftFile.path)
        }

        if (mappedFile.exists() || extension.force) {
            println("Mapping Jar (might take a long time)")
            JarManager.remapJar(minecraftFile.path, mappedFile.path, tempSrgFile.path)
        }

        println("Mapping completed")
    }
}
