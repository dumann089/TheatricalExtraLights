package com.github.dumann089.theatricalextralights.laser;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Predefined laser shapes selected via the Pattern DMX channel. Each pattern
 * generates a list of {@link LaserBeam} given the size/amplitude/speed/rotation
 * channels and the current animation time. Tri-color gradient (C1→C2→C3) is
 * applied along the beam list ordering.
 */
public enum LaserPattern {
    BEAM_SIMPLE,
    LINE,
    CIRCLE,
    SQUARE,
    WAVE,
    TUNNEL,
    STAR,
    CROSS,
    TRIANGLE,
    SPIRAL,
    PARALLEL_LINES,
    DOUBLE_CIRCLE,
    BURST,
    SCATTER;

    public static LaserPattern fromDmx(int dmx) {
        int v = Math.max(0, Math.min(255, dmx));
        int bucket = v / 18;
        LaserPattern[] all = values();
        if (bucket >= all.length) {
            return all[all.length - 1];
        }
        return all[bucket];
    }

    /**
     * Whether this pattern's beams should be rendered as a continuous polyline
     * (ribbons connecting consecutive endpoints) instead of (or in addition to)
     * a fan of beams from the laser origin. Patterns that have a recognisable
     * shape benefit from polyline rendering; ones that are inherently radial or
     * random (BURST, SCATTER, BEAM_SIMPLE) do not.
     */
    public boolean usesPolyline() {
        switch (this) {
            case LINE:
            case CIRCLE:
            case SQUARE:
            case WAVE:
            case STAR:
            case TRIANGLE:
            case SPIRAL:
                return true;
            default:
                return false;
        }
    }

    /** True for closed shapes (last point connects back to first). */
    public boolean isClosed() {
        switch (this) {
            case CIRCLE:
            case SQUARE:
            case STAR:
            case TRIANGLE:
                return true;
            default:
                return false;
        }
    }

