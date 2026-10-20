package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blocks.MovingbarBlock;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Barre LED mobile (pan/tilt) a huit pixels ; voir {@link PixelBarBlockEntity}.
 *
 * <p>Mode classique 7 canaux : dimmer, RGB, focus (ignore), pan, tilt. Mode pixel 34 canaux :
 * pan, tilt, puis dimmer + RGB par pixel.
 */
public class MovingbarBlockEntity extends PixelBarBlockEntity {

    public static final int PIXEL_COUNT = 8;
    /** Canaux d'en-tete du mode pixel : pan, tilt. */
    public static final int PIXEL_HEADER_CHANNELS = 2;

    public MovingbarBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState, PIXEL_COUNT);
    }

    public MovingbarBlockEntity(BlockPos pos, BlockState state) {
        this(BlockEntities.MOVING_BAR.get(), pos, state);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.MOVING_BAR.get();
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
        return "Movingbar";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.MOVING_BAR.getId();
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
        return "block.theatricalextralights.moving_bar";
    }
}
