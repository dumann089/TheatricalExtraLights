package com.github.dumann089.theatricalextralights.util;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import com.github.dumann089.theatricalextralights.compat.SupportCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Applies user-configurable mount offsets before fixture facing / pan / tilt transforms.
 */
public final class FixtureMountTransform {

    public static final float MAX_OFFSET = 2.5f;
    public static final float MAX_ANGLE = 180f;

    /**
     * CSC truss chords sit lower than Theatrical's truss centre. The fixture transform was
     * authored against Theatrical trusses, so on CSC the body ended up a little too high; drop it
     * by a quarter block while keeping it centred on its own block.
     */
    public static final float CSC_MOUNT_DROP = 0.25f;

    private FixtureMountTransform() {
    }

    public static float clampOffset(float value) {
        return Mth.clamp(value, -MAX_OFFSET, MAX_OFFSET);
    }

    public static float clampAngle(float value) {
        return Mth.clamp(value, -MAX_ANGLE, MAX_ANGLE);
    }

    public static void apply(PoseStack poseStack, BaseLightBlockEntity blockEntity) {
        if (!(blockEntity instanceof ExtraLightsLightBlockEntity mountable)) {
            return;
        }

        if (mountable.hasMountTransform()) {
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

        if (isOnCscTruss(mountable)) {
            poseStack.translate(0.0F, -CSC_MOUNT_DROP, 0.0F);
        }
    }

    public static boolean isOnCscTruss(ExtraLightsLightBlockEntity mountable) {
        return mountable.getSupportingStructure()
                .map(SupportCompat::isCscTruss)
                .orElse(false);
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
        if (isOnCscTruss(mountable)) {
            m.translate(0.0f, -CSC_MOUNT_DROP, 0.0f);
        }
        return m;
    }
}