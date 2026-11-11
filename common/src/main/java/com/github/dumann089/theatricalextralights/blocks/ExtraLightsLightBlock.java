package com.github.dumann089.theatricalextralights.blocks;

import com.github.dumann089.theatricalextralights.TheatricalExtraLightsScreens;
import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import com.github.dumann089.theatricalextralights.compat.SupportCompat;
import com.github.dumann089.theatricalextralights.net.OpenExtraLightsScreenPacket;
import com.github.dumann089.theatricalextralights.util.ConfigurationCardHelper;
import com.github.dumann089.theatricalextralights.util.TheatricalNetworkAccess;
import dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import dev.imabad.theatrical.util.UUIDUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Base des fixtures Extra Lights — configuration card avec wrap automatique d'univers DMX.
 */
public abstract class ExtraLightsLightBlock extends BaseLightBlock {

    protected ExtraLightsLightBlock(Properties properties) {
        super(properties);
    }

    /**
     * Theatrical only recognises its own {@link dev.imabad.theatrical.api.Support} blocks as
     * hangers. Accept CSC trusses too, so a fixture mounted under one is placed and kept in the
     * hanging state (and therefore gets the same mount-height transform as on a Theatrical truss).
     */
    @Override
    public boolean isHanging(LevelReader levelReader, BlockPos pos) {
        BlockState state = levelReader.getBlockState(pos);
        if (!(state.getBlock() instanceof HangableBlock) || !state.getValue(HangableBlock.HANGING)) {
            return false;
        }
        BlockPos supportPos = pos.relative(state.getValue(HangableBlock.HANG_DIRECTION));
        return !levelReader.isEmptyBlock(supportPos)
                && SupportCompat.isSupport(levelReader.getBlockState(supportPos));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!level.isClientSide() && be instanceof ExtraLightsLightBlockEntity) {
            // The wrench and tracker are local tools: they must work even when the fixture is
            // patched to a network the player cannot configure. The previous ordering loaded the
            // Theatrical network API first, which made the wrench fail on some builds.
            if (player.getItemInHand(hand).getItem()
                    == com.github.dumann089.theatricalextralights.items.Items.FIXTURE_WRENCH.get()) {
                new OpenExtraLightsScreenPacket(pos, TheatricalExtraLightsScreens.MOUNT_WRENCH)
                        .sendTo((ServerPlayer) player);
                return InteractionResult.SUCCESS;
            }

            if (player.getItemInHand(hand).getItem()
                    == com.github.dumann089.theatricalextralights.items.Items.FIXTURE_TRACKER.get()) {
                com.github.dumann089.theatricalextralights.items.FixtureTrackerItem.addFixtureToSelection(
                        (ServerPlayer) player, player.getItemInHand(hand), (ExtraLightsLightBlockEntity) be);
                return InteractionResult.SUCCESS;
            }

            if (be instanceof BaseDMXConsumerLightBlockEntity consumerLightBlockEntity) {
                if (!consumerLightBlockEntity.getNetworkId().equals(UUIDUtil.NULL)
                        && !TheatricalNetworkAccess.canPlayerConfigure(player, level, consumerLightBlockEntity.getNetworkId())) {
                    return InteractionResult.FAIL;
                }
            }

            if (be instanceof BaseDMXConsumerLightBlockEntity consumerLightBlockEntity) {
                if (player.getItemInHand(hand).getItem() == dev.imabad.theatrical.items.Items.CONFIGURATION_CARD.get()) {
                    ItemStack itemInHand = player.getItemInHand(hand);
                    CompoundTag tagData = itemInHand.getOrCreateTag();
                    consumerLightBlockEntity.setNetworkId(tagData.getUUID("network"));
                    ConfigurationCardHelper.ApplyResult result =
                            ConfigurationCardHelper.applyToFixture(tagData, consumerLightBlockEntity);
                    itemInHand.save(tagData);
                    ConfigurationCardHelper.sendPatchMessages(player, level, consumerLightBlockEntity, result);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        }
        return super.use(state, level, pos, player, hand, hit);
    }
}
