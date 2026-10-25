package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static dev.imabad.theatrical.blocks.HangableBlock.FACING;
import static dev.imabad.theatrical.blocks.HangableBlock.HANGING;
import static dev.imabad.theatrical.blocks.HangableBlock.HANG_DIRECTION;

public class StageBlinder200BlazeBlockEntity extends BlinderBaseBlockEntity {

    public StageBlinder200BlazeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.STAGE_BLINDER_200_BLAZE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StageBlinder200BlazeBlockEntity be) {
        BlinderBaseBlockEntity.tick(level, pos, state, be);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.STAGE_BLINDER_200_BLAZE.get();
    }

    @Override
    public int getDeviceTypeId() {
        return 0x03;
    }

    @Override
    public String getModelName() {
        return "Showtec Stage Blinder 200 Blaze";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.STAGE_BLINDER_200_BLAZE.getId();
    }

    @Override
    public Vector3f getLightPos() {
        BlockState state = getBlockState();
        if (state.getValue(HANGING) && state.getValue(HANG_DIRECTION).getAxis().isHorizontal()) {
            Direction facing = state.getValue(FACING);
            // The diffuser is at z=5.52/16 in the head model, centered at y=6.5/16.
            // Keep the light on that plane rather than several blocks ahead of the fixture.
            return Vec3.atCenterOf(getBlockPos())
                    .add(0, 6.5 / 16.0 - 0.5, 0)
                    .add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.5 - 5.52 / 16.0 + 0.02))
                    .toVector3f();
        }
        return super.getLightPos();
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.stage_blinder_200_blaze";
    }
}
