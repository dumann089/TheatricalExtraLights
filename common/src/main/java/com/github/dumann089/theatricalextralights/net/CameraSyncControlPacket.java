package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.entities.BroadcastCameraEntity;
import com.github.dumann089.theatricalextralights.items.AnimationSynchronizerItem;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.function.Supplier;

/**
 * Cliente → servidor: começa (todas ao mesmo tick) ou para a reprodução das câmeras indicadas.
 * As gravações já foram entregues antes, "armadas", em {@link CameraPlaybackPacket}.
 */
public class CameraSyncControlPacket {

    private static final int MAX_CAMERAS = 64;

    private final boolean start;
    private final int[] ids;

    public CameraSyncControlPacket(boolean start, List<Integer> ids) {
        this.start = start;
        this.ids = ids.stream().mapToInt(Integer::intValue).toArray();
    }

    private CameraSyncControlPacket(boolean start, int[] ids) {
        this.start = start;
        this.ids = ids;
    }

    public static CameraSyncControlPacket decode(FriendlyByteBuf buf) {
        boolean start = buf.readBoolean();
        int count = Math.min(buf.readVarInt(), MAX_CAMERAS);
        int[] ids = new int[count];
        for (int i = 0; i < count; i++) {
            ids[i] = buf.readVarInt();
        }
        return new CameraSyncControlPacket(start, ids);
    }

    public void encode(FriendlyByteBuf buf) {
        int count = Math.min(ids.length, MAX_CAMERAS);
        buf.writeBoolean(start);
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buf.writeVarInt(ids[i]);
        }
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            if (!(contextSupplier.get().getPlayer() instanceof ServerPlayer player)
                    || !AnimationSynchronizerItem.isHolding(player)) {
                return;
            }
            // Tudo no mesmo tick do servidor: as câmeras começam juntas.
            for (int id : ids) {
                Entity entity = player.serverLevel().getEntity(id);
                if (entity instanceof BroadcastCameraEntity camera) {
                    if (start) {
                        camera.startArmedPlayback();
                    } else {
                        camera.stopAutoPlayback();
                    }
                }
            }
        });
    }
}
