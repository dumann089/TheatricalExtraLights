package com.github.dumann089.theatricalextralights.client.render.beam;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record BeamRenderData(
        BlockPos fixturePos,
        Vec3 origin,
        Vec3 beamDir,
        Vec3 axisU,
        Vec3 axisV,
        float zoomNorm,
        float scanLen,
        float tanHalfAngle,
        int color,
        float intensity,
        ResourceLocation tex0,         // <- Textura del Gobo 1
        ResourceLocation tex1,         // <- Textura del Gobo 2
        float wheelProgress,           // <- Progreso de la mezcla
        float goboRotation,
        Level level,
        float widthScale,
        float heightScale,
        float baseRadius
) {
    /** Constructor de compatibilidad actualizado (Opcional, pero útil si lo llamás desde otro lado sin el baseRadius) */
    public BeamRenderData(
            BlockPos fixturePos,
            Vec3 origin,
            Vec3 beamDir,
            Vec3 axisU,
            Vec3 axisV,
            float zoomNorm,
            float scanLen,
            float tanHalfAngle,
            int color,
            float intensity,
            ResourceLocation tex0,
            ResourceLocation tex1,
            float wheelProgress,
            float goboRotation,
            Level level,
            float widthScale,
            float heightScale
    ) {
        this(fixturePos, origin, beamDir, axisU, axisV,
                zoomNorm, scanLen, tanHalfAngle,
                color, intensity, tex0, tex1, wheelProgress, goboRotation, level,
                widthScale, heightScale, 0.05f);
    }

    public int generateStateHash(int slices) {
        int hash = 17;
        hash = 31 * hash + fixturePos.hashCode();
        hash = 31 * hash + Float.floatToIntBits((float) origin.x);
        hash = 31 * hash + Float.floatToIntBits((float) origin.y);
        hash = 31 * hash + Float.floatToIntBits((float) origin.z);
        hash = 31 * hash + Float.floatToIntBits((float) beamDir.x);
        hash = 31 * hash + Float.floatToIntBits((float) beamDir.y);
        hash = 31 * hash + Float.floatToIntBits((float) beamDir.z);
        hash = 31 * hash + Float.floatToIntBits(zoomNorm);
        hash = 31 * hash + Float.floatToIntBits(scanLen);
        hash = 31 * hash + Float.floatToIntBits(tanHalfAngle);
        hash = 31 * hash + color;
        hash = 31 * hash + Float.floatToIntBits(intensity);
        hash = 31 * hash + (tex0 != null ? tex0.hashCode() : 0); // Añadido tex0 al hash
        hash = 31 * hash + (tex1 != null ? tex1.hashCode() : 0); // Añadido tex1 al hash
        hash = 31 * hash + Float.floatToIntBits(wheelProgress);  // Añadido progress al hash
        hash = 31 * hash + Float.floatToIntBits(goboRotation);
        hash = 31 * hash + slices;
        hash = 31 * hash + Float.floatToIntBits(widthScale);
        hash = 31 * hash + Float.floatToIntBits(heightScale);
        hash = 31 * hash + Float.floatToIntBits(baseRadius);
        return hash;
    }
}