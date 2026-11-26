
package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasGobo;
import com.github.dumann089.theatricalextralights.client.gobo.GoboLibrary;
import com.github.dumann089.theatricalextralights.client.gobo.GoboWheelAnimator;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import com.github.dumann089.theatricalextralights.util.BlockEntitySync;
import com.github.dumann089.theatricalextralights.util.FollowspotDmxHelper;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import dev.imabad.theatrical.blocks.HangableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public class FollowspotBlockEntity extends ExtraLightsLightBlockEntity implements HasGobo {

    private int zoom;
    private int prevZoom;

    private final GoboWheelAnimator goboAnimator = new GoboWheelAnimator();

    public FollowspotBlockEntity(
            BlockEntityType<?> blockEntityType,
            BlockPos blockPos,
            BlockState blockState
    ) {
        super(blockEntityType, blockPos, blockState);
        setChannelCount(7);
    }

    public FollowspotBlockEntity(BlockPos pos, BlockState state) {
        this(BlockEntities.FOLLOWSPOT.get(), pos, state);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.FOLLOWSPOT.get();
    }

    @Override
    public void consume(byte[] dmxValues) {
        int start = this.getChannelStart() > 0
                ? this.getChannelStart() - 1
                : 0;

        byte[] ourValues = Arrays.copyOfRange(
                dmxValues,
                start,
                start + this.getChannelCount()
        );

        if (ourValues.length < 7) {
            return;
        }

        boolean prevAdvanced = beginDmxUpdate();

        int oldIntensity = intensity;
        int oldRed = red;
        int oldGreen = green;
        int oldBlue = blue;
        int oldZoom = zoom;
        int oldPan = pan;
        int oldTilt = tilt;

        prevZoom = zoom;

        intensity = convertByteToInt(ourValues[0]);
        red = convertByteToInt(ourValues[1]);
        green = convertByteToInt(ourValues[2]);
        blue = convertByteToInt(ourValues[3]);

        // Channel 5 is now Zoom.
        zoom = convertByteToInt(ourValues[4]);

        // Retain the legacy field as a compatibility alias for the console.
        focus = zoom;

        pan = FollowspotDmxHelper.dmxByteToPan(
                convertByteToInt(ourValues[5])
        );

        tilt = FollowspotDmxHelper.dmxByteToTilt(
                convertByteToInt(ourValues[6])
        );

        boolean changed = intensity != oldIntensity
                || red != oldRed
                || green != oldGreen
                || blue != oldBlue
                || zoom != oldZoom
                || pan != oldPan
                || tilt != oldTilt;

        finishDmxUpdate(changed, prevAdvanced);
    }

    @Override
    public void applyDirectControl(
            int intensity,
            int red,
            int green,
            int blue,
            int focus,
            int pan,
            int tilt
    ) {
        if (level == null || level.isClientSide) {
            return;
        }

        int nextZoom = Mth.clamp(focus, 0, 255);

        int nextPan = FollowspotDmxHelper.quantizePan(pan);
        int nextTilt = FollowspotDmxHelper.quantizeTilt(tilt);

        boolean changed = this.intensity != intensity
                || this.red != red
                || this.green != green
                || this.blue != blue
                || this.zoom != nextZoom
                || this.pan != nextPan
                || this.tilt != nextTilt;

        if (!changed) {
            return;
        }

        prevZoom = this.zoom;
        zoom = nextZoom;
        this.focus = nextZoom;

        this.intensity = intensity;
        this.red = red;
        this.green = green;
        this.blue = blue;

        this.pan = nextPan;
        this.tilt = nextTilt;

        prevPan = nextPan;
        prevTilt = nextTilt;
        prevFocus = nextZoom;
        prevIntensity = intensity;
        prevRed = red;
        prevGreen = green;
        prevBlue = blue;

        setChanged();
        BlockEntitySync.sendData(this);
    }

    @Override
    public void lightTick() {
        super.lightTick();

        if (level != null && level.isClientSide) {
            prevZoom = zoom;
        }
    }

    @Override
    public int getGobo() {
        return 0;
    }

    @Override
    public int getGoboSpin() {
        return 0;
    }

    @Override
    public GoboLibrary getGoboLibrary() {
        return GoboLibrary.MACVIP;
    }

    @Override
    public float getGoboRotation() {
        return 0.0F;
    }

    @Override
    public GoboWheelAnimator getGoboAnimator() {
        return goboAnimator;
    }

    @Override
    public int getZoom() {
        return zoom;
    }

    @Override
    public float getPartialZoom(float partialTicks) {
        return prevZoom + (zoom - prevZoom) * partialTicks;
    }

    @Override
    public float getPartialIntensity(float partialTicks) {
        return getDisplayIntensity(partialTicks);
    }

    @Override
    public int getPartialColour(float partialTicks) {
        return getDisplayColour(partialTicks);
    }

    @Override
    public float getPartialPanDeg(float partialTicks) {
        return getPrevPan()
                + (getPan() - getPrevPan()) * partialTicks;
    }

    @Override
    public float getPartialTiltDeg(float partialTicks) {
        return getPrevTilt()
                + (getTilt() - getPrevTilt()) * partialTicks;
    }

    @Override
    public int getDeviceTypeId() {
        return 0x01;
    }

    @Override
    public String getModelName() {
        return "Followspot";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.FOLLOWSPOT.getId();
    }

    @Override
    public int getActivePersonality() {
        return 0;
    }

    public int convertByteToInt(byte val) {
        return Byte.toUnsignedInt(val);
    }

    @Override
    public boolean isUpsideDown() {
        return getBlockState().getValue(BaseLightBlock.HANGING)
                && getBlockState().getValue(HangableBlock.HANG_DIRECTION) == Direction.UP;
    }

    @Override
    public int getBasePan() {
        return 0;
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.followspot";
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();

        tag.putInt("FollowspotZoom", zoom);
        tag.putInt("FollowspotPrevZoom", prevZoom);

        return tag;
    }

    @Override
    public void write(CompoundTag tag) {
        super.write(tag);

        tag.putInt("FollowspotZoom", zoom);
        tag.putInt("FollowspotPrevZoom", prevZoom);
    }

    @Override
    public void read(CompoundTag tag) {
        super.read(tag);

        // Migration from the old Followspot Focus field.
        zoom = tag.contains("FollowspotZoom")
                ? Mth.clamp(tag.getInt("FollowspotZoom"), 0, 255)
                : Mth.clamp(focus, 0, 255);

        prevZoom = tag.contains("FollowspotPrevZoom")
                ? Mth.clamp(tag.getInt("FollowspotPrevZoom"), 0, 255)
                : zoom;

        focus = zoom;
    }
}
