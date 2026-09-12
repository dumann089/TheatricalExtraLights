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

import java.util.List;

public class MovingScanBeamsFixture extends Fixture {

    private static final List<DMXPersonality> PERSONALITIES = List.of(
            new DMXPersonality(10, "10-Channel Mode")
                    .addSlot(SharedSlots.INTENSITY)
                    .addSlot(SharedSlots.RED)
                    .addSlot(SharedSlots.GREEN)
                    .addSlot(SharedSlots.BLUE)
                    .addSlot(SharedSlots.FOCUS)
                    .addSlot(SharedSlots.PAN)
                    .addSlot(SharedSlots.TILT)
                    .addSlot(SharedSlots.FOCUS) // Prism Beams
                    .addSlot(SharedSlots.FOCUS) // Prism Zoom
                    .addSlot(SharedSlots.FOCUS), // Prism Rotation
            FramingShutterChannels.PERSONALITY_19CH
    );

    private static final ResourceLocation TILT_MODEL = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/moving_scan/moving_scan_tilt");
    private static final ResourceLocation PAN_MODEL = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/moving_scan/moving_scan_pan");
    private static final ResourceLocation STATIC_MODEL = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/moving_scan/moving_scan_static");

    private final float[] tiltRotation = new float[]{0.5F, 1.25F, .46875F};
    private final float[] panRotation = new float[]{0.5F, 1.375F, .59375F};
//    private final float[] beamStartPosition = new float[]{0.5F, 1.875F, 0.4375F};


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
        return new float[]{0.5F, 1.25F, .46875F};
    }

    @Override
    public float getDefaultRotation() {
        return 90;
    }

    @Override
    public float getBeamWidth() {
        return 0.0f;
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
        if(fixtureBlockState.getValue(BaseLightBlock.HANG_DIRECTION) == Direction.UP){
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
