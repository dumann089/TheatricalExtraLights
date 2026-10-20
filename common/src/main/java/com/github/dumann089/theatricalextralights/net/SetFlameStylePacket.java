package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasFlameStyle;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

/** Client → serveur : style de flamme d'une machine a flammes (jet ou burst). */
public class SetFlameStylePacket {

    private final BlockPos pos;
    private final int style;

    public SetFlameStylePacket(BlockPos pos, int style) {
        this.pos = pos;
        this.style = style;
    }

    public static SetFlameStylePacket decode(FriendlyByteBuf buf) {
        return new SetFlameStylePacket(buf.readBlockPos(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(style);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            Player player = contextSupplier.get().getPlayer();
            if (player == null || player.level() == null) {
                return;
            }
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0 * 64.0) {
                return;
            }
            BlockEntity be = player.level().getBlockEntity(pos);
            if (be instanceof HasFlameStyle flame && style >= 0 && style < HasFlameStyle.STYLE_COUNT) {
                flame.setFlameStyle(style);
            }
        });
    }
}
