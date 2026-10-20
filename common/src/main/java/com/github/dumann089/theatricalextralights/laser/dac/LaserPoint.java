package com.github.dumann089.theatricalextralights.laser.dac;

/**
 * One Ether Dream {@code dac_point}: ILDA-style XY, 16-bit RGB + intensity.
 * Blanking is RGB (and optional intensity) below the configured threshold.
 */
public final class LaserPoint {

    public final int control;
    public final short x;
    public final short y;
    public final int r;
    public final int g;
    public final int b;
    public final int i;

    public LaserPoint(int control, short x, short y, int r, int g, int b, int i) {
        this.control = control;
        this.x = x;
        this.y = y;
        this.r = r;
        this.g = g;
        this.b = b;
        this.i = i;
    }

    public boolean changeRate() {
        return (control & LaserProtocol.CTRL_CHANGE_RATE) != 0;
    }

    public boolean isBlank(int threshold) {
        int peak = Math.max(r, Math.max(g, b));
        if (peak < threshold) {
            return true;
        }
        return i > 0 && i < threshold;
    }

    public int rgb() {
        int red = r >> 8;
        int green = g >> 8;
        int blue = b >> 8;
        if (i > 0 && i < 65535) {
            float scale = i / 65535f;
            red = Math.min(255, Math.round(red * scale));
            green = Math.min(255, Math.round(green * scale));
            blue = Math.min(255, Math.round(blue * scale));
        }
        return (red << 16) | (green << 8) | blue;
    }

    public float intensity01() {
        int peak = Math.max(r, Math.max(g, b));
        float rgb = peak / 65535f;
        if (i > 0) {
            return Math.min(1f, rgb * (i / 65535f));
        }
        return Math.min(1f, rgb);
    }
}
