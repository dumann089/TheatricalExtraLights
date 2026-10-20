package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blocks.LaserProjectorBlock;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import com.github.dumann089.theatricalextralights.laser.dac.EtherDreamDevice;
import com.github.dumann089.theatricalextralights.laser.dac.LaserDacHub;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * World projector bound to a virtual laser DAC. Receives no DMX — the picture
 * comes from {@link LaserDacHub} (Ether Dream in V1).
 */
public class LaserProjectorBlockEntity extends ExtraLightsLightBlockEntity {

    public static final float DEFAULT_SCAN_HALF_ANGLE = 30.0f;
    public static final float DEFAULT_SCALE = 1.0f;

    private String dacId = EtherDreamDevice.DEFAULT_ID;
    private float scanHalfAngle = DEFAULT_SCAN_HALF_ANGLE;
    private float projectionScale = DEFAULT_SCALE;

    public LaserProjectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setChannelCount(0);
    }

    public LaserProjectorBlockEntity(BlockPos pos, BlockState state) {
        this(BlockEntities.LASER_PROJECTOR.get(), pos, state);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.LASER_PROJECTOR.get();
    }

    @Override
    public int getDeviceTypeId() {
        return 0x01;
    }

    @Override
    public String getModelName() {
        return "LaserProjector";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.LASER_PROJECTOR.getId();
    }

    @Override
    public int getActivePersonality() {
        return 0;
    }

    @Override
    public boolean isUpsideDown() {
        return getBlockState().getValue(LaserProjectorBlock.HANGING)
                && getBlockState().getValue(LaserProjectorBlock.HANG_DIRECTION) == Direction.UP;
    }

    @Override
    public int getBasePan() {
        return 0;
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.laser_projector";
    }

    @Override
    public void consume(byte[] dmxValues) {
        // Intentionally empty: this fixture is driven by the laser DAC, not Art-Net.
    }

    public String getDacId() {
        return dacId;
    }

    public float getScanHalfAngle() {
        return scanHalfAngle;
    }

    public float getProjectionScale() {
        return projectionScale;
    }

    public void setProjectorSettings(String dacId, float scanHalfAngle, float projectionScale) {
        this.dacId = dacId == null || dacId.isBlank() ? EtherDreamDevice.DEFAULT_ID : dacId;
        this.scanHalfAngle = Mth.clamp(scanHalfAngle, 5.0f, 60.0f);
        this.projectionScale = Mth.clamp(projectionScale, 0.25f, 3.0f);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public float effectiveScanHalfAngle() {
        return scanHalfAngle * projectionScale;
    }

    @Override
    protected boolean needsContinuousClientRender() {
        return LaserDacHub.hasActiveOutput(dacId);
    }

    public static <T extends net.minecraft.world.level.block.entity.BlockEntity> void tick(
            Level level, BlockPos pos, BlockState state, T blockEntity) {
        if (blockEntity instanceof LaserProjectorBlockEntity projector) {
            projector.lightTick();
        }
    }

    @Override
    public void write(CompoundTag tag) {
        super.write(tag);
        tag.putString("DacId", dacId);
        tag.putFloat("ScanHalfAngle", scanHalfAngle);
        tag.putFloat("ProjectionScale", projectionScale);
    }

    @Override
    public void read(CompoundTag tag) {
        super.read(tag);
        dacId = tag.contains("DacId") ? tag.getString("DacId") : EtherDreamDevice.DEFAULT_ID;
        scanHalfAngle = tag.contains("ScanHalfAngle") ? tag.getFloat("ScanHalfAngle") : DEFAULT_SCAN_HALF_ANGLE;
        projectionScale = tag.contains("ProjectionScale") ? tag.getFloat("ProjectionScale") : DEFAULT_SCALE;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putString("DacId", dacId);
        tag.putFloat("ScanHalfAngle", scanHalfAngle);
        tag.putFloat("ProjectionScale", projectionScale);
        return tag;
    }
}
