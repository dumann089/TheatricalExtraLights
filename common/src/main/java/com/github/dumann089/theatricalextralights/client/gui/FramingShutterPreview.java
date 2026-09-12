package com.github.dumann089.theatricalextralights.client.gui;

import com.github.dumann089.theatricalextralights.util.FramingShutterState;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Dessin 2D du faisceau vu de face, comme l'apercu Shapers de grandMA : gobo courant
 * (avec sa rotation), disque du faisceau, lames en convention A/B, rotation du module.
 * Meme geometrie que {@code bladeMask()} dans les shaders, pour que l'apercu corresponde
 * au rendu.
 *
 * <p>La zone eclairee est l'intersection du disque et de 4 demi-plans, donc convexe :
 * chaque ligne de pixels se reduit a un seul intervalle ; on assombrit ce qui est en
 * dehors (hors disque et derriere les lames) ligne par ligne.
 */
public final class FramingShutterPreview {

    /** Rayon du disque ou le bord des lames est completement sorti (voir le shader). */
    private static final float R = 1.15f;

    public static final int COLOR_LIT = 0xFFF6ECD2;
    public static final int COLOR_BLADE = 0xFF2C2C33;
    public static final int COLOR_RING = 0xFF44444E;
    public static final int COLOR_BG = TelUi.BG_SUNKEN;

    private static final float[][] NORMALS = {
            {0f, 1f},   // lame 1 : haut
            {1f, 0f},   // lame 2 : droite
            {0f, -1f},  // lame 3 : bas
            {-1f, 0f}   // lame 4 : gauche
    };

    private FramingShutterPreview() {
    }

    /**
     * Dessine l'apercu dans le carre (x, y, size).
     *
     * @param snap        etat des couteaux, null = disque ouvert
     * @param gobo        texture du gobo courant, null = disque uni
     * @param goboRotDeg  rotation du gobo en degres
     * @param uiScale     echelle appliquee par l'ecran (pour le scissor, qui ignore la pose)
     */
    public static void draw(GuiGraphics g, int x, int y, int size, FramingShutterState.Snapshot snap,
                            ResourceLocation gobo, float goboRotDeg, float uiScale) {
        float cx = x + size / 2f;
        float cy = y + size / 2f;
        float radius = size / 2f - 1.5f;

        g.fill(x, y, x + size, y + size, COLOR_BG);
        TelUi.outline(g, x, y, size, size, TelUi.BORDER_SOFT);

        // Gobo : texture tournee autour du centre, teintee couleur lampe, coupee au carre.
        if (gobo != null) {
            int inner = size - 2;
            g.enableScissor(
                    (int) Math.floor((x + 1) * uiScale), (int) Math.floor((y + 1) * uiScale),
                    (int) Math.ceil((x + 1 + inner) * uiScale), (int) Math.ceil((y + 1 + inner) * uiScale));
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0f);
            g.pose().mulPose(Axis.ZP.rotationDegrees(goboRotDeg));
            g.pose().translate(-cx, -cy, 0f);
            g.setColor(0.965f, 0.925f, 0.82f, 1.0f);
            g.blit(gobo, x + 1, y + 1, 0, 0, inner, inner, inner, inner);
            g.setColor(1f, 1f, 1f, 1f);
            g.pose().popPose();
            g.disableScissor();
        }

        // Contraintes des lames en espace ecran : m'.x * px + m'.y * py <= d  (px, py en unites de rayon)
        int count = 0;
        float[] mx = new float[4];
        float[] my = new float[4];
        float[] md = new float[4];
        if (snap != null) {
            float cr = (float) Math.cos(snap.frameRotation());
            float sr = (float) Math.sin(snap.frameRotation());
            for (int i = 0; i < 4; i++) {
                float a = snap.insertionA()[i];
                float b = snap.insertionB()[i];
                if (a <= 0.0005f && b <= 0.0005f) {
                    continue;
                }
                float nx = NORMALS[i][0];
                float ny = NORMALS[i][1];
                float tx = ny;
                float ty = -nx;
                float ax = nx * (R - 2f * R * a) - tx * R;
                float ay = ny * (R - 2f * R * a) - ty * R;
                float bx = nx * (R - 2f * R * b) + tx * R;
                float by = ny * (R - 2f * R * b) + ty * R;
                float ex = bx - ax;
                float ey = by - ay;
                float len = (float) Math.sqrt(ex * ex + ey * ey);
                float m0x = ey / len;
                float m0y = -ex / len;
                if (m0x * nx + m0y * ny < 0f) {
                    m0x = -m0x;
                    m0y = -m0y;
                }
                // Le shader tourne le point par -rotation ; on tourne la normale par +rotation.
                mx[count] = m0x * cr - m0y * sr;
                my[count] = m0x * sr + m0y * cr;
                md[count] = ax * m0x + ay * m0y;
                count++;
            }
        }

