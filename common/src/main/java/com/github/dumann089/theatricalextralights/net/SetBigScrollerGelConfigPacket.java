
package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.GelScrollerConfigurable;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

public class SetBigScrollerGelConfigPacket {
    private final BlockPos pos;
    private final int[] colors;
    private final float size;
    private final float speed;

    public SetBigScrollerGelConfigPacket(
            BlockPos pos,
            int[] colors,
            float size,
            float speed
    ) {
        this.pos = pos.immutable();
        this.colors = colors.clone();
        this.size = size;
        this.speed = speed;
    }

    public static SetBigScrollerGelConfigPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int count = buf.readVarInt();

        if (count < 2 || count > 24) {
            throw new IllegalArgumentException("Invalid gel count: " + count);
        }

        int[] colors = new int[count];
        for (int i = 0; i < count; i++) {
            colors[i] = buf.readInt() & 0xFFFFFF;
        }

        return new SetBigScrollerGelConfigPacket(
                pos, colors, buf.readFloat(), buf.readFloat());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(colors.length);

        for (int color : colors) {
            buf.writeInt(color & 0xFFFFFF);
        }

        buf.writeFloat(size);
        buf.writeFloat(speed);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            var player = contextSupplier.get().getPlayer();

            if (player == null
                    || !player.level().hasChunkAt(pos)
                    || player.distanceToSqr(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5
            ) > 64.0 * 64.0) {
                return;
            }

            BlockEntity blockEntity = player.level().getBlockEntity(pos);

            if (blockEntity instanceof GelScrollerConfigurable configurable) {
                configurable.setGelConfiguration(colors, size, speed);
            }
        });
    }
}