package com.github.dumann089.theatricalextralights.blocks;

import com.github.dumann089.theatricalextralights.blockentities.VideoSwitcherBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/** Mesa de corte de vídeo ao vivo: clicar abre o menu (teclas das câmeras, modos e fade). */
public class VideoSwitcherBlock extends HorizontalDirectionalBlock implements EntityBlock {

    /** Preenchido pelo cliente: abre o menu da mesa na posição dada. */
    public static volatile Consumer<BlockPos> clientOpenHook;

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public VideoSwitcherBlock() {
        super(Properties.of()
                .strength(2.0f)
                .noOcclusion()
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VideoSwitcherBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (level.isClientSide()) {
            Consumer<BlockPos> hook = clientOpenHook;
            if (hook != null) {
                hook.accept(pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
