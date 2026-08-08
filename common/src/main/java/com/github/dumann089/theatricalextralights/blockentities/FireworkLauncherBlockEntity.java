package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blocks.FireworkLauncherBlock;
import com.github.dumann089.theatricalextralights.entities.FireworkRocketEntity;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import com.github.dumann089.theatricalextralights.firework.FireworkPreset;
import com.github.dumann089.theatricalextralights.firework.FireworkRocketTracker;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import dev.imabad.theatrical.fixtures.SharedSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

public class FireworkLauncherBlockEntity extends ExtraLightsLightBlockEntity {
    private static final List<DMXPersonality> PERSONALITIES = List.of(
            new DMXPersonality(3, "3-Channel Firework")
                    .addSlot(SharedSlots.INTENSITY)
                    .addSlot(SharedSlots.TILT)
                    .addSlot(SharedSlots.FOCUS)
    );

    private static final float MIN_SHOTS_PER_SECOND = 1.0f;
    private static final float MAX_SHOTS_PER_SECOND = 10.0f;
    private static final float MIN_PITCH_DEGREES = 0.0f;
    private static final float MAX_PITCH_DEGREES = 180.0f;
    private static final float LAUNCH_SPEED = 2.0f;
    private static final float MIN_LAUNCH_POWER = 0.7f;
    private static final float MAX_LAUNCH_POWER = 2.6f;
    private static final int MAX_LAUNCHES_PER_TICK = 6;
    private static final float MAX_ACCUMULATOR = 8.0f;
    private static final double TUBE_LENGTH = 10.0 / 16.0;
    private static final double TUBE_BASE_HEIGHT = 4.0 / 16.0;

    protected float fireAccumulator = 0.0f;
    protected int prevIntensity = 0;
    protected boolean pendingOneShot = false;

    public FireworkLauncherBlockEntity(BlockPos pos, BlockState state) {
        this(BlockEntities.FIREWORK_LAUNCHER.get(), pos, state);
    }

