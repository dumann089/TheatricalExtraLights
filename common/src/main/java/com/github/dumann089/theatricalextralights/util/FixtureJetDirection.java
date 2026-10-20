package com.github.dumann089.theatricalextralights.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Direction et origine du jet pour les fixtures orientables (water jets). Le Flow2Jet lit la matrice du renderer. */
public final class FixtureJetDirection {
    private FixtureJetDirection() {
    }

    public static Vector3f computeDirection(float pan, float tilt, Direction facing) {
        return computeDirection(pan, tilt, facing, 0.0f, false);
    }

    public static Vector3f computeDirection(float pan, float tilt, Direction facing, float pitchOffsetDeg, boolean movingJetPitch) {
        double yaw = Math.toRadians(pan);
        double pitch = Math.toRadians(tilt + pitchOffsetDeg + (movingJetPitch ? 90.0 : 0.0));

        double dirX = -Math.sin(yaw) * Math.cos(pitch);
        double dirY = Math.sin(pitch);
        double dirZ = Math.cos(yaw) * Math.cos(pitch);

        switch (facing) {
            case NORTH -> {
                dirX = -dirX;
                dirZ = -dirZ;
            }
            case EAST -> {
                double tmp = dirX;
                dirX = dirZ;
                dirZ = -tmp;
            }
            case WEST -> {
                double tmp = dirX;
                dirX = -dirZ;
                dirZ = tmp;
            }
            default -> {
            }
        }
        return new Vector3f((float) dirX, (float) dirY, (float) dirZ);
    }

    public static Vec3 beamWorldPosition(
            BlockPos blockPos,
            Direction facing,
            float pan,
            float tilt,
            float[] pivot,
            float[] beamStart
    ) {
        Vec3 local = new Vec3(beamStart[0], beamStart[1], beamStart[2]);
        local = rotateYAround(local, 0.5, 0.5, fixtureFacingYaw(facing));
        local = rotateYAround(local, pivot[0], pivot[2], pan);
        local = rotateXAround(local, pivot[0], pivot[1], pivot[2], tilt);
        return new Vec3(
                blockPos.getX() + local.x,
                blockPos.getY() + local.y,
                blockPos.getZ() + local.z
        );
    }

    public static float interpolateAngle(float previous, float current, float partialTick) {
        return previous + (current - previous) * partialTick;
    }

    public static Vec3 pointAlongJet(Vec3 origin, Vector3f direction, double distance) {
        return origin.add(direction.x() * distance, direction.y() * distance, direction.z() * distance);
    }

    public static float intensityFactor(int intensity) {
        return Mth.clamp(intensity, 0, 255) / 255.0f;
    }

    private static float fixtureFacingYaw(Direction facing) {
        if (facing.getAxis() == Direction.Axis.X) {
            return facing.toYRot();
        }
        return facing.getOpposite().toYRot();
    }

    private static Vec3 rotateYAround(Vec3 point, double pivotX, double pivotZ, float degrees) {
        if (Math.abs(degrees) < 1.0e-4f) {
            return point;
        }
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double dx = point.x - pivotX;
        double dz = point.z - pivotZ;
        double rx = dx * cos - dz * sin;
        double rz = dx * sin + dz * cos;
        return new Vec3(rx + pivotX, point.y, rz + pivotZ);
    }

    private static Vec3 rotateXAround(Vec3 point, double pivotX, double pivotY, double pivotZ, float degrees) {
        if (Math.abs(degrees) < 1.0e-4f) {
            return point;
        }
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double dy = point.y - pivotY;
        double dz = point.z - pivotZ;
        double ry = dy * cos - dz * sin;
        double rz = dy * sin + dz * cos;
        return new Vec3(point.x, ry + pivotY, rz + pivotZ);
    }
}
