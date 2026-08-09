package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blocks.AtomicStrobeBlock;
import com.github.dumann089.theatricalextralights.client.StrobeRenderHelper;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * Atomic-style strobe with 34 DMX channels:
 *   1-24  → 8 RGB zones (4 top + 4 bottom) at 3 channels each
 *   25-33 → 9 individually-addressable white LED bar segments (intensity 0-255)
 *   34    → focus (controls light spread / emission distance, mirrors Strobe)
 */
public class AtomicStrobeBlockEntity extends ExtraLightsLightBlockEntity {

    public static final int CHANNEL_COUNT = 34;
    public static final int RGB_ZONE_COUNT = 8;
    public static final int WHITE_SEGMENT_COUNT = 9;

    // Mirror Strobe's focus → spread/distance mapping so both fixtures behave
    // consistently for the same focus value.
    private static final float MIN_LIGHT_SPREAD = 0.35f;
    private static final float CLOSE_EMISSION_DISTANCE = 0.75f;
    private static final float FAR_EMISSION_DISTANCE = 7.5f;
    private static final float MIN_LUMINANCE_SCALE = 0.22f;

    // Each bar segment contributes this multiple of an RGB zone's weight to
    // the emission mix — makes the bar visibly dominate over the colour cells.
    private static final int BAR_SEGMENT_WEIGHT = 2;

    /** Flat array: [zone0.r, zone0.g, zone0.b, zone1.r, zone1.g, zone1.b, ...]. */
    private final int[] rgbZones = new int[RGB_ZONE_COUNT * 3];
    /** Intensity 0-255 per LED segment along the bar. */
    private final int[] whiteSegments = new int[WHITE_SEGMENT_COUNT];
    /** Per-zone client-side dynamic light emitters, so each zone illuminates the
     *  world from its own position. Lazily initialised on first client tick. */
    private AtomicZoneLight[] zoneLights;

