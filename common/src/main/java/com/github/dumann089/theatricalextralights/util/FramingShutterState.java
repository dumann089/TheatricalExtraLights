package com.github.dumann089.theatricalextralights.util;

import net.minecraft.nbt.CompoundTag;

/**
 * Etat DMX d'un module de couteaux (framing shutters) a 4 lames, convention A/B
 * utilisee par grandMA et par les lyres profile (Ayrton Diablo, Clay Paky, Robe) :
 * chaque lame a deux coins, A et B, qui s'enfoncent independamment dans le faisceau.
 * Le bord de la lame est la droite qui relie les deux coins ; des valeurs A != B
 * inclinent donc la lame.
 *
 * <pre>
 *  canal +0  Lame 1 (haut)   coin A  0 = sorti … 255 = rentre a fond
 *  canal +1  Lame 1          coin B
 *  canal +2  Lame 2 (droite) coin A
 *  canal +3  Lame 2          coin B
 *  canal +4  Lame 3 (bas)    coin A
 *  canal +5  Lame 3          coin B
 *  canal +6  Lame 4 (gauche) coin A
 *  canal +7  Lame 4          coin B
 *  canal +8  Rotation du module complet : 0-126 = -55°…0°, 127-128 = 0°, 129-255 = 0°…+55°
 * </pre>
 *
 * Les valeurs brutes DMX sont conservees cote serveur et synchronisees par NBT ; le client
 * interpole entre le tick precedent et le tick courant pour un mouvement de lame fluide.
 */
public final class FramingShutterState {

    public static final int BLADE_COUNT = 4;
    /** Nombre de canaux DMX consommes par le module. */
    public static final int CHANNEL_COUNT = BLADE_COUNT * 2 + 1;
    /** Rotation max du module complet, en degres (Martin : +/-55°, Robe : +/-60°). */
    public static final float FRAME_ROTATION_MAX_DEG = 55.0f;
    /** Valeur DMX neutre pour le canal bipolaire de rotation (127-128 = 0°). */
    public static final int NEUTRAL_DMX = 127;

    private static final String NBT_KEY = "framingShutters";

    private final int[] insertionA = new int[BLADE_COUNT];
    private final int[] insertionB = new int[BLADE_COUNT];
    private int frameRotation = NEUTRAL_DMX;

    private final int[] prevInsertionA = new int[BLADE_COUNT];
    private final int[] prevInsertionB = new int[BLADE_COUNT];
    private int prevFrameRotation = NEUTRAL_DMX;

    // ── DMX ──────────────────────────────────────────────────────────────────

