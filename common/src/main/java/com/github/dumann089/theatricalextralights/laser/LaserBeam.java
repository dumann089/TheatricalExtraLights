package com.github.dumann089.theatricalextralights.laser;

/**
 * One beam emitted by a laser fixture, expressed in fixture-local space (the
 * local Z+ axis points where pan/tilt aim the fixture). Yaw rotates around Y,
 * pitch around X. Length is in blocks (capped by ray trace later if needed).
 *
 * <p>{@code stroke} groups consecutive points that the scanner draws in one
 * continuous trace: the renderer sweeps a sheet between neighbours of the same
 * stroke and treats a point that has no neighbour as a standalone beam.
 */
public final class LaserBeam {
    public final float yawDeg;
    public final float pitchDeg;
    public final float length;
    public final int color;
    public final int stroke;

    public LaserBeam(float yawDeg, float pitchDeg, float length, int color) {
        this(yawDeg, pitchDeg, length, color, 0);
    }

    public LaserBeam(float yawDeg, float pitchDeg, float length, int color, int stroke) {
        this.yawDeg = yawDeg;
        this.pitchDeg = pitchDeg;
        this.length = length;
        this.color = color;
        this.stroke = stroke;
    }
}
