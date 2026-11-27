package com.github.dumann089.theatricalextralights.items;

import com.github.dumann089.theatricalextralights.entities.ModEntities;
import com.github.dumann089.theatricalextralights.entities.ShoulderCameraEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Shoulder camera: segurada, a câmera aparece no ombro do jogador (ver {@code ShoulderCameraLayer}).
 * Usar (botão direito) liga a câmera: o jogador passa a operá-la e a ver pelo olho dela.
 */
public class ShoulderCameraItem extends Item {

    public ShoulderCameraItem(Properties properties) {
        super(properties);
    }

    /** Quem carrega o item sempre tem a câmera no mundo (parada, acompanhando-o): assim ela está nas listas. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (level instanceof ServerLevel serverLevel && holder instanceof ServerPlayer player
                && player.tickCount % 20 == 0) {
            ensureCamera(serverLevel, player);
        }
    }

    /** A shoulder camera do jogador (UUID fixo): a que já existe, ou uma nova junto dele. */
    public static ShoulderCameraEntity ensureCamera(ServerLevel level, ServerPlayer player) {
        UUID id = ShoulderCameraEntity.idFor(player.getUUID());
        if (level.getEntity(id) instanceof ShoulderCameraEntity existing) {
            return existing;
        }
        ShoulderCameraEntity camera = ModEntities.SHOULDER_CAMERA.get().create(level);
        if (camera == null) {
            return null;
        }
        camera.setUUID(id);
        camera.setOwner(player.getUUID());
        camera.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0f);
        return level.addFreshEntity(camera) ? camera : null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (player.isPassenger()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }
        ShoulderCameraEntity camera = ensureCamera(serverLevel, serverPlayer);
        if (camera == null) {
            return InteractionResultHolder.fail(stack);
        }
        camera.stopAutoPlayback(); // se havia um boneco tocando, o jogador assume o controle
        camera.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0f);
        player.startRiding(camera, true);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
