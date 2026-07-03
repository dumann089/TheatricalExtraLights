package com.github.dumann089.theatricalextralights.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TheatricalExtraLightsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File("config/theatricalextralights.json");
    private static TheatricalExtraLightsConfig INSTANCE = new TheatricalExtraLightsConfig();

    /* ================= CAMPOS DE CONFIGURACIÓN ================= */



    private Float laserBeamLength = 400.0f;
    private Float rgbBarBeamLength = 9.0f;
    private Boolean renderLens = true;
    private Float maxGoboDistance = 500.0f;
    private Boolean render2DBeam = false;
    private List<String> laserPassThroughBlocks = null;

    private Boolean volumetricBeamEnabled = false;
    private Float volumetricBeamDistance = 64.0f;
    private Float volumetricBeamBrightness = 0.15f;

    private Integer volumetricBeamSlices = 128;
    private Float volumetricBeamDensity = 0.15f;
    private Float volumetricBeamMaxAlpha = 0.15f;
    private Float volumetricBeamFadeLength = 12.0f;

    private Integer maxConcurrentRockets = 768;
    private Integer maxSparksPerRocket = 600;
    private Double fireworkRenderDistance = 2048.0;
    private Boolean fireworkDynamicLightEnabled = true;
    private Boolean fireworkSmokeEnabled = true;
    private Integer fireworkSmokeBudgetPerTick = 24;
    private Integer fireworkSmokeSpawnInterval = 3;

    private transient Set<String> laserPassThroughSet;

    static {
        load();
    }

    public static void reload() {
        load();
    }

    public static void load() {
        File parent = FILE.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                TheatricalExtraLightsConfig loaded = GSON.fromJson(reader, TheatricalExtraLightsConfig.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        save();
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ================= GETTERS ================= */

    public static boolean isVolumetricBeamEnabled() { return INSTANCE.volumetricBeamEnabled; }
    public static float getVolumetricBeamDistance() { return INSTANCE.volumetricBeamDistance; }
    public static float getVolumetricBeamBrightness() { return INSTANCE.volumetricBeamBrightness; }
    public static int getVolumetricBeamSlices() { return INSTANCE.volumetricBeamSlices; }
    public static float getVolumetricBeamDensity() { return INSTANCE.volumetricBeamDensity; }
    public static float getVolumetricBeamMaxAlpha() { return INSTANCE.volumetricBeamMaxAlpha; }
    public static float getVolumetricBeamFadeLength() { return INSTANCE.volumetricBeamFadeLength != null ? INSTANCE.volumetricBeamFadeLength : 2.0f; }
    public static float getLaserBeamLength() { return INSTANCE.laserBeamLength; }
    public static float getRgbBarBeamLength() { return INSTANCE.rgbBarBeamLength; }
    public static boolean shouldRenderLens() { return INSTANCE.renderLens; }
    public static float getMaxGoboDistance() { return INSTANCE.maxGoboDistance; }
    public static boolean shouldRender2DBeam() { return INSTANCE.render2DBeam; }

    public static int getMaxConcurrentRockets() { return INSTANCE.maxConcurrentRockets != null ? INSTANCE.maxConcurrentRockets : 768; }
    public static int getMaxSparksPerRocket() { return INSTANCE.maxSparksPerRocket != null ? INSTANCE.maxSparksPerRocket : 600; }
    public static double getFireworkRenderDistance() { return INSTANCE.fireworkRenderDistance != null ? INSTANCE.fireworkRenderDistance : 2048.0; }
    public static boolean isFireworkDynamicLightEnabled() { return INSTANCE.fireworkDynamicLightEnabled == null || INSTANCE.fireworkDynamicLightEnabled; }
    public static boolean isFireworkSmokeEnabled() { return INSTANCE.fireworkSmokeEnabled == null || INSTANCE.fireworkSmokeEnabled; }
    public static int getFireworkSmokeBudgetPerTick() { return INSTANCE.fireworkSmokeBudgetPerTick != null ? INSTANCE.fireworkSmokeBudgetPerTick : 24; }
    public static int getFireworkSmokeSpawnInterval() { return Math.max(1, INSTANCE.fireworkSmokeSpawnInterval != null ? INSTANCE.fireworkSmokeSpawnInterval : 3); }

    /* ================= SETTERS ================= */

    public static void setVolumetricBeamEnabled(boolean value) { INSTANCE.volumetricBeamEnabled = value; save(); }
    public static void setVolumetricBeamDistance(float value) { INSTANCE.volumetricBeamDistance = value; save(); }
    public static void setVolumetricBeamBrightness(float value) { INSTANCE.volumetricBeamBrightness = value; save(); }
    public static void setVolumetricBeamFadeLength(float value) { INSTANCE.volumetricBeamFadeLength = value; save(); }
    public static void setLaserBeamLength(float value) { INSTANCE.laserBeamLength = Math.max(20f, value); save(); }
    public static void setRgbBarBeamLength(float value) { INSTANCE.rgbBarBeamLength = Math.max(1f, value); save(); }
    public static void setRenderLens(boolean value) { INSTANCE.renderLens = value; save(); }
    public static void setMaxGoboDistance(float value) { INSTANCE.maxGoboDistance = value; save(); }

    public static boolean isLaserPassThrough(String blockId) {
        if (INSTANCE.laserPassThroughSet == null) {
            INSTANCE.laserPassThroughSet = INSTANCE.laserPassThroughBlocks == null
                    ? new HashSet<>()
                    : new HashSet<>(INSTANCE.laserPassThroughBlocks);
        }
        return INSTANCE.laserPassThroughSet.contains(blockId);
    }
}