        int left = x + 1;
        int right = x + size - 1;
        for (int py = 1; py < size - 1; py++) {
            int rowY = y + py;
            float v = -((py + 0.5f) - size / 2f) / radius; // v vers le haut
            float chord2 = 1f - v * v;
            if (chord2 <= 0f) {
                // Hors du disque : toute la ligne est fond.
                g.fill(left, rowY, right, rowY + 1, COLOR_BG);
                continue;
            }
            float half = (float) Math.sqrt(chord2);
            int ringX0 = Math.round(cx - half * radius);
            int ringX1 = Math.round(cx + half * radius);

            // Coins hors du disque
            if (ringX0 > left) {
                g.fill(left, rowY, ringX0, rowY + 1, COLOR_BG);
            }
            if (ringX1 < right) {
                g.fill(ringX1, rowY, right, rowY + 1, COLOR_BG);
            }

            // Intervalle eclaire apres les 4 demi-plans
            float litLo = -half;
            float litHi = half;
            for (int i = 0; i < count && litLo < litHi; i++) {
                float a = mx[i];
                float c = md[i] - my[i] * v;
                if (Math.abs(a) < 1e-5f) {
                    if (c < 0f) {
                        litLo = litHi;
                    }
                } else if (a > 0f) {
                    litHi = Math.min(litHi, c / a);
                } else {
                    litLo = Math.max(litLo, c / a);
                }
            }

            if (gobo == null) {
                // Pas de texture : la partie eclairee est un aplat.
                if (litLo < litHi) {
                    int x0 = Math.round(cx + litLo * radius);
                    int x1 = Math.round(cx + litHi * radius);
                    if (x1 > x0) {
                        g.fill(x0, rowY, x1, rowY + 1, COLOR_LIT);
                    }
                }
                // Le reste de la corde est lame
                if (litLo >= litHi) {
                    g.fill(ringX0, rowY, ringX1, rowY + 1, COLOR_BLADE);
                } else {
                    int x0 = Math.round(cx + litLo * radius);
                    int x1 = Math.round(cx + litHi * radius);
                    if (x0 > ringX0) g.fill(ringX0, rowY, x0, rowY + 1, COLOR_BLADE);
                    if (x1 < ringX1) g.fill(x1, rowY, ringX1, rowY + 1, COLOR_BLADE);
                }
            } else {
                // Texture deja dessinee : on assombrit seulement ce que les lames cachent.
                if (litLo >= litHi) {
                    g.fill(ringX0, rowY, ringX1, rowY + 1, COLOR_BLADE);
                } else {
                    int x0 = Math.round(cx + litLo * radius);
                    int x1 = Math.round(cx + litHi * radius);
                    if (x0 > ringX0) g.fill(ringX0, rowY, x0, rowY + 1, COLOR_BLADE);
                    if (x1 < ringX1) g.fill(x1, rowY, ringX1, rowY + 1, COLOR_BLADE);
                }
            }
        }

        // Reperes fins sur le pourtour du disque
        int cxPix = Math.round(cx);
        int cyPix = Math.round(cy);
        int rPix = Math.round(radius);
        g.fill(cxPix - rPix, cyPix, cxPix - rPix + 1, cyPix + 1, COLOR_RING);
        g.fill(cxPix + rPix - 1, cyPix, cxPix + rPix, cyPix + 1, COLOR_RING);
        g.fill(cxPix, cyPix - rPix, cxPix + 1, cyPix - rPix + 1, COLOR_RING);
        g.fill(cxPix, cyPix + rPix - 1, cxPix + 1, cyPix + rPix, COLOR_RING);
    }
}
