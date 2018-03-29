package io.jadon.pigeon.launcher

import io.jadon.pigeon.api.ModContainer
import io.jadon.pigeon.api.ModInfo
import io.jadon.pigeon.mod.ModClassVisitor
import net.minecraft.launchwrapper.ITweaker
import net.minecraft.launchwrapper.Launch
import net.minecraft.launchwrapper.LaunchClassLoader
import org.apache.logging.log4j.LogManager
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.Mixins
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile

object Pigeon {

    const val PIGEON_HOME = "pigeon/"
    const val MODS_LOCATION = PIGEON_HOME + "mods/"

    @JvmField
    val Logger = LogManager.getLogger("Pigeon")

    val modContainers = mutableListOf<ModContainer<*>>()

    @JvmStatic
    fun main(args: Array<String>) {
        Logger.info("Starting Pigeon")
        launchClient()
    }

    fun launchClient() {
        Pigeon.Logger.info("Starting Client in LegacyLauncher")
        PigeonTweakClass.LAUNCH_TARGET = "net.minecraft.client.MinecraftClient"
        Launch.main(arrayOf("--tweakClass", PigeonTweakClass::class.java.name))
    }

    fun launchServer() {
        Pigeon.Logger.info("Starting Server in LegacyLauncher")
        PigeonTweakClass.LAUNCH_TARGET = "net.minecraft.server.MinecraftServer"
        Launch.main(arrayOf("--tweakClass", PigeonTweakClass::class.java.name))
    }

    fun setupMods(classLoader: LaunchClassLoader) {
        val modsPath = Paths.get(MODS_LOCATION)
        if (Files.notExists(modsPath)) Files.createDirectories(modsPath)
        if (!Files.isDirectory(modsPath)) {
            Logger.error("Mods folder $modsPath is not a folder! NOT LOADING MODS.")
            return
        }

        val foundMods = mutableListOf<Triple<ModInfo, Path, List<String>>>()

        Files.list(modsPath).filter { it.toFile().extension == "jar" }.forEach { modPath ->
            val modFile = modPath.toFile()
            val jar = ZipFile(modFile)
            val files = jar.entries().toList()

            var modInfo: ModInfo? = null
            val mixinConfigs = mutableListOf<String>()

            var f = false // hack because Kotlin compiler was crashing with "files@"
            files.forEach {
                if (!f) {
                    if (it.name == "plugin.yml") {
                        Logger.error("${modFile.name} looks like a Bukkit Plugin! Put those in /plugins/")
                        f = true
                    }

                    if (it.name == "mcmod.info") {
                        Logger.error("${modFile.name} looks like a Forge or Sponge mod! How the hell did that get in here?")
                        f = true
                    }

                    if (it.name.endsWith(".class")) {
                        val inputStream = jar.getInputStream(it)
                        val bytes = inputStream.readBytes()
                        inputStream.close()
                        val t = ModClassVisitor.getModInfo(bytes)
                        if (t != null && modInfo == null) {
                            modInfo = t
                        }
                    }

                    if (it.name.startsWith("mixins.") && it.name.endsWith(".json")) {
                        mixinConfigs.add(it.name)
                    }
                }
            }

            modInfo?.let {
                foundMods.add(Triple(it, modPath, mixinConfigs))
            }
        }

        foundMods.forEach { (modInfo, modPath, mixinConfigs) ->
            Logger.info("Initializing Mod: ${modInfo.id}")
            val modUrl = modPath.toUri().toURL()
            classLoader.addURL(modUrl)
            val clazz = Class.forName(modInfo.className, true, classLoader)

            val modContainer = ModContainer(clazz, modInfo)

            mixinConfigs.forEach {
                Logger.info("Found Mixin Config in ${modInfo.id}: $it")
                Mixins.addConfiguration(it)
            }

            modContainers.add(modContainer)
        }

        Logger.info("Found mods: $modContainers")
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
