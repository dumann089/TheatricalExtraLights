package com.github.dumann089.theatricalextralights.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Block-space offsets and fixture rotations for Extra Lights fixtures. */
public final class DirectionOffset {
    /** Pivot Blockbench / rotation tête [5, 2.1, 8]. */
    public static final Vec3 FLAME_HEAD_PIVOT_BLOCK = new Vec3(5.0 / 16.0, 2.1 / 16.0, 8.0 / 16.0);
    /** Sommet du tube [5, 3.3, 8]. */
    public static final Vec3 FLAME_NOZZLE_BLOCK = new Vec3(5.0 / 16.0, 3.3 / 16.0, 8.0 / 16.0);
    /** Alignement mesh (base y=0.9 → support y=1.2). */
    public static final Vec3 FLAME_HEAD_MESH_LIFT = new Vec3(0.0, 0.3 / 16.0, 0.0);
    /** Distance pivot → sommet du tube (inclut le mesh lift du renderer). */
    private static final double NOZZLE_ARM = (FLAME_NOZZLE_BLOCK.y - FLAME_HEAD_PIVOT_BLOCK.y) + FLAME_HEAD_MESH_LIFT.y;

    private DirectionOffset() {
    }

    public static float panToAngle(int panValue) {
        return (Mth.clamp(panValue, 0, 255) - 128) / 128.0f * 90.0f;
    }

    public static float panToHeadRenderAngle(int panValue) {
        return panToAngle(panValue);
    }

    public static Vec3 flameNozzlePosition(Direction facing, float headRenderAngleDegrees) {
        float signedAngle = headRenderAngleDegrees * panRotationSign(facing);
        Vec3 tipFromPivot = localNozzleOffset(signedAngle);
        return FLAME_HEAD_PIVOT_BLOCK.add(tipFromPivot);
    }

    public static Vec3 flamePointAlongJet(Direction facing, float headRenderAngleDegrees, double distance) {
        Vec3 nozzle = flameNozzlePosition(facing, headRenderAngleDegrees);
        Vec3 direction = headJetDirection(facing, headRenderAngleDegrees);
        return nozzle.add(direction.scale(distance));
    }

    /** 128 DMX = vertical ; pan incline sud/nord (plan YZ). */
    public static Vec3 headJetDirection(Direction facing, float headRenderAngleDegrees) {
        float signedAngle = headRenderAngleDegrees * panRotationSign(facing);
        return modelDirectionToBlockSpace(facing, localJetDirection(signedAngle));
    }

    public static void applyPanRotation(PoseStack poseStack, Direction facing, float headRenderAngleDegrees) {
        applyFixtureRotation(poseStack, facing, new Vec3(headRenderAngleDegrees, 0.0, 0.0));
    }

    public static Vec3 neutralNozzleBlock() {
        return FLAME_NOZZLE_BLOCK;
    }

    /** Même yaw Y que {@code FlameThrowerRenderer.applyFixturePose} — coords modèle → espace bloc. */
    public static Vec3 modelToBlockSpace(Direction facing, Vec3 modelCoord) {
        double centerX = modelCoord.x - 0.5;
        double centerZ = modelCoord.z - 0.5;
        double rad = Math.toRadians(fixtureFacingYaw(facing));
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double rotatedX = centerX * cos - centerZ * sin;
        double rotatedZ = centerX * sin + centerZ * cos;
        return new Vec3(rotatedX + 0.5, modelCoord.y, rotatedZ + 0.5);
    }

    private static Vec3 modelDirectionToBlockSpace(Direction facing, Vec3 localDir) {
        double rad = Math.toRadians(fixtureFacingYaw(facing));
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double dx = localDir.x;
        double dz = localDir.z;
        Vec3 rotated = new Vec3(
                dx * cos - dz * sin,
                localDir.y,
                dx * sin + dz * cos
        );
        return rotated.lengthSqr() > 1.0e-6 ? rotated.normalize() : new Vec3(0.0, 1.0, 0.0);
    }

    private static float fixtureFacingYaw(Direction facing) {
        if (facing.getAxis() == Direction.Axis.X) {
            return facing.toYRot();
        }
        return facing.getOpposite().toYRot();
    }

    private static float panRotationSign(Direction facing) {
        return switch (facing) {
            case SOUTH, WEST -> -1.0f;
            default -> 1.0f;
        };
    }

    private static Vec3 localNozzleOffset(float signedAngleDegrees) {
        double rad = Math.toRadians(signedAngleDegrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(0.0, cos * NOZZLE_ARM, sin * NOZZLE_ARM);
    }

    private static Vec3 localJetDirection(float signedAngleDegrees) {
        double rad = Math.toRadians(signedAngleDegrees);
        return new Vec3(0.0, Math.cos(rad), Math.sin(rad));
    }

    public static void applyFixtureRotation(PoseStack poseStack, Direction facing, Vec3 rotation) {
        switch (facing) {
            case NORTH -> {
                poseStack.mulPose(Axis.XP.rotationDegrees((float) rotation.x));
                poseStack.mulPose(Axis.YP.rotationDegrees((float) rotation.y));
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) rotation.z));
            }
            case SOUTH -> {
                poseStack.mulPose(Axis.XP.rotationDegrees((float) -rotation.x));
                poseStack.mulPose(Axis.YP.rotationDegrees((float) -rotation.y));
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) rotation.z));
            }
            case EAST -> {
                poseStack.mulPose(Axis.XP.rotationDegrees((float) rotation.x));
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) -rotation.y));
                poseStack.mulPose(Axis.YP.rotationDegrees((float) rotation.z));
            }
            case WEST -> {
                poseStack.mulPose(Axis.XP.rotationDegrees((float) -rotation.x));
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) rotation.y));
                poseStack.mulPose(Axis.YP.rotationDegrees((float) rotation.z));
            }
            default -> poseStack.mulPose(Axis.YP.rotationDegrees((float) rotation.y));
        }
    }
}
