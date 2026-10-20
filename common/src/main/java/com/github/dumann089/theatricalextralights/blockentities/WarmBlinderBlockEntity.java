package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasPersonality;
import com.github.dumann089.theatricalextralights.util.BlockEntitySync;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * Blinder a lampes tungstene chaudes, couleur fixe. Deux modes :
 * <ul>
 *   <li>{@link #MODE_MASTER} : un canal, le niveau de toutes les lampes ;</li>
 *   <li>{@link #MODE_PIXEL} : un canal par lampe, dans l'ordre de lecture vu de face (rangee du
 *   haut d'abord, de gauche a droite).</li>
 * </ul>
 * En mode pixel, l'intensite de l'appareil (lumiere dynamique, garde du rendu) vaut la lampe la
 * plus forte ; le rendu dessine chaque lampe a son propre niveau.
 */
public abstract class WarmBlinderBlockEntity extends ExtraLightsLightBlockEntity implements HasPersonality {

    public static final int MODE_MASTER = 0;
    public static final int MODE_PIXEL = 1;

    /** Couleur d'une lampe tungstene a pleine puissance. */
    private static final int WARM_R = 255;
    private static final int WARM_G = 139;
    private static final int WARM_B = 39;

    private final int lampCount;
    private int personality = MODE_MASTER;
    /** Niveau de chaque lampe en mode pixel, 0..255. */
    private final int[] lamps;

    protected WarmBlinderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int lampCount) {
        super(type, pos, state);
        this.lampCount = lampCount;
        this.lamps = new int[lampCount];
        setChannelCount(channelCount(personality));
    }

    public int getLampCount() {
        return lampCount;
    }

    private int channelCount(int mode) {
        return mode == MODE_PIXEL ? lampCount : 1;
    }

    public boolean isPixelMode() {
        return personality == MODE_PIXEL;
    }

    /** Niveau DMX d'une lampe, 0..255 ; en mode master, celui de l'appareil. */
    public int getLampLevel(int index) {
        if (!isPixelMode()) {
            return intensity;
        }
        return index >= 0 && index < lampCount ? lamps[index] : 0;
    }

    @Override
    public int getFocus() {
        return 255;
    }

    @Override
    public int getActivePersonality() {
        return personality;
    }

    @Override
    public void setActivePersonality(int index) {
        if (index < MODE_MASTER || index > MODE_PIXEL) {
            return;
        }
        personality = index;
        setChannelCount(channelCount(personality));
        Arrays.fill(lamps, 0);
        setChanged();
        if (level != null) {
            BlockEntitySync.sendData(this);
        }
    }

    /** Les niveaux des lampes ne passent pas dans la trame groupee : synchro du bloc en mode pixel. */
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
        int _pi = intensity, _pr = red, _pg = green, _pb = blue, _pf = focus, _pp = pan, _pt = tilt;
        boolean lampsChanged = false;
        if (isPixelMode()) {
            int peak = 0;
            for (int i = 0; i < lampCount; i++) {
                int value = Byte.toUnsignedInt(v[i]);
                lampsChanged |= lamps[i] != value;
                lamps[i] = value;
                peak = Math.max(peak, value);
            }
            intensity = peak;
        } else {
            intensity = Byte.toUnsignedInt(v[0]);
        }

        float scale = intensity / 255f;
        red = (int) (WARM_R * scale);
        green = (int) (WARM_G * scale);
        blue = (int) (WARM_B * scale);

        finishDmxUpdate(lampsChanged || intensity != _pi || red != _pr || green != _pg || blue != _pb
                || focus != _pf || pan != _pp || tilt != _pt, prevAdvanced);
    }

    public int convertByteToInt(byte val) {
        return Byte.toUnsignedInt(val);
    }

    @Override
    public void write(CompoundTag tag) {
        super.write(tag);
        tag.putInt("activePersonality", personality);
        tag.putIntArray("Lamps", lamps);
    }

    @Override
    public void read(CompoundTag tag) {
        super.read(tag);
        if (tag.contains("activePersonality")) {
            personality = Mth.clamp(tag.getInt("activePersonality"), MODE_MASTER, MODE_PIXEL);
        }
        setChannelCount(channelCount(personality));
        int[] saved = tag.getIntArray("Lamps");
        if (saved.length == lampCount) {
            System.arraycopy(saved, 0, lamps, 0, lampCount);
        } else {
            Arrays.fill(lamps, 0);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("activePersonality", personality);
        tag.putIntArray("Lamps", lamps);
        return tag;
    }
}
