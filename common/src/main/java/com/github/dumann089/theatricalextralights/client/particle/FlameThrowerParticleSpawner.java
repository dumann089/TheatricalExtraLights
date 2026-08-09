package com.github.dumann089.theatricalextralights.client.particle;

import com.github.dumann089.theatricalextralights.particle.ModParticle;
import com.github.dumann089.theatricalextralights.firework.FireworkRenderDistances;
import com.github.dumann089.theatricalextralights.util.DirectionOffset;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Émission client des particules du lance-flammes. */
@Environment(EnvType.CLIENT)
public final class FlameThrowerParticleSpawner {
    private static final int BASE_JET_COUNT = 18;
    private static final int EXTRA_JET_VARIANCE = 10;
    private static final int BASE_PUFF_COUNT = 8;
    private static final int EXTRA_PUFF_VARIANCE = 6;
    private static final double PUFF_MIN_DISTANCE = 0.14;
    private static final double PUFF_MAX_DISTANCE = 0.48;

    private FlameThrowerParticleSpawner() {
    }

    public static void spawnJet(
            ClientLevel level,
            BlockPos blockPos,
            Direction facing,
            float headRenderAngleDegrees,
            float intensity,
            RandomSource random
    ) {
        if (intensity <= 0.0f) {
            return;
        }

        Vec3 nozzle = DirectionOffset.flameNozzlePosition(facing, headRenderAngleDegrees);
        double spawnX = blockPos.getX() + nozzle.x;
        double spawnY = blockPos.getY() + nozzle.y;
        double spawnZ = blockPos.getZ() + nozzle.z;

        if (!FireworkRenderDistances.isWithinClientFlameRange(spawnX, spawnY, spawnZ)) {
            return;
        }

        float intensityFactor = Mth.clamp(intensity, 0.0f, 255.0f) / 255.0f;
        int jetCount = Math.max(2, Math.round((BASE_JET_COUNT + random.nextInt(EXTRA_JET_VARIANCE + 1)) * intensityFactor));
        int puffCount = Math.max(2, Math.round((BASE_PUFF_COUNT + random.nextInt(EXTRA_PUFF_VARIANCE + 1)) * intensityFactor));

        Vec3 jetDirection = DirectionOffset.headJetDirection(facing, headRenderAngleDegrees);

        // force=true : contourne la limite vanilla ~32 blocs de ClientLevel.addParticle
        for (int i = 0; i < jetCount; i++) {
            level.addParticle(
                    ModParticle.FLAME_THROWER_JET.get(),
                    true,
                    spawnX,
                    spawnY,
                    spawnZ,
                    jetDirection.x,
                    jetDirection.y,
                    jetDirection.z
            );
        }

        for (int i = 0; i < puffCount; i++) {
            double along = Mth.lerp(random.nextDouble(), PUFF_MIN_DISTANCE, PUFF_MAX_DISTANCE) * intensityFactor;
            Vec3 puffPos = DirectionOffset.flamePointAlongJet(facing, headRenderAngleDegrees, along);
            level.addParticle(
                    ModParticle.FLAME_THROWER_PUFF.get(),
                    true,
                    blockPos.getX() + puffPos.x,
                    blockPos.getY() + puffPos.y,
                    blockPos.getZ() + puffPos.z,
                    jetDirection.x,
                    jetDirection.y,
                    jetDirection.z
            );
        }
    }

    public static double maxSpawnDistanceSq() {
        return FireworkRenderDistances.clientFlameRangeSq();
    }
}
