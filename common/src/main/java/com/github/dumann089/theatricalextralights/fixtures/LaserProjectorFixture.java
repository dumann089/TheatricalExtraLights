package com.github.dumann089.theatricalextralights.fixtures;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.api.HangType;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.List;

/**
 * Ether Dream laser projector. Reuses the existing laser meshes; it is not a DMX fixture.
 */
public class LaserProjectorFixture extends Fixture {

    private static final List<DMXPersonality> PERSONALITIES = Collections.singletonList(
            new DMXPersonality(0, "Ether Dream")
    );

    private static final ResourceLocation TILT_MODEL =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/laser/laser_tilt");
    private static final ResourceLocation PAN_MODEL =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/laser/laser_pan");
    private static final ResourceLocation STATIC_MODEL =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/laser/laser_static");

    private final float[] tiltRotation = new float[]{0.5F, 0.53F, 0.0F};
    private final float[] panRotation = new float[]{0.5F, 0.53F, 0.0F};

    @Override
    public ResourceLocation getTiltModel() {
        return TILT_MODEL;
    }

    @Override
    public ResourceLocation getPanModel() {
        return PAN_MODEL;
    }

    @Override
    public ResourceLocation getStaticModel() {
        return STATIC_MODEL;
    }

    @Override
    public float[] getTiltRotationPosition() {
        return tiltRotation;
    }

    @Override
    public float[] getPanRotationPosition() {
        return panRotation;
    }

    @Override
    public float[] getBeamStartPosition() {
        return new float[]{0.5F, 0.53F, 0.093f};
    }

    @Override
    public float getDefaultRotation() {
        return 90;
    }

    @Override
    public float getBeamWidth() {
        return 0.00f;
    }

    @Override
    public float getRayTraceRotation() {
        return 0;
    }

    @Override
    public HangType getHangType() {
        return HangType.BRACE_BAR;
    }

    @Override
    public float[] getTransforms(BlockState fixtureBlockState, BlockState supportBlockState) {
        if (fixtureBlockState.getValue(BaseLightBlock.HANG_DIRECTION) == Direction.UP) {
            return new float[]{0, .5f, 0};
        }
        return new float[]{0, -0.35F, 0};
    }

    @Override
    public List<DMXPersonality> getDMXPersonalities() {
        return PERSONALITIES;
    }

    @Override
    public double getLightRadius() {
        return 0.0;
    }
}
