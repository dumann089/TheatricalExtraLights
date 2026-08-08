package com.github.dumann089.theatricalextralights.firework;

import com.github.dumann089.theatricalextralights.client.firework.FireworkSmokeEffects;
import com.github.dumann089.theatricalextralights.entities.FireworkRocketEntity;
import com.github.dumann089.theatricalextralights.sounds.ModSounds;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Concrete burst patterns. Each one is a fundamentally different algorithm — not
 * shared spawn helper with parameter tweaks. Each populates the rocket's spark list
 * with its own characteristic geometry. Speeds and lifetimes tuned for big visible
 * explosions (50-100 block diameter typical).
 */
public final class BurstPatterns {
    private BurstPatterns() {
    }

    private static final int FLIGHT_LIFETIME = 160;


    private static int paletteAt(int[] palette, int index) {
        return palette[Math.floorMod(index, palette.length)];
    }

    private static void emitFlightTrail(FireworkRocketEntity rocket, RandomSource random, int color, float scale, int lifetime, boolean trail) {
        Vec3 motion = rocket.getDeltaMovement().scale(-0.05);
        double jitter = 0.06;
        double jx = (random.nextDouble() - 0.5) * jitter;
        double jy = (random.nextDouble() - 0.5) * jitter * 0.4;
        double jz = (random.nextDouble() - 0.5) * jitter;
        rocket.addSpark(new Spark(
                rocket.getX() + jx,
                rocket.getY() + jy,
                rocket.getZ() + jz,
                motion.x + jx * 0.4,
                motion.y + jy * 0.4,
                motion.z + jz * 0.4,
                color, scale, lifetime, 0.0f, 0.95f, trail, false));
    }

    /** Thick colored smoke puff left in the flight trail — turbulence + slow fall. */
    private static void emitDaytimeTrailPuff(FireworkRocketEntity rocket, RandomSource random, int color) {
        Vec3 motion = rocket.getDeltaMovement();
        double turb = 0.10;
        for (int i = 0; i < 2; i++) {
            double jx = (random.nextDouble() - 0.5) * turb;
            double jy = (random.nextDouble() - 0.5) * turb * 0.4;
            double jz = (random.nextDouble() - 0.5) * turb;
            rocket.addSpark(new Spark(
                    rocket.getX() + jx,
                    rocket.getY() + jy,
                    rocket.getZ() + jz,
                    motion.x * -0.04 + jx * 0.2 + (random.nextDouble() - 0.5) * 0.025,
                    motion.y * -0.02 + jy * 0.15 + (random.nextDouble() - 0.5) * 0.015,
                    motion.z * -0.04 + jz * 0.2 + (random.nextDouble() - 0.5) * 0.025,
                    color,
                    0.38f + random.nextFloat() * 0.22f,
                    32 + random.nextInt(18),
                    0.008f,
                    0.990f,
                    false,
                    false
            ));
        }
    }

    /** Cone/stream of colored powder — fan or directed burst. */
    private static void emitDaytimePowderStream(
            FireworkRocketEntity rocket,
            RandomSource random,
            double x, double y, double z,
            double vx, double vy, double vz,
            int color,
            int count
    ) {
        for (int i = 0; i < count; i++) {
            double spread = 0.04;
            rocket.addSpark(new Spark(
                    x + (random.nextDouble() - 0.5) * spread,
                    y + (random.nextDouble() - 0.5) * spread * 0.35,
                    z + (random.nextDouble() - 0.5) * spread,
                    vx + (random.nextDouble() - 0.5) * 0.025,
                    vy + (random.nextDouble() - 0.5) * 0.018,
                    vz + (random.nextDouble() - 0.5) * 0.025,
                    color,
                    0.42f + random.nextFloat() * 0.28f,
                    100 + random.nextInt(80),
                    0.010f,
                    0.986f,
                    false,
                    false
            ));
        }
    }

    /** Tiny color pop at apex — subtle, not a full Holi cloud. */
    private static void emitDaytimeColorBurst(FireworkRocketEntity rocket, RandomSource random) {
        int[] palette = rocket.getColors();
        double cx = rocket.getX();
        double cy = rocket.getY();
        double cz = rocket.getZ();
        int count = 6 + random.nextInt(6);
        for (int i = 0; i < count; i++) {
            double theta = random.nextDouble() * Math.PI * 2.0;
            double cosPhi = random.nextDouble() * 0.55 + 0.2;
            double sinPhi = Math.sqrt(Math.max(0.0, 1.0 - cosPhi * cosPhi));
            double speed = 0.04 + random.nextDouble() * 0.10;
            double vx = sinPhi * Math.cos(theta) * speed;
            double vy = cosPhi * speed * 0.45 + 0.01;
            double vz = sinPhi * Math.sin(theta) * speed;
            int color = paletteAt(palette, i);
            rocket.addSpark(new Spark(
                    cx + (random.nextDouble() - 0.5) * 0.08,
                    cy + (random.nextDouble() - 0.5) * 0.08,
                    cz + (random.nextDouble() - 0.5) * 0.08,
                    vx,
                    vy,
                    vz,
                    color,
                    0.06f + random.nextFloat() * 0.05f,
                    22 + random.nextInt(16),
                    0.009f,
                    0.990f,
                    false,
                    false
            ));
        }
        FireworkSmokeEffects.spawnDaytimeBurstParticles(rocket, random, palette, cx, cy, cz, 3);
    }

    private static double launchYaw(FireworkRocketEntity rocket) {
        Vec3 motion = rocket.getDeltaMovement();
        if (motion.horizontalDistanceSqr() > 1.0E-4) {
            return Math.atan2(-motion.x, motion.z);
        }
        if (rocket.level() != null) {
            BlockState state = rocket.level().getBlockState(rocket.getLauncherPos());
            if (state.hasProperty(BaseLightBlock.FACING)) {
                Direction launchFacing = state.getValue(BaseLightBlock.FACING).getClockWise();
                return Math.toRadians(launchFacing.toYRot());
            }
        }
        return 0.0;
    }

    private static void emitOrientedPowderFan(FireworkRocketEntity rocket, RandomSource random) {
        int[] palette = rocket.getColors();
        double baseYaw = launchYaw(rocket);
        double cx = rocket.getX();
        double cy = rocket.getY();
        double cz = rocket.getZ();
        int streams = 44;
        float spread = 155.0f;
        for (int stream = 0; stream < streams; stream++) {
            float yawOffset = (-spread * 0.5f) + (spread * stream / Math.max(1, streams - 1));
            double yaw = baseYaw + Math.toRadians(yawOffset);
            double pitch = Math.toRadians(30.0 + random.nextDouble() * 50.0);
            double speed = 0.55 + random.nextDouble() * 0.55;
            double horizontal = Math.cos(pitch) * speed;
            double vx = -Math.sin(yaw) * horizontal;
            double vy = Math.sin(pitch) * speed;
            double vz = Math.cos(yaw) * horizontal;
            int color = paletteAt(palette, stream);
            emitDaytimePowderStream(rocket, random, cx, cy, cz, vx, vy, vz, color, 3);
        }
        FireworkSmokeEffects.spawnDaytimeFanParticles(rocket, random, palette, cx, cy, cz, baseYaw, spread);
    }

