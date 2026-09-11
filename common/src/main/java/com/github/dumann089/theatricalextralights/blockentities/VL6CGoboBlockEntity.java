package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasGobo;
import com.github.dumann089.theatricalextralights.blocks.VL6CGoboBlock;
import com.github.dumann089.theatricalextralights.client.gobo.GoboLibrary;
import com.github.dumann089.theatricalextralights.client.gobo.GoboWheelAnimator;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasFramingShutters;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasPersonality;
import com.github.dumann089.theatricalextralights.fixtures.FramingShutterChannels;
import com.github.dumann089.theatricalextralights.util.FramingShutterState;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import java.util.List;

import java.util.Arrays;

public class VL6CGoboBlockEntity extends ExtraLightsLightBlockEntity
        implements HasGobo, HasPersonality, HasFramingShutters {


    @Override
    public GoboLibrary getGoboLibrary() {
        return GoboLibrary.VL6C;
    }

    private int gobo = 0;
    private int prevGobo = 0;
    private int zoom = 0;
    private int prevZoom = 0;

    private int goboSpin = 0;
    private float goboRotation = 0f;

    public int getGoboSpin() { return goboSpin; }
    public float getGoboRotation() { return goboRotation; }

    public VL6CGoboBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
        setChannelCount(10);
    }

    public VL6CGoboBlockEntity(BlockPos pos, BlockState state) {
        this(BlockEntities.VL6C_GOBO.get(), pos, state);
    }

    public int getGobo() { return gobo; }
    public int getPrevGobo() { return prevGobo; }
    public int getZoom() { return zoom; }
    public int getPrevZoom() { return prevZoom; }
    public float getPartialZoom(float partialTicks) {
        return prevZoom + (zoom - prevZoom) * partialTicks;
    }

    @Override
    public void lightTick() {
        super.lightTick();
        if (this.level != null && this.level.isClientSide) {
            this.prevGobo = this.gobo;
            this.prevZoom = this.zoom;
            framingShutters.tickClient();

            if (goboSpin > 0) {
                float speed = (goboSpin / 255f) * 12f; // máximo 5° por tick
                goboRotation = (goboRotation + speed) % 360f;
            }
        }
    }

    private final GoboWheelAnimator goboAnimator = new GoboWheelAnimator();

    @Override
    public GoboWheelAnimator getGoboAnimator() {
        return this.goboAnimator;
    }

    @Override
    public void consume(byte[] dmxValues) {
        int start = this.getChannelStart() > 0 ? this.getChannelStart() - 1 : 0;
        byte[] ourValues = Arrays.copyOfRange(dmxValues, start, start + this.getChannelCount());

        if (ourValues.length < 10) return;

                        boolean prevAdvanced = beginDmxUpdate();
        int _pi = intensity, _pr = red, _pg = green, _pb = blue, _pf = focus, _pp = pan, _pt = tilt;

        intensity = convertByteToInt(ourValues[0]);
        red = convertByteToInt(ourValues[1]);
        green = convertByteToInt(ourValues[2]);
        blue = convertByteToInt(ourValues[3]);
        focus = convertByteToInt(ourValues[4]);
        pan       = (int) ((convertByteToInt(ourValues[5]) * 360) / 255f) - 180;
        tilt      = (int) ((convertByteToInt(ourValues[6]) * 270) / 255F) - 225;
                int dmxGobo = convertByteToInt(ourValues[7]);

        int slotCount =
                getGoboLibrary().getSlotCount();

        int newGobo = Math.min(
                slotCount - 1,
                (int)((dmxGobo / 255f) * slotCount)
        );

        int newZoom = convertByteToInt(ourValues[8]);
        int newGoboSpin = convertByteToInt(ourValues[9]);

        boolean customChanged = (newGobo != this.gobo || newZoom != this.zoom || newGoboSpin != this.goboSpin);

        if (customChanged) {
            this.gobo = newGobo;
            this.zoom = newZoom;
            this.goboSpin = newGoboSpin;
        }
        boolean changed = intensity != _pi || red != _pr || green != _pg || blue != _pb
                || focus != _pf || pan != _pp || tilt != _pt || customChanged;
        changed |= consumeFramingShutters(ourValues);
        finishDmxUpdate(changed, prevAdvanced);
    }

    @Override
    public void write(CompoundTag compoundTag) {
        super.write(compoundTag);
        compoundTag.putInt("gobo", gobo);
        compoundTag.putInt("zoom", zoom);
        compoundTag.putInt("goboSpin", goboSpin);
        compoundTag.putInt("activePersonality", activePersonalityIndex);
        framingShutters.write(compoundTag);
    }

    @Override
    public void read(CompoundTag compoundTag) {
        super.read(compoundTag);
        this.gobo = compoundTag.getInt("gobo");
        this.zoom = compoundTag.getInt("zoom");
        goboSpin = compoundTag.getInt("goboSpin");
        if (compoundTag.contains("activePersonality")) applyPersonality(compoundTag.getInt("activePersonality"));
        framingShutters.read(compoundTag);
        this.prevGobo = this.gobo;
        this.prevZoom = this.zoom;
    }

    @Override
    public Fixture getFixture() { return Fixtures.VL6C_GOBO.get(); }

    @Override
    public ResourceLocation getFixtureId() { return Fixtures.VL6C_GOBO.getId(); }

    @Override
    public int getDeviceTypeId() { return 0x01; }

    @Override
    public String getModelName() { return "VL6C GOBO"; }

    // ── Personnalite DMX + module de couteaux ────────────────────────────────
    private int activePersonalityIndex = 0;
    private final FramingShutterState framingShutters = new FramingShutterState();

    @Override
    public int getActivePersonality() { return activePersonalityIndex; }

    @Override
    public void setActivePersonality(int index) {
        if (!applyPersonality(index)) return;
        setChanged();
        if (level != null)
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private boolean applyPersonality(int index) {
        List<DMXPersonality> p = getFixture().getDMXPersonalities();
        if (index < 0 || index >= p.size()) return false;
        activePersonalityIndex = index;
        setChannelCount(p.get(index).getChannelCount());
        return true;
    }

    @Override
    public FramingShutterState getFramingShutters() { return framingShutters; }

    /** Canaux 11-19 en mode 19ch ; en mode 10ch les lames restent sorties. */
    private boolean consumeFramingShutters(byte[] ourValues) {
        if (getChannelCount() >= FramingShutterChannels.TOTAL_CHANNELS
                && ourValues.length >= FramingShutterChannels.TOTAL_CHANNELS) {
            return framingShutters.consume(ourValues, FramingShutterChannels.BASE_CHANNELS);
        }
        return framingShutters.reset();
    }

    @Override
    public String getTranslationKey() { return "block.theatricalextralights.vl6c_gobo"; }

    @Override
    public int getBasePan() { return 0; }

    @Override
    public boolean isUpsideDown() {
        return getBlockState().getValue(VL6CGoboBlock.HANGING) && getBlockState().getValue(VL6CGoboBlock.HANG_DIRECTION) == Direction.UP;
    }

    @Override
    public float getPartialIntensity(float partialTicks) {
        return getPrevIntensity() + (getIntensity() - getPrevIntensity()) * partialTicks;
    }

    @Override
    public int getColour() {
        return ((getRed() & 0xFF) << 16) | ((getGreen() & 0xFF) << 8) | (getBlue() & 0xFF);
    }

    @Override
    public float getPartialPanDeg(float partialTicks) {
        return getPrevPan() + (getPan() - getPrevPan()) * partialTicks;
    }

    @Override
    public float getPartialTiltDeg(float partialTicks) {
        return getPrevTilt() + (getTilt() - getPrevTilt()) * partialTicks;
    }

    public int convertByteToInt(byte val) { return Byte.toUnsignedInt(val); }
}