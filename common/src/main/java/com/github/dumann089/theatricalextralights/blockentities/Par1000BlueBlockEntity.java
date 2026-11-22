package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blocks.Par1000BlueBlock;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public class Par1000BlueBlockEntity extends Par1000BlockEntity {

    public Par1000BlueBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.PAR1000_BLUE.get(), pos, state);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.PAR1000_BLUE.get();
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
        blue = intensity;

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
    public String getModelName() {
        return "Par 1000 Blue";
    }

    @Override
    public boolean isUpsideDown() {
        return getBlockState().getValue(Par1000BlueBlock.HANGING)
                && getBlockState().getValue(Par1000BlueBlock.HANG_DIRECTION) == Direction.UP;
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.PAR1000_BLUE.getId();
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.par1000_blue";
    }
}
