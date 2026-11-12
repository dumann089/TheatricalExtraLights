package com.github.dumann089.theatricalextralights.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class WaterJetConeParticle extends TextureSheetParticle {

    public static ParticleProvider<WaterJetParticleOptions> provider(SpriteSet spriteSet) {
        return (options, world, x, y, z, vx, vy, vz) ->
                new WaterJetConeParticle(
                        world,
                        x, y, z,
                        vx, vy, vz,
                        options.intensity,
                        options.thickness,
                        options.coneAngle,
                        spriteSet
                );
    }

    private final SpriteSet spriteSet;
    private final float rollSpeed;

    protected WaterJetConeParticle(
            ClientLevel world,
            double x, double y, double z,
            double vx, double vy, double vz,
            float intensity,
            float thickness,
            float coneAngleDeg,
            SpriteSet spriteSet
    ) {
        super(world, x, y, z);
        this.spriteSet = spriteSet;


        this.setSize(0.1F, 0.1F);

        float minSize = 0.05F;
        float maxSize = 0.75F;
        this.quadSize = minSize + (maxSize - minSize)
                * Mth.clamp(thickness, 0.02F, 1.0F);

        this.lifetime = 70;
        this.gravity = 1.4F;
        this.hasPhysics = false;

        float t = Mth.clamp(coneAngleDeg / 90f, 0f, 1f);

        float tiltRad = Mth.lerp(t, Mth.HALF_PI, 0f);

        float theta = random.nextFloat() * Mth.TWO_PI;

        float horizontal = Mth.sin(tiltRad);
        float vertical   = Mth.cos(tiltRad);

        Vec3 coneDir = new Vec3(
                Mth.cos(theta) * horizontal,
                vertical,
                Mth.sin(theta) * horizontal
        ).normalize();

        float totalSpeed = intensity * 0.5f;

        this.xd = vx + coneDir.x * totalSpeed;
        this.yd = vy + coneDir.y * totalSpeed;
        this.zd = vz + coneDir.z * totalSpeed;

        this.setSpriteFromAge(spriteSet);
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        this.rollSpeed = random.nextFloat() * 0.4f - 0.2f;
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
