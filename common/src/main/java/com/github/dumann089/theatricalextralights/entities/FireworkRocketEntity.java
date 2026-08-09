package com.github.dumann089.theatricalextralights.entities;

import com.github.dumann089.theatricalextralights.client.firework.DetachedPyroSparks;
import com.github.dumann089.theatricalextralights.client.firework.FireworkSmokeEffects;
import com.github.dumann089.theatricalextralights.compat.FireworkLightCompat;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.firework.BurstPattern;
import com.github.dumann089.theatricalextralights.firework.FireworkColorUtil;
import com.github.dumann089.theatricalextralights.firework.FireworkPreset;
import com.github.dumann089.theatricalextralights.firework.BurstPattern;
import com.github.dumann089.theatricalextralights.firework.FireworkRenderDistances;
import com.github.dumann089.theatricalextralights.firework.FireworkRocketTracker;
import com.github.dumann089.theatricalextralights.firework.Spark;
import dev.architectury.extensions.network.EntitySpawnExtension;
import dev.architectury.networking.NetworkManager;
import dev.imabad.theatrical.api.DynamicLightProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FireworkRocketEntity extends Entity implements EntitySpawnExtension, DynamicLightProvider {
    private static final int MAX_TOTAL_TICKS = 400;

    private static final EntityDataAccessor<Boolean> DATA_EXPLODED =
            SynchedEntityData.defineId(FireworkRocketEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FADING =
            SynchedEntityData.defineId(FireworkRocketEntity.class, EntityDataSerializers.BOOLEAN);

    private FireworkPreset preset = FireworkPreset.RED_COMET;
    private BlockPos launcherPos = BlockPos.ZERO;
    private int life;
    private int burstTickIndex;
    private int fadeTicks;
    private boolean exploded;
    private boolean fading;
    private boolean burstStarted;
    private final List<Spark> sparks = new ArrayList<>();
    private boolean shimmerLightRegistered;
    private boolean daytimeFanFired;
    private boolean daytimeBurstFired;
    private int[] customColors;
    private double fadeStartY = Double.NaN;
    /** Client-only path samples for pyro fan chase afterglow. */
    private final List<Vec3> cometPathHistory = new ArrayList<>();

    public FireworkRocketEntity(EntityType<? extends FireworkRocketEntity> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
    }

    public FireworkRocketEntity(Level level, FireworkPreset preset, BlockPos launcherPos) {
        this(ModEntities.FIREWORK_ROCKET.get(), level);
        this.preset = preset;
        this.launcherPos = launcherPos.immutable();
    }

    public FireworkPreset getPreset() {
        return preset;
    }

    public void setCustomColors(int red, int green, int blue) {
        this.customColors = FireworkColorUtil.paletteFromRgb(red, green, blue);
    }

    public int getLaunchColor() {
        return customColors != null ? customColors[0] : preset.getLaunchColor();
    }

    public int[] getColors() {
        return customColors != null ? customColors : preset.getColors();
    }

    public boolean hasCustomColors() {
        return customColors != null;
    }

    public boolean isExploded() {
        return exploded;
    }

    public boolean isFading() {
        return fading;
    }

    public int getBurstTickIndex() {
        return burstTickIndex;
    }

    public int getFadeTicks() {
        return fadeTicks;
    }

    /**
     * 1.0 en vol, puis extinction progressive (descente + braises) pour les comètes.
     */
    public float getCometVisualStrength(float partialTick) {
        if (!fading) {
            return 1.0f;
        }
        BurstPattern pattern = preset.getPattern();
        int fadeMax = Math.max(1, pattern.getCometFadeTicks());
        int emberMax = Math.max(0, pattern.getCometEmberTicks());
        float ticks = fadeTicks - partialTick;

        if (ticks > 0.0f) {
            float phase = ticks / fadeMax;
            float smooth = phase * phase * (3.0f - 2.0f * phase);
            return 0.15f + 0.85f * smooth;
        }
        if (emberMax <= 0) {
            return 0.0f;
        }
        float ember = Mth.clamp((ticks + emberMax) / emberMax, 0.0f, 1.0f);
        float smooth = ember * ember * (3.0f - 2.0f * ember);
        return smooth * 0.22f;
    }

    public int getFlightLife() {
        return life;
    }

    /** Server-side max lifetime for this rocket's preset (see {@link BurstPattern#getServerHoldTicks()}). */
    public int getServerHoldTicks() {
        return preset.getPattern().getServerHoldTicks();
    }

    /**
     * True when this rocket should be removed to avoid stacking in unloaded sky chunks.
     */
    public boolean shouldForceCleanup(ServerLevel level) {
        BurstPattern pattern = getPreset().getPattern();
        int hold = pattern.getServerHoldTicks();
        if (tickCount > hold || tickCount > MAX_TOTAL_TICKS) {
            return true;
        }
        double dx = getX() - (launcherPos.getX() + 0.5);
        double dz = getZ() - (launcherPos.getZ() + 0.5);
        double maxDrift = 192.0;
        if (level instanceof ServerLevel) {
            maxDrift = FireworkRenderDistances.maxHorizontalDriftBlocks((ServerLevel) level);
        }
        if (dx * dx + dz * dz > maxDrift * maxDrift) {
            return true;
        }
        if (getY() > 316.0) {
            return true;
        }
        return tickCount > hold - 24 && !FireworkRocketTracker.hasNearbyPlayer(level, this);
    }

    public BlockPos getLauncherPos() {
        return launcherPos;
    }

    /** One-shot guard for rainbow powder fan client effect. */
    public boolean tryFireDaytimeFan() {
        if (daytimeFanFired) {
            return false;
        }
        daytimeFanFired = true;
        return true;
    }

    /** One-shot guard for Holi burst at apex. */
    public boolean tryFireDaytimeBurst() {
        if (daytimeBurstFired) {
            return false;
        }
        daytimeBurstFired = true;
        return true;
    }

    public List<Spark> getSparks() {
        return sparks;
    }

    public void addSpark(Spark spark) {
        int maxSparks = TheatricalExtraLightsConfig.getMaxSparksPerRocket();
        if (maxSparks > 0 && sparks.size() >= maxSparks) {
            return;
        }
        sparks.add(spark);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_EXPLODED, false);
        entityData.define(DATA_FADING, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        preset = FireworkPreset.byBlockId(tag.getString("Preset"));
        launcherPos = BlockPos.of(tag.getLong("LauncherPos"));
        life = tag.getInt("Life");
        burstTickIndex = tag.getInt("BurstTickIndex");
        fadeTicks = tag.getInt("FadeTicks");
        exploded = tag.getBoolean("Exploded");
        fading = tag.getBoolean("Fading");
        burstStarted = tag.getBoolean("BurstStarted");
        if (tag.contains("CustomColorCount")) {
            int count = tag.getInt("CustomColorCount");
            customColors = new int[count];
            for (int i = 0; i < count; i++) {
                customColors[i] = tag.getInt("CustomColor" + i);
            }
        } else {
            customColors = null;
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("Preset", preset.getBlockId());
        tag.putLong("LauncherPos", launcherPos.asLong());
        tag.putInt("Life", life);
        tag.putInt("BurstTickIndex", burstTickIndex);
        tag.putInt("FadeTicks", fadeTicks);
        tag.putBoolean("Exploded", exploded);
        tag.putBoolean("Fading", fading);
        tag.putBoolean("BurstStarted", burstStarted);
        if (customColors != null) {
            tag.putInt("CustomColorCount", customColors.length);
            for (int i = 0; i < customColors.length; i++) {
                tag.putInt("CustomColor" + i, customColors[i]);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        BurstPattern pattern = preset.getPattern();
        boolean clientSide = level().isClientSide;

        if (tickCount > MAX_TOTAL_TICKS) {
            releaseLight();
            if (!clientSide) {
                discard();
            }
            return;
        }

        if (!clientSide && tickCount > pattern.getServerHoldTicks()) {
            releaseLight();
            discard();
            return;
        }

        if (!clientSide && level() instanceof ServerLevel serverLevel && shouldForceCleanup(serverLevel)) {
            releaseLight();
            discard();
            return;
        }

        if (clientSide) {
            syncVisualPhaseFromNetwork(pattern);
            tickClientLight();
            tickSparks();
            if (preset == FireworkPreset.PYRO_FAN_COMET && !exploded) {
                recordCometPath();
            }

            if (!exploded && !fading) {
                pattern.onFlightTick(this, random);
                if (pattern.isDaytimePowder()) {
                    FireworkSmokeEffects.trySpawnDaytimeLaunchPlume(this, random, life);
                    FireworkSmokeEffects.trySpawnDaytimeFlightTrail(this, random, life);
                } else if (!pattern.hasInvisibleFlight()) {
                    FireworkSmokeEffects.trySpawnFlightSmoke(this, random, life);
                    FireworkSmokeEffects.trySpawnPowderParticle(this, random, life);
                }
            } else if (exploded) {
                if (!burstStarted) {
                    pattern.onBurstStart(this, random);
                    burstStarted = true;
                }
                pattern.onBurstTick(this, random, burstTickIndex);
            } else {
                pattern.onFadeTick(this, random, fadeTicks);
            }
        }

        if (exploded) {
            burstTickIndex++;
            if (clientSide && burstTickIndex >= pattern.getBurstDuration()) {
                sparks.removeIf(Spark::isDead);
                if (sparks.isEmpty() || burstTickIndex >= pattern.getBurstDuration() + 80) {
                    sparks.clear();
                    releaseLight();
                }
            }
            return;
        }

        if (fading) {
            tickFadeMotion(pattern);
            if (clientSide) {
                fadeTicks--;
                sparks.removeIf(Spark::isDead);
                int emberEnd = -pattern.getCometEmberTicks();
                if (fadeTicks <= emberEnd && sparks.isEmpty()) {
                    releaseLight();
                }
            }
            return;
        }

        life++;
        Vec3 motion = getDeltaMovement();
        double drag = pattern.getDrag();
        double gravity = pattern.getGravity();
        double wobble = pattern.getFlightWobble();
        double wx = 0.0;
        double wz = 0.0;
        if (wobble > 0.0 && life > 5) {
            double phase = life * 0.25 + getId() * 0.7;
            wx = Math.sin(phase) * wobble;
            wz = Math.cos(phase * 0.83 + getId() * 0.31) * wobble;
        }
        setDeltaMovement(
                motion.x * drag + wx,
                (motion.y - gravity) * drag,
                motion.z * drag + wz
        );
        move(MoverType.SELF, getDeltaMovement());

        boolean reachedApex = !pattern.continuesAfterApex() && getDeltaMovement().y <= 0.0 && life > 10;
        boolean endFlight = life >= pattern.getFlightLifetime() || reachedApex;
        if (endFlight) {
            if (clientSide) {
                beginClientEndPhase(pattern);
            } else {
                triggerEnd();
            }
        }
    }

    /** Client predicts burst/fade at apex so visuals do not wait on network sync. */
    private void beginClientEndPhase(BurstPattern pattern) {
        if (exploded || fading) {
            return;
        }
        if (pattern.isBurst()) {
            exploded = true;
            fading = false;
            burstTickIndex = 0;
            burstStarted = false;
            setDeltaMovement(Vec3.ZERO);
        } else if (pattern.getCometFadeTicks() > 0) {
            beginFadePhase(pattern);
        }
    }

    private void beginFadePhase(BurstPattern pattern) {
        fading = true;
        exploded = false;
        fadeTicks = pattern.getCometFadeTicks();
        if (pattern.getFadeDescentBlocks() > 0.0) {
            fadeStartY = getY();
        }
    }

    private void tickFadeMotion(BurstPattern pattern) {
        Vec3 fadeMotion = getDeltaMovement();
        double fadeDrag = 0.94;
        double maxDescent = pattern.getFadeDescentBlocks();
        if (maxDescent > 0.0) {
            if (Double.isNaN(fadeStartY)) {
                fadeStartY = getY();
            }
            int totalFade = pattern.getCometFadeTicks();
            int elapsed = totalFade - fadeTicks;
            float progress = Math.min(1.0f, Math.max(0.0f, elapsed / (float) Math.max(1, totalFade)));
            double minY = fadeStartY - maxDescent;
            double targetY = fadeStartY - maxDescent * progress;
            double dx = fadeMotion.x * fadeDrag;
            double dz = fadeMotion.z * fadeDrag;
            setPos(getX(), Math.max(minY, targetY), getZ());
            setDeltaMovement(dx, 0.0, dz);
            return;
        }
        setDeltaMovement(fadeMotion.x * fadeDrag, fadeMotion.y * fadeDrag - 0.025, fadeMotion.z * fadeDrag);
        move(MoverType.SELF, getDeltaMovement());
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        if (level().isClientSide) {
            if (teleport) {
                setPos(x, y, z);
            }
            return;
        }
        super.lerpTo(x, y, z, yRot, xRot, steps, teleport);
    }

    private void syncVisualPhaseFromNetwork(BurstPattern pattern) {
        boolean syncedExploded = entityData.get(DATA_EXPLODED);
        boolean syncedFading = entityData.get(DATA_FADING);

        if (syncedExploded) {
            if (!exploded) {
                burstTickIndex = 0;
                burstStarted = false;
                setDeltaMovement(Vec3.ZERO);
            }
            exploded = true;
            fading = false;
            return;
        }

        if (syncedFading) {
            if (!fading) {
                beginFadePhase(pattern);
            }
            fading = true;
            exploded = false;
            return;
        }
    }

    private void setSyncedExploded() {
        exploded = true;
        fading = false;
        burstTickIndex = 0;
        setDeltaMovement(Vec3.ZERO);
        entityData.set(DATA_EXPLODED, true);
        entityData.set(DATA_FADING, false);
    }

    private void setSyncedFading(BurstPattern pattern) {
        beginFadePhase(pattern);
        entityData.set(DATA_FADING, true);
        entityData.set(DATA_EXPLODED, false);
    }

    private void recordCometPath() {
        cometPathHistory.add(new Vec3(getX(), getY(), getZ()));
        int maxSamples = 48;
        if (cometPathHistory.size() > maxSamples) {
            cometPathHistory.subList(0, cometPathHistory.size() - maxSamples).clear();
        }
    }

    private void tickSparks() {
        for (Spark spark : sparks) {
            spark.tick(level());
        }
        sparks.removeIf(Spark::isDead);
    }

    private void triggerEnd() {
        if (exploded || fading) {
            return;
        }
        BurstPattern pattern = preset.getPattern();
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (pattern.isBurst()) {
            if (!pattern.isDaytimePowder()) {
                serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.BLOCKS, 1.0f, 0.95f + random.nextFloat() * 0.1f);
                if (pattern.hasCrackleSound()) {
                    serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.BLOCKS, 0.9f, 0.9f + random.nextFloat() * 0.2f);
                }
            }
            setSyncedExploded();
        } else if (pattern.getCometFadeTicks() > 0) {
            serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_BLAST_FAR, SoundSource.BLOCKS, 0.4f, 1.1f + random.nextFloat() * 0.15f);
            setSyncedFading(pattern);
        } else {
            discard();
        }
    }

    private void tickClientLight() {
        if (isRemoved()) {
            releaseLight();
            return;
        }
        if (getLightLuminance() > 0) {
            FireworkLightCompat.sync(this);
            shimmerLightRegistered = true;
        } else if (shimmerLightRegistered) {
            releaseLight();
        }
    }

    private void releaseLight() {
        if (!shimmerLightRegistered) {
            return;
        }
        FireworkLightCompat.remove(this);
        shimmerLightRegistered = false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkManager.createAddEntityPacket(this);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double max = FireworkRenderDistances.effectiveClientRenderBlocks();
        return distance < max * max;
    }

    /** Inclut la traînée d'étincelles — évite le culling quand seule la tête sort du frustum. */
    @Override
    public AABB getBoundingBoxForCulling() {
        AABB bounds = getBoundingBox().inflate(4.0);
        if (preset == FireworkPreset.PYRO_FAN_COMET) {
            bounds = bounds.minmax(new AABB(
                    launcherPos.getX() + 0.5, launcherPos.getY(), launcherPos.getZ() + 0.5,
                    getX(), getY(), getZ()
            )).inflate(6.0);
        }
        if (level().isClientSide && !sparks.isEmpty()) {
            for (Spark spark : sparks) {
                bounds = bounds.minmax(new AABB(
                        spark.x - 2.5, spark.y - 2.5, spark.z - 2.5,
                        spark.x + 2.5, spark.y + 2.5, spark.z + 2.5));
            }
            bounds = bounds.inflate(12.0);
        }
        return bounds;
    }

    @Override
    public void saveAdditionalSpawnData(FriendlyByteBuf buf) {
        buf.writeUtf(preset.getBlockId());
        buf.writeBlockPos(launcherPos);
        buf.writeBoolean(exploded);
        buf.writeBoolean(fading);
        buf.writeInt(burstTickIndex);
        buf.writeInt(fadeTicks);
        buf.writeBoolean(customColors != null);
        if (customColors != null) {
            buf.writeVarInt(customColors.length);
            for (int color : customColors) {
                buf.writeInt(color);
            }
        }
    }

    @Override
    public void loadAdditionalSpawnData(FriendlyByteBuf buf) {
        preset = FireworkPreset.byBlockId(buf.readUtf());
        launcherPos = buf.readBlockPos();
        exploded = buf.readBoolean();
        fading = buf.readBoolean();
        burstTickIndex = buf.readInt();
        fadeTicks = buf.readInt();
        if (buf.readBoolean()) {
            customColors = new int[buf.readVarInt()];
            for (int i = 0; i < customColors.length; i++) {
                customColors[i] = buf.readInt();
            }
        } else {
            customColors = null;
        }
        entityData.set(DATA_EXPLODED, exploded);
        entityData.set(DATA_FADING, fading);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level().isClientSide && preset == FireworkPreset.PYRO_FAN_COMET) {
            DetachedPyroSparks.adoptComet(
                    new ArrayList<>(sparks),
                    new ArrayList<>(cometPathHistory),
                    getX(), getY(), getZ(),
                    getLaunchColor(),
                    getCometVisualStrength(0.0f)
            );
            cometPathHistory.clear();
        }
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            FireworkRocketTracker.onRemoved(serverLevel);
        }
        releaseLight();
        FireworkLightCompat.remove(this);
        shimmerLightRegistered = false;
        super.remove(reason);
    }

    @Override
    public BlockPos getOwnerPos() {
        UUID uuid = getUUID();
        long combined = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
        int x = (int) (combined & 0xFFFF) - 32768;
        int y = 4096 + (int) ((combined >> 16) & 0x3FF);
        int z = (int) ((combined >> 28) & 0xFFFF) - 32768;
        return new BlockPos(x, y, z);
    }

    @Override
    public Vector3f getLightPos() {
        return new Vector3f((float) getX(), (float) getY(), (float) getZ());
    }

    @Override
    public Level getLightWorld() {
        return level();
    }

    @Override
    public void resetLight() {
    }

    @Override
    public int getLightLuminance() {
        if (!level().isClientSide || !TheatricalExtraLightsConfig.isFireworkDynamicLightEnabled()) {
            return 0;
        }
        BurstPattern pattern = preset.getPattern();
        if (exploded) {
            return pattern.getBurstLuminance(burstTickIndex);
        }
        if (fading) {
            return 0;
        }
        return pattern.getFlightLuminance();
    }

    @Override
    public void lightTick() {
    }

    @Override
    public boolean shouldUpdateLight() {
        return false;
    }

    @Override
    public boolean updateDynamicLight(net.minecraft.client.renderer.LevelRenderer renderer) {
        return false;
    }

    @Override
    public void scheduleTrackedChunksRebuild(net.minecraft.client.renderer.LevelRenderer renderer) {
    }

    @Override
    public int getLightColour() {
        int color = getLaunchColor();
        int luminance = getLightLuminance();
        int intensity = luminance <= 0 ? 0 : luminance * 10;
        return (intensity << 24) | color;
    }

    @Override
    public float getLightSpread() {
        BurstPattern pattern = preset.getPattern();
        if (exploded) {
            return pattern.getBurstLightSpread(burstTickIndex);
        }
        if (fading) {
            return 0.0f;
        }
        return pattern.getFlightLightSpread();
    }
}