    /**
     * Lit les 9 canaux a partir de {@code offset} dans la tranche DMX du projecteur.
     *
     * @return true si au moins une valeur a change
     */
    public boolean consume(byte[] values, int offset) {
        if (values.length < offset + CHANNEL_COUNT) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < BLADE_COUNT; i++) {
            int a = Byte.toUnsignedInt(values[offset + i * 2]);
            int b = Byte.toUnsignedInt(values[offset + i * 2 + 1]);
            if (a != insertionA[i]) {
                insertionA[i] = a;
                changed = true;
            }
            if (b != insertionB[i]) {
                insertionB[i] = b;
                changed = true;
            }
        }
        int rot = Byte.toUnsignedInt(values[offset + BLADE_COUNT * 2]);
        if (rot != frameRotation) {
            frameRotation = rot;
            changed = true;
        }
        return changed;
    }

    /**
     * Remet toutes les lames en position sortie (mode sans couteaux).
     *
     * @return true si l'etat n'etait pas deja neutre
     */
    public boolean reset() {
        boolean changed = false;
        for (int i = 0; i < BLADE_COUNT; i++) {
            if (insertionA[i] != 0) {
                insertionA[i] = 0;
                changed = true;
            }
            if (insertionB[i] != 0) {
                insertionB[i] = 0;
                changed = true;
            }
        }
        if (frameRotation != NEUTRAL_DMX) {
            frameRotation = NEUTRAL_DMX;
            changed = true;
        }
        return changed;
    }

    /** A appeler chaque tick client : avance les valeurs precedentes pour l'interpolation. */
    public void tickClient() {
        System.arraycopy(insertionA, 0, prevInsertionA, 0, BLADE_COUNT);
        System.arraycopy(insertionB, 0, prevInsertionB, 0, BLADE_COUNT);
        prevFrameRotation = frameRotation;
    }

    /** Au moins un coin de lame est engage dans le faisceau. */
    public boolean isActive() {
        for (int i = 0; i < BLADE_COUNT; i++) {
            if (insertionA[i] > 0 || insertionB[i] > 0 || prevInsertionA[i] > 0 || prevInsertionB[i] > 0) {
                return true;
            }
        }
        return false;
    }

    public int getInsertionA(int blade) { return insertionA[blade]; }
    public int getInsertionB(int blade) { return insertionB[blade]; }
    public int getFrameRotation() { return frameRotation; }

    // ── NBT ──────────────────────────────────────────────────────────────────

    public void write(CompoundTag tag) {
        CompoundTag t = new CompoundTag();
        t.putIntArray("a", insertionA.clone());
        t.putIntArray("b", insertionB.clone());
        t.putInt("frameRotation", frameRotation);
        tag.put(NBT_KEY, t);
    }

    public void read(CompoundTag tag) {
        if (!tag.contains(NBT_KEY)) {
            reset();
            tickClient();
            return;
        }
        CompoundTag t = tag.getCompound(NBT_KEY);
        int[] a = t.getIntArray("a");
        int[] b = t.getIntArray("b");
        for (int i = 0; i < BLADE_COUNT; i++) {
            insertionA[i] = i < a.length ? clampDmx(a[i]) : 0;
            insertionB[i] = i < b.length ? clampDmx(b[i]) : 0;
        }
        frameRotation = t.contains("frameRotation") ? clampDmx(t.getInt("frameRotation")) : NEUTRAL_DMX;
        tickClient();
    }

    private static int clampDmx(int v) {
        return Math.max(0, Math.min(255, v));
    }

    // ── Decodage ─────────────────────────────────────────────────────────────

    /** Insertion 0-255 → 0.0 (sortie) … 1.0 (rentree a fond). */
    public static float insertion01(float dmx) {
        return Math.max(0.0f, Math.min(1.0f, dmx / 255.0f));
    }

    /**
     * Canal bipolaire avec zone morte centrale, comme sur les protocoles Martin / Robe :
     * 0-126 → -1…0, 127-128 → 0, 129-255 → 0…+1.
     */
    public static float bipolar(float dmx) {
        if (dmx <= 126.0f) {
            return (dmx - 126.0f) / 126.0f;
        }
        if (dmx >= 129.0f) {
            return (dmx - 129.0f) / 126.0f;
        }
        return 0.0f;
    }

    /** Photo interpolee de l'etat, prete pour le shader. */
    public Snapshot snapshot(float partialTicks) {
        float[] a = new float[BLADE_COUNT];
        float[] b = new float[BLADE_COUNT];
        for (int i = 0; i < BLADE_COUNT; i++) {
            a[i] = insertion01(prevInsertionA[i] + (insertionA[i] - prevInsertionA[i]) * partialTicks);
            b[i] = insertion01(prevInsertionB[i] + (insertionB[i] - prevInsertionB[i]) * partialTicks);
        }
        float rawRot = prevFrameRotation + (frameRotation - prevFrameRotation) * partialTicks;
        float rot = (float) Math.toRadians(bipolar(rawRot) * FRAME_ROTATION_MAX_DEG);
        return new Snapshot(a, b, rot);
    }

    /**
     * Valeurs decodees et interpolees : insertion 0-1 des coins A et B par lame, rotation du
     * module en radians. Immuable, sure a capturer dans un LazyRenderer.
     */
    public record Snapshot(float[] insertionA, float[] insertionB, float frameRotation) {

        public boolean isActive() {
            for (int i = 0; i < BLADE_COUNT; i++) {
                if (insertionA[i] > 0.0005f || insertionB[i] > 0.0005f) {
                    return true;
                }
            }
            return false;
        }

        public int stateHash() {
            int hash = 7;
            for (int i = 0; i < BLADE_COUNT; i++) {
                hash = 31 * hash + Float.floatToIntBits(insertionA[i]);
                hash = 31 * hash + Float.floatToIntBits(insertionB[i]);
            }
            hash = 31 * hash + Float.floatToIntBits(frameRotation);
            return hash;
        }
    }
}
