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
    var clientMappings: String = "null"
    var serverToClientObf: String = "null"
    var serverMappings: String = "null"
    var clientJar: String = "build/minecraft/minecraft_client.jar"
    var serverJar: String = "build/minecraft/minecraft_server.jar"
    var clientMappedJar: String = "build/minecraft/minecraft_client_mapped.jar"
    var serverMappedJar: String = "build/minecraft/minecraft_server_mapped.jar"
}

open class MapMinecraftTask : DefaultTask() {
    @TaskAction
    fun mapMinecraft() {
        println("Generating Merged & Mapped Minecraft Jar")
        val extension = project.extensions.getByType(MapMinecraftExtension::class.java)
        println("Using mappings: ${extension.clientMappings}")
        println("Vanilla Client Jar: ${extension.clientJar}")
        println("Vanilla Server Jar: ${extension.serverJar}")
        println("Mapped Client Jar: ${extension.clientMappedJar}")
        println("Mapped Server Jar: ${extension.serverMappedJar}")

        val clientFile = File(extension.clientJar)
        val serverFile = File(extension.serverJar)
        val clientMappedJar = File(extension.clientMappedJar)
        val serverMappedJar = File(extension.serverMappedJar)

        if (!clientFile.exists() || !serverFile.exists()) {
            println("Downloading Vanilla Jars")
            JarManager.downloadVanillaFiles(clientFile.path, serverFile.path)
        }

        val clientMappingsFile = File(extension.clientMappings)
        if (!clientMappingsFile.exists()) throw IllegalArgumentException("Can't find client mappings file: ${clientMappingsFile.path}")

        val serverToClientObfFile = File(extension.serverToClientObf)
        if (!serverToClientObfFile.exists()) throw IllegalArgumentException("Can't find server to client mappings file: ${serverToClientObfFile.path}")

        val serverMappingsFile = File(extension.serverMappings)
        if (!serverMappingsFile.exists()) throw IllegalArgumentException("Can't find server mappings file: ${serverMappingsFile.path}")

        val tempClientSrgMappings = File("build/minecraft/tempMinecraftClientMappings.srg")
        val tempServerSrgMappings = File("build/minecraft/tempMinecraftServerMappings.srg")

        println("Converting Client TSRG to SRG")
        val clientMappings = TSrgUtil.toSrg(clientMappingsFile, tempClientSrgMappings).toMutableList()
        val serverToClientMappings = TSrgUtil.parseTSrg(serverToClientObfFile.readLines()).toMutableList()
        val serverOnlyMappings = TSrgUtil.parseTSrg(serverMappingsFile.readLines())

        // convert client mappings to server mappings using the serverToClientObf mappings
        println("Creating Server Mappings")
        val classNames = clientMappings.map { it.obf to it.deobf }.toMap()
        val serverMappings = clientMappings.mapNotNull { clientClass ->
            if (!clientClass.deobf.contains("server")) {
                clientClass.deobf = clientClass.deobf.replace("net/minecraft", "net/minecraft/server")
            }
            serverToClientMappings.find { serverToClientMapping ->
                serverToClientMapping.deobf == clientClass.obf
            }?.let { serverToClientClass ->
                val serverFields = clientClass.fields.mapNotNull { clientField ->
                    serverToClientClass.fields.find { it.deobf == clientField.obf }?.let { serverField ->
                        clientField.obf = serverField.obf
                        clientField
                    }
                }.toMutableList()
                val serverMethods = clientClass.methods.mapNotNull { clientMethod ->
                    serverToClientClass.methods.find {
                        it.deobf == clientMethod.obf && it.getDeobfSig(classNames) == clientMethod.obfSig
                    }?.let { serverMethod ->
                        clientMethod.obf = serverMethod.obf
                        clientMethod.obfSig = serverMethod.getDeobfSig(classNames)
                        clientMethod
                    }
                }.toMutableList()
                TSrgUtil.Clazz(serverToClientClass.obf, clientClass.deobf, serverFields, serverMethods)
            }
        }.toMutableList()
        serverMappings.addAll(serverOnlyMappings)

        TSrgUtil.toSrg(serverMappings, tempServerSrgMappings)

        println("Mapping Client Jar (might take a long time)")
        JarManager.remapJar(clientFile.path, clientMappedJar.path, tempClientSrgMappings.path)
        JarManager.transformJar(clientMappedJar, clientMappedJar)

        println("Mapping Server Jar (will take a similar amount of time)")
        JarManager.remapJar(serverFile.path, serverMappedJar.path, tempServerSrgMappings.path)
        JarManager.transformJar(serverMappedJar, serverMappedJar)

        println("Mapping completed")
    }
}
