package com.github.dumann089.theatricalextralights.fixtures;

import ch.bildspur.artnet.rdm.RDMSlotID;
import ch.bildspur.artnet.rdm.RDMSlotType;
import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.api.HangType;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import dev.imabad.theatrical.api.dmx.DMXSlot;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import dev.imabad.theatrical.fixtures.SharedSlots;

import java.util.List;

public class Chcb4Fixture extends Fixture {

    public static final int CELL_COUNT = 4;
    public static final int PERSONALITY_12CH = 0;
    public static final int PERSONALITY_4CH = 1;

    private static final List<DMXPersonality> PERSONALITIES = List.of(
            build12ChannelPersonality(),
            new DMXPersonality(4, "4-Channel RGB + Shutter")
                    .addSlot(SharedSlots.RED)
                    .addSlot(SharedSlots.GREEN)
                    .addSlot(SharedSlots.BLUE)
                    .addSlot(ExtraLightsSlots.SHUTTER)
    );

    private static DMXPersonality build12ChannelPersonality() {
        DMXPersonality personality = new DMXPersonality(12, "12-Channel 4×RGB");
        for (int cell = 1; cell <= CELL_COUNT; cell++) {
            personality.addSlot(new DMXSlot("Cell " + cell + " Red", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_COLOR_SUB_CYAN));
            personality.addSlot(new DMXSlot("Cell " + cell + " Green", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_COLOR_SUB_MAGENTA));
            personality.addSlot(new DMXSlot("Cell " + cell + " Blue", RDMSlotType.ST_PRIMARY, RDMSlotID.SD_COLOR_SUB_YELLOW));
        }
        return personality;
    }

    private static final ResourceLocation STATIC_MODEL =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/chcb4/chcb4");
    private static final ResourceLocation EMPTY_MODEL =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/chcb4/empty");

    /** Centres des 4 cellules sur la face sud (z = 10), Y au milieu de la barre. */
    public static final float[][] BEAM_POSITIONS = {
            {3.0F / 16.0F, 1.5F / 16.0F, 10.0F / 16.0F},
            {5.8F / 16.0F, 1.5F / 16.0F, 10.0F / 16.0F},
            {8.8F / 16.0F, 1.5F / 16.0F, 10.0F / 16.0F},
            {11.6F / 16.0F, 1.5F / 16.0F, 10.0F / 16.0F}
    };

    private final float[] tiltRotation = new float[]{0.5F, 0.5F, 0.5F};
    private final float[] panRotation = new float[]{0.5F, 0.5F, 0.5F};

    @Override
    public ResourceLocation getTiltModel() {
        return EMPTY_MODEL;
    }

    @Override
    public ResourceLocation getPanModel() {
        return EMPTY_MODEL;
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
        return BEAM_POSITIONS[0];
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
    public boolean hasBeam() {
        return false;
    }

    @Override
    public float getRayTraceRotation() {
        return 0f;
    }

    @Override
    public HangType getHangType() {
        return HangType.HOOK_BAR;
    }

    @Override
    public float[] getTransforms(BlockState fixtureBlockState, BlockState supportBlockState) {
        if (fixtureBlockState.getValue(BaseLightBlock.HANG_DIRECTION) == Direction.UP) {
            return new float[]{0, 0.51F, 0};
        }
        return new float[]{0, -0.365F, 0};
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
        return 5.0;
    }
}
