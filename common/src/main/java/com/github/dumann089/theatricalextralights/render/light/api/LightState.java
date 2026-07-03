package com.github.dumann089.theatricalextralights.render.light.api;

/**
 * Representa el estado físico y óptico de una luminaria en un frame específico.
 * DISEÑO ZERO-ALLOCATION: Se instancia una sola vez por Fixture.
 * Los vectores de posición y dirección se manejan como primitivos independientes (float)
 * para evitar dependencias inestables de librerías matemáticas y recolección de basura.
 */
public class LightState {

    // Identidad única (sugerencia: usar la posición del bloque y un offset)
    public long identity;

    // Tipo de luminaria: 0 = Point (Omnidireccional), 1 = Spot (Direccional/Beam)
    public int type = 1;

    // Posición absoluta en el mundo
    public float px, py, pz;

    // Dirección del haz (Normalizado). Fundamental para Moving Heads y Scanners.
    public float dx, dy, dz;

    // Color lineal (RGB) y potencia de la lámpara
    public float r, g, b, intensity;

    // Alcance máximo del haz de luz
    public float radius;

    // Óptica (Apertura del cono para Spots).
    // Zoom e Iris modifican estos valores.
    public float cosOuter;
    public float cosInner;

    // Propiedades volumétricas y densidad en la atmósfera
    public float anisotropy = 0.8F;
    public float density = 1.0F;
    public float beamStrength = 1.0F;

    // Flags físicos y comportamiento de sombras
    public float bulbSize = 0.0F;
    public boolean castsShadows = true;
    public boolean entitiesOnly = false;
    public boolean blocksOnly = false;

    // Soporte futuro para Ruedas de Gobos y Proyectores
    public float cookieLayer = -1F; // -1 = Sin gobo (Luz pura)
    public float cookieRot = 0F;
    public float cookieScale = 1F;
    public float cookieFlags = 0F;
}