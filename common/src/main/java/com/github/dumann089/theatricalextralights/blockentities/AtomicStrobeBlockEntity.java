package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasPersonality;
import com.github.dumann089.theatricalextralights.blocks.AtomicStrobeBlock;
import com.github.dumann089.theatricalextralights.client.StrobeRenderHelper;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import com.github.dumann089.theatricalextralights.util.AtomicStrobeEngine;
import com.github.dumann089.theatricalextralights.util.BlockEntitySync;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity;
import dev.imabad.theatrical.lighting.LightManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * Strobe type Atomic : un tube strobe blanc de neuf segments entre deux plaques de quatre zones
 * RGB. Les valeurs DMX brutes de la personnalite active sont stockees et synchronisees telles
 * quelles ; tout le reste (tube, plaque, niveau instantane des flashs) en est deduit des deux
 * cotes avec {@link AtomicStrobeEngine}, fonction du temps de jeu.
 *
 * @see com.github.dumann089.theatricalextralights.fixtures.AtomicStrobeFixture pour l'ordre des modes
 */
public class AtomicStrobeBlockEntity extends ExtraLightsLightBlockEntity implements HasPersonality {

    public static final int MODE_LEGACY = 0;
    public static final int MODE_STROBE_1CH = 1;
    public static final int MODE_ATOMIC_4CH = 2;
    public static final int MODE_AURA_8CH = 3;
    public static final int MODE_COMPRESSED_10CH = 4;
    public static final int MODE_PIXEL_40CH = 5;
    public static final int MODE_PIXELMAP_400CH = 6;
    private static final int[] CHANNEL_COUNTS = {34, 1, 4, 8, 10, 40, 400};

    public static final int RGB_ZONE_COUNT = 8;
    public static final int WHITE_SEGMENT_COUNT = 9;
    /** Chaque plaque est une grille de 12 x 4 pixels ; les zones en pilotent trois colonnes. */
    public static final int PIXEL_COLS = 12;
    public static final int PIXEL_ROWS = 4;
    public static final int PLATE_PIXELS = PIXEL_COLS * PIXEL_ROWS;
    public static final int PIXEL_COUNT = 2 * PLATE_PIXELS;
    public static final int MAX_CHANNELS = 400;

    private static final float MIN_LIGHT_SPREAD = 18.0f;
    private static final float MAX_LIGHT_SPREAD = 40.0f;
    private static final float EMISSION_DISTANCE = 12.0f;
    /** Chaque segment du tube pese autant que deux zones RGB dans la couleur de la lumiere. */
    private static final int BAR_SEGMENT_WEIGHT = 2;
    /** Mode 1 canal : duree de flash fixe, courte. */
    private static final int STROBE_1CH_DURATION = 40;

    private int personality = MODE_LEGACY;
    /** Valeurs DMX brutes de la personnalite active, les seules donnees synchronisees. */
    private final byte[] dmx = new byte[MAX_CHANNELS];

    // Deduit de dmx et de la personnalite.
    private int barIntensity;
    private int barDuration;
    private int barRate;
    private int barEffect;
    private final int[] barSegments = new int[WHITE_SEGMENT_COUNT];
    private int plateIntensity;
    private int plateDuration;
    private int plateRate;
    /** Couleur moyenne par zone, deduite des pixels ; sert a la lumiere dynamique et au halo. */
    private final int[] plateRgb = new int[RGB_ZONE_COUNT * 3];
    private final int[] zoneBright = new int[RGB_ZONE_COUNT];
    /** Dim, R, G, B par pixel : plaque haute puis basse, rangee par rangee de haut en bas, de gauche a droite. */
    private final int[] pixels = new int[PIXEL_COUNT * 4];

    private AtomicZoneLight[] zoneLights;

