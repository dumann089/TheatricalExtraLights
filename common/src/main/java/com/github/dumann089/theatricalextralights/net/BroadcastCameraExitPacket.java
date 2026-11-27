package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.entities.BroadcastCameraEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.function.Supplier;

/** Cliente → servidor: o operador apertou Esc e quer descer da câmera. */
public class BroadcastCameraExitPacket {

    private final int entityId;

    public BroadcastCameraExitPacket(int entityId) {
        this.entityId = entityId;
    }

    public static BroadcastCameraExitPacket decode(FriendlyByteBuf buf) {
        return new BroadcastCameraExitPacket(buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            if (!(contextSupplier.get().getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            Entity entity = player.level().getEntity(entityId);
            if (entity instanceof BroadcastCameraEntity camera && player.getVehicle() == camera) {
                player.stopRiding();
            }
        });
    }
}