    /**
     * Uniform expanding sphere. Brief, clean, no gravity bias. The benchmark "boule"
     * burst — sparks shoot outward in all directions and fade in place.
     */
    public static class Peony extends BurstPattern {
        @Override public boolean isBurst() { return true; }
        @Override public int getBurstDuration() { return 64; }
        @Override public int getFlightLifetime() { return FLIGHT_LIFETIME; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            emitFlightTrail(rocket, random, rocket.getLaunchColor(), 0.42f, 10, false);
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            int color = rocket.getLaunchColor();
            int count = 260;
            for (int i = 0; i < count; i++) {
                double theta = random.nextDouble() * Math.PI * 2.0;
                double cosPhi = 2.0 * random.nextDouble() - 1.0;
                double sinPhi = Math.sqrt(Math.max(0.0, 1.0 - cosPhi * cosPhi));
                double speed = 1.6 + random.nextDouble() * 0.55;
                double vx = sinPhi * Math.cos(theta) * speed;
                double vy = cosPhi * speed;
                double vz = sinPhi * Math.sin(theta) * speed;
                rocket.addSpark(new Spark(rocket.getX(), rocket.getY(), rocket.getZ(),
                        vx, vy, vz, color, 0.45f, 60, 0.0025f, 0.992f, false, false));
            }
            spawnEmbers(rocket, random, 110);
        }
    }

    /**
     * Sphere with immediate heavy droop. Sparks emit radially then fall hard, drawing
     * long downward streaks like willow branches.
     */
    public static class Willow extends BurstPattern {
        @Override public boolean isBurst() { return true; }
        @Override public boolean hasCrackleSound() { return true; }
        @Override public int getBurstDuration() { return 110; }
        @Override public int getFlightLifetime() { return FLIGHT_LIFETIME; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            emitFlightTrail(rocket, random, rocket.getLaunchColor(), 0.40f, 10, false);
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            int color = rocket.getLaunchColor();
            int count = 280;
            for (int i = 0; i < count; i++) {
                double theta = random.nextDouble() * Math.PI * 2.0;
                double phi = Math.acos(2.0 * random.nextDouble() - 1.0);
                double speed = 0.85 + random.nextDouble() * 0.30;
                double vx = Math.sin(phi) * Math.cos(theta) * speed;
                double vy = Math.cos(phi) * speed + 0.10;
                double vz = Math.sin(phi) * Math.sin(theta) * speed;
                rocket.addSpark(new Spark(rocket.getX(), rocket.getY(), rocket.getZ(),
                        vx, vy, vz, color, 0.40f, 105, 0.015f, 0.992f, true, false));
            }
            spawnEmbers(rocket, random, 150);
        }
    }

    /**
     * Chaotic flickering scatter. Very wide speed range, short lifetime, every spark
     * strobes between bright and dim.
     */
    public static class Strobe extends BurstPattern {
        @Override public boolean isBurst() { return true; }
        @Override public boolean hasCrackleSound() { return true; }
        @Override public int getBurstDuration() { return 40; }
        @Override public int getFlightLifetime() { return FLIGHT_LIFETIME; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            emitFlightTrail(rocket, random, rocket.getLaunchColor(), 0.42f, 10, false);
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            int color = rocket.getLaunchColor();
            int count = 260;
            for (int i = 0; i < count; i++) {
                double theta = random.nextDouble() * Math.PI * 2.0;
                double cosPhi = 2.0 * random.nextDouble() - 1.0;
                double sinPhi = Math.sqrt(Math.max(0.0, 1.0 - cosPhi * cosPhi));
                double speed = 0.90 + random.nextDouble() * 1.95;
                double vx = sinPhi * Math.cos(theta) * speed;
                double vy = cosPhi * speed;
                double vz = sinPhi * Math.sin(theta) * speed;
                rocket.addSpark(new Spark(rocket.getX(), rocket.getY(), rocket.getZ(),
                        vx, vy, vz, color, 0.45f, 32 + random.nextInt(10), 0.001f, 0.992f, false, true));
            }
            spawnEmbers(rocket, random, 110);
        }
    }

    /**
     * Silent ascent (no trail), then a fixed aerial strobe for four seconds at apex.
     */
    public static class AerialStrobe extends BurstPattern {
        private static final int BURST_DURATION = 80;

        @Override public boolean isBurst() { return true; }
        @Override public boolean isAerialStrobe() { return true; }
        @Override public boolean hasInvisibleFlight() { return true; }
        @Override public int getBurstDuration() { return BURST_DURATION; }
        @Override public int getFlightLifetime() { return FLIGHT_LIFETIME; }
        @Override public int getFlightLuminance() { return 0; }
        @Override public float getFlightLightSpread() { return 0.0f; }
        @Override public float getFlightHaloInnerSize() { return 0.0f; }
        @Override public float getFlightHaloOuterSize() { return 0.0f; }

        @Override
        public int getBurstLuminance(int tickIndex) {
            if (tickIndex >= BURST_DURATION) {
                return 0;
            }
            return tickIndex % 4 < 2 ? 15 : 0;
        }

        @Override
        public float getBurstLightSpread(int tickIndex) {
            if (tickIndex >= BURST_DURATION) {
                return 0.0f;
            }
            return tickIndex % 4 < 2 ? 60.0f : 4.0f;
        }

        @Override
        public float getBurstHaloInnerSize(int tickIndex) {
            if (tickIndex >= BURST_DURATION) {
                return 0.0f;
            }
            return tickIndex % 4 < 2 ? 14.0f : 4.0f;
        }

        @Override
        public float getBurstHaloOuterSize(int tickIndex) {
            if (tickIndex >= BURST_DURATION) {
                return 0.0f;
            }
            return tickIndex % 4 < 2 ? 22.0f : 6.0f;
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            spawnAerialStrobeSparks(rocket, random, BURST_DURATION);
        }

        @Override
        public void onBurstTick(FireworkRocketEntity rocket, RandomSource random, int tickIndex) {
            if (tickIndex > 0 && tickIndex % 6 == 0) {
                spawnAerialStrobeSparks(rocket, random, BURST_DURATION - tickIndex);
            }
        }

        private static void spawnAerialStrobeSparks(FireworkRocketEntity rocket, RandomSource random, int remainingTicks) {
            if (remainingTicks <= 0) {
                return;
            }
            int color = rocket.getLaunchColor();
            int count = 4 + random.nextInt(3);
            double radius = 0.35;
            for (int i = 0; i < count; i++) {
                double jx = (random.nextDouble() - 0.5) * radius;
                double jy = (random.nextDouble() - 0.5) * radius * 0.5;
                double jz = (random.nextDouble() - 0.5) * radius;
                rocket.addSpark(new Spark(
                        rocket.getX() + jx,
                        rocket.getY() + jy,
                        rocket.getZ() + jz,
                        0.0,
                        0.0,
                        0.0,
                        color,
                        0.55f + random.nextFloat() * 0.25f,
                        Math.min(remainingTicks, 10 + random.nextInt(8)),
                        0.0f,
                        1.0f,
                        false,
                        true
                ));
            }
        }
    }

