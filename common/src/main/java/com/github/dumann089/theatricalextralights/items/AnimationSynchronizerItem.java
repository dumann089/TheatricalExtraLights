package com.github.dumann089.theatricalextralights.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Sincronizador de animações: usar abre um menu com todas as câmeras do mundo, onde se escolhe a
 * gravação de cada uma e se dá play em todas ao mesmo tempo.
 */
public class AnimationSynchronizerItem extends Item {

    /** Preenchido pelo cliente: abre o menu (o item fica em código comum, a tela é só do cliente). */
    public static volatile Runnable clientOpenHook;

    public AnimationSynchronizerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            Runnable hook = clientOpenHook;
            if (hook != null) {
                hook.run();
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** O servidor só obedece aos pacotes do sincronizador de quem está com ele na mão. */
    public static boolean isHolding(Player player) {
        return player.getMainHandItem().getItem() instanceof AnimationSynchronizerItem
                || player.getOffhandItem().getItem() instanceof AnimationSynchronizerItem;
    }
}
