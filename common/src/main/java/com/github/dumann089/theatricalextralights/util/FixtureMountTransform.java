package com.github.dumann089.theatricalextralights.util;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import net.minecraft.world.phys.Vec3;

/**
 * Applies user-configurable mount offsets before fixture facing / pan / tilt transforms.
 */
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
        poseStack.translate(offsetX, offsetY, offsetZ);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }

    public static Vec3 applyToPoint(BaseLightBlockEntity blockEntity, Vec3 point) {
        if (!(blockEntity instanceof ExtraLightsLightBlockEntity mountable)
                || !mountable.hasMountTransform()) {
            return point;
        }

    /** World-space point after the same mount transform used for rendering. */
    public static Vec3 transformWorldPoint(ExtraLightsLightBlockEntity mountable, BlockPos pos, Vec3 worldPoint) {
        if (!mountable.hasMountTransform()) {
            return worldPoint;
        }
        Matrix4f m = buildMatrix(mountable);
        Vector4f v = m.transform(new Vector4f(
                (float) (worldPoint.x - pos.getX()),
                (float) (worldPoint.y - pos.getY()),
                (float) (worldPoint.z - pos.getZ()),
                1.0f
        ));
        return new Vec3(pos.getX() + v.x, pos.getY() + v.y, pos.getZ() + v.z);
    }

    /** Direction after mount rotation (offsets ignored). */
    public static Vector3f transformDirection(ExtraLightsLightBlockEntity mountable, Vector3f direction) {
        if (!mountable.hasMountTransform()) {
            return direction;
        }
        Matrix4f m = buildMatrix(mountable);
        Vector4f v = m.transform(new Vector4f(direction.x, direction.y, direction.z, 0.0f));
        Vector3f out = new Vector3f(v.x, v.y, v.z);
        if (out.lengthSquared() > 1.0e-8f) {
            out.normalize();
        }
        return out;
    }

    private static Matrix4f buildMatrix(ExtraLightsLightBlockEntity mountable) {
        Matrix4f m = new Matrix4f().identity();
        m.translate(0.5f, 0.5f, 0.5f);
        float yaw = mountable.getMountYaw();
        float pitch = mountable.getMountPitch();
        float roll = mountable.getMountRoll();
        if (yaw != 0.0F) {
            m.rotateY((float) Math.toRadians(yaw));
        }
        if (pitch != 0.0F) {
            m.rotateX((float) Math.toRadians(pitch));
        }
        if (roll != 0.0F) {
            m.rotateZ((float) Math.toRadians(roll));
        }
        m.translate(mountable.getMountOffsetX(), mountable.getMountOffsetY(), mountable.getMountOffsetZ());
        m.translate(-0.5f, -0.5f, -0.5f);
        return m;
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