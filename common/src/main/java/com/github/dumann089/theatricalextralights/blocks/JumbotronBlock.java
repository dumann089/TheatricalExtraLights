package com.github.dumann089.theatricalextralights.blocks;

import com.github.dumann089.theatricalextralights.blockentities.JumbotronBlockEntity;
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

/**
 * Jumbotron: uma placa fina de 4 px que serve de canto (embaixo à esquerda, visto de frente) de uma tela
 * de até 32x20 blocos. Clicar abre a configuração (tamanho, qualidade, mesa de corte).
 */
public class JumbotronBlock extends HorizontalDirectionalBlock implements EntityBlock {

    /** Preenchido pelo cliente: abre a tela de configuração do jumbotron na posição dada. */
    public static volatile Consumer<BlockPos> clientOpenHook;

    // A placa fica ATRÁS (lado oposto ao FACING). Profundidade de 4 px.
    private static final VoxelShape NORTH_SHAPE = Block.box(0, 0, 12, 16, 16, 16);
    private static final VoxelShape SOUTH_SHAPE = Block.box(0, 0, 0, 16, 16, 4);
    private static final VoxelShape WEST_SHAPE = Block.box(12, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST_SHAPE = Block.box(0, 0, 0, 4, 16, 16);

    public JumbotronBlock() {
        super(Properties.of()
                .strength(2.0f)
                .noOcclusion()
                .mapColor(MapColor.COLOR_BLACK)
                .sound(SoundType.METAL));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new JumbotronBlockEntity(pos, state);
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
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> NORTH_SHAPE;
        };
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
