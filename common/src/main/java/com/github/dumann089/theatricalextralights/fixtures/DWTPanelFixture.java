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

public class DWTPanelFixture extends Fixture {

    private static final List<DMXPersonality> PERSONALITIES = Collections.singletonList(
            new DMXPersonality(6, "6-Channel Mode")
                    .addSlot(SharedSlots.FOCUS)
                    .addSlot(SharedSlots.FOCUS)
                    .addSlot(SharedSlots.FOCUS)
                    .addSlot(SharedSlots.FOCUS)
                    .addSlot(SharedSlots.INTENSITY)
                    .addSlot(SharedSlots.PAN)


    );

    private static final ResourceLocation TILT_MODEL = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/dwtpanel/dwtpanel_pan");
    private static final ResourceLocation PAN_MODEL = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/dwtpanel/dwtpanel_whole");
    private static final ResourceLocation STATIC_MODEL = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/dwtpanel/dwtpanel_static");

    private final float[] tiltRotation = new float[]{0.5F, 0.5F, .5F};
    private final float[] panRotation = new float[]{0.5F, 0.5F, 0.5F};
//  private final float[] beamStartPosition = new float[]{ 0.56F, 1.43F, 0.226F };
// private final float[] beamStartPosition2 = new float[]{ -0.56F, 1.43F, 0.226F };
// private final float[] beamStartPosition3 = new float[]{ 0.56F, 0.43F, 0.226F };
// private final float[] beamStartPosition4 = new float[]{ -0.56F, 0.43F, 0.226F };


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
    return new float[]{
        0.56F, 1.43F, 0.226F,   // Beam 1
       -0.56F, 1.43F, 0.226F,   // Beam 2
        0.56F, 0.43F, 0.226F,   // Beam 3
       -0.56F, 0.43F, 0.226F    // Beam 4
        };
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
        if(fixtureBlockState.getValue(BaseLightBlock.HANG_DIRECTION) == Direction.UP){
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
        return false;
    }

    @Override
    public boolean invertPan() {
        return false;
    }

    @Override
    public double getLightRadius() {
        return 9.5;
    }
}
