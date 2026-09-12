package com.github.dumann089.theatricalextralights.util;

import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Choix du cote ou se place l'oeil de l'operateur. Le sens des commandes est gere par
 * {@link FollowspotAimMapper}.
 */
public final class FollowspotOrientationHelper {

    private FollowspotOrientationHelper() {
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
}
