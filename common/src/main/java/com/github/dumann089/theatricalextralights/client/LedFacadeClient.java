package com.github.dumann089.theatricalextralights.client;

import com.github.dumann089.theatricalextralights.blockentities.LedFacadeBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Application client des trames DMX reçues (isolé pour éviter tout chargement de classe client côté serveur). */
@Environment(EnvType.CLIENT)
public final class LedFacadeClient {

    private LedFacadeClient() {
    }

    public static void applyFrames(net.minecraft.core.BlockPos pos, int[] offsets, byte[][] frames) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof LedFacadeBlockEntity facade) {
            facade.applyFrames(offsets, frames);
        }
    }
}
