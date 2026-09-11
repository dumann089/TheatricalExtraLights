package com.github.dumann089.theatricalextralights.util;

import net.minecraft.nbt.CompoundTag;

/**
 * Etat DMX d'un module de couteaux (framing shutters) a 4 lames, calque sur les lyres
 * profile reelles (Martin MAC Encore Performance, Robe T1 Profile, Ayrton Diablo) :
 *
 * <pre>
 *  canal +0  Lame 1 (haut)   insertion   0 = sortie … 255 = rentree a fond
 *  canal +1  Lame 1          angle       0-126 = -30°…0°, 127-128 = 0°, 129-255 = 0°…+30°
 *  canal +2  Lame 2 (droite) insertion
 *  canal +3  Lame 2          angle
 *  canal +4  Lame 3 (bas)    insertion
 *  canal +5  Lame 3          angle
 *  canal +6  Lame 4 (gauche) insertion
 *  canal +7  Lame 4          angle
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
    /** Course angulaire max d'une lame (swivel), en degres. */
    public static final float BLADE_ANGLE_MAX_DEG = 30.0f;
    /** Rotation max du module complet, en degres (Martin : +/-55°, Robe : +/-60°). */
    public static final float FRAME_ROTATION_MAX_DEG = 55.0f;
    /** Valeur DMX neutre pour les canaux bipolaires (127-128 = 0°). */
    public static final int NEUTRAL_DMX = 127;

    private static final String NBT_KEY = "framingShutters";

    private final int[] insertion = new int[BLADE_COUNT];
    private final int[] angle = new int[BLADE_COUNT];
    private int frameRotation = NEUTRAL_DMX;

    private final int[] prevInsertion = new int[BLADE_COUNT];
    private final int[] prevAngle = new int[BLADE_COUNT];
    private int prevFrameRotation = NEUTRAL_DMX;

    public FramingShutterState() {
        java.util.Arrays.fill(angle, NEUTRAL_DMX);
        java.util.Arrays.fill(prevAngle, NEUTRAL_DMX);
    }

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
            int ins = Byte.toUnsignedInt(values[offset + i * 2]);
            int ang = Byte.toUnsignedInt(values[offset + i * 2 + 1]);
            if (ins != insertion[i]) {
                insertion[i] = ins;
                changed = true;
            }
            if (ang != angle[i]) {
                angle[i] = ang;
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
            if (insertion[i] != 0) {
                insertion[i] = 0;
                changed = true;
            }
            if (angle[i] != NEUTRAL_DMX) {
                angle[i] = NEUTRAL_DMX;
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
        System.arraycopy(insertion, 0, prevInsertion, 0, BLADE_COUNT);
        System.arraycopy(angle, 0, prevAngle, 0, BLADE_COUNT);
        prevFrameRotation = frameRotation;
    }

    /** Au moins une lame est engagee dans le faisceau. */
    public boolean isActive() {
        for (int i = 0; i < BLADE_COUNT; i++) {
            if (insertion[i] > 0 || prevInsertion[i] > 0) {
                return true;
            }
        }
        return false;
    }

    public int getInsertion(int blade) { return insertion[blade]; }
    public int getAngle(int blade) { return angle[blade]; }
    public int getFrameRotation() { return frameRotation; }

    // ── NBT ──────────────────────────────────────────────────────────────────

    public void write(CompoundTag tag) {
        CompoundTag t = new CompoundTag();
        t.putIntArray("insertion", insertion.clone());
        t.putIntArray("angle", angle.clone());
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
        int[] ins = t.getIntArray("insertion");
        int[] ang = t.getIntArray("angle");
        for (int i = 0; i < BLADE_COUNT; i++) {
            insertion[i] = i < ins.length ? clampDmx(ins[i]) : 0;
            angle[i] = i < ang.length ? clampDmx(ang[i]) : NEUTRAL_DMX;
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
        float[] ins = new float[BLADE_COUNT];
        float[] ang = new float[BLADE_COUNT];
        for (int i = 0; i < BLADE_COUNT; i++) {
            float rawIns = prevInsertion[i] + (insertion[i] - prevInsertion[i]) * partialTicks;
            float rawAng = prevAngle[i] + (angle[i] - prevAngle[i]) * partialTicks;
            ins[i] = insertion01(rawIns);
            ang[i] = (float) Math.toRadians(bipolar(rawAng) * BLADE_ANGLE_MAX_DEG);
        }
        float rawRot = prevFrameRotation + (frameRotation - prevFrameRotation) * partialTicks;
        float rot = (float) Math.toRadians(bipolar(rawRot) * FRAME_ROTATION_MAX_DEG);
        return new Snapshot(ins, ang, rot);
    }

    /**
     * Valeurs decodees et interpolees : insertion 0-1 par lame, angle de lame en radians,
     * rotation du module en radians. Immuable, sure a capturer dans un LazyRenderer.
     */
    public record Snapshot(float[] insertion, float[] bladeAngle, float frameRotation) {

        public boolean isActive() {
            for (float f : insertion) {
                if (f > 0.0005f) {
                    return true;
                }
            }
            return false;
        }

        public int stateHash() {
            int hash = 7;
            for (int i = 0; i < BLADE_COUNT; i++) {
                hash = 31 * hash + Float.floatToIntBits(insertion[i]);
                hash = 31 * hash + Float.floatToIntBits(bladeAngle[i]);
            }
            hash = 31 * hash + Float.floatToIntBits(frameRotation);
            return hash;
        }
    }
}
