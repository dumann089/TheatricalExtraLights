package com.github.dumann089.theatricalextralights.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.function.Consumer;

/**
 * Rendu des blinders. L'eclairage au sol passe par la lumiere dynamique Theatrical/Shimmer ; ici,
 * ce que l'on voit de l'appareil : chaque lampe est une face emissive au coeur brule vers le
 * blanc, et l'ensemble des lampes bave en un halo avec une lueur dans la haze devant lui.
 * Reference : blinders au-dessus d'une scene sur une video de concert, des paves blancs-chauds
 * qui debordent dans l'image, les lampes a peine distinctes.
 */
public final class BlinderRenderHelper {

    /** Ancien rendu : un seul rectangle colore sur la face. Encore utilise par d'autres appareils. */
    public record FaceQuad(float x, float y, float z, float halfW, float halfH) {
        public static final FaceQuad STANDARD = new FaceQuad(0.5f, 0.68f, 0.422f, 0.3125f, 0.625f);
        public static final FaceQuad DOUBLE_2X2 = new FaceQuad(0.5f, 0.415f, 0.422f, 0.3125f, 0.284375f);
        public static final FaceQuad COMPACT = new FaceQuad(0.5f, 0.40625f, 0.34375f, 0.35f, 0.35f);
    }

    /**
     * Lampes d'un blinder : une grille {@code xs} x {@code ys} sur la face avant en {@code z}
     * (-Z devant), chaque lampe de demi-taille {@code halfW} x {@code halfH}. Releve sur les
     * elements « light » des modeles.
     */
    public record Lamps(float[] xs, float[] ys, float z, float halfW, float halfH) {
        /** 4x2_blinder : deux colonnes, quatre rangees de lentilles de 4/16. */
        public static final Lamps STANDARD_4X2 = new Lamps(
                new float[]{5f / 16f, 11f / 16f},
                new float[]{3.25f / 16f, 8.2f / 16f, 13.2f / 16f, 18.15f / 16f},
                0.422f, 2f / 16f, 2f / 16f);
        /** blinder2x2 : deux colonnes, deux rangees. */
        public static final Lamps DOUBLE_2X2 = new Lamps(
                new float[]{5f / 16f, 11f / 16f},
                new float[]{4.25f / 16f, 9.15f / 16f},
                0.422f, 2f / 16f, 2f / 16f);
        /** blinder1x1 : une lentille de 4.5 x 5. */
        public static final Lamps COMPACT = new Lamps(
                new float[]{0.5f},
                new float[]{6.25f / 16f},
                0.34375f, 2.25f / 16f, 2.5f / 16f);
        /** The circular diffuser of the Stage Blinder 200 Blaze. */
        public static final Lamps STAGE_BLAZE = new Lamps(
                new float[]{0.5f}, new float[]{6.5f / 16f},
                5.52f / 16f, 4.7f / 16f, 4.7f / 16f);
        /** blinder1x2 : la tete du 1x1 a x = 5/16 et 11/16. */
        public static final Lamps DUAL_1X2 = new Lamps(
                new float[]{5f / 16f, 11f / 16f},
                new float[]{6.25f / 16f},
                0.34375f, 2.25f / 16f, 2.5f / 16f);

        int count() {
            return xs.length * ys.length;
        }

        /** Face englobant toutes les lampes, pour le halo commun. */
        StrobeVisualEffects.Face array() {
            float x0 = xs[0], x1 = xs[0], y0 = ys[0], y1 = ys[0];
            for (float x : xs) {
                x0 = Math.min(x0, x);
                x1 = Math.max(x1, x);
            }
            for (float y : ys) {
                y0 = Math.min(y0, y);
                y1 = Math.max(y1, y);
            }
            return new StrobeVisualEffects.Face((x0 + x1) * 0.5f, (y0 + y1) * 0.5f, z,
                    (x1 - x0) * 0.5f + halfW, (y1 - y0) * 0.5f + halfH, false);
        }
    }

