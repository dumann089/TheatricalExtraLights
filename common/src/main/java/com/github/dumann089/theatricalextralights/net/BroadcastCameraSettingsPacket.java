package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.entities.BroadcastCameraEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.function.Supplier;

/** Cliente → servidor: nome e proporção da imagem da câmera (menu Shift + botão direito). */
public class BroadcastCameraSettingsPacket {

    private static final double MAX_DISTANCE_SQR = 10.0 * 10.0;

    private final int entityId;
    private final String name;
    private final int aspect;

    public BroadcastCameraSettingsPacket(int entityId, String name, int aspect) {
        this.entityId = entityId;
        this.name = name;
        this.aspect = aspect;
    }

    public static BroadcastCameraSettingsPacket decode(FriendlyByteBuf buf) {
        return new BroadcastCameraSettingsPacket(buf.readVarInt(), buf.readUtf(32), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUtf(name, 32);
        buf.writeVarInt(aspect);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            if (!(contextSupplier.get().getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            Entity entity = player.level().getEntity(entityId);
            if (entity instanceof BroadcastCameraEntity camera && player.distanceToSqr(camera) <= MAX_DISTANCE_SQR) {
                camera.setCameraName(name);
                camera.setAspect(aspect);
            }
        });
    }
}
