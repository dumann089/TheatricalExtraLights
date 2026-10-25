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

public class bigscrollerFixture extends Fixture {
    private static final List<DMXPersonality> PERSONALITIES = List.of(
            new DMXPersonality(4, "iRGB")
                    .addSlot(SharedSlots.INTENSITY)
                    .addSlot(SharedSlots.RED)
                    .addSlot(SharedSlots.GREEN)
                    .addSlot(SharedSlots.BLUE),
            new DMXPersonality(4, "3-Section Gel Scroller")
                    .addSlot(SharedSlots.INTENSITY)
                    .addSlot(SharedSlots.INTENSITY)
                    .addSlot(SharedSlots.INTENSITY)
                    .addSlot(SharedSlots.INTENSITY)
    );

    private static final ResourceLocation TILT_MODEL =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/scrollers/bigscrollers/bigscroller_whole");
    private static final ResourceLocation PAN_MODEL =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/scrollers/bigscrollers/bigscroller_static");
    private static final ResourceLocation STATIC_MODEL =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/scrollers/bigscrollers/bigscroller_static");

    private final float[] tiltRotation = {0.5F, 0.5F, 0.5F};
    private final float[] panRotation = {0.5F, 0.5F, 0.5F};

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
                0.625F, -0.625F, 0.812F,
                0.625F, -0.1875F, 0.812F,
                0.625F, 0.25F, 0.812F,
                0.625F, 0.6875F, 0.812F,
                0.625F, 1.125F, 0.812F,
                0.625F, 1.5625F, 0.812F,
                0.0F, -0.625F, 0.812F,
                0.0F, -0.1875F, 0.812F,
                0.0F, 0.25F, 0.812F,
                0.0F, 0.6875F, 0.812F,
                0.0F, 1.125F, 0.812F,
                0.0F, 1.5625F, 0.812F,
                -0.618F, -0.625F, 0.812F,
                -0.618F, -0.1875F, 0.812F,
                -0.618F, 0.25F, 0.812F,
                -0.618F, 0.6875F, 0.812F,
                -0.618F, 1.125F, 0.812F,
                -0.618F, 1.5625F, 0.812F
        };
    }

    @Override
    public float getDefaultRotation() {
        return 0;
    }

    @Override
    public float getBeamWidth() {
        return 0.0F;
    }

    @Override
    public float getRayTraceRotation() {
        return 180F;
    }

    @Override
    public HangType getHangType() {
        return HangType.HOOK_BAR;
    }

    @Override
    public float[] getTransforms(BlockState fixtureBlockState, BlockState supportBlockState) {
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