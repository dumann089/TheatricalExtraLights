package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasPersonality;
import com.github.dumann089.theatricalextralights.entities.FireworkRocketEntity;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import com.github.dumann089.theatricalextralights.fixtures.PyroFanFixture;
import com.github.dumann089.theatricalextralights.firework.FireworkLaunchMath;
import com.github.dumann089.theatricalextralights.firework.FireworkPreset;
import com.github.dumann089.theatricalextralights.firework.FireworkRocketTracker;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

public class PyroFanBlockEntity extends ExtraLightsLightBlockEntity implements HasPersonality {
    public static final int TUBE_COUNT = 10;
    /** Demi-largeur physique des tubes sur l'axe latéral (blocs). */
    private static final double FAN_LINE_HALF_WIDTH = 0.85;
    /** Largeur du fan dans le plan vertical (haut + gauche/droite, sans avancer). */
    private static final float FAN_LATERAL_SPREAD = 1.12f;
    /** Composante verticale commune — même inclinaison de base pour tous. */
    private static final float FAN_UP_STRENGTH = 1.32f;
    private static final float BASE_LAUNCH_SPEED = 1.68f;
    private static final float MIN_SHOTS_PER_SECOND = 0.5f;
    private static final float MAX_SHOTS_PER_SECOND = 6.0f;
    private static final int MAX_LAUNCHES_PER_TICK = TUBE_COUNT;
    private static final float MAX_ACCUMULATOR = 8.0f;
    private static final double TUBE_BASE_HEIGHT = 4.0 / 16.0;

    private final int[] tubeIntensity = new int[TUBE_COUNT];
    private final int[] prevTubeIntensity = new int[TUBE_COUNT];
    private final float[] fireAccumulator = new float[TUBE_COUNT];
    private final boolean[] pendingOneShot = new boolean[TUBE_COUNT];
    private int launchRoundRobin;
    private int activePersonalityIndex = PyroFanFixture.PERSONALITY_3CH;

