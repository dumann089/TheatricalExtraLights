package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasPersonality;
import com.github.dumann089.theatricalextralights.blocks.Chcb4Block;
import com.github.dumann089.theatricalextralights.fixtures.Chcb4Fixture;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import com.github.dumann089.theatricalextralights.util.DmxShutterStrobeHelper;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.List;

public class Chcb4BlockEntity extends ExtraLightsLightBlockEntity implements HasPersonality {

    private static final int CELL_COUNT = Chcb4Fixture.CELL_COUNT;
    private static final int OPEN = 255;

    private int activePersonalityIndex = Chcb4Fixture.PERSONALITY_12CH;
    private int shutter = OPEN;
    private final int[] cellRed = new int[CELL_COUNT];
    private final int[] cellGreen = new int[CELL_COUNT];
    private final int[] cellBlue = new int[CELL_COUNT];

    public Chcb4BlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.CHCB4.get(), pos, state);
        setChannelCount(personalityChannelCount());
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.CHCB4.get();
    }

    @Override
    public int getFocus() {
        return 255;
    }

    @Override
    public int getActivePersonality() {
        return activePersonalityIndex;
    }

    @Override
    public void setActivePersonality(int index) {
        List<DMXPersonality> personalities = getFixture().getDMXPersonalities();
        if (index < 0 || index >= personalities.size()) {
            return;
        }
        activePersonalityIndex = index;
        setChannelCount(personalityChannelCount());
        if (is4ChannelMode()) {
            shutter = OPEN;
        }
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public int getChannelCount() {
        return personalityChannelCount();
    }

    @Override
    protected boolean hasExtraDmxChannelsBeyondBatch() {
        return true;
    }

    private int personalityChannelCount() {
        List<DMXPersonality> personalities = getFixture().getDMXPersonalities();
        if (personalities == null || personalities.isEmpty()) {
            return 12;
        }
        int index = Math.max(0, Math.min(activePersonalityIndex, personalities.size() - 1));
        return personalities.get(index).getChannelCount();
    }

    private boolean is4ChannelMode() {
        return activePersonalityIndex == Chcb4Fixture.PERSONALITY_4CH;
    }

    @Override
    public void consume(byte[] dmxValues) {
        int start = this.getChannelStart() > 0 ? this.getChannelStart() - 1 : 0;
        int count = getChannelCount();
        byte[] ourValues = Arrays.copyOfRange(dmxValues, start, start + count);
        if (ourValues.length < count) {
            return;
        }
        boolean prevAdvanced = beginDmxUpdate();
        boolean changed;
        if (is4ChannelMode()) {
            changed = consume4Channel(ourValues);
        } else {
            changed = consume12Channel(ourValues);
        }
        finishDmxUpdate(changed, prevAdvanced);
    }

    private boolean consume12Channel(byte[] ourValues) {
        boolean changed = false;
        int maxLevel = 0;
        int emitR = 0;
        int emitG = 0;
        int emitB = 0;
        for (int cell = 0; cell < CELL_COUNT; cell++) {
            int r = convertByteToInt(ourValues[cell * 3]);
            int g = convertByteToInt(ourValues[cell * 3 + 1]);
            int b = convertByteToInt(ourValues[cell * 3 + 2]);
            if (cellRed[cell] != r || cellGreen[cell] != g || cellBlue[cell] != b) {
                changed = true;
            }
            cellRed[cell] = r;
            cellGreen[cell] = g;
            cellBlue[cell] = b;
            int level = Math.max(r, Math.max(g, b));
            if (level > maxLevel) {
                maxLevel = level;
                emitR = r;
                emitG = g;
                emitB = b;
            }
        }
        intensity = maxLevel;
        red = emitR;
        green = emitG;
        blue = emitB;
        return changed;
    }

    private boolean consume4Channel(byte[] ourValues) {
        int r = convertByteToInt(ourValues[0]);
        int g = convertByteToInt(ourValues[1]);
        int b = convertByteToInt(ourValues[2]);
        int nextShutter = convertByteToInt(ourValues[3]);
        boolean changed = shutter != nextShutter;
        shutter = nextShutter;
        for (int cell = 0; cell < CELL_COUNT; cell++) {
            if (cellRed[cell] != r || cellGreen[cell] != g || cellBlue[cell] != b) {
                changed = true;
            }
            cellRed[cell] = r;
            cellGreen[cell] = g;
            cellBlue[cell] = b;
        }
        intensity = Math.max(r, Math.max(g, b));
        red = r;
        green = g;
        blue = b;
        return changed;
    }

    public int getCellColour(int cell) {
        if (!cellInRange(cell)) {
            return 0;
        }
        return (cellRed[cell] << 16) | (cellGreen[cell] << 8) | cellBlue[cell];
    }

    public float getCellIntensity(int cell) {
        if (!cellInRange(cell)) {
            return 0.0F;
        }
        int rgbLevel = Math.max(cellRed[cell], Math.max(cellGreen[cell], cellBlue[cell]));
        if (is4ChannelMode()) {
            long time = level != null ? level.getGameTime() : 0L;
            return DmxShutterStrobeHelper.computeEffectiveIntensity(rgbLevel, shutter, time) / 255.0F;
        }
        return rgbLevel / 255.0F;
    }

    public boolean isCellOn(int cell) {
        return getCellIntensity(cell) > 0.0F;
    }

    @Override
    protected boolean needsContinuousClientRender() {
        if (is4ChannelMode() && DmxShutterStrobeHelper.isStrobing(shutter) && intensity > 0) {
            return true;
        }
        for (int cell = 0; cell < CELL_COUNT; cell++) {
            if (isCellOn(cell)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void write(CompoundTag tag) {
        super.write(tag);
        tag.putInt("activePersonality", activePersonalityIndex);
        tag.putInt("Shutter", shutter);
        tag.putIntArray("CellRed", cellRed);
        tag.putIntArray("CellGreen", cellGreen);
        tag.putIntArray("CellBlue", cellBlue);
    }

    @Override
    public void read(CompoundTag tag) {
        super.read(tag);
        if (tag.contains("activePersonality")) {
            activePersonalityIndex = tag.getInt("activePersonality");
            setChannelCount(personalityChannelCount());
        }
        if (tag.contains("Shutter")) {
            shutter = tag.getInt("Shutter");
        }
        copyCells(tag.getIntArray("CellRed"), cellRed);
        copyCells(tag.getIntArray("CellGreen"), cellGreen);
        copyCells(tag.getIntArray("CellBlue"), cellBlue);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("activePersonality", activePersonalityIndex);
        tag.putInt("Shutter", shutter);
        tag.putIntArray("CellRed", cellRed);
        tag.putIntArray("CellGreen", cellGreen);
        tag.putIntArray("CellBlue", cellBlue);
        return tag;
    }

    private static void copyCells(int[] src, int[] dest) {
        if (src == null) {
            return;
        }
        int n = Math.min(src.length, dest.length);
        System.arraycopy(src, 0, dest, 0, n);
    }

    private static boolean cellInRange(int cell) {
        return cell >= 0 && cell < CELL_COUNT;
    }

    @Override
    public int getDeviceTypeId() {
        return 0x02;
    }

    @Override
    public String getModelName() {
        return "Color Block DB4";
    }

    @Override
    public boolean isUpsideDown() {
        return getBlockState().getValue(Chcb4Block.HANGING)
                && getBlockState().getValue(Chcb4Block.HANG_DIRECTION) == Direction.UP;
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.CHCB4.getId();
    }

    public int convertByteToInt(byte val) {
        return Byte.toUnsignedInt(val);
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.chcb4";
    }
}
