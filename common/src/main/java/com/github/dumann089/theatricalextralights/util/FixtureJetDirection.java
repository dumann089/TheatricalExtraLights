package com.github.dumann089.theatricalextralights.util;

import com.github.dumann089.theatricalextralights.fixtures.Flow2JetFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Direction et origine du jet pour fixtures orientables (Flow2Jet, water jets). */
public final class FixtureJetDirection {
    private FixtureJetDirection() {
    }

    public static Vector3f computeDirection(float pan, float tilt, Direction facing) {
        return computeDirection(pan, tilt, facing, 0.0f, false);
    }

    public static Vec3 beamWorldPositionFlow2Jet(
            BlockPos blockPos,
            Direction facing,
            float pan,
            float userTilt,
            float[] headPivot,
            float[] beamStart,
            boolean rigged,
            boolean flipped
    ) {
        return transformFlow2JetModelPoint(
                blockPos,
                facing,
                pan,
                userTilt,
                headPivot,
                beamStart,
                rigged,
                flipped
        );
    }

    public static Vec3 beamWorldPositionFlow2Jet(
            BlockPos blockPos,
            Direction facing,
            float pan,
            float userTilt,
            float[] headPivot,
            float[] beamStart,
            boolean hanging
    ) {
        return beamWorldPositionFlow2Jet(blockPos, facing, pan, userTilt, headPivot, beamStart, hanging, hanging);
    }

    public static Vec3 transformFlow2JetModelPointLocal(
            Direction facing,
            float pan,
            float userTilt,
            float[] headPivot,
            float[] modelPoint,
            boolean rigged,
            boolean flipped
    ) {
        float effectiveTilt = Flow2JetFixture.effectiveTilt(userTilt, rigged, flipped);
        Vec3 local = new Vec3(modelPoint[0], modelPoint[1], modelPoint[2]);
        local = rotateYAround(local, 0.5, 0.5, fixtureFacingYaw(facing));

        if (flipped) {
            local = flipAroundBlockCenter(local);
        }

        Vec3 panTiltPivot = flow2JetPanTiltPivot(headPivot, null, rigged);
        local = rotateYAround(local, panTiltPivot.x, panTiltPivot.z, pan);
        local = rotateXAround(local, panTiltPivot.x, panTiltPivot.y, panTiltPivot.z, effectiveTilt);
        return local;
    }

    public static Vec3 transformFlow2JetModelPointLocal(
            Direction facing,
            float pan,
            float userTilt,
            float[] headPivot,
            float[] modelPoint,
            boolean hanging
    ) {
        return transformFlow2JetModelPointLocal(facing, pan, userTilt, headPivot, modelPoint, hanging, hanging);
    }

    /** Pivot pan/tilt — même point que le renderer (facing déjà appliqué sur les points si null). */
    public static Vec3 flow2JetPanTiltPivot(float[] headPivot, Direction facing, boolean hanging) {
        Vec3 pivot = new Vec3(headPivot[0], headPivot[1], headPivot[2]);
        if (facing != null) {
            pivot = rotateYAround(pivot, 0.5, 0.5, fixtureFacingYaw(facing));
        }
        return pivot;
    }

    public static Vec3 flow2JetPanTiltPivot(float[] headPivot, Direction facing) {
        return flow2JetPanTiltPivot(headPivot, facing, false);
    }

    private static Vec3 transformFlow2JetModelPoint(
            BlockPos blockPos,
            Direction facing,
            float pan,
            float userTilt,
            float[] headPivot,
            float[] modelPoint,
            boolean rigged,
            boolean flipped
    ) {
        Vec3 local = transformFlow2JetModelPointLocal(
                facing, pan, userTilt, headPivot, modelPoint, rigged, flipped
        );
        return new Vec3(
                blockPos.getX() + local.x,
                blockPos.getY() + local.y,
                blockPos.getZ() + local.z
        );
    }

    public static Vector3f directionFromFlow2JetPose(
            BlockPos blockPos,
            Direction facing,
            float pan,
            float userTilt,
            float[] headPivot,
            float[] beamStart,
            boolean rigged,
            boolean flipped
    ) {
        Vec3 origin = transformFlow2JetModelPointLocal(
                facing, pan, userTilt, headPivot, beamStart, rigged, flipped
        );
        float[] tip = new float[]{beamStart[0], beamStart[1] + 0.05f, beamStart[2]};
        Vec3 ahead = transformFlow2JetModelPointLocal(
                facing, pan, userTilt, headPivot, tip, rigged, flipped
        );
        Vec3 delta = ahead.subtract(origin);
        if (delta.lengthSqr() < 1.0e-8) {
            delta = transformFlow2JetDirectionLocal(facing, pan, userTilt, rigged, flipped);
        } else {
            delta = delta.normalize();
        }
        return new Vector3f((float) delta.x, (float) delta.y, (float) delta.z);
    }

    public static Vector3f directionFromFlow2JetPose(
            BlockPos blockPos,
            Direction facing,
            float pan,
            float userTilt,
            float[] headPivot,
            float[] beamStart,
            boolean hanging
    ) {
        return directionFromFlow2JetPose(blockPos, facing, pan, userTilt, headPivot, beamStart, hanging, hanging);
    }

    /** Direction fumée = +Y Blockbench (sortie buse vers le haut), transformé comme un vecteur. */
    public static Vec3 transformFlow2JetDirectionLocal(
            Direction facing,
            float pan,
            float userTilt,
            boolean rigged,
            boolean flipped
    ) {
        float effectiveTilt = Flow2JetFixture.effectiveTilt(userTilt, rigged, flipped);
        Vec3 dir = new Vec3(0, 1, 0);
        dir = rotateYVector(dir, fixtureFacingYaw(facing));

        if (flipped) {
            dir = flipDirectionAroundBlockCenter(dir);
        }

        dir = rotateYVector(dir, pan);
        dir = rotateXVector(dir, effectiveTilt);
        double len = dir.length();
        if (len < 1.0e-8) {
            return flipped || rigged ? new Vec3(0, -1, 0) : new Vec3(0, 1, 0);
        }
        return dir.scale(1.0 / len);
    }

    public static Vec3 transformFlow2JetDirectionLocal(
            Direction facing,
            float pan,
            float userTilt,
            boolean hanging
    ) {
        return transformFlow2JetDirectionLocal(facing, pan, userTilt, hanging, hanging);
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

    private static Vec3 flipAroundBlockCenter(Vec3 point) {
        return new Vec3(1.0 - point.x, 1.0 - point.y, point.z);
    }

    private static Vec3 flipDirectionAroundBlockCenter(Vec3 vector) {
        return new Vec3(-vector.x, -vector.y, vector.z);
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

    private static Vec3 rotateYVector(Vec3 vector, float degrees) {
        if (Math.abs(degrees) < 1.0e-4f) {
            return vector;
        }
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(
                vector.x * cos - vector.z * sin,
                vector.y,
                vector.x * sin + vector.z * cos
        );
    }

    private static Vec3 rotateXVector(Vec3 vector, float degrees) {
        if (Math.abs(degrees) < 1.0e-4f) {
            return vector;
        }
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(
                vector.x,
                vector.y * cos - vector.z * sin,
                vector.y * sin + vector.z * cos
        );
    }
}
