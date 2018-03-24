package io.jadon.pigeon

import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths

object JarManager {

    val DOWNLOAD_URL = URL("http://s3.amazonaws.com/Minecraft.Download/versions/b1.7.3/b1.7.3.jar")

    fun downloadVanilla(destinationFile: String) {
        val path = Paths.get(destinationFile)
        path.parent.toFile().mkdirs()
        if (!path.toFile().isFile) {
            DOWNLOAD_URL.openStream().use { Files.copy(it, path) }
        }
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