    /**
     * Three concentric color shells expanding at different speeds. Each shell is a
     * full uniform sphere in its own color from the preset palette, so three rings
     * are visible separating outward.
     */
    public static class Multicolor extends BurstPattern {
        @Override public boolean isBurst() { return true; }
        @Override public int getBurstDuration() { return 70; }
        @Override public int getFlightLifetime() { return FLIGHT_LIFETIME; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            emitFlightTrail(rocket, random, rocket.getLaunchColor(), 0.42f, 10, false);
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            int[] palette = rocket.getColors();
            double[] shellSpeeds = {0.85, 1.45, 2.05};
            int[] shellCounts = {100, 115, 130};
            for (int shell = 0; shell < shellSpeeds.length; shell++) {
                int color = paletteAt(palette, shell);
                double baseSpeed = shellSpeeds[shell];
                int count = shellCounts[shell];
                for (int i = 0; i < count; i++) {
                    double theta = random.nextDouble() * Math.PI * 2.0;
                    double cosPhi = 2.0 * random.nextDouble() - 1.0;
                    double sinPhi = Math.sqrt(Math.max(0.0, 1.0 - cosPhi * cosPhi));
                    double speed = baseSpeed + random.nextDouble() * 0.10;
                    double vx = sinPhi * Math.cos(theta) * speed;
                    double vy = cosPhi * speed;
                    double vz = sinPhi * Math.sin(theta) * speed;
                    rocket.addSpark(new Spark(rocket.getX(), rocket.getY(), rocket.getZ(),
                            vx, vy, vz, color, 0.42f, 64, 0.005f, 0.992f, false, false));
                }
            }
            spawnEmbers(rocket, random, 130);
        }
    }

    /**
     * Sphere of sparks each leaving a long visible streak (long lifetime, smooth alpha
     * curve from trail flag). Looks like a flower made of streaks rather than dots.
     */
    public static class Chrysanthemum extends BurstPattern {
        @Override public boolean isBurst() { return true; }
        @Override public boolean hasCrackleSound() { return true; }
        @Override public int getBurstDuration() { return 90; }
        @Override public int getFlightLifetime() { return FLIGHT_LIFETIME; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            emitFlightTrail(rocket, random, rocket.getLaunchColor(), 0.42f, 10, false);
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            int[] palette = rocket.getColors();
            int count = 280;
            for (int i = 0; i < count; i++) {
                double theta = random.nextDouble() * Math.PI * 2.0;
                double cosPhi = 2.0 * random.nextDouble() - 1.0;
                double sinPhi = Math.sqrt(Math.max(0.0, 1.0 - cosPhi * cosPhi));
                double speed = 1.50 + random.nextDouble() * 0.55;
                double vx = sinPhi * Math.cos(theta) * speed;
                double vy = cosPhi * speed + 0.08;
                double vz = sinPhi * Math.sin(theta) * speed;
                int color = paletteAt(palette, i);
                rocket.addSpark(new Spark(rocket.getX(), rocket.getY(), rocket.getZ(),
                        vx, vy, vz, color, 0.45f, 85, 0.009f, 0.992f, true, false));
            }
            spawnEmbers(rocket, random, 170);
        }
    }

    /**
     * Two-phase silhouette: a thin vertical trunk shoots up at burst start, then over
     * the next several ticks the apex blooms with falling fronds. The result is a
     * recognisable palm-tree shape.
     */
    public static class Palm extends BurstPattern {
        private static final double APEX_Y_OFFSET = 14.0;

        @Override public boolean isBurst() { return true; }
        @Override public int getBurstDuration() { return 110; }
        @Override public int getFlightLifetime() { return FLIGHT_LIFETIME; }
        @Override public float getLaunchSpeedMultiplier() { return 0.90f; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            emitFlightTrail(rocket, random, rocket.getLaunchColor(), 0.46f, 12, true);
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            int color = rocket.getLaunchColor();
            int trunkCount = 50;
            for (int i = 0; i < trunkCount; i++) {
                double sx = (random.nextDouble() - 0.5) * 0.06;
                double sz = (random.nextDouble() - 0.5) * 0.06;
                double sy = 1.30 + random.nextDouble() * 0.55;
                rocket.addSpark(new Spark(rocket.getX(), rocket.getY(), rocket.getZ(),
                        sx, sy, sz, color, 0.50f, 55, 0.012f, 0.993f, true, false));
            }
            spawnEmbers(rocket, random, 60);
        }

        @Override
        public void onBurstTick(FireworkRocketEntity rocket, RandomSource random, int tickIndex) {
            if (tickIndex == 14 || tickIndex == 22 || tickIndex == 30 || tickIndex == 38) {
                int color = rocket.getLaunchColor();
                int frondCount = 50;
                double apexX = rocket.getX();
                double apexY = rocket.getY() + APEX_Y_OFFSET;
                double apexZ = rocket.getZ();
                for (int i = 0; i < frondCount; i++) {
                    double theta = random.nextDouble() * Math.PI * 2.0;
                    double horiz = 0.55 + random.nextDouble() * 0.30;
                    double drop = -0.65 - random.nextDouble() * 0.30;
                    Vec3 dir = new Vec3(Math.cos(theta) * horiz, drop, Math.sin(theta) * horiz);
                    Vec3 motion = dir.normalize().scale(1.55 + random.nextDouble() * 0.45);
                    rocket.addSpark(new Spark(apexX, apexY, apexZ,
                            motion.x, motion.y, motion.z, color, 0.45f, 80, 0.015f, 0.992f, true, false));
                }
            }
        }
    }

    /**
     * Tight downward cone — sparks only travel below the burst point in near-parallel
     * lines. Long lifetime, heavy falloff. Looks like a long horse tail of falling
     * silver streaks.
     */
    public static class Horsetail extends BurstPattern {
        @Override public boolean isBurst() { return true; }
        @Override public int getBurstDuration() { return 130; }
        @Override public int getFlightLifetime() { return FLIGHT_LIFETIME; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            emitFlightTrail(rocket, random, rocket.getLaunchColor(), 0.44f, 11, true);
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            int color = rocket.getLaunchColor();
            int count = 260;
            for (int i = 0; i < count; i++) {
                double theta = random.nextDouble() * Math.PI * 2.0;
                double horiz = 0.18 + random.nextDouble() * 0.25;
                double down = -(0.85 + random.nextDouble() * 0.40);
                Vec3 dir = new Vec3(Math.cos(theta) * horiz, down, Math.sin(theta) * horiz);
                Vec3 motion = dir.normalize().scale(1.30 + random.nextDouble() * 0.40);
                rocket.addSpark(new Spark(rocket.getX(), rocket.getY(), rocket.getZ(),
                        motion.x, motion.y, motion.z, color, 0.42f, 110, 0.010f, 0.996f, true, false));
            }
            spawnEmbers(rocket, random, 100);
        }
    }

    /**
     * Flat horizontal ring expansion — every spark travels on a plane perpendicular
     * to the vertical axis. Tight Y range so the ring stays clean and visible.
     */
    public static class Ring extends BurstPattern {
        @Override public boolean isBurst() { return true; }
        @Override public int getBurstDuration() { return 50; }
        @Override public int getFlightLifetime() { return FLIGHT_LIFETIME; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            emitFlightTrail(rocket, random, rocket.getLaunchColor(), 0.42f, 10, false);
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            int color = rocket.getLaunchColor();
            int count = 220;
            double speed = 1.85;
            for (int i = 0; i < count; i++) {
                double theta = random.nextDouble() * Math.PI * 2.0;
                double yJitter = (random.nextDouble() - 0.5) * 0.05;
                double vx = Math.cos(theta) * speed;
                double vy = yJitter;
                double vz = Math.sin(theta) * speed;
                rocket.addSpark(new Spark(rocket.getX(), rocket.getY(), rocket.getZ(),
                        vx, vy, vz, color, 0.45f, 48, 0.005f, 0.993f, false, false));
            }
            spawnEmbers(rocket, random, 70);
        }
    }

