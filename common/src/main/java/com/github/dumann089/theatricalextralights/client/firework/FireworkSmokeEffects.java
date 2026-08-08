package com.github.dumann089.theatricalextralights.client.firework;

import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.entities.FireworkRocketEntity;
import com.github.dumann089.theatricalextralights.firework.Spark;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

public final class FireworkSmokeEffects {
    private static int budgetThisTick;
    private static long budgetTick = Long.MIN_VALUE;
    private static int mortarSmokeBudget = 0;

    private static final int MAX_MORTAR_SMOKE_PARTICLES = 1500;

    private FireworkSmokeEffects() {
    }

    public static void beginClientTick(long gameTime) {
        if (gameTime != budgetTick) {
            budgetTick = gameTime;
            budgetThisTick = TheatricalExtraLightsConfig.getFireworkSmokeBudgetPerTick();

            mortarSmokeBudget = 0;
        }
    }

    private static boolean canSpawnNearPlayer(double x, double y, double z, double maxRange) {
        if (Minecraft.getInstance().player == null) {
            return false;
        }
        double dx = x - Minecraft.getInstance().player.getX();
        double dy = y - Minecraft.getInstance().player.getY();
        double dz = z - Minecraft.getInstance().player.getZ();
        return dx * dx + dy * dy + dz * dz <= maxRange * maxRange;
    }

    private static boolean consumeBudget() {
        if (!TheatricalExtraLightsConfig.isFireworkSmokeEnabled() || budgetThisTick <= 0) {
            return false;
        }
        budgetThisTick--;
        return true;
    }

    public static void trySpawnFlightSmoke(FireworkRocketEntity rocket, RandomSource random, int flightLife) {
        if (!TheatricalExtraLightsConfig.isFireworkSmokeEnabled() || budgetThisTick <= 0) {
            return;
        }
        if (!rocket.getPreset().getPattern().spawnsFlightSmoke()) {
            return;
        }
        if (rocket.getPreset().getPattern().isDaytimePowder()) {
            return;
        }
        if (flightLife % TheatricalExtraLightsConfig.getFireworkSmokeSpawnInterval() != 0) {
            return;
        }
        if (!(rocket.level() instanceof ClientLevel level)) {
            return;
        }
        if (!canSpawnNearPlayer(rocket.getX(), rocket.getY(), rocket.getZ(), 96.0)) {
            return;
        }

        budgetThisTick--;
        double vx = rocket.getDeltaMovement().x * -0.04 + (random.nextDouble() - 0.5) * 0.01;
        double vy = rocket.getDeltaMovement().y * -0.04 + 0.01;
        double vz = rocket.getDeltaMovement().z * -0.04 + (random.nextDouble() - 0.5) * 0.01;
        level.addParticle(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                rocket.getX(),
                rocket.getY(),
                rocket.getZ(),
                vx,
                vy,
                vz
        );
    }

    public static void trySpawnPowderParticle(FireworkRocketEntity rocket, RandomSource random, int flightLife) {
        if (!TheatricalExtraLightsConfig.isFireworkSmokeEnabled() || budgetThisTick <= 0) {
            return;
        }
        if (!rocket.getPreset().getPattern().usesColoredPowderParticles()) {
            return;
        }
        if (flightLife % TheatricalExtraLightsConfig.getFireworkSmokeSpawnInterval() != 0) {
            return;
        }
        if (!(rocket.level() instanceof ClientLevel level)) {
            return;
        }
        if (!canSpawnNearPlayer(rocket.getX(), rocket.getY(), rocket.getZ(), 96.0)) {
            return;
        }

        budgetThisTick--;
        spawnColoredDust(level, rocket.getLaunchColor(), rocket.getX(), rocket.getY(), rocket.getZ(),
                rocket.getDeltaMovement().x * -0.025 + (random.nextDouble() - 0.5) * 0.004,
                rocket.getDeltaMovement().y * -0.015,
                rocket.getDeltaMovement().z * -0.025 + (random.nextDouble() - 0.5) * 0.004,
                0.55f + random.nextFloat() * 0.35f);
    }

