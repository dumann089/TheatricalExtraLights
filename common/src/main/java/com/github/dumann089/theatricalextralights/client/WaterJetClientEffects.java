package com.github.dumann089.theatricalextralights.client;

import com.github.dumann089.theatricalextralights.blockentities.*;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasJetConeAngle;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasJetHeight;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasJetThickness;
import com.github.dumann089.theatricalextralights.client.particle.JetVariant;
import com.github.dumann089.theatricalextralights.client.particle.WaterJetParticleOptions;
import com.github.dumann089.theatricalextralights.particle.ModParticle;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import dev.imabad.theatrical.blocks.HangableBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Optional;

/**
 * Client-side water jet particles — per-frame spawns from BER (with distance culling).
 */
public final class WaterJetClientEffects {
    private static final double MAX_SPAWN_DISTANCE_SQ = 512.0 * 512.0;
    private static final double[] ORGAN_PIPE_X = {-1.4375, -1.125, -0.75, -0.375, 0.0, 0.375, 0.75, 1.125, 1.4375};
    private static final double[] ORGAN_PIPE_HEIGHT_MULT = {0.6, 0.7, 0.8, 0.9, 1.0, 0.9, 0.8, 0.7, 0.6};
    private static final double[] ORGAN_PIPE_INV_HEIGHT_MULT = {1.0, 0.9, 0.8, 0.7, 0.6, 0.7, 0.8, 0.9, 1.0};
    private static final double[][] SPINNER_NOZZLES = {
            {0.96, 2.0, 0.501},
            {0.5, 2.0, 0.968},
            {0.5, 2.0, 0.034},
            {0.034, 2.0, 0.501}
    };
    private static final double[] FAN_OFFSETS_X = {
            -0.507, -0.415, -0.321, -0.227, -0.133, -0.040,
            0.040,  0.133,  0.227,  0.321,  0.415,  0.507
    };
    private static final float[] FAN_ANGLES = {
            -10.5F, -8.5F, -6.5F, -4.5F, -2.5F, -0.5F,
            0.5f,  2.5F,  4.5F,  6.5F,  8.5F,  10.5F
    };
    private static final double[] FAN_HEIGHT_MULT = {
            0.75, 0.8, 0.85, 0.9, 0.95, 1.0,
            1.0, 0.95, 0.9, 0.85, 0.8, 0.75
    };
    private static final int[] CAKE_COUNTS = {10, 8, 6};
    private static final double[] CAKE_RADII = {0.6, 0.35, 0.1};
    private static final float[] CAKE_ELEVATIONS = {80.0F, 85.0F, 88.0F};
    private static final double[] CAKE_SPEED_MULT = {0.6, 0.8, 1.0};

    private static final int[] VASE_COUNTS = {0, 0, 12};
    private static final double[] VASE_RADII = {0.0, 0.00, 0.2};
    private static final float[] VASE_ELEVATIONS = {0.0F, 0.0F, 85.0F};
    private static final double[] VASE_SPEED_MULT = {0.0, 0.0, 1.0};

    public record JetPreset(
            int interval,
            float yOffset,
            double speedMult,
            double fixedMaxHeight,
            JetVariant variant,
            boolean legacyParticle,
            float pitchOffsetDeg,
            boolean fixedUp,
            double smoothFactor
    ) {
    }

    public static final JetPreset WATER_JET = new JetPreset(8, 1.81F, 0.05, 37.0, null, true, 0.0F, false, 0.1);
    public static final JetPreset BIG = new JetPreset(6, 2.03F, 0.09, 0.0, JetVariant.JET1, false, 0.0F, false, 0.1);
    public static final JetPreset THIN = new JetPreset(8, 2.03F, 0.09, 0.0, JetVariant.JET3, false, 0.0F, false, 0.1);
    public static final JetPreset SPREAD = new JetPreset(8, 2.03F, 0.09, 0.0, JetVariant.JET2, false, 0.0F, false, 0.1);
    public static final JetPreset FOG = new JetPreset(4, 2.03F, 0.09, 0.0, JetVariant.JETFog, false, 0.0F, false, 0.1);
    public static final JetPreset CONE = new JetPreset(4, 2.03F, 0.09, 0.0, JetVariant.JETCone, false, 0.0F, false, 0.1);
    public static final JetPreset CENTRAL = new JetPreset(7, 2.03F, 0.09, 0.0, JetVariant.JET4, false, 0.0F, false, 0.1);
    public static final JetPreset BLOOM = new JetPreset(4, 2.03F, 0.09, 0.0, JetVariant.JETBloom, false, 0.0F, true, 0.1);
    public static final JetPreset MOVING = new JetPreset(8, 1.81F, 0.09, 0.0, JetVariant.JET3, false, 0.0F, false, 0.1);

    private WaterJetClientEffects() {
    }

