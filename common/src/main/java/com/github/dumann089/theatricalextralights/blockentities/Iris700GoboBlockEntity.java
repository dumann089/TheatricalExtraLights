package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasGobo;
import com.github.dumann089.theatricalextralights.blocks.Iris700GoboBlock;
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

import java.util.Arrays;

public class Iris700GoboBlockEntity extends ExtraLightsLightBlockEntity
        implements HasGobo {


    @Override
    public GoboLibrary getGoboLibrary() {
        return GoboLibrary.IRIS700;
    }

    private int gobo = 0;
    private int prevGobo = 0;
    private int zoom = 0;
    private int prevZoom = 0;

    private int goboSpin = 0;
    private float goboRotation = 0f;

    public int getGoboSpin() { return goboSpin; }
    public float getGoboRotation() { return goboRotation; }

    public Iris700GoboBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
        setChannelCount(10);
    }

    public Iris700GoboBlockEntity(BlockPos pos, BlockState state) {
        this(BlockEntities.IRIS_700_GOBO.get(), pos, state);
    }

    private final GoboWheelAnimator goboAnimator = new GoboWheelAnimator();

    @Override
    public GoboWheelAnimator getGoboAnimator() {
        return this.goboAnimator;
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

            if (goboSpin > 0) {
                float speed = (goboSpin / 255f) * 12f; // máximo 5° por tick
                goboRotation = (goboRotation + speed) % 360f;
            }
        }
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

        int slotCount = getGoboLibrary().getSlotCount();

        int newGobo = Math.round((dmxGobo / 255f) * (slotCount - 1));

        newGobo = Math.min(slotCount - 1, Math.max(0, newGobo));

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
        finishDmxUpdate(changed, prevAdvanced);
    }

    @Override
    public void write(CompoundTag compoundTag) {
        super.write(compoundTag);
        compoundTag.putInt("gobo", gobo);
        compoundTag.putInt("zoom", zoom);
        compoundTag.putInt("goboSpin", goboSpin);
    }

    @Override
    public void read(CompoundTag compoundTag) {
        super.read(compoundTag);
        this.gobo = compoundTag.getInt("gobo");
        this.zoom = compoundTag.getInt("zoom");
        goboSpin = compoundTag.getInt("goboSpin");
        this.prevGobo = this.gobo;
        this.prevZoom = this.zoom;
    }

    @Override
    public Fixture getFixture() { return Fixtures.IRIS_700_GOBO.get(); }

    @Override
    public ResourceLocation getFixtureId() { return Fixtures.IRIS_700_GOBO.getId(); }

    @Override
    public int getDeviceTypeId() { return 0x01; }

    @Override
    public String getModelName() { return "IRIS 700 GOBO"; }

    @Override
    public int getActivePersonality() { return 0; }

    @Override
    public String getTranslationKey() { return "block.theatricalextralights.iris_700_gobo"; }

    @Override
    public int getBasePan() { return 0; }

    @Override
    public boolean isUpsideDown() {
        return getBlockState().getValue(Iris700GoboBlock.HANGING) && getBlockState().getValue(Iris700GoboBlock.HANG_DIRECTION) == Direction.UP;
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