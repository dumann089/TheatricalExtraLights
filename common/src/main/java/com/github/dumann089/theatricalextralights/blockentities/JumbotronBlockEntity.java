package com.github.dumann089.theatricalextralights.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jumbotron: um único bloco que desenha uma tela de {@code width x height} blocos (para a direita e para
 * cima, vista de frente). Não são vários blocos: é UM block entity, então aumentar a tela não custa nada
 * a mais em chunks, colisão ou block entities.
 *
 * <p>O servidor guarda qual câmera está "no ar" (a mesa de corte vinculada avisa por
 * {@link VideoSwitcherBlockEntity#setProgram}) e sincroniza com os clientes. A imagem em si é desenhada só
 * no cliente (ver {@code JumbotronFeed}), nunca trafega pela rede.
 */
public class JumbotronBlockEntity extends BlockEntity {

    public static final int MAX_WIDTH = 32;
    public static final int MAX_HEIGHT = 20;
    public static final int MAX_AREA = 400;
    public static final int QUALITY_COUNT = 4;
    /** Alcance máximo para procurar a mesa de corte automaticamente. */
    public static final double AUTO_LINK_RANGE = 256.0;

    /** Todos os jumbotrons carregados (nos dois lados; o servidor filtra por mundo). */
    private static final Set<JumbotronBlockEntity> ALL = ConcurrentHashMap.newKeySet();

    private int width = 8;
    private int height = 5;
    /** 0 = baixa, 1 = média, 2 = alta, 3 = ultra. */
    private int quality = 2;
    /** Mesa de corte fixada pelo jogador; null = a mais próxima. */
    @Nullable
    private BlockPos linkedSwitcher;

    /** Câmera no ar, vinda da mesa de corte. */
    @Nullable
    private UUID program;
    private boolean programFade;
    private float programFadeSeconds = 2f;
    /** Sobe a cada troca: o cliente usa para perceber que houve um corte novo. */
    private int programRevision;

    // ---- só cliente (não vai pro NBT) ----
    @Nullable
    public transient UUID clientShown;
    @Nullable
    public transient UUID clientFrom;
    public transient long clientFadeStartNanos;
    public transient float clientFadeSeconds;

    public JumbotronBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.JUMBOTRON.get(), pos, state);
        ALL.add(this); // o nível só é conhecido depois; quem usa filtra por getLevel()
    }

    public static Iterable<JumbotronBlockEntity> all() {
        return ALL;
    }

    // ------------------------------------------------------------------ geometria

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
    }

    /** Caixa que a tela ocupa (para culling e alcance). */
    public AABB screenBounds() {
        Direction right = facing().getCounterClockWise();
        BlockPos far = worldPosition.offset(right.getStepX() * (width - 1), height - 1, right.getStepZ() * (width - 1));
        return new AABB(worldPosition).minmax(new AABB(far)).inflate(0.5);
    }

    /**
     * Forge/Embeddium cortam o desenho do block entity por esta caixa. O padrão é só o bloco-canto, então a
     * tela sumia quando o canto saía da mira. Sem @Override de propósito: no código comum este método do Forge
     * não existe, mas em runtime (Forge) sobrescreve o padrão da interface.
     */
    public AABB getRenderBoundingBox() {
        return screenBounds();
    }

    // ------------------------------------------------------------------ configuração

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getQuality() {
        return quality;
    }

    @Nullable
    public BlockPos getLinkedSwitcher() {
        return linkedSwitcher;
    }

    /** Aplica (e sanea) uma configuração vinda do cliente. */
    public void configure(int newWidth, int newHeight, int newQuality, @Nullable BlockPos link) {
        width = Mth.clamp(newWidth, 1, MAX_WIDTH);
        height = Mth.clamp(newHeight, 1, MAX_HEIGHT);
        while (width * height > MAX_AREA) {
            if (width >= height) {
                width--;
            } else {
                height--;
            }
        }
        quality = Mth.clamp(newQuality, 0, QUALITY_COUNT - 1);
        linkedSwitcher = link;
        refreshFromSwitcher();
        syncToClients();
    }

    // ------------------------------------------------------------------ programa (mesa de corte)

    @Nullable
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

    /** Servidor: a mesa de corte trocou a câmera no ar. */
    public void applyProgram(@Nullable UUID camera, boolean fade, float fadeSeconds) {
        if (java.util.Objects.equals(program, camera)) {
            return;
        }
        program = camera;
        programFade = fade;
        programFadeSeconds = Mth.clamp(fadeSeconds, 0.2f, 10f);
        programRevision++;
        syncToClients();
    }

    /** A mesa de corte que este jumbotron segue: a fixada, ou a mais próxima. */
    @Nullable
    public VideoSwitcherBlockEntity resolveSwitcher() {
        Level lvl = level;
        if (lvl == null) {
            return null;
        }
        if (linkedSwitcher != null) {
            return findLoadedSwitcher(lvl, linkedSwitcher);
        }
        VideoSwitcherBlockEntity best = null;
        double bestDist = AUTO_LINK_RANGE * AUTO_LINK_RANGE;
        for (VideoSwitcherBlockEntity desk : VideoSwitcherBlockEntity.all()) {
            if (desk.getLevel() != lvl || desk.isRemoved()) {
                continue;
            }
            double dist = desk.getBlockPos().distSqr(worldPosition);
            if (dist <= bestDist) {
                bestDist = dist;
                best = desk;
            }
        }
        return best;
    }

    /**
     * Procura a mesa em {@code pos} SEM esperar chunk: no servidor, {@code isLoaded}/{@code getBlockEntity}
     * bloqueiam até o chunk terminar de carregar, e se este mesmo chunk está carregando (a mesa e o jumbotron
     * no mesmo chunk) o jogo espera por si mesmo e trava. {@code getChunkNow} devolve null se não está pronto.
     */
    @Nullable
    private static VideoSwitcherBlockEntity findLoadedSwitcher(Level lvl, BlockPos pos) {
        BlockEntity found = null;
        if (lvl.getChunkSource() instanceof net.minecraft.server.level.ServerChunkCache cache) {
            net.minecraft.world.level.chunk.LevelChunk chunk = cache.getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk != null) {
                found = chunk.getBlockEntity(pos, net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK);
            }
        } else if (lvl.isClientSide() && lvl.hasChunkAt(pos)) {
            found = lvl.getBlockEntity(pos);
        }
        return found instanceof VideoSwitcherBlockEntity desk ? desk : null;
    }

    /** Servidor: copia o estado atual da mesa (ao carregar, ao colocar, ao mudar a ligação). */
    public void refreshFromSwitcher() {
        if (level == null || level.isClientSide()) {
            return;
        }
        VideoSwitcherBlockEntity desk = resolveSwitcher();
        if (desk != null) {
            applyProgram(desk.getProgram(), desk.isProgramFade(), desk.getProgramFadeSeconds());
        }
    }

    // ------------------------------------------------------------------ ciclo de vida

    /**
     * O vanilla chama quando o block entity ganha o mundo, no meio do carregamento do chunk. Nada de mexer no
     * mundo aqui: copiar o programa da mesa fica para o próximo ciclo do servidor.
     */
    @Override
    public void setLevel(Level newLevel) {
        super.setLevel(newLevel);
        if (newLevel instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.getServer().execute(() -> {
                if (!isRemoved()) {
                    refreshFromSwitcher();
                }
            });
        }
    }

    @Override
    public void setRemoved() {
        ALL.remove(this);
        super.setRemoved();
    }

    private void syncToClients() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    // ------------------------------------------------------------------ NBT / sincronização

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Width", width);
        tag.putInt("Height", height);
        tag.putInt("Quality", quality);
        if (linkedSwitcher != null) {
            tag.putLong("Link", linkedSwitcher.asLong());
        }
        if (program != null) {
            tag.putUUID("Program", program);
        }
        tag.putBoolean("ProgramFade", programFade);
        tag.putFloat("ProgramFadeSeconds", programFadeSeconds);
        tag.putInt("ProgramRevision", programRevision);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        width = Mth.clamp(tag.contains("Width") ? tag.getInt("Width") : 8, 1, MAX_WIDTH);
        height = Mth.clamp(tag.contains("Height") ? tag.getInt("Height") : 5, 1, MAX_HEIGHT);
        quality = Mth.clamp(tag.contains("Quality") ? tag.getInt("Quality") : 2, 0, QUALITY_COUNT - 1);
        linkedSwitcher = tag.contains("Link") ? BlockPos.of(tag.getLong("Link")) : null;
        program = tag.hasUUID("Program") ? tag.getUUID("Program") : null;
        programFade = tag.getBoolean("ProgramFade");
        programFadeSeconds = tag.contains("ProgramFadeSeconds") ? tag.getFloat("ProgramFadeSeconds") : 2f;
        programRevision = tag.getInt("ProgramRevision");
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
