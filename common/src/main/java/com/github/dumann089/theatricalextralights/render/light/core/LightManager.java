package com.github.dumann089.theatricalextralights.render.light.core;

import com.github.dumann089.theatricalextralights.render.light.api.ILightEngine;
import com.github.dumann089.theatricalextralights.render.light.api.LightState;
import com.github.dumann089.theatricalextralights.render.light.compat.IrlCoreLightBridge;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.WeakHashMap;
import java.util.Map;

public class LightManager {

    private static final LightManager INSTANCE = new LightManager();
    private final ILightEngine engine;

    // WeakHashMap vincula el estado de vida del BlockEntity al LightState.
    // Zero-allocation por frame, liberación automática por el Garbage Collector.
    private final Map<BlockEntity, LightState> activeLights = new WeakHashMap<>();

    private LightManager() {
        this.engine = new IrlCoreLightBridge();
        this.engine.init();
    }

    public static LightManager getInstance() {
        return INSTANCE;
    }

    /**
     * Obtiene el estado de la luz, o lo crea SOLO la primera vez que se renderiza.
     */
    public LightState getStateFor(BlockEntity entity) {
        return activeLights.computeIfAbsent(entity, k -> {
            LightState state = new LightState();
            state.identity = entity.getBlockPos().asLong(); // Identidad única y rápida
            return state;
        });
    }

    public void processFrame() {
        if (!engine.isActive()) return;

        // Usamos un iterador para poder eliminar luces de forma segura en pleno ciclo
        var iterator = activeLights.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            net.minecraft.world.level.block.entity.BlockEntity be = entry.getKey();
            LightState state = entry.getValue();

            // FIX: Si el bloque fue roto o el chunk se descargó, lo eliminamos al instante
            if (be == null || be.isRemoved()) {
                iterator.remove();
                continue;
            }

            // Solo enviamos si tiene brillo
            if (state.intensity > 0.001F) {
                engine.submitLight(state);
            }
        }
    }
}