package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasPersonality;
import com.github.dumann089.theatricalextralights.client.StrobeRenderHelper;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import com.github.dumann089.theatricalextralights.util.BlockEntitySync;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * Panneau LED : un mode de base (dimmer + RGB pour tout le panneau) et deux modes pixel, une grille
 * 4 x 4 puis 8 x 8, chaque pixel avec son dimmer et son RGB. Pixels rangee par rangee depuis le
 * haut, de gauche a droite vus de face.
 */
public class LEDPanel2BlockEntity extends ExtraLightsLightBlockEntity implements HasPersonality {
    public static final int MODE_BASIC = 0;
    public static final int MODE_4X4 = 1;
    public static final int MODE_8X8 = 2;
    private static final int[] GRID = {1, 4, 8};
    public static final int MAX_GRID = 8;
    public static final int STRIDE = 4;

    private int personality = MODE_BASIC;
    /** Dim, R, G, B par pixel de la grille active. */
    private final int[] pixels = new int[MAX_GRID * MAX_GRID * STRIDE];

    public LEDPanel2BlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.LED_PANEL_2.get(), blockPos, blockState);
        setChannelCount(channelCount(personality));
    }

    public static int channelCount(int mode) {
        return mode == MODE_BASIC ? 4 : GRID[mode] * GRID[mode] * STRIDE;
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.LED_PANEL_2.get();
    }

    @Override
    public int getActivePersonality() {
        return personality;
    }

    @Override
    public void setActivePersonality(int index) {
        if (index < 0 || index >= GRID.length) {
            return;
        }
        personality = index;
        setChannelCount(channelCount(personality));
        Arrays.fill(pixels, 0);
        setChanged();
        if (level != null) {
            BlockEntitySync.sendData(this);
        }
    }

    /** Cote de la grille : 1 en mode de base. */
    public int getGrid() {
        return GRID[personality];
    }

    public boolean isPixelMode() {
        return personality != MODE_BASIC;
    }

    public int pixelDim(int p) {
        return pixels[p * STRIDE];
    }

    public int pixelColour(int p) {
        return (pixels[p * STRIDE + 1] << 16) | (pixels[p * STRIDE + 2] << 8) | pixels[p * STRIDE + 3];
    }

    @Override
    protected boolean hasExtraDmxChannelsBeyondBatch() {
        return isPixelMode() || super.hasExtraDmxChannelsBeyondBatch();
    }

    @Override
    public void consume(byte[] dmxValues) {
        int count = channelCount(personality);
        int start = this.getChannelStart() > 0 ? this.getChannelStart() - 1 : 0;
        byte[] v = Arrays.copyOfRange(dmxValues, start, start + count);
        if (v.length < count) {
            return;
        }
        boolean prevAdvanced = beginDmxUpdate();
        int _pi = intensity, _pr = red, _pg = green, _pb = blue;
        int[] before = pixels.clone();
        if (!isPixelMode()) {
            intensity = u(v[0]);
            red = u(v[1]);
            green = u(v[2]);
            blue = u(v[3]);
        } else {
            for (int i = 0; i < count; i++) {
                pixels[i] = u(v[i]);
            }
            aggregate();
        }
        boolean changed = intensity != _pi || red != _pr || green != _pg || blue != _pb || !Arrays.equals(pixels, before);
        finishDmxUpdate(changed, prevAdvanced);
    }

    /** Lumiere de la piece : le pixel le plus fort comme niveau, la couleur moyenne ponderee. */
    private void aggregate() {
        int n = getGrid() * getGrid();
        int peak = 0;
        long r = 0, g = 0, b = 0, w = 0;
        for (int p = 0; p < n; p++) {
            int dim = pixelDim(p);
            int cr = pixels[p * STRIDE + 1], cg = pixels[p * STRIDE + 2], cb = pixels[p * STRIDE + 3];
            int bright = Math.max(cr, Math.max(cg, cb)) * dim / 255;
            peak = Math.max(peak, dim);
            r += (long) cr * bright;
            g += (long) cg * bright;
            b += (long) cb * bright;
            w += bright;
        }
        intensity = w > 0 ? peak : 0;
        red = w > 0 ? (int) (r / w) : 0;
        green = w > 0 ? (int) (g / w) : 0;
        blue = w > 0 ? (int) (b / w) : 0;
    }

    private static int u(byte b) {
        return Byte.toUnsignedInt(b);
    }

    public int convertByteToInt(byte val) {
        return Byte.toUnsignedInt(val);
    }

    @Override
    public void write(CompoundTag tag) {
        super.write(tag);
        tag.putInt("activePersonality", personality);
        tag.putIntArray("Pixels", pixels);
    }

    @Override
    public void read(CompoundTag tag) {
        super.read(tag);
        if (tag.contains("activePersonality")) {
            personality = Mth.clamp(tag.getInt("activePersonality"), 0, GRID.length - 1);
        }
        setChannelCount(channelCount(personality));
        int[] saved = tag.getIntArray("Pixels");
        if (saved.length == pixels.length) {
            System.arraycopy(saved, 0, pixels, 0, pixels.length);
        }
        if (level != null && level.isClientSide) {
            StrobeRenderHelper.markSectionDirty(getBlockPos());
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("activePersonality", personality);
        tag.putIntArray("Pixels", pixels);
        return tag;
    }

    @Override
    public int getDeviceTypeId() {
        return 0x03;
    }

    @Override
    public String getModelName() {
        return "LED Panel 2";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.LED_PANEL_2.getId();
    }

    @Override
    public float getMaxLightDistance() {
        return 1;
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.led_panel_2";
    }
}
