package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blocks.RGBbarBlock;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Barre LED horizontale a neuf pixels ; voir {@link PixelBarBlockEntity}. */
public class RGBBarBlockEntity extends PixelBarBlockEntity {

    public static final int PIXEL_COUNT = 9;

    public RGBBarBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.RGB_BAR.get(), pos, state, PIXEL_COUNT);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.RGB_BAR.get();
    }

    @Override
    protected int classicChannelCount() {
        return 4;
    }

    @Override
    protected void consumeClassic(byte[] values) {
        intensity = convertByteToInt(values[0]);
        red = convertByteToInt(values[1]);
        green = convertByteToInt(values[2]);
        blue = convertByteToInt(values[3]);
    }

    @Override
    public int getDeviceTypeId() {
        return 0x02;
    }

    @Override
    public String getModelName() {
        return "RGB Bar";
    }

    @Override
    public boolean isUpsideDown() {
        return getBlockState().getValue(RGBbarBlock.HANGING) && getBlockState().getValue(RGBbarBlock.HANG_DIRECTION) == Direction.UP;
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.RGB_BAR.getId();
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.rgb_bar";
    }
}
