package com.github.dumann089.theatricalextralights.client;

import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Rendu du flash d'un strobe : face emissive, coeur surexpose, halo et lueur dans la haze,
 * tous additifs. L'eclairage au sol passe par la lumiere dynamique Theatrical/Shimmer.
 */
public final class StrobeVisualEffects {

    /** Face emissive d'un strobe, en coordonnees locales de la tete. */
    public record Face(float x, float y, float z, float halfW, float halfH, boolean frontIsPositiveZ) {
        public static final Face STROBE = new Face(0.5f, 0.65f, 0.37f, 0.4375f, 0.21875f, false);
        /** Zone LED de l'Atomic Strobe, cadre plastique exclu (voir AtomicStrobeRenderer). */
        public static final Face ATOMIC = new Face(8.0f / 16f, 6.985f / 16f, 11.55f / 16f,
                6.33f / 16f, 3.235f / 16f, true);
        /** Face de la tete de l'Atomic Tilt, dans le repere prepare par son renderer (tilt applique) : l'avant est -Z. */
        public static final Face ATOMIC_TILT = new Face(0.509f, 0.573f, 0.346f, 0.3828f, 0.25f, false);

        /** Place la pose au centre de la face, -Z sortant vers l'avant du projecteur. */
        public void apply(PoseStack poseStack) {
            poseStack.translate(x, y, z);
            if (frontIsPositiveZ) {
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }
        }
    }


    /**
     * Couches du bloom : {rayon, force, blanchiment}. Reference : video de concert (strobes
     * au-dessus du plateau, salle enfumee). L'appareil reste un petit rectangle blanc net ; le
     * halo autour est doux, sans plateau ni bord, et faible loin du centre. Le melange est
     * additif : avec douze strobes cote a cote, des forces elevees saturent en boules blanches.
     */
    private static final float[][] BLOOM_LAYERS = {
            {0.45f, 0.80f, 1.00f},
            {1.40f, 0.30f, 0.85f},
            {3.50f, 0.12f, 0.60f},
            {7.00f, 0.05f, 0.35f},
    };
    /** Force du bloom vu de dos ou de cote, relative a l'axe : la haze diffuse tout autour. */
    private static final float BLOOM_BACK = 0.45f;
    private static final float BLOOM_TOWARD_CAMERA = 0.8f;
    /** Grossissement par bloc de distance, pour garder une taille apparente au loin. */
    private static final float BLOOM_DISTANCE_GROWTH = 0.02f;
    /** Etendue de la frange douce autour de la face, en multiple de la face. */
    private static final float HOT_CORE_OVERSIZE = 1.5f;
    /** Blanchiment du coeur et de la frange de la face : 0 = couleur pure, pas de blanc visible. */
    private static final float FACE_WHITEN = 0.0f;
    /**
     * Teinte de la haze eclairee par un flash xenon : bleu-blanc froid sur la video, le blanc
     * de l'appareil vire au bleu dans la fumee et a la camera.
     */
    private static final int HAZE_TINT = 0xA9BEFF;
    private static final float HAZE_TINT_AMOUNT = 0.45f;

    /**
     * Lueur dans la haze devant la face : large et diffuse, elle descend vers le plateau sans
     * dessiner de cone. Faible : les disques se cumulent entre eux et avec ceux des voisins, et
     * la ou un disque traverse un mur ou un plafond, la coupe ne doit pas se voir.
     */
    private static final int AIR_GLOW_LAYERS = 8;
    private static final float AIR_GLOW_DEPTH = 8.0f;
    private static final float AIR_GLOW_NEAR_RADIUS = 1.5f;
    private static final float AIR_GLOW_FAR_RADIUS = 5.0f;
    private static final float AIR_GLOW_STRENGTH = 0.09f;
    /** Voile spherique autour de l'appareil : la haze s'allume aussi derriere et sur les cotes. */
    private static final float AIR_WASH_RADIUS = 4.0f;
    private static final float AIR_WASH_STRENGTH = 0.05f;

    /** Anneaux du degrade d'un disque : assez pour que le fondu soit lisse, pas de plateau ni de bord. */
    private static final int DISC_RINGS = 6;
    private static final int DISC_SEGMENTS = 24;

    private StrobeVisualEffects() {
    }

    /** Pose deja placee au centre de la face ({@link Face#apply}). */
    public static void renderFace(
            MultiBufferSource.BufferSource bufferSource,
            PoseStack poseStack,
            Face face,
            int r,
            int g,
            int b,
            int a
    ) {
        VertexConsumer faceConsumer = bufferSource.getBuffer(ExtraLightsRenderTypes.BEAM);
        emitFaceQuad(faceConsumer, poseStack.last().pose(), face, 1.0f, 0f, r, g, b, a);
    }

