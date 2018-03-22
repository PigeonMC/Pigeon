package io.jadon.pigeon.launcher

import java.io.File

object PigeonLauncher {

    @JvmStatic
    fun main(args: Array<String>) {
        val srgFile = File("mappings/client.srg")
        val tsrgFile = File("mappings/client.tsrg")
        val generatedSrgFile = File("mappings/client_generated.srg")

        TSrgUtil.fromSrg(srgFile, tsrgFile)
        TSrgUtil.toSrg(tsrgFile, generatedSrgFile)
    }

}
