package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import com.github.dumann089.theatricalextralights.client.gobo.GoboDirectLightManager;
import com.github.dumann089.theatricalextralights.client.render.beam.BeamRenderData;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.util.FramingShutterState;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class GelScrollerGoboProjector {
    private static final ResourceLocation OPEN_GOBO = new ResourceLocation(
            "theatricalextralights",
            "textures/gobos/generic_1/2wash.png"
    );

    private static final double RAY_START_OFFSET = 0.15D;
    private static final float BASE_RADIUS = 0.58F;
    private static final float HALF_ANGLE_DEGREES = 25.0F;
    private static final float FOCUS = 0.95F;

    private GelScrollerGoboProjector() {
    }

    public static void submit(
            ExtraLightsLightBlockEntity blockEntity,
            PoseStack fixturePose,
            float localX,
            float localY,
            float localZ,
            int[] gelRgb,
            float intensity
    ) {
        if (blockEntity == null
                || fixturePose == null
                || gelRgb == null
                || gelRgb.length < 3
                || blockEntity.getLevel() == null
                || !blockEntity.getLevel().isClientSide
                || !Float.isFinite(intensity)
                || intensity <= 0.0F) {
            return;
        }

        Level level = blockEntity.getLevel();
        Vec3 direction = BaseLightBlockEntity.rayTraceDir(blockEntity);

        if (direction == null || direction.lengthSqr() <= 1.0E-8D) {
            return;
        }

        direction = direction.normalize();

        Vector3f localPosition = fixturePose.last().pose().transformPosition(
                localX,
                localY,
                localZ,
                new Vector3f()
        );

        BlockPos blockPos = blockEntity.getBlockPos();

        Vec3 origin = new Vec3(
                blockPos.getX() + localPosition.x,
                blockPos.getY() + localPosition.y,
                blockPos.getZ() + localPosition.z
        ).add(direction.scale(RAY_START_OFFSET));

        float configuredDistance = TheatricalExtraLightsConfig.getMaxGoboDistance();

        float maxDistance = Float.isFinite(configuredDistance)
                ? Math.max(1.0F, Math.min(64.0F, configuredDistance))
                : 64.0F;

        Vec3 rayEnd = origin.add(direction.scale(maxDistance));

        BlockHitResult hit = level.clip(new ClipContext(
                origin,
                rayEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
        ));

        boolean hasOcclusion = hit.getType() == HitResult.Type.BLOCK
                && !hit.isInside();

        Vec3 occlusionPosition = hasOcclusion
                ? hit.getLocation()
                : Vec3.ZERO;

        Direction hitDirection = hasOcclusion
                ? hit.getDirection()
                : Direction.UP;

        Vec3 occlusionNormal = hasOcclusion
                ? new Vec3(
                hitDirection.getStepX(),
                hitDirection.getStepY(),
                hitDirection.getStepZ()
        ).normalize()
                : Vec3.ZERO;

        Vec3 referenceUp = Math.abs(direction.y) > 0.95D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(0.0D, 1.0D, 0.0D);

        Vec3 axisU = direction.cross(referenceUp).normalize();
        Vec3 axisV = direction.cross(axisU).normalize();

        if (axisU.lengthSqr() <= 1.0E-8D
                || axisV.lengthSqr() <= 1.0E-8D) {
            return;
        }

        int red = clampChannel(gelRgb[0]);
        int green = clampChannel(gelRgb[1]);
        int blue = clampChannel(gelRgb[2]);
        int color = (red << 16) | (green << 8) | blue;

        float normalizedIntensity = Math.max(
                0.0F,
                Math.min(1.0F, intensity / 255.0F)
        );

        float tanHalfAngle = (float) Math.tan(
                Math.toRadians(HALF_ANGLE_DEGREES)
        );

        GoboDirectLightManager.submit(
                origin,
                direction,
                axisU,
                axisV,
                BASE_RADIUS,
                tanHalfAngle,
                maxDistance,
                0.0F,
                color,
                normalizedIntensity,
                OPEN_GOBO,
                OPEN_GOBO,
                0.0F,
                FOCUS,
                (FramingShutterState.Snapshot) null,
                (BeamRenderData.Animation) null,
                hasOcclusion,
                occlusionPosition,
                occlusionNormal
        );
    }

    private static int clampChannel(int channel) {
        return Math.max(0, Math.min(255, channel));
    }
}