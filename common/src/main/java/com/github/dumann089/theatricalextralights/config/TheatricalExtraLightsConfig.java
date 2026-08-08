package com.github.dumann089.theatricalextralights.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TheatricalExtraLightsConfig {

    /* ================= CAMPOS DE CONFIGURACIÓN ================= */
    // Ahora tienen anotaciones para generar la UI automáticamente.

    @ConfigOption(name = "Laser Beam Length", min = 20.0, max = 1000.0)
    public Float laserBeamLength = 100.0f;

    @ConfigOption(name = "RGB Bar Beam Length", min = 1.0, max = 50.0)
    public Float rgbBarBeamLength = 9.0f;

    @ConfigOption(name = "Render Lens", tooltip = "Enable/disable lens rendering")
    public Boolean renderLens = true;

    @ConfigOption(name = "Max Gobo Distance", min = 10.0, max = 1000.0)
    public Float maxGoboDistance = 90.0f;

    @ConfigOption(name = "Render 2D Beam")
    public Boolean render2DBeam = true;

    public List<String> laserPassThroughBlocks = null;

    @ConfigOption(name = "Volumetric Beam Enabled")
    public Boolean volumetricBeamEnabled = true;

    @ConfigOption(name = "Volumetric Beam Distance", min = 16.0, max = 256.0)
    public Float volumetricBeamDistance = 64.0f;

    @ConfigOption(name = "Volumetric Beam Brightness", min = 0.0, max = 1.0)
    public Float volumetricBeamBrightness = 0.15f;

    @ConfigOption(name = "Volumetric Beam Slices", min = 16.0, max = 512.0)
    public Integer volumetricBeamSlices = 128;

    @ConfigOption(name = "Volumetric Beam Density", min = 0.0, max = 1.0)
    public Float volumetricBeamDensity = 0.15f;

    @ConfigOption(name = "Volumetric Beam Max Alpha", min = 0.0, max = 1.0)
    public Float volumetricBeamMaxAlpha = 0.15f;

    @ConfigOption(name = "Volumetric Beam Fade Length", min = 1.0, max = 50.0)
    public Float volumetricBeamFadeLength = 12.0f;

    @ConfigOption(name = "Max Concurrent Rockets", min = 10.0, max = 2000.0)
    public Integer maxConcurrentRockets = 768;

    @ConfigOption(name = "Max Sparks Per Rocket", min = 10.0, max = 1500.0)
    public Integer maxSparksPerRocket = 600;

    @ConfigOption(name = "Firework Render Distance", min = 64.0, max = 8192.0)
    public Double fireworkRenderDistance = 2048.0;

    @ConfigOption(name = "Firework Dynamic Light")
    public Boolean fireworkDynamicLightEnabled = true;

    @ConfigOption(name = "Firework Smoke")
    public Boolean fireworkSmokeEnabled = true;

    @ConfigOption(name = "Smoke Budget Per Tick", min = 1.0, max = 100.0)
    public Integer fireworkSmokeBudgetPerTick = 24;

    @ConfigOption(name = "Smoke Spawn Interval", min = 1.0, max = 20.0)
    public Integer fireworkSmokeSpawnInterval = 3;

    private Integer ledFacadeMaxUniverses = 64;

    private transient Set<String> laserPassThroughSet;

    // Inicialización
    static {
        ConfigManager.load();
    }

    /* ================= GETTERS (Compatibilidad con tu código actual) ================= */
    private static TheatricalExtraLightsConfig get() { return ConfigManager.getInstance(); }

    public static boolean isVolumetricBeamEnabled() { return get().volumetricBeamEnabled; }
    public static float getVolumetricBeamDistance() { return get().volumetricBeamDistance; }
    public static float getVolumetricBeamBrightness() { return get().volumetricBeamBrightness; }
    public static int getVolumetricBeamSlices() { return get().volumetricBeamSlices; }
    public static float getVolumetricBeamDensity() { return get().volumetricBeamDensity; }
    public static float getVolumetricBeamMaxAlpha() { return get().volumetricBeamMaxAlpha; }
    public static float getVolumetricBeamFadeLength() { return get().volumetricBeamFadeLength != null ? get().volumetricBeamFadeLength : 2.0f; }
    public static float getLaserBeamLength() { return get().laserBeamLength; }
    public static float getRgbBarBeamLength() { return get().rgbBarBeamLength; }
    public static boolean shouldRenderLens() { return get().renderLens; }
    public static float getMaxGoboDistance() { return get().maxGoboDistance; }
    public static boolean shouldRender2DBeam() { return get().render2DBeam; }

    public static int getMaxConcurrentRockets() { return get().maxConcurrentRockets != null ? get().maxConcurrentRockets : 768; }
    public static int getMaxSparksPerRocket() { return get().maxSparksPerRocket != null ? get().maxSparksPerRocket : 600; }
    public static double getFireworkRenderDistance() { return get().fireworkRenderDistance != null ? get().fireworkRenderDistance : 2048.0; }
    public static boolean isFireworkDynamicLightEnabled() { return get().fireworkDynamicLightEnabled == null || get().fireworkDynamicLightEnabled; }
    public static boolean isFireworkSmokeEnabled() { return get().fireworkSmokeEnabled == null || get().fireworkSmokeEnabled; }
    public static int getFireworkSmokeBudgetPerTick() { return get().fireworkSmokeBudgetPerTick != null ? get().fireworkSmokeBudgetPerTick : 24; }
    public static int getFireworkSmokeSpawnInterval() { return Math.max(1, get().fireworkSmokeSpawnInterval != null ? get().fireworkSmokeSpawnInterval : 3); }
    public static int getLedFacadeMaxUniverses() { return INSTANCE.ledFacadeMaxUniverses != null ? INSTANCE.ledFacadeMaxUniverses : 64; }

    /* ================= SETTERS ================= */
    public static void setVolumetricBeamEnabled(boolean value) { get().volumetricBeamEnabled = value; ConfigManager.save(); }
    public static void setVolumetricBeamDistance(float value) { get().volumetricBeamDistance = value; ConfigManager.save(); }
    public static void setVolumetricBeamBrightness(float value) { get().volumetricBeamBrightness = value; ConfigManager.save(); }
    public static void setVolumetricBeamFadeLength(float value) { get().volumetricBeamFadeLength = value; ConfigManager.save(); }
    public static void setLaserBeamLength(float value) { get().laserBeamLength = Math.max(20f, value); ConfigManager.save(); }
    public static void setRgbBarBeamLength(float value) { get().rgbBarBeamLength = Math.max(1f, value); ConfigManager.save(); }
    public static void setRenderLens(boolean value) { get().renderLens = value; ConfigManager.save(); }
    public static void setMaxGoboDistance(float value) { get().maxGoboDistance = value; ConfigManager.save(); }

    public static void load() {
        ConfigManager.load();
    }

    public static void save() {
        ConfigManager.save();
    }

    public static void reload() {
        ConfigManager.reload();
    }

    public static boolean isLaserPassThrough(String blockId) {
        if (get().laserPassThroughSet == null) {
            get().laserPassThroughSet = get().laserPassThroughBlocks == null
                    ? new HashSet<>()
                    : new HashSet<>(get().laserPassThroughBlocks);
        }
        return get().laserPassThroughSet.contains(blockId);
    }
}