package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.blocks.LaserBlock;
import com.github.dumann089.theatricalextralights.client.StrobeRenderHelper;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import com.github.dumann089.theatricalextralights.laser.LaserPattern;
import dev.imabad.theatrical.api.Fixture;
import com.github.dumann089.theatricalextralights.compat.dmx.DmxFrameExtendedFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class LaserBlockEntity extends ExtraLightsLightBlockEntity implements DmxFrameExtendedFixture {
    public static final int CHANNEL_COUNT = 19;

    // Secondary/tertiary RGB
    private int red2, green2, blue2;
    private int red3, green3, blue3;

    // Pattern + animation parameters
    private int pattern;
    private int size;
    private int amplitude;
    private int speed;
    private int rotation;
    private int persistence;

    /** Arret d'urgence : coupe la sortie quel que soit le DMX, jusqu'au rearmement manuel. */
    private boolean emergencyStop;

    /**
     * Client-side persistence trail. Each entry holds a snapshot of beam endpoints
     * captured at a render frame; older entries fade and eventually drop.
     */
    private final Deque<TrailFrame> trailBuffer = new ArrayDeque<>();

    // DEBUG: limit how many consume()/load()/save() calls we log
    private int consumeLogCounter = 0;
    private int loadLogCounter = 0;
    private int saveLogCounter = 0;

    public LaserBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
        setChannelCount(CHANNEL_COUNT);
        TheatricalExtraLights.LOGGER.info("[LaserBE] constructed @{} channelCount={} (NEW 19ch class loaded)",
                blockPos, CHANNEL_COUNT);
    }

    public LaserBlockEntity(BlockPos pos, BlockState state) {
        this(BlockEntities.LASER.get(), pos, state);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.LASER.get();
    }

    @Override
    public void consume(byte[] dmxValues) {
        int start = this.getChannelStart() > 0 ? this.getChannelStart() - 1 : 0;
        byte[] v = Arrays.copyOfRange(dmxValues, start, start + this.getChannelCount());
        if (v.length < CHANNEL_COUNT) {
            return;
        }
        boolean prevAdvanced = beginDmxUpdate();
        int _pi = intensity, _pr = red, _pg = green, _pb = blue, _pf = focus, _pp = pan, _pt = tilt;
        int _pr2 = red2, _pg2 = green2, _pb2 = blue2, _pr3 = red3, _pg3 = green3, _pb3 = blue3;
        int _pattern = pattern, _size = size, _amp = amplitude, _speed = speed, _rot = rotation, _persist = persistence;

        intensity = u(v[0]);
        red = u(v[1]);
        green = u(v[2]);
        blue = u(v[3]);
        red2 = u(v[4]);
        green2 = u(v[5]);
        blue2 = u(v[6]);
        red3 = u(v[7]);
        green3 = u(v[8]);
        blue3 = u(v[9]);
        pattern = u(v[10]);
        size = u(v[11]);
        amplitude = u(v[12]);
        speed = u(v[13]);
        rotation = u(v[14]);
        pan = (int) ((u(v[15]) * 160) / 255f) - 80;
        tilt = -(int) ((u(v[16]) - 127) * 45) / 127;
        focus = u(v[17]);
        persistence = u(v[18]);
        if (consumeLogCounter < 5) {
            consumeLogCounter++;
            TheatricalExtraLights.LOGGER.info(
                    "[LaserBE@{}] consume() #{} intensity={} pattern={} size={} amp={} pan={} tilt={} channelCount={} channelStart={} v.length={}",
                    getBlockPos(), consumeLogCounter, intensity, pattern, size, amplitude, pan, tilt,
                    getChannelCount(), getChannelStart(), v.length);
        }
        boolean changed = intensity != _pi || red != _pr || green != _pg || blue != _pb || focus != _pf
                || pan != _pp || tilt != _pt
                || red2 != _pr2 || green2 != _pg2 || blue2 != _pb2
                || red3 != _pr3 || green3 != _pg3 || blue3 != _pb3
                || pattern != _pattern || size != _size || amplitude != _amp || speed != _speed
                || rotation != _rot || persistence != _persist;
        finishDmxUpdate(changed, prevAdvanced);
    }

    @Override
    public byte dmxFrameExtraType() {
        return EXTRA_TYPE_LASER;
    }

    /** 12 bytes: pattern params (6) + secondary/tertiary RGB (6). */
    @Override
    public void writeDmxFrameExtras(FriendlyByteBuf buf) {
        buf.writeByte(pattern);
        buf.writeByte(size);
        buf.writeByte(amplitude);
        buf.writeByte(speed);
        buf.writeByte(rotation);
        buf.writeByte(persistence);
        buf.writeByte(red2);
        buf.writeByte(green2);
        buf.writeByte(blue2);
        buf.writeByte(red3);
        buf.writeByte(green3);
        buf.writeByte(blue3);
    }

    @Override
    public void applyDmxFrameExtras(FriendlyByteBuf buf) {
        pattern = buf.readUnsignedByte();
        size = buf.readUnsignedByte();
        amplitude = buf.readUnsignedByte();
        speed = buf.readUnsignedByte();
        rotation = buf.readUnsignedByte();
        persistence = buf.readUnsignedByte();
        red2 = buf.readUnsignedByte();
        green2 = buf.readUnsignedByte();
        blue2 = buf.readUnsignedByte();
        red3 = buf.readUnsignedByte();
        green3 = buf.readUnsignedByte();
        blue3 = buf.readUnsignedByte();
        markClientBeamDirty();
    }

    @Override
    public void applyDmxFrameBase(int intensity, int red, int green, int blue,
                                  int prevIntensity, int prevRed, int prevGreen, int prevBlue) {
        super.applyDmxFrameBase(intensity, red, green, blue, prevIntensity, prevRed, prevGreen, prevBlue);
        markClientBeamDirty();
    }

    @Override
    public void applyDmxFramePanTiltFocus(int pan, int tilt, int focus,
                                          int prevPan, int prevTilt, int prevFocus) {
        super.applyDmxFramePanTiltFocus(pan, tilt, focus, prevPan, prevTilt, prevFocus);
        markClientBeamDirty();
    }

    /** Batch DMX does not trigger block entity packets — force Sodium to re-run lazy beam render. */
    private void markClientBeamDirty() {
        if (level != null && level.isClientSide) {
            StrobeRenderHelper.markSectionDirty(getBlockPos());
        }
    }

    @Override
    public int getDeviceTypeId() {
        return 0x01;
    }

    @Override
    public String getModelName() {
        return "Laser";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.LASER.getId();
    }

    @Override
    public int getActivePersonality() {
        return 0;
    }

    @Override
    public boolean isUpsideDown() {
        return getBlockState().getValue(LaserBlock.HANGING) && getBlockState().getValue(LaserBlock.HANG_DIRECTION) == Direction.UP;
    }

    @Override
    public int getBasePan() {
        return 0;
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.laser";
    }

    /**
     * Overrides Theatrical's NBTStorage.write — this is the path used by both disk
     * saves (via BaseBlockEntity.method_11007 → write) AND client sync packets
     * (ClientSyncBlockEntity.getUpdateTag → write). Don't override saveAdditional;
     * Theatrical's chain skips it for sync.
     */
    @Override
    public void write(CompoundTag tag) {
        super.write(tag);
        tag.putInt("Red2", red2);
        tag.putInt("Green2", green2);
        tag.putInt("Blue2", blue2);
        tag.putInt("Red3", red3);
        tag.putInt("Green3", green3);
        tag.putInt("Blue3", blue3);
        tag.putInt("Pattern", pattern);
        tag.putInt("Size", size);
        tag.putInt("Amplitude", amplitude);
        tag.putInt("Speed", speed);
        tag.putInt("Rotation", rotation);
        tag.putInt("Persistence", persistence);
        tag.putBoolean("EmergencyStop", emergencyStop);
        if (saveLogCounter < 5) {
            saveLogCounter++;
            TheatricalExtraLights.LOGGER.info("[LaserBE@{}] write() #{} pattern={} size={} amp={} hasKey={}",
                    getBlockPos(), saveLogCounter, pattern, size, amplitude, tag.contains("Pattern"));
        }
    }

    @Override
    public void read(CompoundTag tag) {
        super.read(tag);
        setChannelCount(CHANNEL_COUNT);
        red2 = tag.getInt("Red2");
        green2 = tag.getInt("Green2");
        blue2 = tag.getInt("Blue2");
        red3 = tag.getInt("Red3");
        green3 = tag.getInt("Green3");
        blue3 = tag.getInt("Blue3");
        pattern = tag.getInt("Pattern");
        size = tag.getInt("Size");
        amplitude = tag.getInt("Amplitude");
        speed = tag.getInt("Speed");
        rotation = tag.getInt("Rotation");
        persistence = tag.getInt("Persistence");
        emergencyStop = tag.getBoolean("EmergencyStop");
        if (loadLogCounter < 5) {
            loadLogCounter++;
            TheatricalExtraLights.LOGGER.info("[LaserBE@{}] read() #{} pattern={} size={} amp={} hasKey={} side={}",
                    getBlockPos(), loadLogCounter, pattern, size, amplitude,
                    tag.contains("Pattern"), level == null ? "?" : (level.isClientSide ? "CLIENT" : "SERVER"));
        }
    }

    private static int u(byte b) {
        return Byte.toUnsignedInt(b);
    }

    @Override
    protected boolean needsContinuousClientRender() {
        return !emergencyStop && (intensity > 0 || speed > 0);
    }

    // ----- Emergency stop -----

    public boolean isEmergencyStop() {
        return emergencyStop;
    }

    /** True when the laser actually emits: DMX intensity above zero and not stopped. */
    public boolean isOutputActive() {
        return !emergencyStop && intensity > 0;
    }

    /**
     * Engage or release the emergency stop. On the server this persists and syncs to
     * clients; on the client it only updates the local copy for instant feedback.
     */
    public void setEmergencyStop(boolean stop) {
        if (emergencyStop == stop) {
            return;
        }
        emergencyStop = stop;
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        } else if (level != null) {
            StrobeRenderHelper.markSectionDirty(getBlockPos());
        }
    }

    // ----- Getters used by the renderer -----

    public int getRed2()   { return red2; }
    public int getGreen2() { return green2; }
    public int getBlue2()  { return blue2; }
    public int getRed3()   { return red3; }
    public int getGreen3() { return green3; }
    public int getBlue3()  { return blue3; }

    public int getColour2() { return (red2 << 16) | (green2 << 8) | blue2; }
    public int getColour3() { return (red3 << 16) | (green3 << 8) | blue3; }

    public int getPatternRaw()   { return pattern; }
    public int getSizeRaw()      { return size; }
    public int getAmplitudeRaw() { return amplitude; }
    public int getSpeedRaw()     { return speed; }
    public int getRotationRaw()  { return rotation; }
    public int getPersistenceRaw() { return persistence; }

    public LaserPattern getPattern() {
        return LaserPattern.fromDmx(pattern);
    }

    public Deque<TrailFrame> getTrailBuffer() {
        return trailBuffer;
    }

    /**
     * One snapshot of active beam endpoints at a given render frame, with an age
     * counter that the renderer uses to fade them out for the persistence trail.
     */
    public static final class TrailFrame {
        public final float[] yaws;
        public final float[] pitches;
        public final float[] lengths;
        public final int[] colors;
        public final boolean[] hits;
        public int ageFrames;

        public TrailFrame(int n) {
            this.yaws = new float[n];
            this.pitches = new float[n];
            this.lengths = new float[n];
            this.colors = new int[n];
            this.hits = new boolean[n];
            this.ageFrames = 0;
        }
    }
}