    public static boolean isWaterJetFixture(BaseLightBlockEntity blockEntity) {
        return blockEntity instanceof WaterJetBlockEntity
                || blockEntity instanceof WaterJetBigBlockEntity
                || blockEntity instanceof WaterJetThinBlockEntity
                || blockEntity instanceof WaterJetSpreadBlockEntity
                || blockEntity instanceof WaterJetFogBlockEntity
                || blockEntity instanceof WaterJetConeBlockEntity
                || blockEntity instanceof WaterJetCentralBlockEntity
                || blockEntity instanceof WaterJetBloomBlockEntity
                || blockEntity instanceof MovingJetBlockEntity
                || blockEntity instanceof OrganPipesBlockEntity
                || blockEntity instanceof OrganPipesInvBlockEntity
                || blockEntity instanceof SpinnerBlockEntity
                || blockEntity instanceof FanWaterJetBlockEntity
                || blockEntity instanceof CakeWaterJetBlockEntity
                || blockEntity instanceof VaseWaterJetBlockEntity
                || blockEntity instanceof WaltzesWaterJetBlockEntity
                || blockEntity instanceof WaltzCurtainBlockEntity;
    }

    private static boolean usesWaterJetTiltConvention(BaseLightBlockEntity blockEntity) {
        return blockEntity instanceof WaterJetThinBlockEntity
                || blockEntity instanceof WaterJetSpreadBlockEntity
                || blockEntity instanceof WaterJetBigBlockEntity
                || blockEntity instanceof WaterJetCentralBlockEntity
                || blockEntity instanceof OrganPipesBlockEntity
                || blockEntity instanceof OrganPipesInvBlockEntity
                || blockEntity instanceof FanWaterJetBlockEntity;
    }

    public static void updateWaltzesClient(WaltzesWaterJetBlockEntity blockEntity) {
        if (resolveClientLevel(blockEntity) == null) {
            return;
        }
        float globalSwayTime = (blockEntity.getLevel().getGameTime() * 0.45F);
        blockEntity.prevAngle = blockEntity.currentAngle;

        float baseTilt = -25.0F + (blockEntity.getTilt() / 255.0F) * 50.0F;
        float targetAngle = baseTilt;

        if (blockEntity.swingChannel > 0) {
            float speed = (blockEntity.swingChannel / 255.0F) * 0.02F;
            float amplitude = 25.0F;
            targetAngle = baseTilt + (float) Math.sin(globalSwayTime * speed * 20.0F) * amplitude;
        }

        float alpha = 0.15F;
        blockEntity.currentAngle += (targetAngle - blockEntity.currentAngle) * alpha;

        float intensityNorm = blockEntity.getIntensity() / 255.0F;
        double targetHeight = intensityNorm * blockEntity.getJetHeight();
        blockEntity.smoothedHeight += (targetHeight - blockEntity.smoothedHeight) * 0.15;
    }

    public static void updateWaltzCurtainClient(WaltzCurtainBlockEntity blockEntity) {
        if (resolveClientLevel(blockEntity) == null) {
            return;
        }
        float globalSwayTime = (blockEntity.getLevel().getGameTime() * 0.45F);
        blockEntity.prevAngle = blockEntity.currentAngle;

        float baseTilt = -25.0F + (blockEntity.getTilt() / 255.0F) * 50.0F;
        float targetAngle = baseTilt;

        if (blockEntity.swingChannel > 0) {
            float speed = (blockEntity.swingChannel / 255.0F) * 0.02F;
            float amplitude = 25.0F;
            targetAngle = baseTilt + (float) Math.sin(globalSwayTime * speed * 20.0F) * amplitude;
        }

        float alpha = 0.15F;
        blockEntity.currentAngle += (targetAngle - blockEntity.currentAngle) * alpha;

        float intensityNorm = blockEntity.getIntensity() / 255.0F;
        double targetHeight = intensityNorm * blockEntity.getJetHeight();
        blockEntity.smoothedHeight += (targetHeight - blockEntity.smoothedHeight) * 0.15;
    }

