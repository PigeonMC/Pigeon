package io.jadon.pigeon.mixin.server.merge;

import net.minecraft.FrozenThread;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FrozenThread.class)
public abstract class MixinFrozenThread extends Thread {

    public static void serverInit(FrozenThread instance, MinecraftServer server) {
        instance.setDaemon(true);
        instance.start();
    }

}
