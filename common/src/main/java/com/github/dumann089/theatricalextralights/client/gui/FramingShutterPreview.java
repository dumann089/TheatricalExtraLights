package com.github.dumann089.theatricalextralights.client.gui;

import com.github.dumann089.theatricalextralights.util.FramingShutterState;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Dessin 2D du module de couteaux, vu de face comme sur l'apercu Shapers de grandMA :
 * disque du faisceau, lames en convention A/B, rotation du module. Meme geometrie que
 * {@code bladeMask()} dans les shaders, pour que l'apercu corresponde au rendu.
 *
 * <p>La zone eclairee est l'intersection du disque et de 4 demi-plans, donc convexe :
 * chaque ligne de pixels se reduit a un seul intervalle, dessine en un {@code fill}.
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
     * Dessine l'apercu dans le carre (x, y, size). {@code snap} peut etre null : disque
     * ouvert. L'axe v du faisceau pointe vers le haut de l'ecran.
     */
    public static void draw(GuiGraphics g, int x, int y, int size, FramingShutterState.Snapshot snap) {
        int cx2 = 2 * x + size;         // centre * 2 pour rester en entiers
        int cy2 = 2 * y + size;
        float radius = size / 2f - 1.5f;

        g.fill(x, y, x + size, y + size, COLOR_BG);
        TelUi.outline(g, x, y, size, size, TelUi.BORDER_SOFT);

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

        // Anneau + disque, ligne par ligne.
        for (int py = 0; py < size; py++) {
            float v = -((py + 0.5f) - size / 2f) / radius; // v vers le haut
            float chord2 = 1f - v * v;
            if (chord2 <= 0f) {
                continue;
            }
            float half = (float) Math.sqrt(chord2);
            float lo = -half;
            float hi = half;

            // Occultation : l'anneau sombre couvre toute la corde, la partie eclairee est
            // l'intervalle restant apres les 4 demi-plans.
            int rowY = y + py;
            int ringX0 = Math.round(cx2 / 2f + lo * radius);
            int ringX1 = Math.round(cx2 / 2f + hi * radius);
            g.fill(ringX0, rowY, ringX1, rowY + 1, COLOR_BLADE);

            float litLo = lo;
            float litHi = hi;
            for (int i = 0; i < count && litLo < litHi; i++) {
                float a = mx[i];
                float c = md[i] - my[i] * v;
                if (Math.abs(a) < 1e-5f) {
                    if (c < 0f) {
                        litLo = litHi; // toute la ligne occultee
                    }
                } else if (a > 0f) {
                    litHi = Math.min(litHi, c / a);
                } else {
                    litLo = Math.max(litLo, c / a);
                }
            }
            if (litLo < litHi) {
                int x0 = Math.round(cx2 / 2f + litLo * radius);
                int x1 = Math.round(cx2 / 2f + litHi * radius);
                if (x1 > x0) {
                    g.fill(x0, rowY, x1, rowY + 1, COLOR_LIT);
                }
            }
        }

        // Fin liseret sur le pourtour du disque
        int cyPix = cy2 / 2;
        int cxPix = cx2 / 2;
        int rPix = Math.round(radius);
        g.fill(cxPix - rPix, cyPix, cxPix - rPix + 1, cyPix + 1, COLOR_RING);
        g.fill(cxPix + rPix - 1, cyPix, cxPix + rPix, cyPix + 1, COLOR_RING);
        g.fill(cxPix, cyPix - rPix, cxPix + 1, cyPix - rPix + 1, COLOR_RING);
        g.fill(cxPix, cyPix + rPix - 1, cxPix + 1, cyPix + rPix, COLOR_RING);
    }
}
