package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasPersonality;
import com.github.dumann089.theatricalextralights.util.DmxFrameStrobeSync;
import com.github.dumann089.theatricalextralights.util.DmxShutterStrobeHelper;
import com.github.dumann089.theatricalextralights.util.DmxStrobeFixture;
import com.github.dumann089.theatricalextralights.client.StrobeRenderHelper;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public abstract class BlinderBaseBlockEntity extends ExtraLightsLightBlockEntity implements HasPersonality, DmxStrobeFixture, DmxFrameStrobeSync {

    private int activePersonalityIndex = 0;

    /** Valeur DMX du canal strobe (canal 5). */
    protected int strobe = 255;
    protected int prevStrobe = 255;

    protected BlinderBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setChannelCount(getPersonalityChannelCount());
    }

    protected int getPersonalityChannelCount() {
        List<DMXPersonality> personalities = getFixture().getDMXPersonalities();
        if (personalities == null || personalities.isEmpty()) {
            return 4;
        }
        return personalities.get(activePersonalityIndex).getChannelCount();
    }

    protected long getGameTimeForStrobe() {
        return level != null ? level.getGameTime() : 0L;
    }

    @Override
    public float getIntensity() {
        return DmxShutterStrobeHelper.computeEffectiveIntensity(intensity, strobe, getGameTimeForStrobe());
    }

    @Override
    public int getLightLuminance() {
        float effective = getIntensity();
        return (int) ((effective / 255f) * 15f);
    }

    @Override
    public boolean shouldTrace() {
        return emitsLight() && intensity > 0;
    }

    @Override
    public Vector3f getLightPos() {
        BlockPos emission = getEmissionBlock();
        if (emission != null) {
            return Vec3.atCenterOf(emission).toVector3f();
        }
        return Vec3.atCenterOf(getBlockPos()).toVector3f();
    }

    @Override
    public int getRawDimmer() {
        return intensity;
    }

    @Override
    public int getStrobeChannelValue() {
        return strobe;
    }

    @Override
    public long getStrobeGameTime() {
        return getGameTimeForStrobe();
    }

    @Override
    public float getRenderedIntensity(float partialTick) {
        return DmxStrobeFixture.super.getRenderedIntensity(partialTick);
    }

    @Override
    public int getPrevIntensity() {
        return (int) DmxShutterStrobeHelper.computeEffectiveIntensity(
                prevIntensity,
                prevStrobe,
                Math.max(0L, getGameTimeForStrobe() - 1)
        );
    }

    @Override
    public int getFocus() {
        return 255;
    }

    @Override
    public float getLightSpread() {
        return (float) getFixture().getLightRadius();
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
        setChannelCount(getPersonalityChannelCount());
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public int getSyncStrobe() {
        return strobe;
    }

    @Override
    public int getSyncPrevStrobe() {
        return prevStrobe;
    }

    @Override
    public void setSyncStrobe(int value) {
        strobe = value;
        focus = value;
    }

    @Override
    public void setSyncPrevStrobe(int value) {
        prevStrobe = value;
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
        markStrobeFrameApplied();
    }

    @Override
    public void applyDmxFramePanTiltFocus(int pan, int tilt, int focusValue,
                                          int prevPan, int prevTilt, int prevFocusValue) {
        super.applyDmxFramePanTiltFocus(pan, tilt, focusValue, prevPan, prevTilt, prevFocusValue);
        markStrobeFrameApplied();
    }

    @Override
    public void consume(byte[] dmxValues) {
        if (dmxValues == null) {
            return;
        }
        int start = getChannelStart() > 0 ? getChannelStart() - 1 : 0;
        if (start < 0 || start + 4 > dmxValues.length) {
            return;
        }

        boolean prevAdvanced = beginDmxUpdate();
        int _pi = intensity, _pr = red, _pg = green, _pb = blue, _ps = strobe;

        intensity = convertByteToInt(dmxValues[start]);
        red = convertByteToInt(dmxValues[start + 1]);
        green = convertByteToInt(dmxValues[start + 2]);
        blue = convertByteToInt(dmxValues[start + 3]);
        // Personality is 4ch iRGB. Strobe is only present on a 5ch footprint.
        if (start + 5 <= dmxValues.length && getPersonalityChannelCount() > 4) {
            strobe = convertByteToInt(dmxValues[start + 4]);
            focus = strobe;
        }

        boolean changed = intensity != _pi || red != _pr || green != _pg || blue != _pb || strobe != _ps;
        finishDmxUpdate(changed, prevAdvanced);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("activePersonality", activePersonalityIndex);
        tag.putInt("strobe", strobe);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("activePersonality")) {
            activePersonalityIndex = tag.getInt("activePersonality");
            setChannelCount(getPersonalityChannelCount());
        }
        if (tag.contains("strobe")) {
            strobe = tag.getInt("strobe");
        } else if (tag.contains("shutter")) {
            strobe = tag.getInt("shutter");
        } else {
            strobe = focus;
        }
        focus = strobe;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("activePersonality", activePersonalityIndex);
        tag.putInt("strobe", strobe);
        return tag;
    }

    @Override
    public void lightTick() {
        super.lightTick();
        if (level != null && level.isClientSide) {
            prevStrobe = strobe;
            if (shouldForceStrobeRepaint()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
                StrobeRenderHelper.markSectionDirty(getBlockPos());
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlinderBaseBlockEntity be) {
        ExtraLightsLightBlockEntity.tick(level, pos, state, be);
    }

    public int convertByteToInt(byte val) {
        return Byte.toUnsignedInt(val);
    }
}
