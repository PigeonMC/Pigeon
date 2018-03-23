package io.jadon.pigeon.launcher

import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.provider.JointProvider
import net.minecraft.launchwrapper.ITweaker
import net.minecraft.launchwrapper.Launch
import net.minecraft.launchwrapper.LaunchClassLoader
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.MixinEnvironment
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths

object PigeonLauncher {

    @JvmStatic
    fun main(args: Array<String>) {
        val srgFile = File("mappings/client.srg")
        val tsrgFile = File("mappings/client.tsrg")
        val generatedSrgFile = File("mappings/client_generated.srg")

        TSrgUtil.fromSrg(srgFile, tsrgFile)
        TSrgUtil.toSrg(tsrgFile, generatedSrgFile)

        val workDirectory = "build/minecraft/"
        JarManager.downloadVanilla("$workDirectory/minecraft.jar")
        JarManager.remapJar("$workDirectory/minecraft.jar", generatedSrgFile.path)

        println("hey no errors remapping!")

        Bootstrap.init()
    }

}

object JarManager {

    val downloadUrl = URL("http://s3.amazonaws.com/Minecraft.Download/versions/b1.7.3/b1.7.3.jar")

    fun downloadVanilla(destinationFile: String) {
        val path = Paths.get(destinationFile)
        path.parent.toFile().mkdirs()
        if (path.toFile().isFile) path.toFile().delete()
        downloadUrl.openStream().use { Files.copy(it, path) }
    }

    fun remapJar(destinationFile: String, srgFile: String) {
        val jarMapping = JarMapping()
        jarMapping.loadMappings(srgFile, false, false, null, null)

        val inheritanceProviders = JointProvider()
        jarMapping.setFallbackInheritanceProvider(inheritanceProviders)

        val jarRemapper = JarRemapper(jarMapping)
        jarRemapper.remapJar(
                Jar.init(File(destinationFile)),
                Paths.get(destinationFile).parent.resolve("minecraft_mapped.jar").toFile()
        )
    }

}

class Bootstrap : ITweaker {
    companion object {
        val LAUNCH_TARGET = "net.minecraft.client.Minecraft"

        @JvmStatic
        fun init() {
            Launch.main(arrayOf("--tweakClass", Bootstrap::class.java.name))
        }
    }

    override fun getLaunchTarget(): String = LAUNCH_TARGET

    override fun injectIntoClassLoader(classLoader: LaunchClassLoader) {
        MixinBootstrap.init()
        MixinEnvironment.getDefaultEnvironment().side = MixinEnvironment.Side.CLIENT
    }

    override fun getLaunchArguments(): Array<String> = arrayOf()

    override fun acceptOptions(args: List<String>) {}

}
