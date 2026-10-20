package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasPersonality;
import com.github.dumann089.theatricalextralights.client.StrobeRenderHelper;
import com.github.dumann089.theatricalextralights.util.BlockEntitySync;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.List;

/**
 * Barre LED a pixels : RGB Bar, Vertical RGB Bar, Moving Bar.
 *
 * <p>Deux personnalites : la classique (index 0 : dimmer + un RGB pour toute la barre, plus pan et
 * tilt sur la barre mobile) et le mode pixel (index 1 : un dimmer et un RGB par pixel, sans dimmer
 * general, precedes des canaux d'en-tete de la sous-classe, pan/tilt par exemple). Dans les deux
 * cas {@code red/green/blue} portent la couleur de la barre (en mode pixel : celle du groupe de
 * pixels dominant) et {@code intensity} son niveau (en mode pixel : le dimmer du pixel le plus
 * fort), pour la lumiere dynamique et les outils qui ne
 * connaissent qu'une couleur ; le rendu par pixel lit {@link #getPixelColour(int)} et
 * {@link #getPixelDimmer(int)}.
 */
public abstract class PixelBarBlockEntity extends ExtraLightsLightBlockEntity implements HasPersonality {

    /** Canaux DMX par pixel : dimmer, rouge, vert, bleu. */
    public static final int PIXEL_STRIDE = 4;
    protected static final int MODE_CLASSIC = 0;
    protected static final int MODE_PIXEL = 1;

    private final int pixelCount;
    /** [p0.dim, p0.r, p0.g, p0.b, p1.dim, ...] bruts ; en mode classique, ignores. */
    private final int[] pixels;
    private int activePersonalityIndex = MODE_CLASSIC;

