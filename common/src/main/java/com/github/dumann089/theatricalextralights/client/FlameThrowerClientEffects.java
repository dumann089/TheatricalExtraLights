package com.github.dumann089.theatricalextralights.client;

import com.github.dumann089.theatricalextralights.blockentities.FlameThrowerBlockEntity;
import com.github.dumann089.theatricalextralights.client.particle.FlameThrowerParticleSpawner;
import com.github.dumann089.theatricalextralights.client.sfx.FixtureLoopSfx;
import com.github.dumann089.theatricalextralights.sounds.ModSounds;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public final class FlameThrowerClientEffects {
    /** Volume de base — atténué par la distance (son fixed-range + LINEAR). */
    private static final float LOOP_VOLUME = 0.14f;
    /** Audible seulement à proximité du lance-flammes. */
    private static final double HEAR_DISTANCE = 48.0;
    private static final double HEAR_DISTANCE_SQ = HEAR_DISTANCE * HEAR_DISTANCE;

    private FlameThrowerClientEffects() {
    }

    public static void tick(FlameThrowerBlockEntity blockEntity) {
        boolean active = blockEntity.getIntensity() > 0;
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos pos = blockEntity.getBlockPos();
        Vec3 center = pos.getCenter();

        boolean playerCanHear = minecraft.player != null
                && minecraft.player.distanceToSqr(center) <= HEAR_DISTANCE_SQ;
        boolean shouldPlaySound = active && playerCanHear;

        if (shouldPlaySound) {
            FixtureLoopSfx.sustain(
                    blockEntity.getLevel(),
                    pos,
                    ModSounds.FLAME_THROWER_LOOP.get(),
                    LOOP_VOLUME,
                    1.0f
            );
        } else {
            FixtureLoopSfx.release(pos);
        }

        if (!active) {
            return;
        }

        if (!(blockEntity.getLevel() instanceof ClientLevel level)) {
            return;
        }

        if (!com.github.dumann089.theatricalextralights.firework.FireworkRenderDistances.isWithinClientFlameRange(
                center.x, center.y, center.z)) {
            return;
        }

        Direction facing = blockEntity.getBlockState().getValue(BaseLightBlock.FACING);
        float headRenderAngle = blockEntity.getHeadRenderAngle(minecraft.getFrameTime());

        FlameThrowerParticleSpawner.spawnJet(
                level,
                pos,
                facing,
                headRenderAngle,
                blockEntity.getIntensity(),
                level.random
        );
    }

    public static void stop(BlockPos pos) {
        FixtureLoopSfx.release(pos);
    }
}