    public PyroFanBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.PYRO_FAN.get(), pos, state);
        tilt = 128;
        focus = 120;
        syncChannelCountFromPersonality();
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, PyroFanBlockEntity blockEntity) {
        blockEntity.tickServer();
    }

    private void syncChannelCountFromPersonality() {
        setChannelCount(activePersonalityChannelCount());
    }

    private int activePersonalityChannelCount() {
        List<DMXPersonality> personalities = getFixture().getDMXPersonalities();
        int index = getActivePersonality();
        if (index < 0 || index >= personalities.size()) {
            return TUBE_COUNT;
        }
        return personalities.get(index).getChannelCount();
    }

    @Override
    public int getChannelCount() {
        return activePersonalityChannelCount();
    }

    private void tickServer() {
        if (!(level instanceof ServerLevel serverLevel) || level.isClientSide) {
            return;
        }
        processLaunches(serverLevel);
    }

    private void processLaunches(ServerLevel serverLevel) {
        boolean anyActive = false;
        for (int i = 0; i < TUBE_COUNT; i++) {
            if (tubeIntensity[i] > 0) {
                anyActive = true;
                break;
            }
        }
        if (!anyActive) {
            Arrays.fill(fireAccumulator, 0.0f);
            Arrays.fill(pendingOneShot, false);
            return;
        }

        int launchesThisTick = 0;

        for (int offset = 0; offset < TUBE_COUNT && launchesThisTick < MAX_LAUNCHES_PER_TICK; offset++) {
            int tube = (launchRoundRobin + offset) % TUBE_COUNT;
            if (!pendingOneShot[tube] || tubeIntensity[tube] <= 0) {
                continue;
            }
            if (launchTube(serverLevel, tube)) {
                launchesThisTick++;
                pendingOneShot[tube] = false;
            }
        }
        launchRoundRobin = (launchRoundRobin + 1) % TUBE_COUNT;

        for (int tube = 0; tube < TUBE_COUNT && launchesThisTick < MAX_LAUNCHES_PER_TICK; tube++) {
            int intensity = tubeIntensity[tube];
            if (intensity <= 0) {
                fireAccumulator[tube] = 0.0f;
                continue;
            }

            if (intensity < 2) {
                fireAccumulator[tube] = 0.0f;
                continue;
            }

            fireAccumulator[tube] += shotsPerSecond(intensity) / 20.0f;
            fireAccumulator[tube] = Math.min(fireAccumulator[tube], MAX_ACCUMULATOR);
            while (fireAccumulator[tube] >= 1.0f && launchesThisTick < MAX_LAUNCHES_PER_TICK) {
                if (!launchTube(serverLevel, tube)) {
                    break;
                }
                fireAccumulator[tube] -= 1.0f;
                launchesThisTick++;
            }
        }
    }

    private boolean launchTube(ServerLevel serverLevel, int tubeIndex) {
        if (!FireworkRocketTracker.tryRegisterLaunch(serverLevel)) {
            return false;
        }

        Vec3 spawn = getTubeLaunchPosition(tubeIndex);
        Vec3 velocity = getTubeLaunchVelocity(tubeIndex, serverLevel.random);
        FireworkRocketEntity rocket = new FireworkRocketEntity(serverLevel, FireworkPreset.PYRO_FAN_COMET, worldPosition);
        rocket.moveTo(spawn.x, spawn.y, spawn.z, 0.0f, 0.0f);
        rocket.setDeltaMovement(velocity);
        if (!serverLevel.addFreshEntity(rocket)) {
            return false;
        }
        FireworkRocketTracker.registerLaunch(serverLevel);
        serverLevel.playSound(
                null,
                spawn.x,
                spawn.y,
                spawn.z,
                SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.BLOCKS,
                0.75f,
                0.85f + serverLevel.random.nextFloat() * 0.25f
        );
        return true;
    }

    private void applyTubeIntensity(int tubeIndex, int newIntensity) {
        if (prevTubeIntensity[tubeIndex] == 0 && newIntensity > 0) {
            pendingOneShot[tubeIndex] = true;
        }
        prevTubeIntensity[tubeIndex] = newIntensity;
        tubeIntensity[tubeIndex] = newIntensity;
    }

    private float tubeLinePosition(int tubeIndex) {
        if (TUBE_COUNT <= 1) {
            return 0.0f;
        }
        float normalized = tubeIndex / (float) (TUBE_COUNT - 1);
        return Mth.lerp(normalized, -1.0f, 1.0f);
    }

    /**
     * Axe latéral du fan : perpendiculaire au facing, dans le plan horizontal.
     * Aucune composante vers l'avant du bloc.
     */
    private Vec3 fanPerpendicularUnit(Direction facing) {
        double px = -facing.getStepZ();
        double pz = facing.getStepX();
        double length = Math.sqrt(px * px + pz * pz);
        if (length < 1.0E-6) {
            return new Vec3(1.0, 0.0, 0.0);
        }
        return new Vec3(px / length, 0.0, pz / length);
    }

    private float lateralSpreadScale() {
        if (getActivePersonality() != PyroFanFixture.PERSONALITY_3CH) {
            return FAN_LATERAL_SPREAD;
        }
        float tiltNorm = (Mth.clamp(tilt, 0, 255) - 128) / 128.0f;
        return FAN_LATERAL_SPREAD * (1.0f + tiltNorm * 0.15f);
    }

    /** Direction droite : montée + écart latéral, jamais vers l'avant. */
    private Vec3 tubeLaunchDirection(int tubeIndex) {
        float linePos = tubeLinePosition(tubeIndex);
        Vec3 lateral = fanPerpendicularUnit(getLaunchFacing());
        double spread = lateralSpreadScale();
        double lx = lateral.x * linePos * spread;
        double ly = FAN_UP_STRENGTH;
        double lz = lateral.z * linePos * spread;
        return new Vec3(lx, ly, lz).normalize();
    }

    private Direction getLaunchFacing() {
        return getBlockState().getValue(BaseLightBlock.FACING);
    }

    private float getLaunchPowerMultiplier() {
        return FireworkLaunchMath.launchPowerFromDmx(focus);
    }

    private float launchSpeed() {
        return BASE_LAUNCH_SPEED
                * getLaunchPowerMultiplier()
                * FireworkPreset.PYRO_FAN_COMET.getPattern().getLaunchSpeedMultiplier();
    }

    private Vec3 getTubeLaunchPosition(int tubeIndex) {
        float linePos = tubeLinePosition(tubeIndex);
        Vec3 lateral = fanPerpendicularUnit(getLaunchFacing());
        return new Vec3(
                worldPosition.getX() + 0.5 + lateral.x * FAN_LINE_HALF_WIDTH * linePos,
                worldPosition.getY() + TUBE_BASE_HEIGHT,
                worldPosition.getZ() + 0.5 + lateral.z * FAN_LINE_HALF_WIDTH * linePos
        );
    }

    private Vec3 getTubeLaunchVelocity(int tubeIndex, net.minecraft.util.RandomSource random) {
        Vec3 direction = tubeLaunchDirection(tubeIndex);
        float speed = launchSpeed();
        Vec3 lateral = fanPerpendicularUnit(getLaunchFacing());
        double drift = (random.nextDouble() * 2.0 - 1.0) * 0.03;
        return new Vec3(
                direction.x * speed + lateral.x * drift,
                direction.y * speed,
                direction.z * speed + lateral.z * drift
        );
    }

    private static float shotsPerSecond(int intensity) {
        int clamped = Mth.clamp(intensity, 2, 255);
        return Mth.lerp((clamped - 2) / 253.0f, MIN_SHOTS_PER_SECOND, MAX_SHOTS_PER_SECOND);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.PYRO_FAN.get();
    }

    @Override
    public int getFocus() {
        return 255;
    }

    @Override
    public void consume(byte[] dmxValues) {
        int start = getChannelStart() > 0 ? getChannelStart() - 1 : 0;
        int channelFootprint = getChannelCount();
        if (start + channelFootprint > dmxValues.length) {
            return;
        }
        byte[] ourValues = Arrays.copyOfRange(dmxValues, start, start + channelFootprint);

        if (storePrev() && level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }

        if (getActivePersonality() == PyroFanFixture.PERSONALITY_3CH) {
            int allTubes = Byte.toUnsignedInt(ourValues[0]);
            for (int tube = 0; tube < TUBE_COUNT; tube++) {
                applyTubeIntensity(tube, allTubes);
            }
            tilt = ourValues.length >= 2 ? Byte.toUnsignedInt(ourValues[1]) : tilt;
            focus = ourValues.length >= 3 ? Byte.toUnsignedInt(ourValues[2]) : focus;
        } else {
            int tubesToRead = Math.min(TUBE_COUNT, ourValues.length);
            for (int tube = 0; tube < tubesToRead; tube++) {
                applyTubeIntensity(tube, Byte.toUnsignedInt(ourValues[tube]));
            }
            for (int tube = tubesToRead; tube < TUBE_COUNT; tube++) {
                applyTubeIntensity(tube, 0);
            }
        }

        intensity = 0;
        for (int value : tubeIntensity) {
            intensity = Math.max(intensity, value);
        }
        pan = 0;
        if (getActivePersonality() != PyroFanFixture.PERSONALITY_3CH) {
            tilt = 0;
            focus = 255;
        }
        red = (FireworkPreset.PYRO_FAN_COMET.getLaunchColor() >> 16) & 0xFF;
        green = (FireworkPreset.PYRO_FAN_COMET.getLaunchColor() >> 8) & 0xFF;
        blue = FireworkPreset.PYRO_FAN_COMET.getLaunchColor() & 0xFF;

        if (level instanceof ServerLevel serverLevel) {
            processLaunches(serverLevel);
        }

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
        return "Pyro Fan";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.PYRO_FAN.getId();
    }

    @Override
    public int getActivePersonality() {
        return activePersonalityIndex;
    }

    @Override
    public void setActivePersonality(int index) {
        if (index < 0 || index >= getFixture().getDMXPersonalities().size()) {
            return;
        }
        activePersonalityIndex = index;
        syncChannelCountFromPersonality();
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("activePersonality", activePersonalityIndex);
        tag.putIntArray("TubeIntensity", tubeIntensity);
        tag.putIntArray("PrevTubeIntensity", prevTubeIntensity);
        tag.putIntArray("FireAccumulator", encodeFloatArray(fireAccumulator));
        tag.putByteArray("PendingOneShot", toPendingBytes());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("activePersonality")) {
            setActivePersonality(tag.getInt("activePersonality"));
        } else {
            syncChannelCountFromPersonality();
        }
        if (tag.contains("TubeIntensity")) {
            int[] loaded = tag.getIntArray("TubeIntensity");
            System.arraycopy(loaded, 0, tubeIntensity, 0, Math.min(loaded.length, TUBE_COUNT));
        }
        if (tag.contains("PrevTubeIntensity")) {
            int[] loaded = tag.getIntArray("PrevTubeIntensity");
            System.arraycopy(loaded, 0, prevTubeIntensity, 0, Math.min(loaded.length, TUBE_COUNT));
        }
        if (tag.contains("FireAccumulator")) {
            float[] loaded = decodeFloatArray(tag.getIntArray("FireAccumulator"));
            for (int i = 0; i < Math.min(loaded.length, TUBE_COUNT); i++) {
                fireAccumulator[i] = loaded[i];
            }
        }
        if (tag.contains("PendingOneShot")) {
            byte[] loaded = tag.getByteArray("PendingOneShot");
            for (int i = 0; i < Math.min(loaded.length, TUBE_COUNT); i++) {
                pendingOneShot[i] = loaded[i] != 0;
            }
        }
    }

    private byte[] toPendingBytes() {
        byte[] pendingBytes = new byte[TUBE_COUNT];
        for (int i = 0; i < TUBE_COUNT; i++) {
            pendingBytes[i] = (byte) (pendingOneShot[i] ? 1 : 0);
        }
        return pendingBytes;
    }

    private static int[] encodeFloatArray(float[] values) {
        int[] encoded = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            encoded[i] = Float.floatToIntBits(values[i]);
        }
        return encoded;
    }

    private static float[] decodeFloatArray(int[] encoded) {
        float[] values = new float[encoded.length];
        for (int i = 0; i < encoded.length; i++) {
            values[i] = Float.intBitsToFloat(encoded[i]);
        }
        return values;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("activePersonality", activePersonalityIndex);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.pyro_fan";
    }
}
