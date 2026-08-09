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

/** Colonne CO₂ — jet continu, collision blocs, pas de boules distinctes. */
@Environment(EnvType.CLIENT)
public class Co2JetCoreParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float startSize;
    private final float peakSize;

    protected Co2JetCoreParticle(
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
        gravity = 0.0f;
        friction = 0.94f;
        lifetime = 38 + random.nextInt(22);
        float distanceScale = FireworkRenderDistances.flameParticleSizeScale(x, y, z);
        startSize = (0.14f + random.nextFloat() * 0.06f) * distanceScale;
        peakSize = startSize * (2.4f + random.nextFloat() * 1.1f);
        quadSize = startSize * 0.85f;
        alpha = 0.22f + random.nextFloat() * 0.08f;
        rCol = 0.88f + random.nextFloat() * 0.06f;
        gCol = 0.88f + random.nextFloat() * 0.06f;
        bCol = 0.92f + random.nextFloat() * 0.04f;
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

        xd += (random.nextDouble() - 0.5) * 0.003;
        yd += (random.nextDouble() - 0.5) * 0.003;
        zd += (random.nextDouble() - 0.5) * 0.003;
        move(xd, yd, zd);

        if (onGround) {
            xd *= 0.3;
            yd *= 0.12;
            zd *= 0.3;
        }

        float life = (float) age / (float) lifetime;
        quadSize = Mth.lerp(life * life * life, startSize, peakSize);
        if (life < 0.55f) {
            alpha = 0.24f + life * 0.52f;
        } else {
            float fade = (life - 0.55f) / 0.45f;
            alpha = (1.0f - fade) * (1.0f - fade) * 0.58f;
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
            return new Co2JetCoreParticle(level, x, y, z, dirX, dirY, dirZ, sprites, level.random);
        }
    }
}
