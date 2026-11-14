package com.github.dumann089.theatricalextralights.client.particle;

import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.firework.FireworkRenderDistances;
import com.github.dumann089.theatricalextralights.util.FixtureJetDirection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Emission du jet CO2 : un coeur serre et rapide, des volutes en cone autour. La vitesse de sortie
 * suit la pression (intensite DMX), l'ouverture de vanne donne un coup de pression sur les premiers
 * ticks.
 */
@Environment(EnvType.CLIENT)
public final class Flow2JetParticleSpawner {
    private static final int PUFF_BASE = 14;
    private static final int CORE_BASE = 7;
    /** Demi-angle du cone de volutes ; le coeur reste serre. */
    private static final float PUFF_CONE_DEGREES = 6.0f;
    private static final float CORE_CONE_DEGREES = 1.6f;
    /** Vitesse de sortie en blocs/tick : 0.35 a pression minimale, 0.85 a pleine pression (~17 m/s). */
    private static final float SPEED_MIN = 0.35f;
    private static final float SPEED_RANGE = 0.50f;
    /** Freinage du gaz par tick, le meme que celui des bouffees. */
    public static final float DRAG = 0.905f;
    /** Vitesse du front de coupure quand la vanne se ferme, blocs/tick. */
    public static final float CUT_SPEED = 0.55f;
    private static final int BURST_TICKS = 3;
    private static final float BURST_GAIN = 1.25f;

    private Flow2JetParticleSpawner() {
    }

    /** Vitesse de sortie, blocs/tick, fois la portee reglee dans la config. */
    public static float exitSpeed(float pressure) {
        return (SPEED_MIN + SPEED_RANGE * pressure) * TheatricalExtraLightsConfig.getCo2JetReach();
    }

    /** Distance du front du panache apres {@code ageTicks} ticks : v0 (1 - drag^age) / (1 - drag). */
    public static float plumeLength(float pressure, float ageTicks) {
        return exitSpeed(pressure) * (1f - (float) Math.pow(DRAG, Math.max(0f, ageTicks))) / (1f - DRAG);
    }

    /**
     * @param nozzle       bouche de la buse en coordonnees monde (pose du modele deja appliquee)
     * @param jetDirection axe du jet, unitaire, en coordonnees monde
     * @param ticksActive  ticks ecoules depuis l'ouverture de la vanne (0 au premier)
     */
    public static void spawnJet(
            ClientLevel level,
            Vec3 nozzle,
            Vector3f jetDirection,
            int intensity,
            int ticksActive,
            RandomSource random
    ) {
        if (intensity <= 0 || !Co2JetPuffParticle.ready() || !Co2JetCoreParticle.ready()) {
            return;
        }
        if (!FireworkRenderDistances.isWithinClientFlameRange(nozzle.x, nozzle.y, nozzle.z)) {
            return;
        }

        ParticleEngine engine = Minecraft.getInstance().particleEngine;
        float pressure = FixtureJetDirection.intensityFactor(intensity);
        float flow = 0.35f + 0.65f * pressure;
        boolean burst = ticksActive < BURST_TICKS;
        float speed = (SPEED_MIN + SPEED_RANGE * pressure) * (burst ? 1.1f : 1f);

        int puffs = Math.round((PUFF_BASE + random.nextInt(3)) * flow * (burst ? BURST_GAIN : 1f));
        for (int i = 0; i < puffs; i++) {
            float cone = PUFF_CONE_DEGREES * (0.6f + random.nextFloat() * 0.8f);
            Vec3 velocity = Co2SmokePhysics.randomUnitCone(jetDirection, cone, random)
                    .scale(speed * (0.88f + random.nextFloat() * 0.24f));
            Vec3 pos = emitPoint(nozzle, jetDirection, velocity, 0.03f, random);
            engine.add(new Co2JetPuffParticle(level, pos, velocity, jetDirection, random, pressure));
        }

        int cores = Math.round((CORE_BASE + random.nextInt(2)) * flow);
        for (int i = 0; i < cores; i++) {
            Vec3 velocity = Co2SmokePhysics.randomUnitCone(jetDirection, CORE_CONE_DEGREES, random)
                    .scale(speed * 1.06f * (0.95f + random.nextFloat() * 0.10f));
            Vec3 pos = emitPoint(nozzle, jetDirection, velocity, 0.02f, random);
            engine.add(new Co2JetCoreParticle(level, pos, velocity, jetDirection, random, pressure));
        }
    }

    /**
     * Point d'emission : la bouche de la buse, etalee sur le trajet du premier tick. Sinon chaque lot
     * part du meme point et la colonne se decoupe en anneaux espaces d'un tick de vol.
     */
    private static Vec3 emitPoint(Vec3 nozzle, Vector3f axis, Vec3 velocity, float radius, RandomSource random) {
        return nozzle.add(Co2SmokePhysics.randomDisk(axis, random, radius)).add(velocity.scale(random.nextDouble()));
    }
}
