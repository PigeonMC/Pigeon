package io.jadon.pigeon.mixin.merge;

import io.jadon.pigeon.launcher.Pigeon;
import io.jadon.pigeon.JvmUtil;
import net.minecraft.util.FrozenThread;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements Runnable, CommandListener {

    @Redirect(
            remap = false,
            method = "<init>",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/server/MinecraftServer;)Lnet/minecraft/util/FrozenThread;"
            )
    )
    public FrozenThread fixFrozenThread(MinecraftServer minecraftServer) {
        Pigeon.Logger.info("MinecraftServer Constructor Redirected");
        FrozenThread instance = JvmUtil.getUnsafeInstance(FrozenThread.class);
//        instance.setDaemon(true);
//        instance.start();
        return instance;
    }

}
