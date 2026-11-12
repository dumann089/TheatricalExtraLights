package com.github.dumann089.theatricalextralights.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;


@Environment(EnvType.CLIENT)
public class WaterFanParticle extends TextureSheetParticle {

    public static WaterFanParticleProvider provider(SpriteSet spriteSet) {
        return new WaterFanParticleProvider(spriteSet);
    }

    public static class WaterFanParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public WaterFanParticleProvider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new WaterFanParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }

    private final SpriteSet spriteSet;
    private final float rollSpeed;
    private float targetIntensity;
    private float currentIntensity;

    protected WaterFanParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
        super(world, x, y, z);
        this.spriteSet = spriteSet;
        this.setSize((float) 0.1, (float) 0.1);

        this.targetIntensity = (float) vy;
        this.currentIntensity = this.targetIntensity;

        float minSize = 0.45F;
        float maxSize = 0.45F;
        this.quadSize = minSize + (maxSize - minSize) * this.currentIntensity;

        this.lifetime = 35;
        this.gravity = 1.4F;
        this.hasPhysics = false;

        double dimmer = Mth.clamp(vy, 0.0, 1.0);

        Vec3 dir = new Vec3(vx, 1.0, vz).normalize();

        Vec3 up = Math.abs(dir.y) > 0.99
                ? new Vec3(1, 0, 0)
                : new Vec3(0, 1, 0);

        Vec3 side = dir.cross(up).normalize();

        double spread = 0.4;
        double factor = (Math.random() * 2 - 1) * spread;

        Vec3 finalDir = dir.add(side.scale(factor)).normalize();

        double speed = dimmer * 0.6;

        this.xd = finalDir.x * speed;
        this.yd = finalDir.y * speed;
        this.zd = finalDir.z * speed;

        this.setSpriteFromAge(spriteSet);
        this.roll = (float)(Math.random() * Math.PI * 2);
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

        if (!this.removed) {
            float lifeRatio = (float)this.age / (float)this.lifetime;
            float alpha = 1.0F - lifeRatio;

            this.alpha = alpha;

            this.oRoll = this.roll;
            this.roll += this.rollSpeed;

            this.setSpriteFromAge(this.spriteSet);
        }
    }
}