    public AtomicStrobeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.ATOMIC_STROBE.get(), pos, state);
        setChannelCount(CHANNEL_COUNTS[personality]);
        focus = 255;
        decode();
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.ATOMIC_STROBE.get();
    }

    // ── Personnalite ────────────────────────────────────────────────────────────

    @Override
    public int getActivePersonality() {
        return personality;
    }

    @Override
    public void setActivePersonality(int index) {
        if (index < 0 || index >= CHANNEL_COUNTS.length) {
            return;
        }
        personality = index;
        setChannelCount(CHANNEL_COUNTS[personality]);
        Arrays.fill(dmx, (byte) 0);
        decode();
        aggregate();
        setChanged();
        if (level != null) {
            BlockEntitySync.sendData(this);
        }
    }

    /** Les canaux ne passent jamais par le lot DmxFrame : le bloc entier est synchronise. */
    @Override
    protected boolean hasExtraDmxChannelsBeyondBatch() {
        return true;
    }

    // ── DMX ─────────────────────────────────────────────────────────────────────

    @Override
    public void consume(byte[] dmxValues) {
        int count = CHANNEL_COUNTS[personality];
        int start = getChannelStart() > 0 ? getChannelStart() - 1 : 0;
        byte[] v = Arrays.copyOfRange(dmxValues, start, start + count);
        if (v.length < count) {
            return;
        }
        boolean prevAdvanced = beginDmxUpdate();
        int _pi = intensity, _pr = red, _pg = green, _pb = blue;
        boolean changed = false;
        for (int i = 0; i < count; i++) {
            if (dmx[i] != v[i]) {
                dmx[i] = v[i];
                changed = true;
            }
        }
        decode();
        aggregate();
        changed |= intensity != _pi || red != _pr || green != _pg || blue != _pb;
        finishDmxUpdate(changed, prevAdvanced);
    }

    private int ch(int index) {
        return Byte.toUnsignedInt(dmx[index]);
    }

    /** Traduit les canaux bruts de la personnalite active en tube + plaque. */
    private void decode() {
        Arrays.fill(barSegments, 255);
        plateRate = 0;
        plateDuration = 0;
        switch (personality) {
            case MODE_STROBE_1CH -> {
                int s = ch(0);
                plateIntensity = 0;
                if (s <= 0) {
                    barIntensity = 0;
                    barRate = 0;
                    barEffect = 0;
                } else if (s >= 255) {
                    barIntensity = 255;
                    barRate = 0;
                    barEffect = 20; // blinder : allume en continu
                } else {
                    barIntensity = 255;
                    barRate = s;
                    barEffect = 0;
                }
                barDuration = STROBE_1CH_DURATION;
            }
            case MODE_ATOMIC_4CH -> {
                readBar();
                plateIntensity = 0;
            }
            case MODE_AURA_8CH -> {
                readBar();
                plateIntensity = ch(4);
                fillPlate(ch(5), ch(6), ch(7));
            }
            case MODE_COMPRESSED_10CH -> {
                readBar();
                plateIntensity = ch(4);
                plateDuration = ch(5);
                plateRate = ch(6);
                fillPlate(ch(7), ch(8), ch(9));
            }
            case MODE_PIXEL_40CH -> {
                readBar();
                for (int i = 0; i < WHITE_SEGMENT_COUNT; i++) {
                    barSegments[i] = ch(4 + i);
                }
                plateIntensity = ch(13);
                plateDuration = ch(14);
                plateRate = ch(15);
                for (int i = 0; i < RGB_ZONE_COUNT * 3; i++) {
                    plateRgb[i] = ch(16 + i);
                }
            }
            case MODE_PIXELMAP_400CH -> {
                readBar();
                for (int i = 0; i < WHITE_SEGMENT_COUNT; i++) {
                    barSegments[i] = ch(4 + i);
                }
                plateIntensity = ch(13);
                plateDuration = ch(14);
                plateRate = ch(15);
                for (int i = 0; i < PIXEL_COUNT * 4; i++) {
                    pixels[i] = ch(16 + i);
                }
            }
            default -> {
                // Historique : zones et segments en niveaux directs, tout allume en continu.
                for (int i = 0; i < RGB_ZONE_COUNT * 3; i++) {
                    plateRgb[i] = ch(i);
                }
                for (int i = 0; i < WHITE_SEGMENT_COUNT; i++) {
                    barSegments[i] = ch(RGB_ZONE_COUNT * 3 + i);
                }
                focus = Math.max(1, ch(RGB_ZONE_COUNT * 3 + WHITE_SEGMENT_COUNT));
                barIntensity = 255;
                barDuration = 0;
                barRate = 0;
                barEffect = 20; // blinder
                plateIntensity = 255;
            }
        }
        if (personality != MODE_LEGACY) {
            focus = 255;
        }
        if (personality == MODE_PIXELMAP_400CH) {
            zonesFromPixels();
        } else {
            pixelsFromZones();
        }
    }

    /** Index d'un pixel : plaque 0 haute / 1 basse, rangee 0 en haut, colonne 0 a gauche. */
    public static int pixelIndex(int plate, int row, int col) {
        return plate * PLATE_PIXELS + row * PIXEL_COLS + col;
    }

    private static int zoneOfPixel(int pixel) {
        int plate = pixel / PLATE_PIXELS;
        int col = (pixel % PLATE_PIXELS) % PIXEL_COLS;
        return plate * 4 + col / (PIXEL_COLS / 4);
    }

    /** Modes par zone : chaque pixel prend la couleur de sa zone, a plein dimmer. */
    private void pixelsFromZones() {
        for (int p = 0; p < PIXEL_COUNT; p++) {
            int zone = zoneOfPixel(p);
            pixels[p * 4] = 255;
            pixels[p * 4 + 1] = plateRgb[zone * 3];
            pixels[p * 4 + 2] = plateRgb[zone * 3 + 1];
            pixels[p * 4 + 3] = plateRgb[zone * 3 + 2];
        }
        for (int zone = 0; zone < RGB_ZONE_COUNT; zone++) {
            zoneBright[zone] = Math.max(plateRgb[zone * 3], Math.max(plateRgb[zone * 3 + 1], plateRgb[zone * 3 + 2]));
        }
    }

    /** Mode pixel map : la zone resume ses pixels, niveau max et couleur moyenne ponderee. */
    private void zonesFromPixels() {
        long[] r = new long[RGB_ZONE_COUNT], g = new long[RGB_ZONE_COUNT], b = new long[RGB_ZONE_COUNT], w = new long[RGB_ZONE_COUNT];
        Arrays.fill(zoneBright, 0);
        for (int p = 0; p < PIXEL_COUNT; p++) {
            int zone = zoneOfPixel(p);
            int dim = pixels[p * 4];
            int bright = Math.max(pixels[p * 4 + 1], Math.max(pixels[p * 4 + 2], pixels[p * 4 + 3])) * dim / 255;
            zoneBright[zone] = Math.max(zoneBright[zone], bright);
            r[zone] += (long) pixels[p * 4 + 1] * bright;
            g[zone] += (long) pixels[p * 4 + 2] * bright;
            b[zone] += (long) pixels[p * 4 + 3] * bright;
            w[zone] += bright;
        }
        for (int zone = 0; zone < RGB_ZONE_COUNT; zone++) {
            plateRgb[zone * 3] = w[zone] > 0 ? (int) (r[zone] / w[zone]) : 0;
            plateRgb[zone * 3 + 1] = w[zone] > 0 ? (int) (g[zone] / w[zone]) : 0;
            plateRgb[zone * 3 + 2] = w[zone] > 0 ? (int) (b[zone] / w[zone]) : 0;
        }
    }

    public int pixelDim(int pixel) {
        return pixels[pixel * 4];
    }

    public int pixelRed(int pixel) {
        return pixels[pixel * 4 + 1];
    }

    public int pixelGreen(int pixel) {
        return pixels[pixel * 4 + 2];
    }

    public int pixelBlue(int pixel) {
        return pixels[pixel * 4 + 3];
    }

    private void readBar() {
        barIntensity = ch(0);
        barDuration = ch(1);
        barRate = ch(2);
        barEffect = ch(3);
    }

    private void fillPlate(int r, int g, int b) {
        for (int zone = 0; zone < RGB_ZONE_COUNT; zone++) {
            plateRgb[zone * 3] = r;
            plateRgb[zone * 3 + 1] = g;
            plateRgb[zone * 3 + 2] = b;
        }
    }

    /**
     * Lumiere de base Theatrical : le pic comme intensite (le niveau instantane vient de
     * {@link #getIntensity()}), et une couleur ponderee de tout ce qui peut s'allumer, le tube
     * blanc pesant double.
     */
    private void aggregate() {
        int peak = 0;
        long rWeighted = 0, gWeighted = 0, bWeighted = 0, totalWeight = 0;
        if (plateIntensity > 0) {
            for (int zone = 0; zone < RGB_ZONE_COUNT; zone++) {
                int r = plateRgb[zone * 3], g = plateRgb[zone * 3 + 1], b = plateRgb[zone * 3 + 2];
                int bright = zoneBright[zone] * plateIntensity / 255;
                peak = Math.max(peak, bright);
                rWeighted += (long) r * bright;
                gWeighted += (long) g * bright;
                bWeighted += (long) b * bright;
                totalWeight += bright;
            }
        }
        if (AtomicStrobeEngine.canLight(barIntensity, barRate, barEffect)) {
            for (int seg : barSegments) {
                int bright = seg * barIntensity / 255;
                peak = Math.max(peak, bright);
                long w = (long) bright * BAR_SEGMENT_WEIGHT;
                rWeighted += 255L * w;
                gWeighted += 255L * w;
                bWeighted += 255L * w;
                totalWeight += w;
            }
        }
        intensity = peak;
        if (totalWeight > 0) {
            red = (int) Math.min(255, rWeighted / totalWeight);
            green = (int) Math.min(255, gWeighted / totalWeight);
            blue = (int) Math.min(255, bWeighted / totalWeight);
        } else {
            red = green = blue = 0;
        }
    }

    // ── Niveaux instantanes ─────────────────────────────────────────────────────

    private long seed() {
        return getBlockPos().asLong();
    }

    private double seconds(float partialTick, double offsetSeconds) {
        long time = level != null ? level.getGameTime() : 0L;
        return (time + partialTick) / 20.0 + offsetSeconds;
    }

    /** Niveau 0..1 d'un segment du tube sur une fenetre de temps (image ou tick). */
    public float barSegmentLevel(int segment, float partialTick, double window) {
        return barSegmentLevel(segment, partialTick, window, 0.0);
    }

    private float barSegmentLevel(int segment, float partialTick, double window, double offset) {
        if (segment < 0 || segment >= WHITE_SEGMENT_COUNT || barSegments[segment] <= 0) {
            return 0f;
        }
        float level = AtomicStrobeEngine.levelOverWindow(barIntensity, barDuration, barRate, barEffect,
                seconds(partialTick, offset), window, seed(), segment, WHITE_SEGMENT_COUNT);
        return level * barSegments[segment] / 255f;
    }

    /** Niveau 0..1 du segment le plus fort du tube. */
    public float barPeakLevel(float partialTick, double window) {
        float best = 0f;
        for (int i = 0; i < WHITE_SEGMENT_COUNT; i++) {
            best = Math.max(best, barSegmentLevel(i, partialTick, window));
        }
        return best;
    }

    /** Niveau 0..1 de la plaque RGB : continue si sa cadence est a 0, sinon elle strobe. */
    public float plateLevel(float partialTick, double window) {
        return plateLevel(partialTick, window, 0.0);
    }

    private float plateLevel(float partialTick, double window, double offset) {
        if (plateIntensity <= 0) {
            return 0f;
        }
        if (plateRate <= 0) {
            return plateIntensity / 255f;
        }
        return AtomicStrobeEngine.levelOverWindow(plateIntensity, plateDuration, plateRate, 0,
                seconds(partialTick, offset), window, seed() ^ 0x5BD1E995L, 0, 1);
    }

    /** Luminosite 0..255 de la zone la plus forte de la plaque, avant strobe. */
    public int platePeakBrightness() {
        int best = 0;
        for (int zone = 0; zone < RGB_ZONE_COUNT; zone++) {
            best = Math.max(best, zoneBrightness(zone));
        }
        return best;
    }

    private int zoneBrightness(int zone) {
        return zoneBright[zone];
    }

    /** Couleur moyenne de la plaque, ponderee par la luminosite des zones ; blanc si tout est noir. */
    public int plateMeanColour() {
        long r = 0, g = 0, b = 0, w = 0;
        for (int zone = 0; zone < RGB_ZONE_COUNT; zone++) {
            int bright = zoneBrightness(zone);
            r += (long) plateRgb[zone * 3] * bright;
            g += (long) plateRgb[zone * 3 + 1] * bright;
            b += (long) plateRgb[zone * 3 + 2] * bright;
            w += bright;
        }
        if (w == 0) {
            return 0xFFFFFF;
        }
        return ((int) (r / w) << 16) | ((int) (g / w) << 8) | (int) (b / w);
    }

    /** Intensite 0..255 instantanee de l'appareil entier, pour la lumiere dynamique. */
    private float liveIntensity(float partialTick, double window, double offset) {
        float bar = 0f;
        for (int i = 0; i < WHITE_SEGMENT_COUNT; i++) {
            bar = Math.max(bar, barSegmentLevel(i, partialTick, window, offset));
        }
        float plate = plateLevel(partialTick, window, offset) * platePeakBrightness() / 255f;
        return Math.max(bar, plate) * 255f;
    }

    @Override
    public float getIntensity() {
        return liveIntensity(0f, AtomicStrobeEngine.TICK_SECONDS, 0.0);
    }

    @Override
    public int getPrevIntensity() {
        return (int) liveIntensity(0f, AtomicStrobeEngine.TICK_SECONDS, -AtomicStrobeEngine.TICK_SECONDS);
    }

    /** Vrai si quelque chose peut s'allumer avec les valeurs courantes. */
    public boolean mayLight() {
        if (plateIntensity > 0 && platePeakBrightness() > 0) {
            return true;
        }
        if (!AtomicStrobeEngine.canLight(barIntensity, barRate, barEffect)) {
            return false;
        }
        for (int seg : barSegments) {
            if (seg > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean needsContinuousClientRender() {
        return mayLight();
    }

    public int getZoneRed(int zone) {
        return zone < 0 || zone >= RGB_ZONE_COUNT ? 0 : plateRgb[zone * 3];
    }

    public int getZoneGreen(int zone) {
        return zone < 0 || zone >= RGB_ZONE_COUNT ? 0 : plateRgb[zone * 3 + 1];
    }

    public int getZoneBlue(int zone) {
        return zone < 0 || zone >= RGB_ZONE_COUNT ? 0 : plateRgb[zone * 3 + 2];
    }

    /** @return 0xRRGGBB de la zone */
    public int getZoneColour(int zone) {
        return (getZoneRed(zone) << 16) | (getZoneGreen(zone) << 8) | getZoneBlue(zone);
    }

    // ── Lumiere ─────────────────────────────────────────────────────────────────

    @Override
    public int getDeviceTypeId() {
        return 0x02;
    }

    @Override
    public String getModelName() {
        return "Atomic Strobe";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.ATOMIC_STROBE.getId();
    }

    @Override
    public int getFocus() {
        return Math.max(1, focus);
    }

    @Override
    public int getLightLuminance() {
        return (int) ((getIntensity() / 255f) * 15f);
    }

    @Override
    public float getLightSpread() {
        return Mth.lerp((Math.max(1, getFocus()) - 1) / 254.0f, MIN_LIGHT_SPREAD, MAX_LIGHT_SPREAD);
    }

    @Override
    public float getMaxLightDistance() {
        return EMISSION_DISTANCE;
    }

    /** Dix-sept emetteurs, un par zone et par segment, positionnes sur les LED de la face. */
    private void ensureZoneLights() {
        if (zoneLights != null) {
            return;
        }
        zoneLights = new AtomicZoneLight[RGB_ZONE_COUNT + WHITE_SEGMENT_COUNT];
        final float faceZ = 11.55f / 16f;
        final float topY = (7.53f + 10.22f) / 2f / 16f;
        final float botY = (3.75f + 6.47f) / 2f / 16f;
        final float barY = (6.59f + 7.44f) / 2f / 16f;
        final float xStart = 1.684f / 16f;
        final float xEnd = 14.34f / 16f;
        final float zoneSpan = (xEnd - xStart) / 4f;
        for (int i = 0; i < 4; i++) {
            final int zone = i;
            float x = xStart + zoneSpan * (i + 0.5f);
            zoneLights[i] = new AtomicZoneLight(this, x, topY, faceZ,
                    () -> (int) (plateLevel(0f, AtomicStrobeEngine.TICK_SECONDS) * zoneBrightness(zone)),
                    () -> getZoneColour(zone));
        }
        for (int i = 0; i < 4; i++) {
            final int zone = 4 + i;
            float x = xStart + zoneSpan * (i + 0.5f);
            zoneLights[4 + i] = new AtomicZoneLight(this, x, botY, faceZ,
                    () -> (int) (plateLevel(0f, AtomicStrobeEngine.TICK_SECONDS) * zoneBrightness(zone)),
                    () -> getZoneColour(zone));
        }
        final float segSpan = (xEnd - xStart) / WHITE_SEGMENT_COUNT;
        for (int i = 0; i < WHITE_SEGMENT_COUNT; i++) {
            final int seg = i;
            float x = xStart + segSpan * (i + 0.5f);
            zoneLights[RGB_ZONE_COUNT + i] = new AtomicZoneLight(this, x, barY, faceZ,
                    () -> (int) (barSegmentLevel(seg, 0f, AtomicStrobeEngine.TICK_SECONDS) * 255f),
                    () -> 0xFFFFFF);
        }
    }

    private void updateZoneLights() {
        Level lvl = getLevel();
        if (lvl == null || !lvl.isClientSide || !LightManager.shouldUpdateDynamicLight()) {
            return;
        }
        ensureZoneLights();
        for (AtomicZoneLight zl : zoneLights) {
            LightManager.updateTracking(zl);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AtomicStrobeBlockEntity be) {
        BaseDMXConsumerLightBlockEntity.tick(level, pos, state, be);
        be.updateZoneLights();
        if (level.isClientSide && be.mayLight()) {
            StrobeRenderHelper.markSectionDirty(pos);
        }
    }

    @Override
    public void setRemoved() {
        if (zoneLights != null) {
            for (AtomicZoneLight zl : zoneLights) {
                zl.setLightEnabled(false);
            }
        }
        super.setRemoved();
    }

    @Override
    public boolean isUpsideDown() {
        return getBlockState().getValue(AtomicStrobeBlock.HANGING)
                && getBlockState().getValue(AtomicStrobeBlock.HANG_DIRECTION) == Direction.UP;
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.atomic_strobe";
    }

    // ── NBT ─────────────────────────────────────────────────────────────────────

    @Override
    public void write(CompoundTag tag) {
        super.write(tag);
        tag.putInt("Personality", personality);
        tag.putByteArray("Dmx", dmx.clone());
    }

    @Override
    public void read(CompoundTag tag) {
        super.read(tag);
        if (tag.contains("Personality")) {
            personality = Mth.clamp(tag.getInt("Personality"), 0, CHANNEL_COUNTS.length - 1);
        }
        if (tag.contains("Dmx")) {
            byte[] stored = tag.getByteArray("Dmx");
            Arrays.fill(dmx, (byte) 0);
            System.arraycopy(stored, 0, dmx, 0, Math.min(stored.length, dmx.length));
        } else if (tag.contains("RgbZones") || tag.contains("WhiteSegments")) {
            // Monde d'avant les modes : zones et segments en tableaux d'entiers du 34 canaux.
            int[] zones = tag.getIntArray("RgbZones");
            int[] segs = tag.getIntArray("WhiteSegments");
            for (int i = 0; i < Math.min(zones.length, RGB_ZONE_COUNT * 3); i++) {
                dmx[i] = (byte) zones[i];
            }
            for (int i = 0; i < Math.min(segs.length, WHITE_SEGMENT_COUNT); i++) {
                dmx[RGB_ZONE_COUNT * 3 + i] = (byte) segs[i];
            }
            dmx[33] = (byte) 255;
        }
        setChannelCount(CHANNEL_COUNTS[personality]);
        decode();
        aggregate();
        if (level != null && level.isClientSide) {
            StrobeRenderHelper.markSectionDirty(getBlockPos());
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("Personality", personality);
        tag.putByteArray("Dmx", dmx.clone());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
