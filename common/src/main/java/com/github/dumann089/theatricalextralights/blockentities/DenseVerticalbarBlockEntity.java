package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Barre LED verticale dense : 46 pixels du bas vers le haut, un par LED ; voir {@link PixelBarBlockEntity}. */
public class DenseVerticalbarBlockEntity extends PixelBarBlockEntity {

    public static final int PIXEL_COUNT = 46;

    public DenseVerticalbarBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.DENSE_VERTICAL_BAR.get(), pos, state, PIXEL_COUNT);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.DENSE_VERTICAL_BAR.get();
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
        return "Dense Vertical RGB Bar";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.DENSE_VERTICAL_BAR.getId();
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.dense_vertical_bar";
    }
}
