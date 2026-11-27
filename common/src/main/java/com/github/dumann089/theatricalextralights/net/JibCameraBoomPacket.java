package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.entities.JibCameraEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.function.Supplier;

/** Cliente → servidor: giro e elevação do braço da jib camera, vindos do operador. */
public class JibCameraBoomPacket {

    private final int entityId;
    private final float boomYaw;
    private final float boomPitch;

    public JibCameraBoomPacket(int entityId, float boomYaw, float boomPitch) {
        this.entityId = entityId;
        this.boomYaw = boomYaw;
        this.boomPitch = boomPitch;
    }

    public static JibCameraBoomPacket decode(FriendlyByteBuf buf) {
        return new JibCameraBoomPacket(buf.readVarInt(), buf.readFloat(), buf.readFloat());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeFloat(boomYaw);
        buf.writeFloat(boomPitch);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            if (!(contextSupplier.get().getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            if (!Float.isFinite(boomYaw) || !Float.isFinite(boomPitch)) {
                return;
            }
            Entity entity = player.level().getEntity(entityId);
            // Só quem está operando a grua pode mexer no braço.
            if (entity instanceof JibCameraEntity jib && player.getVehicle() == jib) {
                jib.setBoom(boomYaw, boomPitch);
            }
        });
    }
}
