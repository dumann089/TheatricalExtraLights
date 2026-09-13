package com.github.dumann089.theatricalextralights.blockentities.interfaces;

/**
 * Effet pyrotechnique avec cle d'armement : tant que la machine n'est pas armee, le DMX
 * est lu mais la sortie reste a zero, comme sur une vraie machine a flammes.
 */
public interface HasSafetyArm {

    boolean isArmed();

    /** Serveur : persiste et synchronise ; client : mise a jour locale immediate. */
    void setArmed(boolean armed);
}
