package com.github.dumann089.theatricalextralights.blockentities;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda a configuração da mesa de corte (teclas, modos, fade), a câmera que está no ar e uma sequência
 * gravada de trocas. Tudo é sincronizado com os clientes.
 *
 * <p><b>Sequência:</b> enquanto grava, cada troca do operador (corte ou fade, com a duração do fade) é
 * guardada com o tempo desde o início da gravação. Ao tocar, o SERVIDOR refaz as trocas nos mesmos instantes:
 * funciona sem ninguém operando a mesa, vale para todos os jumbotrons e para quem estiver na mesa.
 */
public class VideoSwitcherBlockEntity extends BlockEntity {

    public static final int MAX_STEPS = 512;
    public static final int MAX_LENGTH_TICKS = 20 * 60 * 60; // 1 hora

    /** Uma troca gravada: em que tick (desde o início), para qual câmera, se com fade e quanto. */
    public static final class Step {
        public final int tick;
        public final UUID camera;
        public final boolean fade;
        public final float fadeSeconds;

        public Step(int tick, UUID camera, boolean fade, float fadeSeconds) {
            this.tick = tick;
            this.camera = camera;
            this.fade = fade;
            this.fadeSeconds = fadeSeconds;
        }
    }

    /** Todas as mesas carregadas (dois lados); os jumbotrons procuram a mais próxima aqui. */
    private static final Set<VideoSwitcherBlockEntity> ALL = ConcurrentHashMap.newKeySet();
    /** Mesas tocando uma sequência agora (só servidor). */
    private static final Set<VideoSwitcherBlockEntity> PLAYING = ConcurrentHashMap.newKeySet();

    private VideoSwitcherConfig config = new VideoSwitcherConfig();

    /** Câmera que está no ar agora (o "programa" da mesa), como entrou e quanto durou o fade. */
    private UUID program;
    private boolean programFade;
    private float programFadeSeconds = 2f;
    /** Sobe a cada troca: o operador na mesa percebe trocas feitas pela sequência. */
    private int programRevision;
    private long lastProgramTick = -100L; // (não usar Long.MIN_VALUE: gameTime - MIN_VALUE estoura o long)

    // ---- sequência ----
    private final List<Step> sequence = new ArrayList<>();
    private int sequenceLength;
    private boolean loop;
    private boolean recording;
    private boolean playing;
    private long recordStartTick;
    private long playStartTick;
    private int playIndex;

