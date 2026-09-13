package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasPersonality;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.github.dumann089.theatricalextralights.util.BlockEntitySync;

import java.util.Arrays;
import java.util.List;

public abstract class BaseExtraLightsBlockEntity extends ExtraLightsLightBlockEntity implements HasPersonality {

    private int activePersonalityIndex = 0;

    protected BaseExtraLightsBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setChannelCount(getPersonalityChannelCount());
    }

    // ─── HasPersonality ───────────────────────────────────────────────────────

    @Override
    public int getActivePersonality() {
        return activePersonalityIndex;
    }

    @Override
    public void setActivePersonality(int index) {
        List<DMXPersonality> personalities = getFixture().getDMXPersonalities();
        if (index < 0 || index >= personalities.size()) return;
        activePersonalityIndex = index;
        setChannelCount(getPersonalityChannelCount());
        setChanged();
        if (level != null) {
            BlockEntitySync.sendData(this);
        }
    }

    protected int getPersonalityChannelCount() {
        List<DMXPersonality> p = getFixture().getDMXPersonalities();
        if (p == null || p.isEmpty()) return 7;
        return p.get(activePersonalityIndex).getChannelCount();
    }

    // ─── NBT ─────────────────────────────────────────────────────────────────

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("activePersonality", activePersonalityIndex);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("activePersonality")) {
            activePersonalityIndex = tag.getInt("activePersonality");
            setChannelCount(getPersonalityChannelCount());
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("activePersonality", activePersonalityIndex);
        return tag;
    }

    // ─── Consume DMX ─────────────────────────────────────────────────────────

    @Override
    public void consume(byte[] dmxValues) {
        int channelCount = getPersonalityChannelCount();
        int start = this.getChannelStart() > 0 ? this.getChannelStart() - 1 : 0;
        byte[] ourValues = Arrays.copyOfRange(dmxValues, start, start + channelCount);

        if (ourValues.length < 7) return;

        boolean prevAdvanced = beginDmxUpdate();
        int _pi = intensity, _pr = red, _pg = green, _pb = blue, _pf = focus, _pp = pan, _pt = tilt;

        intensity = convertByteToInt(ourValues[0]);
        red       = convertByteToInt(ourValues[1]);
        green     = convertByteToInt(ourValues[2]);
        blue      = convertByteToInt(ourValues[3]);
        focus     = convertByteToInt(ourValues[4]);
        pan       = (int) ((convertByteToInt(ourValues[5]) * 360) / 255f) - 180;
        tilt      = (int) ((convertByteToInt(ourValues[6]) * 270) / 255F) - 225;

        boolean changed = intensity != _pi || red != _pr || green != _pg || blue != _pb
                || focus != _pf || pan != _pp || tilt != _pt;

        if (ourValues.length > 7) {
            changed |= consumeExtendedChannels(ourValues, channelCount);
        }

        finishDmxUpdate(changed, prevAdvanced);
    }

    /**
     *
     *
     *
     *
     *
     *
     * @param values
     * @param totalCount
     */
    protected boolean consumeExtendedChannels(byte[] values, int totalCount) {
        return false;
    }

    public int convertByteToInt(byte val) {
        return Byte.toUnsignedInt(val);
    }
}