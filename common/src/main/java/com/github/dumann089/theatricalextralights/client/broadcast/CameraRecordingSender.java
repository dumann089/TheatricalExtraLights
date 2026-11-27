package com.github.dumann089.theatricalextralights.client.broadcast;

import com.github.dumann089.theatricalextralights.net.CameraPlaybackPacket;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;

import java.util.List;

/** Envia uma gravação à câmera, em pedaços (o limite de um pacote é ~32 KB). */
public final class CameraRecordingSender {

    private CameraRecordingSender() {
    }

    /**
     * @param from      primeiro quadro enviado
     * @param startTick onde (dentro do que foi enviado) a reprodução começa
     * @param armOnly   true: a câmera guarda a gravação e espera o comando de começar
     */
    public static void send(int entityId, List<float[]> frames, int from, int startTick, boolean loop, boolean armOnly) {
        send(entityId, frames, from, startTick, loop, armOnly, null);
    }

    /** {@code source}, se tiver o lugar da gravação, o leva junto (a shoulder camera reaparece lá). */
    public static void send(int entityId, List<float[]> frames, int from, int startTick, boolean loop,
                            boolean armOnly, CameraRecording source) {
        net.minecraft.world.phys.Vec3 origin = source != null && source.hasOrigin
                ? new net.minecraft.world.phys.Vec3(source.originX, source.originY, source.originZ) : null;
        int total = frames.size() - from;
        if (total < 2) {
            return;
        }
        for (int chunkStart = 0; chunkStart < total; chunkStart += CameraPlaybackPacket.CHUNK_FRAMES) {
            int count = Math.min(CameraPlaybackPacket.CHUNK_FRAMES, total - chunkStart);
            float[] data = new float[count * CameraRecording.COLUMNS];
            for (int i = 0; i < count; i++) {
                System.arraycopy(frames.get(from + chunkStart + i), 0, data, i * CameraRecording.COLUMNS,
                        CameraRecording.COLUMNS);
            }
            ModNetworkHandler.CHANNEL.sendToServer(
                    new CameraPlaybackPacket(entityId, total, startTick, loop, chunkStart, data, armOnly,
                            origin, source == null ? 0f : source.originYaw, source == null ? "" : source.dimension));
        }
    }
}
