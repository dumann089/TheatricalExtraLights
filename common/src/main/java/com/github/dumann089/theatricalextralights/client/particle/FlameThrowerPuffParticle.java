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

/** Bouffée au bout du jet — grosse, s'évapore vite. */
@Environment(EnvType.CLIENT)
public class FlameThrowerPuffParticle extends TextureSheetParticle {
    private static final float EXPAND_RATE = 0.075f;

    private final SpriteSet sprites;
    private final float peakSize;

    protected FlameThrowerPuffParticle(
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
        Vec3 lateral = randomDiskOffset(axis, random, 0.09f + random.nextFloat() * 0.07f);
        float drift = 0.05f + random.nextFloat() * 0.07f;

        xd = axis.x * drift + lateral.x;
        yd = axis.y * drift + lateral.y + 0.01f;
        zd = axis.z * drift + lateral.z;

        hasPhysics = false;
        gravity = 0.0f;
        lifetime = 5 + random.nextInt(5);
        float distanceScale = FireworkRenderDistances.flameParticleSizeScale(x, y, z);
        peakSize = (0.28f + random.nextFloat() * 0.14f) * distanceScale;
        quadSize = peakSize * 0.65f;
        alpha = 0.8f + random.nextFloat() * 0.2f;
        rCol = 1.0f;
        gCol = 0.45f + random.nextFloat() * 0.2f;
        bCol = 0.05f + random.nextFloat() * 0.08f;
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

        xd *= 0.88f;
        yd *= 0.88f;
        yd += 0.003f;
        zd *= 0.88f;
        move(xd, yd, zd);

        float life = (float) age / (float) lifetime;
        quadSize = Math.min(peakSize * 2.8f, quadSize + EXPAND_RATE);

        if (life < 0.35f) {
            float t = life / 0.35f;
            rCol = Mth.lerp(t, 1.0f, 0.85f);
            gCol = Mth.lerp(t, 0.55f, 0.35f);
            bCol = 0.0f;
            alpha = Mth.lerp(t, 0.9f, 0.6f);
        } else {
            float t = (life - 0.35f) / 0.65f;
            t = Mth.clamp(t, 0.0f, 1.0f);
            rCol = Mth.lerp(t, 0.85f, 0.35f);
            gCol = Mth.lerp(t, 0.35f, 0.08f);
            bCol = 0.0f;
            alpha = (1.0f - t) * (1.0f - t) * 0.6f;
            quadSize = peakSize * (1.6f - t * 0.55f);
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
            return new FlameThrowerPuffParticle(level, x, y, z, dirX, dirY, dirZ, sprites, level.random);
        }
    }
}
