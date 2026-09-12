package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.client.particle.FireworkSparkParticleOptions;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasSafetyArm;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public class GerbBlockEntity extends ExtraLightsLightBlockEntity implements HasSafetyArm {

    @Override
    public boolean isArmed() {
        return safetyArmed;
    }

    @Override
    public void setArmed(boolean armed) {
        applySafetyArm(armed);
    }
    private static final int GOLD_CORE_COLOR = 0xFFE9AE;
    private static final int GOLD_HOT_COLOR = 0xFFC451;
    private static final float MIN_SPRAY_HEIGHT = 0.6f;
    private static final float MAX_SPRAY_HEIGHT = 1.6f;
    private static final float MIN_SPREAD = 0.04f;
    private static final float MAX_SPREAD = 0.45f;

    private int soundCooldown;

    public GerbBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.GERB_GOLD.get(), pos, state);
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
        if (blockEntity instanceof GerbBlockEntity gerb) {
            gerb.tickServer();
        }
    }

    private void tickServer() {
        if (!(level instanceof ServerLevel serverLevel) || level.isClientSide || intensity <= 0) {
            soundCooldown = 0;
            return;
        }

        float intensityFactor = Math.max(0, Math.min(255, intensity)) / 255.0f;
        float sprayHeight = Mth.lerp(intensityFactor, MIN_SPRAY_HEIGHT, MAX_SPRAY_HEIGHT);
        float spread = Mth.lerp(Math.max(0, Math.min(255, tilt)) / 255.0f, MIN_SPREAD, MAX_SPREAD);

        int sparkCount = 4 + Math.round(intensityFactor * 16.0f);
        double baseX = worldPosition.getX() + 0.5;
        double baseY = worldPosition.getY() + 0.45;
        double baseZ = worldPosition.getZ() + 0.5;

        for (int i = 0; i < sparkCount; i++) {
            double theta = serverLevel.random.nextDouble() * Math.PI * 2.0;
            double radial = serverLevel.random.nextDouble() * spread;
            double upward = sprayHeight * (0.85 + serverLevel.random.nextDouble() * 0.30);
            double mx = Math.cos(theta) * radial;
            double mz = Math.sin(theta) * radial;

            int color = serverLevel.random.nextFloat() < 0.55f ? GOLD_HOT_COLOR : GOLD_CORE_COLOR;
            FireworkSparkParticleOptions spark = new FireworkSparkParticleOptions(
                    ((color >> 16) & 0xFF) / 255.0f,
                    ((color >> 8) & 0xFF) / 255.0f,
                    (color & 0xFF) / 255.0f,
                    1.4f + serverLevel.random.nextFloat() * 0.6f,
                    0.95f,
                    14 + serverLevel.random.nextInt(10),
                    0.046f,
                    true,
                    false
            );
            serverLevel.sendParticles(spark, baseX, baseY, baseZ, 1, mx, upward, mz, 1.0);
        }

        if (soundCooldown <= 0) {
            serverLevel.playSound(null, baseX, baseY, baseZ, SoundEvents.FIREWORK_ROCKET_BLAST_FAR, SoundSource.BLOCKS, 0.4f * intensityFactor, 1.4f + serverLevel.random.nextFloat() * 0.2f);
            soundCooldown = 6 + serverLevel.random.nextInt(6);
        } else {
            soundCooldown--;
        }
    }

    public Direction getFacing() {
        return getBlockState().getValue(BaseLightBlock.FACING);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.GERB_GOLD.get();
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

        intensity = safetyArmed ? Byte.toUnsignedInt(ourValues[0]) : 0;
        tilt = Byte.toUnsignedInt(ourValues[1]);
        pan = 0;
        focus = 255;
        red = (GOLD_HOT_COLOR >> 16) & 0xFF;
        green = (GOLD_HOT_COLOR >> 8) & 0xFF;
        blue = GOLD_HOT_COLOR & 0xFF;

        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        setChanged();
    }

    @Override
    public int getDeviceTypeId() {
        return 0x04;
    }

    @Override
    public String getModelName() {
        return "Gold Gerb";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.GERB_GOLD.getId();
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
