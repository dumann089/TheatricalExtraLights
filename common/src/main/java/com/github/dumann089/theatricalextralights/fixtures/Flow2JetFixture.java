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

public class Flow2JetFixture extends Fixture {

    private static final List<DMXPersonality> PERSONALITIES = Collections.singletonList(
            new DMXPersonality(1, "1-Channel Flow2Jet")
                    .addSlot(SharedSlots.INTENSITY)
    );

    private static final ResourceLocation WHOLE_MODEL =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/flow2jet/flow2jet_whole");
    private static final ResourceLocation STATIC_MODEL =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/flow2jet/flow2jet_static");

    /** Pivot rotation — machine entière (pas la buse seule). */
    private final float[] rotationPivot = new float[]{0.5F, 11f / 16f, 0.5F};
    /** Sortie buse — bas de l'ouverture Blockbench (y=10.5). */
    private final float[] beamStartPosition = new float[]{8.5f / 16f, 10.5f / 16f, 13f / 16f};

    @Override
    public ResourceLocation getTiltModel() {
        return WHOLE_MODEL;
    }

    @Override
    public ResourceLocation getPanModel() {
        return WHOLE_MODEL;
    }

    @Override
    public ResourceLocation getStaticModel() {
        return STATIC_MODEL;
    }

    @Override
    public float[] getTiltRotationPosition() {
        return rotationPivot;
    }

    @Override
    public float[] getPanRotationPosition() {
        return rotationPivot;
    }

    @Override
    public float[] getBeamStartPosition() {
        return beamStartPosition;
    }

    /** Au sol : tilt 0 = buse vers le ciel (+Y Blockbench). Suspendu : tilt 0 = vers le bas. */
    public static final float TILT_HANGING_NEUTRAL_OFFSET = 180f;

    public static float effectiveTilt(float userTilt, boolean rigged, boolean flipped) {
        if (flipped) {
            return -180f + userTilt;
        }
        if (rigged) {
            return userTilt + TILT_HANGING_NEUTRAL_OFFSET;
        }
        return userTilt;
    }

    /** Flip corps entier — seulement rig sans support (pas sur truss). */
    public static boolean shouldApplyBodyFlip(boolean upsideDown, boolean mounted) {
        return upsideDown && !mounted;
    }

    public static float effectiveTilt(float userTilt, boolean rigged) {
        return effectiveTilt(userTilt, rigged, false);
    }

    public static float effectiveTilt(float userTilt) {
        return effectiveTilt(userTilt, false, false);
    }

    @Override
    public float getDefaultRotation() {
        return 0;
    }

    @Override
    public float getBeamWidth() {
        return 0.2f;
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
            return new float[]{0, 0.5f, 0};
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
        return 0.0;
    }
}
