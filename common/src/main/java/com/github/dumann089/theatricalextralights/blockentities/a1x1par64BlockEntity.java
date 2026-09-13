package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasPersonality;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.github.dumann089.theatricalextralights.util.BlockEntitySync;

import java.util.List;

public class a1x1par64BlockEntity extends ExtraLightsLightBlockEntity implements HasPersonality {

    private int activePersonalityIndex = 0;

    private static final int[][] COLORS = new int[][]{
            {255, 0, 0}, // RED
            {0, 255, 0}, // GREEN
            {0, 0, 255}, // BLUE
            {255, 255, 0}, // YELLOW
            {255, 115, 0}, // ORANGE
            {128, 0, 255}, // PURPLE
            {255, 0, 200}, // MAGENTA
            {0, 128, 255}, // LIGHTBLUE
            {255, 255, 255} // WHITE
    };

    public a1x1par64BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setChannelCount(1);
        applyColor();
    }

    public a1x1par64BlockEntity(BlockPos pos, BlockState state) {
        this(BlockEntities.A1X1PAR64.get(), pos, state);
    }

    @Override
    public int getFocus() {
        return 255;
    }

    // ─── PERSONALITY ──────────────────────────────────────────────────────────
    @Override
    public int getActivePersonality() {
        return activePersonalityIndex;
    }

    @Override
    public void setActivePersonality(int index) {
        List<DMXPersonality> p = getFixture().getDMXPersonalities();
        if (index < 0 || index >= p.size()) return;

        activePersonalityIndex = index;
        setChannelCount(p.get(index).getChannelCount());

        if (p.get(index).getChannelCount() < 4) {
            applyColor();
        }

        setChanged();
        if (level != null)
            BlockEntitySync.sendData(this);
    }

    // ─── COLOR ────────────────────────────────────────────────────────────────
    private void applyColor() {
        if (activePersonalityIndex >= COLORS.length) return;

        int[] c = COLORS[activePersonalityIndex];
        red = c[0];
        green = c[1];
        blue = c[2];
    }

    public void nextColor() {
        setActivePersonality((activePersonalityIndex + 1) % COLORS.length);
    }

    public void prevColor() {
        int prev = activePersonalityIndex - 1;
        if (prev < 0) prev = COLORS.length - 1;
        setActivePersonality(prev);
    }

    @Override
    public void consume(byte[] dmxValues) {
        int start = this.getChannelStart() > 0 ? this.getChannelStart() - 1 : 0;
        int channelCount = getFixture().getDMXPersonalities().get(activePersonalityIndex).getChannelCount();

        if (dmxValues.length < start + channelCount) return;

        boolean prevAdvanced = beginDmxUpdate();
        int _pi = intensity, _pr = red, _pg = green, _pb = blue, _pf = focus, _pp = pan, _pt = tilt;

        if (channelCount >= 4) {
            intensity = convertByteToInt(dmxValues[start]);
            red       = convertByteToInt(dmxValues[start + 1]);
            green     = convertByteToInt(dmxValues[start + 2]);
            blue      = convertByteToInt(dmxValues[start + 3]);
        } else {
            int newIntensity = convertByteToInt(dmxValues[start]);
            if (newIntensity == intensity) return;
            intensity = newIntensity;
        }

        finishDmxUpdate(intensity != _pi || red != _pr || green != _pg || blue != _pb || focus != _pf || pan != _pp || tilt != _pt, prevAdvanced);
    }

    // ─── NBT ──────────────────────────────────────────────────────────────────
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
        }

        applyColor();
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("activePersonality", activePersonalityIndex);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    // ─── FIXTURE INFO ─────────────────────────────────────────────────────────
    @Override
    public Fixture getFixture() {
        return Fixtures.A1X1PAR64.get();
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.A1X1PAR64.getId();
    }

    @Override
    public int getDeviceTypeId() {
        return 0x01;
    }

    @Override
    public String getModelName() {
        return "A1x1par64";
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.a1x1par64";
    }

    public int convertByteToInt(byte val) {
        return Byte.toUnsignedInt(val);
    }
}