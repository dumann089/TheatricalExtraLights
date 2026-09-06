package com.github.dumann089.theatricalextralights.util;

import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Maps operator keys to pan/tilt deltas based on how the fixture is mounted (truss, flip, etc.).
 */
public final class FollowspotOrientationHelper {

    private static final float SAMPLE_DELTA = FollowspotDmxHelper.PAN_TILT_STEP;
    private static final Vec3 WORLD_UP = new Vec3(0, 1, 0);

    private FollowspotOrientationHelper() {
    }

    public record InputRemap(float panLeft, float panRight, float tiltUp, float tiltDown) {
    }

    public static InputRemap computeInputRemap(BaseLightBlockEntity fixture, float pan, float tilt) {
        float[] look = FollowspotBeamHelper.getLookAngles(fixture, pan, tilt);
        Vec3 forward = anglesToDirection(look[0], look[1]).normalize();
        Vec3 right = forward.cross(WORLD_UP);
        if (right.lengthSqr() < 1.0e-6) {
            right = new Vec3(1, 0, 0);
        } else {
            right = right.normalize();
        }
        Vec3 cameraUp = right.cross(forward).normalize();

        Vec3 base = FollowspotBeamHelper.getBeamDirection(fixture, pan, tilt);
        Vec3 panPlus = FollowspotBeamHelper.getBeamDirection(fixture, pan + SAMPLE_DELTA, tilt);
        Vec3 tiltPlus = FollowspotBeamHelper.getBeamDirection(fixture, pan, tilt + SAMPLE_DELTA);
        Vec3 dPan = panPlus.subtract(base);
        Vec3 dTilt = tiltPlus.subtract(base);

        // Sampling already includes hang / upside-down transforms from FollowspotBeamHelper —
        // do not flip again or hung fixtures feel inverted.
        float panLeft = signOrDefault(dPan.dot(right.scale(-1.0)));
        float panRight = signOrDefault(dPan.dot(right));
        float tiltUp = signOrDefault(dTilt.dot(cameraUp));
        float tiltDown = signOrDefault(dTilt.dot(cameraUp.scale(-1.0)));

        return new InputRemap(panLeft, panRight, tiltUp, tiltDown);
    }

    /** Pick the eye side that keeps the operator looking along the beam when mounted on truss. */
    public static float getOperatorEyeSide(BaseLightBlockEntity fixture, float pan, float tilt) {
        float preferred = -0.85f;
        float alternate = 0.85f;
        if (cameraAlignsWithBeam(fixture, pan, tilt, preferred)) {
            return preferred;
        }
        if (cameraAlignsWithBeam(fixture, pan, tilt, alternate)) {
            return alternate;
        }
        return preferred;
    }

    private static boolean cameraAlignsWithBeam(BaseLightBlockEntity fixture, float pan, float tilt, float eyeSide) {
        Vec3 beam = FollowspotBeamHelper.getBeamDirection(fixture, pan, tilt).normalize();
        Vec3 eyePos = FollowspotBeamHelper.getCameraPosition(fixture, pan, tilt, eyeSide);
        Vec3 beamOrigin = FollowspotBeamHelper.getBeamOrigin(fixture, pan, tilt);
        Vec3 look = beamOrigin.subtract(eyePos);
        if (look.lengthSqr() < 1.0e-6) {
            return false;
        }
        return look.normalize().dot(beam) > 0.35;
    }

    private static float signOrDefault(double value) {
        if (value > 1.0e-4) {
            return 1f;
        }
        if (value < -1.0e-4) {
            return -1f;
        }
        return 1f;
    }

    private static Vec3 anglesToDirection(float yaw, float pitch) {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        float cosPitch = (float) Math.cos(pitchRad);
        return new Vec3(
                -Math.sin(yawRad) * cosPitch,
                -Math.sin(pitchRad),
                Math.cos(yawRad) * cosPitch
        );
    }
}
