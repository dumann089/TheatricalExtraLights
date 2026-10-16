package com.github.dumann089.theatricalextralights.util;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;

/**
 * Server-side aiming for the Fixture Tracker: turns "look at that entity" into pan/tilt.
 *
 * <p>Deliberately self-contained (JOML only, no {@code PoseStack} / {@code Axis}): the geometry below
 * mirrors {@code FollowspotBeamHelper} (which is client-side code) so it is safe on a dedicated server.
 * The beam direction is sampled numerically, so it is correct for every mounting (floor, ceiling,
 * side truss, flipped) and no per-fixture maths is needed. Pan/tilt are then found with a few Newton
 * iterations starting from the current angles.
 */
public final class FixtureTrackingHelper {

    /** Head speed limit, in degrees per tick (15 = 300 deg/s). Makes the head sweep instead of teleport. */
    public static final float MAX_DEG_PER_TICK = 15.0f;

    private static final float PAN_LIMIT = 180.0f;
    private static final float TILT_MIN = -225.0f;
    private static final float TILT_MAX = 45.0f;

    private static final int ITERATIONS = 4;
    private static final float SAMPLE_DEG = 1.0f;
    private static final float MAX_NEWTON_STEP_DEG = 20.0f;
    /** ~0.1 degree: close enough, stop iterating. */
    private static final double DONE_RAD = 0.002;
    /** Aim at the chest rather than the feet. */
    private static final double AIM_HEIGHT = 0.6;

    private static final Vec3 WORLD_UP = new Vec3(0, 1, 0);

    private FixtureTrackingHelper() {
    }

    /** Called every server tick by a fixture that has a tracked entity. */
    public static void tick(ExtraLightsLightBlockEntity fixture, ServerLevel level, UUID targetId) {
        Entity target = level.getEntity(targetId);
        if (target == null || target.isRemoved() || !target.isAlive()) {
            // Keep the last aim; the entity may be in an unloaded chunk or respawning.
            return;
        }
        if (!fixture.supportsTracking()) {
            fixture.setTrackedEntity(null);
            return;
        }

        float startPan = fixture.hasTrackAngles() ? fixture.getTrackPan() : fixture.getPan();
        float startTilt = fixture.hasTrackAngles() ? fixture.getTrackTilt() : fixture.getTilt();

        Vec3 aim = new Vec3(target.getX(), target.getY(AIM_HEIGHT), target.getZ());
        Vec3 desired = aim.subtract(beamOrigin(fixture, startPan, startTilt));
        if (desired.lengthSqr() < 1.0e-4) {
            return;
        }
        desired = desired.normalize();

        float pan = startPan;
        float tilt = startTilt;
        for (int i = 0; i < ITERATIONS; i++) {
            Vec3 forward = beamDirection(fixture, pan, tilt);
            Vec3 right = forward.cross(WORLD_UP);
            if (right.lengthSqr() < 1.0e-4) {
                // Beam almost vertical: use the block's own right as the reference.
                Vec3 facing = Vec3.atLowerCornerOf(
                        fixture.getBlockState().getValue(HangableBlock.FACING).getClockWise().getNormal());
                right = facing.lengthSqr() > 0 ? facing.normalize() : new Vec3(1, 0, 0);
            } else {
                right = right.normalize();
            }
            Vec3 up = right.cross(forward).normalize();

            double forwardDot = desired.dot(forward);
            double rightRad = Math.atan2(desired.dot(right), forwardDot);
            double upRad = Math.atan2(desired.dot(up), forwardDot);
            if (Math.abs(rightRad) < DONE_RAD && Math.abs(upRad) < DONE_RAD) {
                break;
            }

            Vec3 dPan = beamDirection(fixture, pan + SAMPLE_DEG, tilt)
                    .subtract(beamDirection(fixture, pan - SAMPLE_DEG, tilt)).scale(1.0 / (2.0 * SAMPLE_DEG));
            Vec3 dTilt = beamDirection(fixture, pan, tilt + SAMPLE_DEG)
                    .subtract(beamDirection(fixture, pan, tilt - SAMPLE_DEG)).scale(1.0 / (2.0 * SAMPLE_DEG));
            double a = dPan.dot(right);
            double b = dTilt.dot(right);
            double c = dPan.dot(up);
            double d = dTilt.dot(up);
            double det = a * d - b * c;
            if (Math.abs(det) < 1.0e-7) {
                break;
            }
            pan += (float) Mth.clamp((rightRad * d - b * upRad) / det, -MAX_NEWTON_STEP_DEG, MAX_NEWTON_STEP_DEG);
            tilt += (float) Mth.clamp((a * upRad - c * rightRad) / det, -MAX_NEWTON_STEP_DEG, MAX_NEWTON_STEP_DEG);
            tilt = Mth.clamp(tilt, TILT_MIN, TILT_MAX);
        }

        // Head speed limit (keeps the direction of travel).
        float dPanTotal = pan - startPan;
        float dTiltTotal = tilt - startTilt;
        float biggest = Math.max(Math.abs(dPanTotal), Math.abs(dTiltTotal));
        if (biggest > MAX_DEG_PER_TICK) {
            float k = MAX_DEG_PER_TICK / biggest;
            pan = startPan + dPanTotal * k;
            tilt = startTilt + dTiltTotal * k;
        }
        tilt = Mth.clamp(tilt, TILT_MIN, TILT_MAX);

        // Pan is periodic: wrapping keeps the same direction, so no visible jump (snap = no interpolation).
        boolean snap = false;
        if (pan > PAN_LIMIT) {
            pan -= 360.0f;
            snap = true;
        } else if (pan < -PAN_LIMIT) {
            pan += 360.0f;
            snap = true;
        }

        fixture.applyTrackedAngles(pan, tilt, snap);
    }

