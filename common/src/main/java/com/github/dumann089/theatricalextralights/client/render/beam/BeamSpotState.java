package com.github.dumann089.theatricalextralights.client.render.beam;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Geometrie du cone d'un projecteur, publiee par le renderer et relue par la lumiere
 * dynamique pour dimensionner la tache lumineuse.
 *
 * <p>On publie <b>l'angle</b> du cone, pas le rayon du faisceau dessine. La taille de la
 * tache se calcule ensuite en {@code distance x tan(demi-angle)} : la lumiere atteint la
 * surface meme quand le faisceau volumetrique, lui, s'est deja eteint avant. Une premiere
 * version publiait le rayon a l'impact et ne le faisait que si le faisceau touchait
 * quelque chose — un projecteur qui eclaire au loin sans obstacle ne publiait donc rien du
 * tout, et la correction ne s'appliquait jamais.
 *
 * <p>Le probleme de fond resolu : Theatrical derive le rayon de la lumiere du seul canal
 * focus, sans tenir compte de la distance. Un cone serre eclairant a 100 m produisait donc
 * la meme tache qu'a 3 m.
 *
 * <p>Ecrit depuis le thread de rendu, relu depuis le tick client : d'ou la map concurrente.
 */
public final class BeamSpotState {

    private BeamSpotState() {}

    /**
     * Duree de validite d'une entree, en ticks. Genereuse : l'angle est une propriete lente
     * du projecteur, et son renderer peut cesser de tourner un instant (projecteur hors
     * champ) alors que sa lumiere continue d'eclairer le monde.
     */
    private static final long STALE_TICKS = 100;

    /** Taille au-dela de laquelle on purge les entrees perimees. */
    private static final int PRUNE_THRESHOLD = 256;

    /**
     * @param tanHalfAngle tangente du demi-angle du cone.
     * @param baseRadius   rayon a la lentille, plancher de la section.
     * @param shapeScale   etirement de la section (>1 pour les barres).
     */
    public record Cone(float tanHalfAngle, float baseRadius, float shapeScale, long tick) {

        /** Rayon de la section du cone a la distance donnee. */
        public float radiusAt(double distance) {
            return (float) Math.max(baseRadius, distance * tanHalfAngle) * shapeScale;
        }
    }

    private static final Map<BlockPos, Cone> CONES = new ConcurrentHashMap<>();

    /**
     * Publie la geometrie du cone d'un projecteur.
     *
     * <p>Appele sans condition a chaque soumission de faisceau : ni la presence d'un
     * obstacle ni le mode de rendu n'entrent en jeu. Un projecteur multi-faisceaux publie
     * plusieurs fois par frame ; on retient le cone le plus large du tick, qui est celui
     * qui determine l'etendue eclairee.
     */
    public static void publish(BlockPos pos, float tanHalfAngle, float baseRadius,
                               float shapeScale, long tick) {
        if (pos == null || tanHalfAngle < 0f) return;

        Cone candidate = new Cone(tanHalfAngle, Math.max(0f, baseRadius),
                Math.max(1.0e-3f, shapeScale), tick);

        CONES.compute(pos.immutable(), (key, prev) -> {
            if (prev == null || prev.tick() != tick) return candidate;
            return candidate.tanHalfAngle() > prev.tanHalfAngle() ? candidate : prev;
        });

        if (CONES.size() > PRUNE_THRESHOLD) prune(tick);
    }

    /** @return le cone publie pour ce projecteur, ou {@code null} s'il n'y en a pas de frais. */
    public static Cone cone(BlockPos pos, long tick) {
        if (pos == null) return null;
        Cone cone = CONES.get(pos);
        if (cone == null) return null;
        if (tick - cone.tick() > STALE_TICKS) return null;
        return cone;
    }

    private static void prune(long tick) {
        CONES.entrySet().removeIf(e -> tick - e.getValue().tick() > STALE_TICKS);
    }
}
