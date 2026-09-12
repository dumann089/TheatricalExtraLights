package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasSafetyArm;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import com.github.dumann089.theatricalextralights.pyro.ConfettiCannonEffects;
import com.github.dumann089.theatricalextralights.pyro.ConfettiCannonOrientation;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

public class ConfettiCannonBlockEntity extends ExtraLightsLightBlockEntity implements HasSafetyArm {

    @Override
    public boolean isArmed() {
        return safetyArmed;
    }

    @Override
    public void setArmed(boolean armed) {
        applySafetyArm(armed);
    }
    private int prevIntensityDm = 0;
    private int burstCooldown = 0;

    public ConfettiCannonBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.CONFETTI_CANNON.get(), pos, state);
        setChannelCount(1);
        intensity = 0;
    }

    public static <T extends net.minecraft.world.level.block.entity.BlockEntity> void tick(
            net.minecraft.world.level.Level level,
            BlockPos pos,
            BlockState state,
            T blockEntity
    ) {
        if (blockEntity instanceof ConfettiCannonBlockEntity cannon) {
            cannon.tickServer();
        }
    }

    private void tickServer() {
        if (!(level instanceof ServerLevel serverLevel) || level.isClientSide) {
            return;
        }

        int dm = Math.max(0, Math.min(255, intensity));
        float norm = dm / 255.0f;

        if (prevIntensityDm == 0 && dm > 0) {
            fire(serverLevel, norm);
        } else if (dm > 0) {
            if (burstCooldown > 0) {
                burstCooldown--;
            } else {
                int interval = Math.max(6, Math.round(Mth.lerp(norm, 28.0f, 8.0f)));
                if (dm >= 200 || level.getGameTime() % interval == 0) {
                    fire(serverLevel, norm);
                    burstCooldown = Math.max(4, interval / 2);
                }
            }
        } else {
            burstCooldown = 0;
        }

        prevIntensityDm = dm;
    }

    private void fire(ServerLevel serverLevel, float norm) {
        BlockState state = getBlockState();
        Vec3 origin = ConfettiCannonOrientation.getNozzlePosition(worldPosition, state, this);
        Vec3 direction = ConfettiCannonOrientation.getLaunchDirection(state, this);
        ConfettiCannonEffects.fireBurst(serverLevel, origin, direction, norm);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.CONFETTI_CANNON.get();
    }

    @Override
    public int getFocus() {
        return 255;
    }

    @Override
    public void consume(byte[] dmxValues) {
        int start = getChannelStart() > 0 ? getChannelStart() - 1 : 0;
        byte[] ourValues = Arrays.copyOfRange(dmxValues, start, start + getChannelCount());
        if (ourValues.length < 1) {
            return;
        }

        boolean prevAdvanced = beginDmxUpdate();
        int previous = intensity;
        intensity = safetyArmed ? Byte.toUnsignedInt(ourValues[0]) : 0;
        finishDmxUpdate(intensity != previous, prevAdvanced);
    }

    @Override
    public int getDeviceTypeId() {
        return 0x03;
    }

    @Override
    public String getModelName() {
        return "Confetti Cannon";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.CONFETTI_CANNON.getId();
    }

    @Override
    public int getActivePersonality() {
        return 0;
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.confetti_cannon";
    }
}
