package io.jadon.pigeon.launcher

import net.minecraft.launchwrapper.ITweaker
import net.minecraft.launchwrapper.Launch
import net.minecraft.launchwrapper.LaunchClassLoader
import org.apache.logging.log4j.LogManager
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.Mixins
import java.nio.file.Files
import java.nio.file.Paths

object Pigeon {

    @JvmField
    val Logger = LogManager.getLogger("Pigeon")

    @JvmStatic
    fun main(args: Array<String>) {
        Logger.info("Starting Pigeon")
        launchClient()
    }

    fun launchClient() {
        Pigeon.Logger.info("Starting Client in LegacyLauncher")
        PigeonTweakClass.LAUNCH_TARGET = "net.minecraft.client.Minecraft"
        Launch.main(arrayOf("--tweakClass", PigeonTweakClass::class.java.name))
    }

    fun launchServer() {
        Pigeon.Logger.info("Starting Server in LegacyLauncher")
        PigeonTweakClass.LAUNCH_TARGET = "net.minecraft.server.MinecraftServer"
        Launch.main(arrayOf("--tweakClass", PigeonTweakClass::class.java.name))
    }

    fun setupMods(classLoader: LaunchClassLoader) {
        // TODO: Load mods
    }

}

class PigeonTweakClass : ITweaker {
    companion object {
        var LAUNCH_TARGET = "null (user needs to set this)"
        // TODO: get this from Gradle?
        const val LOCATION_OF_LOCAL_MAPPED_MINECRAFT = "build/minecraft/minecraft_merged_mapped.jar"
    }

    override fun getLaunchTarget(): String = LAUNCH_TARGET

    override fun injectIntoClassLoader(classLoader: LaunchClassLoader) {
        Pigeon.Logger.info("Initializing Mixins")
        MixinBootstrap.init()
        MixinEnvironment.getDefaultEnvironment().side = MixinEnvironment.Side.CLIENT

        // Get Pigeon Mixin Configs
        val mixinConfigsLocation = javaClass.getResource("/")
        val mixinPath = Paths.get(mixinConfigsLocation.toURI()).parent
        Files.walk(mixinPath, 3).forEach {
            if (it.toFile().isFile) {
                Pigeon.Logger.info("Found Mixin Config: ${it.fileName}")
                Mixins.addConfiguration(it.fileName.toString())
            }
        }

        Pigeon.setupMods(classLoader)

        classLoader.addURL(Paths.get(LOCATION_OF_LOCAL_MAPPED_MINECRAFT).toUri().toURL())
    }

    override fun getLaunchArguments(): Array<String> = arrayOf()

    override fun acceptOptions(args: List<String>) {}

}
