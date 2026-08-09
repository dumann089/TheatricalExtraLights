package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasExtendedBeamChannels;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasPersonality;
import com.github.dumann089.theatricalextralights.blocks.MovingVL2CBlock;
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

import java.util.Arrays;
import java.util.List;

public class SearchlightBlockEntity extends ExtraLightsLightBlockEntity implements HasPersonality, HasExtendedBeamChannels {

    private int activePersonalityIndex = 0;

    @Override
    public int getActivePersonality() { return activePersonalityIndex; }

    @Override
    public void setActivePersonality(int index) {
        List<DMXPersonality> p = getFixture().getDMXPersonalities();
        if (index < 0 || index >= p.size()) return;
        activePersonalityIndex = index;
        setChannelCount(p.get(index).getChannelCount());
        setChanged();
        if (level != null)
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private int gobo = 0;
    private int prevGobo = 0;
    private int zoom = 0;
    private int prevZoom = 0;
    private int goboSpin = 0;
    private float goboRotation = 0f;

    public int getGobo()           { return gobo; }
    public int getPrevGobo()       { return prevGobo; }
    public int getZoom()           { return zoom; }
    public int getPrevZoom()       { return prevZoom; }
    public int getGoboSpin()       { return goboSpin; }
    public float getGoboRotation() { return goboRotation; }
    public float getPartialZoom(float partialTicks) {
        return prevZoom + (zoom - prevZoom) * partialTicks;
    }

    public SearchlightBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setChannelCount(7);
    }

    public SearchlightBlockEntity(BlockPos pos, BlockState state) {
        this(BlockEntities.SEARCHLIGHT.get(), pos, state);
    }

    // ─── Tick ─────────────────────────────────────────────────────────────────
    @Override
    public void lightTick() {
        super.lightTick();
        if (this.level != null && this.level.isClientSide) {
            prevGobo = gobo;
            prevZoom = zoom;
            if (goboSpin > 0) {
                float speed = (goboSpin / 255f) * 12f;
                goboRotation = (goboRotation + speed) % 360f;
            }
        }
    }

    // ─── Consume DMX ─────────────────────────────────────────────────────────
    @Override
    public void consume(byte[] dmxValues) {
        int channelCount = getFixture().getDMXPersonalities().get(activePersonalityIndex).getChannelCount();
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

                        if (channelCount >= 10 && ourValues.length >= 10) {
            int newGobo     = convertByteToInt(ourValues[7]);
            int newZoom     = convertByteToInt(ourValues[8]);
            int newGoboSpin = convertByteToInt(ourValues[9]);
            if (newGobo != gobo || newZoom != zoom || newGoboSpin != goboSpin) {
                gobo = newGobo; zoom = newZoom; goboSpin = newGoboSpin; changed = true;
            }
        }

        finishDmxUpdate(changed, prevAdvanced);
    }

    // ─── NBT ──────────────────────────────────────────────────────────────────
    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("activePersonality", activePersonalityIndex);
        tag.putInt("gobo",     gobo);
        tag.putInt("zoom",     zoom);
        tag.putInt("goboSpin", goboSpin);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("activePersonality"))
            setActivePersonality(tag.getInt("activePersonality"));
        gobo     = tag.getInt("gobo");
        zoom     = tag.getInt("zoom");
        goboSpin = tag.getInt("goboSpin");
        prevGobo = gobo;
        prevZoom = zoom;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("activePersonality", activePersonalityIndex);
        tag.putInt("gobo",     gobo);
        tag.putInt("zoom",     zoom);
        tag.putInt("goboSpin", goboSpin);
        return tag;
    }
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public Fixture getFixture()           { return Fixtures.SEARCHLIGHT.get(); }
    @Override public ResourceLocation getFixtureId(){ return Fixtures.SEARCHLIGHT.getId(); }
    @Override public int getDeviceTypeId()          { return 0x01; }
    @Override public String getModelName()          { return "Searchlight"; }
    @Override public int getBasePan()               { return 0; }
    @Override public String getTranslationKey()     { return "block.theatricalextralights.Searchlight"; }

    public int convertByteToInt(byte val) { return Byte.toUnsignedInt(val); }

    @Override
    public boolean isUpsideDown() {
        return getBlockState().getValue(MovingVL2CBlock.HANGING)
                && getBlockState().getValue(MovingVL2CBlock.HANG_DIRECTION) == Direction.UP;
    }
}