package io.jadon.pigeon.mixin.merge;

import net.minecraft.network.packet.EntityStatusPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.AbstractSaveHandler;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld extends World {

    // Make compiler happy
    public MixinServerWorld(AbstractSaveHandler var1, String var2, WorldProvider var3, long var4) {
        super(var1, var2, var3, var4);
    }

    // Expected to exist
    public AbstractSaveHandler getSaveHandler() {
        return this.saveHandler;
    }

    @Redirect(
            remap = false,
            method = "a(Lnet/minecraft/entity/Entity;B)V",
            at = @At(
                    value = "NEW",
                    target = "(IB)Lnet/minecraft/network/packet/EntityStatusPacket;"
            )
    )
    public EntityStatusPacket redirectConstructor(int entityId, byte entityStatus) {
        EntityStatusPacket packet = new EntityStatusPacket();
        packet.entityId = entityId;
        packet.entityStatus = entityStatus;
        return packet;
    }

}
