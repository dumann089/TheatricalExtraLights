package com.github.dumann089.theatricalextralights.entities;

import com.github.dumann089.theatricalextralights.items.Items;
import com.github.dumann089.theatricalextralights.items.ShoulderCameraItem;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Shoulder camera: a mesma câmera de broadcast, mas nos ombros de uma pessoa. Quem usa o item monta
 * nesta entidade e opera igual à câmera de broadcast (mouse = pan/tilt, WASD = andar, zoom, exposição,
 * gravações...), só que o "carrinho" é o próprio jogador. Enquanto o jogador opera, quem aparece é ele
 * mesmo (a câmera é desenhada no ombro dele); quando a gravação toca sozinha, sem ninguém montado, a
 * entidade desenha um boneco com a pele do dono carregando a câmera, para ela não ficar flutuando.
 *
 * <p>A entidade existe só enquanto é usada: some sozinha pouco depois de não ter mais operador nem
 * gravação tocando. O UUID dela é fixo por dono ({@link #idFor}), então a mesa de corte guarda sempre a
 * mesma câmera para cada jogador.
 */
public class ShoulderCameraEntity extends BroadcastCameraEntity {

    /** O ombro direito, em blocos: ao lado, à frente e acima do chão. */
    public static final float SHOULDER_RIGHT = 0.31f;
    public static final float SHOULDER_FORWARD = 0.08f;
    public static final float SHOULDER_UP = 1.55f;

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(ShoulderCameraEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    /** true enquanto uma gravação toca sozinha: só então o boneco aparece. */
    private static final EntityDataAccessor<Boolean> DATA_MANNEQUIN =
            SynchedEntityData.defineId(ShoulderCameraEntity.class, EntityDataSerializers.BOOLEAN);


    // Só do cliente: animação do boneco.
    private float limbSwing;
    private float limbAmount;
    private float limbAmountO;
    private float renderBodyYaw;
    private boolean bodyInit;

    public ShoulderCameraEntity(EntityType<? extends ShoulderCameraEntity> type, Level level) {
        super(type, level);
    }

    /** UUID fixo da shoulder camera de cada jogador. */
    public static UUID idFor(UUID owner) {
        return UUID.nameUUIDFromBytes(("theatricalextralights:shoulder_camera:" + owner)
                .getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_OWNER, Optional.empty());
        entityData.define(DATA_MANNEQUIN, false);
    }

    public Optional<UUID> getOwnerId() {
        return entityData.get(DATA_OWNER);
    }

    public void setOwner(UUID owner) {
        entityData.set(DATA_OWNER, Optional.ofNullable(owner));
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            double dx = getX() - xo;
            double dz = getZ() - zo;
            float speed = (float) Math.sqrt(dx * dx + dz * dz);
            limbAmountO = limbAmount;
            limbAmount += (Math.min(1f, speed * 4f) - limbAmount) * 0.4f;
            limbSwing += limbAmount;
        } else {
            tickServer();
        }
    }

    /**
     * Parada (sem operador nem gravação tocando) a câmera acompanha o dono e não aparece: assim ela está
     * sempre na lista do sincronizador e da mesa de corte. Some quando o dono sai do jogo ou perde o item.
     */
    private void tickServer() {
        boolean playing = isAutoPlaying();
        if (entityData.get(DATA_MANNEQUIN) != playing) {
            entityData.set(DATA_MANNEQUIN, playing);
        }
        if (isVehicle() || playing) {
            return;
        }
        ServerPlayer owner = ownerPlayer();
        if (!hasPendingPlayback() && (owner == null || (tickCount % 20 == 0 && !carriesCamera(owner)))) {
            discard();
            return;
        }
        if (owner != null) {
            moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0f);
            setDeltaMovement(Vec3.ZERO);
        }
    }

    private ServerPlayer ownerPlayer() {
        UUID id = getOwnerId().orElse(null);
        if (id == null) {
            return null;
        }
        return level().getPlayerByUUID(id) instanceof ServerPlayer player ? player : null;
    }

    private static boolean carriesCamera(ServerPlayer owner) {
        Inventory inventory = owner.getInventory();
        for (ItemStack stack : inventory.items) {
            if (stack.getItem() instanceof ShoulderCameraItem) {
                return true;
            }
        }
        for (ItemStack stack : inventory.offhand) {
            if (stack.getItem() instanceof ShoulderCameraItem) {
                return true;
            }
        }
        return false;
    }

    public boolean isMannequin() {
        return entityData.get(DATA_MANNEQUIN);
    }

    @Override
    public void advanceRender() {
        super.advanceRender();
        float target = getYRot() + getRenderYaw();
        if (!bodyInit) {
            renderBodyYaw = target;
            bodyInit = true;
        } else {
            renderBodyYaw += Mth.wrapDegrees(target - renderBodyYaw) * 0.12f;
        }
    }

    /** Para onde o boneco olha (o corpo vira atrás da câmera, devagar). */
    public float getRenderBodyYaw() {
        return renderBodyYaw;
    }

    public float getLimbSwing(float partialTick) {
        return limbSwing - limbAmount * (1f - partialTick);
    }

    public float getLimbAmount(float partialTick) {
        return Mth.lerp(partialTick, limbAmountO, limbAmount);
    }

    // ── Geometria ────────────────────────────────────────────────────────────

    @Override
    protected EntityDimensions dimensionsFor(float extension) {
        return EntityDimensions.scalable(0.6f, 1.8f);
    }

    @Override
    public float getExtensionMin() {
        return 0f;
    }

    @Override
    public float getExtensionMax() {
        return 0f;
    }

    /** O pivô da cabeça é o ombro direito (e não o topo de um tripé). */
    @Override
    public Vec3 getHeadPivot(float partialTick, float extension) {
        double yaw = Math.toRadians(getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        return getPosition(partialTick)
                .add(right.scale(SHOULDER_RIGHT))
                .add(forward.scale(SHOULDER_FORWARD))
                .add(0, SHOULDER_UP, 0);
    }

    // ── Operador ─────────────────────────────────────────────────────────────

    /** O operador fica em pé, no lugar da entidade (não sentado, nem atrás de um tripé). */
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        moveFunction.accept(passenger, getX(), getY(), getZ());
    }

    /**
     * Sem {@code @Override}: este método só existe no Forge (IForgeEntity), não no Minecraft puro. No
     * Forge ele faz o operador ficar em pé; sem o Forge, esta linha simplesmente não tem efeito.
     */
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return new Vec3(getX(), getY(), getZ());
    }

    // ── Não é um objeto no mundo: ninguém clica, empurra, bate nem pega ──────

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide() && source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            discard(); // só o vazio mata
            return true;
        }
        return false;
    }

    @Override
    protected ItemStack getDropStack() {
        return new ItemStack(Items.SHOULDER_CAMERA.get());
    }

    @Override
    protected void teleportToPlaybackOrigin(Vec3 origin, float yaw) {
        moveTo(origin.x, origin.y, origin.z, yaw, 0f);
        setDeltaMovement(Vec3.ZERO);
    }

    // ── Não é guardada no mundo ──────────────────────────────────────────────

    /** Existe só enquanto é usada: ao salvar / carregar o mundo ela não entra. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