    /** Le verre de la lampe, tire vers le blanc : une lampe de blinder crame l'image. */
    private static final float LAMP_WHITEN = 0.7f;
    /** Disque de la lampe elle-meme (ronde, tournee vers la camera), en multiple du demi-format de la lentille. */
    private static final float LAMP_DISC_RADIUS = 2.0f;
    /** Bloom propre de chaque lampe, dans sa couleur, pour la lecture quand l'appareil est baisse. */
    private static final float LAMP_BLOOM_RADIUS = 3.5f;
    private static final float LAMP_BLOOM_STRENGTH = 0.8f;
    /**
     * Eblouissement : disques blancs-chauds sur tout l'appareil, {rayon en demi-formats de rangee,
     * force}. En additif, la couleur seule ne sature jamais au blanc (un orange n'ajoute pas de bleu)
     * ; il faut ajouter du blanc, et assez pour que la carrosserie disparaisse dans la tache, comme
     * a la camera devant un vrai blinder. Plusieurs disques proches se cumulent en un plateau.
     */
    private static final float[][] GLARE_LAYERS = {
            {1.6f, 1.0f}, {2.4f, 1.0f}, {3.2f, 1.0f}, {4.5f, 0.9f}
    };
    private static final float GLARE_WHITEN = 0.85f;
    /** Couronne dans la haze, dans la couleur de l'appareil, autour de l'eblouissement. */
    private static final float[][] HAZE_LAYERS = {
            {6.0f, 0.5f}, {10.0f, 0.35f}, {16.0f, 0.2f}
    };
    /** Part de l'eblouissement et de la couronne : une lampe seule, puis huit lampes. */
    private static final float ARRAY_SHARE_ONE_LAMP = 0.6f;
    private static final float ARRAY_SHARE_EIGHT_LAMPS = 1.0f;
    /** Pull the legacy blinder glow toward the camera so it clears the housing. */
    private static final float TOWARD_CAMERA = 0.6f;
    /**
     * L'eblouissement n'existe que devant l'appareil : plein quand la camera est a moins d'une
     * soixantaine de degres de l'axe (cos > 0.5), nul derriere. On ne voit pas la lumiere d'un
     * blinder depuis son dos.
     */
    private static final float FRONT_FULL_COS = 0.5f;
    private static final float FRONT_NONE_COS = -0.1f;
    /** L'eblouissement tombe vite avec le dimmer (a moitie, on revoit l'appareil), la couronne lentement. */
    private static final float GLARE_LEVEL_EXPONENT = 1.2f;
    private static final float HAZE_LEVEL_EXPONENT = 0.6f;

    private BlinderRenderHelper() {
    }

