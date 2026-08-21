package com.github.dumann089.theatricalextralights.util;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class FixtureMountTransform {

    public static final float MAX_OFFSET = 1.5F;
    public static final float MAX_ANGLE = 180.0F;

    private FixtureMountTransform() {
    }

    public static float clampOffset(float value) {
        return Mth.clamp(value, -MAX_OFFSET, MAX_OFFSET);
    }

    public static float clampAngle(float value) {
        return Mth.clamp(value, -MAX_ANGLE, MAX_ANGLE);
    }

    /**
     * Applies the mount transform to an already-positioned fixture.
     *
     * The caller is responsible for applying the fixture's own
     * position/facing/hanging transforms before calling this method.
     */
    public static void apply(PoseStack poseStack, BaseLightBlockEntity blockEntity) {
        if (!(blockEntity instanceof ExtraLightsLightBlockEntity mountable)
                || !mountable.hasMountTransform()) {
            return;
        }

        float offsetX = mountable.getMountOffsetX();
        float offsetY = mountable.getMountOffsetY();
        float offsetZ = mountable.getMountOffsetZ();

        float yaw = mountable.getMountYaw();
        float pitch = mountable.getMountPitch();
        float roll = mountable.getMountRoll();

        /*
         * Position offset.
         *
         * At this point the fixture has already been oriented by its
         * own renderer, so the offset belongs to the fixture transform.
         */
        poseStack.translate(offsetX, offsetY, offsetZ);

        /*
         * Mount rotation around the fixture origin.
         */
        if (yaw != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        }

        if (pitch != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        }

        if (roll != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        }
    }

    public static Vec3 applyToPoint(BaseLightBlockEntity blockEntity, Vec3 point) {
        if (!(blockEntity instanceof ExtraLightsLightBlockEntity mountable)
                || !mountable.hasMountTransform()) {
            return point;
        }

        double x = point.x - 0.5;
        double y = point.y - 0.5;
        double z = point.z - 0.5;

        float yaw = mountable.getMountYaw();
        float pitch = mountable.getMountPitch();
        float roll = mountable.getMountRoll();

        double r;

        // CORRECCIÓN: Signos invertidos en Yaw para coincidir con la matriz JOML de Minecraft (Axis.YP)
        r = Math.toRadians(yaw);
        double nx = x * Math.cos(r) + z * Math.sin(r);
        double nz = -x * Math.sin(r) + z * Math.cos(r);
        x = nx;
        z = nz;

        r = Math.toRadians(pitch);
        double ny = y * Math.cos(r) - z * Math.sin(r);
        nz = y * Math.sin(r) + z * Math.cos(r);
        y = ny;
        z = nz;

        r = Math.toRadians(roll);
        nx = x * Math.cos(r) - y * Math.sin(r);
        ny = x * Math.sin(r) + y * Math.cos(r);
        x = nx;
        y = ny;

        return new Vec3(
                x + 0.5 + mountable.getMountOffsetX(),
                y + 0.5 + mountable.getMountOffsetY(),
                z + 0.5 + mountable.getMountOffsetZ()
        );
    }
}