    public AtomicStrobeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.ATOMIC_STROBE.get(), pos, state);
        setChannelCount(CHANNEL_COUNT);
        focus = 1;
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.ATOMIC_STROBE.get();
    }

    @Override
    public void consume(byte[] dmxValues) {
        int start = this.getChannelStart() > 0 ? this.getChannelStart() - 1 : 0;
        byte[] v = Arrays.copyOfRange(dmxValues, start, start + CHANNEL_COUNT);
        if (v.length < CHANNEL_COUNT) {
            return;
        }
                boolean prevAdvanced = beginDmxUpdate();
        int _pi = intensity, _pr = red, _pg = green, _pb = blue, _pf = focus, _pp = pan, _pt = tilt;
        int[] prevZones = rgbZones.clone();
        int[] prevSegments = whiteSegments.clone();
        // 1-24 → 8 RGB zones
        for (int i = 0; i < RGB_ZONE_COUNT * 3; i++) {
            rgbZones[i] = u(v[i]);
        }
        // 25-33 → 9 white segments
        for (int i = 0; i < WHITE_SEGMENT_COUNT; i++) {
            whiteSegments[i] = u(v[RGB_ZONE_COUNT * 3 + i]);
        }
        // 34 → focus
        focus = Math.max(1, u(v[RGB_ZONE_COUNT * 3 + WHITE_SEGMENT_COUNT]));

        // Aggregate emission: every RGB zone and every bar segment is treated
        // as its own emitter that contributes to the single dynamic light.
        //  - intensity = brightness of the strongest individual emitter, so a
        //    single zone (or single segment) at 255 still reads as a full-power
        //    strobe (matches the modified Strobe at intensity 255).
        //  - red/green/blue = weighted-average colour of every lit emitter;
        //    each bar segment counts as BAR_SEGMENT_WEIGHT× an RGB zone so the
        //    white bar visibly dominates the tint.
        int peak = 0;
        long rWeighted = 0, gWeighted = 0, bWeighted = 0;
        long totalWeight = 0;
        for (int i = 0; i < RGB_ZONE_COUNT; i++) {
            int r = rgbZones[i * 3];
            int g = rgbZones[i * 3 + 1];
            int b = rgbZones[i * 3 + 2];
            int zoneBright = Math.max(r, Math.max(g, b));
            if (zoneBright > peak) peak = zoneBright;
            rWeighted += (long) r * zoneBright;
            gWeighted += (long) g * zoneBright;
            bWeighted += (long) b * zoneBright;
            totalWeight += zoneBright;
        }
        for (int seg : whiteSegments) {
            if (seg > peak) peak = seg;
            int w = seg * BAR_SEGMENT_WEIGHT;
            rWeighted += 255L * w;
            gWeighted += 255L * w;
            bWeighted += 255L * w;
            totalWeight += w;
        }
        intensity = peak;
        if (totalWeight > 0) {
            red   = (int) Math.min(255, rWeighted / totalWeight);
            green = (int) Math.min(255, gWeighted / totalWeight);
            blue  = (int) Math.min(255, bWeighted / totalWeight);
        } else {
            red = green = blue = 0;
        }
        boolean zonesChanged = !Arrays.equals(rgbZones, prevZones) || !Arrays.equals(whiteSegments, prevSegments);
        boolean changed = intensity != _pi || red != _pr || green != _pg || blue != _pb || focus != _pf
                || pan != _pp || tilt != _pt || zonesChanged;
        finishDmxUpdate(changed, prevAdvanced);
    }

    @Override
    protected boolean needsContinuousClientRender() {
        if (super.needsContinuousClientRender()) {
            return true;
        }
        for (int zone : rgbZones) {
            if (zone > 0) {
                return true;
            }
        }
        for (int segment : whiteSegments) {
            if (segment > 0) {
                return true;
            }
        }
        return false;
    }

    /** @return packed 0xRRGGBB for the zone (0–{@link #RGB_ZONE_COUNT}-1). */
    public int getZoneColour(int zoneIdx) {
        if (zoneIdx < 0 || zoneIdx >= RGB_ZONE_COUNT) return 0;
        int base = zoneIdx * 3;
        return (rgbZones[base] << 16) | (rgbZones[base + 1] << 8) | rgbZones[base + 2];
    }

    public int getZoneRed(int zoneIdx) {
        return (zoneIdx < 0 || zoneIdx >= RGB_ZONE_COUNT) ? 0 : rgbZones[zoneIdx * 3];
    }
    public int getZoneGreen(int zoneIdx) {
        return (zoneIdx < 0 || zoneIdx >= RGB_ZONE_COUNT) ? 0 : rgbZones[zoneIdx * 3 + 1];
    }
    public int getZoneBlue(int zoneIdx) {
        return (zoneIdx < 0 || zoneIdx >= RGB_ZONE_COUNT) ? 0 : rgbZones[zoneIdx * 3 + 2];
    }

    /** @return intensity 0-255 of the white LED segment (0–{@link #WHITE_SEGMENT_COUNT}-1). */
    public int getWhiteSegment(int segIdx) {
        if (segIdx < 0 || segIdx >= WHITE_SEGMENT_COUNT) return 0;
        return whiteSegments[segIdx];
    }

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
    public int getActivePersonality() {
        return 0;
    }

    @Override
    public int getFocus() {
        return Math.max(1, focus);
    }

    @Override
    public int getLightLuminance() {
        float scale = Mth.lerp(getNormalizedFocus(), MIN_LUMINANCE_SCALE, 1.0f);
        float effective = getIntensity();
        return (int) ((effective / 255f) * scale * 15f);
    }

    @Override
    public float getLightSpread() {
        float maxLightSpread = (float) getFixture().getLightRadius();
        return Mth.lerp(getNormalizedFocus(), MIN_LIGHT_SPREAD, maxLightSpread);
    }

    @Override
    public float getMaxLightDistance() {
        return Mth.lerp(getNormalizedFocus(), CLOSE_EMISSION_DISTANCE, FAR_EMISSION_DISTANCE);
    }

    private float getNormalizedFocus() {
        return (Math.max(1, getFocus()) - 1) / 254.0f;
    }

    /** Build the 17 per-zone dynamic-light emitters lazily on the client. */
    private void ensureZoneLights() {
        if (zoneLights != null) return;
        // Match the emitter positions to the actual LED pixel area in the
        // texture (the 24px outer plastic frame on the 512x256 sheet is
        // excluded), so each light originates from inside an LED rather than
        // straddling the dark plastic frame.
        zoneLights = new AtomicZoneLight[RGB_ZONE_COUNT + WHITE_SEGMENT_COUNT];
        final float faceZ = 11.2f / 16f;
        final float topY = (5.05f + 6.65f) / 2f / 16f;
        final float botY = (2.40f + 3.95f) / 2f / 16f;
        final float barY = (4.10f + 4.95f) / 2f / 16f;
        final float xStart = 2.85f / 16f;
        final float xEnd   = 13.25f / 16f;
        final float zoneSpan = (xEnd - xStart) / 4f;
        // Top 4 RGB zones (idx 0..3)
        for (int i = 0; i < 4; i++) {
            final int zoneIdx = i;
            float x = xStart + zoneSpan * (i + 0.5f);
            zoneLights[i] = new AtomicZoneLight(this, x, topY, faceZ,
                    () -> Math.max(getZoneRed(zoneIdx), Math.max(getZoneGreen(zoneIdx), getZoneBlue(zoneIdx))),
                    () -> getZoneColour(zoneIdx));
        }
        // Bottom 4 RGB zones (idx 4..7)
        for (int i = 0; i < 4; i++) {
            final int zoneIdx = 4 + i;
            float x = xStart + zoneSpan * (i + 0.5f);
            zoneLights[4 + i] = new AtomicZoneLight(this, x, botY, faceZ,
                    () -> Math.max(getZoneRed(zoneIdx), Math.max(getZoneGreen(zoneIdx), getZoneBlue(zoneIdx))),
                    () -> getZoneColour(zoneIdx));
        }
        // 9 bar segments — each emits white when its segment is lit
        final float segSpan = (xEnd - xStart) / WHITE_SEGMENT_COUNT;
        for (int i = 0; i < WHITE_SEGMENT_COUNT; i++) {
            final int segIdx = i;
            float x = xStart + segSpan * (i + 0.5f);
            zoneLights[RGB_ZONE_COUNT + i] = new AtomicZoneLight(this, x, barY, faceZ,
                    () -> getWhiteSegment(segIdx),
                    () -> 0xFFFFFF);
        }
    }

    /** Sync each zone-light's enabled state with the LightManager.
     *  Called every tick on the client. */
    private void updateZoneLights() {
        Level lvl = getLevel();
        if (lvl == null || !lvl.isClientSide) return;
        if (!LightManager.shouldUpdateDynamicLight()) return;
        ensureZoneLights();
        for (AtomicZoneLight zl : zoneLights) {
            LightManager.updateTracking(zl);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AtomicStrobeBlockEntity be) {
        BaseDMXConsumerLightBlockEntity.tick(level, pos, state, be);
        be.updateZoneLights();
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

    private static int u(byte b) {
        return Byte.toUnsignedInt(b);
    }

    @Override
    public void write(CompoundTag tag) {
        super.write(tag);
        tag.putIntArray("RgbZones", rgbZones);
        tag.putIntArray("WhiteSegments", whiteSegments);
    }

    @Override
    public void read(CompoundTag tag) {
        super.read(tag);
        setChannelCount(CHANNEL_COUNT);
        int[] zones = tag.getIntArray("RgbZones");
        if (zones.length == rgbZones.length) {
            System.arraycopy(zones, 0, rgbZones, 0, rgbZones.length);
        }
        int[] segs = tag.getIntArray("WhiteSegments");
        if (segs.length == whiteSegments.length) {
            System.arraycopy(segs, 0, whiteSegments, 0, whiteSegments.length);
        }
        if (level != null && level.isClientSide) {
            StrobeRenderHelper.markSectionDirty(getBlockPos());
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
