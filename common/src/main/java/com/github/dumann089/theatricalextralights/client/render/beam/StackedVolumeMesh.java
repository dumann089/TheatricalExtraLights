package com.github.dumann089.theatricalextralights.client.render.beam;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Filled cone mesh: stacked discs, crossed longitudinal planes, and a soft 8-sided shell.
 * Coordinates are the same space as {@link BeamRenderData#origin()} (block-local).
 */
public final class StackedVolumeMesh {

    public static final int FLOATS_PER_VERT = 6;
    public static final int VERTS_PER_QUAD = 4;
    public static final int FLOATS_PER_QUAD = FLOATS_PER_VERT * VERTS_PER_QUAD;
    public static final int CENTER_STRIDE = 6;

    private StackedVolumeMesh() {
    }

    public static int discCount(float scanLen) {
        int discs = Math.round(scanLen * 0.75f);
        return Math.max(32, Math.min(64, discs));
    }

    public static int estimateQuads(int discs) {
        int segs = Math.max(1, discs - 1);
        return discs * 2 + segs * 2 + segs * 8;
    }

    public static int estimateFloats(int discs) {
        return estimateQuads(discs) * FLOATS_PER_QUAD;
    }

    public static float sliceAlpha(float dist, float scanLen, float density, float maxAlpha,
                                   float fadeLen, boolean hitBlock) {
        float dRef = Math.max(scanLen * 0.35f, 1.0f);
        float dn = dist / dRef;
        float falloff = 1.0f / (1.0f + dn * dn);
        float along = (float) Math.exp(-(dist / Math.max(scanLen, 1.0e-3f)) * density);
        float endFade = 1.0f;
        if (fadeLen > 0.0f && !hitBlock) {
            float left = scanLen - dist;
            if (left < fadeLen) {
                float t = Math.max(left / fadeLen, 0.0f);
                endFade = t * t * t;
            }
        }
        return along * falloff * endFade * maxAlpha;
    }

    public static float radiusAt(float dist, float tanHalfAngle, float baseRadius) {
        if (tanHalfAngle < 0.001f) {
            return baseRadius;
        }
        return Math.max(baseRadius, dist * tanHalfAngle);
    }

    /**
     * @return number of quads written
     */
    public static int build(float[] dest, BeamRenderData data, float scanLen,
                            float density, float maxAlpha, float fadeLen, boolean hitBlock, int discs) {
        Axes axes = Axes.from(data);
        int idx = 0;
        int quads = 0;
        int segs = discs - 1;
        if (segs < 1) {
            return 0;
        }

        float[] cx = new float[discs];
        float[] cy = new float[discs];
        float[] cz = new float[discs];
        float[] rW = new float[discs];
        float[] rH = new float[discs];
        float[] al = new float[discs];

        for (int i = 0; i < discs; i++) {
            float t = i / (float) (discs - 1);
            float dist = t * scanLen;
            cx[i] = (float) (data.origin().x + axes.bx * dist);
            cy[i] = (float) (data.origin().y + axes.by * dist);
            cz[i] = (float) (data.origin().z + axes.bz * dist);
            float radius = radiusAt(dist, data.tanHalfAngle(), data.baseRadius());
            rW[i] = radius * data.widthScale();
            rH[i] = radius * data.heightScale();
            al[i] = sliceAlpha(dist, scanLen, density, maxAlpha, fadeLen, hitBlock);
        }

        for (int i = 0; i < discs; i++) {
            if (al[i] <= 0.001f) {
                continue;
            }
            float a = al[i] * 0.85f;
            idx = emitDisc(dest, idx, cx[i], cy[i], cz[i], axes, rW[i], rH[i], a, 0.0);
            quads++;
            idx = emitDisc(dest, idx, cx[i], cy[i], cz[i], axes, rW[i], rH[i], a, Math.PI * 0.25);
            quads++;
        }

        for (int i = 0; i < segs; i++) {
            if (al[i] <= 0.001f && al[i + 1] <= 0.001f) {
                continue;
            }
            float a0 = al[i] * 0.45f;
            float a1 = al[i + 1] * 0.45f;
            idx = emitLongitudinal(dest, idx, cx[i], cy[i], cz[i], rW[i], rH[i], a0,
                    cx[i + 1], cy[i + 1], cz[i + 1], rW[i + 1], rH[i + 1], a1, axes, true);
            quads++;
            idx = emitLongitudinal(dest, idx, cx[i], cy[i], cz[i], rW[i], rH[i], a0,
                    cx[i + 1], cy[i + 1], cz[i + 1], rW[i + 1], rH[i + 1], a1, axes, false);
            quads++;

            float shell0 = al[i] * 0.12f;
            float shell1 = al[i + 1] * 0.12f;
            for (int s = 0; s < 8; s++) {
                double aA = s * Math.PI * 0.25;
                double aB = (s + 1) * Math.PI * 0.25;
                idx = emitShellQuad(dest, idx,
                        cx[i], cy[i], cz[i], rW[i], rH[i], shell0,
                        cx[i + 1], cy[i + 1], cz[i + 1], rW[i + 1], rH[i + 1], shell1,
                        axes, aA, aB);
                quads++;
            }
        }

        return quads;
    }

    public static int buildCenters(float[] dest, BeamRenderData data, float scanLen,
                                   float density, float maxAlpha, float fadeLen, boolean hitBlock, int discs) {
        Axes axes = Axes.from(data);
        int n = 0;
        for (int i = 0; i < discs; i++) {
            float t = i / (float) (discs - 1);
            float dist = t * scanLen;
            float alpha = sliceAlpha(dist, scanLen, density, maxAlpha, fadeLen, hitBlock) * 0.7f;
            if (alpha <= 0.001f) {
                continue;
            }
            float radius = radiusAt(dist, data.tanHalfAngle(), data.baseRadius());
            int o = n * CENTER_STRIDE;
            dest[o] = (float) (data.origin().x + axes.bx * dist);
            dest[o + 1] = (float) (data.origin().y + axes.by * dist);
            dest[o + 2] = (float) (data.origin().z + axes.bz * dist);
            dest[o + 3] = radius * data.widthScale();
            dest[o + 4] = radius * data.heightScale();
            dest[o + 5] = alpha;
            n++;
        }
        return n;
    }

    public static void emitCachedQuads(VertexConsumer vc, Matrix4f mat, float[] verts, int quadCount,
                                       int r, int g, int b, float alphaScale) {
        for (int i = 0; i < quadCount; i++) {
            int base = i * FLOATS_PER_QUAD;
            float maxA = 0.0f;
            for (int v = 0; v < 4; v++) {
                maxA = Math.max(maxA, verts[base + v * FLOATS_PER_VERT + 5] * alphaScale);
            }
            if (maxA <= 0.001f) {
                continue;
            }
            for (int v = 0; v < 4; v++) {
                int o = base + v * FLOATS_PER_VERT;
                int a = Math.min(255, Math.max(0, (int) (verts[o + 5] * alphaScale * 255.0f)));
                vc.vertex(mat, verts[o], verts[o + 1], verts[o + 2])
                        .color(r, g, b, a)
                        .uv(verts[o + 3], verts[o + 4])
                        .endVertex();
            }
        }
    }

    public static void emitCameraDiscs(VertexConsumer vc, Matrix4f mat, float[] centers, int count,
                                       double blockX, double blockY, double blockZ,
                                       double camX, double camY, double camZ,
                                       int r, int g, int b, float alphaScale) {
        Vector3f worldUp = new Vector3f(0f, 1f, 0f);
        for (int i = 0; i < count; i++) {
            int o = i * CENTER_STRIDE;
            float px = centers[o];
            float py = centers[o + 1];
            float pz = centers[o + 2];
            float rW = centers[o + 3];
            float rH = centers[o + 4];
            float alpha = centers[o + 5] * alphaScale;
            if (alpha <= 0.001f) {
                continue;
            }

            double wx = blockX + px;
            double wy = blockY + py;
            double wz = blockZ + pz;
            double dx = wx - camX;
            double dy = wy - camY;
            double dz = wz - camZ;
            double dSq = dx * dx + dy * dy + dz * dz;
            if (dSq < 0.04) {
                continue;
            }
            float camFade = 1.0f;
            if (dSq < 2.25) {
                camFade = (float) (Math.sqrt(dSq) / 1.5);
            }

            Vector3f toCam = new Vector3f((float) -dx, (float) -dy, (float) -dz);
            if (toCam.lengthSquared() < 1.0e-8f) {
                continue;
            }
            toCam.normalize();
            Vector3f right = new Vector3f();
            worldUp.cross(toCam, right);
            if (right.lengthSquared() < 1.0e-6f) {
                right.set(1f, 0f, 0f);
            } else {
                right.normalize();
            }
            Vector3f up = new Vector3f();
            toCam.cross(right, up);
            up.normalize();

            int a = Math.min(255, (int) (alpha * camFade * 255.0f));
            if (a <= 0) {
                continue;
            }
            emitBillboard(vc, mat, px, py, pz, right, up, rW, rH, r, g, b, a);
        }
    }

    private static void emitBillboard(VertexConsumer vc, Matrix4f mat,
                                      float cx, float cy, float cz,
                                      Vector3f right, Vector3f up, float rW, float rH,
                                      int r, int g, int b, int a) {
        float x0 = cx - right.x * rW - up.x * rH;
        float y0 = cy - right.y * rW - up.y * rH;
        float z0 = cz - right.z * rW - up.z * rH;
        float x1 = cx + right.x * rW - up.x * rH;
        float y1 = cy + right.y * rW - up.y * rH;
        float z1 = cz + right.z * rW - up.z * rH;
        float x2 = cx + right.x * rW + up.x * rH;
        float y2 = cy + right.y * rW + up.y * rH;
        float z2 = cz + right.z * rW + up.z * rH;
        float x3 = cx - right.x * rW + up.x * rH;
        float y3 = cy - right.y * rW + up.y * rH;
        float z3 = cz - right.z * rW + up.z * rH;
        vc.vertex(mat, x0, y0, z0).color(r, g, b, a).uv(0f, 1f).endVertex();
        vc.vertex(mat, x1, y1, z1).color(r, g, b, a).uv(1f, 1f).endVertex();
        vc.vertex(mat, x2, y2, z2).color(r, g, b, a).uv(1f, 0f).endVertex();
        vc.vertex(mat, x3, y3, z3).color(r, g, b, a).uv(0f, 0f).endVertex();
    }

    private static int emitDisc(float[] dest, int idx,
                                float cx, float cy, float cz, Axes axes,
                                float rW, float rH, float alpha, double rot) {
        double c = Math.cos(rot);
        double s = Math.sin(rot);
        double ux = axes.ux * c - axes.vx * s;
        double uy = axes.uy * c - axes.vy * s;
        double uz = axes.uz * c - axes.vz * s;
        double vx = axes.ux * s + axes.vx * c;
        double vy = axes.uy * s + axes.vy * c;
        double vz = axes.uz * s + axes.vz * c;
        idx = put(dest, idx, (float) (cx - ux * rW - vx * rH), (float) (cy - uy * rW - vy * rH), (float) (cz - uz * rW - vz * rH), 0f, 1f, alpha);
        idx = put(dest, idx, (float) (cx + ux * rW - vx * rH), (float) (cy + uy * rW - vy * rH), (float) (cz + uz * rW - vz * rH), 1f, 1f, alpha);
        idx = put(dest, idx, (float) (cx + ux * rW + vx * rH), (float) (cy + uy * rW + vy * rH), (float) (cz + uz * rW + vz * rH), 1f, 0f, alpha);
        idx = put(dest, idx, (float) (cx - ux * rW + vx * rH), (float) (cy - uy * rW + vy * rH), (float) (cz - uz * rW + vz * rH), 0f, 0f, alpha);
        return idx;
    }

    private static int emitLongitudinal(float[] dest, int idx,
                                        float x0, float y0, float z0, float rW0, float rH0, float a0,
                                        float x1, float y1, float z1, float rW1, float rH1, float a1,
                                        Axes axes, boolean alongU) {
        if (alongU) {
            idx = put(dest, idx, (float) (x0 - axes.ux * rW0), (float) (y0 - axes.uy * rW0), (float) (z0 - axes.uz * rW0), 0f, 0.5f, a0);
            idx = put(dest, idx, (float) (x0 + axes.ux * rW0), (float) (y0 + axes.uy * rW0), (float) (z0 + axes.uz * rW0), 1f, 0.5f, a0);
            idx = put(dest, idx, (float) (x1 + axes.ux * rW1), (float) (y1 + axes.uy * rW1), (float) (z1 + axes.uz * rW1), 1f, 0.5f, a1);
            idx = put(dest, idx, (float) (x1 - axes.ux * rW1), (float) (y1 - axes.uy * rW1), (float) (z1 - axes.uz * rW1), 0f, 0.5f, a1);
        } else {
            idx = put(dest, idx, (float) (x0 - axes.vx * rH0), (float) (y0 - axes.vy * rH0), (float) (z0 - axes.vz * rH0), 0f, 0.5f, a0);
            idx = put(dest, idx, (float) (x0 + axes.vx * rH0), (float) (y0 + axes.vy * rH0), (float) (z0 + axes.vz * rH0), 1f, 0.5f, a0);
            idx = put(dest, idx, (float) (x1 + axes.vx * rH1), (float) (y1 + axes.vy * rH1), (float) (z1 + axes.vz * rH1), 1f, 0.5f, a1);
            idx = put(dest, idx, (float) (x1 - axes.vx * rH1), (float) (y1 - axes.vy * rH1), (float) (z1 - axes.vz * rH1), 0f, 0.5f, a1);
        }
        return idx;
    }

    private static int emitShellQuad(float[] dest, int idx,
                                     float x0, float y0, float z0, float rW0, float rH0, float a0,
                                     float x1, float y1, float z1, float rW1, float rH1, float a1,
                                     Axes axes, double angA, double angB) {
        float cA = (float) Math.cos(angA);
        float sA = (float) Math.sin(angA);
        float cB = (float) Math.cos(angB);
        float sB = (float) Math.sin(angB);
        float u0 = (angA / (Math.PI * 2.0)) < 0 ? 0f : (float) (angA / (Math.PI * 2.0));
        float u1 = (float) (angB / (Math.PI * 2.0));
        idx = put(dest, idx,
                (float) (x0 + axes.ux * rW0 * cA + axes.vx * rH0 * sA),
                (float) (y0 + axes.uy * rW0 * cA + axes.vy * rH0 * sA),
                (float) (z0 + axes.uz * rW0 * cA + axes.vz * rH0 * sA),
                u0, 0f, a0);
        idx = put(dest, idx,
                (float) (x0 + axes.ux * rW0 * cB + axes.vx * rH0 * sB),
                (float) (y0 + axes.uy * rW0 * cB + axes.vy * rH0 * sB),
                (float) (z0 + axes.uz * rW0 * cB + axes.vz * rH0 * sB),
                u1, 0f, a0);
        idx = put(dest, idx,
                (float) (x1 + axes.ux * rW1 * cB + axes.vx * rH1 * sB),
                (float) (y1 + axes.uy * rW1 * cB + axes.vy * rH1 * sB),
                (float) (z1 + axes.uz * rW1 * cB + axes.vz * rH1 * sB),
                u1, 1f, a1);
        idx = put(dest, idx,
                (float) (x1 + axes.ux * rW1 * cA + axes.vx * rH1 * sA),
                (float) (y1 + axes.uy * rW1 * cA + axes.vy * rH1 * sA),
                (float) (z1 + axes.uz * rW1 * cA + axes.vz * rH1 * sA),
                u0, 1f, a1);
        return idx;
    }

    private static int put(float[] dest, int idx, float x, float y, float z, float u, float v, float a) {
        dest[idx++] = x;
        dest[idx++] = y;
        dest[idx++] = z;
        dest[idx++] = u;
        dest[idx++] = v;
        dest[idx++] = a;
        return idx;
    }

    private static final class Axes {
        final double bx, by, bz;
        final double ux, uy, uz;
        final double vx, vy, vz;

        private Axes(double bx, double by, double bz, double ux, double uy, double uz, double vx, double vy, double vz) {
            this.bx = bx;
            this.by = by;
            this.bz = bz;
            this.ux = ux;
            this.uy = uy;
            this.uz = uz;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
        }

        static Axes from(BeamRenderData data) {
            double rad = Math.toRadians(-data.goboRotation());
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            double ux = data.axisU().x * cos - data.axisV().x * sin;
            double uy = data.axisU().y * cos - data.axisV().y * sin;
            double uz = data.axisU().z * cos - data.axisV().z * sin;
            double vx = data.axisU().x * sin + data.axisV().x * cos;
            double vy = data.axisU().y * sin + data.axisV().y * cos;
            double vz = data.axisU().z * sin + data.axisV().z * cos;
            return new Axes(data.beamDir().x, data.beamDir().y, data.beamDir().z, ux, uy, uz, vx, vy, vz);
        }
    }
}