    // ── Geometry (mirror of FollowspotBeamHelper, server-safe) ───────────────

    /** Same formula as Theatrical's {@code BaseLightBlockEntity.rayTraceDir}, with pan/tilt as parameters. */
    static Vec3 beamDirection(BaseLightBlockEntity fixture, float pan, float tilt) {
        BlockState state = fixture.getBlockState();
        Direction hangDirection = state.getValue(BaseLightBlock.HANG_DIRECTION);
        Direction facing = state.getValue(BaseLightBlock.FACING);
        boolean hanging = state.getValue(BaseLightBlock.HANGING);
        boolean hangingNonVertically = hanging && hangDirection != Direction.DOWN && hangDirection != Direction.UP;
        Fixture def = fixture.getFixture();

        if (!hangingNonVertically) {
            float t = tilt;
            if (fixture.isUpsideDown() || def.invertTilt()) {
                t = -t;
            }
            float p = facing.toYRot() - pan;
            if (facing.getAxis() == Direction.Axis.X) {
                p -= 180f;
            }
            if (def.invertPan()) {
                p = -p;
            }
            if (fixture.isUpsideDown()) {
                if (facing.getAxis() == Direction.Axis.X) {
                    p = facing.getOpposite().toYRot() + pan;
                } else {
                    p = facing.toYRot() + pan;
                }
            }
            float f = t * 0.017453292F;
            float g = -p * 0.017453292F;
            return new Vec3(Mth.sin(g) * Mth.cos(f), -Mth.sin(f), Mth.cos(g) * Mth.cos(f));
        }

        Direction opposite = hangDirection.getOpposite();
        int step = opposite.getAxisDirection().getStep();
        float toRad = (float) (Math.PI / 180.0);
        float p = (fixture.getBasePan() + pan) * step * toRad;
        float t = tilt * -step * toRad;
        float x = Mth.sin(p) * Mth.cos(t);
        float y = Mth.sin(t);
        float z = Mth.cos(p) * Mth.cos(t);
        AxisCycle cycle = AxisCycle.VALUES[(opposite.getAxis().ordinal() + 2) % AxisCycle.VALUES.length];
        return new Vec3(
                cycle.cycle(x, y, z, Direction.Axis.X),
                cycle.cycle(x, y, z, Direction.Axis.Y),
                cycle.cycle(x, y, z, Direction.Axis.Z)
        ).normalize();
    }

    /** World position of the lens for the given pan/tilt (same transform chain as the block-entity renderer). */
    static Vec3 beamOrigin(BaseLightBlockEntity fixture, float pan, float tilt) {
        BlockState state = fixture.getBlockState();
        BlockPos blockPos = fixture.getBlockPos();
        Fixture def = fixture.getFixture();
        boolean isHanging = state.getValue(BaseLightBlock.HANGING);
        boolean isFlipped = fixture.isUpsideDown();
        Direction facing = state.getValue(HangableBlock.FACING);

        Matrix4f m = new Matrix4f();
        m.translate(0.5F, 0.0F, 0.5F);
        if (isHanging) {
            Direction hangDirection = state.getValue(HangableBlock.HANG_DIRECTION);
            m.translate(0.0F, 0.5F, 0.0F);
            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    if (hangDirection == Direction.SOUTH) {
                        m.rotateZ(rad(90));
                        m.rotateX(rad(-90));
                    } else {
                        m.rotateZ(rad(90));
                        m.rotateX(rad(90));
                    }
                } else if (hangDirection == Direction.EAST) {
                    m.rotateZ(rad(90));
                } else {
                    m.rotateZ(rad(-90));
                }
            } else if (hangDirection == Direction.UP) {
                m.rotateX(rad(180));
            }
            m.translate(0.0F, -0.5F, 0.0F);
        }
        m.rotateY(rad(facing.toYRot()));
        m.translate(-0.5F, 0.0F, -0.5F);
        if (isHanging) {
            Optional<BlockState> support = fixture.getSupportingStructure();
            if (support.isPresent()) {
                float[] transforms = def.getTransforms(state, support.get());
                m.translate(transforms[0], transforms[1], transforms[2]);
            } else {
                m.translate(0.0F, 0.19F, 0.0F);
            }
            m.translate(0.0F, -0.08F, 0.0F);
        }
        if (isFlipped) {
            m.translate(0.5F, 0.5F, 0.5F);
            m.rotateZ(rad(180));
            m.translate(-0.5F, -0.5F, -0.5F);
        }

        float[] pans = def.getPanRotationPosition();
        m.translate(pans[0], pans[1], pans[2]);
        m.rotateY(rad(pan));
        m.translate(-pans[0], -pans[1], -pans[2]);

        float[] tilts = def.getTiltRotationPosition();
        m.translate(tilts[0], tilts[1], tilts[2]);
        m.rotateX(rad(180));
        m.rotateX(rad(tilt));
        m.translate(-tilts[0], -tilts[1], -tilts[2]);

        float[] beam = def.getBeamStartPosition();
        m.translate(beam[0], beam[1], beam[2]);

        Vector3f local = m.getTranslation(new Vector3f());
        return new Vec3(blockPos.getX() + local.x, blockPos.getY() + local.y, blockPos.getZ() + local.z);
    }

    private static float rad(float degrees) {
        return degrees * Mth.DEG_TO_RAD;
    }
}
