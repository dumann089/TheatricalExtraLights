package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blocks.Par1000Block;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public class Par1000BlockEntity extends ManualParGoboBlockEntity {

    public Par1000BlockEntity(BlockPos pos, BlockState state) {
        this(BlockEntities.PAR1000.get(), pos, state);
    }

    protected Par1000BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setChannelCount(1);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.PAR1000.get();
    }

    @Override
    public int getFocus() {
        return 200;
    }

    @Override
    public void consume(byte[] dmxValues) {
        int start = getChannelStart() > 0 ? getChannelStart() - 1 : 0;
        byte[] ourValues = Arrays.copyOfRange(
                dmxValues,
                start,
                Math.min(start + getChannelCount(), dmxValues.length)
        );
        if (ourValues.length < 1) {
            return;
        }

        boolean prevAdvanced = beginDmxUpdate();
        int oldIntensity = intensity;
        int oldRed = red;
        int oldGreen = green;
        int oldBlue = blue;
        int oldFocus = focus;
        int oldPan = pan;
        int oldTilt = tilt;

        intensity = Byte.toUnsignedInt(ourValues[0]);
        float scale = intensity / 255.0F;
        red = (int) (255.0F * scale);
        green = (int) (219.0F * scale);
        blue = (int) (122.0F * scale);

        boolean changed = intensity != oldIntensity
                || red != oldRed
                || green != oldGreen
                || blue != oldBlue
                || focus != oldFocus
                || pan != oldPan
                || tilt != oldTilt;

        finishDmxUpdate(changed, prevAdvanced);
    }

    @Override
    public int getDeviceTypeId() {
        return 0x02;
    }

    @Override
    public String getModelName() {
        return "Par 1000";
    }

    @Override
    public boolean isUpsideDown() {
        return getBlockState().getValue(Par1000Block.HANGING)
                && getBlockState().getValue(Par1000Block.HANG_DIRECTION) == Direction.UP;
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.PAR1000.getId();
    }

    @Override
    public int getActivePersonality() {
        return 0;
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.par1000";
    }
}
