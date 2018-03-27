package io.jadon.pigeon.mixin;

import io.jadon.pigeon.launcher.Pigeon;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft implements Runnable {

//    @Inject(method = "main", at = @At("HEAD"))
    @Overwrite
    public static void main(String[] args) {
        Pigeon.Logger.info("Starting Minecraft Client with Pigeon Mixins");
    }

}
