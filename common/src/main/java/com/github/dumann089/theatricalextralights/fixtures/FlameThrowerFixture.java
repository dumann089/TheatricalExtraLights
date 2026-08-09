package com.github.dumann089.theatricalextralights.fixtures;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.util.DirectionOffset;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.api.HangType;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import dev.imabad.theatrical.fixtures.SharedSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class FlameThrowerFixture extends Fixture {
    private static final List<DMXPersonality> PERSONALITIES = List.of(
            new DMXPersonality(2, "2-Channel Flame Thrower")
                    .addSlot(SharedSlots.INTENSITY)
                    .addSlot(SharedSlots.PAN)
    );

    private final ResourceLocation bodyModel;
    private final ResourceLocation headModel;
    private final float[] pivot = new float[]{
            (float) DirectionOffset.FLAME_HEAD_PIVOT_BLOCK.x,
            (float) DirectionOffset.FLAME_HEAD_PIVOT_BLOCK.y,
            (float) DirectionOffset.FLAME_HEAD_PIVOT_BLOCK.z
    };

    public FlameThrowerFixture() {
        this.bodyModel = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/flame_thrower_body");
        this.headModel = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/flame_thrower_head");
    }

    @Override
    public ResourceLocation getTiltModel() {
        return headModel;
    }

    @Override
    public ResourceLocation getPanModel() {
        return headModel;
    }

    @Override
    public ResourceLocation getStaticModel() {
        return bodyModel;
    }

    @Override
    public float[] getTiltRotationPosition() {
        return pivot;
    }

    @Override
    public float[] getPanRotationPosition() {
        return pivot;
    }

    @Override
    public float[] getBeamStartPosition() {
        return new float[]{
                (float) DirectionOffset.FLAME_NOZZLE_BLOCK.x,
                (float) DirectionOffset.FLAME_NOZZLE_BLOCK.y,
                (float) DirectionOffset.FLAME_NOZZLE_BLOCK.z
        };
    }

    @Override
    public float getDefaultRotation() {
        return 180.0f;
    }

    @Override
    public float getBeamWidth() {
        return 0.0f;
    }

    @Override
    public float getRayTraceRotation() {
        return 0.0f;
    }

    @Override
    public HangType getHangType() {
        return HangType.BRACE_BAR;
    }

    @Override
    public float[] getTransforms(BlockState fixtureBlockState, BlockState supportBlockState) {
        if (fixtureBlockState.getValue(BaseLightBlock.HANG_DIRECTION) == Direction.UP) {
            return new float[]{0, 0.5f, 0};
        }
        return new float[]{0, -0.35f, 0};
    }

    @Override
    public List<DMXPersonality> getDMXPersonalities() {
        return PERSONALITIES;
    }

    @Override
    public double getLightRadius() {
        return 0.5d;
    }
}
