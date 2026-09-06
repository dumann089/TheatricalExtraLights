package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.util.FollowspotDmxHelper;
import com.github.dumann089.theatricalextralights.util.FollowspotTargetHelper;
import dev.imabad.theatrical.blockentities.ClientSyncBlockEntity;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import dev.imabad.theatrical.util.UUIDUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.UUID;

public class FollowspotConsoleBlockEntity extends ClientSyncBlockEntity {

    private UUID networkId = UUIDUtil.NULL;
    private int universe;
    private int dmxAddress = 1;

    private int intensity = 255;
    private int red = 255;
    private int green = 255;
    private int blue = 255;
    private int focus = 128;
    private int pan;
    private int tilt;
    /** When true, console only writes pan/tilt — intensity/RGB/focus stay on the desk / Art-Net. */
    private boolean panTiltOnly;

    public FollowspotConsoleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public FollowspotConsoleBlockEntity(BlockPos pos, BlockState state) {
        this(BlockEntities.FOLLOWSPOT_CONSOLE.get(), pos, state);
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public void setNetworkId(UUID networkId) {
        this.networkId = networkId == null ? UUIDUtil.NULL : networkId;
        setChanged();
    }

    public int getUniverse() {
        return universe;
    }

    public void setUniverse(int universe) {
        this.universe = Math.max(0, universe);
        setChanged();
    }

    public int getDmxAddress() {
        return dmxAddress;
    }

    public void setDmxAddress(int dmxAddress) {
        this.dmxAddress = FollowspotDmxHelper.isValidDmxAddress(dmxAddress)
                ? dmxAddress
                : Math.max(1, Math.min(FollowspotDmxHelper.MAX_DMX_ADDRESS, dmxAddress));
        setChanged();
    }

    public int getIntensity() {
        return intensity;
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public int getFocus() {
        return focus;
    }

    public int getPan() {
        return pan;
    }

    public int getTilt() {
        return tilt;
    }

    public boolean isPanTiltOnly() {
        return panTiltOnly;
    }

    public void setPanTiltOnly(boolean panTiltOnly) {
        this.panTiltOnly = panTiltOnly;
        setChanged();
    }

    public void setControlState(int intensity, int red, int green, int blue, int focus, int pan, int tilt) {
        if (!panTiltOnly) {
            this.intensity = clamp(intensity);
            this.red = clamp(red);
            this.green = clamp(green);
            this.blue = clamp(blue);
            this.focus = clamp(focus);
        }
        this.pan = FollowspotDmxHelper.quantizePan(pan);
        this.tilt = FollowspotDmxHelper.quantizeTilt(tilt);
        setChanged();
    }

    /** Persist + push BE to tracking clients (setChanged alone does not sync). */
    public void syncToClients() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    public void applyToLinkedFixture(Level level) {
        if (!FollowspotTargetHelper.isValidNetwork(networkId)) {
            return;
        }
        Optional<FollowspotTargetHelper.TargetMatch> target =
                FollowspotTargetHelper.findTarget(level, networkId, universe, dmxAddress, getBlockPos());
        if (target.isEmpty()) {
            return;
        }

        BaseLightBlockEntity light = target.get().fixture();
        byte[] dmx = new byte[512];
        int start = dmxAddress - 1;
        if (start < 0 || start + FollowspotTargetHelper.REQUIRED_CHANNEL_COUNT > dmx.length) {
            return;
        }

        if (panTiltOnly) {
            // Aim only — do not rewrite intensity/RGB/focus (timecode / desk owns them).
            if (light instanceof ExtraLightsLightBlockEntity extra) {
                extra.applyDirectPanTilt(pan, tilt);
            } else if (light instanceof dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity consumer) {
                dmx[start] = (byte) (int) light.getIntensity();
                dmx[start + 1] = (byte) light.getRed();
                dmx[start + 2] = (byte) light.getGreen();
                dmx[start + 3] = (byte) light.getBlue();
                dmx[start + 4] = (byte) light.getFocus();
                dmx[start + 5] = (byte) FollowspotDmxHelper.panToDmxByte(pan);
                dmx[start + 6] = (byte) FollowspotDmxHelper.tiltToDmxByte(tilt);
                consumer.consume(dmx);
            }
            return;
        }

        dmx[start] = (byte) intensity;
        dmx[start + 1] = (byte) red;
        dmx[start + 2] = (byte) green;
        dmx[start + 3] = (byte) blue;
        dmx[start + 4] = (byte) focus;
        dmx[start + 5] = (byte) FollowspotDmxHelper.panToDmxByte(pan);
        dmx[start + 6] = (byte) FollowspotDmxHelper.tiltToDmxByte(tilt);

        if (light instanceof dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity consumer) {
            consumer.consume(dmx);
        }
        if (light instanceof ExtraLightsLightBlockEntity extra) {
            extra.applyDirectControl(intensity, red, green, blue, focus, pan, tilt);
        }
    }

    public void syncFromLinkedFixture(Level level) {
        if (!FollowspotTargetHelper.isValidNetwork(networkId)) {
            return;
        }
        Optional<FollowspotTargetHelper.TargetMatch> target =
                FollowspotTargetHelper.findTarget(level, networkId, universe, dmxAddress, getBlockPos());
        if (target.isEmpty()) {
            return;
        }
        BaseLightBlockEntity light = target.get().fixture();
        intensity = (int) light.getIntensity();
        red = light.getRed();
        green = light.getGreen();
        blue = light.getBlue();
        focus = light.getFocus();
        pan = light.getPan();
        tilt = light.getTilt();
        setChanged();
    }

    @Override
    public void write(CompoundTag tag) {
        tag.putUUID("network", networkId);
        tag.putInt("universe", universe);
        tag.putInt("dmxAddress", dmxAddress);
        tag.putInt("intensity", intensity);
        tag.putInt("red", red);
        tag.putInt("green", green);
        tag.putInt("blue", blue);
        tag.putInt("focus", focus);
        tag.putInt("pan", pan);
        tag.putInt("tilt", tilt);
        tag.putBoolean("panTiltOnly", panTiltOnly);
    }

    @Override
    public void read(CompoundTag tag) {
        if (tag.contains("network")) {
            networkId = tag.getUUID("network");
        }
        universe = tag.getInt("universe");
        dmxAddress = Math.max(1, Math.min(FollowspotDmxHelper.MAX_DMX_ADDRESS, tag.getInt("dmxAddress")));
        intensity = tag.contains("intensity") ? tag.getInt("intensity") : 255;
        red = tag.contains("red") ? tag.getInt("red") : 255;
        green = tag.contains("green") ? tag.getInt("green") : 255;
        blue = tag.contains("blue") ? tag.getInt("blue") : 255;
        focus = tag.contains("focus") ? tag.getInt("focus") : 128;
        pan = tag.getInt("pan");
        tilt = tag.getInt("tilt");
        panTiltOnly = tag.contains("panTiltOnly") && tag.getBoolean("panTiltOnly");
    }
}
