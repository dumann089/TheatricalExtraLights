package com.github.dumann089.theatricalextralights.client.render.beam;

import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Dimensionne la tache lumineuse projetee sur la geometrie du cone du projecteur.
 *
 * <p>Le probleme corrige : {@code BaseLightBlockEntity.getLightSpread()} de Theatrical derive
 * le rayon du <b>seul</b> canal focus, par une interpolation entre 1 et le rayon declare par
 * la fixture. La distance n'y entre jamais. Un cone serre eclairant a 60 blocs produisait donc
 * exactement la meme tache qu'a 3 blocs, alors que le faisceau dessine, lui, s'elargit avec la
 * distance. Les deux tailles ne pouvaient coincider qu'a une distance precise.
 *
 * <p>Ici la taille vaut {@code distance x tan(demi-angle)}, ou la distance est celle a laquelle
 * Theatrical place le point d'emission. La tache correspond donc a la section du cone la ou la
 * lumiere arrive — y compris quand le faisceau volumetrique s'est eteint avant, ce qui n'empeche
 * pas la lumiere d'atteindre la surface.
 *
 * <p>Seuls les projecteurs dont le cone varie publient leur geometrie (voir
 * {@code ExtraLightsFixtureRenderer.publishCone}). Les appareils a cone fixe et ceux qui
 * surchargent deja {@code getLightSpread()} — strobes, blinders — gardent leur comportement.
 */
public final class BeamSpotLighting {

    private BeamSpotLighting() {}

    /** Plancher de rayon : en dessous, la lumiere dynamique n'eclaire plus rien du tout. */
    private static final float MIN_RADIUS = 1.0f;

    /**
     * Memo a une entree. {@code LightManager.maxDynamicLightLevel} appelle
     * {@code getLightSpread()} <em>pour chaque bloc</em> de la zone eclairee, et toujours pour
     * la meme source d'affilee : sans ce cache on relirait la config et le cone par bloc.
     */
    private record Memo(BlockPos pos, long tick, double distance, float radius) {}

    private static volatile Memo memo;

    /**
     * Rayon de la tache pour ce projecteur.
     *
     * @param distance distance a laquelle la lumiere arrive, celle du point d'emission calcule
     *                 par Theatrical.
     * @return le rayon derive du cone, ou {@code Float.NaN} si aucun cone n'a ete publie —
     *         l'appelant garde alors le comportement d'origine.
     */
    public static float spotRadius(Level level, BlockPos fixturePos, double distance) {
        if (level == null || fixturePos == null
                || !TheatricalExtraLightsConfig.doesSpotFollowBeam()) {
            return Float.NaN;
        }

        long tick = level.getGameTime();

        Memo current = memo;
        if (current != null && current.tick == tick && current.pos.equals(fixturePos)
                && current.distance == distance) {
            return current.radius > 0f ? current.radius : Float.NaN;
        }

        BeamSpotState.Cone cone = BeamSpotState.cone(fixturePos, tick);
        if (cone == null) {
            memo = new Memo(fixturePos.immutable(), tick, distance, -1f);
            return Float.NaN;
        }

        float radius = Math.max(MIN_RADIUS,
                Math.min(cone.radiusAt(distance), TheatricalExtraLightsConfig.getSpotMaxRadius()));

        memo = new Memo(fixturePos.immutable(), tick, distance, radius);
        return radius;
    }
}
