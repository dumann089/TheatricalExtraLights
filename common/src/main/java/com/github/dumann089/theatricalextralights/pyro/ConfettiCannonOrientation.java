package com.github.dumann089.theatricalextralights.pyro;

import com.github.dumann089.theatricalextralights.blockentities.ConfettiCannonBlockEntity;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Barrel aim derived from the same transforms as {@code ConfettiCannonRenderer}.
 * Uses JOML only so dedicated server ticks never touch client-only {@code PoseStack}.
 */
public final class ConfettiCannonOrientation {
    /** Blockbench barrel part rotation (Z -37.5°). */
    private static final float BARREL_Z_ROT = -0.6545F;
    /** Barrel mouth / tip in Blockbench pixels (root space). */
    private static final float MOUTH_X = -3.0F;
    private static final float MOUTH_Y = 13.0F;
    private static final float TIP_ALONG_BARREL = 7.5F;

    private ConfettiCannonOrientation() {
    }

    public static Vec3 getLaunchDirection(BlockState state, ConfettiCannonBlockEntity blockEntity) {
        Matrix4f pose = buildOrientationMatrix(state, blockEntity);

        Vector3f mouth = modelPointToBlockLocal(MOUTH_X, MOUTH_Y, 0.0F);
        Vector3f tip = modelPointToBlockLocal(barrelTipX(), barrelTipY(), 0.0F);
        mouth.mulPosition(pose);
        tip.mulPosition(pose);

        Vector3f dir = new Vector3f(tip).sub(mouth);
        if (dir.lengthSquared() < 1.0E-6F) {
            dir.set(0.0F, 1.0F, 0.0F);
        } else {
            dir.normalize();
        }
        if (dir.y < 0.0F) {
            dir.y = -dir.y;
        }
        return new Vec3(dir.x, dir.y, dir.z);
    }

    public static Vec3 getNozzlePosition(BlockPos pos, BlockState state, ConfettiCannonBlockEntity blockEntity) {
        Matrix4f pose = buildOrientationMatrix(state, blockEntity);

        Vector3f mouth = modelPointToBlockLocal(MOUTH_X, MOUTH_Y, 0.0F);
        mouth.mulPosition(pose);
        return new Vec3(pos.getX() + mouth.x, pos.getY() + mouth.y, pos.getZ() + mouth.z);
    }

    /**
     * Same transform chain as the block entity renderer (server-safe).
     */
    public static Matrix4f buildOrientationMatrix(BlockState state, ConfettiCannonBlockEntity blockEntity) {
        Matrix4f m = new Matrix4f().identity();
        Direction facing = state.getValue(HangableBlock.FACING);
        boolean isFlipped = blockEntity.isUpsideDown();
        boolean isHanging = state.getValue(BaseLightBlock.HANGING);

        m.translate(0.5F, 0.0F, 0.5F);
        if (isHanging) {
            Direction hangDirection = state.getValue(HangableBlock.HANG_DIRECTION);
            m.translate(0.0F, 0.5F, 0.0F);
            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    m.rotateX((hangDirection == Direction.SOUTH ? -90.0F : 90.0F) * Mth.DEG_TO_RAD);
                    m.rotateY(180.0F * Mth.DEG_TO_RAD);
                } else {
                    // PoseStack used Axis.ZN (negative Z) with EAST -90 / else +90
                    m.rotateZ(-(hangDirection == Direction.EAST ? -90.0F : 90.0F) * Mth.DEG_TO_RAD);
                }
            }
            m.translate(0.0F, -0.5F, 0.0F);
        }
        m.rotateY(facing.toYRot() * Mth.DEG_TO_RAD);
        m.translate(-0.5F, 0.0F, -0.5F);

        if (isHanging) {
            var support = blockEntity.getSupportingStructure();
            if (support.isPresent()) {
                float[] transforms = blockEntity.getFixture().getTransforms(state, support.get());
                m.translate(transforms[0], transforms[1], transforms[2]);
            } else {
                m.translate(0.0F, 0.19F, 0.0F);
            }
            m.translate(0.0F, -0.08F, 0.0F);
        }
        if (isFlipped) {
            m.translate(0.5F, 0.5F, 0.5F);
            m.rotateZ(180.0F * Mth.DEG_TO_RAD);
            m.translate(-0.5F, -0.5F, -0.5F);
        }

        applyBlockbenchEntityTransform(m);
        return m;
    }

    public static void applyBlockbenchEntityTransform(Matrix4f m) {
        m.translate(0.5F, 1.5F, 0.5F);
        m.rotateY(180.0F * Mth.DEG_TO_RAD);
        m.scale(-1.0F, -1.0F, 1.0F);
    }

    public static Vector3f spreadDirection(RandomSource random, Vec3 axis, float spread) {
        Vector3f facing = axis.toVector3f();
        Vector3f orth = orthogonal(facing);
        orth.rotateAxis(random.nextFloat() * Mth.TWO_PI, facing.x, facing.y, facing.z);
        orth.mul((float) (random.nextGaussian() * spread));
        facing.add((Vector3fc) orth).normalize();
        return facing;
    }

    private static Vector3f modelPointToBlockLocal(float px, float py, float pz) {
        return new Vector3f(px / 16.0F, py / 16.0F, pz / 16.0F);
    }

    private static float barrelTipX() {
        Vector3f along = new Vector3f(-TIP_ALONG_BARREL, 0.0F, 0.0F);
        along.rotateZ(BARREL_Z_ROT);
        return MOUTH_X + along.x;
    }

    private static float barrelTipY() {
        Vector3f along = new Vector3f(-TIP_ALONG_BARREL, 0.0F, 0.0F);
        along.rotateZ(BARREL_Z_ROT);
        return MOUTH_Y + along.y;
    }

    private static Vector3f orthogonal(Vector3f v) {
        if (Math.abs(v.x) > Math.abs(v.y)) {
            return new Vector3f(-v.z, 0.0F, v.x).normalize();
        }
        return new Vector3f(0.0F, v.z, -v.y).normalize();
    }
}
