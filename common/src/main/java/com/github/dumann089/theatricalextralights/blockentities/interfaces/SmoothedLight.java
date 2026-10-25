package com.github.dumann089.theatricalextralights.blockentities.interfaces;

/**
 * Implementada em tempo de execução (via Mixin — ver {@code BaseLightBlockEntityMixin}) por
 * {@code dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity}. Ela NÃO existe no
 * Theatrical oficial em tempo de compilação, então o Extra Lights nunca chama esses métodos
 * direto num tipo do Theatrical: use sempre {@code instanceof SmoothedLight}.
 *
 * <p>{@code getSmoothed*} = valor suavizado "puro" (sem shutter/strobe). O
 * {@code ExtraLightsLightBlockEntity} usa esses dois pra montar {@code getDisplayIntensity}
 * (suavizado × fator de shutter/strobe da fixture) e {@code getDisplayColour}.
 *
 * <p>{@code getDisplay*} aqui na interface são a versão "genérica" (igual ao suavizado puro) pra
 * fixtures do Theatrical base; nas fixtures do Extra Lights elas são sobrescritas pelo
 * {@code ExtraLightsLightBlockEntity}, que aplica o strobe por cima.
 */
public interface SmoothedLight {
    float getSmoothedIntensity(float partialTicks);
    int getSmoothedColour(float partialTicks);

    float getDisplayIntensity(float partialTicks);
    int getDisplayColour(float partialTicks);
}
