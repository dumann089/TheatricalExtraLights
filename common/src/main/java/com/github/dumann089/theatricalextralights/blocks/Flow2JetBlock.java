package com.github.dumann089.theatricalextralights.blocks;

import com.github.dumann089.theatricalextralights.TheatricalExtraLightsScreens;
import com.github.dumann089.theatricalextralights.blockentities.Flow2JetBlockEntity;
import com.github.dumann089.theatricalextralights.client.Flow2JetClientEffects;
import com.github.dumann089.theatricalextralights.net.OpenExtraLightsScreenPacket;
import dev.imabad.theatrical.blocks.Blocks;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class Flow2JetBlock extends ExtraLightsLightBlock {

    public Flow2JetBlock() {
        super(Properties.of()
                .requiresCorrectToolForDrops()
                .strength(3, 3)
                .noOcclusion()
                .isValidSpawn(Blocks::neverAllowSpawn)
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .pushReaction(PushReaction.DESTROY));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && !state.getValue(HANGING)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Flow2JetBlockEntity flow2Jet) {
                flow2Jet.resetNeutralPose();
                flow2Jet.setChanged();
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (state.getValue(HANGING)) {
            stack.getOrCreateTag().put("BlockStateTag", NbtUtils.writeBlockState(state));
        }
        return stack;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new Flow2JetBlockEntity(blockPos, blockState);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        return super.getStateForPlacement(blockPlaceContext).setValue(HANGING,
                blockPlaceContext.getClickedFace() == Direction.DOWN
                        || isHanging(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos()));
    }

    @Override
    public boolean canSurvive(BlockState blockState, LevelReader levelReader, BlockPos blockPos) {
        if (blockState.getValue(HANGING)) {
            return true;
        }
        return !levelReader.getBlockState(blockPos.below()).isAir();
    }

    @Override
    public Direction getLightFacing(Direction hangDirection, Player placingPlayer) {
        if (hangDirection == Direction.UP) {
            return placingPlayer.getDirection();
        }
        Direction playerFacing = placingPlayer.getDirection();
        if (playerFacing.getAxis() == Direction.Axis.X) {
            if (playerFacing == Direction.WEST) {
                return Direction.SOUTH;
            } else {
                return Direction.NORTH;
            }
        } else {
            if (playerFacing == Direction.SOUTH) {
                return Direction.WEST;
            } else {
                return Direction.EAST;
            }
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == com.github.dumann089.theatricalextralights.blockentities.BlockEntities.FLOW2JET.get()
                ? Flow2JetBlockEntity::tick
                : null;
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
        InteractionResult base = super.use(state, level, pos, player, hand, hit);
        // Wrench / config card handled by ExtraLightsLightBlock — do not open pan/tilt over them.
        if (base != InteractionResult.PASS) {
            return base;
        }
        // TheatricalClient is client-only — never touch it on the dedicated server.
        if (level.isClientSide) {
            if (player.isCrouching()) {
                Flow2JetClientEffects.toggleDebugOverlay(pos);
            }
            return InteractionResult.SUCCESS;
        }
        if (!player.isCrouching()) {
            new OpenExtraLightsScreenPacket(pos, TheatricalExtraLightsScreens.CHANNEL_PANTILT)
                    .sendTo((ServerPlayer) player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            Flow2JetClientEffects.onBlockRemoved(pos);
        }
        super.destroy(level, pos, state);
    }
}
