package com.github.dumann089.theatricalextralights.laser.dac;

/**
 * Visible scan segment: two consecutive unblanked points, rendered as a
 * volumetric ray from the projector through the midpoint (plus a sheet).
 *
 * <p>{@link #age} is the position of the segment inside the persistence
 * window that produced the frame: {@code 0} = just drawn by the scanner,
 * {@code 1} = oldest point still inside the window. Renderers use it to
 * reproduce the phosphor-like decay of a real scan trail instead of popping.
 */
public final class LaserSegment {

    public final short x0;
    public final short y0;
    public final short x1;
    public final short y1;
    public final int color;
    public final float intensity;
    public final float age;

    public LaserSegment(short x0, short y0, short x1, short y1, int color, float intensity) {
        this(x0, y0, x1, y1, color, intensity, 0.0f);
    }

    public LaserSegment(short x0, short y0, short x1, short y1, int color, float intensity, float age) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.color = color;
        this.intensity = intensity;
        this.age = age < 0.0f ? 0.0f : (age > 1.0f ? 1.0f : age);
    }

    public boolean isPoint() {
        return x0 == x1 && y0 == y1;
    }

    /** Perceived brightness after persistence decay (newest = 1, oldest ≈ 0.3). */
    public float persistence() {
        float fresh = 1.0f - age;
        return 0.30f + 0.70f * fresh * fresh;
    }
}
