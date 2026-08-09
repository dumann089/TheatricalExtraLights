package com.github.dumann089.theatricalextralights.blockentities.interfaces;

/**
 * Fixtures with DMX channels 8+ (gobo slot, prism strength, zoom, spin).
 * Used for client-side Sodium invalidation when intensity alone is not enough.
 */
public interface HasExtendedBeamChannels {

    /** Gobo slot index, or prism beam strength (0 = off / open beam). */
    int getGobo();

    /** Spin speed 0–255 (gobo wheel or prism rotation). */
    int getGoboSpin();
}
