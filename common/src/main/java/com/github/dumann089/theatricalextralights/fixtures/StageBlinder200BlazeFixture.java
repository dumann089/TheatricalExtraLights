package com.github.dumann089.theatricalextralights.fixtures;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.api.HangType;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import dev.imabad.theatrical.fixtures.SharedSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.List;

public class StageBlinder200BlazeFixture extends Fixture {

    private static final List<DMXPersonality> PERSONALITIES = Collections.singletonList(
            new DMXPersonality(4, "4-Channel Dimmer RGB")
                    .addSlot(SharedSlots.INTENSITY)
                    .addSlot(SharedSlots.RED)
                    .addSlot(SharedSlots.GREEN)
                    .addSlot(SharedSlots.BLUE)
    );

    private static final ResourceLocation TILT_MODEL = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/stage_blinder_200_blaze/stage_blinder_200_blaze_tilt");
    private static final ResourceLocation PAN_MODEL = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/stage_blinder_200_blaze/stage_blinder_200_blaze_pan");
    // The fixed rear bracket for wall and side-truss mounts; the floor yoke is the pan model.
    private static final ResourceLocation STATIC_MODEL = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/stage_blinder_200_blaze/stage_blinder_200_blaze_wall_pan");

    private final float[] tiltRotation = new float[]{0.5F, 0.40625F, 0.625F};
    private final float[] panRotation = new float[]{0.5F, 0.96875F, 0.625F};
    private final float[] beamStartPosition = new float[]{0.5F, 0.40625F, 0.345F};

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
        return beamStartPosition;
    }

    @Override
    public float getDefaultRotation() {
        return 0;
    }

    @Override
    public float getBeamWidth() {
        return 0.0f;
    }

    @Override
    public float getRayTraceRotation() {
        return 180f;
    }

    @Override
    public HangType getHangType() {
        return HangType.HOOK_BAR;
    }

    @Override
    public float[] getTransforms(BlockState fixtureBlockState, BlockState supportBlockState) {
        if (fixtureBlockState.getValue(BaseLightBlock.HANG_DIRECTION) == Direction.UP) {
            return new float[]{0, .5f, 0};
        }
        return new float[]{0, 0.5F, 0};
    }

    @Override
    public List<DMXPersonality> getDMXPersonalities() {
        return PERSONALITIES;
    }

    @Override
    public boolean invertTilt() {
        return true;
    }

    @Override
    public boolean invertPan() {
        return true;
    }

    @Override
    public double getLightRadius() {
        return 34;
    }
}
