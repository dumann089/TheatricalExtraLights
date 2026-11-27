package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.entities.BroadcastCameraEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.function.Supplier;

/**
 * Cliente → servidor: para onde o operador está apontando a câmera de broadcast e o que ela está
 * transmitindo (zoom e exposição), para quem vê o sinal da câmera de fora (a mesa de corte).
 */
public class BroadcastCameraAimPacket {

    private final int entityId;
    private final float yaw;
    private final float pitch;
    private final float zoom;
    private final int exposureThirds;

    public BroadcastCameraAimPacket(int entityId, float yaw, float pitch) {
        this(entityId, yaw, pitch, 1f, 0);
    }

    public BroadcastCameraAimPacket(int entityId, float yaw, float pitch, float zoom, int exposureThirds) {
        this.entityId = entityId;
        this.yaw = yaw;
        this.pitch = pitch;
        this.zoom = zoom;
        this.exposureThirds = exposureThirds;
    }

    public static BroadcastCameraAimPacket decode(FriendlyByteBuf buf) {
        return new BroadcastCameraAimPacket(buf.readVarInt(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeFloat(zoom);
        buf.writeVarInt(exposureThirds);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            if (!(contextSupplier.get().getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            if (!Float.isFinite(yaw) || !Float.isFinite(pitch) || !Float.isFinite(zoom)) {
                return;
            }
            Entity entity = player.level().getEntity(entityId);
            // Só quem está operando a câmera pode apontá-la.
            if (entity instanceof BroadcastCameraEntity camera && player.getVehicle() == camera) {
                camera.setAim(yaw, pitch);
                camera.setOutput(zoom, exposureThirds);
            }
        });
    }
}