    public static void spawnFromBeam(BaseLightBlockEntity blockEntity, float partialTick) {
        ClientLevel level = resolveClientLevel(blockEntity);
        if (level == null || blockEntity.getIntensity() <= 0) {
            return;
        }

        if (blockEntity instanceof WaterJetBlockEntity) {
            spawnJet(blockEntity, level, WATER_JET, partialTick);
        } else if (blockEntity instanceof WaterJetBigBlockEntity) {
            spawnJet(blockEntity, level, BIG, partialTick);
        } else if (blockEntity instanceof WaterJetThinBlockEntity) {
            spawnJet(blockEntity, level, THIN, partialTick);
        } else if (blockEntity instanceof WaterJetSpreadBlockEntity) {
            spawnJet(blockEntity, level, SPREAD, partialTick);
        } else if (blockEntity instanceof WaterJetFogBlockEntity) {
            spawnJet(blockEntity, level, FOG, partialTick);
        } else if (blockEntity instanceof WaterJetConeBlockEntity) {
            spawnJet(blockEntity, level, CONE, partialTick);
        } else if (blockEntity instanceof WaterJetCentralBlockEntity) {
            spawnJet(blockEntity, level, CENTRAL, partialTick);
        } else if (blockEntity instanceof WaterJetBloomBlockEntity) {
            spawnJet(blockEntity, level, BLOOM, partialTick);
        } else if (blockEntity instanceof MovingJetBlockEntity) {
            spawnJet(blockEntity, level, MOVING, partialTick);
        } else if (blockEntity instanceof OrganPipesBlockEntity organ) {
            spawnOrganPipes(organ, level, JetVariant.JET3, partialTick);
        } else if (blockEntity instanceof FanWaterJetBlockEntity fan) {
            spawnFanJets(fan, level, JetVariant.JET3, partialTick);
        } else if (blockEntity instanceof CakeWaterJetBlockEntity cake) {
            spawnCakeJets(cake, level, JetVariant.JET3, partialTick);
        } else if (blockEntity instanceof VaseWaterJetBlockEntity vase) {
            spawnVaseJets(vase, level, JetVariant.JET3, partialTick);
        } else if (blockEntity instanceof WaltzesWaterJetBlockEntity waltzes) {
            updateWaltzesClient(waltzes);
            spawnWaltzesParticles(waltzes, level, JetVariant.JET3, partialTick);
        } else if (blockEntity instanceof WaltzCurtainBlockEntity waltzcurtain) {
            updateWaltzCurtainClient(waltzcurtain);
            spawnWaltzCurtainParticles(waltzcurtain, level, JetVariant.JET3, partialTick);
        } else if (blockEntity instanceof OrganPipesInvBlockEntity organInv) {
            spawnOrganPipes(organInv, level, JetVariant.JET3, partialTick);
        } else if (blockEntity instanceof SpinnerBlockEntity spinner) {
            updateSpinnerClient(spinner);
            spawnSpinner(spinner, level, partialTick);
        }
    }

    public static void updateSpinnerClient(SpinnerBlockEntity blockEntity) {
        if (resolveClientLevel(blockEntity) == null) {
            return;
        }
        float intensityNorm = blockEntity.getIntensity() / 255.0F;
        double targetHeight = intensityNorm * blockEntity.getJetHeight();
        blockEntity.smoothedHeight += (targetHeight - blockEntity.smoothedHeight) * 0.15;
        blockEntity.updateSpin();
    }

