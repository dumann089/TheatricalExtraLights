package com.github.dumann089.theatricalextralights.entities;

import com.github.dumann089.theatricalextralights.items.Items;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Câmera de broadcast sobre tripé com rodas. Quem clica nela "monta": a visão vai para a lente e o
 * operador gira a cabeça (pan/tilt) e empurra o carrinho. O carrinho é simulado pelo cliente do
 * operador, como um barco — o Minecraft já sincroniza isso com o servidor.
 */
public class BroadcastCameraEntity extends Entity {

    public static final float PITCH_MIN = -60f;
    public static final float PITCH_MAX = 40f;

    /** Quanto a coluna do tripé pode descer / subir em relação à altura padrão, em blocos. */
    public static final float TRIPOD_EXT_MIN = -0.2f;
    public static final float TRIPOD_EXT_MAX = 1.0f;
    private static final float BASE_WIDTH = 0.9f;
    private static final float BASE_HEIGHT = 1.7f;

    private static final EntityDataAccessor<Float> DATA_YAW =
            SynchedEntityData.defineId(BroadcastCameraEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH =
            SynchedEntityData.defineId(BroadcastCameraEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> DATA_HEIGHT =
            SynchedEntityData.defineId(BroadcastCameraEntity.class, EntityDataSerializers.FLOAT);
    /** O que a câmera transmite (para a mesa de corte): zoom e exposição em terços de EV. */
    private static final EntityDataAccessor<Float> DATA_ZOOM =
            SynchedEntityData.defineId(BroadcastCameraEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_EXPOSURE =
            SynchedEntityData.defineId(BroadcastCameraEntity.class, EntityDataSerializers.INT);
    /** Nome dado pelo jogador (aparece nas listas) e proporção da imagem: 0 = da janela, 1 = 16:9, 2 = 4:3. */
    private static final EntityDataAccessor<String> DATA_NAME =
            SynchedEntityData.defineId(BroadcastCameraEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_ASPECT =
            SynchedEntityData.defineId(BroadcastCameraEntity.class, EntityDataSerializers.INT);

    /** Quão rápido a velocidade do carrinho alcança a velocidade pedida (0..1 por tick). */
    private static final double ACCELERATION = 0.12;
    private static final double GRAVITY = 0.08;

    /**
     * Preenchido pelo código de cliente: devolve a velocidade horizontal desejada (blocos/tick) se
     * o jogador local está operando esta câmera, ou null. Nulo no servidor dedicado.
     */
    public static volatile Function<BroadcastCameraEntity, Vec3> clientControlHook;

    /** Preenchido pelo código de cliente: abre o menu de altura do tripé (Shift + botão direito). */
    public static volatile Consumer<BroadcastCameraEntity> clientSettingsHook;

    // Suavização só do cliente, usada pelo renderer.
    private float renderHeight;
    private float renderYaw;
    private float renderPitch;
    private boolean renderInitialised;

    public BroadcastCameraEntity(EntityType<? extends BroadcastCameraEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        setMaxUpStep(0.5f);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_YAW, 0f);
        entityData.define(DATA_PITCH, 0f);
        entityData.define(DATA_HEIGHT, 0f);
        entityData.define(DATA_ZOOM, 1f);
        entityData.define(DATA_EXPOSURE, 0);
        entityData.define(DATA_NAME, "");
        entityData.define(DATA_ASPECT, 0);
    }

    // ── Altura do tripé ──────────────────────────────────────────────────────

    /** Extensão da coluna em blocos, em relação à altura padrão (negativa = mais baixa). */
    public float getTripodExtension() {
        return entityData.get(DATA_HEIGHT);
    }

    /**
     * Muda a altura do tripé. No servidor, a altura é limitada para a câmera não crescer para
     * dentro de um teto: sobe só até onde a hitbox ainda cabe.
     */
    public void setTripodExtension(float requested) {
        float wanted = Mth.clamp(requested, getExtensionMin(), getExtensionMax());
        if (!level().isClientSide() && wanted > getTripodExtension()) {
            while (wanted > getExtensionMin()
                    && !level().noCollision(this, dimensionsFor(wanted).makeBoundingBox(position()))) {
                wanted -= 0.05f;
            }
            wanted = Math.max(wanted, getTripodExtension());
        }
        entityData.set(DATA_HEIGHT, wanted);
    }

    protected EntityDimensions dimensionsFor(float extension) {
        return EntityDimensions.scalable(BASE_WIDTH, BASE_HEIGHT + extension);
    }

    /** Limites da extensão (coluna do tripé; na jib, o comprimento do braço). */
    public float getExtensionMin() {
        return TRIPOD_EXT_MIN;
    }

    public float getExtensionMax() {
        return TRIPOD_EXT_MAX;
    }

    /** Altura do pivô da cabeça com a coluna na posição padrão, em blocos (mesma medida do renderer). */
    public static final float HEAD_PIVOT_Y = 21.7f / 16f;

    /**
     * Pivô da cabeça em coordenadas do mundo. {@code extension} já vem suavizada pela sessão.
     * A jib sobrescreve isto: o pivô fica na ponta do braço, não em cima do tripé.
     */
    public Vec3 getHeadPivot(float partialTick, float extension) {
        return getPosition(partialTick).add(0, HEAD_PIVOT_Y + extension, 0);
    }

    /** Item devolvido ao quebrar / pegar (botão do meio) a câmera. */
    protected ItemStack getDropStack() {
        return new ItemStack(Items.BROADCAST_CAMERA.get());
    }

    // Textos do menu Shift + botão direito (a jib troca "altura do tripé" por "comprimento do braço").
    public String getSettingsTitleKey() {
        return "gui.theatricalextralights.broadcast_camera.title";
    }

    public String getSettingsValueKey() {
        return "gui.theatricalextralights.broadcast_camera.height";
    }

    /** Número mostrado no slider para uma dada extensão. */
    public float getSettingsDisplayValue(float extension) {
        return HEAD_PIVOT_Y + extension;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return dimensionsFor(getTripodExtension());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_HEIGHT.equals(accessor)) {
            refreshDimensions();
        }
    }

    // ── Pose da cabeça ───────────────────────────────────────────────────────

    /** Pan da cabeça em relação ao corpo do carrinho, em graus (sentido do yaw do Minecraft). */
    public float getHeadYaw() {
        return entityData.get(DATA_YAW);
    }

    /** Tilt da cabeça, em graus; positivo = olhando para baixo, como no Minecraft. */
    public float getHeadPitch() {
        return entityData.get(DATA_PITCH);
    }

    public void setAim(float yaw, float pitch) {
        entityData.set(DATA_YAW, Mth.wrapDegrees(yaw));
        entityData.set(DATA_PITCH, Mth.clamp(pitch, PITCH_MIN, PITCH_MAX));
    }

    public String getCameraName() {
        return entityData.get(DATA_NAME);
    }

    /** Nome curto, sem caracteres de controle. */
    public void setCameraName(String name) {
        String clean = name == null ? "" : name.replaceAll("\\p{Cntrl}", "").trim();
        entityData.set(DATA_NAME, clean.length() > 24 ? clean.substring(0, 24) : clean);
    }

    public int getAspect() {
        return entityData.get(DATA_ASPECT);
    }

    public void setAspect(int aspect) {
        entityData.set(DATA_ASPECT, Mth.clamp(aspect, 0, 2));
    }

    public float getOutputZoom() {
        return entityData.get(DATA_ZOOM);
    }

    public int getOutputExposure() {
        return entityData.get(DATA_EXPOSURE);
    }

    /** Zoom e exposição que a câmera está transmitindo (vindos do operador ou de uma gravação). */
    public void setOutput(float zoom, int exposureThirds) {
        entityData.set(DATA_ZOOM, Mth.clamp(zoom, 1f, 100f));
        entityData.set(DATA_EXPOSURE, Mth.clamp(exposureThirds, -9, 9));
    }

    /** O tally fica aceso enquanto alguém opera a câmera. */
    public boolean isLive() {
        return !getPassengers().isEmpty();
    }

    /** Chamado pelo renderer a cada frame: aproxima a pose desenhada da pose recebida. */
    public void advanceRender() {
        float yaw = getHeadYaw();
        float pitch = getHeadPitch();
        float height = getTripodExtension();
        if (!renderInitialised) {
            renderYaw = yaw;
            renderPitch = pitch;
            renderHeight = height;
            renderInitialised = true;
            return;
        }
        renderHeight += (height - renderHeight) * 0.3f;
        renderYaw += Mth.wrapDegrees(yaw - renderYaw) * 0.35f;
        renderPitch += (pitch - renderPitch) * 0.35f;
    }

    public float getRenderHeight() {
        return renderHeight;
    }

    public float getRenderYaw() {
        return renderYaw;
    }

    public float getRenderPitch() {
        return renderPitch;
    }

    // ── Salvamento / rede ────────────────────────────────────────────────────

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setAim(tag.getFloat("HeadYaw"), tag.getFloat("HeadPitch"));
        entityData.set(DATA_HEIGHT, Mth.clamp(tag.getFloat("TripodHeight"), getExtensionMin(), getExtensionMax()));
        setCameraName(tag.getString("CameraName"));
        setAspect(tag.getInt("Aspect"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("HeadYaw", getHeadYaw());
        tag.putFloat("HeadPitch", getHeadPitch());
        tag.putFloat("TripodHeight", getTripodExtension());
        tag.putString("CameraName", getCameraName());
        tag.putInt("Aspect", getAspect());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkManager.createAddEntityPacket(this);
    }

    // ── Interação ────────────────────────────────────────────────────────────

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public ItemStack getPickResult() {
        return getDropStack();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            // Shift + botão direito: menu da altura do tripé (só existe no cliente).
            if (level().isClientSide()) {
                Consumer<BroadcastCameraEntity> hook = clientSettingsHook;
                if (hook != null) {
                    hook.accept(this);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        if (!getPassengers().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide()) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || isRemoved()) {
            return false;
        }
        if (!level().isClientSide() && source.getEntity() instanceof Player player) {
            if (!player.getAbilities().instabuild) {
                spawnAtLocation(getDropStack());
            }
            discard();
            return true;
        }
        return false;
    }

    // ── Operador (passageiro) ────────────────────────────────────────────────

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.0;
    }

    /** O operador fica em pé atrás do carrinho (em relação à direção em que ele foi colocado). */
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        double rad = Math.toRadians(getYRot());
        double backX = Math.sin(rad) * 1.0;
        double backZ = -Math.cos(rad) * 1.0;
        moveFunction.accept(passenger, getX() + backX, getY(), getZ() + backZ);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Direction behind = Direction.fromYRot(getYRot()).getOpposite();
        return new Vec3(getX() + behind.getStepX() * 1.2, getY(), getZ() + behind.getStepZ() * 1.2);
    }

    // ── Movimento ────────────────────────────────────────────────────────────

    // ── Reprodução automática (continua depois que o operador sai) ───────────

    /** Colunas de cada quadro gravado (mesmo formato das gravações do cliente). */
    public static final int PLAYBACK_COLUMNS = 9;
    public static final int MAX_PLAYBACK_FRAMES = 20 * 60 * 30;
    private static final int PB_YAW = 0;
    private static final int PB_PITCH = 1;
    private static final int PB_ZOOM = 2;
    private static final int PB_EXPOSURE = 3;
    private static final int PB_EXTENSION = 6;
    private static final int PB_MOVE_X = 7;
    private static final int PB_MOVE_Z = 8;

    // Só no servidor: a gravação chega em pedaços e, completa, toca a 1 quadro por tick.
    private float[] playBuffer;
    private int playTotal;
    private int playReceived;
    private int playTick;
    private boolean playLoop;
    private boolean autoPlaying;
    /** Gravação completa guardada, esperando o comando do sincronizador. */
    private boolean armed;
    /** Onde a gravação foi feita; a shoulder camera reaparece lá ao começar. */
    private Vec3 playOrigin;
    private float playOriginYaw;
    private String playDimension = "";

    /** Recebe um pedaço da gravação; quando todos chegam, a câmera começa a tocar sozinha. */
    public void receivePlaybackChunk(int total, int startTick, boolean loop, int chunkStart, float[] data,
                                     boolean armOnly, Vec3 origin, float originYaw, String dimension) {
        if (level().isClientSide() || total < 2 || total > MAX_PLAYBACK_FRAMES
                || startTick < 0 || startTick >= total || chunkStart < 0
                || data.length == 0 || data.length % PLAYBACK_COLUMNS != 0) {
            return;
        }
        int frames = data.length / PLAYBACK_COLUMNS;
        if (chunkStart == 0) {
            autoPlaying = false;
            armed = false;
            playBuffer = new float[total * PLAYBACK_COLUMNS];
            playTotal = total;
            playReceived = 0;
        } else if (playBuffer == null || playTotal != total || playReceived != chunkStart) {
            return;
        }
        if (chunkStart + frames > total) {
            return;
        }
        System.arraycopy(data, 0, playBuffer, chunkStart * PLAYBACK_COLUMNS, data.length);
        playReceived += frames;
        if (playReceived == total) {
            playTick = startTick;
            playLoop = loop;
            playOrigin = origin;
            playOriginYaw = originYaw;
            playDimension = dimension == null ? "" : dimension;
            if (armOnly) {
                armed = true;
            } else {
                beginAutoPlayback();
            }
        }
    }

    /** Sincronizador: começa a gravação que a câmera já guardou. */
    public void startArmedPlayback() {
        if (!level().isClientSide() && armed && playBuffer != null && playReceived == playTotal) {
            armed = false;
            beginAutoPlayback();
        }
    }

    private void beginAutoPlayback() {
        autoPlaying = true;
        // Só no mesmo mundo em que foi gravada.
        if (playOrigin != null && level().dimension().location().toString().equals(playDimension)) {
            teleportToPlaybackOrigin(playOrigin, playOriginYaw);
        }
    }

    /** A shoulder camera vai para o lugar gravado; as outras tocam de onde estão. */
    protected void teleportToPlaybackOrigin(Vec3 origin, float yaw) {
    }

    public void stopAutoPlayback() {
        autoPlaying = false;
        armed = false;
        playBuffer = null;
    }

    /** Tocando sozinha ou com uma gravação guardada esperando o comando de começar. */
    public boolean hasPendingPlayback() {
        return autoPlaying || armed;
    }

    public boolean isAutoPlaying() {
        return autoPlaying;
    }

    /** Aplica o quadro atual (olhar, altura, extras) e devolve o comando de velocidade do carrinho. */
    private Vec3 autoPlaybackStep() {
        int base = playTick * PLAYBACK_COLUMNS;
        setAim(playBuffer[base + PB_YAW], playBuffer[base + PB_PITCH]);
        setTripodExtension(playBuffer[base + PB_EXTENSION]);
        applyPlaybackExtras(playBuffer, base);
        setOutput(playBuffer[base + PB_ZOOM], Math.round(playBuffer[base + PB_EXPOSURE]));
        Vec3 velocity = new Vec3(playBuffer[base + PB_MOVE_X], 0, playBuffer[base + PB_MOVE_Z]);
        playTick++;
        if (playTick >= playTotal) {
            if (playLoop) {
                playTick = 0;
            } else {
                stopAutoPlayback();
            }
        }
        return velocity;
    }

    /** Variantes com mais movimentos (o braço da jib) aplicam o resto do quadro aqui. */
    protected void applyPlaybackExtras(float[] buffer, int base) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && (autoPlaying || armed) && isVehicle()) {
            stopAutoPlayback(); // alguém montou na câmera: assume o controle
        }
        if (!isControlledByLocalInstance()) {
            return;
        }
        Vec3 target = Vec3.ZERO;
        if (!level().isClientSide() && autoPlaying) {
            target = autoPlaybackStep();
        }
        if (level().isClientSide()) {
            Function<BroadcastCameraEntity, Vec3> hook = clientControlHook;
            if (hook != null) {
                Vec3 wanted = hook.apply(this);
                if (wanted != null) {
                    target = wanted;
                }
            }
        }
        Vec3 velocity = getDeltaMovement();
        double x = velocity.x + (target.x - velocity.x) * ACCELERATION;
        double z = velocity.z + (target.z - velocity.z) * ACCELERATION;
        if (Math.abs(x) < 1.0e-4) x = 0;
        if (Math.abs(z) < 1.0e-4) z = 0;
        double y = onGround() ? 0 : velocity.y - GRAVITY;
        setDeltaMovement(x, y, z);
        move(MoverType.SELF, getDeltaMovement());
    }
}