    /**
     * Surexposition de la face : le centre crame au blanc et le blanc s'estompe vers les bords,
     * qui gardent la couleur ; au-dela, une frange douce qui s'eteint sur un demi-format. Pas de
     * quad blanc plein plus grand que la face : il se lit comme un contour, pas comme un eblouissement.
     * Pose deja placee au centre de la face.
     */
    public static void renderHotCore(
            MultiBufferSource.BufferSource bufferSource,
            PoseStack poseStack,
            Face face,
            int color,
            float level
    ) {
        if (level <= 0f) {
            return;
        }
        VertexConsumer glow = bufferSource.getBuffer(ExtraLightsRenderTypes.GLOW);
        Matrix4f m = poseStack.last().pose();
        float w = face.halfW();
        float h = face.halfH();
        float z = -0.004f;

        // Coeur : plus lumineux au centre, degrade vers les bords, sans virer au blanc : la
        // couleur du strobe reste pure (demande utilisateur, un rebord blanc se voyait).
        int[] hot = whiten(color, FACE_WHITEN);
        int aCentre = alpha(level * 0.9f);
        int aEdge = alpha(level * 0.15f);
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        float cw = w * 0.35f, ch = h * 0.35f;
        // Rectangle central plein.
        quad(glow, m, -cw, ch, cw, ch, cw, -ch, -cw, -ch, z, hot, aCentre, hot, aCentre);
        // Quatre trapezes du centre aux bords de la face.
        trapezoid(glow, m, -cw, ch, cw, ch, w, h, -w, h, z, hot, aCentre, new int[]{r, g, b}, aEdge);
        trapezoid(glow, m, cw, -ch, -cw, -ch, -w, -h, w, -h, z, hot, aCentre, new int[]{r, g, b}, aEdge);
        trapezoid(glow, m, cw, ch, cw, -ch, w, -h, w, h, z, hot, aCentre, new int[]{r, g, b}, aEdge);
        trapezoid(glow, m, -cw, -ch, -cw, ch, -w, h, -w, -h, z, hot, aCentre, new int[]{r, g, b}, aEdge);

        // Frange : du bord de la face (teinte haze, alpha moyen) a zero au-dela.
        int[] fringe = whiten(color, FACE_WHITEN);
        int aFringe = alpha(level * 0.35f);
        float fw = w * HOT_CORE_OVERSIZE, fh = h * HOT_CORE_OVERSIZE;
        float z2 = -0.006f;
        trapezoid(glow, m, -w, h, w, h, fw, fh, -fw, fh, z2, fringe, aFringe, fringe, 0);
        trapezoid(glow, m, w, -h, -w, -h, -fw, -fh, fw, -fh, z2, fringe, aFringe, fringe, 0);
        trapezoid(glow, m, w, h, w, -h, fw, -fh, fw, fh, z2, fringe, aFringe, fringe, 0);
        trapezoid(glow, m, -w, -h, -w, h, -fw, fh, -fw, -fh, z2, fringe, aFringe, fringe, 0);
    }

    /** Quad plein : deux premiers sommets en {@code c0}/{@code a0}, deux derniers en {@code c1}/{@code a1}. */
    private static void quad(VertexConsumer vc, Matrix4f m,
                             float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                             float z, int[] c0, int a0, int[] c1, int a1) {
        vc.vertex(m, x0, y0, z).color(c0[0], c0[1], c0[2], a0).endVertex();
        vc.vertex(m, x1, y1, z).color(c0[0], c0[1], c0[2], a0).endVertex();
        vc.vertex(m, x2, y2, z).color(c1[0], c1[1], c1[2], a1).endVertex();
        vc.vertex(m, x3, y3, z).color(c1[0], c1[1], c1[2], a1).endVertex();
    }

    /** Trapeze : arete interieure (x0,y0)-(x1,y1) en {@code cIn}, arete exterieure (x2,y2)-(x3,y3) en {@code cOut}. */
    private static void trapezoid(VertexConsumer vc, Matrix4f m,
                                  float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                                  float z, int[] cIn, int aIn, int[] cOut, int aOut) {
        quad(vc, m, x0, y0, x1, y1, x2, y2, x3, y3, z, cIn, aIn, cOut, aOut);
    }

