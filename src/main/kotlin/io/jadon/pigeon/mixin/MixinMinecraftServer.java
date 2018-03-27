package io.jadon.pigeon.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ICommandListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements Runnable, ICommandListener {

    @Overwrite
    public static void main(String[] args) {
        System.out.println("MinecraftServer.main() has been OVERWRITTEN");
    }

}
