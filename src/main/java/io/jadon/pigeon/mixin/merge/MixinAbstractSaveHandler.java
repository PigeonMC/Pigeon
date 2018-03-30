package io.jadon.pigeon.mixin.merge;

import net.minecraft.server.entity.AbstractPlayerManager;
import net.minecraft.world.AbstractSaveHandler;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractSaveHandler.class)
public interface MixinAbstractSaveHandler {

    AbstractPlayerManager getPlayerFileManager();

}
