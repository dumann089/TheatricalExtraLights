package com.github.dumann089.theatricalextralights.blockentities.interfaces;

import com.github.dumann089.theatricalextralights.util.FramingShutterState;

/**
 * Projecteur equipe d'un module de couteaux (framing shutters) pilote par DMX
 * lorsque la personnalite etendue est active.
 */
public interface HasFramingShutters {

    FramingShutterState getFramingShutters();

    /** True si la personnalite courante expose les canaux couteaux et qu'une lame est engagee. */
    default boolean hasActiveFramingShutters() {
        FramingShutterState state = getFramingShutters();
        return state != null && state.isActive();
    }
}
