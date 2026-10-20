package com.github.dumann089.theatricalextralights.blockentities.interfaces;

/**
 * Machine a flammes dont l'allure se regle dans son menu : {@link #STYLE_JET} jet long et
 * tendu qui roule en bout, {@link #STYLE_BURST} colonne courte, large et explosive.
 */
public interface HasFlameStyle {
    int STYLE_JET = 0;
    int STYLE_BURST = 1;
    int STYLE_COUNT = 2;

    int getFlameStyle();

    void setFlameStyle(int style);
}
