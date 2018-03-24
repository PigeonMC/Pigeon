package io.jadon.pigeon.mixin

import net.minecraft.client.Minecraft
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Overwrite

@Mixin(Minecraft::class)
abstract class MixinMinecraft2 : Runnable {
    private companion object {
        @JvmStatic
        @Overwrite
        fun main(args: Array<String>) {
            println("Minecraft.main() has been overwritten by a Kotlin Mixin")
        }
    }
}
