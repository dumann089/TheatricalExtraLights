package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasPersonality;
import com.github.dumann089.theatricalextralights.util.DmxShutterStrobeHelper;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.List;

public abstract class BlinderBaseBlockEntity extends ExtraLightsLightBlockEntity implements HasPersonality {

    private int activePersonalityIndex = 0;

    /** Valeur DMX du canal strobe (canal 5). */
    protected int strobe = 255;

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
    public Vector3f getLightPos() {
        BlockPos emission = getEmissionBlock();
        if (emission != null) {
            return Vec3.atCenterOf(emission).toVector3f();
        }
        return Vec3.atCenterOf(getBlockPos()).toVector3f();
    }

    @Override
    public int getPrevIntensity() {
        return (int) DmxShutterStrobeHelper.computeEffectiveIntensity(
                prevIntensity,
                prevFocus,
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
    public void consume(byte[] dmxValues) {
        int channelCount = getPersonalityChannelCount();
        int start = getChannelStart() > 0 ? getChannelStart() - 1 : 0;
        byte[] ourValues = Arrays.copyOfRange(dmxValues, start, start + channelCount);
        if (ourValues.length < channelCount) {
            return;
        }

        boolean prevAdvanced = beginDmxUpdate();
        int _pi = intensity, _pr = red, _pg = green, _pb = blue, _ps = strobe;

        intensity = convertByteToInt(ourValues[0]);
        red = convertByteToInt(ourValues[1]);
        green = convertByteToInt(ourValues[2]);
        blue = convertByteToInt(ourValues[3]);
        strobe = convertByteToInt(ourValues[4]);
        focus = strobe;

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
        if (level != null && level.isClientSide && DmxShutterStrobeHelper.isStrobing(strobe)) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlinderBaseBlockEntity be) {
        ExtraLightsLightBlockEntity.tick(level, pos, state, be);
    }

    public int convertByteToInt(byte val) {
        return Byte.toUnsignedInt(val);
    }
}
