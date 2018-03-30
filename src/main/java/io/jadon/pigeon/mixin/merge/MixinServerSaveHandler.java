package io.jadon.pigeon.mixin.merge;

import net.minecraft.entity.PlayerEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.entity.AbstractPlayerManager;
import net.minecraft.server.world.ServerSaveHandler;
import net.minecraft.util.CompressedStreamTools;
import net.minecraft.world.AbstractSaveHandler;
import net.minecraft.world.SaveHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.io.FileOutputStream;
import java.util.logging.Logger;

@Mixin(ServerSaveHandler.class)
public abstract class MixinServerSaveHandler implements AbstractSaveHandler, AbstractPlayerManager {

//    @Shadow
//    private static Logger logger;
//
//    @Shadow
//    private File playersDirectory;

    // Expected to be there
    public AbstractPlayerManager getPlayerFileManager() {
        return this;
    }

//    @Override
//    public void a(PlayerEntity player) {
//        try {
//            NBTTagCompound compound = new NBTTagCompound();
//            player.writeEntityToNBT(compound);
//            File tempFile = new File(playersDirectory, "_tmp_.dat");
//            File playerFile = new File(playersDirectory, player.username + ".dat");
//
//            CompressedStreamTools.a(compound, new FileOutputStream(tempFile));
//            if (playerFile.exists()) {
//                playerFile.delete();
//            }
//            tempFile.renameTo(playerFile);
//        } catch (Exception e) {
//            logger.warning("Failed to save player data for " + player.username);
//        }
//    }
//
//    @Override
//    public void b(PlayerEntity player) {
//
//    }

}
