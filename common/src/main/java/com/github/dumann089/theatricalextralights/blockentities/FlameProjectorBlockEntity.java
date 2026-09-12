package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasSafetyArm;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.nbt.CompoundTag;
import dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public class FlameProjectorBlockEntity extends ExtraLightsLightBlockEntity implements HasSafetyArm {
    private static final int FLAME_HOT_COLOR = 0xFF8434;

    /** Cle d'armement : desarmee, la machine lit le DMX mais ne crache rien. */
    private boolean armed = true;

    @Override
    public boolean isArmed() {
        return armed;
    }

    @Override
    public void setArmed(boolean value) {
        if (armed == value) {
            return;
        }
        armed = value;
        if (!armed) {
            intensity = 0;
            prevIntensity = 0;
        }
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void write(CompoundTag tag) {
        super.write(tag);
        tag.putBoolean("Armed", armed);
    }

    @Override
    public void read(CompoundTag tag) {
        super.read(tag);
        armed = !tag.contains("Armed") || tag.getBoolean("Armed");
    }

    /** Longueur de flamme 0-255 (canal 2). */
    public int getFlameLengthRaw() {
        return tilt;
    }
    private static final float MIN_LENGTH = 0.4f;
    private static final float MAX_LENGTH = 1.6f;
    private static final float MIN_FLAMES = 4.0f;
    private static final float MAX_FLAMES = 14.0f;

    private int soundCooldown;

    public FlameProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.FLAME_PROJECTOR.get(), pos, state);
        setChannelCount(2);
        intensity = 0;
        tilt = 0;
    }

    public static <T extends net.minecraft.world.level.block.entity.BlockEntity> void tick(
            net.minecraft.world.level.Level level,
            BlockPos pos,
            BlockState state,
            T blockEntity
    ) {
        if (blockEntity instanceof FlameProjectorBlockEntity flame) {
            flame.tickServer();
        }
    }

    private void tickServer() {
        if (!(level instanceof ServerLevel serverLevel) || level.isClientSide || intensity <= 0) {
            soundCooldown = 0;
            return;
        }

        float intensityFactor = Math.max(0, Math.min(255, intensity)) / 255.0f;
        float lengthFactor = Math.max(0, Math.min(255, tilt)) / 255.0f;
        float length = Mth.lerp(lengthFactor, MIN_LENGTH, MAX_LENGTH);
        int flameCount = Math.round(Mth.lerp(intensityFactor, MIN_FLAMES, MAX_FLAMES));

        Direction facing = getBlockState().getValue(BaseLightBlock.FACING);
        double dirX = facing.getStepX();
        double dirZ = facing.getStepZ();
        double sideX = -dirZ;
        double sideZ = dirX;

        double baseX = worldPosition.getX() + 0.5 + dirX * 0.35;
        double baseY = worldPosition.getY() + 0.55;
        double baseZ = worldPosition.getZ() + 0.5 + dirZ * 0.35;

        for (int i = 0; i < flameCount; i++) {
            double t = serverLevel.random.nextDouble();
            double sideJitter = (serverLevel.random.nextDouble() - 0.5) * 0.18;
            double upJitter = (serverLevel.random.nextDouble() - 0.5) * 0.12;
            double speed = (0.55 + serverLevel.random.nextDouble() * 0.30) * length;

            double mx = dirX * speed + sideX * sideJitter;
            double my = upJitter + 0.05;
            double mz = dirZ * speed + sideZ * sideJitter;

            double startSpread = 0.10 * t;
            double sx = baseX + sideX * (serverLevel.random.nextDouble() - 0.5) * startSpread;
            double sy = baseY + (serverLevel.random.nextDouble() - 0.5) * startSpread;
            double sz = baseZ + sideZ * (serverLevel.random.nextDouble() - 0.5) * startSpread;

            serverLevel.sendParticles(ParticleTypes.FLAME, sx, sy, sz, 1, mx, my, mz, 0.0);
            if (serverLevel.random.nextFloat() < 0.4f) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, sx + mx * 1.4, sy + my * 1.4, sz + mz * 1.4, 1, mx * 0.4, my * 0.4 + 0.06, mz * 0.4, 0.0);
            }
        }

        if (soundCooldown <= 0) {
            serverLevel.playSound(null, baseX, baseY, baseZ, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.35f * intensityFactor, 1.6f + serverLevel.random.nextFloat() * 0.2f);
            soundCooldown = 4 + serverLevel.random.nextInt(4);
        } else {
            soundCooldown--;
        }
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.FLAME_PROJECTOR.get();
    }

    @Override
    public int getFocus() {
        return 255;
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

        intensity = armed ? Byte.toUnsignedInt(ourValues[0]) : 0;
        tilt = Byte.toUnsignedInt(ourValues[1]);
        pan = 0;
        focus = 255;
        red = (FLAME_HOT_COLOR >> 16) & 0xFF;
        green = (FLAME_HOT_COLOR >> 8) & 0xFF;
        blue = FLAME_HOT_COLOR & 0xFF;

        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        setChanged();
    }

    @Override
    public int getDeviceTypeId() {
        return 0x05;
    }

    @Override
    public String getModelName() {
        return "Flame Projector";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.FLAME_PROJECTOR.getId();
    }

    @Override
    public int getActivePersonality() {
        return 0;
    }

    @Override
    public String getTranslationKey() {
        return getBlockState().getBlock().getDescriptionId();
    }
}
