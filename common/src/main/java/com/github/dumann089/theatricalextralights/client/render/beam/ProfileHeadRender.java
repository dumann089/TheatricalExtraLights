package com.github.dumann089.theatricalextralights.client.render.beam;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasGobo;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Aides de rendu pour la personnalite Profile : prisme a facettes sur le faisceau volumetrique. */
public final class ProfileHeadRender {

    /** Ecart angulaire entre le faisceau central et chaque facette, en degres. */
    private static final float PRISM_SPREAD_DEG = 4.5f;

    private ProfileHeadRender() {
    }

    /**
     * Retourne les faisceaux a dessiner : le faisceau tel quel sans prisme, sinon une copie
     * par facette, ecartee du centre et tournee autour de l'axe du faisceau selon l'angle
     * du prisme. L'intensite est repartie pour garder la meme lumiere totale.
     */
    public static List<BeamRenderData> prismBeams(BeamRenderData data, Object blockEntity, float partialTicks) {
        if (!(blockEntity instanceof HasGobo gobo) || gobo.getPrismFacets() <= 1) {
            return List.of(data);
        }
        int facets = gobo.getPrismFacets();
        float baseAngle = gobo.getPrismAngleDeg(partialTicks);
        double spread = Math.toRadians(PRISM_SPREAD_DEG);
        float intensity = data.intensity() * 0.75f;

        Vec3 dir = data.beamDir();
        Vec3 u = data.axisU();
        Vec3 v = data.axisV();
        BeamRenderData[] out = new BeamRenderData[facets];
        for (int i = 0; i < facets; i++) {
            double a = Math.toRadians(baseAngle + i * 360.0 / facets);
            Vec3 offset = u.scale(Math.cos(a)).add(v.scale(Math.sin(a)));
            Vec3 newDir = dir.scale(Math.cos(spread)).add(offset.scale(Math.sin(spread))).normalize();
            // Re-orthogonalise les axes du gobo autour de la nouvelle direction.
            Vec3 newU = u.subtract(newDir.scale(u.dot(newDir))).normalize();
            Vec3 newV = newDir.cross(newU).normalize();
            out[i] = new BeamRenderData(
                    data.fixturePos(), data.origin(), newDir, newU, newV,
                    data.zoomNorm(), data.scanLen(), data.tanHalfAngle(), data.color(), intensity,
                    data.goboTexture(), data.nextGoboTexture(), data.goboRotation(), data.wheelTransition(),
                    data.level(), data.widthScale(), data.heightScale(), data.baseRadius(), data.shutters());
        }
        return List.of(out);
    }
}
