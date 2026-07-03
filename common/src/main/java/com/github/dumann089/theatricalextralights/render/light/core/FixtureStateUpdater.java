package com.github.dumann089.theatricalextralights.render.light.core;

import com.github.dumann089.theatricalextralights.blockentities.MovingVL2CBeamsBlockEntity;
import com.github.dumann089.theatricalextralights.client.gobo.GoboLibrary;
import com.github.dumann089.theatricalextralights.client.gobo.GlobalGoboRegistry;
import com.github.dumann089.theatricalextralights.render.light.api.LightState;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import net.minecraft.core.BlockPos;

public class FixtureStateUpdater {

    public static void updateVL2C(MovingVL2CBeamsBlockEntity be, LightState state, Matrix4f headMatrix, float partialTicks) {
        BlockPos pos = be.getBlockPos();

        // 1. Vectores de dirección (Normalizados por JOML)
        state.dx = -headMatrix.m20();
        state.dy = -headMatrix.m21();
        state.dz = -headMatrix.m22();

        // 2. Posición con offset (45cm hacia adelante para evitar sombras propias)
        float offset = 0.45f;
        state.px = pos.getX() + headMatrix.m30() + (state.dx * offset);
        state.py = pos.getY() + headMatrix.m31() + (state.dy * offset);
        state.pz = pos.getZ() + headMatrix.m32() + (state.dz * offset);

        // 3. Intensidad, Color y Alcance
        float rawIntensity = be.getPartialIntensity(partialTicks) / 255.0F;
        state.intensity = rawIntensity * 30.0F; // Multiplicador general

        state.r = be.getRed() / 255.0F;
        state.g = be.getGreen() / 255.0F;
        state.b = be.getBlue() / 255.0F;

        state.radius = 128.0F; // Forzamos un alcance largo
        state.type = 1; // 1 = Spot

        // 4. Óptica (Zoom / Cone Angle)
        float zoomNorm = be.getPartialZoom(partialTicks) / 255.0f;
        float minAngle = 10.0f;
        float maxAngle = 50.0f;
        float coneHalfAngle = minAngle + zoomNorm * (maxAngle - minAngle);
        state.cosOuter = (float) Math.cos(Math.toRadians(coneHalfAngle));
        state.cosInner = (float) Math.cos(Math.toRadians(coneHalfAngle * 0.9f));

        // ========================================================
        // 5. SISTEMA DE GOBOS (INTEGRADO CON IRLIGHTS)
        // ========================================================
        // 5. SISTEMA DE GOBOS
        // ... (dentro de FixtureStateUpdater)
        // 5. SISTEMA DE GOBOS (INTEGRADO CON IRLIGHTS)
        int currentSlot = be.getGobo();

        if (currentSlot <= 0) {
            state.cookieLayer = -1.0F;
            state.type = 1; // Aseguramos que sea spotlight aunque esté abierto
        } else {
            GoboLibrary lib = be.getGoboLibrary();
            ResourceLocation currentGoboTex = lib.getTexture(currentSlot);
            int irlightsCookieID = GlobalGoboRegistry.getCookieLayer(currentGoboTex);

// LOG DE DIAGNÓSTICO
            if (be.getLevel().isClientSide() && be.getLevel().getGameTime() % 20 == 0) {
                System.out.println("[TEL-DEBUG] Slot: " + currentSlot + " | Tex: " + currentGoboTex + " | Layer a enviar: " + irlightsCookieID);
            }
            // DIAGNÓSTICO: Si el ID es inválido, no enviamos basura, enviamos -1 para que la luz sobreviva
            if (irlightsCookieID >= 0) {
                state.cookieLayer = (float) irlightsCookieID;
                state.type = 1; // CRÍTICO: El spotlight DEBE ser tipo 1 para aceptar texturas
            } else {
                state.cookieLayer = -1.0F; // Fallback seguro
                state.type = 1;
            }
        }

        // Rotación continua/indexada: Convertimos los grados del bloque a radianes para la GPU
        state.cookieRot = (float) Math.toRadians(be.getGoboRotation());
        state.cookieScale = 1.0F;
        state.cookieFlags = 0.0F;

        // 6. Atmósfera Volumétrica
        state.density = 0.6F;
        state.beamStrength = 5.0F;
        state.anisotropy = 0.4F;
        state.castsShadows = true;
    }
}