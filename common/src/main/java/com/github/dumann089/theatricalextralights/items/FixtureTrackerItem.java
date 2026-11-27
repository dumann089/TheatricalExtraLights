package com.github.dumann089.theatricalextralights.items;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import com.github.dumann089.theatricalextralights.util.TheatricalNetworkAccess;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Locks one or more moving heads (and their beams) on an entity.
 *
 * <ol>
 *   <li>Right-click fixtures one by one to add each to the selection (the tool glows).</li>
 *   <li>Right-click an entity: every selected fixture follows it. Right-click the air instead:
 *       they follow you.</li>
 *   <li>Shift + right-click a selected (not-yet-assigned) fixture: removes just that one from the
 *       selection. Shift + right-click a fixture that is already tracking something: releases it.
 *       Shift + right-click the air: clears the whole selection.</li>
 * </ol>
 *
 * The selection (a list of positions, all in the same dimension) is stored in the item; each
 * fixture's own target is stored on the fixture (survives restarts). Adding a fixture to the
 * selection is handled in {@code ExtraLightsLightBlock#use}, like the Fixture Wrench.
 */
public class FixtureTrackerItem extends Item {
    private static final String TAG_POSITIONS = "TrackerFixturePositions";
    private static final String TAG_DIM = "TrackerFixtureDim";
    private static final String KEY = "message.theatricalextralights.tracker.";

    public FixtureTrackerItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
                .arch$tab(TheatricalExtraLights.TAB));
    }

    /** Entity clicks: registered once from the mod initializer. */
    public static void registerEvents() {
        InteractionEvent.INTERACT_ENTITY.register((player, entity, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof FixtureTrackerItem)) {
                return EventResult.pass();
            }
            // Client: let the click go through so the server receives it.
            if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return EventResult.pass();
            }
            if (!hasSelection(stack)) {
                serverPlayer.displayClientMessage(Component.translatable(KEY + "hint"), true);
            } else {
                assignTarget(serverPlayer, stack, entity);
            }
            return EventResult.interruptTrue();
        });
    }

    /** Called from {@code ExtraLightsLightBlock#use} (server) when the tool is right-clicked on a fixture. */
    public static void addFixtureToSelection(ServerPlayer player, ItemStack stack, ExtraLightsLightBlockEntity fixture) {
        Component name = Component.translatable(fixture.getTranslationKey());
        if (!fixture.supportsTracking() || fixture.getLevel() == null) {
            player.displayClientMessage(Component.translatable(KEY + "unsupported", name), true);
            return;
        }
        String dimension = fixture.getLevel().dimension().location().toString();
        CompoundTag tag = stack.getOrCreateTag();
        if (hasSelection(stack) && !tag.getString(TAG_DIM).equals(dimension)) {
            player.displayClientMessage(Component.translatable(KEY + "different_dimension"), true);
            return;
        }
        BlockPos pos = fixture.getBlockPos();
        List<BlockPos> positions = readPositions(tag);
        if (positions.contains(pos)) {
            player.displayClientMessage(Component.translatable(KEY + "already_selected", name), true);
            return;
        }
        positions.add(pos);
        writePositions(tag, positions);
        tag.putString(TAG_DIM, dimension);
        player.displayClientMessage(Component.translatable(KEY + "added", name, positions.size()), true);
    }

    /** Shift + right-click on a fixture: the block's own {@code use} is skipped while sneaking, so it lands here. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ExtraLightsLightBlockEntity fixture)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ItemStack stack = player.getItemInHand(context.getHand());
            Component name = Component.translatable(fixture.getTranslationKey());
            List<BlockPos> positions = readPositions(stack.getTag());
            if (positions.remove(pos)) {
                // Was only pending assignment, not yet tracking anything: just drop it from the selection.
                writePositions(stack.getOrCreateTag(), positions);
                serverPlayer.displayClientMessage(Component.translatable(KEY + "removed", name, positions.size()), true);
            } else if (!TheatricalNetworkAccess.canPlayerConfigure(player, level, fixture.getNetworkId())) {
                serverPlayer.displayClientMessage(Component.translatable(KEY + "denied"), true);
            } else if (fixture.getTrackedEntity() != null) {
                fixture.setTrackedEntity(null);
                serverPlayer.displayClientMessage(Component.translatable(KEY + "released", name), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Right-click an entity that vanilla didn't pick up on its own (missed by {@link #registerEvents},
     * because the target's {@code isPickable()} returns false - common for cosmetic/scene-actor entities
     * from mods like BBS): track it. Otherwise, right-click in the air tracks yourself. Shift + right-click
     * in the air clears the selection.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // A click that hits a block is not an "air" click.
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                if (hasSelection(stack)) {
                    clearSelection(stack);
                    serverPlayer.displayClientMessage(Component.translatable(KEY + "cleared"), true);
                }
            } else if (hasSelection(stack)) {
                Entity target = manualEntityPick(player);
                assignTarget(serverPlayer, stack, target != null ? target : player);
            } else {
                serverPlayer.displayClientMessage(Component.translatable(KEY + "hint"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * Raytraces against every entity's real bounding box along the crosshair, ignoring
     * {@code Entity#isPickable()} (unlike vanilla's own entity-interaction targeting). Actor/scene entities
     * from cosmetic mods (BBS and similar) often leave {@code isPickable()} at its default of false, which
     * makes vanilla's own click handling skip straight to this item's {@code useOn}/{@code use} without
     * ever visiting {@link #registerEvents}'s {@code INTERACT_ENTITY} hook - this fallback catches those.
     */
    @Nullable
    private static Entity manualEntityPick(Player player) {
        double reach = 6.0;
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 end = start.add(player.getViewVector(1.0F).scale(reach));
        AABB searchBox = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0);
        EntityHitResult result = ProjectileUtil.getEntityHitResult(player.level(), player, start, end, searchBox,
                candidate -> !candidate.isSpectator() && candidate.isAlive() && candidate != player);
        return result != null ? result.getEntity() : null;
    }

    private static void assignTarget(ServerPlayer player, ItemStack stack, Entity target) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_DIM)) {
            player.displayClientMessage(Component.translatable(KEY + "hint"), true);
            return;
        }
        List<BlockPos> positions = readPositions(tag);
        if (positions.isEmpty()) {
            player.displayClientMessage(Component.translatable(KEY + "hint"), true);
            return;
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(tag.getString(TAG_DIM)));
        ServerLevel fixtureLevel = player.server.getLevel(dimension);
        if (fixtureLevel == null) {
            clearSelection(stack);
            player.displayClientMessage(Component.translatable(KEY + "unloaded"), true);
            return;
        }
        if (target.level() != fixtureLevel) {
            player.displayClientMessage(Component.translatable(KEY + "dimension"), true);
            return;
        }

        int assigned = 0;
        int skipped = 0;
        Component lastName = null;
        for (BlockPos pos : positions) {
            if (!fixtureLevel.isLoaded(pos)
                    || !(fixtureLevel.getBlockEntity(pos) instanceof ExtraLightsLightBlockEntity fixture)
                    || !fixture.supportsTracking()
                    || !TheatricalNetworkAccess.canPlayerConfigure(player, fixtureLevel, fixture.getNetworkId())) {
                skipped++;
                continue;
            }
            fixture.setTrackedEntity(target.getUUID());
            lastName = Component.translatable(fixture.getTranslationKey());
            assigned++;
        }
        clearSelection(stack);

        if (assigned == 0) {
            player.displayClientMessage(Component.translatable(KEY + "assign_failed"), true);
        } else if (assigned == 1) {
            player.displayClientMessage(Component.translatable(KEY + "tracking", lastName, target.getDisplayName()), true);
        } else if (skipped == 0) {
            player.displayClientMessage(Component.translatable(KEY + "tracking_multiple", assigned, target.getDisplayName()), true);
        } else {
            player.displayClientMessage(
                    Component.translatable(KEY + "tracking_multiple_partial", assigned, skipped, target.getDisplayName()), true);
        }
    }

    private static boolean hasSelection(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_POSITIONS) && !tag.getList(TAG_POSITIONS, 10).isEmpty();
    }

    private static void clearSelection(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_POSITIONS);
            tag.remove(TAG_DIM);
        }
    }

    private static List<BlockPos> readPositions(@Nullable CompoundTag tag) {
        List<BlockPos> positions = new ArrayList<>();
        if (tag == null || !tag.contains(TAG_POSITIONS)) {
            return positions;
        }
        for (var entry : tag.getList(TAG_POSITIONS, 10)) {
            positions.add(NbtUtils.readBlockPos((CompoundTag) entry));
        }
        return positions;
    }

    private static void writePositions(CompoundTag tag, List<BlockPos> positions) {
        if (positions.isEmpty()) {
            tag.remove(TAG_POSITIONS);
            tag.remove(TAG_DIM);
            return;
        }
        ListTag list = new ListTag();
        for (BlockPos pos : positions) {
            list.add(NbtUtils.writeBlockPos(pos));
        }
        tag.put(TAG_POSITIONS, list);
    }

    /** Glows while a fixture is selected. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return hasSelection(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.theatricalextralights.fixture_tracker.tooltip.1"));
        tooltip.add(Component.translatable("item.theatricalextralights.fixture_tracker.tooltip.2"));
        tooltip.add(Component.translatable("item.theatricalextralights.fixture_tracker.tooltip.3"));
        tooltip.add(Component.translatable("item.theatricalextralights.fixture_tracker.tooltip.4"));
        int count = readPositions(stack.getTag()).size();
        if (count > 0) {
            tooltip.add(Component.translatable("item.theatricalextralights.fixture_tracker.tooltip.selected", count));
        }
    }
}
