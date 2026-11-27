package com.github.dumann089.theatricalextralights.net;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Servidor → cliente: as câmeras do mundo (id, variante, posição e se estão tocando sozinhas). */
public class SyncCameraListPacket {

    public record Entry(int id, boolean jib, int x, int y, int z, boolean playing, java.util.UUID uuid,
                        boolean shoulder, String owner, String name) {
        /** Chave de tradução do nome do tipo de câmera. */
        public String entityKey() {
            return shoulder ? "entity.theatricalextralights.shoulder_camera"
                    : jib ? "entity.theatricalextralights.jib_camera"
                    : "entity.theatricalextralights.broadcast_camera";
        }
    }

    /** Preenchido pelo cliente: entrega a lista ao menu aberto. */
    public static volatile Consumer<List<Entry>> clientHook;

    private static final int MAX_CAMERAS = 64;

    private final List<Entry> cameras;

    public SyncCameraListPacket(List<Entry> cameras) {
        this.cameras = cameras;
    }

    public static SyncCameraListPacket decode(FriendlyByteBuf buf) {
        int count = Math.min(buf.readVarInt(), MAX_CAMERAS);
        List<Entry> cameras = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cameras.add(new Entry(buf.readVarInt(), buf.readBoolean(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readBoolean(), buf.readUUID(), buf.readBoolean(), buf.readUtf(32), buf.readUtf(32)));
        }
        return new SyncCameraListPacket(cameras);
    }

    public void encode(FriendlyByteBuf buf) {
        int count = Math.min(cameras.size(), MAX_CAMERAS);
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            Entry entry = cameras.get(i);
            buf.writeVarInt(entry.id());
            buf.writeBoolean(entry.jib());
            buf.writeInt(entry.x());
            buf.writeInt(entry.y());
            buf.writeInt(entry.z());
            buf.writeBoolean(entry.playing());
            buf.writeUUID(entry.uuid());
            buf.writeBoolean(entry.shoulder());
            buf.writeUtf(entry.owner(), 32);
            buf.writeUtf(entry.name(), 32);
        }
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            Consumer<List<Entry>> hook = clientHook;
            if (hook != null) {
                hook.accept(cameras);
            }
        });
    }
}
