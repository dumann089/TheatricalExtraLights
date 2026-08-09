package com.github.dumann089.theatricalextralights.util;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import net.minecraft.util.Mth;

/**
 * Applies user-configurable mount offsets before fixture facing / pan / tilt transforms.
 */
public final class FixtureMountTransform {

    public static final float MAX_OFFSET = 1.5f;
    public static final float MAX_ANGLE = 180f;

    private FixtureMountTransform() {
    }

    public static float clampOffset(float value) {
        return Mth.clamp(value, -MAX_OFFSET, MAX_OFFSET);
    }

    public static float clampAngle(float value) {
        return Mth.clamp(value, -MAX_ANGLE, MAX_ANGLE);
    }

    public static void apply(PoseStack poseStack, BaseLightBlockEntity blockEntity) {
        if (!(blockEntity instanceof ExtraLightsLightBlockEntity mountable) || !mountable.hasMountTransform()) {
            return;
        }

        float offsetX = mountable.getMountOffsetX();
        float offsetY = mountable.getMountOffsetY();
        float offsetZ = mountable.getMountOffsetZ();
        float yaw = mountable.getMountYaw();
        float pitch = mountable.getMountPitch();
        float roll = mountable.getMountRoll();

        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (yaw != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        }
        if (pitch != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        }
        if (roll != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        }
        poseStack.translate(offsetX, offsetY, offsetZ);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }
}
