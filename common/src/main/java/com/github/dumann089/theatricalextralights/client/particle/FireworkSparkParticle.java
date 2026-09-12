package com.github.dumann089.theatricalextralights.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

public class FireworkSparkParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final boolean trail;
    private final boolean strobe;
    private final float baseAlpha;

    protected FireworkSparkParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, FireworkSparkParticleOptions options, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.trail = options.trail;
        this.strobe = options.strobe;
        this.baseAlpha = options.alpha;
        this.gravity = options.gravity;
        this.lifetime = Math.max(4, options.lifetime + this.random.nextInt(4) - 1);
        this.quadSize *= options.scale;
        this.friction = trail ? 0.985f : 0.998f;
        this.rCol = options.red;
        this.gCol = options.green;
        this.bCol = options.blue;
        this.alpha = options.alpha;
        this.hasPhysics = false;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
        if (trail) {
            quadSize *= 0.994f;
        } else {
            quadSize *= 0.998f;
        }

        if (strobe) {
            alpha = age % 2 == 0 ? baseAlpha : baseAlpha * 0.35f;
        } else {
            float t = (float) age / (float) lifetime;
            float curve = 1.0f - t * t;
            alpha = Math.max(0.0f, baseAlpha * curve);
        }
    }

    @Override
    public int getLightColor(float partialTick) {
        return super.getLightColor(partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static class Provider implements ParticleProvider<FireworkSparkParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(FireworkSparkParticleOptions options, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new FireworkSparkParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options, sprites);
        }
    }
}
