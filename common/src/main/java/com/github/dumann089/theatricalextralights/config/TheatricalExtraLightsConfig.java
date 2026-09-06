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
    private Boolean render2DBeam = true;
    private List<String> laserPassThroughBlocks = null;

    private Boolean volumetricBeamEnabled = true;
    private Float volumetricBeamDistance = 64.0f;
    private Float volumetricBeamBrightness = 0.15f;

    private Integer volumetricBeamSlices = 128;
    private Float volumetricBeamDensity = 0.15f;
    private Float volumetricBeamMaxAlpha = 0.15f;
    private Float volumetricBeamFadeLength = 12.0f;

    /** RAYMARCH (default) or LEGACY_SLICES */
    private String volumetricEngine = "RAYMARCH";
    /** LOW, MEDIUM, HIGH, ULTRA */
    private String raymarchQuality = "HIGH";
    private Float raymarchAnisotropy = 0.55f;
    private Float raymarchDustAmount = 0.55f;
    private Integer raymarchMaxBeamsPerFrame = 128;

    /**
     * Dimensionne la tache lumineuse des lyres sur la section du cone a la distance eclairee,
     * au lieu du rayon derive du seul focus par Theatrical, qui ignore la distance.
     */
    private Boolean spotFollowsBeam = true;
    /** Garde-fou : au-dela, la lumiere dynamique couvrirait un volume absurde. */
    private Float spotMaxRadius = 48.0f;

    private Integer maxConcurrentRockets = 768;
    private Integer maxSparksPerRocket = 600;
    private Double fireworkRenderDistance = 2048.0;
    /** Si true, la portée pyro suit la render distance client / view distance serveur (plafonnée par fireworkRenderDistance). */
    private Boolean fireworkDynamicRenderDistance = true;
    private Boolean fireworkDynamicLightEnabled = true;
    private Boolean fireworkSmokeEnabled = true;
    private Integer fireworkSmokeBudgetPerTick = 24;
    private Integer fireworkSmokeSpawnInterval = 3;

    private Integer ledFacadeMaxUniverses = 64;

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

    /**
     * Ecriture differee. L'ecran de reglages ouvre un lot a l'ouverture et le ferme a la
     * fermeture : les curseurs modifient la config en direct pour l'apercu, sans reecrire le
     * fichier JSON a chaque pixel de glissement.
     */
    private static boolean batching = false;

    public static void beginBatch() {
        batching = true;
    }

    public static void endBatch() {
        batching = false;
        save();
    }

    /**
     * Remet tous les reglages a leurs valeurs par defaut.
     *
     * <p>On repart d'une instance neuve plutot que de reaffecter champ par champ : les defauts
     * sont portes par les initialiseurs de champs, donc c'est la seule facon de garantir
     * qu'aucun reglage n'est oublie quand on en ajoute un.
     */
    public static void resetDefaults() {
        INSTANCE = new TheatricalExtraLightsConfig();
        save();
    }

    public static void save() {
        if (batching) return;

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

    public static boolean isRaymarchEngine() {
        String engine = INSTANCE.volumetricEngine;
        return engine == null || !"LEGACY_SLICES".equalsIgnoreCase(engine.trim());
    }

    public static String getRaymarchQuality() {
        return INSTANCE.raymarchQuality != null ? INSTANCE.raymarchQuality : "HIGH";
    }

    public static int getRaymarchSteps() {
        String q = getRaymarchQuality().trim().toUpperCase();
        return switch (q) {
            case "LOW" -> 8;
            case "MEDIUM" -> 16;
            case "ULTRA" -> 32;
            default -> 24; // HIGH
        };
    }

    public static float getRaymarchAnisotropy() {
        return INSTANCE.raymarchAnisotropy != null ? INSTANCE.raymarchAnisotropy : 0.55f;
    }

    public static float getRaymarchDustAmount() {
        return INSTANCE.raymarchDustAmount != null ? INSTANCE.raymarchDustAmount : 0.55f;
    }

    public static boolean doesSpotFollowBeam() {
        return INSTANCE.spotFollowsBeam == null || INSTANCE.spotFollowsBeam;
    }

    public static float getSpotMaxRadius() {
        float v = INSTANCE.spotMaxRadius != null ? INSTANCE.spotMaxRadius : 48.0f;
        return Math.max(1.0f, Math.min(256.0f, v));
    }

    public static void setSpotMaxRadius(float value) {
        INSTANCE.spotMaxRadius = Math.max(1.0f, Math.min(256.0f, value));
        save();
    }

    /** RAYMARCH ou LEGACY_SLICES. */
    public static String getVolumetricEngine() {
        return INSTANCE.volumetricEngine != null ? INSTANCE.volumetricEngine : "RAYMARCH";
    }

    public static void setVolumetricEngine(String value) {
        INSTANCE.volumetricEngine = "LEGACY_SLICES".equalsIgnoreCase(value) ? "LEGACY_SLICES" : "RAYMARCH";
        save();
    }

    public static void setRaymarchQuality(String value) {
        String q = value == null ? "HIGH" : value.trim().toUpperCase();
        INSTANCE.raymarchQuality = switch (q) {
            case "LOW", "MEDIUM", "ULTRA" -> q;
            default -> "HIGH";
        };
        save();
    }

    public static void setRaymarchAnisotropy(float value) {
        INSTANCE.raymarchAnisotropy = Math.max(-0.9f, Math.min(0.9f, value));
        save();
    }

    public static void setRaymarchDustAmount(float value) {
        INSTANCE.raymarchDustAmount = Math.max(0.0f, Math.min(1.0f, value));
        save();
    }

    public static void setRaymarchMaxBeamsPerFrame(int value) {
        INSTANCE.raymarchMaxBeamsPerFrame = Math.max(1, Math.min(128, value));
        save();
    }

    public static void setRender2DBeam(boolean value) { INSTANCE.render2DBeam = value; save(); }

    public static void setVolumetricBeamSlices(int value) {
        INSTANCE.volumetricBeamSlices = Math.max(16, Math.min(512, value));
        save();
    }

    public static void setVolumetricBeamDensity(float value) {
        INSTANCE.volumetricBeamDensity = Math.max(0.0f, Math.min(2.0f, value));
        save();
    }

    public static void setVolumetricBeamMaxAlpha(float value) {
        INSTANCE.volumetricBeamMaxAlpha = Math.max(0.0f, Math.min(1.0f, value));
        save();
    }

    public static void setSpotFollowsBeam(boolean value) {
        INSTANCE.spotFollowsBeam = value;
        save();
    }

    public static int getRaymarchMaxBeamsPerFrame() {
        int value = INSTANCE.raymarchMaxBeamsPerFrame != null ? INSTANCE.raymarchMaxBeamsPerFrame : 128;
        return Math.max(1, Math.min(128, value));
    }
    public static float getLaserBeamLength() { return INSTANCE.laserBeamLength; }
    public static float getRgbBarBeamLength() { return INSTANCE.rgbBarBeamLength; }
    public static boolean shouldRenderLens() { return INSTANCE.renderLens; }
    public static float getMaxGoboDistance() { return INSTANCE.maxGoboDistance; }
    public static boolean shouldRender2DBeam() { return INSTANCE.render2DBeam; }

    public static int getMaxConcurrentRockets() { return INSTANCE.maxConcurrentRockets != null ? INSTANCE.maxConcurrentRockets : 768; }
    public static int getMaxSparksPerRocket() { return INSTANCE.maxSparksPerRocket != null ? INSTANCE.maxSparksPerRocket : 600; }
    public static double getFireworkRenderDistance() { return INSTANCE.fireworkRenderDistance != null ? INSTANCE.fireworkRenderDistance : 2048.0; }
    public static boolean useFireworkDynamicRenderDistance() { return INSTANCE.fireworkDynamicRenderDistance == null || INSTANCE.fireworkDynamicRenderDistance; }
    public static boolean isFireworkDynamicLightEnabled() { return INSTANCE.fireworkDynamicLightEnabled == null || INSTANCE.fireworkDynamicLightEnabled; }
    public static boolean isFireworkSmokeEnabled() { return INSTANCE.fireworkSmokeEnabled == null || INSTANCE.fireworkSmokeEnabled; }
    public static int getFireworkSmokeBudgetPerTick() { return INSTANCE.fireworkSmokeBudgetPerTick != null ? INSTANCE.fireworkSmokeBudgetPerTick : 24; }
    public static int getFireworkSmokeSpawnInterval() { return Math.max(1, INSTANCE.fireworkSmokeSpawnInterval != null ? INSTANCE.fireworkSmokeSpawnInterval : 3); }
    public static int getLedFacadeMaxUniverses() { return INSTANCE.ledFacadeMaxUniverses != null ? INSTANCE.ledFacadeMaxUniverses : 64; }

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