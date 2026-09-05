package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasPersonality;
import com.github.dumann089.theatricalextralights.blocks.StrobeBlock;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import com.github.dumann089.theatricalextralights.util.DmxFrameStrobeSync;
import com.github.dumann089.theatricalextralights.util.DmxShutterStrobeHelper;
import com.github.dumann089.theatricalextralights.util.DmxStrobeFixture;
import com.github.dumann089.theatricalextralights.client.StrobeRenderHelper;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
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
import java.util.List;

public class StrobeBlockEntity extends ExtraLightsLightBlockEntity implements HasPersonality, DmxStrobeFixture, DmxFrameStrobeSync {
    private static final int LEGACY_4CH_MODE = 0;
    private static final int FOCUS_5CH_MODE = 1;
    private static final int FOCUS_STROBE_6CH_MODE = 2;
    private static final int RGB_ONLY_3CH_MODE = 3;
    /** Dimmer fixe pour le mode RGB only (100 %). */
    private static final int FIXED_RGB_ONLY_INTENSITY = 255;
    /** Focus fixe pour le mode RGB only (faisceau serré type strobe). */
    private static final int FIXED_RGB_ONLY_FOCUS = 255;
    private static final int OPEN = 255;
    /** Tache au sol (rayon Shimmer) : focus DMX 1 ≈ 3 blocs, focus 255 ≈ 20 blocs. */
    private static final float MIN_LIGHT_SPREAD = 3.0f;
    private static final float MAX_LIGHT_SPREAD = 20.0f;
    private static final float CLOSE_EMISSION_DISTANCE = 0.75f;
    private static final float FAR_EMISSION_DISTANCE = 7.5f;

    private int activePersonalityIndex = FOCUS_STROBE_6CH_MODE;
    /** Canal strobe DMX (personnalité 6 canaux). */
    private int strobe = 255;
    private int prevStrobe = 255;