    /**
     * True when every trace of this pattern loops back on itself (rings, polygons). Open
     * traces (lines, waves, spirals) end with a dwelling beam instead.
     */
    public boolean strokesClosed() {
        switch (this) {
            case CIRCLE:
            case SQUARE:
            case STAR:
            case TRIANGLE:
            case TUNNEL:
            case DOUBLE_CIRCLE:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param sizeRaw DMX 0-255 for the Size channel
     * @param amplitudeRaw DMX 0-255 for the Amplitude channel
     * @param speedRaw DMX 0-255 for the Speed channel
     * @param rotationRaw DMX 0-255 for the Rotation channel
     * @param animTimeSec wall time in seconds for animations (uses Speed)
     * @param c1 primary color RGB
     * @param c2 secondary color RGB
     * @param c3 tertiary color RGB
     */
    public List<LaserBeam> generate(int sizeRaw, int amplitudeRaw, int speedRaw,
                                    int rotationRaw, double animTimeSec,
                                    int c1, int c2, int c3) {
        float size01 = clamp01(sizeRaw / 255f);
        float amp01 = clamp01(amplitudeRaw / 255f);
        float speed01 = clamp01(speedRaw / 255f);
        float rot01 = clamp01(rotationRaw / 255f);

        // Rotation comes ONLY from the Rotation DMX channel — Speed never
        // rotates patterns. Speed DMX feeds shape animations that aren't
        // rotation: wave phase sliding, scatter jitter rate, etc.
        float staticRotDeg = rot01 * 360f;
        // 5% deadzone so DMX 0-12 are static. Above that, scales linearly.
        final float speedDeadzone = 0.05f;
        float effectiveSpeed = (speed01 < speedDeadzone) ? 0f
                : (speed01 - speedDeadzone) / (1f - speedDeadzone);
        // Wave phase advances at up to 10 cycles per second at max speed.
        float animPhase = (float) (animTimeSec * effectiveSpeed * 10f);

        switch (this) {
            case BEAM_SIMPLE:    return beamSimple(size01, c1);
            case LINE:           return line(size01, amp01, staticRotDeg, c1, c2, c3);
            case CIRCLE:         return circle(size01, amp01, staticRotDeg, c1, c2, c3);
            case SQUARE:         return square(size01, amp01, staticRotDeg, c1, c2, c3);
            case WAVE:           return wave(size01, amp01, staticRotDeg, animPhase, c1, c2, c3);
            case TUNNEL:         return tunnel(size01, amp01, staticRotDeg, c1, c2, c3);
            case STAR:           return star(size01, amp01, staticRotDeg, c1, c2, c3);
            case CROSS:          return cross(size01, amp01, staticRotDeg, c1, c2, c3);
            case TRIANGLE:       return triangle(size01, staticRotDeg, c1, c2, c3);
            case SPIRAL:         return spiral(size01, amp01, staticRotDeg, c1, c2, c3);
            case PARALLEL_LINES: return parallelLines(size01, amp01, staticRotDeg, c1, c2, c3);
            case DOUBLE_CIRCLE:  return doubleCircle(size01, amp01, staticRotDeg, c1, c2, c3);
            case BURST:          return burst(size01, amp01, effectiveSpeed, animTimeSec, c1, c2, c3);
            case SCATTER:        return scatter(size01, amp01, speed01, animTimeSec, c1, c2, c3);
        }
        return List.of();
    }

    // ----- Generators -----

    private static List<LaserBeam> beamSimple(float size01, int c1) {
        float length = lerp(size01, 8f, 64f);
        return List.of(new LaserBeam(0f, 0f, length, c1));
    }

    /**
     * Single horizontal line of beams in a row. Size = total span. Amplitude
     * adds a slight upward arch (0 = perfectly flat).
     */
    private static List<LaserBeam> line(float size01, float amp01, float rotDeg,
                                        int c1, int c2, int c3) {
        int count = 16;
        float spanDeg = lerp(size01, 8f, 70f);
        float archDeg = lerp(amp01, 0f, 14f); // amp = arch height (parabolic)
        List<LaserBeam> out = new ArrayList<>(count);
        double rotRad = Math.toRadians(rotDeg);
        float cosR = (float) Math.cos(rotRad);
        float sinR = (float) Math.sin(rotRad);
        for (int i = 0; i < count; i++) {
            float t = i / (float) (count - 1);
            float yaw = (t - 0.5f) * spanDeg;
            // parabolic arch: max at center, 0 at edges
            float u = 2f * t - 1f;
            float pitch = archDeg * (1f - u * u);
            float yr = yaw * cosR - pitch * sinR;
            float pr = yaw * sinR + pitch * cosR;
            int color = triGradient(t, c1, c2, c3);
            out.add(new LaserBeam(yr, pr, 32f, color));
        }
        return out;
    }

    private static List<LaserBeam> circle(float size01, float amp01, float rotDeg,
                                          int c1, int c2, int c3) {
        int count = 72;
        float radiusDeg = lerp(size01, 4f, 35f);
        float vertRadiusDeg = radiusDeg * lerp(amp01, 0.3f, 1.4f); // amp = ovale
        List<LaserBeam> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            float t = i / (float) count;
            float ang = (float) Math.toRadians(t * 360f + rotDeg);
            float yaw = (float) (Math.cos(ang) * radiusDeg);
            float pitch = (float) (Math.sin(ang) * vertRadiusDeg);
            int color = triGradient(t, c1, c2, c3);
            out.add(new LaserBeam(yaw, pitch, 32f, color));
        }
        return out;
    }

    private static List<LaserBeam> square(float size01, float amp01, float rotDeg,
                                          int c1, int c2, int c3) {
        int perSide = 18;
        float halfDeg = lerp(size01, 4f, 35f);
        float ratio = lerp(amp01, 1.0f, 1.8f); // amp = ratio largeur/hauteur
        float halfX = halfDeg * ratio;
        float halfY = halfDeg;
        // Build square corner points (in local pan/pitch degrees space)
        float[][] corners = {
                { halfX,  halfY},
                {-halfX,  halfY},
                {-halfX, -halfY},
                { halfX, -halfY}
        };
        List<LaserBeam> out = new ArrayList<>(perSide * 4);
        double rotRad = Math.toRadians(rotDeg);
        float cosR = (float) Math.cos(rotRad);
        float sinR = (float) Math.sin(rotRad);
        int total = perSide * 4;
        int idx = 0;
        for (int side = 0; side < 4; side++) {
            float[] a = corners[side];
            float[] b = corners[(side + 1) % 4];
            for (int i = 0; i < perSide; i++) {
                float u = i / (float) perSide;
                float yaw = a[0] + (b[0] - a[0]) * u;
                float pitch = a[1] + (b[1] - a[1]) * u;
                // rotate
                float yr = yaw * cosR - pitch * sinR;
                float pr = yaw * sinR + pitch * cosR;
                int color = triGradient(idx / (float) total, c1, c2, c3);
                out.add(new LaserBeam(yr, pr, 32f, color));
                idx++;
            }
        }
        return out;
    }

    private static List<LaserBeam> wave(float size01, float amp01, float rotDeg,
                                        float animPhase, int c1, int c2, int c3) {
        int count = 64;
        float spanDeg = lerp(size01, 12f, 70f);   // total horizontal span
        float ampDeg = lerp(amp01, 2f, 25f);       // wave height
        int waves = 3;
        List<LaserBeam> out = new ArrayList<>(count);
        double rotRad = Math.toRadians(rotDeg);
        float cosR = (float) Math.cos(rotRad);
        float sinR = (float) Math.sin(rotRad);
        // animPhase grows by 1 per second at max speed → 1 wavelength shift/sec
        double phaseShift = animPhase * Math.PI * 2.0;
        for (int i = 0; i < count; i++) {
            float t = i / (float) (count - 1);
            float yaw = (t - 0.5f) * spanDeg;
            float pitch = (float) Math.sin(t * waves * Math.PI * 2.0 + phaseShift) * ampDeg;
            float yr = yaw * cosR - pitch * sinR;
            float pr = yaw * sinR + pitch * cosR;
            int color = triGradient(t, c1, c2, c3);
            out.add(new LaserBeam(yr, pr, 32f, color));
        }
        return out;
    }

    private static List<LaserBeam> tunnel(float size01, float amp01, float rotDeg,
                                          int c1, int c2, int c3) {
        int rings = 4;
        int perRing = 28;
        float baseRadiusDeg = lerp(size01, 3f, 20f);
        float ringSpacingDeg = lerp(amp01, 1f, 8f); // amp = depth
        List<LaserBeam> out = new ArrayList<>(rings * perRing);
        int total = rings * perRing;
        int idx = 0;
        for (int r = 0; r < rings; r++) {
            float ringRadius = baseRadiusDeg + r * ringSpacingDeg;
            float length = lerp(r / (float) (rings - 1), 32f, 48f);
            for (int i = 0; i < perRing; i++) {
                float t = i / (float) perRing;
                float ang = (float) Math.toRadians(t * 360f + rotDeg + r * 8f);
                float yaw = (float) (Math.cos(ang) * ringRadius);
                float pitch = (float) (Math.sin(ang) * ringRadius);
                int color = triGradient(idx / (float) total, c1, c2, c3);
                out.add(new LaserBeam(yaw, pitch, length, color, r));
                idx++;
            }
        }
        return out;
    }

    private static List<LaserBeam> star(float size01, float amp01, float rotDeg,
                                        int c1, int c2, int c3) {
        int points = 5;
        int verticesTotal = points * 2;
        int perEdge = 12;
        float outerRadius = lerp(size01, 6f, 35f);
        float innerRadius = outerRadius * lerp(amp01, 0.55f, 0.20f); // smaller inner = sharper points
        float[][] verts = new float[verticesTotal][2];
        for (int v = 0; v < verticesTotal; v++) {
            float r = (v % 2 == 0) ? outerRadius : innerRadius;
            float ang = (float) Math.toRadians((v * 360f / verticesTotal) - 90f + rotDeg);
            verts[v][0] = (float) (Math.cos(ang) * r);
            verts[v][1] = (float) (Math.sin(ang) * r);
        }
        List<LaserBeam> out = new ArrayList<>(verticesTotal * perEdge);
        int total = verticesTotal * perEdge;
        int idx = 0;
        for (int e = 0; e < verticesTotal; e++) {
            float[] a = verts[e];
            float[] b = verts[(e + 1) % verticesTotal];
            for (int i = 0; i < perEdge; i++) {
                float u = i / (float) perEdge;
                float yaw = a[0] + (b[0] - a[0]) * u;
                float pitch = a[1] + (b[1] - a[1]) * u;
                int color = triGradient(idx / (float) total, c1, c2, c3);
                out.add(new LaserBeam(yaw, pitch, 32f, color));
                idx++;
            }
        }
        return out;
    }

    private static List<LaserBeam> cross(float size01, float amp01, float rotDeg,
                                         int c1, int c2, int c3) {
        int perBranch = 18;
        float length = lerp(size01, 6f, 35f);
        float thickness = lerp(amp01, 0.5f, 4f);
        // 4 branches: +X, -X, +Y, -Y, each is a small rectangle drawn as a line of beams
        List<LaserBeam> out = new ArrayList<>(perBranch * 4);
        double rotRad = Math.toRadians(rotDeg);
        float cosR = (float) Math.cos(rotRad);
        float sinR = (float) Math.sin(rotRad);
        int total = perBranch * 4;
        int idx = 0;
        float[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (float[] dir : dirs) {
            for (int i = 0; i < perBranch; i++) {
                float t = (i + 1) / (float) perBranch;
                float yaw = dir[0] * length * t + dir[1] * (((idx % 2 == 0) ? 1 : -1) * thickness * 0.3f);
                float pitch = dir[1] * length * t + dir[0] * (((idx % 2 == 0) ? 1 : -1) * thickness * 0.3f);
                float yr = yaw * cosR - pitch * sinR;
                float pr = yaw * sinR + pitch * cosR;
                int color = triGradient(idx / (float) total, c1, c2, c3);
                out.add(new LaserBeam(yr, pr, 32f, color, idx / perBranch));
                idx++;
            }
        }
        return out;
    }

    private static List<LaserBeam> triangle(float size01, float rotDeg,
                                            int c1, int c2, int c3) {
        int perEdge = 24;
        float radius = lerp(size01, 6f, 35f);
        float[][] verts = new float[3][2];
        for (int v = 0; v < 3; v++) {
            float ang = (float) Math.toRadians((v * 120f) - 90f + rotDeg);
            verts[v][0] = (float) (Math.cos(ang) * radius);
            verts[v][1] = (float) (Math.sin(ang) * radius);
        }
        List<LaserBeam> out = new ArrayList<>(perEdge * 3);
        int total = perEdge * 3;
        int idx = 0;
        for (int e = 0; e < 3; e++) {
            float[] a = verts[e];
            float[] b = verts[(e + 1) % 3];
            for (int i = 0; i < perEdge; i++) {
                float u = i / (float) perEdge;
                float yaw = a[0] + (b[0] - a[0]) * u;
                float pitch = a[1] + (b[1] - a[1]) * u;
                int color = triGradient(idx / (float) total, c1, c2, c3);
                out.add(new LaserBeam(yaw, pitch, 32f, color));
                idx++;
            }
        }
        return out;
    }

    private static List<LaserBeam> spiral(float size01, float amp01, float rotDeg,
                                          int c1, int c2, int c3) {
        int count = 90;
        float maxRadius = lerp(size01, 8f, 35f);
        float pitch01 = lerp(amp01, 0.4f, 2.0f); // tighter or looser turns
        float turns = 2.5f * pitch01;
        List<LaserBeam> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            float t = i / (float) (count - 1);
            float r = t * maxRadius;
            float ang = (float) Math.toRadians(t * 360f * turns + rotDeg);
            float yaw = (float) (Math.cos(ang) * r);
            float pitchDeg = (float) (Math.sin(ang) * r);
            int color = triGradient(t, c1, c2, c3);
            out.add(new LaserBeam(yaw, pitchDeg, 32f, color));
        }
        return out;
    }

    private static List<LaserBeam> parallelLines(float size01, float amp01, float rotDeg,
                                                 int c1, int c2, int c3) {
        int lineCount = 4 + (int) (amp01 * 8); // amp = number of lines (4..12)
        int perLine = 24;
        float spacingDeg = lerp(size01, 2f, 8f);
        float lineLengthDeg = 30f;
        List<LaserBeam> out = new ArrayList<>(lineCount * perLine);
        double rotRad = Math.toRadians(rotDeg);
        float cosR = (float) Math.cos(rotRad);
        float sinR = (float) Math.sin(rotRad);
        int total = lineCount * perLine;
        int idx = 0;
        float startY = -((lineCount - 1) * spacingDeg) * 0.5f;
        for (int l = 0; l < lineCount; l++) {
            float lineY = startY + l * spacingDeg;
            for (int i = 0; i < perLine; i++) {
                float u = i / (float) (perLine - 1);
                float yaw = (u - 0.5f) * lineLengthDeg;
                float pitch = lineY;
                float yr = yaw * cosR - pitch * sinR;
                float pr = yaw * sinR + pitch * cosR;
                int color = triGradient(idx / (float) total, c1, c2, c3);
                out.add(new LaserBeam(yr, pr, 32f, color, l));
                idx++;
            }
        }
        return out;
    }

    private static List<LaserBeam> doubleCircle(float size01, float amp01, float rotDeg,
                                                int c1, int c2, int c3) {
        int countOuter = 48;
        int countInner = 48;
        float outer = lerp(size01, 6f, 35f);
        float inner = outer * lerp(amp01, 0.85f, 0.40f); // amp = écart entre cercles
        List<LaserBeam> out = new ArrayList<>(countOuter + countInner);
        int total = countOuter + countInner;
        int idx = 0;
        for (int i = 0; i < countOuter; i++) {
            float ang = (float) Math.toRadians((i / (float) countOuter) * 360f + rotDeg);
            float yaw = (float) (Math.cos(ang) * outer);
            float pitch = (float) (Math.sin(ang) * outer);
            int color = triGradient(idx / (float) total, c1, c2, c3);
            out.add(new LaserBeam(yaw, pitch, 32f, color, 0));
            idx++;
        }
        for (int i = 0; i < countInner; i++) {
            float ang = (float) Math.toRadians((i / (float) countInner) * 360f - rotDeg);
            float yaw = (float) (Math.cos(ang) * inner);
            float pitch = (float) (Math.sin(ang) * inner);
            int color = triGradient(idx / (float) total, c1, c2, c3);
            out.add(new LaserBeam(yaw, pitch, 32f, color, 1));
            idx++;
        }
        return out;
    }

    private static List<LaserBeam> burst(float size01, float amp01, float effectiveSpeed,
                                         double animTimeSec, int c1, int c2, int c3) {
        int count = 8 + (int) (amp01 * 24); // amp = density (8..32)
        float maxRadius = lerp(size01, 8f, 40f);
        // Re-seed rate scales with Speed DMX: 0 at deadzone (stable burst,
        // never changes), up to ~12.5 reseeds/sec at max for a strobe-like
        // flicker.
        long seed = (long) (animTimeSec * effectiveSpeed * 12.5);
        Random rng = new Random(seed);
        List<LaserBeam> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            float ang = (float) (rng.nextFloat() * Math.PI * 2.0);
            float r = (0.3f + rng.nextFloat() * 0.7f) * maxRadius;
            float yaw = (float) (Math.cos(ang) * r);
            float pitch = (float) (Math.sin(ang) * r);
            int color = triGradient(rng.nextFloat(), c1, c2, c3);
            out.add(new LaserBeam(yaw, pitch, 32f, color, i));
        }
        return out;
    }