    /**
     * Dessine les lampes allumees : par lampe un verre emissif dans le plan de la lentille, un
     * disque rond et brulant tourne vers la camera et un bloom serre ; pour la rangee, deux
     * disques larges dans la couleur de l'appareil, la lueur dans la haze. A appeler depuis un
     * LazyRenderer.
     *
     * @param viewPose      pose recue par le LazyRenderer (vue camera)
     * @param headTransform place une pose fraiche sur la tete : decalage du bloc puis preparePoseStack
     */
    public static void renderLamps(BaseLightBlockEntity blockEntity, MultiBufferSource.BufferSource bufferSource,
                                   PoseStack viewPose, Camera camera, float partialTick,
                                   Consumer<PoseStack> headTransform, Lamps lamps) {
        float intensity = StrobeRenderHelper.renderedIntensity(blockEntity, partialTick);
        if (intensity <= 0f) {
            return;
        }
        float level = Math.min(1f, intensity / 255f);
        int color = blockEntity.getColour();
        int hot = whiten(color, LAMP_WHITEN);
        int white = whiten(color, GLARE_WHITEN);
        int hr = (hot >> 16) & 0xFF;
        int hg = (hot >> 8) & 0xFF;
        int hb = hot & 0xFF;
        int a = (int) (level * 255f);
        float lampRadius = Math.max(lamps.halfW(), lamps.halfH());

        // Axe de l'appareil et position de la rangee dans le repere camera.
        StrobeVisualEffects.Face array = lamps.array();
        PoseStack arrayWorld = new PoseStack();
        headTransform.accept(arrayWorld);
        array.apply(arrayWorld);
        Matrix4f am = arrayWorld.last().pose();
        Vector3f arrayCentre = new Vector3f(am.m30(), am.m31(), am.m32());
        Vector3f forward = new Vector3f(-am.m20(), -am.m21(), -am.m22()).normalize();
        float dist = arrayCentre.length();
        Vector3f toCamera = dist > 1.0e-3f ? new Vector3f(arrayCentre).negate().div(dist) : new Vector3f(0f, 0f, 1f);
        float front = smoothstep(FRONT_NONE_COS, FRONT_FULL_COS, forward.dot(toCamera));
        if (front <= 0f) {
            // Vue de dos : seul le verre des lampes, que la carrosserie cache de toute facon.
            return;
        }
        float glare = (float) Math.pow(level, GLARE_LEVEL_EXPONENT) * front;
        float haze = (float) Math.pow(level, HAZE_LEVEL_EXPONENT) * front;
        // A pleine puissance de face, les lampes disparaissent dans l'eblouissement : les quatre
        // cellules ne se distinguent plus. Elles reviennent quand l'appareil baisse.
        float lampShow = 1f - glare;

        for (float x : lamps.xs()) {
            for (float y : lamps.ys()) {
                if (lampShow <= 0.01f) {
                    continue;
                }
                // Le verre de la lampe est fondu (pas additif) et se dessine apres l'eblouissement :
                // a pleine puissance il peindrait quatre carres orange sur le blanc. Il suit lampShow.
                StrobeVisualEffects.Face lamp = new StrobeVisualEffects.Face(x, y, lamps.z(), lamps.halfW(), lamps.halfH(), false);
                viewPose.pushPose();
                headTransform.accept(viewPose);
                lamp.apply(viewPose);
                if (lamps == Lamps.STAGE_BLAZE) {
                    renderCircularFace(bufferSource, viewPose, lampRadius, hr, hg, hb, (int) (a * lampShow));
                } else {
                    StrobeVisualEffects.renderFace(bufferSource, viewPose, lamp, hr, hg, hb, (int) (a * lampShow));
                }
                viewPose.popPose();
                Vector3f centre = lamps == Lamps.STAGE_BLAZE
                        ? placeDiscAtLens(worldCentre(headTransform, lamp), forward)
                        : placeDisc(worldCentre(headTransform, lamp), forward);
                StrobeVisualEffects.renderGlowDot(bufferSource, viewPose, camera, centre, hot,
                        lampRadius * LAMP_DISC_RADIUS, level * front * lampShow);
                StrobeVisualEffects.renderGlowDot(bufferSource, viewPose, camera, centre, color,
                        lampRadius * LAMP_BLOOM_RADIUS, haze * LAMP_BLOOM_STRENGTH * lampShow);
            }
        }

        Vector3f glareCentre = lamps == Lamps.STAGE_BLAZE
                ? placeDiscAtLens(arrayCentre, forward) : placeDisc(arrayCentre, forward);
        float arrayRadius = Math.max(array.halfW(), array.halfH());
        float share = ARRAY_SHARE_ONE_LAMP + (ARRAY_SHARE_EIGHT_LAMPS - ARRAY_SHARE_ONE_LAMP)
                * Math.min(1f, (lamps.count() - 1) / 7f);
        // L'eblouissement : la carrosserie disparait dans un pave blanc-chaud.
        for (float[] layer : GLARE_LAYERS) {
            StrobeVisualEffects.renderGlowDot(bufferSource, viewPose, camera, glareCentre, white,
                    arrayRadius * layer[0], glare * share * layer[1]);
        }
        // La couronne coloree dans la haze.
        for (float[] layer : HAZE_LAYERS) {
            StrobeVisualEffects.renderGlowDot(bufferSource, viewPose, camera, glareCentre, color,
                    arrayRadius * layer[0], haze * share * layer[1]);
        }
    }

    /** Devant la face le long de l'axe de l'appareil, puis un peu vers la camera. */
    private static Vector3f placeDisc(Vector3f facePoint, Vector3f forward) {
        return towardCamera(facePoint);
    }

