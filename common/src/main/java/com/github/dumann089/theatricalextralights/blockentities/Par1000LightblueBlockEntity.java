package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blocks.Par1000LightblueBlock;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public class Par1000LightblueBlockEntity extends Par1000BlockEntity {

    public Par1000LightblueBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.PAR1000_LIGHTBLUE.get(), pos, state);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.PAR1000_LIGHTBLUE.get();
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
        green = (int) (intensity * 0.65F);

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
        return "Par 1000 Light blue";
    }

    @Override
    public boolean isUpsideDown() {
        return getBlockState().getValue(Par1000LightblueBlock.HANGING)
                && getBlockState().getValue(Par1000LightblueBlock.HANG_DIRECTION) == Direction.UP;
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.PAR1000_LIGHTBLUE.getId();
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.par1000_lightblue";
    }
}
