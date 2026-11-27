package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.blockentities.VideoSwitcherBlockEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/** Cliente → servidor: controle do gravador de sequências da mesa de corte. */
public class VideoSwitcherSequencePacket {

    public static final byte RECORD_START = 0;
    public static final byte RECORD_STOP = 1;
    public static final byte PLAY = 2;
    public static final byte STOP = 3;
    public static final byte CLEAR = 4;
    public static final byte TOGGLE_LOOP = 5;

    private final BlockPos pos;
    private final byte action;

    public VideoSwitcherSequencePacket(BlockPos pos, byte action) {
        this.pos = pos;
        this.action = action;
    }

    public static VideoSwitcherSequencePacket decode(FriendlyByteBuf buf) {
        return new VideoSwitcherSequencePacket(buf.readBlockPos(), buf.readByte());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(action);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            if (!(contextSupplier.get().getPlayer() instanceof ServerPlayer player)
                    || !player.level().isLoaded(pos)
                    || player.distanceToSqr(Vec3.atCenterOf(pos)) > 12.0 * 12.0
                    || !(player.level().getBlockEntity(pos) instanceof VideoSwitcherBlockEntity desk)) {
                return;
            }
            long now = player.level().getGameTime();
            switch (action) {
                case RECORD_START -> desk.startRecording(now);
                case RECORD_STOP -> desk.stopRecording(now);
                case PLAY -> desk.startPlaying(now);
                case STOP -> desk.stopPlaying();
                case CLEAR -> desk.clearSequence();
                case TOGGLE_LOOP -> desk.toggleLoop();
                default -> { }
            }
        });
    }
}
