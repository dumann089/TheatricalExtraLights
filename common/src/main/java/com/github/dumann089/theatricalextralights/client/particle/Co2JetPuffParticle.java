package com.github.dumann089.theatricalextralights.client.particle;

import com.github.dumann089.theatricalextralights.firework.FireworkRenderDistances;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Volume CO₂ — gros billows, collision blocs, pas de petits points. */
@Environment(EnvType.CLIENT)
public class Co2JetPuffParticle extends TextureSheetParticle {
    private static final float EXPAND_RATE = 0.016f;

    private final SpriteSet sprites;
    private final float peakSize;

    protected Co2JetPuffParticle(
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
        super(level, x, y, z, dirX, dirY, dirZ);
        this.sprites = sprites;

        xd = dirX;
        yd = dirY;
        zd = dirZ;

        hasPhysics = true;
        gravity = 0.0015f;
        friction = 0.955f;
        lifetime = 30 + random.nextInt(12);
        float distanceScale = FireworkRenderDistances.flameParticleSizeScale(x, y, z);
        peakSize = (0.42f + random.nextFloat() * 0.2f) * distanceScale;
        quadSize = peakSize * 0.75f;
        alpha = 0.34f + random.nextFloat() * 0.12f;
        rCol = 0.9f + random.nextFloat() * 0.05f;
        gCol = 0.9f + random.nextFloat() * 0.05f;
        bCol = 0.94f + random.nextFloat() * 0.04f;
        pickSprite(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ExtraLightsRenderTypes.co2JetRenderType();
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

        xd += (random.nextDouble() - 0.5) * 0.0015;
        yd += (random.nextDouble() - 0.5) * 0.0015;
        zd += (random.nextDouble() - 0.5) * 0.0015;
        move(xd, yd, zd);

        if (onGround) {
            xd *= 0.22;
            yd *= 0.06;
            zd *= 0.22;
        }

        float life = (float) age / (float) lifetime;
        quadSize = Math.min(peakSize * 1.55f, quadSize + EXPAND_RATE);
        if (life < 0.5f) {
            alpha = 0.36f + life * 0.42f;
        } else {
            float fade = (life - 0.5f) / 0.5f;
            alpha = (1.0f - fade) * (1.0f - fade) * 0.52f;
        }

        long gameTime = level.getGameTime();
        float dissipation = Flow2JetDissipation.alphaMultiplier(x, y, z, gameTime);
        alpha *= dissipation;
        if (dissipation < 1f) {
            float damp = Flow2JetDissipation.motionDamping(x, y, z, gameTime);
            xd *= damp;
            yd *= damp;
            zd *= damp;
        }
        if (dissipation < 0.12f || alpha < 0.012f) {
            remove();
            return;
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
            return new Co2JetPuffParticle(level, x, y, z, dirX, dirY, dirZ, sprites, level.random);
        }
    }
}
