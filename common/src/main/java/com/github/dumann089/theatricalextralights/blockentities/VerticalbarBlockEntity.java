package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Barre LED verticale a neuf pixels, du bas vers le haut ; voir {@link PixelBarBlockEntity}. */
public class VerticalbarBlockEntity extends PixelBarBlockEntity {

    public static final int PIXEL_COUNT = 9;

    public VerticalbarBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.VERTICAL_BAR.get(), pos, state, PIXEL_COUNT);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.VERTICAL_BAR.get();
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
        return "Vertical RGB Bar";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.VERTICAL_BAR.getId();
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.vertical_bar";
    }
}