    private static PoseStack getFixturePoseStack(BaseLightBlockEntity blockEntity, float partialTick) {
        PoseStack poseStack = new PoseStack();
        BlockState blockState = blockEntity.getBlockState();

        Direction facing = blockState.hasProperty(HangableBlock.FACING)
                ? blockState.getValue(HangableBlock.FACING)
                : Direction.NORTH;

        boolean isHanging = blockState.hasProperty(HangableBlock.HANGING)
                && blockState.getValue(HangableBlock.HANGING);

        float pan = blockEntity.getPrevPan()
                + (blockEntity.getPan() - blockEntity.getPrevPan()) * partialTick;

        float tilt = blockEntity.getPrevTilt()
                + (blockEntity.getTilt() - blockEntity.getPrevTilt()) * partialTick;

        /*
         * IMPORTANTE:
         * El renderer aplica FixtureMountTransform ANTES de facing/hanging.
         * Las partículas tienen que usar exactamente el mismo orden.
         */
        FixtureMountTransform.apply(poseStack, blockEntity);

        // ---------------------------------------------------------
        // Base fixture transform
        // ---------------------------------------------------------
        poseStack.translate(0.5F, 0.0F, 0.5F);

        // ---------------------------------------------------------
        // Hanging transform
        // ---------------------------------------------------------
        if (isHanging && blockState.hasProperty(HangableBlock.HANG_DIRECTION)) {
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);

            poseStack.translate(0.0, 0.5, 0.0);

            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    if (hangDirection == Direction.SOUTH) {
                        poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    } else {
                        poseStack.mulPose(Axis.XN.rotationDegrees(90));
                    }
                } else {
                    if (hangDirection == Direction.EAST) {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(90));
                    } else {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(-90));
                    }
                }
            }

            poseStack.translate(0.0, -0.5, 0.0);
        }

        // ---------------------------------------------------------
        // Facing
        // ---------------------------------------------------------
        if (facing.getAxis() == Direction.Axis.X) {
            poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(facing.getOpposite().toYRot()));
        }

        poseStack.translate(-0.5F, 0.0F, -0.5F);

        // ---------------------------------------------------------
        // Hanging support transform
        // ---------------------------------------------------------
        if (isHanging) {
            if (blockEntity instanceof ExtraLightsLightBlockEntity extra) {
                Optional<BlockState> optionalSupport = extra.getSupportingStructure();

                if (optionalSupport != null && optionalSupport.isPresent()) {
                    float[] transforms =
                            extra.getFixture().getTransforms(blockState, optionalSupport.get());

                    poseStack.translate(
                            transforms[0],
                            transforms[1],
                            transforms[2]
                    );
                } else {
                    poseStack.translate(0.0F, 0.19F, 0.0F);
                }
            } else {
                poseStack.translate(0.0F, 0.19F, 0.0F);
            }
        }

        // ---------------------------------------------------------
        // Pan
        // ---------------------------------------------------------
        float[] pans = blockEntity.getFixture().getPanRotationPosition();

        poseStack.translate(pans[0], pans[1], pans[2]);
        poseStack.mulPose(Axis.YN.rotationDegrees(pan));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);

        // ---------------------------------------------------------
        // Tilt
        // ---------------------------------------------------------
        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();

        poseStack.translate(tilts[0], tilts[1], tilts[2]);
        poseStack.mulPose(Axis.XP.rotationDegrees(tilt));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);

        return poseStack;
    }

    private static Vec3 transformPoint(Matrix4f matrix, double x, double y, double z) {
        Vector4f vec = new Vector4f((float) x, (float) y, (float) z, 1.0F);
        vec.mul(matrix);
        return new Vec3(vec.x(), vec.y(), vec.z());
    }

    private static Vec3 transformDirection(Matrix4f matrix, double vx, double vy, double vz) {
        Vector4f vec = new Vector4f((float) vx, (float) vy, (float) vz, 0.0F);
        vec.mul(matrix);
        return new Vec3(vec.x(), vec.y(), vec.z());
    }

    private static void spawnJet(BaseLightBlockEntity blockEntity, ClientLevel level, JetPreset preset, float partialTick) {
        double maxHeight = preset.fixedMaxHeight() > 0.0
                ? preset.fixedMaxHeight()
                : ((HasJetHeight) blockEntity).getJetHeight();
        double targetHeight = (blockEntity.getIntensity() / 255.0) * maxHeight;
        double smoothed = readSmoothedHeight(blockEntity);
        smoothed += (targetHeight - smoothed) * preset.smoothFactor();
        writeSmoothedHeight(blockEntity, smoothed);
        double speed = smoothed * preset.speedMult();

        Matrix4f pose = getFixturePoseStack(blockEntity, partialTick).last().pose();

        Vec3 posWorld = transformPoint(pose, 0.5, preset.yOffset(), 0.5);
        double x = blockEntity.getBlockPos().getX() + posWorld.x;
        double y = blockEntity.getBlockPos().getY() + posWorld.y;
        double z = blockEntity.getBlockPos().getZ() + posWorld.z;

        if (!isNearPlayer(level, x, y, z)) {
            return;
        }

        Vec3 vel;
        if (preset.fixedUp()) {
            vel = new Vec3(0.0, speed, 0.0);
        } else {
            // Dirección local apuntando hacia ARRIBA (+Y)
            double localDirX;
            double localDirY;
            double localDirZ;

            if (usesWaterJetTiltConvention(blockEntity)) {
                // Estos waterjets tienen 0° horizontal y +90° vertical.
                localDirX = 0.0;
                localDirY = 0.0;
                localDirZ = -1.0;
            } else {
                // NO tocar los demás fixtures.
                double pitchRad = Math.toRadians(preset.pitchOffsetDeg());
                localDirX = 0.0;
                localDirY = Math.cos(pitchRad);
                localDirZ = Math.sin(pitchRad);
            }

            Vec3 dirWorld = transformDirection(pose, localDirX, localDirY, localDirZ);
            vel = dirWorld.scale(speed);
        }

        if (preset.legacyParticle()) {
            emit(level, ModParticle.WATERJETPARTICLE.get(), x, y, z, vel.x, vel.y, vel.z);
            return;
        }

        float intensity = blockEntity.getIntensity() / 255.0F;
        float thickness = blockEntity instanceof HasJetThickness jetThickness ? jetThickness.getJetThickness() : 0.12F;
        float coneAngle = blockEntity instanceof HasJetConeAngle jetCone ? jetCone.getJetConeAngle() : 45.0F;
        WaterJetParticleOptions options = new WaterJetParticleOptions(intensity, thickness, coneAngle, preset.variant());

        emit(level, options, x, y, z, vel.x, vel.y, vel.z);
    }

    private static void spawnOrganPipes(BaseLightBlockEntity blockEntity, ClientLevel level, JetVariant variant, float partialTick) {
        if (!(blockEntity instanceof HasJetHeight hasJetHeight) || !(blockEntity instanceof HasJetThickness hasJetThickness)) {
            return;
        }

        boolean isInv = blockEntity instanceof OrganPipesInvBlockEntity;
        double[] currentHeightMult = isInv ? ORGAN_PIPE_INV_HEIGHT_MULT : ORGAN_PIPE_HEIGHT_MULT;

        double maxHeight = hasJetHeight.getJetHeight();
        double targetHeight = (blockEntity.getIntensity() / 255.0) * maxHeight;
        double smoothed = getOrganSmoothed(blockEntity);
        smoothed += (targetHeight - smoothed) * 0.1;
        setOrganSmoothed(blockEntity, smoothed);

        double baseSpeed = smoothed * 0.09;
        float intensity = blockEntity.getIntensity() / 255.0F;
        float thickness = hasJetThickness.getJetThickness();

        Matrix4f pose = getFixturePoseStack(blockEntity, partialTick).last().pose();
        Vec3 dirWorld = transformDirection(pose, 0.0, 0.0, -1.0);

        for (int i = 0; i < ORGAN_PIPE_X.length; i++) {
            double localX = 0.5 + ORGAN_PIPE_X[i];
            double localY = 2.0;
            double localZ = 0.5;

            Vec3 posWorld = transformPoint(pose, localX, localY, localZ);
            double particleX = blockEntity.getBlockPos().getX() + posWorld.x;
            double particleY = blockEntity.getBlockPos().getY() + posWorld.y;
            double particleZ = blockEntity.getBlockPos().getZ() + posWorld.z;

            if (!isNearPlayer(level, particleX, particleY, particleZ)) {
                continue;
            }

            double particleSpeed = baseSpeed * currentHeightMult[i];
            WaterJetParticleOptions options = new WaterJetParticleOptions(
                    intensity * (float) currentHeightMult[i],
                    thickness,
                    variant
            );
            emit(level, options, particleX, particleY, particleZ,
                    dirWorld.x * particleSpeed, dirWorld.y * particleSpeed, dirWorld.z * particleSpeed);
        }
    }

    private static void spawnCakeJets(BaseLightBlockEntity blockEntity, ClientLevel level, JetVariant variant, float partialTick) {
        if (!(blockEntity instanceof HasJetHeight hasJetHeight) || !(blockEntity instanceof HasJetThickness hasJetThickness)) {
            return;
        }

        double maxHeight = hasJetHeight.getJetHeight();
        double targetHeight = (blockEntity.getIntensity() / 255.0) * maxHeight;

        double smoothed = readSmoothedHeight(blockEntity);
        smoothed += (targetHeight - smoothed) * 0.1;
        writeSmoothedHeight(blockEntity, smoothed);

        double baseSpeed = smoothed * 0.09;
        float intensity = blockEntity.getIntensity() / 255.0F;
        float thickness = hasJetThickness.getJetThickness();

        Matrix4f pose = getFixturePoseStack(blockEntity, partialTick).last().pose();

        for (int t = 0; t < 3; t++) {
            int count = CAKE_COUNTS[t];
            double radius = CAKE_RADII[t];
            float elevation = CAKE_ELEVATIONS[t];
            double speedMult = CAKE_SPEED_MULT[t];

            double elevationRad = Math.toRadians(elevation);
            double verticalForce = Math.sin(elevationRad);
            double horizontalForce = Math.cos(elevationRad);

            for (int i = 0; i < count; i++) {
                float angleDeg = (360.0F / count) * i;
                if (t == 1) angleDeg += (360.0F / count) / 2.0F;

                double angleRad = Math.toRadians(angleDeg);

                double localX = 0.5 + Math.cos(angleRad) * radius;
                double localY = 2.0;
                double localZ = 0.5 + Math.sin(angleRad) * radius;

                double dirX = Math.cos(angleRad) * horizontalForce;
                double dirY = verticalForce;
                double dirZ = Math.sin(angleRad) * horizontalForce;

                Vec3 posWorld = transformPoint(pose, localX, localY, localZ);
                Vec3 dirWorld = transformDirection(pose, dirX, dirY, dirZ);

                double spawnX = blockEntity.getBlockPos().getX() + posWorld.x;
                double spawnY = blockEntity.getBlockPos().getY() + posWorld.y;
                double spawnZ = blockEntity.getBlockPos().getZ() + posWorld.z;

                if (!isNearPlayer(level, spawnX, spawnY, spawnZ)) {
                    continue;
                }

                double particleSpeed = baseSpeed * speedMult;
                WaterJetParticleOptions options = new WaterJetParticleOptions(
                        intensity * (float) speedMult,
                        thickness,
                        variant
                );
                emit(level, options, spawnX, spawnY, spawnZ,
                        dirWorld.x * particleSpeed, dirWorld.y * particleSpeed, dirWorld.z * particleSpeed);
            }
        }
    }

    private static void spawnVaseJets(BaseLightBlockEntity blockEntity, ClientLevel level, JetVariant variant, float partialTick) {
        if (!(blockEntity instanceof HasJetHeight hasJetHeight) || !(blockEntity instanceof HasJetThickness hasJetThickness)) {
            return;
        }

        double maxHeight = hasJetHeight.getJetHeight();
        double targetHeight = (blockEntity.getIntensity() / 255.0) * maxHeight;

        double smoothed = readSmoothedHeight(blockEntity);
        smoothed += (targetHeight - smoothed) * 0.1;
        writeSmoothedHeight(blockEntity, smoothed);

        double baseSpeed = smoothed * 0.09;
        float intensity = blockEntity.getIntensity() / 255.0F;
        float thickness = hasJetThickness.getJetThickness();

        Matrix4f pose = getFixturePoseStack(blockEntity, partialTick).last().pose();

        for (int t = 0; t < 3; t++) {
            int count = VASE_COUNTS[t];
            double radius = VASE_RADII[t];
            float elevation = VASE_ELEVATIONS[t];
            double speedMult = VASE_SPEED_MULT[t];

            double elevationRad = Math.toRadians(elevation);
            double verticalForce = Math.sin(elevationRad);
            double horizontalForce = Math.cos(elevationRad);

            for (int i = 0; i < count; i++) {
                float angleDeg = (360.0F / count) * i;
                if (t == 1) angleDeg += (360.0F / count) / 2.0F;

                double angleRad = Math.toRadians(angleDeg);

                double localX = 0.5 + Math.cos(angleRad) * radius;
                double localY = 2.0;
                double localZ = 0.5 + Math.sin(angleRad) * radius;

                double dirX = Math.cos(angleRad) * horizontalForce;
                double dirY = verticalForce;
                double dirZ = Math.sin(angleRad) * horizontalForce;

                Vec3 posWorld = transformPoint(pose, localX, localY, localZ);
                Vec3 dirWorld = transformDirection(pose, dirX, dirY, dirZ);

                double spawnX = blockEntity.getBlockPos().getX() + posWorld.x;
                double spawnY = blockEntity.getBlockPos().getY() + posWorld.y;
                double spawnZ = blockEntity.getBlockPos().getZ() + posWorld.z;

                if (!isNearPlayer(level, spawnX, spawnY, spawnZ)) {
                    continue;
                }

                double particleSpeed = baseSpeed * speedMult;
                WaterJetParticleOptions options = new WaterJetParticleOptions(
                        intensity * (float) speedMult,
                        thickness,
                        variant
                );
                emit(level, options, spawnX, spawnY, spawnZ,
                        dirWorld.x * particleSpeed, dirWorld.y * particleSpeed, dirWorld.z * particleSpeed);
            }
        }
    }

    private static void spawnFanJets(BaseLightBlockEntity blockEntity, ClientLevel level, JetVariant variant, float partialTick) {
        if (!(blockEntity instanceof HasJetHeight hasJetHeight) || !(blockEntity instanceof HasJetThickness hasJetThickness)) {
            return;
        }

        double maxHeight = hasJetHeight.getJetHeight();
        double targetHeight = (blockEntity.getIntensity() / 255.0) * maxHeight;

        double smoothed = readSmoothedHeight(blockEntity);
        smoothed += (targetHeight - smoothed) * 0.1;
        writeSmoothedHeight(blockEntity, smoothed);

        double baseSpeed = smoothed * 0.09;
        float intensity = blockEntity.getIntensity() / 255.0F;
        float thickness = hasJetThickness.getJetThickness();

        Matrix4f pose = getFixturePoseStack(blockEntity, partialTick).last().pose();

        for (int i = 0; i < FAN_OFFSETS_X.length; i++) {
            double localX = 0.5 + FAN_OFFSETS_X[i];
            double localY = 2.0;
            double localZ = 0.5;

            double angleRad = Math.toRadians(FAN_ANGLES[i]);
            double dirX = Math.sin(angleRad);
            double dirY = 0.0;
            double dirZ = -Math.cos(angleRad);

            Vec3 posWorld = transformPoint(pose, localX, localY, localZ);
            Vec3 dirWorld = transformDirection(pose, dirX, dirY, dirZ);

            double spawnX = blockEntity.getBlockPos().getX() + posWorld.x;
            double spawnY = blockEntity.getBlockPos().getY() + posWorld.y;
            double spawnZ = blockEntity.getBlockPos().getZ() + posWorld.z;

            if (!isNearPlayer(level, spawnX, spawnY, spawnZ)) {
                continue;
            }

            double particleSpeed = baseSpeed * FAN_HEIGHT_MULT[i];

            WaterJetParticleOptions options = new WaterJetParticleOptions(
                    intensity * (float) FAN_HEIGHT_MULT[i],
                    thickness,
                    variant
            );

            emit(level, options, spawnX, spawnY, spawnZ,
                    dirWorld.x * particleSpeed, dirWorld.y * particleSpeed, dirWorld.z * particleSpeed);
        }
    }

    public static void spawnWaltzesParticles(WaltzesWaterJetBlockEntity blockEntity, ClientLevel level, JetVariant variant, float partialTicks) {
        float intensityNorm = blockEntity.getIntensity() / 255.0f;
        float smoothAngle = blockEntity.getRenderAngle(partialTicks);
        double tiltRad = Math.toRadians(smoothAngle);

        Matrix4f pose = getFixturePoseStack(blockEntity, partialTicks).last().pose();

        double speed = blockEntity.smoothedHeight * 0.1;
        double localDirY = Math.sin(tiltRad);
        double localDirZ = -Math.cos(tiltRad);
        Vec3 dirWorld = transformDirection(pose, 0.0, localDirY, localDirZ);

        double[] xOffsets = {-1.4375, -1.125, -0.75, -0.375, 0.0, 0.375, 0.75, 1.125, 1.4375};

        for (double xOffset : xOffsets) {
            Vec3 posWorld = transformPoint(pose, 0.5 + xOffset, 2.0, 0.5);

            double finalX = blockEntity.getBlockPos().getX() + posWorld.x;
            double finalY = blockEntity.getBlockPos().getY() + posWorld.y;
            double finalZ = blockEntity.getBlockPos().getZ() + posWorld.z;

            if (!isNearPlayer(level, finalX, finalY, finalZ)) {
                continue;
            }

            emit(level,
                    new WaterJetParticleOptions(intensityNorm, blockEntity.getJetThickness(), variant),
                    finalX, finalY, finalZ,
                    dirWorld.x * speed, dirWorld.y * speed, dirWorld.z * speed
            );
        }
    }

    public static void spawnWaltzCurtainParticles(WaltzCurtainBlockEntity blockEntity, ClientLevel level, JetVariant variant, float partialTicks) {
        float intensityNorm = blockEntity.getIntensity() / 255.0f;
        float smoothAngle = blockEntity.getRenderAngle(partialTicks);
        double tiltRad = Math.toRadians(smoothAngle);

        Matrix4f pose = getFixturePoseStack(blockEntity, partialTicks).last().pose();

        double speed = blockEntity.smoothedHeight * 0.1;
        double localDirX = Math.sin(tiltRad);
        double localDirZ = -Math.cos(tiltRad);
        Vec3 dirWorld = transformDirection(pose, localDirX, 0.0, localDirZ);

        double[] xOffsets = {-0.375, 0.0, 0.375};

        for (double xOffset : xOffsets) {
            Vec3 posWorld = transformPoint(pose, 0.5 + xOffset, 2.0, 0.5);

            double finalX = blockEntity.getBlockPos().getX() + posWorld.x;
            double finalY = blockEntity.getBlockPos().getY() + posWorld.y;
            double finalZ = blockEntity.getBlockPos().getZ() + posWorld.z;

            if (!isNearPlayer(level, finalX, finalY, finalZ)) {
                continue;
            }

            emit(level,
                    new WaterJetParticleOptions(intensityNorm, blockEntity.getJetThickness(), variant),
                    finalX, finalY, finalZ,
                    dirWorld.x * speed, dirWorld.y * speed, dirWorld.z * speed
            );
        }
    }

    private static void spawnSpinner(SpinnerBlockEntity blockEntity, ClientLevel level, float partialTick) {
        double speed = blockEntity.smoothedHeight * 0.10;
        float intensity = blockEntity.getIntensity() / 255.0F;
        float thickness = blockEntity.getJetThickness();
        WaterJetParticleOptions options = new WaterJetParticleOptions(intensity, thickness, JetVariant.JET3);

        Matrix4f pose = getFixturePoseStack(blockEntity, partialTick).last().pose();

        float angle = blockEntity.getSpinAngle() % 360.0F;
        double rad = Math.toRadians(angle);
        double nozzleRad = Math.toRadians(blockEntity.getNozzleAngle());

        for (double[] nozzle : SPINNER_NOZZLES) {
            double offsetX = nozzle[0] - 0.5;
            double offsetZ = nozzle[2] - 0.5;
            double rotatedX = offsetX * Math.cos(rad) - offsetZ * Math.sin(rad);
            double rotatedZ = offsetX * Math.sin(rad) + offsetZ * Math.cos(rad);

            Vec3 posWorld = transformPoint(pose, 0.5 + rotatedX, nozzle[1], 0.5 + rotatedZ);
            double worldX = blockEntity.getBlockPos().getX() + posWorld.x;
            double worldY = blockEntity.getBlockPos().getY() + posWorld.y;
            double worldZ = blockEntity.getBlockPos().getZ() + posWorld.z;

            if (!isNearPlayer(level, worldX, worldY, worldZ)) {
                continue;
            }

            double dirX = rotatedX;
            double dirZ = rotatedZ;
            double dirLength = Math.sqrt(dirX * dirX + dirZ * dirZ);
            if (dirLength > 0.001) {
                dirX /= dirLength;
                dirZ /= dirLength;
            } else {
                dirX = 0.0;
                dirZ = 0.0;
            }

            double velAlongAxis = Math.cos(nozzleRad);
            double horizontalSpeed = Math.sin(nozzleRad);

            Vec3 dirWorld = transformDirection(pose, dirX * horizontalSpeed, velAlongAxis, dirZ * horizontalSpeed);

            emit(level, options, worldX, worldY, worldZ, dirWorld.x * speed, dirWorld.y * speed, dirWorld.z * speed);
        }
    }

    private static double readSmoothedHeight(BaseLightBlockEntity blockEntity) {
        if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.WaterJetBlockEntity e) {
            return e.smoothedHeight;
        }
        if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.WaterJetBigBlockEntity e) {
            return e.smoothedHeight;
        }
        if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.WaterJetThinBlockEntity e) {
            return e.smoothedHeight;
        }
        if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.WaterJetSpreadBlockEntity e) {
            return e.smoothedHeight;
        }
        if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.WaterJetFogBlockEntity e) {
            return e.smoothedHeight;
        }
        if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.WaterJetConeBlockEntity e) {
            return e.smoothedHeight;
        }
        if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.WaterJetCentralBlockEntity e) {
            return e.smoothedHeight;
        }
        if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.WaterJetBloomBlockEntity e) {
            return e.smoothedHeight;
        }
        if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.MovingJetBlockEntity e) {
            return e.smoothedHeight;
        }
        if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.FanWaterJetBlockEntity e) {
            return e.smoothedHeight;
        }
        if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.CakeWaterJetBlockEntity e) {
            return e.smoothedHeight;
        }
        if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.VaseWaterJetBlockEntity e) {
            return e.smoothedHeight;
        }
        if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.WaltzesWaterJetBlockEntity e) {
            return e.smoothedHeight;
        }
        return 0.0;
    }

    private static void writeSmoothedHeight(BaseLightBlockEntity blockEntity, double value) {
        if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.WaterJetBlockEntity e) {
            e.smoothedHeight = value;
        } else if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.WaterJetBigBlockEntity e) {
            e.smoothedHeight = value;
        } else if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.WaterJetThinBlockEntity e) {
            e.smoothedHeight = value;
        } else if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.WaterJetSpreadBlockEntity e) {
            e.smoothedHeight = value;
        } else if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.WaterJetFogBlockEntity e) {
            e.smoothedHeight = value;
        } else if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.WaterJetConeBlockEntity e) {
            e.smoothedHeight = value;
        } else if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.WaterJetCentralBlockEntity e) {
            e.smoothedHeight = value;
        } else if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.WaterJetBloomBlockEntity e) {
            e.smoothedHeight = value;
        } else if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.MovingJetBlockEntity e) {
            e.smoothedHeight = value;
        } else if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.FanWaterJetBlockEntity e) {
            e.smoothedHeight = value;
        } else if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.CakeWaterJetBlockEntity e) {
            e.smoothedHeight = value;
        } else if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.VaseWaterJetBlockEntity e) {
            e.smoothedHeight = value;
        } else if (blockEntity instanceof com.github.dumann089.theatricalextralights.blockentities.WaltzesWaterJetBlockEntity e) {
            e.smoothedHeight = value;
        }
    }

    private static ClientLevel resolveClientLevel(BaseLightBlockEntity blockEntity) {
        if (Minecraft.getInstance().isPaused()) {
            return null;
        }
        net.minecraft.world.level.Level level = blockEntity.getLevel();
        if (level == null || !level.isClientSide()) {
            return null;
        }
        return (ClientLevel) level;
    }

    private static void emit(ClientLevel level, ParticleOptions options, double x, double y, double z, double vx, double vy, double vz) {
        level.addAlwaysVisibleParticle(options, true, x, y, z, vx, vy, vz);
    }

    private static boolean isNearPlayer(ClientLevel level, double x, double y, double z) {
        if (Minecraft.getInstance().player == null) {
            return false;
        }
        double dx = x - Minecraft.getInstance().player.getX();
        double dy = y - Minecraft.getInstance().player.getY();
        double dz = z - Minecraft.getInstance().player.getZ();
        return dx * dx + dy * dy + dz * dz <= MAX_SPAWN_DISTANCE_SQ;
    }

    private static double getOrganSmoothed(BaseLightBlockEntity blockEntity) {
        if (blockEntity instanceof OrganPipesBlockEntity organ) {
            return organ.smoothedHeight;
        }
        return ((OrganPipesInvBlockEntity) blockEntity).smoothedHeight;
    }

    private static void setOrganSmoothed(BaseLightBlockEntity blockEntity, double value) {
        if (blockEntity instanceof OrganPipesBlockEntity organ) {
            organ.smoothedHeight = value;
        } else {
            ((OrganPipesInvBlockEntity) blockEntity).smoothedHeight = value;
        }
    }
}