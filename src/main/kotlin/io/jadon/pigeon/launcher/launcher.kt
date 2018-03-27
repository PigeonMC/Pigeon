package io.jadon.pigeon.launcher

import net.minecraft.launchwrapper.ITweaker
import net.minecraft.launchwrapper.Launch
import net.minecraft.launchwrapper.LaunchClassLoader
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.Mixins
import java.nio.file.Paths
import java.util.logging.Logger

object PigeonLauncher {

    val Log = Logger.getLogger("Pigeon")

    @JvmStatic
    fun main(args: Array<String>) {
        Log.info("Starting Pigeon")
        launchClient()
    }

    fun launchClient() {
        PigeonLauncher.Log.info("Starting Client in LegacyLauncher")
        Bootstrap.LAUNCH_TARGET = "net.minecraft.client.Minecraft"
        Launch.main(arrayOf("--tweakClass", Bootstrap::class.java.name))
    }

    fun launchServer() {
        PigeonLauncher.Log.info("Starting Server in LegacyLauncher")
        Bootstrap.LAUNCH_TARGET = "net.minecraft.server.MinecraftServer"
        Launch.main(arrayOf("--tweakClass", Bootstrap::class.java.name))
    }

}

class Bootstrap : ITweaker {
    companion object {
        var LAUNCH_TARGET = "null (user needs to set this)"
        // TODO: get this from Gradle?
        val LOCATION_OF_LOCAL_MAPPED_MINECRAFT = "build/minecraft/minecraft_merged_mapped.jar"
    }

    override fun getLaunchTarget(): String = LAUNCH_TARGET

    override fun injectIntoClassLoader(classLoader: LaunchClassLoader) {
        PigeonLauncher.Log.info("Initializing Mixins")
        MixinBootstrap.init()
        MixinEnvironment.getDefaultEnvironment().side = MixinEnvironment.Side.CLIENT
        Mixins.addConfiguration("mixins.pigeon.json")

        // TODO: Load mods here

        classLoader.addURL(Paths.get(LOCATION_OF_LOCAL_MAPPED_MINECRAFT).toUri().toURL())
    }

    override fun getLaunchArguments(): Array<String> = arrayOf()

    override fun acceptOptions(args: List<String>) {}

}