    public StrobeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.STROBE.get(), pos, state);
        setChannelCount(getPersonalityChannelCount());
        focus = 1;
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.STROBE.get();
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
        if (activePersonalityIndex == LEGACY_4CH_MODE) {
            focus = 255;
        } else if (activePersonalityIndex == RGB_ONLY_3CH_MODE) {
            intensity = FIXED_RGB_ONLY_INTENSITY;
            focus = FIXED_RGB_ONLY_FOCUS;
            strobe = OPEN;
            prevStrobe = OPEN;
        } else {
            focus = Math.max(1, focus);
        }
        if (activePersonalityIndex == FOCUS_STROBE_6CH_MODE) {
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
            return 4;
        }
        return personalities.get(activePersonalityIndex).getChannelCount();
    }

    private boolean usesStrobeChannel() {
        return activePersonalityIndex == FOCUS_STROBE_6CH_MODE;
    }

    private boolean isRgbOnlyMode() {
        return activePersonalityIndex == RGB_ONLY_3CH_MODE;
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
        return usesStrobeChannel() ? strobe : OPEN;
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
        if (activePersonalityIndex == LEGACY_4CH_MODE) {
            return 255;
        }
        if (isRgbOnlyMode()) {
            return FIXED_RGB_ONLY_FOCUS;
        }
        return Math.max(1, focus);
    }

    @Override
    public int getLightLuminance() {
        return (int) ((getIntensity() / 255f) * 15f);
    }

    @Override
    public boolean shouldTrace() {
        if (!emitsLight()) {
            return false;
        }
        // Raycast tant que le dimmer est ouvert — le focus met à jour l'impact même si le shutter coupe la lumière.
        return intensity > 0;
    }

    @Override
    public float getLightSpread() {
        if (activePersonalityIndex == LEGACY_4CH_MODE) {
            return MAX_LIGHT_SPREAD;
        }
        return Mth.lerp(getNormalizedFocus(), MIN_LIGHT_SPREAD, MAX_LIGHT_SPREAD);
    }

    @Override
    public float getMaxLightDistance() {
        if (activePersonalityIndex == LEGACY_4CH_MODE) {
            return 50.0f;
        }

        return Mth.lerp(
                getNormalizedFocus(),
                CLOSE_EMISSION_DISTANCE,
                FAR_EMISSION_DISTANCE
        );
    }

    private float getNormalizedFocus() {
        return (Math.max(1, getFocus()) - 1) / 254.0f;
    }

    @Override
    public int getSyncStrobe() {
        return getStrobeChannelValue();
    }

    @Override
    public int getSyncPrevStrobe() {
        return usesStrobeChannel() ? prevStrobe : OPEN;
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
    protected boolean needsContinuousClientRender() {
        return intensity > 0
                || focus != prevFocus
                || red != prevRed || green != prevGreen || blue != prevBlue
                || (usesStrobeChannel() && DmxShutterStrobeHelper.isStrobing(strobe));
    }

    @Override
    public void applyDmxFramePanTiltFocus(int pan, int tilt, int focusValue,
                                          int prevPan, int prevTilt, int prevFocusValue) {
        super.applyDmxFramePanTiltFocus(pan, tilt, focusValue, prevPan, prevTilt, prevFocusValue);
        markStrobeFrameApplied();
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
        int _pi = intensity, _pr = red, _pg = green, _pb = blue, _pf = focus, _ps = strobe;

        if (isRgbOnlyMode()) {
            red = convertByteToInt(ourValues[0]);
            green = convertByteToInt(ourValues[1]);
            blue = convertByteToInt(ourValues[2]);
            intensity = FIXED_RGB_ONLY_INTENSITY;
            focus = FIXED_RGB_ONLY_FOCUS;
            strobe = OPEN;
        } else {
            intensity = convertByteToInt(ourValues[0]);
            red = convertByteToInt(ourValues[1]);
            green = convertByteToInt(ourValues[2]);
            blue = convertByteToInt(ourValues[3]);
            if (channelCount >= 5) {
                focus = Math.max(1, convertByteToInt(ourValues[4]));
            } else {
                focus = 128;
            }
            if (channelCount >= 6) {
                strobe = convertByteToInt(ourValues[5]);
            } else if (usesStrobeChannel()) {
                strobe = 255;
            }
        }

        boolean changed = intensity != _pi || red != _pr || green != _pg || blue != _pb
                || focus != _pf || strobe != _ps;
        finishDmxUpdate(changed, prevAdvanced);
    }

    @Override
    public void lightTick() {
        super.lightTick();
        if (level != null && level.isClientSide && usesStrobeChannel()) {
            prevStrobe = strobe;
            if (shouldForceStrobeRepaint()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
                markStrobeSectionDirty();
            }
        }
    }

    private void markStrobeSectionDirty() {
        StrobeRenderHelper.markSectionDirty(getBlockPos());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StrobeBlockEntity be) {
        ExtraLightsLightBlockEntity.tick(level, pos, state, be);
    }

    @Override
    public int getDeviceTypeId() {
        return 0x02;
    }

    @Override
    public String getModelName() {
        return "Strobe";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.STROBE.getId();
    }

    @Override
    public boolean isUpsideDown() {
        return getBlockState().getValue(StrobeBlock.HANGING) && getBlockState().getValue(StrobeBlock.HANG_DIRECTION) == Direction.UP;
    }

    public int convertByteToInt(byte val) {
        return Byte.toUnsignedInt(val);
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
            setActivePersonality(tag.getInt("activePersonality"));
        } else {
            setActivePersonality(FOCUS_STROBE_6CH_MODE);
        }
        if (tag.contains("strobe")) {
            strobe = tag.getInt("strobe");
            prevStrobe = strobe;
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("activePersonality", activePersonalityIndex);
        tag.putInt("strobe", strobe);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.strobe";
    }
}
