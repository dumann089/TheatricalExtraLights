package com.github.dumann089.theatricalextralights.fixtures;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.blockentities.PixelBarBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.VerticalbarBlockEntity;
import dev.imabad.theatrical.Theatrical;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.api.HangType;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import dev.imabad.theatrical.fixtures.SharedSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class VerticalbarFixture extends Fixture {

    /** Un dimmer et un RGB par pixel, pas de dimmer general : 9 x 4 = 36 canaux. */
    public static final int PIXEL_CHANNEL_COUNT = PixelBarBlockEntity.pixelModeChannelCount(0, VerticalbarBlockEntity.PIXEL_COUNT);

    private static final List<DMXPersonality> PERSONALITIES = List.of(
            new DMXPersonality(4, "4-Channel Mode")
                    .addSlot(SharedSlots.INTENSITY)
                    .addSlot(SharedSlots.RED)
                    .addSlot(SharedSlots.GREEN)
                    .addSlot(SharedSlots.BLUE),
            PixelBarPersonalities.addPixels(new DMXPersonality(PIXEL_CHANNEL_COUNT,
                    PixelBarPersonalities.pixelModeName(PIXEL_CHANNEL_COUNT, VerticalbarBlockEntity.PIXEL_COUNT, "")),
                    VerticalbarBlockEntity.PIXEL_COUNT)
    );

    private static final ResourceLocation TILT_MODEL = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/barvertical/barvertical_tilt");
    private static final ResourceLocation PAN_MODEL = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/barvertical/barvertical_pan");
    private static final ResourceLocation STATIC_MODEL = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/barvertical/barvertical_pan");

    private final float[] tiltRotation = new float[]{0.5F, .5F, 0.796F};
    private final float[] panRotation = new float[]{0.5F, 0F, 0.90F};
    private final float[] beamStartPosition = new float[]{0.5F, 0.5F, 0.093F};

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
        return true;
    }

    @Override
    public boolean invertPan() {
        return true;
    }

    @Override
    public double getLightRadius() {
        return 8.5;
    }
}
