package com.github.dumann089.theatricalextralights.blocks;

import com.github.dumann089.theatricalextralights.blockentities.BlockEntities;
import com.github.dumann089.theatricalextralights.blockentities.DenseVerticalbarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** La Vertical RGB Bar avec sa variante dense de 46 pixels : meme bloc, autre block entity. */
public class DenseVerticalbarBlock extends VerticalbarBlock {

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new DenseVerticalbarBlockEntity(blockPos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == BlockEntities.DENSE_VERTICAL_BAR.get() ? DenseVerticalbarBlockEntity::tick : null;
    }
}
