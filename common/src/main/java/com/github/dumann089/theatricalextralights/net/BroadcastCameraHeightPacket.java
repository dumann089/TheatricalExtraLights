package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.entities.BroadcastCameraEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.function.Supplier;

/** Cliente → servidor: nova altura do tripé (menu aberto com Shift + botão direito). */
public class BroadcastCameraHeightPacket {

    private static final double MAX_DISTANCE_SQR = 10.0 * 10.0;

    private final int entityId;
    private final float extension;

    public BroadcastCameraHeightPacket(int entityId, float extension) {
        this.entityId = entityId;
        this.extension = extension;
    }

    public static BroadcastCameraHeightPacket decode(FriendlyByteBuf buf) {
        return new BroadcastCameraHeightPacket(buf.readVarInt(), buf.readFloat());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeFloat(extension);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            if (!(contextSupplier.get().getPlayer() instanceof ServerPlayer player) || !Float.isFinite(extension)) {
                return;
            }
            Entity entity = player.level().getEntity(entityId);
            if (entity instanceof BroadcastCameraEntity camera && player.distanceToSqr(camera) <= MAX_DISTANCE_SQR) {
                camera.setTripodExtension(extension);
            }
        });
    }
}