    /**
     * Bell-shape comet that flies high, lingers, then explodes into a dense gold
     * sphere. Combines pure comet flight (long bright trail) with a final burst.
     */
    public static class BellComet extends BurstPattern {
        @Override public boolean isBurst() { return true; }
        @Override public boolean hasCrackleSound() { return true; }
        @Override public int getBurstDuration() { return 95; }
        @Override public int getFlightLifetime() { return FLIGHT_LIFETIME; }
        @Override public double getGravity() { return 0.018; }
        @Override public double getDrag() { return 0.99; }
        @Override public float getLaunchSpeedMultiplier() { return 1.05f; }
        @Override public float getFlightLightSpread() { return 38.0f; }
        @Override public float getFlightHaloInnerSize() { return 2.5f; }
        @Override public float getFlightHaloOuterSize() { return 4.2f; }
        @Override public boolean spawnsFlightSmoke() { return true; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            int color = rocket.getLaunchColor();
            for (int i = 0; i < 2; i++) {
                emitFlightTrail(rocket, random, color, 0.55f, 14, true);
            }
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            int color = rocket.getLaunchColor();
            int count = 320;
            for (int i = 0; i < count; i++) {
                double theta = random.nextDouble() * Math.PI * 2.0;
                double cosPhi = 2.0 * random.nextDouble() - 1.0;
                double sinPhi = Math.sqrt(Math.max(0.0, 1.0 - cosPhi * cosPhi));
                double speed = 1.20 + random.nextDouble() * 1.20;
                double vx = sinPhi * Math.cos(theta) * speed;
                double vy = cosPhi * speed + 0.10;
                double vz = sinPhi * Math.sin(theta) * speed;
                rocket.addSpark(new Spark(rocket.getX(), rocket.getY(), rocket.getZ(),
                        vx, vy, vz, color, 0.55f, 90, 0.011f, 0.992f, true, false));
            }
            spawnEmbers(rocket, random, 140);
        }
    }

    /**
     * Spirals upward — emits trail sparks in a rotating circle around the rocket so
     * the flight trail forms a corkscrew/helix. Standard sphere burst at apex.
     */
    public static class Spinner extends BurstPattern {
        @Override public boolean isBurst() { return true; }
        @Override public int getBurstDuration() { return 60; }
        @Override public int getFlightLifetime() { return FLIGHT_LIFETIME; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            int color = rocket.getLaunchColor();
            double baseAngle = rocket.tickCount * 0.55;
            double radius = 0.55;
            for (int i = 0; i < 3; i++) {
                double a = baseAngle + i * (Math.PI * 2.0 / 3.0);
                double dx = Math.cos(a) * radius;
                double dz = Math.sin(a) * radius;
                rocket.addSpark(new Spark(rocket.getX() + dx, rocket.getY(), rocket.getZ() + dz,
                        dx * 0.18, -0.03, dz * 0.18, color, 0.36f, 18, 0.003f, 0.96f, true, false));
            }
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            int color = rocket.getLaunchColor();
            int count = 220;
            for (int i = 0; i < count; i++) {
                double theta = random.nextDouble() * Math.PI * 2.0;
                double cosPhi = 2.0 * random.nextDouble() - 1.0;
                double sinPhi = Math.sqrt(Math.max(0.0, 1.0 - cosPhi * cosPhi));
                double speed = 1.4 + random.nextDouble() * 0.5;
                double vx = sinPhi * Math.cos(theta) * speed;
                double vy = cosPhi * speed;
                double vz = sinPhi * Math.sin(theta) * speed;
                rocket.addSpark(new Spark(rocket.getX(), rocket.getY(), rocket.getZ(),
                        vx, vy, vz, color, 0.42f, 50, 0.004f, 0.992f, false, false));
            }
            spawnEmbers(rocket, random, 90);
        }
    }

    /**
     * Crossette: each main spark explodes again into a small cross. The main burst
     * spawns ~30 sparks outward, then at tick 12 every still-living main spark
     * detonates into 6 perpendicular sub-sparks (±X, ±Y, ±Z) creating the classic
     * crisscross-pattern signature of a real crossette.
     */
    public static class Crossette extends BurstPattern {
        private static final float MAIN_SPARK_SCALE = 0.55f;

        @Override public boolean isBurst() { return true; }
        @Override public int getBurstDuration() { return 70; }
        @Override public int getFlightLifetime() { return FLIGHT_LIFETIME; }


        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            emitFlightTrail(rocket, random, rocket.getLaunchColor(), 0.42f, 10, false);
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            int color = rocket.getLaunchColor();
            int count = 30;
            for (int i = 0; i < count; i++) {
                double theta = random.nextDouble() * Math.PI * 2.0;
                double cosPhi = 2.0 * random.nextDouble() - 1.0;
                double sinPhi = Math.sqrt(Math.max(0.0, 1.0 - cosPhi * cosPhi));
                double speed = 0.95 + random.nextDouble() * 0.30;
                double vx = sinPhi * Math.cos(theta) * speed;
                double vy = cosPhi * speed;
                double vz = sinPhi * Math.sin(theta) * speed;
                rocket.addSpark(new Spark(rocket.getX(), rocket.getY(), rocket.getZ(),
                        vx, vy, vz, color, MAIN_SPARK_SCALE, 60, 0.005f, 0.992f, true, false));
            }
            spawnEmbers(rocket, random, 50);
        }

