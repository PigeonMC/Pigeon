package io.jadon.pigeon.example.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MinecraftClient.class)
public class MixinMinecraft {

    @Overwrite
    public static void main(String[] args) {
        System.out.println("Minecraft.main Overwritten from Mod");
    }

}
