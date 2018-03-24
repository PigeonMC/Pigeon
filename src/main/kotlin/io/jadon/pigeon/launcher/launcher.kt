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
import java.util.logging.Logger

object PigeonLauncher {

    val Log = Logger.getLogger("Pigeon")

    @JvmStatic
    fun main(args: Array<String>) {
        Log.info("Starting Pigeon")
        Bootstrap.init()
    }

}

class Bootstrap : ITweaker {
    companion object {
        val LAUNCH_TARGET = "net.minecraft.client.Minecraft"
        // TODO: get this from Gradle?
        val LOCATION_OF_LOCAL_MAPPED_MINECRAFT = "build/minecraft/minecraft_mapped.jar"

        @JvmStatic
        fun init() {
            PigeonLauncher.Log.info("Starting LegacyLauncher")
            Launch.main(arrayOf("--tweakClass", Bootstrap::class.java.name))
        }
    }

    override fun getLaunchTarget(): String = LAUNCH_TARGET

    override fun injectIntoClassLoader(classLoader: LaunchClassLoader) {
        PigeonLauncher.Log.info("Initializing Mixins")
        MixinBootstrap.init()
        MixinEnvironment.getDefaultEnvironment().side = MixinEnvironment.Side.CLIENT

        classLoader.addURL(Paths.get(LOCATION_OF_LOCAL_MAPPED_MINECRAFT).toUri().toURL())
    }

    override fun getLaunchArguments(): Array<String> = arrayOf()

    override fun acceptOptions(args: List<String>) {}

}
