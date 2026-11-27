package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.entities.BroadcastCameraEntity;
import com.github.dumann089.theatricalextralights.items.AnimationSynchronizerItem;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Cliente → servidor: entrega à câmera (em pedaços, o pacote tem limite de tamanho) a gravação que
 * estava tocando quando o operador saiu, ou que o sincronizador quer tocar. Pode levar o lugar onde a
 * gravação foi feita (a shoulder camera reaparece lá).
 */
public class CameraPlaybackPacket {

    /** Quadros por pedaço: 700 × 9 floats ≈ 25 KB, abaixo do limite de 32 KB do pacote. */
    public static final int CHUNK_FRAMES = 700;

    private static final double MAX_DISTANCE_SQR = 16.0 * 16.0;

    private final int entityId;
    private final int totalFrames;
    private final int startTick;
    private final boolean loop;
    private final int chunkStart;
    private final float[] data;
    /** true: a câmera guarda a gravação e só toca quando chegar o comando do sincronizador. */
    private final boolean armOnly;
    /** Onde a gravação foi feita (null = tocar de onde a câmera estiver). */
    private final Vec3 origin;
    private final float originYaw;
    private final String dimension;

    public CameraPlaybackPacket(int entityId, int totalFrames, int startTick, boolean loop,
                                int chunkStart, float[] data) {
        this(entityId, totalFrames, startTick, loop, chunkStart, data, false, null, 0f, "");
    }

    public CameraPlaybackPacket(int entityId, int totalFrames, int startTick, boolean loop,
                                int chunkStart, float[] data, boolean armOnly) {
        this(entityId, totalFrames, startTick, loop, chunkStart, data, armOnly, null, 0f, "");
    }

    public CameraPlaybackPacket(int entityId, int totalFrames, int startTick, boolean loop,
                                int chunkStart, float[] data, boolean armOnly,
                                Vec3 origin, float originYaw, String dimension) {
        this.entityId = entityId;
        this.totalFrames = totalFrames;
        this.startTick = startTick;
        this.loop = loop;
        this.chunkStart = chunkStart;
        this.data = data;
        this.armOnly = armOnly;
        this.origin = origin;
        this.originYaw = originYaw;
        this.dimension = dimension == null ? "" : dimension;
    }

    public static CameraPlaybackPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int totalFrames = buf.readVarInt();
        int startTick = buf.readVarInt();
        boolean loop = buf.readBoolean();
        int chunkStart = buf.readVarInt();
        boolean armOnly = buf.readBoolean();
        Vec3 origin = null;
        float originYaw = 0f;
        String dimension = "";
        if (buf.readBoolean()) {
            origin = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            originYaw = buf.readFloat();
            dimension = buf.readUtf(64);
        }
        int length = buf.readVarInt();
        if (length < 0 || length > CHUNK_FRAMES * BroadcastCameraEntity.PLAYBACK_COLUMNS) {
            length = 0; // pacote inválido: chega vazio e o servidor descarta
        }
        float[] data = new float[length];
        for (int i = 0; i < length; i++) {
            data[i] = buf.readFloat();
        }
        return new CameraPlaybackPacket(entityId, totalFrames, startTick, loop, chunkStart, data, armOnly,
                origin, originYaw, dimension);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(totalFrames);
        buf.writeVarInt(startTick);
        buf.writeBoolean(loop);
        buf.writeVarInt(chunkStart);
        buf.writeBoolean(armOnly);
        buf.writeBoolean(origin != null);
        if (origin != null) {
            buf.writeDouble(origin.x);
            buf.writeDouble(origin.y);
            buf.writeDouble(origin.z);
            buf.writeFloat(originYaw);
            buf.writeUtf(dimension, 64);
        }
        buf.writeVarInt(data.length);
        for (float value : data) {
            buf.writeFloat(value);
        }
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            if (!(contextSupplier.get().getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            for (float value : data) {
                if (!Float.isFinite(value)) {
                    return;
                }
            }
            if (origin != null && (!Double.isFinite(origin.x) || !Double.isFinite(origin.y)
                    || !Double.isFinite(origin.z) || !Float.isFinite(originYaw))) {
                return;
            }
            Entity entity = player.level().getEntity(entityId);
            if (!(entity instanceof BroadcastCameraEntity camera) || camera.isVehicle()) {
                return; // sem ninguém operando (quem acabou de sair já não está montado)
            }
            boolean holding = AnimationSynchronizerItem.isHolding(player);
            // Quem saiu da câmera: perto dela. O sincronizador, na mão, alcança qualquer câmera carregada.
            boolean fromOperator = !armOnly && player.distanceToSqr(camera) <= MAX_DISTANCE_SQR;
            if (fromOperator || holding) {
                // Mudar o lugar da câmera só vale para quem está com o sincronizador.
                Vec3 place = holding ? origin : null;
                camera.receivePlaybackChunk(totalFrames, startTick, loop, chunkStart, data, armOnly,
                        place, originYaw, dimension);
            }
        });
    }
}