    protected PixelBarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int pixelCount) {
        super(type, pos, state);
        this.pixelCount = pixelCount;
        this.pixels = new int[pixelCount * PIXEL_STRIDE];
        setChannelCount(getPersonalityChannelCount());
    }

    public int getPixelCount() {
        return pixelCount;
    }

    /** Nombre de canaux du mode pixel : en-tete + 4 par pixel. */
    public static int pixelModeChannelCount(int headerChannels, int pixelCount) {
        return headerChannels + pixelCount * PIXEL_STRIDE;
    }

    // ── Sous-classe ──────────────────────────────────────────────────────────

    /** Canaux du mode classique (4 pour une barre fixe, 7 pour une barre mobile). */
    protected abstract int classicChannelCount();

    /** Lit les canaux du mode classique dans intensity/red/green/blue (et pan/tilt). */
    protected abstract void consumeClassic(byte[] values);

    /** Canaux qui precedent les pixels en mode pixel (pan/tilt d'une barre mobile) ; 0 par defaut. */
    protected int pixelHeaderChannelCount() {
        return 0;
    }

    /** Lit les canaux d'en-tete du mode pixel. */
    protected void consumePixelHeader(byte[] values) {
    }

    // ── Personnalite ─────────────────────────────────────────────────────────

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
        setChannelCount(getPersonalityChannelCount());
        setChanged();
        if (level != null) {
            BlockEntitySync.sendData(this);
        }
    }

    private int getPersonalityChannelCount() {
        List<DMXPersonality> personalities = getFixture().getDMXPersonalities();
        if (personalities == null || personalities.isEmpty()) {
            return classicChannelCount();
        }
        return personalities.get(activePersonalityIndex).getChannelCount();
    }

    public boolean isPixelMode() {
        return activePersonalityIndex == MODE_PIXEL;
    }

    // ── Pixels ───────────────────────────────────────────────────────────────

    /** Dimmer propre du pixel 0..255 ; 255 en mode classique, ou le dimmer de la barre est {@code intensity}. */
    public int getPixelDimmer(int pixel) {
        if (!isPixelMode()) {
            return 255;
        }
        if (pixel < 0 || pixel >= pixelCount) {
            return 0;
        }
        return pixels[pixel * PIXEL_STRIDE];
    }

    /** Couleur 0xRRGGBB brute du pixel (avant son dimmer) ; en mode classique, la couleur de la barre. */
    public int getPixelColour(int pixel) {
        if (!isPixelMode()) {
            return getColour();
        }
        if (pixel < 0 || pixel >= pixelCount) {
            return 0;
        }
        int base = pixel * PIXEL_STRIDE;
        return (pixels[base + 1] << 16) | (pixels[base + 2] << 8) | pixels[base + 3];
    }

    /**
     * Luminosite du pixel 0..255 : composante la plus forte de sa couleur, multipliee par son dimmer.
     * En mode classique, celle de la couleur de la barre (le dimmer est {@code intensity}).
     */
    public int getPixelLevel(int pixel) {
        int c = getPixelColour(pixel);
        int max = Math.max((c >> 16) & 0xFF, Math.max((c >> 8) & 0xFF, c & 0xFF));
        return (max * getPixelDimmer(pixel) + 127) / 255;
    }

    // ── DMX ──────────────────────────────────────────────────────────────────

    @Override
    public void consume(byte[] dmxValues) {
        int channelCount = getPersonalityChannelCount();
        int start = this.getChannelStart() > 0 ? this.getChannelStart() - 1 : 0;
        byte[] ourValues = Arrays.copyOfRange(dmxValues, start, start + channelCount);
        if (ourValues.length < channelCount) {
            return;
        }
        boolean prevAdvanced = beginDmxUpdate();
        int _pi = intensity, _pr = red, _pg = green, _pb = blue, _pf = focus, _pp = pan, _pt = tilt;
        int[] prevPixels = pixels.clone();

        if (isPixelMode()) {
            consumePixelHeader(ourValues);
            int base = pixelHeaderChannelCount();
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = convertByteToInt(ourValues[base + i]);
            }
            // Pas de dimmer general : la barre prend le dimmer du pixel le plus fort. Sa couleur,
            // pour la lumiere dynamique, est celle du groupe de pixels de meme couleur qui pese le
            // plus (somme des niveaux) : une moyenne de couleurs differentes tirerait vers le blanc,
            // et un projecteur de scene doit garder une lumiere saturee meme sur un look multicolore.
            // Une barre unie donne ainsi la meme lumiere dynamique qu'en mode classique.
            int maxDim = 0;
            int bestColour = 0;
            long bestWeight = 0;
            for (int p = 0; p < pixelCount; p++) {
                maxDim = Math.max(maxDim, getPixelDimmer(p));
                int level = getPixelLevel(p);
                if (level <= 0) {
                    continue;
                }
                int colour = getPixelColour(p);
                long weight = 0;
                for (int q = 0; q < pixelCount; q++) {
                    if (getPixelColour(q) == colour) {
                        weight += getPixelLevel(q);
                    }
                }
                if (weight > bestWeight) {
                    bestWeight = weight;
                    bestColour = colour;
                }
            }
            intensity = maxDim;
            red = (bestColour >> 16) & 0xFF;
            green = (bestColour >> 8) & 0xFF;
            blue = bestColour & 0xFF;
        } else {
            consumeClassic(ourValues);
        }

        boolean changed = intensity != _pi || red != _pr || green != _pg || blue != _pb
                || focus != _pf || pan != _pp || tilt != _pt || !Arrays.equals(pixels, prevPixels);
        finishDmxUpdate(changed, prevAdvanced);
    }

    /** En mode pixel, les canaux depassent le DmxFrame de Theatrical : sync bloc complete. */
    @Override
    protected boolean hasExtraDmxChannelsBeyondBatch() {
        return isPixelMode() || super.hasExtraDmxChannelsBeyondBatch();
    }

    public int convertByteToInt(byte val) {
        return Byte.toUnsignedInt(val);
    }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    public void write(CompoundTag tag) {
        super.write(tag);
        tag.putInt("activePersonality", activePersonalityIndex);
        tag.putIntArray("Pixels", pixels);
    }

    @Override
    public void read(CompoundTag tag) {
        super.read(tag);
        if (tag.contains("activePersonality")) {
            int index = tag.getInt("activePersonality");
            List<DMXPersonality> personalities = getFixture().getDMXPersonalities();
            if (index >= 0 && index < personalities.size()) {
                activePersonalityIndex = index;
            }
        }
        setChannelCount(getPersonalityChannelCount());
        int[] saved = tag.getIntArray("Pixels");
        if (saved.length == pixels.length) {
            System.arraycopy(saved, 0, pixels, 0, pixels.length);
        }
        if (level != null && level.isClientSide) {
            StrobeRenderHelper.markSectionDirty(getBlockPos());
        }
    }
}