    private static List<LaserBeam> scatter(float size01, float amp01, float speed01,
                                           double animTimeSec, int c1, int c2, int c3) {
        int count = 5 + (int) (size01 * 25); // 5..30 beams
        float coneHalfDeg = lerp(amp01, 5f, 90f);
        // Seed evolves with speed for jitter; speed=0 gives a stable seed.
        // Max multiplier ~20.5 → very fast scatter at full DMX speed.
        long seed = (speed01 < 0.01f) ? 0L : (long) (animTimeSec * (0.5 + speed01 * 20.0));
        Random rng = new Random(seed * 1000003L + 17L);
        List<LaserBeam> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            float ang = (float) (rng.nextFloat() * Math.PI * 2.0);
            // sqrt for uniform area distribution
            float r = (float) Math.sqrt(rng.nextFloat()) * coneHalfDeg;
            float yaw = (float) (Math.cos(ang) * r);
            float pitch = (float) (Math.sin(ang) * r);
            int color = triGradient(i / (float) count, c1, c2, c3);
            out.add(new LaserBeam(yaw, pitch, 48f, color, i));
        }
        return out;
    }

    // ----- Helpers -----

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static float lerp(float t, float a, float b) {
        return a + (b - a) * t;
    }

    /**
     * Tri-color gradient C1 → C2 → C3 sampled at t in [0, 1].
     */
    public static int triGradient(float t, int c1, int c2, int c3) {
        t = clamp01(t);
        if (t < 0.5f) {
            float u = t * 2f;
            return mixRgb(c1, c2, u);
        } else {
            float u = (t - 0.5f) * 2f;
            return mixRgb(c2, c3, u);
        }
    }

    private static int mixRgb(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int rr = (int) (ar + (br - ar) * t);
        int gg = (int) (ag + (bg - ag) * t);
        int bbb = (int) (ab + (bb - ab) * t);
        return (rr << 16) | (gg << 8) | bbb;
    }
}
