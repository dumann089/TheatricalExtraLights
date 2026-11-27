package com.github.dumann089.theatricalextralights.entities;

import com.github.dumann089.theatricalextralights.items.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Jib camera (grua): a mesma câmera de broadcast, mas pendurada na ponta de um braço sobre um
 * tripé com rodas que NÃO anda. O operador controla o braço, não o carrinho:
 * A/D giram o braço, W/S esticam / recolhem, Espaço / Shift sobem / descem a ponta.
 * Pan, tilt, zoom e exposição da câmera continuam como na Broadcast Camera.
 *
 * <p>Estado sincronizado: o comprimento do braço reaproveita {@code tripodExtension} (em blocos,
 * em relação a {@link #ARM_BASE}); giro e elevação do braço são os dois valores novos abaixo.
 *
 * <p>As medidas em pixels de bloco (1/16) têm que bater com {@code JibCameraEntityRenderer}.
 */
public class JibCameraEntity extends BroadcastCameraEntity {

    /** Comprimento do braço à frente do pivô, com extensão 0, em blocos. */
    public static final float ARM_BASE = 2.5f;
    public static final float EXT_MIN = -0.5f;
    public static final float EXT_MAX = 4.5f;

    /** Altura do pivô do braço sobre o tripé. */
    public static final float FULCRUM_Y_PX = 30f;
    /** Elevação do braço (graus, ponta para cima): padrão e limites. */
    public static final float BOOM_PITCH_DEFAULT = 12f;
    public static final float BOOM_PITCH_MIN = -8f;
    public static final float BOOM_PITCH_MAX = 45f;
    /** Quanto o pivô da cabeça fica abaixo da ponta do braço. */
    public static final float HANG_PX = 10f;

    private static final float BODY_WIDTH = 1.0f;
    private static final float BODY_HEIGHT = 1.9f;

    private static final EntityDataAccessor<Float> DATA_BOOM_YAW =
            SynchedEntityData.defineId(JibCameraEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BOOM_PITCH =
            SynchedEntityData.defineId(JibCameraEntity.class, EntityDataSerializers.FLOAT);

    // Suavização só do cliente, usada pelo renderer.
    private float renderBoomYaw;
    private float renderBoomPitch;
    private boolean boomRenderInit;

    public JibCameraEntity(EntityType<? extends JibCameraEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_BOOM_YAW, 0f);
        entityData.define(DATA_BOOM_PITCH, BOOM_PITCH_DEFAULT);
    }

    // ── Braço ────────────────────────────────────────────────────────────────

    @Override
    public float getExtensionMin() {
        return EXT_MIN;
    }

    @Override
    public float getExtensionMax() {
        return EXT_MAX;
    }

    /** Giro do braço em relação ao carrinho, em graus (sentido do yaw do Minecraft: + = direita). */
    public float getBoomYaw() {
        return entityData.get(DATA_BOOM_YAW);
    }

    public float getBoomPitch() {
        return entityData.get(DATA_BOOM_PITCH);
    }

    public void setBoom(float yaw, float pitch) {
        entityData.set(DATA_BOOM_YAW, Mth.wrapDegrees(yaw));
        entityData.set(DATA_BOOM_PITCH, Mth.clamp(pitch, BOOM_PITCH_MIN, BOOM_PITCH_MAX));
    }

    /** Comprimento do braço à frente do pivô, em pixels de bloco. */
    public static float armLengthPx(float extension) {
        return (ARM_BASE + extension) * 16f;
    }

    @Override
    public void advanceRender() {
        super.advanceRender();
        float yaw = getBoomYaw();
        float pitch = getBoomPitch();
        if (!boomRenderInit) {
            renderBoomYaw = yaw;
            renderBoomPitch = pitch;
            boomRenderInit = true;
            return;
        }
        renderBoomYaw += Mth.wrapDegrees(yaw - renderBoomYaw) * 0.3f;
        renderBoomPitch += (pitch - renderBoomPitch) * 0.3f;
    }

    public float getRenderBoomYaw() {
        return renderBoomYaw;
    }

    public float getRenderBoomPitch() {
        return renderBoomPitch;
    }

    /** O tripé não muda de altura: a hitbox é fixa (o braço atravessa blocos, só é visual). */
    @Override
    protected EntityDimensions dimensionsFor(float extension) {
        return EntityDimensions.scalable(BODY_WIDTH, BODY_HEIGHT);
    }

    /** Sem isto a câmera some quando a base sai da tela, mesmo com o braço ainda visível. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(ARM_BASE + EXT_MAX + 1.5);
    }

    /** Pivô da cabeça com o braço como está sincronizado (quem vê de fora). */
    @Override
    public Vec3 getHeadPivot(float partialTick, float extension) {
        return getHeadPivot(partialTick, extension, getBoomYaw(), getBoomPitch());
    }

    /** Pivô da cabeça = ponta do braço, menos o pendural. O operador passa os valores dele (sem atraso de rede). */
    public Vec3 getHeadPivot(float partialTick, float extension, float boomYaw, float boomPitch) {
        double pitch = Math.toRadians(boomPitch);
        double lf = armLengthPx(extension);
        double forward = lf * Math.cos(pitch) / 16.0;
        double up = (FULCRUM_Y_PX + lf * Math.sin(pitch) - HANG_PX) / 16.0;
        double yaw = Math.toRadians(getYRot() + boomYaw);
        Vec3 front = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        return getPosition(partialTick).add(front.scale(forward)).add(0, up, 0);
    }

    @Override
    protected void applyPlaybackExtras(float[] buffer, int base) {
        setBoom(buffer[base + 4], buffer[base + 5]);
    }

    // ── Operador ─────────────────────────────────────────────────────────────

    /** Atrás do tripé fica o contrapeso: o operador fica ao lado do carrinho. */
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        double yaw = Math.toRadians(getYRot());
        double sideX = Math.cos(yaw);
        double sideZ = Math.sin(yaw);
        double backX = Math.sin(yaw);
        double backZ = -Math.cos(yaw);
        moveFunction.accept(passenger,
                getX() + sideX * 1.1 + backX * 0.3,
                getY(),
                getZ() + sideZ * 1.1 + backZ * 0.3);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        double yaw = Math.toRadians(getYRot());
        return new Vec3(getX() + Math.cos(yaw) * 1.6, getY(), getZ() + Math.sin(yaw) * 1.6);
    }

    // ── Salvamento ───────────────────────────────────────────────────────────

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setBoom(tag.contains("BoomYaw") ? tag.getFloat("BoomYaw") : 0f,
                tag.contains("BoomPitch") ? tag.getFloat("BoomPitch") : BOOM_PITCH_DEFAULT);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("BoomYaw", getBoomYaw());
        tag.putFloat("BoomPitch", getBoomPitch());
    }

    // ── Textos e item ────────────────────────────────────────────────────────

    @Override
    public String getSettingsTitleKey() {
        return "gui.theatricalextralights.jib_camera.title";
    }

    @Override
    public String getSettingsValueKey() {
        return "gui.theatricalextralights.jib_camera.length";
    }

    @Override
    public float getSettingsDisplayValue(float extension) {
        return ARM_BASE + extension;
    }

    @Override
    protected ItemStack getDropStack() {
        return new ItemStack(Items.JIB_CAMERA.get());
    }
}