    protected FireworkLauncherBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setChannelCount(3);
        intensity = 0;
        tilt = 0;
        pan = 0;
        focus = 0;
    }

    public static <T extends net.minecraft.world.level.block.entity.BlockEntity> void tick(
            net.minecraft.world.level.Level level,
            BlockPos pos,
            BlockState state,
            T blockEntity
    ) {
        if (blockEntity instanceof FireworkLauncherBlockEntity launcher) {
            launcher.tickServer();
        }
    }

    private void tickServer() {
        if (!(level instanceof ServerLevel serverLevel) || level.isClientSide) {
            return;
        }

        if (intensity <= 0) {
            fireAccumulator = 0.0f;
            pendingOneShot = false;
            return;
        }

        int launchesThisTick = 0;

        if (pendingOneShot) {
            if (launch(serverLevel)) {
                launchesThisTick++;
            }
            pendingOneShot = false;
        }


        if (getPreset().getPattern().isTriggerShot()) {
            return;
        }


        if (intensity < 2) {
            fireAccumulator = 0.0f;
            return;
        }

        fireAccumulator += getShotsPerSecond() / 20.0f;

        fireAccumulator = Math.min(fireAccumulator, MAX_ACCUMULATOR);

        while (fireAccumulator >= 1.0f && launchesThisTick < MAX_LAUNCHES_PER_TICK) {
            if (!launch(serverLevel)) {
                break;
            }
            fireAccumulator -= 1.0f;
            launchesThisTick++;
        }
    }

    private boolean launch(ServerLevel serverLevel) {
        if (!FireworkRocketTracker.tryRegisterLaunch(serverLevel)) {
            return false;
        }
        Vec3 spawn = getLaunchPosition();
        Vec3 velocity = getLaunchVelocity();
        FireworkRocketEntity rocket = createRocket(serverLevel);
        rocket.moveTo(spawn.x, spawn.y, spawn.z, 0.0f, 0.0f);
        rocket.setDeltaMovement(velocity);
        if (!serverLevel.addFreshEntity(rocket)) {
            return false;
        }
        FireworkRocketTracker.registerLaunch(serverLevel);
        serverLevel.playSound(null, spawn.x, spawn.y, spawn.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 0.9f, 0.9f + serverLevel.random.nextFloat() * 0.2f);
        return true;
    }

    protected FireworkRocketEntity createRocket(ServerLevel serverLevel) {
        return new FireworkRocketEntity(serverLevel, getPreset(), worldPosition);
    }

    private Direction getLaunchFacing() {
        return getBlockState().getValue(BaseLightBlock.FACING).getClockWise();
    }

    private Vec3 getLaunchPosition() {
        Direction facing = getLaunchFacing();
        float pitchRad = getPitchDegrees() * Mth.DEG_TO_RAD;
        double tubeForward = TUBE_LENGTH * Math.cos(pitchRad);
        double tubeUp = TUBE_LENGTH * Math.sin(pitchRad);

        double x = worldPosition.getX() + 0.5 + facing.getStepX() * tubeForward;
        double y = worldPosition.getY() + TUBE_BASE_HEIGHT + tubeUp;
        double z = worldPosition.getZ() + 0.5 + facing.getStepZ() * tubeForward;
        return new Vec3(x, y, z);
    }

    private Vec3 getLaunchVelocity() {
        Direction facing = getLaunchFacing();
        float yawRad = facing.toYRot() * Mth.DEG_TO_RAD;
        float pitchRad = getPitchDegrees() * Mth.DEG_TO_RAD;
        float launchSpeed = LAUNCH_SPEED * getLaunchPowerMultiplier() * getPreset().getPattern().getLaunchSpeedMultiplier();

        double horizontal = Math.cos(pitchRad) * launchSpeed;
        double x = -Mth.sin(yawRad) * horizontal;
        double y = Mth.sin(pitchRad) * launchSpeed;
        double z = Mth.cos(yawRad) * horizontal;

        if (level != null) {
            double driftX = (level.random.nextDouble() * 2.0 - 1.0) * 0.20;
            double driftZ = (level.random.nextDouble() * 2.0 - 1.0) * 0.20;
            x += driftX;
            z += driftZ;
        }

        return new Vec3(x, y, z);
    }

    public float getPitchDegrees() {
        int dmx = Math.max(0, Math.min(255, tilt));
        return Mth.lerp(dmx / 255.0f, MIN_PITCH_DEGREES, MAX_PITCH_DEGREES);
    }

    public float getLaunchPowerMultiplier() {
        int dmx = Math.max(0, Math.min(255, focus));
        return Mth.lerp(dmx / 255.0f, MIN_LAUNCH_POWER, MAX_LAUNCH_POWER);
    }

    public float getShotsPerSecond() {
        if (intensity < 2) {
            return 0.0f;
        }
        int clamped = Math.max(2, Math.min(255, intensity));
        return Mth.lerp((clamped - 2) / 253.0f, MIN_SHOTS_PER_SECOND, MAX_SHOTS_PER_SECOND);
    }

    public FireworkPreset getPreset() {
        if (getBlockState().getBlock() instanceof FireworkLauncherBlock launcherBlock) {
            return launcherBlock.getPreset();
        }
        return FireworkPreset.RED_COMET;
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.getFireworkFixture(getPreset());
    }

    @Override
    public int getFocus() {
        return focus;
    }

    @Override
    public void consume(byte[] dmxValues) {
        int start = getChannelStart() > 0 ? getChannelStart() - 1 : 0;
        byte[] ourValues = Arrays.copyOfRange(dmxValues, start, start + getChannelCount());
        if (ourValues.length < 2) {
            return;
        }

        if (storePrev() && level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }

        int newIntensity = Byte.toUnsignedInt(ourValues[0]);
        if (prevIntensity == 0 && newIntensity > 0) {
            pendingOneShot = true;
        }
        prevIntensity = newIntensity;

        intensity = newIntensity;
        tilt = Byte.toUnsignedInt(ourValues[1]);
        focus = ourValues.length >= 3 ? Byte.toUnsignedInt(ourValues[2]) : 0;
        pan = 0;
        red = (getPreset().getLaunchColor() >> 16) & 0xFF;
        green = (getPreset().getLaunchColor() >> 8) & 0xFF;
        blue = getPreset().getLaunchColor() & 0xFF;

        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        setChanged();
    }

    @Override
    public int getDeviceTypeId() {
        return 0x03;
    }

    @Override
    public String getModelName() {
        return getPreset().getDisplayName();
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.getFireworkFixtureId(getPreset());
    }

    @Override
    public int getActivePersonality() {
        return 0;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("FireAccumulator", fireAccumulator);
        tag.putInt("PrevIntensity", prevIntensity);
        tag.putBoolean("PendingOneShot", pendingOneShot);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        setChannelCount(3);
        fireAccumulator = tag.getFloat("FireAccumulator");
        prevIntensity = tag.getInt("PrevIntensity");
        pendingOneShot = tag.getBoolean("PendingOneShot");
    }

    @Override
    public String getTranslationKey() {
        return getBlockState().getBlock().getDescriptionId();
    }
}