    public VideoSwitcherBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.VIDEO_SWITCHER.get(), pos, state);
        ALL.add(this); // o nível só é conhecido depois; quem usa filtra por getLevel()
    }

    public static Iterable<VideoSwitcherBlockEntity> all() {
        return ALL;
    }

    /** Chamado uma vez na inicialização: o servidor faz a sequência andar. */
    public static void registerEvents() {
        TickEvent.SERVER_LEVEL_POST.register(VideoSwitcherBlockEntity::onServerLevelTick);
    }

    private static void onServerLevelTick(ServerLevel level) {
        if (PLAYING.isEmpty()) {
            return;
        }
        for (VideoSwitcherBlockEntity desk : PLAYING) {
            if (desk.isRemoved() || desk.level == null) {
                PLAYING.remove(desk);
            } else if (desk.level == level) {
                desk.tickPlayback(level.getGameTime());
            }
        }
    }

    // ------------------------------------------------------------------ estado (lido nos dois lados)

    public UUID getProgram() {
        return program;
    }

    public boolean isProgramFade() {
        return programFade;
    }

    public float getProgramFadeSeconds() {
        return programFadeSeconds;
    }

    public int getProgramRevision() {
        return programRevision;
    }

    public boolean isRecording() {
        return recording;
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isLoop() {
        return loop;
    }

    public int getSequenceSize() {
        return sequence.size();
    }

    public int getSequenceLength() {
        return sequenceLength;
    }

    public long getRecordStartTick() {
        return recordStartTick;
    }

    public long getPlayStartTick() {
        return playStartTick;
    }

    // ------------------------------------------------------------------ servidor: troca no ar

    /**
     * O operador trocou a câmera no ar. Trocas com menos de 2 ticks de intervalo são ignoradas (anti-flood).
     * Trocar na mão durante uma sequência a interrompe (o operador assumiu); durante a gravação, a troca é
     * gravada.
     */
    public void setProgram(UUID camera, boolean fade, long gameTime) {
        if (level == null || level.isClientSide() || gameTime - lastProgramTick < 2) {
            return;
        }
        lastProgramTick = gameTime;
        if (playing) {
            stopPlaying();
        }
        float seconds = config.fadeSeconds;
        if (recording && sequence.size() < MAX_STEPS) {
            long offset = gameTime - recordStartTick;
            if (offset >= 0 && offset <= MAX_LENGTH_TICKS) {
                sequence.add(new Step((int) offset, camera, fade, fade ? seconds : 0f));
            }
        }
        applyProgram(camera, fade, seconds);
    }

    /** Põe a câmera no ar: guarda, avisa os jumbotrons que seguem esta mesa e sincroniza com os clientes. */
    private void applyProgram(UUID camera, boolean fade, float seconds) {
        program = camera;
        programFade = fade;
        programFadeSeconds = seconds;
        programRevision++;
        setChanged();
        int updated = 0;
        int seen = 0;
        for (JumbotronBlockEntity jumbotron : JumbotronBlockEntity.all()) {
            if (jumbotron.getLevel() == level && !jumbotron.isRemoved()) {
                seen++;
                if (jumbotron.resolveSwitcher() == this) {
                    jumbotron.applyProgram(camera, fade, seconds);
                    updated++;
                }
            }
        }
        com.github.dumann089.theatricalextralights.JumbotronDebug.log("programa-servidor",
                "camera=" + camera + " jumbotrons no mundo=" + seen + " atualizados=" + updated);
        syncToClients();
    }

    // ------------------------------------------------------------------ servidor: sequência

    /** Começa a gravar (apaga a sequência anterior). A câmera que já está no ar vira o primeiro passo. */
    public void startRecording(long now) {
        if (level == null || level.isClientSide()) {
            return;
        }
        stopPlaying();
        sequence.clear();
        sequenceLength = 0;
        recording = true;
        recordStartTick = now;
        if (program != null) {
            sequence.add(new Step(0, program, false, 0f));
        }
        syncToClients();
    }

    public void stopRecording(long now) {
        if (!recording) {
            return;
        }
        recording = false;
        int last = sequence.isEmpty() ? 0 : sequence.get(sequence.size() - 1).tick;
        sequenceLength = (int) Math.min(MAX_LENGTH_TICKS, Math.max(last, now - recordStartTick));
        setChanged();
        syncToClients();
    }

    public void startPlaying(long now) {
        if (level == null || level.isClientSide() || sequence.isEmpty()) {
            return;
        }
        if (recording) {
            stopRecording(now);
        }
        playing = true;
        playStartTick = now;
        playIndex = 0;
        PLAYING.add(this);
        tickPlayback(now); // o primeiro passo (tick 0) entra já
        syncToClients();
    }

    public void stopPlaying() {
        if (!playing) {
            return;
        }
        playing = false;
        PLAYING.remove(this);
        syncToClients();
    }

    public void clearSequence() {
        if (level == null || level.isClientSide()) {
            return;
        }
        recording = false;
        stopPlaying();
        sequence.clear();
        sequenceLength = 0;
        setChanged();
        syncToClients();
    }

    public void toggleLoop() {
        loop = !loop;
        setChanged();
        syncToClients();
    }

    private void tickPlayback(long now) {
        long elapsed = now - playStartTick;
        while (playing && playIndex < sequence.size() && sequence.get(playIndex).tick <= elapsed) {
            Step step = sequence.get(playIndex++);
            applyProgram(step.camera, step.fade, step.fadeSeconds);
        }
        if (playing && playIndex >= sequence.size() && elapsed >= sequenceLength) {
            if (loop) {
                playStartTick = now;
                playIndex = 0;
                syncToClients();
            } else {
                stopPlaying();
            }
        }
    }

    // ------------------------------------------------------------------ ciclo de vida / config

    /** Ao carregar no servidor não dá para retomar uma gravação/reprodução que estava em andamento. */
    @Override
    public void setLevel(Level newLevel) {
        super.setLevel(newLevel);
        if (newLevel != null && !newLevel.isClientSide()) {
            recording = false;
            playing = false;
        }
    }

    @Override
    public void setRemoved() {
        ALL.remove(this);
        PLAYING.remove(this);
        super.setRemoved();
    }

    public VideoSwitcherConfig getConfig() {
        return config;
    }

    public void setConfig(VideoSwitcherConfig config) {
        this.config = config;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void syncToClients() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        config.save(tag);
        if (program != null) {
            tag.putUUID("ProgramCamera", program);
        }
        tag.putBoolean("ProgramFade", programFade);
        tag.putFloat("ProgramFadeSeconds", programFadeSeconds);
        tag.putInt("ProgramRevision", programRevision);
        tag.putBoolean("SeqLoop", loop);
        tag.putInt("SeqLength", sequenceLength);
        tag.putBoolean("SeqRecording", recording);
        tag.putBoolean("SeqPlaying", playing);
        tag.putLong("SeqRecordStart", recordStartTick);
        tag.putLong("SeqPlayStart", playStartTick);
        ListTag steps = new ListTag();
        for (Step step : sequence) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("T", step.tick);
            entry.putUUID("C", step.camera);
            entry.putBoolean("F", step.fade);
            entry.putFloat("S", step.fadeSeconds);
            steps.add(entry);
        }
        tag.put("Seq", steps);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        VideoSwitcherConfig loaded = new VideoSwitcherConfig();
        loaded.load(tag);
        config = loaded;
        program = tag.hasUUID("ProgramCamera") ? tag.getUUID("ProgramCamera") : null;
        programFade = tag.getBoolean("ProgramFade");
        programFadeSeconds = tag.contains("ProgramFadeSeconds") ? tag.getFloat("ProgramFadeSeconds") : 2f;
        programRevision = tag.getInt("ProgramRevision");
        loop = tag.getBoolean("SeqLoop");
        sequenceLength = tag.getInt("SeqLength");
        recording = tag.getBoolean("SeqRecording");
        playing = tag.getBoolean("SeqPlaying");
        recordStartTick = tag.getLong("SeqRecordStart");
        playStartTick = tag.getLong("SeqPlayStart");
        sequence.clear();
        ListTag steps = tag.getList("Seq", Tag.TAG_COMPOUND);
        for (int i = 0; i < steps.size() && i < MAX_STEPS; i++) {
            CompoundTag entry = steps.getCompound(i);
            if (entry.hasUUID("C")) {
                sequence.add(new Step(entry.getInt("T"), entry.getUUID("C"), entry.getBoolean("F"), entry.getFloat("S")));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
