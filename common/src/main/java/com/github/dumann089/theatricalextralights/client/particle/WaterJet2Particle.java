package com.github.dumann089.theatricalextralights.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public class WaterJet2Particle extends TextureSheetParticle {

    public static ParticleProvider<WaterJetParticleOptions> provider(SpriteSet spriteSet) {
        return (options, world, x, y, z, vx, vy, vz) ->
                new WaterJet2Particle(
                        world,
                        x, y, z,
                        vx, vy, vz,
                        options.intensity,
                        options.thickness,
                        spriteSet
                );
    }

    private final SpriteSet spriteSet;
    private float thickness;
    private final float rollSpeed;

    protected WaterJet2Particle(
            ClientLevel world,
            double x, double y, double z,
            double vx, double vy, double vz,
            float intensity,
            float thickness,
            SpriteSet spriteSet
    ) {
        super(world, x, y, z);
        this.spriteSet = spriteSet;
        this.thickness = Mth.clamp(thickness, 0.02F, 1.0F);

        this.setSize(0.1F, 0.1F);

        float minSize = 0.05F;
        float maxSize = 0.75F;
        this.quadSize = minSize + (maxSize - minSize) * this.thickness;

        this.lifetime = 70;
        this.gravity = 1.4F;
        this.hasPhysics = false;

        double spread = 0.03;
        this.xd = (vx + (Math.random() - 0.25) * spread);
        this.yd = vy + (Math.random() * 0.25);
        this.zd = (vz + (Math.random() - 0.25) * spread);

        this.setSpriteFromAge(spriteSet);
        this.roll = (float)(Math.random() * 6 * Math.PI);
        this.oRoll = this.roll;
        this.rollSpeed = (float)(Math.random() * 0.4 - 0.2);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            float lifeRatio = (float) age / lifetime;
            this.alpha = 1.0F - lifeRatio;
            setSpriteFromAge(spriteSet);
        }
    }
}