package com.github.dumann089.theatricalextralights.laser.dac;

/**
 * Latest reconstructed scan picture published by a DAC playback clock.
 * Protocol-agnostic so IDN (or others) can publish the same snapshot later.
 */
public final class LaserFrame {

    public static final LaserFrame EMPTY = new LaserFrame(new LaserSegment[0], 0, 0L, 0);

    public final LaserSegment[] segments;
    public final int pointRate;
    public final long pointCount;
    public final long publishedAtNanos;

    public LaserFrame(LaserSegment[] segments, int pointRate, long pointCount, long publishedAtNanos) {
        this.segments = segments;
        this.pointRate = pointRate;
        this.pointCount = pointCount;
        this.publishedAtNanos = publishedAtNanos;
    }

    public boolean isEmpty() {
        return segments == null || segments.length == 0;
    }

    public boolean isFresh(long nowNanos, long maxAgeNanos) {
        return !isEmpty() && nowNanos - publishedAtNanos <= maxAgeNanos;
    }
}
