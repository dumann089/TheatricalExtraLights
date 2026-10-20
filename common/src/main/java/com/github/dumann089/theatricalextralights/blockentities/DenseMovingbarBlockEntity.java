package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blocks.MovingbarBlock;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Barre LED mobile dense : 24 pixels, un par LED ; voir {@link PixelBarBlockEntity}.
 *
 * <p>Mode classique 7 canaux : dimmer, RGB, focus (ignore), pan, tilt. Mode pixel 98 canaux :
 * pan, tilt, puis dimmer + RGB par pixel.
 */
public class DenseMovingbarBlockEntity extends PixelBarBlockEntity {

    public static final int PIXEL_COUNT = 24;
    /** Canaux d'en-tete du mode pixel : pan, tilt. */
    public static final int PIXEL_HEADER_CHANNELS = 2;

    public DenseMovingbarBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.DENSE_MOVING_BAR.get(), pos, state, PIXEL_COUNT);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.DENSE_MOVING_BAR.get();
    }

    @Override
    protected int classicChannelCount() {
        return 7;
    }

    @Override
    protected void consumeClassic(byte[] values) {
        intensity = convertByteToInt(values[0]);
        red = convertByteToInt(values[1]);
        green = convertByteToInt(values[2]);
        blue = convertByteToInt(values[3]);
        pan = panFromDmx(values[5]);
        tilt = tiltFromDmx(values[6]);
    }

    @Override
    protected int pixelHeaderChannelCount() {
        return PIXEL_HEADER_CHANNELS;
    }

    @Override
    protected void consumePixelHeader(byte[] values) {
        pan = panFromDmx(values[0]);
        tilt = tiltFromDmx(values[1]);
    }

    private int panFromDmx(byte value) {
        return (int) ((convertByteToInt(value) * 360) / 255f) - 180;
    }

    private int tiltFromDmx(byte value) {
        return (int) ((convertByteToInt(value) * 270) / 255F) - 225;
    }

    @Override
    public int getDeviceTypeId() {
        return 0x01;
    }

    @Override
    public String getModelName() {
        return "Dense Movingbar";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.DENSE_MOVING_BAR.getId();
    }

    @Override
    public boolean isUpsideDown() {
        return getBlockState().getValue(MovingbarBlock.HANGING) && getBlockState().getValue(MovingbarBlock.HANG_DIRECTION) == Direction.UP;
    }

    @Override
    public int getBasePan() {
        return 0;
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.dense_moving_bar";
    }
}
