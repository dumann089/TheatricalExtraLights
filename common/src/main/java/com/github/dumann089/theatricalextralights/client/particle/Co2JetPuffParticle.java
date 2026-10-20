package com.github.dumann089.theatricalextralights.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Volutes : grosses et molles, elles font le corps du panache et sa tete qui roule en ralentissant. */
@Environment(EnvType.CLIENT)
public class Co2JetPuffParticle extends Co2FogParticle {
    private static final float DRAG = 0.905f;
    private static final float TURBULENCE = 0.020f;

    private static SpriteSet spriteSet;

    /** @param pressure 0..1, intensite DMX : a basse pression le panache reste plus fin. */
    Co2JetPuffParticle(ClientLevel level, Vec3 pos, Vec3 velocity, Vector3f axis, RandomSource random, float pressure) {
        super(level, pos, velocity, axis, spriteSet, random,
                0.05f + random.nextFloat() * 0.02f,
                0.16f + random.nextFloat() * 0.06f,
                (0.50f + random.nextFloat() * 0.25f) * (0.7f + 0.3f * pressure),
                0.40f + random.nextFloat() * 0.10f,
                DRAG,
                TURBULENCE,
                1f,
                28 + random.nextInt(16));
    }

    static boolean ready() {
        return spriteSet != null;
    }

    @Environment(EnvType.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        public Provider(SpriteSet sprites) {
            spriteSet = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double dirX,
                double dirY,
                double dirZ
        ) {
            Vec3 velocity = new Vec3(dirX, dirY, dirZ);
            Vec3 axis = velocity.lengthSqr() > 1.0e-8 ? velocity.normalize() : new Vec3(0, 1, 0);
            return new Co2JetPuffParticle(level, new Vec3(x, y, z), velocity, new Vector3f((float) axis.x, (float) axis.y, (float) axis.z), level.random, 1f);
        }
    }
}