        @Override
        public void onBurstTick(FireworkRocketEntity rocket, RandomSource random, int tickIndex) {
            if (tickIndex != 12) {
                return;
            }
            int color = rocket.getLaunchColor();
            double subSpeed = 0.55;
            double[][] dirs = {
                    { subSpeed, 0, 0}, {-subSpeed, 0, 0},
                    {0,  subSpeed, 0}, {0, -subSpeed, 0},
                    {0, 0,  subSpeed}, {0, 0, -subSpeed}
            };
            java.util.List<Spark> sparks = rocket.getSparks();
            int snapshotSize = sparks.size();
            for (int i = 0; i < snapshotSize; i++) {
                Spark main = sparks.get(i);
                if (main.scale < MAIN_SPARK_SCALE - 0.01f || main.isDead() || main.age > 13) {
                    continue;
                }
                for (double[] d : dirs) {
                    double jitter = 0.05;
                    double vx = d[0] + (random.nextDouble() - 0.5) * jitter;
                    double vy = d[1] + (random.nextDouble() - 0.5) * jitter;
                    double vz = d[2] + (random.nextDouble() - 0.5) * jitter;
                    rocket.addSpark(new Spark(main.x, main.y, main.z, vx, vy, vz,
                            color, 0.34f, 28, 0.004f, 0.992f, false, false));
                }
            }
        }
    }

    /**
     * Mine: a ground-level upward fountain. The launcher fires no rocket — instead
     * the burst happens immediately at the launcher position, throwing many sparks
     * into a tall narrow cone of fire above the device. Used for stage mine effects.
     */
    public static class Mine extends BurstPattern {
        @Override public boolean isBurst() { return true; }
        @Override public boolean hasCrackleSound() { return true; }
        @Override public int getBurstDuration() { return 30; }
        @Override public int getFlightLifetime() { return 1; }
        @Override public float getLaunchSpeedMultiplier() { return 0.0f; }

        @Override
        public boolean isTriggerShot() {
            return true;
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            int[] palette = rocket.getColors();
            double floorY = rocket.getY();
            int count = 300;

            for (int i = 0; i < count; i++) {

                double spawnAngle = random.nextDouble() * Math.PI * 2.5;
                double spawnRadius = random.nextDouble() * 0.2;

                double x = rocket.getX() + Math.cos(spawnAngle) * spawnRadius;
                double z = rocket.getZ() + Math.sin(spawnAngle) * spawnRadius;

                double spread = Math.pow(random.nextDouble(), 2.5);
                double coneAngle = 0.1 + spread * 0.15;

                double azimuth = random.nextDouble() * Math.PI * 2.0;

                double speed;
                float r = random.nextFloat();

                if (r < 0.12f) {
                    speed = 1.35 + random.nextDouble() * 0.35;
                } else if (r < 0.65f) {
                    speed = 1.15 + random.nextDouble() * 0.35;
                } else {
                    speed = 0.95 + random.nextDouble() * 0.35;
                }

                double vy = Math.cos(coneAngle) * speed;
                double horiz = Math.sin(coneAngle) * speed;

                double vx = Math.cos(azimuth) * horiz;
                double vz = Math.sin(azimuth) * horiz;

                int color = paletteAt(palette, i);

                rocket.addSpark(new Spark(
                        x,
                        floorY,
                        z,
                        vx,
                        vy,
                        vz,
                        color,
                        0.15f + random.nextFloat() * 0.12f,
                        (int)(10 + speed * 4 + random.nextInt(4)),
                        0.04f + random.nextFloat() * 0.004f,
                        0.986f + random.nextFloat() * 0.006f,
                        true,
                        false,
                        false,
                        floorY
                ));
            }
            for (int i = 0; i < 35; i++) {
                double theta = random.nextDouble() * Math.PI * 2.0;
                double spread = 0.12 + random.nextDouble() * 0.18;
                double speed = 0.25 + random.nextDouble() * 0.40;
                double vx = Math.cos(theta) * spread * speed;
                double vz = Math.sin(theta) * spread * speed;
                double vy = 0.40 + random.nextDouble() * 0.50;
                int color = paletteAt(palette, i);
                rocket.addSpark(new Spark(
                        rocket.getX(), floorY, rocket.getZ(),
                        vx, vy, vz,
                        color,
                        0.18f + random.nextFloat() * 0.10f,
                        10 + random.nextInt(5),
                        0.045f,
                        0.996f,
                        false,
                        false,
                        false,
                        floorY
                ));
            }
            FireworkSmokeEffects.spawnMineSmoke(rocket, random);
        }
    }
    // Silver Jet
    public static class SilverJet extends BurstPattern {
        private static final double HEIGHT = 9.5;

        private static final double START_SIZE = 0.05;
        private static final double MIDDLE_SIZE = 1.95;
        private static final double END_SIZE = 0.85;

        private static final double START_HEIGHT = 0.40;
        private static final double END_HEIGHT = 0.40;

        private static final double SPREAD = 1.0;

        @Override public boolean isBurst() { return true; }
        @Override public boolean hasCrackleSound() { return true; }
        @Override public int getBurstDuration() { return 85; }
        @Override public int getFlightLifetime() { return 1; }
        @Override public float getLaunchSpeedMultiplier() { return 0.0f; }
        @Override public float getFlightLightSpread() { return 75.0f; }


        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
        }

        @Override
        public boolean isTriggerShot() {
            return true;
        }

        @Override
        public void onBurstTick(FireworkRocketEntity rocket, RandomSource random, int tick) {
            if (tick >= 10) {
                return;
            }
            spawnSilverJet(rocket, random, 135, tick);
        }

        private void spawnSilverJet(
                FireworkRocketEntity rocket,
                RandomSource random,
                int count,
                int tick
        ) {
            int[] palette = rocket.getColors();

            double baseX = rocket.getX();
            double baseY = rocket.getY();
            double baseZ = rocket.getZ();

            for (int i = 0; i < count; i++) {
                double h = random.nextDouble();
                double width;

                if (h < START_HEIGHT) {
                    double t = h / START_HEIGHT;
                    width = START_SIZE +
                            (MIDDLE_SIZE - START_SIZE) * t;
                }
                else if (h < 1.0 - END_HEIGHT) {
                    width = MIDDLE_SIZE;
                }
                else {
                    double t = (h - (1.0 - END_HEIGHT)) / END_HEIGHT;
                    width = MIDDLE_SIZE +
                            (END_SIZE - MIDDLE_SIZE) * t;
                }
                width *= 0.85 + random.nextDouble() * 0.30;

                double theta = random.nextDouble() * Math.PI * 2.0;
                double radius =
                        Math.pow(random.nextDouble(), 0.45)
                                * width
                                * SPREAD;
                radius += random.nextGaussian() * 0.04;
                double x =
                        baseX +
                                Math.cos(theta) * radius;
                double z =
                        baseZ +
                                Math.sin(theta) * radius;
                double y =
                        baseY +
                                h * HEIGHT;
                double vx =
                        Math.cos(theta) * 0.025
                                + random.nextGaussian() * 0.02;
                double vz =
                        Math.sin(theta) * 0.025
                                + random.nextGaussian() * 0.02;
                double vy =
                        0.25 +
                                random.nextDouble() * 0.35;
                rocket.addSpark(new Spark(
                        x,
                        y,
                        z,
                        vx,
                        vy,
                        vz,
                        paletteAt(
                                palette,
                                tick * count + i
                        ),
                        0.09f +
                                random.nextFloat() * 0.08f,

                        15 +
                                random.nextInt(12),
                        0.035f,
                        0.985f,
                        true,
                        false
                ));
            }
        }
    }





    // FLAME PROJECTOR
    public static class FlameProjector extends BurstPattern {

        private static final double FLAME_HEIGHT = 4.5;

        private static final double EMITTER_RADIUS = 0.1;

        private static final double TURBULENCE = 0.02;

        private static final double VERTICAL_SPEED = 0.75;

        private static final int MAX_PARTICLES = 40;

        private static final int DURATION = 85;

        private static final double FADE_IN = 0.15;

        private static final double FADE_OUT = 0.50;

        private static final double MIN_EMITTER = 0.0;

        private static final float MIN_SIZE = 0.05f;
        private static final float MAX_SIZE = 0.45f;

        private static final double PARTICLE_CURVE = 2.5;

        @Override
        public boolean isBurst() {
            return true;
        }

        @Override
        public boolean hasCrackleSound() {
            return true;
        }

        @Override
        public int getFlightLifetime() {
            return 1;
        }

        @Override
        public float getLaunchSpeedMultiplier() {
            return 0.0f;
        }

        @Override
        public float getFlightLightSpread() {
            return 75.0f;
        }

        @Override
        public int getFlightLuminance() {
            return 9;
        }

        @Override
        public int getBurstDuration() {
            return DURATION;
        }

        @Override
        public int getBurstLuminance(int tick) {

            double progress = tick / (double) DURATION;

            double fade;

            if (progress < FADE_IN) {
                fade = progress / FADE_IN;
            } else if (progress > FADE_OUT) {
                fade = 1.0 - ((progress - FADE_OUT) / (1.0 - FADE_OUT));
            } else {
                fade = 1.0;
            }

            fade = Math.max(0.0, Math.min(1.0, fade));

            return Math.max(0, (int) Math.round(9 * fade));
        }

        @Override
        public float getBurstLightSpread(int tick) {

            double progress = tick / (double) DURATION;

            double fade;

            if (progress < FADE_IN) {
                fade = progress / FADE_IN;
            } else if (progress > FADE_OUT) {
                fade = 1.0 - ((progress - FADE_OUT) / (1.0 - FADE_OUT));
            } else {
                fade = 1.0;
            }

            fade = Math.max(0.0, Math.min(1.0, fade));

            return (float) (25.0 * fade);
        }

        @Override
        public boolean isTriggerShot() {
            return true;
        }

        @Override
        public void onBurstTick(FireworkRocketEntity rocket, RandomSource random, int tick) {
            spawnFlameProjector(rocket, random, tick);
        }

        private void spawnFlameProjector(
                FireworkRocketEntity rocket,
                RandomSource random,
                int tick
        ) {

            int[] palette = rocket.getColors();

            double baseX = rocket.getX();
            double baseY = rocket.getY();
            double baseZ = rocket.getZ();

            double progress = tick / (double) DURATION;

            double fade;

            // Fade IN
            if (progress < FADE_IN) {
                fade = progress / FADE_IN;
            }
            // Fade OUT
            else if (progress > FADE_OUT) {
                fade = 1.0 - ((progress - FADE_OUT) / (1.0 - FADE_OUT));
            }
            else {
                fade = 1.0;
            }

            fade = Math.max(0.0, Math.min(1.0, fade));

            if (fade <= 0.01) {
                return;
            }

            int count = (int) (MAX_PARTICLES * Math.pow(fade, PARTICLE_CURVE));

            if (count <= 0) {
                return;
            }

            double flameHeight = FLAME_HEIGHT * (0.30 + 0.70 * fade);

            double emitterRadius = EMITTER_RADIUS * fade * fade;

            double turbulence = TURBULENCE * (0.35 + 0.65 * fade);

            double verticalSpeed = VERTICAL_SPEED * (0.40 + 0.60 * fade);

            for (int i = 0; i < count; i++) {

                double theta = random.nextDouble() * Math.PI * 2.0;
                double r = Math.sqrt(random.nextDouble()) * emitterRadius;

                double x = baseX + Math.cos(theta) * r;
                double y = baseY + random.nextDouble() * 0.12;
                double z = baseZ + Math.sin(theta) * r;

                double vx = random.nextGaussian() * turbulence;
                double vz = random.nextGaussian() * turbulence;

                double vy = verticalSpeed + random.nextDouble() * (flameHeight * 0.025);

                float size =
                        (MIN_SIZE + random.nextFloat() * (MAX_SIZE - MIN_SIZE))
                                * (0.35f + 0.65f * (float) fade);

                rocket.addSpark(new Spark(
                        x,
                        y,
                        z,
                        vx,
                        vy,
                        vz,
                        paletteAt(palette, tick * MAX_PARTICLES + i),
                        size,
                        11 + random.nextInt(5),
                        0.050f,
                        0.975f,
                        true,
                        false,
                        false,
                        baseY
                ));
            }
        }
    }

    // MORTAR HIT
    public static class MortarHit extends BurstPattern {
        @Override
        public boolean isBurst() {
            return true;
        }
        @Override
        public boolean isTriggerShot() {
            return true;
        }
        @Override
        public boolean hasCrackleSound() {
            return false;
        }
        @Override
        public int getBurstDuration() {
            return 6;
        }

        @Override
        public int getFlightLifetime() {
            return 1;
        }
        @Override public float
        getFlightLightSpread() { return 255.0f; }

        @Override
        public float getLaunchSpeedMultiplier() {
            return 0.0f;
        }

        @Override
        public int getBurstLuminance(int tick) {
            final int LIGHT_DURATION = 10;
            if (tick >= LIGHT_DURATION) {
                return 0;
            }
            double t = tick / (double) LIGHT_DURATION;
            double fade = Math.pow(1.0 - t, 4.0);
            return Math.max(0, (int) Math.round(18 * fade));
        }

        @Override
        public float getBurstLightSpread(int tick) {
            final int LIGHT_DURATION = 10;
            if (tick >= LIGHT_DURATION) {
                return 0.0f;
            }
            double t = tick / (double) LIGHT_DURATION;
            double fade = Math.pow(1.0 - t, 4.0);
            return (float) (420.0 * fade);
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            rocket.level().playLocalSound(
                    rocket.getX(),
                    rocket.getY(),
                    rocket.getZ(),
                    ModSounds.MORTAR_HIT.get(),
                    SoundSource.MASTER,
                    3.0F,
                    1.0F,
                    false
            );

            FireworkSmokeEffects.spawnMortarHit(rocket, random);
        }

        @Override
        public void onBurstTick(FireworkRocketEntity rocket, RandomSource random, int tick) {
            if (tick > 1) {
                return;
            }
            spawnFlash(rocket, random, tick == 0 ? 140 : 60);
        }

        private void spawnFlash(
                FireworkRocketEntity rocket,
                RandomSource random,
                int count
        ) {

            int[] palette = rocket.getColors();

            double x = rocket.getX();
            double y = rocket.getY() + 3.8;
            double z = rocket.getZ();

            for (int i = 0; i < count; i++) {

                double theta = random.nextDouble() * Math.PI * 2.0;
                double phi = Math.acos(2.0 * random.nextDouble() - 1.0);

                double speed = 0.18 + random.nextDouble() * 0.35;

                double vx = Math.sin(phi) * Math.cos(theta) * speed;
                double vy = Math.cos(phi) * speed * 0.55;
                double vz = Math.sin(phi) * Math.sin(theta) * speed;

                rocket.addSpark(new Spark(
                        x,
                        y,
                        z,
                        vx,
                        vy,
                        vz,
                        paletteAt(palette, i),
                        0.0f + random.nextFloat() * 0.00f,
                        1 + random.nextInt(2),
                        0.0f,
                        0.92f,
                        true,
                        false
                ));
            }
        }
    }


    /**
     * Wide flat fan — sparks spread mostly horizontally with a tight Y range. Quick
     * and dramatic, like a spider web exploding outward.
     */
    public static class Spider extends BurstPattern {
        @Override public boolean isBurst() { return true; }
        @Override public int getBurstDuration() { return 32; }
        @Override public int getFlightLifetime() { return FLIGHT_LIFETIME; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            emitFlightTrail(rocket, random, rocket.getLaunchColor(), 0.42f, 10, false);
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            int color = rocket.getLaunchColor();
            int count = 240;
            for (int i = 0; i < count; i++) {
                double theta = random.nextDouble() * Math.PI * 2.0;
                double yJitter = (random.nextDouble() - 0.5) * 0.35;
                double speed = 2.10 + random.nextDouble() * 0.45;
                double vx = Math.cos(theta) * speed;
                double vy = yJitter * speed;
                double vz = Math.sin(theta) * speed;
                rocket.addSpark(new Spark(rocket.getX(), rocket.getY(), rocket.getZ(),
                        vx, vy, vz, color, 0.40f, 30, 0.004f, 0.993f, false, false));
            }
            spawnEmbers(rocket, random, 90);
        }
    }

    /**
     * Slow lingering sphere — sparks barely fall, holding their shape for a long
     * fade. Looks like a glowing crown of stars suspended in the sky.
     */
    public static class Diadem extends BurstPattern {
        @Override public boolean isBurst() { return true; }
        @Override public int getBurstDuration() { return 110; }
        @Override public int getFlightLifetime() { return FLIGHT_LIFETIME; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            emitFlightTrail(rocket, random, rocket.getLaunchColor(), 0.42f, 10, false);
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            int color = rocket.getLaunchColor();
            int count = 280;
            for (int i = 0; i < count; i++) {
                double theta = random.nextDouble() * Math.PI * 2.0;
                double cosPhi = 2.0 * random.nextDouble() - 1.0;
                double sinPhi = Math.sqrt(Math.max(0.0, 1.0 - cosPhi * cosPhi));
                double speed = 0.90 + random.nextDouble() * 0.25;
                double vx = sinPhi * Math.cos(theta) * speed;
                double vy = cosPhi * speed;
                double vz = sinPhi * Math.sin(theta) * speed;
                rocket.addSpark(new Spark(rocket.getX(), rocket.getY(), rocket.getZ(),
                        vx, vy, vz, color, 0.50f, 100, 0.0010f, 0.997f, false, false));
            }
            spawnEmbers(rocket, random, 140);
        }
    }

    /**
     * Pure flash — a brief, very bright detonation with minimal trailing sparks.
     * Loud bang, big halo, scattered embers. Classic salute / aerial bomb feel.
     */
    public static class Salute extends BurstPattern {
        @Override public boolean isBurst() { return true; }
        @Override public boolean hasCrackleSound() { return true; }
        @Override public int getBurstDuration() { return 16; }
        @Override public int getFlightLifetime() { return FLIGHT_LIFETIME; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            emitFlightTrail(rocket, random, rocket.getLaunchColor(), 0.42f, 10, false);
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            int color = rocket.getLaunchColor();
            int count = 80;
            for (int i = 0; i < count; i++) {
                double theta = random.nextDouble() * Math.PI * 2.0;
                double cosPhi = 2.0 * random.nextDouble() - 1.0;
                double sinPhi = Math.sqrt(Math.max(0.0, 1.0 - cosPhi * cosPhi));
                double speed = 3.0 + random.nextDouble() * 1.5;
                double vx = sinPhi * Math.cos(theta) * speed;
                double vy = cosPhi * speed;
                double vz = sinPhi * Math.sin(theta) * speed;
                rocket.addSpark(new Spark(rocket.getX(), rocket.getY(), rocket.getZ(),
                        vx, vy, vz, color, 0.55f, 14, 0.003f, 0.99f, false, false));
            }
            spawnEmbers(rocket, random, 130);
        }
    }

    /**
     * Heart-shaped burst — sparks emit along a parametric heart curve in the camera
     * plane. Uses the classic 16sin³(t)/13cos(t)-... heart equation.
     */
    public static class Heart extends BurstPattern {
        @Override public boolean isBurst() { return true; }
        @Override public int getBurstDuration() { return 70; }
        @Override public int getFlightLifetime() { return FLIGHT_LIFETIME; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            emitFlightTrail(rocket, random, rocket.getLaunchColor(), 0.42f, 10, false);
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            int color = rocket.getLaunchColor();
            int count = 220;
            double scale = 0.11;
            for (int i = 0; i < count; i++) {
                double t = (i / (double) count) * Math.PI * 2.0 + random.nextDouble() * 0.04;
                double hx = 16.0 * Math.pow(Math.sin(t), 3);
                double hy = 13.0 * Math.cos(t) - 5.0 * Math.cos(2.0 * t) - 2.0 * Math.cos(3.0 * t) - Math.cos(4.0 * t);
                double zJitter = (random.nextDouble() - 0.5) * 0.06;
                double vx = hx * scale;
                double vy = hy * scale;
                double vz = zJitter;
                rocket.addSpark(new Spark(rocket.getX(), rocket.getY(), rocket.getZ(),
                        vx, vy, vz, color, 0.42f, 65, 0.0025f, 0.995f, false, false));
            }
            spawnEmbers(rocket, random, 90);
        }
    }

    /**
     * Two distinct expansion phases: a small inner sphere immediately, then a larger
     * outer sphere at tickIndex 14 in a different palette color. Layered explosion.
     */
    public static class DoubleBurst extends BurstPattern {
        @Override public boolean isBurst() { return true; }
        @Override public int getBurstDuration() { return 75; }
        @Override public int getFlightLifetime() { return FLIGHT_LIFETIME; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            emitFlightTrail(rocket, random, rocket.getLaunchColor(), 0.42f, 10, false);
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            int[] palette = rocket.getColors();
            int color1 = palette[0];
            int count = 130;
            for (int i = 0; i < count; i++) {
                double theta = random.nextDouble() * Math.PI * 2.0;
                double cosPhi = 2.0 * random.nextDouble() - 1.0;
                double sinPhi = Math.sqrt(Math.max(0.0, 1.0 - cosPhi * cosPhi));
                double speed = 0.85 + random.nextDouble() * 0.20;
                double vx = sinPhi * Math.cos(theta) * speed;
                double vy = cosPhi * speed;
                double vz = sinPhi * Math.sin(theta) * speed;
                rocket.addSpark(new Spark(rocket.getX(), rocket.getY(), rocket.getZ(),
                        vx, vy, vz, color1, 0.42f, 38, 0.004f, 0.992f, false, false));
            }
        }

        @Override
        public void onBurstTick(FireworkRocketEntity rocket, RandomSource random, int tickIndex) {
            if (tickIndex == 14) {
                int[] palette = rocket.getColors();
                int color2 = palette.length > 1 ? palette[1] : palette[0];
                int count = 220;
                for (int i = 0; i < count; i++) {
                    double theta = random.nextDouble() * Math.PI * 2.0;
                    double cosPhi = 2.0 * random.nextDouble() - 1.0;
                    double sinPhi = Math.sqrt(Math.max(0.0, 1.0 - cosPhi * cosPhi));
                    double speed = 1.75 + random.nextDouble() * 0.45;
                    double vx = sinPhi * Math.cos(theta) * speed;
                    double vy = cosPhi * speed;
                    double vz = sinPhi * Math.sin(theta) * speed;
                    rocket.addSpark(new Spark(rocket.getX(), rocket.getY(), rocket.getZ(),
                            vx, vy, vz, color2, 0.50f, 60, 0.005f, 0.992f, false, false));
                }
                spawnEmbers(rocket, random, 110);
            }
        }
    }

    /**
     * Whistler — pure flying comet (no burst) with a thicker oscillating trail and a
     * crackle sound. Side sparks alternate left/right as it climbs.
     */
    public static class Whistler extends BurstPattern {
        @Override public boolean isBurst() { return false; }
        @Override public boolean hasCrackleSound() { return true; }
        @Override public int getBurstDuration() { return 0; }
        @Override public int getCometFadeTicks() { return 4; }
        @Override public int getFlightLifetime() { return FLIGHT_LIFETIME; }
        @Override public float getFlightLightSpread() { return 34.0f; }
        @Override public float getFlightHaloInnerSize() { return 1.9f; }
        @Override public float getFlightHaloOuterSize() { return 2.85f; }
        @Override public double getFlightWobble() { return 0.025; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            int color = rocket.getLaunchColor();
            for (int i = 0; i < 3; i++) {
                emitFlightTrail(rocket, random, color, 0.50f, 12, true);
            }
            double baseAngle = rocket.tickCount * 0.65;
            Vec3 baseVel = rocket.getDeltaMovement().scale(-0.05);
            int sparkCount = 4;
            for (int i = 0; i < sparkCount; i++) {
                double angle = baseAngle + i * (Math.PI * 2.0 / sparkCount) + (random.nextDouble() - 0.5) * 0.4;
                double radius = 0.50 + random.nextDouble() * 0.80;
                double dx = Math.cos(angle) * radius;
                double dz = Math.sin(angle) * radius;
                double outwardSpeed = 0.18 + random.nextDouble() * 0.15;
                double inv = 1.0 / Math.max(0.001, radius);
                double ovx = dx * inv * outwardSpeed;
                double ovz = dz * inv * outwardSpeed;
                int life = 14 + random.nextInt(6);
                float scale = 0.30f + random.nextFloat() * 0.10f;
                rocket.addSpark(new Spark(
                        rocket.getX() + dx, rocket.getY() + (random.nextDouble() - 0.5) * 0.4, rocket.getZ() + dz,
                        baseVel.x + ovx, baseVel.y, baseVel.z + ovz,
                        color, scale, life, 0.003f, 0.96f, true, false));
            }
        }

        @Override
        public void onFadeTick(FireworkRocketEntity rocket, RandomSource random, int remainingTicks) {
            // Silent fade — no new sparks. Rocket despawns shortly after apex.
        }
    }

    /**
     * Pure flying comet with a soft fade — never explodes. Used for the basic
     * single-color comets: red, blue, green, gold.
     */
    public static class Comet extends BurstPattern {
        @Override public boolean isBurst() { return false; }
        @Override public int getBurstDuration() { return 0; }
        @Override public int getCometFadeTicks() { return 7; }
        @Override public int getFlightLifetime() { return FLIGHT_LIFETIME; }
        @Override public float getFlightLightSpread() { return 32.0f; }
        @Override public float getFlightHaloInnerSize() { return 1.7f; }
        @Override public float getFlightHaloOuterSize() { return 2.55f; }
        @Override public boolean spawnsFlightSmoke() { return true; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            int color = rocket.getLaunchColor();
            emitFlightTrail(rocket, random, color, 0.50f, 12, true);
        }

        @Override
        public void onFadeTick(FireworkRocketEntity rocket, RandomSource random, int remainingTicks) {
            int color = rocket.getLaunchColor();
            int fadeTicks = getCometFadeTicks();
            float fade = remainingTicks / (float) Math.max(1, fadeTicks);
            if (fade <= 0.0f || random.nextFloat() > fade * 0.85f) {
                return;
            }
            Vec3 motion = rocket.getDeltaMovement().scale(-0.05);
            double scatter = 0.06;
            double vx = motion.x + (random.nextDouble() - 0.5) * scatter;
            double vy = motion.y + (random.nextDouble() - 0.5) * scatter * 0.5;
            double vz = motion.z + (random.nextDouble() - 0.5) * scatter;
            rocket.addSpark(new Spark(rocket.getX(), rocket.getY(), rocket.getZ(),
                    vx, vy, vz, color, 0.30f + random.nextFloat() * 0.12f, 14, 0.006f, 0.97f, true, false));
        }
    }

    /**
     * Long ascending comet with dense trail, then a short controlled drop after apex.
     */
    public static class LongTrailComet extends BurstPattern {
        private static final double FADE_DESCENT_BLOCKS = 3.0;

        @Override public boolean isBurst() { return false; }
        @Override public int getBurstDuration() { return 0; }
        @Override public int getCometFadeTicks() { return 52; }
        @Override public int getFlightLifetime() { return 220; }
        @Override public boolean continuesAfterApex() { return false; }
        @Override public double getFadeDescentBlocks() { return FADE_DESCENT_BLOCKS; }
        @Override public int getServerHoldTicks() {
            return getFlightLifetime() + getCometFadeTicks() + 24;
        }
        @Override public float getLaunchSpeedMultiplier() { return 1.12f; }
        @Override public float getFlightLightSpread() { return 34.0f; }
        @Override public float getFlightHaloInnerSize() { return 1.8f; }
        @Override public float getFlightHaloOuterSize() { return 2.7f; }
        @Override public boolean spawnsFlightSmoke() { return true; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            int[] palette = rocket.getColors();
            int primary = rocket.getLaunchColor();
            int secondary = palette.length > 1 ? palette[1] : primary;
            emitFlightTrail(rocket, random, primary, 0.56f, 18, true);
            if (random.nextFloat() < 0.45f) {
                emitFlightTrail(rocket, random, secondary, 0.42f, 14, true);
            }
        }

        @Override
        public void onFadeTick(FireworkRocketEntity rocket, RandomSource random, int remainingTicks) {
            int[] palette = rocket.getColors();
            int primary = rocket.getLaunchColor();
            int secondary = palette.length > 1 ? palette[1] : primary;
            float fade = remainingTicks / (float) Math.max(1, getCometFadeTicks());
            if (fade <= 0.0f) {
                return;
            }
            int count = 2 + random.nextInt(3);
            for (int i = 0; i < count; i++) {
                double scatter = 0.10 * fade;
                double vx = (random.nextDouble() - 0.5) * scatter;
                double vy = -0.04 - random.nextDouble() * 0.10 * fade;
                double vz = (random.nextDouble() - 0.5) * scatter;
                int color = random.nextBoolean() ? primary : secondary;
                rocket.addSpark(new Spark(
                        rocket.getX(), rocket.getY(), rocket.getZ(),
                        vx, vy, vz,
                        color,
                        0.28f + random.nextFloat() * 0.14f,
                        24 + random.nextInt(18),
                        0.012f,
                        0.975f,
                        true,
                        false
                ));
            }
        }
    }

    /**
     * Daytime smoke rocket — launch plume, colored ascent trail, Holi burst at apex.
     */
    public static class DaytimePowder extends BurstPattern {
        @Override public boolean isBurst() { return false; }
        @Override public boolean isDaytimePowder() { return true; }
        @Override public int getBurstDuration() { return 0; }
        @Override public int getCometFadeTicks() { return 120; }
        @Override public int getFlightLifetime() { return 62; }
        @Override public boolean continuesAfterApex() { return false; }
        @Override public double getFlightWobble() { return 0.012; }
        @Override public double getGravity() { return 0.028; }
        @Override public double getDrag() { return 0.991; }
        @Override public float getLaunchSpeedMultiplier() { return 0.72f; }
        @Override public int getFlightLuminance() { return 0; }
        @Override public float getFlightLightSpread() { return 0.0f; }
        @Override public float getFlightHaloInnerSize() { return 0.0f; }
        @Override public float getFlightHaloOuterSize() { return 0.0f; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            int[] palette = rocket.getColors();
            emitDaytimeTrailPuff(rocket, random, rocket.getLaunchColor());
            if (palette.length > 1 && random.nextFloat() < 0.35f) {
                emitDaytimeTrailPuff(rocket, random, palette[1]);
            }
        }

        @Override
        public void onFadeTick(FireworkRocketEntity rocket, RandomSource random, int remainingTicks) {
            if (rocket.tryFireDaytimeBurst()) {
                emitDaytimeColorBurst(rocket, random);
            }
        }
    }

    /**
     * Rainbow daytime powder fan — ground cone burst, no night flash.
     */
    public static class DaytimePowderFan extends BurstPattern {
        @Override public boolean isBurst() { return true; }
        @Override public boolean isDaytimePowder() { return true; }
        @Override public int getBurstDuration() { return 200; }
        @Override public int getFlightLifetime() { return 1; }
        @Override public float getLaunchSpeedMultiplier() { return 0.02f; }
        @Override public double getFlightWobble() { return 0.0; }
        @Override public int getFlightLuminance() { return 0; }
        @Override public float getFlightLightSpread() { return 0.0f; }
        @Override public float getFlightHaloInnerSize() { return 0.0f; }
        @Override public float getFlightHaloOuterSize() { return 0.0f; }

        @Override
        public void onFlightTick(FireworkRocketEntity rocket, RandomSource random) {
            if (rocket.tryFireDaytimeFan()) {
                emitOrientedPowderFan(rocket, random);
            }
        }

        @Override
        public void onBurstStart(FireworkRocketEntity rocket, RandomSource random) {
            if (rocket.tryFireDaytimeFan()) {
                emitOrientedPowderFan(rocket, random);
            }
        }
    }
}
