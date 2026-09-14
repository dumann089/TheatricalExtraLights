package com.github.dumann089.theatricalextralights.client.render.beam;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasGobo;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Aides de rendu pour la personnalite Profile : prisme a facettes sur le faisceau volumetrique. */
public final class ProfileHeadRender {

    /** Ecart angulaire entre le faisceau central et chaque facette, en degres. */
    private static final float PRISM_SPREAD_DEG = 4.5f;

    /** Intensite de chaque facette : la lumiere est repartie sans etre divisee strictement. */
    public static final float PRISM_INTENSITY = 0.75f;

    private ProfileHeadRender() {
    }

    /** Facteur d'intensite par facette selon leur nombre (3 → 0.75, 6 → 0.6, 9 → 0.5). */
    public static float prismIntensity(int facets) {
        if (facets <= 1) return 1f;
        if (facets <= 3) return PRISM_INTENSITY;
        if (facets <= 6) return 0.6f;
        return 0.5f;
    }

    /** Ecart du centre : plus de facettes, cercle un peu plus large pour qu'elles se distinguent. */
    private static float spreadDeg(int facets) {
        if (facets <= 3) return PRISM_SPREAD_DEG;
        if (facets <= 6) return PRISM_SPREAD_DEG + 1.0f;
        return PRISM_SPREAD_DEG + 2.0f;
    }

    /**
     * Reperes (direction, axe U, axe V) de chaque facette du prisme. Sans prisme, retourne le
     * repere d'origine seul. Partage entre le faisceau volumetrique et la projection du gobo
     * pour que les deux images se superposent exactement.
     */
    public static List<Vec3[]> prismFrames(Vec3 dir, Vec3 u, Vec3 v, Object blockEntity, float partialTicks) {
        if (!(blockEntity instanceof HasGobo gobo) || gobo.getPrismFacets() <= 1) {
            return java.util.Collections.singletonList(new Vec3[] { dir, u, v });
        }
        int facets = gobo.getPrismFacets();
        float baseAngle = gobo.getPrismAngleDeg(partialTicks);
        double spread = Math.toRadians(spreadDeg(facets));
        Vec3[][] out = new Vec3[facets][];
        for (int i = 0; i < facets; i++) {
            double a = Math.toRadians(baseAngle + i * 360.0 / facets);
            Vec3 offset = u.scale(Math.cos(a)).add(v.scale(Math.sin(a)));
            Vec3 newDir = dir.scale(Math.cos(spread)).add(offset.scale(Math.sin(spread))).normalize();
            // Re-orthogonalise les axes du gobo autour de la nouvelle direction.
            Vec3 newU = u.subtract(newDir.scale(u.dot(newDir))).normalize();
            Vec3 newV = newDir.cross(newU).normalize();
            out[i] = new Vec3[] { newDir, newU, newV };
        }
        return List.of(out);
    }

    /**
     * Retourne les faisceaux a dessiner : le faisceau tel quel sans prisme, sinon une copie
     * par facette, ecartee du centre et tournee autour de l'axe du faisceau selon l'angle
     * du prisme. L'intensite est repartie pour garder la meme lumiere totale.
     */
    public static List<BeamRenderData> prismBeams(BeamRenderData data, Object blockEntity, float partialTicks) {
        List<Vec3[]> frames = prismFrames(data.beamDir(), data.axisU(), data.axisV(), blockEntity, partialTicks);
        if (frames.size() <= 1) {
            return List.of(data);
        }
        float intensity = data.intensity() * prismIntensity(frames.size());
        BeamRenderData[] out = new BeamRenderData[frames.size()];
        for (int i = 0; i < out.length; i++) {
            Vec3[] f = frames.get(i);
            out[i] = new BeamRenderData(
                    data.fixturePos(), data.origin(), f[0], f[1], f[2],
                    data.zoomNorm(), data.scanLen(), data.tanHalfAngle(), data.color(), intensity,
                    data.goboTexture(), data.nextGoboTexture(), data.goboRotation(), data.wheelTransition(),
                    data.level(), data.widthScale(), data.heightScale(), data.baseRadius(), data.shutters());
        }
        return List.of(out);
    }
}