    /**
     * Lumiere du flash vue de la camera : un halo eblouissant sur la face, attenue hors de l'axe
     * (vu de cote ou de dos, un strobe n'eblouit pas), et une lueur diffuse dans la haze juste
     * devant, visible sous tous les angles. Pas de cone : un strobe arrose large, il ne dessine
     * pas de faisceau.
     *
     * @param viewPose  pose recue par le LazyRenderer (vue camera)
     * @param faceWorld pose relative a la camera, sans la vue, placee au centre de la face
     */
    public static void renderHalo(
            MultiBufferSource.BufferSource bufferSource,
            PoseStack viewPose,
            Camera camera,
            PoseStack faceWorld,
            int color,
            float level
    ) {
        if (level <= 0f) {
            return;
        }
        Matrix4f face = faceWorld.last().pose();
        Vector3f origin = new Vector3f(face.m30(), face.m31(), face.m32());
        Vector3f forward = new Vector3f(-face.m20(), -face.m21(), -face.m22()).normalize();
        Vector3f right = new Vector3f(camera.getLeftVector());
        Vector3f up = new Vector3f(camera.getUpVector());
        Matrix4f view = viewPose.last().pose();
        VertexConsumer vc = bufferSource.getBuffer(ExtraLightsRenderTypes.GLOW);
        int[] core = hazeTint(color, 0.75f);
        int tint = packed(hazeTint(color, 0.35f));

        if (TheatricalExtraLightsConfig.isVolumetricBeamEnabled()) {
            // Voile spherique centre sur la face : le flash eclaire la haze tout autour de lui.
            float wash = level * AIR_WASH_STRENGTH;
            softDisc(vc, view, origin, right, up, AIR_WASH_RADIUS, core, tint, wash);

            // Des disques de plus en plus larges et pales, poses devant la face : ils s'additionnent
            // en une lueur qui s'evase et s'eteint en quelques blocs.
            for (int i = 0; i < AIR_GLOW_LAYERS; i++) {
                float t = (i + 0.5f) / AIR_GLOW_LAYERS;
                Vector3f centre = new Vector3f(forward).mul(AIR_GLOW_DEPTH * t).add(origin);
                float radius = Mth.lerp(t, AIR_GLOW_NEAR_RADIUS, AIR_GLOW_FAR_RADIUS);
                float fade = 1f - t;
                float strength = level * AIR_GLOW_STRENGTH * fade;
                softDisc(vc, view, centre, right, up, radius, core, tint, strength);
            }
        }

        float dist = origin.length();
        if (dist < 1.0e-3f) {
            return;
        }
        Vector3f toCamera = new Vector3f(origin).negate().div(dist);
        // Bloom : une source aussi lumineuse bave sur l'image sous tous les angles, plus fort
        // dans l'axe. Tire vers la camera pour passer devant la carrosserie du projecteur.
        float facing = Math.max(0f, forward.dot(toCamera));
        float angular = BLOOM_BACK + (1f - BLOOM_BACK) * facing;
        Vector3f centre = new Vector3f(toCamera).mul(Math.min(BLOOM_TOWARD_CAMERA, dist * 0.5f)).add(origin);
        // Taille apparente minimale : comme un vrai bloom (effet d'image), il ne disparait pas au loin.
        float grow = 1f + dist * BLOOM_DISTANCE_GROWTH;
        for (float[] layer : BLOOM_LAYERS) {
            float strength = level * angular * layer[1];
            float radius = layer[0] * grow * (0.7f + 0.3f * level);
            int[] layerCore = hazeTint(color, layer[2]);
            int layerRing = packed(hazeTint(color, layer[2] * 0.6f));
            softDisc(vc, view, centre, right, up, radius, layerCore, layerRing, strength);
        }
    }

    /**
     * Lueur douce d'une LED : un disque tourne vers la camera, centre sur {@code centre}
     * (repere relatif a la camera, sans la vue). Sert aux pixels des barres LED.
     */
    public static void renderGlowDot(MultiBufferSource.BufferSource bufferSource, PoseStack viewPose, Camera camera,
                                     Vector3f centre, int color, float radius, float strength) {
        renderGlowDot(bufferSource, viewPose, camera, centre, color, radius, strength, false);
    }

    /**
     * @param lowDetail trois anneaux de douze segments au lieu de six de vingt-quatre : pour les LED
     *                  minuscules des barres denses, ou des dizaines de disques se chevauchent.
     */
    public static void renderGlowDot(MultiBufferSource.BufferSource bufferSource, PoseStack viewPose, Camera camera,
                                     Vector3f centre, int color, float radius, float strength, boolean lowDetail) {
        if (strength <= 0f) {
            return;
        }
        VertexConsumer vc = bufferSource.getBuffer(ExtraLightsRenderTypes.GLOW);
        Vector3f right = new Vector3f(camera.getLeftVector());
        Vector3f up = new Vector3f(camera.getUpVector());
        softDisc(vc, viewPose.last().pose(), centre, right, up, radius, whiten(color, 0.35f), color, strength,
                lowDetail ? 3 : DISC_RINGS, lowDetail ? 12 : DISC_SEGMENTS);
    }

