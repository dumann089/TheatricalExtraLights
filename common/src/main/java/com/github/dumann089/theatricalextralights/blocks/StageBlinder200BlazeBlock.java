package com.github.dumann089.theatricalextralights.blocks;

import com.github.dumann089.theatricalextralights.TheatricalExtraLightsScreens;
import com.github.dumann089.theatricalextralights.blockentities.StageBlinder200BlazeBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.BlockEntities;
import com.github.dumann089.theatricalextralights.net.OpenExtraLightsScreenPacket;
import dev.imabad.theatrical.TheatricalClient;
import dev.imabad.theatrical.api.Support;
import dev.imabad.theatrical.blocks.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class StageBlinder200BlazeBlock extends ExtraLightsLightBlock {

    public StageBlinder200BlazeBlock() {
        super(Properties.of()
                .requiresCorrectToolForDrops()
                .strength(3, 3)
                .noOcclusion()
                .isValidSpawn(Blocks::neverAllowSpawn)
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .pushReaction(PushReaction.DESTROY));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new StageBlinder200BlazeBlockEntity(blockPos, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        Direction clickedFace = blockPlaceContext.getClickedFace();
        boolean attached = clickedFace != Direction.UP;
        return super.getStateForPlacement(blockPlaceContext)
                .setValue(FACING, clickedFace.getAxis().isHorizontal()
                        ? clickedFace : blockPlaceContext.getHorizontalDirection())
                .setValue(HANG_DIRECTION, clickedFace.getOpposite())
                .setValue(HANGING, attached);
    }

    @Override
    public boolean canSurvive(BlockState blockState, LevelReader levelReader, BlockPos blockPos) {
        if (blockState.getValue(HANGING)) {
            Direction supportDirection = blockState.getValue(HANG_DIRECTION);
            BlockPos supportPos = blockPos.relative(supportDirection);
            BlockState support = levelReader.getBlockState(supportPos);
            return support.getBlock() instanceof Support
                    || support.isFaceSturdy(levelReader, supportPos, supportDirection.getOpposite());
        }
        return !levelReader.getBlockState(blockPos.below()).isAir();
    }

    @Override
    public Direction getLightFacing(Direction hangDirection, Player placingPlayer) {
        return hangDirection.getAxis().isHorizontal()
                ? hangDirection.getOpposite() : placingPlayer.getDirection();
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == BlockEntities.STAGE_BLINDER_200_BLAZE.get() ? StageBlinder200BlazeBlockEntity::tick : null;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityCollisionContext && entityCollisionContext.getEntity() == null) {
            return Shapes.empty();
        }
        return super.getVisualShape(state, level, pos, context);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (super.use(state, level, pos, player, hand, hit) == InteractionResult.PASS) {
            if (!level.isClientSide) {
                if (player.isCrouching()) {
                    if (TheatricalClient.DEBUG_BLOCKS.contains(pos)) {
                        TheatricalClient.DEBUG_BLOCKS.remove(pos);
                    } else {
                        TheatricalClient.DEBUG_BLOCKS.add(pos);
                    }
                    return InteractionResult.SUCCESS;
                }
                new OpenExtraLightsScreenPacket(pos, TheatricalExtraLightsScreens.CHANNEL_PANTILT)
                        .sendTo((ServerPlayer) player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            TheatricalClient.DEBUG_BLOCKS.remove(pos);
        }
        super.destroy(level, pos, state);
    }
}
