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

/**
 * Tete strobe sur lyre a tilt, sans pan. Les deux premiers modes sont les historiques ; les deux
 * suivants reprennent le moteur Atomic (intensite, duree de flash, cadence, effets).
 */
public class AtomictiltFixture extends Fixture {

    private static final List<DMXPersonality> PERSONALITIES = List.of(
            new DMXPersonality(6, "6-Channel RGB + Focus + Tilt")
                    .addSlot(SharedSlots.INTENSITY)
                    .addSlot(SharedSlots.RED)
                    .addSlot(SharedSlots.GREEN)
                    .addSlot(SharedSlots.BLUE)
                    .addSlot(SharedSlots.FOCUS)
                    .addSlot(SharedSlots.TILT),
            new DMXPersonality(7, "7-Channel RGB + Focus + Strobe + Tilt")
                    .addSlot(SharedSlots.INTENSITY)
                    .addSlot(SharedSlots.RED)
                    .addSlot(SharedSlots.GREEN)
                    .addSlot(SharedSlots.BLUE)
                    .addSlot(SharedSlots.FOCUS)
                    .addSlot(SharedSlots.FOCUS)
                    .addSlot(SharedSlots.TILT),
            new DMXPersonality(6, "6-Channel Atomic + Focus + Tilt")
                    .addSlot(AtomicSlots.INTENSITY)
                    .addSlot(AtomicSlots.DURATION)
                    .addSlot(AtomicSlots.RATE)
                    .addSlot(AtomicSlots.EFFECTS)
                    .addSlot(SharedSlots.FOCUS)
                    .addSlot(SharedSlots.TILT),
            new DMXPersonality(9, "9-Channel Atomic RGB + Focus + Tilt")
                    .addSlot(AtomicSlots.INTENSITY)
                    .addSlot(AtomicSlots.DURATION)
                    .addSlot(AtomicSlots.RATE)
                    .addSlot(AtomicSlots.EFFECTS)
                    .addSlot(SharedSlots.RED)
                    .addSlot(SharedSlots.GREEN)
                    .addSlot(SharedSlots.BLUE)
                    .addSlot(SharedSlots.FOCUS)
                    .addSlot(SharedSlots.TILT)
    );

    private static final ResourceLocation TILT_MODEL = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/atomictilt/atomictilt_tilt");
    private static final ResourceLocation STATIC_MODEL = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/atomictilt/atomictilt_base");
    private static final ResourceLocation PAN_MODEL = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/atomictilt/atomictilt_base");

    private final float[] tiltRotation = new float[]{0.503F, 0.567F, .462F};

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
        return new float[]{0f, 0f, 0f};
    }

    @Override
    public float[] getBeamStartPosition() {
        return new float[]{0.5F, 0.812F, 0.253F};
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
        return 12.5;
    }
}
