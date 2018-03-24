package io.jadon.pigeon.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft implements Runnable {

    @Overwrite
    public static void main(String[] args) {
        System.out.println("Minecraft.main() has been OVERWRITTEN");
    }
}
