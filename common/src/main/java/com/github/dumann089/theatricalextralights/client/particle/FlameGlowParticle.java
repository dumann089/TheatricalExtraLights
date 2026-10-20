package com.github.dumann089.theatricalextralights.client.particle;

import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
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
import net.minecraft.world.phys.Vec3;

/**
 * Halo de bloom des flammes : grand sprite doux, tres transparent, en additif, qui suit le jet.
 * Les halos se superposent le long du jet et donnent la lueur sans aucun post-traitement de
 * l'image (le bloom plein ecran cassait le rendu sous Shimmer). La vitesse suit la longueur du
 * vecteur direction, comme pour {@link FlameThrowerJetParticle}.
 */
@Environment(EnvType.CLIENT)
public class FlameGlowParticle extends TextureSheetParticle {
    private static final float DRAG = 0.915f;
    /** Opacite d'un halo a force 1 ; une dizaine se superposent au coeur du jet. */
    private static final float BASE_ALPHA = 0.09f;
    /** Part des halos larges, plus pales, qui font deborder la lueur autour du jet. */
    private static final float WIDE_CHANCE = 0.3f;

    private final float startSize;
    private final float endSize;
    private final float peakAlpha;

    protected FlameGlowParticle(ClientLevel level, double x, double y, double z,
                                double dirX, double dirY, double dirZ, SpriteSet sprites) {
        super(level, x, y, z);
        double magnitude = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        Vec3 axis = magnitude > 1.0e-4 ? new Vec3(dirX / magnitude, dirY / magnitude, dirZ / magnitude)
                : new Vec3(0.0, 1.0, 0.0);
        float reach = magnitude > 1.0e-4 ? (float) Math.max(0.2, Math.min(2.0, magnitude)) : 1f;
        float speed = (0.55f + random.nextFloat() * 0.4f) * reach;
        xd = axis.x * speed;
        yd = axis.y * speed;
        zd = axis.z * speed;

        hasPhysics = false;
        gravity = 0.0f;
        lifetime = 12 + random.nextInt(6);
        float distanceScale = FireworkRenderDistances.flameParticleSizeScale(x, y, z);
        boolean wide = random.nextFloat() < WIDE_CHANCE;
        float spread = (0.8f + random.nextFloat() * 0.4f) * (wide ? 2.1f : 1.0f);
        startSize = 0.9f * spread * distanceScale;
        endSize = 2.8f * spread * (0.6f + 0.4f * reach) * distanceScale;
        quadSize = startSize;
        peakAlpha = BASE_ALPHA * (wide ? 0.45f : 1.0f) * TheatricalExtraLightsConfig.getFlameBloomStrength();
        alpha = 0.0f;
        rCol = 1.0f;
        gCol = 0.72f;
        bCol = 0.34f;
        pickSprite(sprites);
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

        quadSize = Mth.lerp(Mth.sqrt(life), startSize, endSize);
        float fadeIn = Mth.clamp(life / 0.15f, 0.0f, 1.0f);
        float fadeOut = (float) Math.pow(1.0f - life, 1.3f);
        alpha = peakAlpha * fadeIn * fadeIn * fadeOut;
        gCol = Mth.lerp(life, 0.72f, 0.4f);
        bCol = Mth.lerp(life, 0.34f, 0.08f);
    }

    @Environment(EnvType.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double dirX, double dirY, double dirZ) {
            return new FlameGlowParticle(level, x, y, z, dirX, dirY, dirZ, sprites);
        }
    }
}
