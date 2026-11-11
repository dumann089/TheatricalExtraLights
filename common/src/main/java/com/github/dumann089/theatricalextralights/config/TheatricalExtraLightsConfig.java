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
    /** Multiplicateur des nappes de haze des barres LED (RGB Bar, Vertical RGB Bar, Moving Bar) ; 1 = reglage d'origine. */
    private Float rgbBarBeamIntensity = 1.0f;
    /** Multiplicateur de portee du jet CO2 (Flow2Jet) ; 1 = environ 9 blocs a pleine pression. */
    private Float co2JetReach = 1.0f;
    private Boolean renderLens = true;
    private Float maxGoboDistance = 500.0f;
    /** Multiplicador de brilho do gobo projetado (shader GoboBrightness). */
    private Float goboBrightness = 1.0f;
    /** Quanto o gobo toma a iluminacao da superficie (shader SurfaceTintStrength), 0..1. */
    private Float goboSurfaceTint = 0.6f;
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
    /** Ombres portees des blocs et entites dans le faisceau et sur la tache projetee. */
    private Boolean beamShadows = true;

    /** Laser realiste : faisceaux fins et nappes integres dans la brume, impacts sur les surfaces. */
    private Boolean laserRealistic = true;
    /** Densite de brume vue par les lasers, 0 = air limpide (seuls les impacts restent). */
    private Float laserHaze = 0.7f;
    private Float laserBrightness = 1.0f;
    /** Rayon du faisceau a la sortie, centimetres. */
    private Float laserBeamRadiusCm = 1.0f;
    private Boolean laserImpacts = true;
    /** Tete de balayage visible quand la persistance DMX est basse. */
    private Boolean laserScanFlicker = true;

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
    /** Bloom des flammes (post-process sans shaderpack). */
    private Boolean flameBloom = true;
    private Float flameBloomStrength = 1.0f;
    /**
     * Flammes en volume (shader, experimental) au lieu des particules. Nouvelle cle, coupee par
     * defaut : l'ancienne {@code realisticFlames} etait a true dans les configs existantes.
     */
    private Boolean volumetricFlames = false;
    /** LOW, MEDIUM, HIGH, ULTRA : pas de marche et octaves de turbulence. */
    private String flameQuality = "HIGH";
    private Float flameBrightness = 1.0f;
    /** Distorsion de chaleur : copie de l'image par image quand une flamme est visible. */
    private Boolean flameHeatDistortion = true;
    private Float flameHeatStrength = 1.0f;
    /** Lumiere dynamique qui vacille avec la flamme (reconstruit les chunks autour, a 6 Hz). */
    private Boolean flameLightFlicker = true;
    private Integer fireworkSmokeBudgetPerTick = 24;
    private Integer fireworkSmokeSpawnInterval = 3;

    private Integer ledFacadeMaxUniverses = 64;

    private Boolean etherDreamEnabled = true;
    private String etherDreamBindAddress = "0.0.0.0";
    private Integer etherDreamTcpPort = 7765;
    private Integer etherDreamBroadcastPort = 7654;
    private Integer etherDreamBufferCapacity = 1800;
    private Integer etherDreamMaxPointRate = 100000;
    private Integer etherDreamHwRevision = 2;
    private Integer etherDreamSwRevision = 2;
    private String etherDreamMac = "02:00:00:ED:01:00";
    /**
     * Segments max d'une figure DAC par image. Au-dela, les points sont decimes et les courbes
     * (un « 60 » en texte) deviennent anguleuses. Chaque segment coute a chaque pixel couvert.
     */
    private Integer laserDacMaxRays = 192;
    private Integer laserDacBlankThreshold = 256;
    /** Persistence window (ms) used to rebuild the scan picture: must cover a full ILDA frame (>= 1/fps). */
    private Integer laserDacPersistenceMs = 90;
    /** Volumetric haze budget per projector: raymarched needle rays and scan sheets. */
    private Integer laserDacVolumetricRays = 24;
    private Integer laserDacVolumetricSheets = 24;
    /** Haze radius of a needle ray (blocks) and thickness of a scan sheet (blocks). */
    private Float laserDacHazeRadius = 0.11f;
    private Float laserDacSheetThickness = 0.07f;
    /** Velocidad de la transición suave de color/intensidad por tick. 1.0 = instantáneo. */
    private Float colorTransitionSpeed = 0.35f;

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

    public static boolean isBeamShadowsEnabled() {
        return INSTANCE.beamShadows == null || INSTANCE.beamShadows;
    }

    public static void setBeamShadows(boolean value) {
        INSTANCE.beamShadows = value;
        save();
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
        INSTANCE.raymarchMaxBeamsPerFrame = Math.max(1, Math.min(512, value));
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
        return Math.max(1, Math.min(512, value));
    }
    /* ---- Laser realiste ---- */

    public static boolean isLaserRealistic() {
        return INSTANCE.laserRealistic == null || INSTANCE.laserRealistic;
    }

    public static void setLaserRealistic(boolean value) {
        INSTANCE.laserRealistic = value;
        save();
    }

    public static float getLaserHaze() {
        float v = INSTANCE.laserHaze != null ? INSTANCE.laserHaze : 0.7f;
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    public static void setLaserHaze(float value) {
        INSTANCE.laserHaze = Math.max(0.0f, Math.min(1.0f, value));
        save();
    }

    public static float getLaserBrightness() {
        float v = INSTANCE.laserBrightness != null ? INSTANCE.laserBrightness : 1.0f;
        return Math.max(0.05f, Math.min(4.0f, v));
    }

    public static void setLaserBrightness(float value) {
        INSTANCE.laserBrightness = Math.max(0.05f, Math.min(4.0f, value));
        save();
    }

    public static float getLaserBeamRadiusCm() {
        float v = INSTANCE.laserBeamRadiusCm != null ? INSTANCE.laserBeamRadiusCm : 1.0f;
        return Math.max(0.2f, Math.min(6.0f, v));
    }

    public static void setLaserBeamRadiusCm(float value) {
        INSTANCE.laserBeamRadiusCm = Math.max(0.2f, Math.min(6.0f, value));
        save();
    }

    public static boolean isLaserImpactsEnabled() {
        return INSTANCE.laserImpacts == null || INSTANCE.laserImpacts;
    }

    public static void setLaserImpacts(boolean value) {
        INSTANCE.laserImpacts = value;
        save();
    }

    public static boolean isLaserScanFlickerEnabled() {
        return INSTANCE.laserScanFlicker == null || INSTANCE.laserScanFlicker;
    }

    public static void setLaserScanFlicker(boolean value) {
        INSTANCE.laserScanFlicker = value;
        save();
    }

    public static float getLaserBeamLength() { return INSTANCE.laserBeamLength; }
    public static float getRgbBarBeamLength() { return INSTANCE.rgbBarBeamLength != null ? INSTANCE.rgbBarBeamLength : 9.0f; }
    public static float getRgbBarBeamIntensity() {
        float v = INSTANCE.rgbBarBeamIntensity != null ? INSTANCE.rgbBarBeamIntensity : 1.0f;
        return Math.max(0f, Math.min(4.0f, v));
    }
    public static float getCo2JetReach() {
        float v = INSTANCE.co2JetReach != null ? INSTANCE.co2JetReach : 1.0f;
        return Math.max(0.5f, Math.min(3.0f, v));
    }
    public static boolean shouldRenderLens() { return INSTANCE.renderLens; }
    public static float getMaxGoboDistance() { return INSTANCE.maxGoboDistance; }
    public static float getGoboBrightness() {
        float v = INSTANCE.goboBrightness != null ? INSTANCE.goboBrightness : 1.0f;
        return Math.max(0.0f, Math.min(4.0f, v));
    }
    public static float getGoboSurfaceTint() {
        float v = INSTANCE.goboSurfaceTint != null ? INSTANCE.goboSurfaceTint : 0.6f;
        return Math.max(0.0f, Math.min(1.0f, v));
    }
    public static boolean shouldRender2DBeam() { return INSTANCE.render2DBeam; }

    public static int getMaxConcurrentRockets() { return INSTANCE.maxConcurrentRockets != null ? INSTANCE.maxConcurrentRockets : 768; }
    public static int getMaxSparksPerRocket() { return INSTANCE.maxSparksPerRocket != null ? INSTANCE.maxSparksPerRocket : 600; }
    public static double getFireworkRenderDistance() { return INSTANCE.fireworkRenderDistance != null ? INSTANCE.fireworkRenderDistance : 2048.0; }
    public static boolean useFireworkDynamicRenderDistance() { return INSTANCE.fireworkDynamicRenderDistance == null || INSTANCE.fireworkDynamicRenderDistance; }
    public static boolean isFireworkDynamicLightEnabled() { return INSTANCE.fireworkDynamicLightEnabled == null || INSTANCE.fireworkDynamicLightEnabled; }
    public static boolean isVolumetricFlamesEnabled() { return INSTANCE.volumetricFlames != null && INSTANCE.volumetricFlames; }
    public static void setVolumetricFlames(boolean value) { INSTANCE.volumetricFlames = value; save(); }
    public static String getFlameQuality() { return INSTANCE.flameQuality != null ? INSTANCE.flameQuality : "HIGH"; }
    public static void setFlameQuality(String value) {
        String q = value == null ? "HIGH" : value.trim().toUpperCase();
        INSTANCE.flameQuality = switch (q) {
            case "LOW", "MEDIUM", "ULTRA" -> q;
            default -> "HIGH";
        };
        save();
    }
    public static int getFlameSteps() {
        return switch (getFlameQuality().trim().toUpperCase()) {
            case "LOW" -> 12;
            case "MEDIUM" -> 18;
            case "ULTRA" -> 40;
            default -> 26;
        };
    }
    public static int getFlameNoiseOctaves() {
        return switch (getFlameQuality().trim().toUpperCase()) {
            case "LOW" -> 2;
            case "MEDIUM" -> 3;
            case "ULTRA" -> 5;
            default -> 4;
        };
    }
    public static float getFlameBrightness() {
        float v = INSTANCE.flameBrightness != null ? INSTANCE.flameBrightness : 1.0f;
        return Math.max(0.2f, Math.min(3.0f, v));
    }
    public static void setFlameBrightness(float value) { INSTANCE.flameBrightness = Math.max(0.2f, Math.min(3.0f, value)); save(); }
    public static boolean isFlameHeatDistortionEnabled() { return INSTANCE.flameHeatDistortion == null || INSTANCE.flameHeatDistortion; }
    public static void setFlameHeatDistortion(boolean value) { INSTANCE.flameHeatDistortion = value; save(); }
    public static float getFlameHeatStrength() {
        float v = INSTANCE.flameHeatStrength != null ? INSTANCE.flameHeatStrength : 1.0f;
        return Math.max(0.0f, Math.min(3.0f, v));
    }
    public static void setFlameHeatStrength(float value) { INSTANCE.flameHeatStrength = Math.max(0.0f, Math.min(3.0f, value)); save(); }
    public static boolean isFlameLightFlickerEnabled() { return INSTANCE.flameLightFlicker == null || INSTANCE.flameLightFlicker; }
    public static void setFlameLightFlicker(boolean value) { INSTANCE.flameLightFlicker = value; save(); }

    public static boolean isFlameBloomEnabled() { return INSTANCE.flameBloom == null || INSTANCE.flameBloom; }
    public static void setFlameBloom(boolean value) { INSTANCE.flameBloom = value; save(); }
    public static float getFlameBloomStrength() {
        float v = INSTANCE.flameBloomStrength != null ? INSTANCE.flameBloomStrength : 1.0f;
        return Math.max(0.1f, Math.min(3.0f, v));
    }
    public static void setFlameBloomStrength(float value) {
        INSTANCE.flameBloomStrength = Math.max(0.1f, Math.min(3.0f, value));
        save();
    }
    public static boolean isFireworkSmokeEnabled() { return INSTANCE.fireworkSmokeEnabled == null || INSTANCE.fireworkSmokeEnabled; }
    public static int getFireworkSmokeBudgetPerTick() { return INSTANCE.fireworkSmokeBudgetPerTick != null ? INSTANCE.fireworkSmokeBudgetPerTick : 24; }
    public static int getFireworkSmokeSpawnInterval() { return Math.max(1, INSTANCE.fireworkSmokeSpawnInterval != null ? INSTANCE.fireworkSmokeSpawnInterval : 3); }
    public static int getLedFacadeMaxUniverses() { return INSTANCE.ledFacadeMaxUniverses != null ? INSTANCE.ledFacadeMaxUniverses : 64; }
    public static float getColorTransitionSpeed() {
        float v = INSTANCE.colorTransitionSpeed != null ? INSTANCE.colorTransitionSpeed : 0.35f;
        return Math.max(0.01f, Math.min(1.0f, v));
    }

    public static boolean isEtherDreamEnabled() { return INSTANCE.etherDreamEnabled == null || INSTANCE.etherDreamEnabled; }
    public static void setEtherDreamEnabled(boolean value) { INSTANCE.etherDreamEnabled = value; save(); }
    public static String getEtherDreamBindAddress() {
        return INSTANCE.etherDreamBindAddress == null || INSTANCE.etherDreamBindAddress.isBlank()
                ? "0.0.0.0" : INSTANCE.etherDreamBindAddress.trim();
    }
    public static int getEtherDreamTcpPort() {
        int port = INSTANCE.etherDreamTcpPort != null ? INSTANCE.etherDreamTcpPort : 7765;
        return Math.max(1, Math.min(65535, port));
    }
    public static int getEtherDreamBroadcastPort() {
        int port = INSTANCE.etherDreamBroadcastPort != null ? INSTANCE.etherDreamBroadcastPort : 7654;
        return Math.max(1, Math.min(65535, port));
    }
    public static int getEtherDreamBufferCapacity() {
        int value = INSTANCE.etherDreamBufferCapacity != null ? INSTANCE.etherDreamBufferCapacity : 1800;
        return Math.max(256, Math.min(32768, value));
    }
    public static int getEtherDreamMaxPointRate() {
        int value = INSTANCE.etherDreamMaxPointRate != null ? INSTANCE.etherDreamMaxPointRate : 100000;
        return Math.max(1000, Math.min(200000, value));
    }
    public static int getEtherDreamHwRevision() {
        return INSTANCE.etherDreamHwRevision != null ? INSTANCE.etherDreamHwRevision : 2;
    }
    public static int getEtherDreamSwRevision() {
        return INSTANCE.etherDreamSwRevision != null ? INSTANCE.etherDreamSwRevision : 2;
    }
    public static byte[] getEtherDreamMac() {
        byte[] parsed = parseMac(INSTANCE.etherDreamMac);
        return parsed != null ? parsed : com.github.dumann089.theatricalextralights.laser.dac.LaserProtocol.DEFAULT_MAC.clone();
    }
    public static int getLaserDacMaxRays() {
        int value = INSTANCE.laserDacMaxRays != null ? INSTANCE.laserDacMaxRays : 192;
        return Math.max(8, Math.min(MAX_LASER_SEGMENTS, value));
    }
    public static void setLaserDacMaxRays(int value) {
        INSTANCE.laserDacMaxRays = Math.max(8, Math.min(MAX_LASER_SEGMENTS, value));
        save();
    }
    /** Doit rester egal a LaserFigure.MAX_SEGMENTS et a la borne de boucle de laser_raymarch.fsh. */
    public static final int MAX_LASER_SEGMENTS = 512;
    public static int getLaserDacPersistenceMs() {
        int value = INSTANCE.laserDacPersistenceMs != null ? INSTANCE.laserDacPersistenceMs : 90;
        return Math.max(20, Math.min(400, value));
    }
    public static int getLaserDacVolumetricRays() {
        int value = INSTANCE.laserDacVolumetricRays != null ? INSTANCE.laserDacVolumetricRays : 24;
        return Math.max(0, Math.min(32, value));
    }
    public static int getLaserDacVolumetricSheets() {
        int value = INSTANCE.laserDacVolumetricSheets != null ? INSTANCE.laserDacVolumetricSheets : 24;
        return Math.max(0, Math.min(32, value));
    }
    public static float getLaserDacHazeRadius() {
        float value = INSTANCE.laserDacHazeRadius != null ? INSTANCE.laserDacHazeRadius : 0.11f;
        return Math.max(0.03f, Math.min(0.5f, value));
    }
    public static float getLaserDacSheetThickness() {
        float value = INSTANCE.laserDacSheetThickness != null ? INSTANCE.laserDacSheetThickness : 0.07f;
        return Math.max(0.02f, Math.min(0.4f, value));
    }
    public static int getLaserDacBlankThreshold() {
        int value = INSTANCE.laserDacBlankThreshold != null ? INSTANCE.laserDacBlankThreshold : 256;
        return Math.max(1, Math.min(4096, value));
    }

    private static byte[] parseMac(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.trim().split("[:\\-]");
        if (parts.length != 6) {
            return null;
        }
        byte[] mac = new byte[6];
        try {
            for (int i = 0; i < 6; i++) {
                mac[i] = (byte) Integer.parseInt(parts[i], 16);
            }
            return mac;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /* ================= SETTERS ================= */

    public static void setVolumetricBeamEnabled(boolean value) { INSTANCE.volumetricBeamEnabled = value; save(); }
    public static void setVolumetricBeamDistance(float value) { INSTANCE.volumetricBeamDistance = value; save(); }
    public static void setVolumetricBeamBrightness(float value) { INSTANCE.volumetricBeamBrightness = value; save(); }
    public static void setVolumetricBeamFadeLength(float value) { INSTANCE.volumetricBeamFadeLength = value; save(); }
    public static void setLaserBeamLength(float value) { INSTANCE.laserBeamLength = Math.max(20f, value); save(); }
    public static void setRgbBarBeamLength(float value) { INSTANCE.rgbBarBeamLength = Math.max(1f, Math.min(64f, value)); save(); }
    public static void setRgbBarBeamIntensity(float value) { INSTANCE.rgbBarBeamIntensity = Math.max(0f, Math.min(4.0f, value)); save(); }
    public static void setCo2JetReach(float value) { INSTANCE.co2JetReach = Math.max(0.5f, Math.min(3.0f, value)); save(); }
    public static void setRenderLens(boolean value) { INSTANCE.renderLens = value; save(); }
    public static void setMaxGoboDistance(float value) { INSTANCE.maxGoboDistance = value; save(); }
    public static void setGoboBrightness(float value) {
        INSTANCE.goboBrightness = Math.max(0.0f, Math.min(4.0f, value));
        save();
    }
    public static void setGoboSurfaceTint(float value) {
        INSTANCE.goboSurfaceTint = Math.max(0.0f, Math.min(1.0f, value));
        save();
    }
    public static void setColorTransitionSpeed(float value) {
        INSTANCE.colorTransitionSpeed = Math.max(0.01f, Math.min(1.0f, value));
        save();
    }

    public static boolean isLaserPassThrough(String blockId) {
        if (INSTANCE.laserPassThroughSet == null) {
            INSTANCE.laserPassThroughSet = INSTANCE.laserPassThroughBlocks == null
                    ? new HashSet<>()
                    : new HashSet<>(INSTANCE.laserPassThroughBlocks);
        }
        return INSTANCE.laserPassThroughSet.contains(blockId);
    }
}