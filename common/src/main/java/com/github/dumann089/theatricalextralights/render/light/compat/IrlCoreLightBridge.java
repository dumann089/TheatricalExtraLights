package com.github.dumann089.theatricalextralights.render.light.compat;

import com.github.dumann089.theatricalextralights.render.light.api.ILightEngine;
import com.github.dumann089.theatricalextralights.render.light.api.LightState;

import java.lang.reflect.Method;

/**
 * Puente mediante Reflexión. No requiere tener irl-core para compilar.
 */
public class IrlCoreLightBridge implements ILightEngine {

    private boolean active = false;
    private Method registerSpotMethod;

    @Override
    public void init() {
        try {
            // Buscamos la clase de irl-core en la memoria del juego
            Class<?> registryClass = Class.forName("org.qualet.irl.light.LightRegistry");

            // Buscamos el método exacto con todos sus parámetros primitivos
            registerSpotMethod = registryClass.getMethod("registerSpot",
                    float.class, float.class, float.class, // px, py, pz
                    float.class, float.class, float.class, // dx, dy, dz
                    float.class, float.class, float.class, // r, g, b
                    float.class, float.class,              // intensity, radius
                    float.class, float.class,              // cosOuter, cosInner
                    boolean.class, boolean.class,          // entitiesOnly, blocksOnly
                    float.class, float.class, float.class, // anisotropy, density, beamStrength
                    float.class, boolean.class,            // bulbSize, castsShadows
                    float.class, float.class, float.class, float.class, // cookieLayer, cookieRot, cookieScale, cookieFlags
                    long.class                             // identity
            );

            this.active = true;
            System.out.println("[Theatrical Extra Lights] irl-core detectado. Iluminación física activada.");
        } catch (Exception e) {
            this.active = false;
            System.out.println("[Theatrical Extra Lights] irl-core no está instalado. Usando renderizado clásico.");
        }
    }

    @Override
    public void submitLight(LightState state) {
        if (!active || registerSpotMethod == null) return;

        try {
            if (state.type == 1) { // Spot
                registerSpotMethod.invoke(null,
                        state.px, state.py, state.pz,
                        state.dx, state.dy, state.dz,
                        state.r, state.g, state.b,
                        state.intensity, state.radius,
                        state.cosOuter, state.cosInner,
                        state.entitiesOnly, state.blocksOnly,
                        state.anisotropy, state.density, state.beamStrength,
                        state.bulbSize, state.castsShadows,
                        state.cookieLayer, state.cookieRot, state.cookieScale, state.cookieFlags,
                        state.identity
                );
            }
        } catch (Exception e) {
            // Silenciar para no spamear la consola si algo falla en un frame
        }
    }

    @Override public void clear() {} // Delegado a irl-core
    @Override public void flush() {} // Delegado a irl-core

    @Override
    public boolean isActive() {
        return active;
    }
}