    /** Phase 1 — white launch plume at the tube mouth. */
    public static void trySpawnDaytimeLaunchPlume(FireworkRocketEntity rocket, RandomSource random, int flightLife) {
        if (flightLife > 6 || flightLife % 2 != 0) {
            return;
        }
        if (!(rocket.level() instanceof ClientLevel level)) {
            return;
        }
        if (!canSpawnNearPlayer(rocket.getX(), rocket.getY(), rocket.getZ(), 128.0)) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            if (!consumeBudget()) {
                return;
            }
            double spread = 0.12;
            level.addParticle(
                    ParticleTypes.CLOUD,
                    rocket.getX() + (random.nextDouble() - 0.5) * spread,
                    rocket.getY() + (random.nextDouble() - 0.5) * spread * 0.4,
                    rocket.getZ() + (random.nextDouble() - 0.5) * spread,
                    (random.nextDouble() - 0.5) * 0.04,
                    0.06 + random.nextDouble() * 0.08,
                    (random.nextDouble() - 0.5) * 0.04
            );
        }
        if (consumeBudget()) {
            level.addParticle(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    rocket.getX(),
                    rocket.getY(),
                    rocket.getZ(),
                    (random.nextDouble() - 0.5) * 0.02,
                    0.12,
                    (random.nextDouble() - 0.5) * 0.02
            );
        }
    }

    /** Phase 2 — thick colored smoke trail with turbulence during ascent. */
    public static void trySpawnDaytimeFlightTrail(FireworkRocketEntity rocket, RandomSource random, int flightLife) {
        if (!rocket.getPreset().getPattern().isDaytimePowder()) {
            return;
        }
        if (flightLife % 2 != 0) {
            return;
        }
        if (!(rocket.level() instanceof ClientLevel level)) {
            return;
        }
        if (!canSpawnNearPlayer(rocket.getX(), rocket.getY(), rocket.getZ(), 128.0)) {
            return;
        }

        int[] palette = rocket.getColors();
        int color = palette[Math.floorMod(flightLife, palette.length)];
        for (int i = 0; i < 2; i++) {
            if (!consumeBudget()) {
                return;
            }
            double turb = 0.08;
            spawnDaytimePowderSpark(
                    rocket,
                    color,
                    rocket.getX() + (random.nextDouble() - 0.5) * turb,
                    rocket.getY() + (random.nextDouble() - 0.5) * turb * 0.35,
                    rocket.getZ() + (random.nextDouble() - 0.5) * turb,
                    rocket.getDeltaMovement().x * -0.03 + (random.nextDouble() - 0.5) * 0.03,
                    rocket.getDeltaMovement().y * -0.02 + (random.nextDouble() - 0.5) * 0.02,
                    rocket.getDeltaMovement().z * -0.03 + (random.nextDouble() - 0.5) * 0.03,
                    0.55f + random.nextFloat() * 0.35f,
                    14 + random.nextInt(10)
            );
        }
    }

    /** Phase 3 — Holi / color-burst cloud at apex. */
    public static void spawnDaytimeBurstParticles(
            FireworkRocketEntity rocket,
            RandomSource random,
            int[] palette,
            double originX,
            double originY,
            double originZ,
            int count
    ) {
        if (!(rocket.level() instanceof ClientLevel level)) {
            return;
        }
        if (!canSpawnNearPlayer(originX, originY, originZ, 160.0)) {
            return;
        }

        for (int i = 0; i < count; i++) {
            if (!consumeBudget()) {
                return;
            }
            double theta = random.nextDouble() * Math.PI * 2.0;
            double cosPhi = random.nextDouble() * 0.75 + 0.05;
            double sinPhi = Math.sqrt(Math.max(0.0, 1.0 - cosPhi * cosPhi));
            double speed = 0.03 + random.nextDouble() * 0.10;
            double vx = sinPhi * Math.cos(theta) * speed;
            double vy = cosPhi * speed * 0.5 + 0.008;
            double vz = sinPhi * Math.sin(theta) * speed;
            int color = palette[random.nextInt(palette.length)];
            spawnDaytimePowderSpark(
                    rocket,
                    color,
                    originX + (random.nextDouble() - 0.5) * 0.08,
                    originY + (random.nextDouble() - 0.5) * 0.08,
                    originZ + (random.nextDouble() - 0.5) * 0.08,
                    vx,
                    vy,
                    vz,
                    0.22f + random.nextFloat() * 0.12f,
                    18 + random.nextInt(10)
            );
        }
    }

    /** Phase 3 — ground fan / cone burst (rainbow powder fan). */
    public static void spawnDaytimeFanParticles(
            FireworkRocketEntity rocket,
            RandomSource random,
            int[] palette,
            double originX,
            double originY,
            double originZ,
            double baseYaw,
            float spreadDegrees
    ) {
        if (!(rocket.level() instanceof ClientLevel level)) {
            return;
        }
        if (!canSpawnNearPlayer(originX, originY, originZ, 160.0)) {
            return;
        }

        int streams = 32;
        for (int stream = 0; stream < streams; stream++) {
            if (!consumeBudget()) {
                return;
            }
            float yawOffset = (-spreadDegrees * 0.5f) + (spreadDegrees * stream / Math.max(1, streams - 1));
            double yaw = baseYaw + Math.toRadians(yawOffset);
            double pitch = Math.toRadians(28.0 + random.nextDouble() * 38.0);
            double speed = 0.35 + random.nextDouble() * 0.45;
            double horizontal = Math.cos(pitch) * speed;
            double vx = -Math.sin(yaw) * horizontal;
            double vy = Math.sin(pitch) * speed;
            double vz = Math.cos(yaw) * horizontal;

            int color = palette[Math.floorMod(stream, palette.length)];
            spawnDaytimePowderSpark(
                    rocket,
                    color,
                    originX + (random.nextDouble() - 0.5) * 0.1,
                    originY + (random.nextDouble() - 0.5) * 0.05,
                    originZ + (random.nextDouble() - 0.5) * 0.1,
                    vx,
                    vy,
                    vz,
                    0.75f + random.nextFloat() * 0.35f,
                    45 + random.nextInt(25)
            );
        }
    }

    private static void spawnDaytimePowderSpark(
            FireworkRocketEntity rocket,
            int color,
            double x, double y, double z,
            double vx, double vy, double vz,
            float scale,
            int lifetime
    ) {
        rocket.addSpark(new Spark(
                x, y, z,
                vx, vy, vz,
                color,
                scale,
                lifetime,
                0.010f,
                0.986f,
                false,
                false,
                true
        ));
    }

    private static void spawnColoredDust(
            ClientLevel level,
            int color,
            double x, double y, double z,
            double vx, double vy, double vz,
            float size
    ) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(r, g, b), size);
        level.addParticle(dust, x, y, z, vx, vy, vz);
    }



    //MORTAR HIT SMOKE
    public static void spawnMortarHit(
            FireworkRocketEntity rocket,
            RandomSource random
    ) {
        if (!(rocket.level() instanceof ClientLevel level)) {
            return;
        }

        if (!canSpawnNearPlayer(
                rocket.getX(),
                rocket.getY(),
                rocket.getZ(),
                160.0
        )) {
            return;
        }

        final double x = rocket.getX();
        final double y = rocket.getY();
        final double z = rocket.getZ();

        final double columnHeight = 7.5;
        final double columnRadius = 0.68;

        final double mushroomRadius = 1.6;
        final double mushroomHeight = 1.5;

        final double spreadRandomness = 3.65;
         //
        // COLUMN
       //
        for (int i = 0; i < 40; i++) {

            if (mortarSmokeBudget >= MAX_MORTAR_SMOKE_PARTICLES) {
                break;
            }

            mortarSmokeBudget++;

            double h = random.nextDouble();

            double theta = random.nextDouble() * Math.PI * 2.0;
            double r = Math.sqrt(random.nextDouble()) *
                    columnRadius *
                    (1.0 + (random.nextDouble() - 0.5) * spreadRandomness);

            level.addParticle(
                    ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,

                    x + Math.cos(theta) * r,
                    y + h * columnHeight,
                    z + Math.sin(theta) * r,

                    random.nextGaussian() * 0.023,
                    0.025 + random.nextDouble() * 0.020,
                    random.nextGaussian() * 0.023
            );
        }
        //
        // MUSHROOM
        //
        double topY = y + columnHeight;

        for (int i = 0; i < 60; i++) {

            if (mortarSmokeBudget >= MAX_MORTAR_SMOKE_PARTICLES) {
                break;
            }

            mortarSmokeBudget++;
            double yy = (random.nextDouble() * 2.0 - 1.0) * mushroomHeight;
            double profile = 1.0 - Math.abs(yy) / mushroomHeight;
            profile = Math.max(profile, 0.0);
            profile = Math.pow(profile, 0.45);

            double radius =
                    profile *
                            mushroomRadius *
                            (0.75 + random.nextDouble() * 0.35) *
                            (1.0 + (random.nextDouble() - 0.5) * spreadRandomness);

            double theta = random.nextDouble() * Math.PI * 2.0;

            level.addParticle(
                    ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,

                    x + Math.cos(theta) * radius,
                    topY + yy,
                    z + Math.sin(theta) * radius,

                    random.nextGaussian() * 0.023,
                    0.025 + random.nextDouble() * 0.020,
                    random.nextGaussian() * 0.023
            );
        }
    }









    // MINE SMOKE
    public static void spawnMineSmoke(
            FireworkRocketEntity rocket,
            RandomSource random
    ) {
        if (!(rocket.level() instanceof ClientLevel level)) {
            return;
        }

        if (!canSpawnNearPlayer(
                rocket.getX(),
                rocket.getY(),
                rocket.getZ(),
                160.0
        )) {
            return;
        }

        final double x = rocket.getX();
        final double y = rocket.getY();
        final double z = rocket.getZ();

        final double columnHeight = 8.5;
        final double columnRadius = 0.68;

        final double mushroomRadius = 1.1;
        final double mushroomHeight = 1.1;

        final double spreadRandomness = 7.65;
        //
        // COLUMN
        //
        for (int i = 0; i < 20; i++) {

            if (mortarSmokeBudget >= MAX_MORTAR_SMOKE_PARTICLES) {
                break;
            }

            mortarSmokeBudget++;

            double h = random.nextDouble();

            double theta = random.nextDouble() * Math.PI * 2.0;
            double r = Math.sqrt(random.nextDouble()) *
                    columnRadius *
                    (1.0 + (random.nextDouble() - 0.5) * spreadRandomness);

            level.addParticle(
                    ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,

                    x + Math.cos(theta) * r,
                    y + h * columnHeight,
                    z + Math.sin(theta) * r,

                    random.nextGaussian() * 0.023,
                    0.025 + random.nextDouble() * 0.020,
                    random.nextGaussian() * 0.023
            );
        }
        //
        // MUSHROOM
        //
        double topY = y + columnHeight;

        for (int i = 0; i < 20; i++) {

            if (mortarSmokeBudget >= MAX_MORTAR_SMOKE_PARTICLES) {
                break;
            }

            mortarSmokeBudget++;
            double yy = (random.nextDouble() * 2.0 - 1.0) * mushroomHeight;
            double profile = 1.0 - Math.abs(yy) / mushroomHeight;
            profile = Math.max(profile, 0.0);
            profile = Math.pow(profile, 0.45);

            double radius =
                    profile *
                            mushroomRadius *
                            (0.75 + random.nextDouble() * 0.35) *
                            (1.0 + (random.nextDouble() - 0.5) * spreadRandomness);

            double theta = random.nextDouble() * Math.PI * 2.0;

            level.addParticle(
                    ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,

                    x + Math.cos(theta) * radius,
                    topY + yy,
                    z + Math.sin(theta) * radius,

                    random.nextGaussian() * 0.023,
                    0.025 + random.nextDouble() * 0.020,
                    random.nextGaussian() * 0.023
            );
        }
    }
}
