package com.github.dumann089.theatricalextralights.util;

import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Convertit un mouvement voulu de la tache lumineuse, exprime dans l'ecran de l'operateur
 * (droite / haut), en variation de pan et de tilt du projecteur.
 *
 * <p>La direction du faisceau est echantillonnee numeriquement via
 * {@link FollowspotBeamHelper}, qui inclut deja l'accroche (truss, plafond, retournement).
 * On projette les derivees d(direction)/d(pan) et d(direction)/d(tilt) sur la base ecran de
 * la camera operateur (roll nul, verticale monde) et on resout le systeme 2x2. Le sens des
 * commandes est donc toujours celui que voit l'operateur, quelle que soit la facon dont la
 * lyre est montee : plus d'inversion sur les machines accrochees a l'envers.
 */
public final class FollowspotAimMapper {

    private static final Vec3 WORLD_UP = new Vec3(0, 1, 0);
    /** Pas d'echantillonnage des derivees, en degres. */
    private static final float SAMPLE_DEG = 1.0f;
    /** Variation max par appel, en degres, pour ne pas sauter pres d'une singularite. */
    private static final float MAX_STEP_DEG = 20.0f;

    private FollowspotAimMapper() {
    }

    /** Base ecran + jacobienne en un point (pan, tilt). Les derivees sont en radians par degre. */
    public record Basis(Vec3 forward, Vec3 right, Vec3 up, Vec3 dPan, Vec3 dTilt) {
    }

    public static Basis compute(BaseLightBlockEntity fixture, float pan, float tilt) {
        Vec3 forward = FollowspotBeamHelper.getBeamDirection(fixture, pan, tilt).normalize();
        Vec3 right = forward.cross(WORLD_UP);
        if (right.lengthSqr() < 1.0e-4) {
            // Faisceau quasi vertical : on prend la droite du bloc comme reference.
            Vec3 facing = Vec3.atLowerCornerOf(fixture.getBlockState()
                    .getValue(dev.imabad.theatrical.blocks.HangableBlock.FACING).getClockWise().getNormal());
            right = facing.lengthSqr() > 0 ? facing.normalize() : new Vec3(1, 0, 0);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(forward).normalize();

        Vec3 panPlus = FollowspotBeamHelper.getBeamDirection(fixture, pan + SAMPLE_DEG, tilt);
        Vec3 panMinus = FollowspotBeamHelper.getBeamDirection(fixture, pan - SAMPLE_DEG, tilt);
        Vec3 tiltPlus = FollowspotBeamHelper.getBeamDirection(fixture, pan, tilt + SAMPLE_DEG);
        Vec3 tiltMinus = FollowspotBeamHelper.getBeamDirection(fixture, pan, tilt - SAMPLE_DEG);
        Vec3 dPan = panPlus.subtract(panMinus).scale(1.0 / (2.0 * SAMPLE_DEG));
        Vec3 dTilt = tiltPlus.subtract(tiltMinus).scale(1.0 / (2.0 * SAMPLE_DEG));
        return new Basis(forward, right, up, dPan, dTilt);
    }

    /**
     * Resout le deplacement angulaire voulu (radians vers la droite et vers le haut de l'ecran)
     * en (dPan, dTilt) degres. Retourne {0, 0} si la configuration est degeneree.
     */
    public static float[] solve(Basis basis, double rightRad, double upRad) {
        double a = basis.dPan().dot(basis.right());
        double b = basis.dTilt().dot(basis.right());
        double c = basis.dPan().dot(basis.up());
        double d = basis.dTilt().dot(basis.up());
        double det = a * d - b * c;
        if (Math.abs(det) < 1.0e-7) {
            return new float[]{0f, 0f};
        }
        double dPan = (rightRad * d - b * upRad) / det;
        double dTilt = (a * upRad - c * rightRad) / det;
        return new float[]{
                (float) Mth.clamp(dPan, -MAX_STEP_DEG, MAX_STEP_DEG),
                (float) Mth.clamp(dTilt, -MAX_STEP_DEG, MAX_STEP_DEG)
        };
    }

    /** Raccourci : deplacement ecran en degres → (dPan, dTilt) en degres. */
    public static float[] solveDegrees(BaseLightBlockEntity fixture, float pan, float tilt,
                                       double rightDeg, double upDeg) {
        Basis basis = compute(fixture, pan, tilt);
        return solve(basis, Math.toRadians(rightDeg), Math.toRadians(upDeg));
    }
}
