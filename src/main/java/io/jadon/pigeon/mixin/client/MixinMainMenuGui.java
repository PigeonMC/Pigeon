package io.jadon.pigeon.mixin.client;

import net.minecraft.client.gui.MainMenuGui;
import net.minecraft.client.gui.ScreenGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = MainMenuGui.class, priority = 1)
public abstract class MixinMainMenuGui extends ScreenGui {

    @ModifyArg(
            remap = false,
            method = "drawScreen",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/MainMenuGui;drawString(Lnet/minecraft/client/render/FontRenderer;Ljava/lang/String;III)V",
                    ordinal = 0
            ),
            index = 1
    )
    public String changeVersionString(String version) {
        return version + " (Pigeon Mod Loader)";
    }

}
