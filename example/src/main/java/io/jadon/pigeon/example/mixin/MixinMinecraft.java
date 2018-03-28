package io.jadon.pigeon.example.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Overwrite
    public static void main(String[] args) {
        System.out.println("Minecraft.main Overwritten from Mod");
    }

}