    /**
     * Disque tourne vers la camera, degrade en anneaux : alpha = a0 * (1 - r/R)^2.4, soit un
     * pic etroit au centre et une longue queue qui s'eteint bien avant le bord. Le coeur porte
     * la couleur {@code core}, les anneaux la couleur {@code ring}.
     */
    private static void softDisc(VertexConsumer vc, Matrix4f view, Vector3f centre, Vector3f right, Vector3f up,
                                 float radius, int[] core, int ring, float strength) {
        softDisc(vc, view, centre, right, up, radius, core, ring, strength, DISC_RINGS, DISC_SEGMENTS);
    }

    private static void softDisc(VertexConsumer vc, Matrix4f view, Vector3f centre, Vector3f right, Vector3f up,
                                 float radius, int[] core, int ring, float strength, int rings, int segments) {
        int a0 = alpha(strength);
        if (a0 <= 0) {
            return;
        }
        int rr = (ring >> 16) & 0xFF;
        int rg = (ring >> 8) & 0xFF;
        int rb = ring & 0xFF;
        for (int k = 0; k < rings; k++) {
            float t0 = (float) k / rings;
            float t1 = (float) (k + 1) / rings;
            float r0 = radius * t0;
            float r1 = radius * t1;
            int a0k = alpha(strength * (float) Math.pow(1f - t0, 2.4));
            int a1k = k + 1 == rings ? 0 : alpha(strength * (float) Math.pow(1f - t1, 2.4));
            // Le centre porte la couleur du coeur ; des le premier anneau, celle de la haze.
            int cr = k == 0 ? core[0] : rr, cg = k == 0 ? core[1] : rg, cb = k == 0 ? core[2] : rb;
            for (int i = 0; i < segments; i++) {
                double p0 = i * (Math.PI * 2.0) / segments;
                double p1 = (i + 1) * (Math.PI * 2.0) / segments;
                float c0 = (float) Math.cos(p0), s0 = (float) Math.sin(p0);
                float c1 = (float) Math.cos(p1), s1 = (float) Math.sin(p1);
                haloVertex(vc, view, centre, right, up, c0 * r0, s0 * r0, cr, cg, cb, a0k);
                haloVertex(vc, view, centre, right, up, c0 * r1, s0 * r1, rr, rg, rb, a1k);
                haloVertex(vc, view, centre, right, up, c1 * r1, s1 * r1, rr, rg, rb, a1k);
                haloVertex(vc, view, centre, right, up, c1 * r0, s1 * r0, cr, cg, cb, a0k);
            }
        }
    }

    private static void haloVertex(VertexConsumer vc, Matrix4f view, Vector3f centre, Vector3f right, Vector3f up,
                                   float du, float dv, int r, int g, int b, int a) {
        float x = centre.x + right.x * du + up.x * dv;
        float y = centre.y + right.y * du + up.y * dv;
        float z = centre.z + right.z * du + up.z * dv;
        vc.vertex(view, x, y, z).color(r, g, b, a).endVertex();
    }

    private static void emitFaceQuad(VertexConsumer vc, Matrix4f m, Face face, float scale, float z,
                                     int r, int g, int b, int a) {
        float w = face.halfW() * scale;
        float h = face.halfH() * scale;
        vc.vertex(m, -w, h, z).color(r, g, b, a).endVertex();
        vc.vertex(m, w, h, z).color(r, g, b, a).endVertex();
        vc.vertex(m, w, -h, z).color(r, g, b, a).endVertex();
        vc.vertex(m, -w, -h, z).color(r, g, b, a).endVertex();
    }

    private static int[] whiten(int color, float amount) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return new int[]{
                (int) (r + (255 - r) * amount),
                (int) (g + (255 - g) * amount),
                (int) (b + (255 - b) * amount)
        };
    }

    /**
     * Blanchit la couleur puis la tire vers la teinte de la haze : plus le blanchiment est fort,
     * plus la teinte froide prend, comme le halo bleu-blanc autour d'un strobe blanc sur la video.
     */
    private static int[] hazeTint(int color, float whiteAmount) {
        int[] w = whiten(color, whiteAmount);
        float k = HAZE_TINT_AMOUNT * whiteAmount;
        int hr = (HAZE_TINT >> 16) & 0xFF;
        int hg = (HAZE_TINT >> 8) & 0xFF;
        int hb = HAZE_TINT & 0xFF;
        return new int[]{
                (int) (w[0] + (hr - w[0]) * k),
                (int) (w[1] + (hg - w[1]) * k),
                (int) (w[2] + (hb - w[2]) * k)
        };
    }

    private static int packed(int[] rgb) {
        return (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
    }

    private static int alpha(float level) {
        return Math.max(0, Math.min(255, (int) (level * 255f)));
    }
}
