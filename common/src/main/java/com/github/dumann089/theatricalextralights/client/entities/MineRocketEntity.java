package com.github.dumann089.theatricalextralights.entities;

import com.github.dumann089.theatricalextralights.firework.FireworkPreset;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class MineRocketEntity extends FireworkRocketEntity {
    public MineRocketEntity(Level level, BlockPos launcherPos) {
        super(level, FireworkPreset.MINE_COMET, launcherPos);
    }
}