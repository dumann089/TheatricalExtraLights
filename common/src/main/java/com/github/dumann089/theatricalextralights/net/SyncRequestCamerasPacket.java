package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.blockentities.VideoSwitcherBlockEntity;
import com.github.dumann089.theatricalextralights.entities.BroadcastCameraEntity;
import com.github.dumann089.theatricalextralights.entities.JibCameraEntity;
import com.github.dumann089.theatricalextralights.items.AnimationSynchronizerItem;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Cliente → servidor: "quais câmeras existem neste mundo?" (o servidor responde com a lista). Vale para
 * quem está com o sincronizador na mão ou perto de uma mesa de corte (nesse caso a posição vai junto).
 */
public class SyncRequestCamerasPacket {

    private static final int MAX_CAMERAS = 64;

    private final BlockPos deskPos;

    public SyncRequestCamerasPacket() {
        this(null);
    }

    public SyncRequestCamerasPacket(BlockPos deskPos) {
        this.deskPos = deskPos;
    }

    public static SyncRequestCamerasPacket decode(FriendlyByteBuf buf) {
        return new SyncRequestCamerasPacket(buf.readBoolean() ? buf.readBlockPos() : null);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(deskPos != null);
        if (deskPos != null) {
            buf.writeBlockPos(deskPos);
        }
    }

    private boolean allowed(ServerPlayer player) {
        if (AnimationSynchronizerItem.isHolding(player)) {
            return true;
        }
        return deskPos != null && player.distanceToSqr(Vec3.atCenterOf(deskPos)) <= 12.0 * 12.0
                && player.level().getBlockEntity(deskPos) instanceof VideoSwitcherBlockEntity;
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            if (!(contextSupplier.get().getPlayer() instanceof ServerPlayer player) || !allowed(player)) {
                return;
            }
            List<SyncCameraListPacket.Entry> cameras = new ArrayList<>();
            for (Entity entity : player.serverLevel().getAllEntities()) {
                if (entity instanceof BroadcastCameraEntity camera && !camera.isRemoved()) {
                    String ownerName = "";
                    if (camera instanceof com.github.dumann089.theatricalextralights.entities.ShoulderCameraEntity shoulder
                            && shoulder.getOwnerId().isPresent()) {
                        ServerPlayer ownerPlayer = player.server.getPlayerList().getPlayer(shoulder.getOwnerId().get());
                        if (ownerPlayer != null) {
                            ownerName = ownerPlayer.getGameProfile().getName();
                        }
                    }
                    cameras.add(new SyncCameraListPacket.Entry(camera.getId(), camera instanceof JibCameraEntity,
                            camera.getBlockX(), camera.getBlockY(), camera.getBlockZ(), camera.isAutoPlaying(),
                            camera.getUUID(), camera instanceof com.github.dumann089.theatricalextralights.entities.ShoulderCameraEntity,
                            ownerName, camera.getCameraName()));
                    if (cameras.size() >= MAX_CAMERAS) {
                        break;
                    }
                }
            }
            ModNetworkHandler.CHANNEL.sendToPlayer(player, new SyncCameraListPacket(cameras));
        });
    }
}