    private static Vector3f placeDiscAtLens(Vector3f facePoint, Vector3f forward) {
        // Camera-space pull changes depth without shifting the projected centre of the lens.
        // It keeps the camera-facing glare disc clear of the diffuser at oblique angles.
        return towardCamera(new Vector3f(facePoint).add(new Vector3f(forward).mul(0.035f)));
    }

    /** The same blinder face buffer, tessellated to match the round diffuser. */
    private static void renderCircularFace(MultiBufferSource.BufferSource buffers, PoseStack pose,
                                           float radius, int r, int g, int b, int a) {
        VertexConsumer vertices = buffers.getBuffer(ExtraLightsRenderTypes.BEAM);
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        for (int i = 0; i < 24; i++) {
            double p0 = i * Math.PI / 12.0;
            double p1 = (i + 1) * Math.PI / 12.0;
            addVertex(vertices, matrix, normal, r, g, b, a, 0, 0, 0);
            addVertex(vertices, matrix, normal, r, g, b, a,
                    radius * (float) Math.cos(p1), radius * (float) Math.sin(p1), 0);
            addVertex(vertices, matrix, normal, r, g, b, a,
                    radius * (float) Math.cos(p0), radius * (float) Math.sin(p0), 0);
            addVertex(vertices, matrix, normal, r, g, b, a, 0, 0, 0);
        }
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Math.max(0f, Math.min(1f, (x - edge0) / (edge1 - edge0)));
        return t * t * (3f - 2f * t);
    }

    /** Rapproche un point de la camera (repere camera) pour que le disque passe devant la carrosserie. */
    private static Vector3f towardCamera(Vector3f point) {
        float dist = point.length();
        if (dist < 1.0e-3f) {
            return point;
        }
        float pull = Math.min(TOWARD_CAMERA, dist * 0.5f);
        return new Vector3f(point).mul(1f - pull / dist);
    }

    /** Centre d'une face dans le repere camera (sans la vue), la ou les disques se dessinent. */
    private static Vector3f worldCentre(Consumer<PoseStack> headTransform, StrobeVisualEffects.Face face) {
        PoseStack world = new PoseStack();
        headTransform.accept(world);
        face.apply(world);
        Matrix4f m = world.last().pose();
        return new Vector3f(m.m30(), m.m31(), m.m32());
    }

    private static int whiten(int color, float amount) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        r += (int) ((255 - r) * amount);
        g += (int) ((255 - g) * amount);
        b += (int) ((255 - b) * amount);
        return (r << 16) | (g << 8) | b;
    }

    /** Ancien rendu : rectangle colore sur la face, sans coeur ni halo. */
    public static void renderFace(
            BaseLightBlockEntity blockEntity,
            MultiBufferSource.BufferSource bufferSource,
            PoseStack poseStack,
            float partialTicks,
            FaceQuad face
    ) {
        float intensity = StrobeRenderHelper.renderedIntensity(blockEntity, partialTicks);
        if (intensity <= 0f) {
            return;
        }

        int color = blockEntity.getColour();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) intensity;

        VertexConsumer beamConsumer = bufferSource.getBuffer(ExtraLightsRenderTypes.BEAM);
        poseStack.pushPose();
        poseStack.translate(face.x(), face.y(), face.z());
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        addVertex(beamConsumer, matrix, normal, r, g, b, a, -face.halfW(), face.halfH(), 0f);
        addVertex(beamConsumer, matrix, normal, r, g, b, a, face.halfW(), face.halfH(), 0f);
        addVertex(beamConsumer, matrix, normal, r, g, b, a, face.halfW(), -face.halfH(), 0f);
        addVertex(beamConsumer, matrix, normal, r, g, b, a, -face.halfW(), -face.halfH(), 0f);
        poseStack.popPose();
    }

    public static Vec3 lazyRenderPos(BaseLightBlockEntity blockEntity) {
        return blockEntity.getBlockPos().getCenter();
    }

    private static void addVertex(
            VertexConsumer builder,
            Matrix4f matrix,
            Matrix3f normal,
            int r,
            int g,
            int b,
            int a,
            float x,
            float y,
            float z
    ) {
        builder.vertex(matrix, x, y, z).color(r, g, b, a).endVertex();
    }
}
