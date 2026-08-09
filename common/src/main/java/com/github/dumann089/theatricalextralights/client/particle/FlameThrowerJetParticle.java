package com.github.dumann089.theatricalextralights.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import com.github.dumann089.theatricalextralights.firework.FireworkRenderDistances;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Coeur du jet — gros, chaud, visible de loin. */
@Environment(EnvType.CLIENT)
public class FlameThrowerJetParticle extends TextureSheetParticle {
    private static final float CONE_SPREAD = 0.055f;
    private static final float DRAG = 0.915f;

    private final SpriteSet sprites;
    private final float startSize;
    private final float peakSize;

    protected FlameThrowerJetParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double dirX,
            double dirY,
            double dirZ,
            SpriteSet sprites,
            RandomSource random
    ) {
        super(level, x, y, z);
        this.sprites = sprites;

        Vec3 axis = normalizeDirection(dirX, dirY, dirZ);
        float speed = 0.62f + random.nextFloat() * 0.45f;
        Vec3 lateral = randomDiskOffset(axis, random, CONE_SPREAD * (0.65f + random.nextFloat() * 0.55f));

        xd = axis.x * speed + lateral.x;
        yd = axis.y * speed + lateral.y;
        zd = axis.z * speed + lateral.z;

        hasPhysics = false;
        gravity = 0.0f;
        lifetime = 10 + random.nextInt(10);
        float distanceScale = FireworkRenderDistances.flameParticleSizeScale(x, y, z);
        startSize = (0.18f + random.nextFloat() * 0.1f) * distanceScale;
        peakSize = startSize * (1.85f + random.nextFloat() * 0.55f);
        quadSize = startSize * 0.9f;
        alpha = 1.0f;
        rCol = 1.0f;
        gCol = 0.95f + random.nextFloat() * 0.05f;
        bCol = 0.65f + random.nextFloat() * 0.15f;
        pickSprite(sprites);
    }

    private static Vec3 normalizeDirection(double dx, double dy, double dz) {
        Vec3 dir = new Vec3(dx, dy, dz);
        if (dir.lengthSqr() < 1.0e-8) {
            return new Vec3(0.0, 1.0, 0.0);
        }
        return dir.normalize();
    }

    private static Vec3 randomDiskOffset(Vec3 axis, RandomSource random, float radius) {
        Vec3 helper = Math.abs(axis.y) < 0.92 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 tangent = axis.cross(helper);
        if (tangent.lengthSqr() < 1.0e-8) {
            tangent = new Vec3(1.0, 0.0, 0.0);
        }
        tangent = tangent.normalize();
        Vec3 bitangent = axis.cross(tangent).normalize();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double dist = random.nextDouble() * radius;
        return tangent.scale(Math.cos(angle) * dist).add(bitangent.scale(Math.sin(angle) * dist));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ExtraLightsRenderTypes.flameThrowerJetRenderType();
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }

        float life = (float) age / (float) lifetime;
        float drag = life > 0.45f ? DRAG * 0.88f : DRAG;
        xd *= drag;
        yd *= drag;
        zd *= drag;
        if (life > 0.5f) {
            yd += 0.005f * (life - 0.5f);
        }
        move(xd, yd, zd);

        if (life < 0.2f) {
            float t = life / 0.2f;
            quadSize = Mth.lerp(t, startSize * 0.9f, peakSize);
            rCol = 1.0f;
            gCol = Mth.lerp(t, 0.85f, 1.0f);
            bCol = Mth.lerp(t, 0.35f, 0.7f);
            alpha = 0.98f;
        } else if (life < 0.55f) {
            float t = (life - 0.2f) / 0.35f;
            quadSize = Mth.lerp(t, peakSize, peakSize * 0.82f);
            rCol = 1.0f;
            gCol = Mth.lerp(t, 1.0f, 0.5f);
            bCol = Mth.lerp(t, 0.7f, 0.1f);
            alpha = 1.0f - t * 0.3f;
        } else {
            float t = (life - 0.55f) / 0.45f;
            t = Mth.clamp(t, 0.0f, 1.0f);
            quadSize = peakSize * (1.0f - t * 0.75f);
            rCol = Mth.lerp(t, 1.0f, 0.4f);
            gCol = Mth.lerp(t, 0.5f, 0.08f);
            bCol = 0.0f;
            alpha = (1.0f - t) * (1.0f - t);
        }

        setSpriteFromAge(sprites);
    }

    @Environment(EnvType.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
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
            return new FlameThrowerJetParticle(level, x, y, z, dirX, dirY, dirZ, sprites, level.random);
        }
    }
}
