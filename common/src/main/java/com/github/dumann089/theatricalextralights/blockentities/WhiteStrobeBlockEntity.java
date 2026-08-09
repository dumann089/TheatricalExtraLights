package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blocks.WhiteStrobeBlock;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import com.github.dumann089.theatricalextralights.util.DmxShutterStrobeHelper;
import com.github.dumann089.theatricalextralights.util.DmxStrobeFixture;
import com.github.dumann089.theatricalextralights.client.StrobeRenderHelper;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public class WhiteStrobeBlockEntity extends ExtraLightsLightBlockEntity implements DmxStrobeFixture {

    public WhiteStrobeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.WHITE_STROBE.get(), pos, state);
        setChannelCount(1);
    }

    private long getStrobeGameTimeInternal() {
        return level != null ? level.getGameTime() : 0L;
    }

    @Override
    public int getRawDimmer() {
        return 255;
    }

    @Override
    public int getStrobeChannelValue() {
        return intensity;
    }

    @Override
    public long getStrobeGameTime() {
        return getStrobeGameTimeInternal();
    }

    @Override
    public float getIntensity() {
        return DmxShutterStrobeHelper.computeEffectiveIntensity(255, intensity, getStrobeGameTimeInternal());
    }

    @Override
    public int getPrevIntensity() {
        return (int) DmxShutterStrobeHelper.computeEffectiveIntensity(
                255, prevIntensity, Math.max(0L, getStrobeGameTimeInternal() - 1));
    }

    @Override
    public void lightTick() {
        super.lightTick();
        if (level != null && level.isClientSide && shouldForceStrobeRepaint()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            StrobeRenderHelper.markSectionDirty(getBlockPos());
        }
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.WHITE_STROBE.get();
    }

    @Override
    public int getFocus() {
        return 255;
    }

    @Override
    public void consume(byte[] dmxValues) {
        int start = this.getChannelStart() > 0 ? this.getChannelStart() - 1 : 0;
        byte[] ourValues = Arrays.copyOfRange(dmxValues, start,
                start+ this.getChannelCount());
        if(ourValues.length < 1){
            return;
        }
                boolean prevAdvanced = beginDmxUpdate();
        int _pi = intensity, _pr = red, _pg = green, _pb = blue, _pf = focus, _pp = pan, _pt = tilt;
        intensity = convertByteToInt(ourValues[0]);
        red = convertByteToInt(ourValues[0]);
        green = convertByteToInt(ourValues[0]);
        blue = convertByteToInt(ourValues[0]);
        finishDmxUpdate(intensity != _pi || red != _pr || green != _pg || blue != _pb || focus != _pf || pan != _pp || tilt != _pt, prevAdvanced);
    }

    @Override
    public int getDeviceTypeId() {
        return 0x02;
    }

    @Override
    public String getModelName() {
        return "White Strobe";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.WHITE_STROBE.getId();
    }

    @Override
    public boolean isUpsideDown() {
        return getBlockState().getValue(WhiteStrobeBlock.HANGING) && getBlockState().getValue(WhiteStrobeBlock.HANG_DIRECTION) == Direction.UP;
    }

    @Override
    public int getActivePersonality() {
        return 0;
    }

    public int convertByteToInt(byte val) {
        return Byte.toUnsignedInt(val);
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.white_strobe";
    }
}