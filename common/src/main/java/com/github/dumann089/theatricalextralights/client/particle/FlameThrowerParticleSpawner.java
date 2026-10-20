package com.github.dumann089.theatricalextralights.client.particle;

import com.github.dumann089.theatricalextralights.client.IrisCompat;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
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
    /** Halos de bloom emis par tick a pleine intensite. */
    private static final float GLOW_COUNT = 5.0f;
    /** Les halos partent un peu devant la buse pour ne pas baver sur l'appareil. */
    private static final double GLOW_START = 0.3;

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

        spawnGlow(level, spawnX, spawnY, spawnZ, jetDirection.normalize(), 1.0f, intensityFactor, random);
    }

    /**
     * Le meme jet que le lance-flammes, depuis une buse et dans une direction quelconques.
     *
     * @param reach portee relative du jet, 1 = lance-flammes plein (environ 6 blocs)
     */
    public static void spawnColumn(ClientLevel level, double x, double y, double z, Vec3 direction,
                                   float intensity, float reach, RandomSource random) {
        if (intensity <= 0.0f || !FireworkRenderDistances.isWithinClientFlameRange(x, y, z)) {
            return;
        }
        float intensityFactor = Mth.clamp(intensity, 0.0f, 255.0f) / 255.0f;
        int jetCount = Math.max(2, Math.round((BASE_JET_COUNT + random.nextInt(EXTRA_JET_VARIANCE + 1)) * intensityFactor));
        int puffCount = Math.max(2, Math.round((BASE_PUFF_COUNT + random.nextInt(EXTRA_PUFF_VARIANCE + 1)) * intensityFactor));
        Vec3 dir = direction.normalize();
        Vec3 scaled = dir.scale(reach);
        for (int i = 0; i < jetCount; i++) {
            level.addParticle(ModParticle.FLAME_THROWER_JET.get(), true, x, y, z, scaled.x, scaled.y, scaled.z);
        }
        for (int i = 0; i < puffCount; i++) {
            double along = Mth.lerp(random.nextDouble(), PUFF_MIN_DISTANCE, PUFF_MAX_DISTANCE) * intensityFactor * reach;
            level.addParticle(ModParticle.FLAME_THROWER_PUFF.get(), true,
                    x + dir.x * along, y + dir.y * along, z + dir.z * along, dir.x, dir.y, dir.z);
        }
        spawnGlow(level, x, y, z, dir, reach, intensityFactor, random);
    }

    /**
     * Bloom des flammes : quelques grands halos additifs qui suivent le jet. Coupe en config, et
     * quand un shaderpack tourne (il fait deja son bloom).
     */
    private static void spawnGlow(ClientLevel level, double x, double y, double z, Vec3 dir, float reach,
                                  float intensityFactor, RandomSource random) {
        if (!TheatricalExtraLightsConfig.isFlameBloomEnabled() || IrisCompat.isShadersActive()) {
            return;
        }
        float wanted = GLOW_COUNT * intensityFactor;
        int count = (int) wanted + (random.nextFloat() < wanted - (int) wanted ? 1 : 0);
        double start = GLOW_START * reach;
        for (int i = 0; i < Math.max(1, count); i++) {
            level.addParticle(ModParticle.FLAME_GLOW.get(), true,
                    x + dir.x * start, y + dir.y * start, z + dir.z * start,
                    dir.x * reach, dir.y * reach, dir.z * reach);
        }
    }

    public static double maxSpawnDistanceSq() {
        return FireworkRenderDistances.clientFlameRangeSq();
    }
}
