package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasPersonality;
import com.github.dumann089.theatricalextralights.blocks.MovingMiniBarBlock;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.github.dumann089.theatricalextralights.util.BlockEntitySync;

import java.util.Arrays;
import java.util.List;

public class MovingMiniBarBlockEntity extends ExtraLightsLightBlockEntity implements HasPersonality {

    public static final int BEAM_COUNT = 7;
    public static final int UNITE_MODE = 0;
    public static final int ALONE_MODE = 1;

    private int activePersonalityIndex = UNITE_MODE;

    private final int[] beamIntensity = new int[BEAM_COUNT];
    private final int[] beamRed = new int[BEAM_COUNT];
    private final int[] beamGreen = new int[BEAM_COUNT];
    private final int[] beamBlue = new int[BEAM_COUNT];
    private final int[] beamTilt = new int[BEAM_COUNT];

    private final int[] prevBeamIntensity = new int[BEAM_COUNT];
    private final int[] prevBeamRed = new int[BEAM_COUNT];
    private final int[] prevBeamGreen = new int[BEAM_COUNT];
    private final int[] prevBeamBlue = new int[BEAM_COUNT];
    private final int[] prevBeamTilt = new int[BEAM_COUNT];

    public MovingMiniBarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setChannelCount(5);
    }

    public MovingMiniBarBlockEntity(BlockPos pos, BlockState state) {
        this(BlockEntities.MOVING_MINI_BAR.get(), pos, state);
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
        setChannelCount(personalities.get(index).getChannelCount());
        if (activePersonalityIndex == UNITE_MODE) {
            syncBeamsToGlobalState();
        }
        setChanged();
        if (level != null) {
            BlockEntitySync.sendData(this);
        }
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.MOVING_MINI_BAR.get();
    }

    @Override
    public int getFocus() {
        return 255;
    }

    @Override
    public void consume(byte[] dmxValues) {
        int channelCount = getFixture().getDMXPersonalities().get(activePersonalityIndex).getChannelCount();
        int start = this.getChannelStart() > 0 ? this.getChannelStart() - 1 : 0;
        if (start >= dmxValues.length) {
            return;
        }
        int end = Math.min(dmxValues.length, start + channelCount);
        byte[] ourValues = Arrays.copyOfRange(dmxValues, start, end);
        if (ourValues.length < channelCount) {
            return;
        }

        copyBeamState(prevBeamIntensity, beamIntensity);
        copyBeamState(prevBeamRed, beamRed);
        copyBeamState(prevBeamGreen, beamGreen);
        copyBeamState(prevBeamBlue, beamBlue);
        copyBeamState(prevBeamTilt, beamTilt);

                boolean prevAdvanced = beginDmxUpdate();
        int _pi = intensity, _pr = red, _pg = green, _pb = blue, _pf = focus, _pp = pan, _pt = tilt;
        pan = 0;
        focus = 255;

        if (activePersonalityIndex == UNITE_MODE) {
            intensity = convertByteToInt(ourValues[0]);
            tilt = convertTilt(ourValues[1]);
            red = convertByteToInt(ourValues[2]);
            green = convertByteToInt(ourValues[3]);
            blue = convertByteToInt(ourValues[4]);
            syncBeamsToGlobalState();
        } else {
            int maxIntensity = 0;
            int dominantIndex = 0;
            for (int i = 0; i < BEAM_COUNT; i++) {
                int baseIndex = i * 5;
                beamIntensity[i] = convertByteToInt(ourValues[baseIndex]);
                beamTilt[i] = convertTilt(ourValues[baseIndex + 1]);
                beamRed[i] = convertByteToInt(ourValues[baseIndex + 2]);
                beamGreen[i] = convertByteToInt(ourValues[baseIndex + 3]);
                beamBlue[i] = convertByteToInt(ourValues[baseIndex + 4]);

                if (beamIntensity[i] >= maxIntensity) {
                    maxIntensity = beamIntensity[i];
                    dominantIndex = i;
                }
            }
            intensity = maxIntensity;
            red = beamRed[dominantIndex];
            green = beamGreen[dominantIndex];
            blue = beamBlue[dominantIndex];
            tilt = beamTilt[BEAM_COUNT / 2];
        }

        if (level != null) {
            BlockEntitySync.sendData(this);
        }
        setChanged();
    }

    public int getBeamIntensity(int index) {
        return beamIntensity[index];
    }

    public int getPrevBeamIntensity(int index) {
        return prevBeamIntensity[index];
    }

    public int getBeamRed(int index) {
        return beamRed[index];
    }

    public int getPrevBeamRed(int index) {
        return prevBeamRed[index];
    }

    public int getBeamGreen(int index) {
        return beamGreen[index];
    }

    public int getPrevBeamGreen(int index) {
        return prevBeamGreen[index];
    }

    public int getBeamBlue(int index) {
        return beamBlue[index];
    }

    public int getPrevBeamBlue(int index) {
        return prevBeamBlue[index];
    }

    public float getPartialBeamTilt(int index, float partialTicks) {
        return prevBeamTilt[index] + (beamTilt[index] - prevBeamTilt[index]) * partialTicks;
    }

    public float getPartialBeamIntensity(int index, float partialTicks) {
        return prevBeamIntensity[index] + (beamIntensity[index] - prevBeamIntensity[index]) * partialTicks;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("activePersonality", activePersonalityIndex);
        tag.putIntArray("beamIntensity", beamIntensity);
        tag.putIntArray("beamRed", beamRed);
        tag.putIntArray("beamGreen", beamGreen);
        tag.putIntArray("beamBlue", beamBlue);
        tag.putIntArray("beamTilt", beamTilt);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("activePersonality")) {
            setActivePersonality(tag.getInt("activePersonality"));
        }
        loadArray(tag, "beamIntensity", beamIntensity);
        loadArray(tag, "beamRed", beamRed);
        loadArray(tag, "beamGreen", beamGreen);
        loadArray(tag, "beamBlue", beamBlue);
        loadArray(tag, "beamTilt", beamTilt);
        copyBeamState(prevBeamIntensity, beamIntensity);
        copyBeamState(prevBeamRed, beamRed);
        copyBeamState(prevBeamGreen, beamGreen);
        copyBeamState(prevBeamBlue, beamBlue);
        copyBeamState(prevBeamTilt, beamTilt);
        if (activePersonalityIndex == UNITE_MODE) {
            syncGlobalStateFromBeam(0);
        } else {
            int dominantIndex = getDominantBeamIndex();
            intensity = beamIntensity[dominantIndex];
            red = beamRed[dominantIndex];
            green = beamGreen[dominantIndex];
            blue = beamBlue[dominantIndex];
            tilt = beamTilt[BEAM_COUNT / 2];
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("activePersonality", activePersonalityIndex);
        tag.putIntArray("beamIntensity", beamIntensity);
        tag.putIntArray("beamRed", beamRed);
        tag.putIntArray("beamGreen", beamGreen);
        tag.putIntArray("beamBlue", beamBlue);
        tag.putIntArray("beamTilt", beamTilt);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.MOVING_MINI_BAR.getId();
    }

    @Override
    public int getDeviceTypeId() {
        return 0x01;
    }

    @Override
    public String getModelName() {
        return "Moving Mini Bar";
    }

    @Override
    public int getBasePan() {
        return 0;
    }

    @Override
    public boolean isUpsideDown() {
        return getBlockState().getValue(MovingMiniBarBlock.HANGING)
                && getBlockState().getValue(MovingMiniBarBlock.HANG_DIRECTION) == Direction.UP;
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.moving_mini_bar";
    }

    private void syncBeamsToGlobalState() {
        for (int i = 0; i < BEAM_COUNT; i++) {
            beamIntensity[i] = intensity;
            beamRed[i] = red;
            beamGreen[i] = green;
            beamBlue[i] = blue;
            beamTilt[i] = tilt;
        }
    }

    private void syncGlobalStateFromBeam(int index) {
        intensity = beamIntensity[index];
        red = beamRed[index];
        green = beamGreen[index];
        blue = beamBlue[index];
        tilt = beamTilt[index];
    }

    private int getDominantBeamIndex() {
        int dominantIndex = 0;
        int dominantIntensity = -1;
        for (int i = 0; i < BEAM_COUNT; i++) {
            if (beamIntensity[i] >= dominantIntensity) {
                dominantIntensity = beamIntensity[i];
                dominantIndex = i;
            }
        }
        return dominantIndex;
    }

    private static void copyBeamState(int[] target, int[] source) {
        System.arraycopy(source, 0, target, 0, BEAM_COUNT);
    }

    private static void loadArray(CompoundTag tag, String key, int[] target) {
        int[] source = tag.getIntArray(key);
        if (source.length == BEAM_COUNT) {
            System.arraycopy(source, 0, target, 0, BEAM_COUNT);
        }
    }

    private static int convertTilt(byte value) {
        return (int) ((Byte.toUnsignedInt(value) * 270) / 255f) - 225;
    }

    public int convertByteToInt(byte value) {
        return Byte.toUnsignedInt(value);
    }
}
