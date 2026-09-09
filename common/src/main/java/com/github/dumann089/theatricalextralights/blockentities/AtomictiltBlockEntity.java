package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasPersonality;
import com.github.dumann089.theatricalextralights.blocks.AtomictiltBlock;
import com.github.dumann089.theatricalextralights.client.StrobeRenderHelper;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import com.github.dumann089.theatricalextralights.util.DmxFrameAtomictiltSync;
import com.github.dumann089.theatricalextralights.util.DmxShutterStrobeHelper;
import com.github.dumann089.theatricalextralights.util.DmxStrobeFixture;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.List;

public class AtomictiltBlockEntity extends ExtraLightsLightBlockEntity
        implements HasPersonality, DmxStrobeFixture, DmxFrameAtomictiltSync {
    private static final int RGB_FOCUS_TILT_MODE = 0;
    private static final int RGB_FOCUS_STROBE_TILT_MODE = 1;

    private int activePersonalityIndex = RGB_FOCUS_TILT_MODE;
    private int strobe = 255;
    private int prevStrobe = 255;
    /** Valeur DMX brute du canal tilt (0–255) — sync extended + NBT. */
    private int rawTiltDmx;
    private int prevRawTiltDmx;

    public AtomictiltBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
        syncChannelCountWithPersonality();
    }

    public AtomictiltBlockEntity(BlockPos pos, BlockState state) {
        this(BlockEntities.ATOMICTILT.get(), pos, state);
    }

    private void syncChannelCountWithPersonality() {
        setChannelCount(getPersonalityChannelCount());
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.ATOMICTILT.get();
    }

    @Override
    public int getActivePersonality() {
        return activePersonalityIndex;
    }

    @Override
    public void setActivePersonality(int index) {
        List<DMXPersonality> personalities = getFixture().getDMXPersonalities();
        if (index < 0 || index >= personalities.size()) {
            return;
        }
        activePersonalityIndex = index;
        syncChannelCountWithPersonality();
        if (activePersonalityIndex == RGB_FOCUS_STROBE_TILT_MODE) {
            strobe = 255;
            prevStrobe = 255;
        }
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private int getPersonalityChannelCount() {
        List<DMXPersonality> personalities = getFixture().getDMXPersonalities();
        if (personalities == null || personalities.isEmpty()) {
            return 6;
        }
        return personalities.get(activePersonalityIndex).getChannelCount();
    }

    private boolean usesStrobeChannel() {
        return activePersonalityIndex == RGB_FOCUS_STROBE_TILT_MODE;
    }

    @Override
    public boolean usesStrobeExtras() {
        return usesStrobeChannel();
    }

    private long getGameTimeForStrobe() {
        return level != null ? level.getGameTime() : 0L;
    }

    @Override
    public int getRawDimmer() {
        return intensity;
    }

    @Override
    public int getStrobeChannelValue() {
        return usesStrobeChannel() ? strobe : 255;
    }

    @Override
    public long getStrobeGameTime() {
        return getGameTimeForStrobe();
    }

    @Override
    public float getRenderedIntensity(float partialTick) {
        if (usesStrobeChannel()) {
            return DmxStrobeFixture.super.getRenderedIntensity(partialTick);
        }
        return prevIntensity + (intensity - prevIntensity) * partialTick;
    }

    @Override
    public float getIntensity() {
        if (usesStrobeChannel()) {
            return DmxShutterStrobeHelper.computeEffectiveIntensity(intensity, strobe, getGameTimeForStrobe());
        }
        return intensity;
    }

    @Override
    public int getPrevIntensity() {
        if (usesStrobeChannel()) {
            return (int) DmxShutterStrobeHelper.computeEffectiveIntensity(
                    prevIntensity,
                    prevStrobe,
                    Math.max(0L, getGameTimeForStrobe() - 1)
            );
        }
        return prevIntensity;
    }

    @Override
    public int getFocus() {
        return Math.max(1, focus);
    }

    @Override
    public int getSyncStrobe() {
        return getStrobeChannelValue();
    }

    @Override
    public int getSyncPrevStrobe() {
        return usesStrobeChannel() ? prevStrobe : 255;
    }

    @Override
    public void setSyncStrobe(int value) {
        strobe = value;
    }

    @Override
    public void setSyncPrevStrobe(int value) {
        prevStrobe = value;
    }

    @Override
    public int getSyncTiltDmx() {
        return rawTiltDmx;
    }

    @Override
    public int getSyncPrevTiltDmx() {
        return prevRawTiltDmx;
    }

    @Override
    public void applySyncTiltDmx(int raw, int prevRaw) {
        rawTiltDmx = raw;
        prevRawTiltDmx = prevRaw;
        tilt = mapTiltDmx(raw);
        prevTilt = mapTiltDmx(prevRaw);
    }

    @Override
    public BlockPos getSyncBlockPos() {
        return getBlockPos();
    }

    @Override
    public Level getSyncLevel() {
        return level;
    }

    @Override
    public void applyDmxFrameBase(int intensity, int red, int green, int blue,
                                  int prevIntensity, int prevRed, int prevGreen, int prevBlue) {
        super.applyDmxFrameBase(intensity, red, green, blue, prevIntensity, prevRed, prevGreen, prevBlue);
        markAtomicTiltFrameApplied();
    }

    @Override
    public void applyDmxFramePanTiltFocus(int pan, int tiltValue, int focusValue,
                                          int prevPan, int prevTiltValue, int prevFocusValue) {
        // Keep our mapped tilt. The standard 7ch layout treats ch5 as pan / ch6 as tilt,
        // which is wrong for both Atomic personalities and snaps the head to 0.
        super.applyDmxFramePanTiltFocus(this.pan, this.tilt, focusValue, this.prevPan, this.prevTilt, prevFocusValue);
        markAtomicTiltFrameApplied();
    }

    @Override
    protected boolean needsContinuousClientRender() {
        return intensity > 0
                || tilt != prevTilt
                || rawTiltDmx != prevRawTiltDmx
                || (usesStrobeChannel() && DmxShutterStrobeHelper.isStrobing(strobe));
    }

    /**
     * 6ch omits pan/tilt/focus from the DmxFrame batch; 7ch maps strobe onto the pan slot.
     * Dual batch+NBT was fighting (jitter) and snapping tilt back to 0.
     */
    @Override
    protected boolean hasExtraDmxChannelsBeyondBatch() {
        return true;
    }

    @Override
    public void consume(byte[] dmxValues) {
        int channelCount = getPersonalityChannelCount();
        int start = this.getChannelStart() > 0 ? this.getChannelStart() - 1 : 0;
        byte[] ourValues = Arrays.copyOfRange(dmxValues, start, start + channelCount);
        if (ourValues.length < channelCount) {
            return;
        }
        boolean prevAdvanced = beginDmxUpdate();
        int _pi = intensity, _pr = red, _pg = green, _pb = blue, _pf = focus, _pp = pan, _pt = tilt, _ps = strobe;
        int _rawTilt = rawTiltDmx;

        intensity = convertByteToInt(ourValues[0]);
        red = convertByteToInt(ourValues[1]);
        green = convertByteToInt(ourValues[2]);
        blue = convertByteToInt(ourValues[3]);
        focus = convertByteToInt(ourValues[4]);

        int newRawTilt;
        if (usesStrobeChannel()) {
            strobe = convertByteToInt(ourValues[5]);
            newRawTilt = convertByteToInt(ourValues[6]);
        } else {
            newRawTilt = convertByteToInt(ourValues[5]);
        }

        prevRawTiltDmx = rawTiltDmx;
        rawTiltDmx = newRawTilt;
        tilt = mapTiltDmx(rawTiltDmx);

        boolean changed = intensity != _pi || red != _pr || green != _pg || blue != _pb
                || focus != _pf || pan != _pp || tilt != _pt || strobe != _ps || rawTiltDmx != _rawTilt;
        finishDmxUpdate(changed, prevAdvanced);
    }

    public static int mapTiltDmx(int dmx) {
        return (int) ((dmx * 270) / 255F) - 225;
    }

    @Override
    public void lightTick() {
        super.lightTick();
        if (level != null && level.isClientSide && usesStrobeChannel()) {
            prevStrobe = strobe;
            if (shouldForceStrobeRepaint()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
                StrobeRenderHelper.markSectionDirty(getBlockPos());
            }
        }
    }

    @Override
    public void read(CompoundTag tag) {
        int oldTilt = tilt;
        super.read(tag);
        if (tag.contains("activePersonality")) {
            activePersonalityIndex = tag.getInt("activePersonality");
        }
        if (tag.contains("strobe")) {
            strobe = tag.getInt("strobe");
            prevStrobe = strobe;
        }
        if (tag.contains("rawTiltDmx")) {
            rawTiltDmx = tag.getInt("rawTiltDmx");
            prevRawTiltDmx = tag.contains("prevRawTiltDmx") ? tag.getInt("prevRawTiltDmx") : rawTiltDmx;
            tilt = mapTiltDmx(rawTiltDmx);
            prevTilt = mapTiltDmx(prevRawTiltDmx);
        }
        syncChannelCountWithPersonality();
        if (level != null && level.isClientSide && tilt != oldTilt) {
            StrobeRenderHelper.markSectionDirty(getBlockPos());
        }
    }

    @Override
    public void write(CompoundTag tag) {
        super.write(tag);
        tag.putInt("rawTiltDmx", rawTiltDmx);
        tag.putInt("prevRawTiltDmx", prevRawTiltDmx);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AtomictiltBlockEntity be) {
        ExtraLightsLightBlockEntity.tick(level, pos, state, be);
    }

    @Override
    public int getDeviceTypeId() {
        return 0x01;
    }

    @Override
    public String getModelName() {
        return "Atomic Tilt";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.ATOMICTILT.getId();
    }

    public int convertByteToInt(byte val) {
        return Byte.toUnsignedInt(val);
    }

    @Override
    public boolean isUpsideDown() {
        return getBlockState().getValue(AtomictiltBlock.HANGING)
                && getBlockState().getValue(AtomictiltBlock.HANG_DIRECTION) == Direction.UP;
    }

    @Override
    public int getBasePan() {
        return 0;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("activePersonality", activePersonalityIndex);
        tag.putInt("strobe", strobe);
        tag.putInt("rawTiltDmx", rawTiltDmx);
        tag.putInt("prevRawTiltDmx", prevRawTiltDmx);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("activePersonality", activePersonalityIndex);
        tag.putInt("strobe", strobe);
        tag.putInt("rawTiltDmx", rawTiltDmx);
        tag.putInt("prevRawTiltDmx", prevRawTiltDmx);
        return tag;
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.atomictilt";
    }
}
