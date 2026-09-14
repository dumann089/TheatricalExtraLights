package com.github.dumann089.theatricalextralights.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/**
 * Etat de la personnalite « Profile 16 bit » des lyres a gobos : canaux d'une vraie lyre
 * profile (shutter, dimmer 16 bit, RGB, prisme, frost, pan/tilt 16 bit avec vitesse moteur).
 * Les couteaux restent dans {@link FramingShutterState}.
 *
 * <pre>
 *  1  Shutter             0 ferme, 1-254 strobe 0.5→10 Hz, 255 ouvert
 *  2  Dimmer (coarse)     3  Dimmer (fine)
 *  4  Red   5  Green   6  Blue
 *  7  Roue de gobos       8  Rotation du gobo
 *  9  Prisme              0-127 hors faisceau, 128-170 3 facettes, 171-213 6 facettes, 214-255 9 facettes
 * 10  Rotation du prisme  0-127 position indexee, 128-191 CW lent→rapide, 192-255 CCW lent→rapide
 * 11  Frost               0 aucun … 255 plein
 * 12  Zoom               13  Focus
 * 14  Pan (coarse)       15  Pan (fine)   16  Tilt (coarse)   17  Tilt (fine)
 * 18  Vitesse pan/tilt    0-2 tracking (instantane), 3-255 rapide→lent
 * 19-27 Couteaux (voir FramingShutterState)
 * </pre>
 */
public final class ProfileHeadState {

    public static final int CHANNELS_BEFORE_SHUTTERS = 18;

    /** Vitesse moteur max (tracking) et min, en degres par seconde. */
    private static final float MOTOR_FAST_DEG_S = 720f;
    private static final float MOTOR_SLOW_DEG_S = 25f;

    /**
     * Plage du strobe mecanique, en Hz, sur DMX 1..254. Le rendu du faisceau n'est rafraichi
     * qu'au tick (20 Hz), donc la periode est arrondie a un nombre entier de ticks et le
     * maximum utile est 10 Hz : au-dela, l'echantillonnage au tick produit un battement lent.
     */
    private static final float STROBE_MIN_HZ = 0.5f;
    private static final float STROBE_MAX_HZ = 10f;

    private boolean active;

    // Valeurs DMX brutes
    private int shutter = 255;      // ouvert par defaut
    private int dimmer16;
    private int red = 255, green = 255, blue = 255;
    private int prism;
    private int prismRotation;
    private int frost;
    private int pan16 = 32768;
    private int tilt16 = 54613; // ~ tilt 0°
    private int speed;

    // Cibles / moteur (client)
    private float targetPan;
    private float targetTilt;
    private float motorPan;
    private float motorTilt;
    private float prevMotorPan;
    private float prevMotorTilt;
    private boolean motorInit;

    // Animation client du prisme
    private float prismAngle;
    private float prevPrismAngle;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // ── DMX ──────────────────────────────────────────────────────────────────

    /** Lit les 18 premiers canaux. Retourne true si une valeur a change. */
    public boolean consume(byte[] v) {
        int h = hashState();
        shutter = u(v[0]);
        dimmer16 = (u(v[1]) << 8) | u(v[2]);
        red = u(v[3]);
        green = u(v[4]);
        blue = u(v[5]);
        prism = u(v[8]);
        prismRotation = u(v[9]);
        frost = u(v[10]);
        pan16 = (u(v[13]) << 8) | u(v[14]);
        tilt16 = (u(v[15]) << 8) | u(v[16]);
        speed = u(v[17]);
        targetPan = pan16 / 65535f * 360f - 180f;
        targetTilt = tilt16 / 65535f * 270f - 225f;
        active = true;
        return h != hashState();
    }

    private int hashState() {
        int h = shutter;
        h = 31 * h + dimmer16;
        h = 31 * h + red;
        h = 31 * h + green;
        h = 31 * h + blue;
        h = 31 * h + prism;
        h = 31 * h + prismRotation;
        h = 31 * h + frost;
        h = 31 * h + pan16;
        h = 31 * h + tilt16;
        h = 31 * h + speed;
        return h;
    }

    private static int u(byte b) {
        return Byte.toUnsignedInt(b);
    }

    // ── Decodage ─────────────────────────────────────────────────────────────

    /** Intensite 0-255 issue du dimmer 16 bit. */
    public int intensity8() {
        return Math.round(dimmer16 / 257f);
    }

    public int getGoboRaw(byte[] v) { return u(v[6]); }
    public int getGoboSpinRaw(byte[] v) { return u(v[7]); }
    public int getZoomRaw(byte[] v) { return u(v[11]); }
    public int getFocusRaw(byte[] v) { return u(v[12]); }

    public int getPanInt() { return Math.round(targetPan); }
    public int getTiltInt() { return Math.round(targetTilt); }

    public float getTargetPan() { return targetPan; }
    public float getTargetTilt() { return targetTilt; }
    public int getShutter() { return shutter; }
    public int getRed() { return red; }
    public int getGreen() { return green; }
    public int getBlue() { return blue; }
    public int getPrism() { return prism; }
    public int getPrismRotation() { return prismRotation; }
    public int getFrost() { return frost; }
    public int getSpeed() { return speed; }

    /** Couleur RGB a ecrire dans les champs du block entity. */
    public int staticColour() {
        return (red << 16) | (green << 8) | blue;
    }

    /** Ouvert en continu (255). */
    public boolean isShutterOpen() {
        return shutter >= 255;
    }

    /** Strobe mecanique (1-254). */
    public boolean isShutterStrobing() {
        return shutter > 0 && shutter < 255;
    }

    /** Periode du strobe en ticks (>= 2) pour la valeur courante, 0 si ferme ou ouvert. */
    public int strobePeriodTicks() {
        if (!isShutterStrobing()) return 0;
        float hz = STROBE_MIN_HZ + (shutter - 1) / 253f * (STROBE_MAX_HZ - STROBE_MIN_HZ);
        return Math.max(2, Math.round(20f / hz));
    }

    /** Frequence reelle du strobe en Hz apres arrondi au tick (0 si ferme ou ouvert). */
    public float strobeHz() {
        int period = strobePeriodTicks();
        return period == 0 ? 0f : 20f / period;
    }

    /**
     * Facteur 0-1 applique au dimmer : 0 ferme, 1-254 strobe de 0.5 a 10 Hz par periodes
     * entieres de ticks (moitie allume, moitie eteint), 255 ouvert.
     */
    public float shutterFactor(long gameTime, float partialTicks) {
        if (shutter <= 0) return 0f;
        if (shutter >= 255) return 1f;
        int period = strobePeriodTicks();
        long phase = Math.floorMod(gameTime, (long) period);
        return phase < (period + 1) / 2 ? 1f : 0f;
    }

    public boolean hasPrism() {
        return prism >= 128;
    }

    /** Nombre de facettes du prisme : 1 (hors faisceau), 3, 6 ou 9. */
    public int prismFacets() {
        if (prism < 128) return 1;
        if (prism < 171) return 3;
        if (prism < 214) return 6;
        return 9;
    }

    /** Angle du prisme en degres (indexe ou anime). */
    public float prismAngle(float partialTicks) {
        if (prismRotation < 128) {
            return prismRotation / 127f * 360f;
        }
        return prevPrismAngle + (prismAngle - prevPrismAngle) * partialTicks;
    }

    // ── Moteur pan/tilt et animations (client) ───────────────────────────────

    public void tickClient() {
        prevMotorPan = motorPan;
        prevMotorTilt = motorTilt;
        prevPrismAngle = prismAngle;

        if (!motorInit) {
            motorPan = targetPan;
            motorTilt = targetTilt;
            prevMotorPan = motorPan;
            prevMotorTilt = motorTilt;
            motorInit = true;
        } else if (speed <= 2) {
            motorPan = targetPan;
            motorTilt = targetTilt;
        } else {
            float degPerSec = Mth.lerp((speed - 3) / 252f, MOTOR_FAST_DEG_S, MOTOR_SLOW_DEG_S);
            float maxStep = degPerSec / 20f;
            motorPan = approach(motorPan, targetPan, maxStep);
            motorTilt = approach(motorTilt, targetTilt, maxStep);
        }

        if (prismRotation >= 128) {
            boolean cw = prismRotation < 192;
            float norm = cw ? (prismRotation - 128) / 63f : (prismRotation - 192) / 63f;
            float degPerTick = Mth.lerp(norm, 0.5f, 12f) * (cw ? 1f : -1f);
            // Angle cumule sans modulo : l'interpolation prev→courant ne traverse jamais un saut
            // 359→0. On recale les deux valeurs ensemble quand elles deviennent trop grandes.
            prismAngle += degPerTick;
            if (Math.abs(prismAngle) > 360000f) {
                float shift = Math.copySign(360000f, prismAngle);
                prismAngle -= shift;
                prevPrismAngle -= shift;
            }
        } else {
            // Mode indexe : on suit la position pour que la rotation continue reparte de la.
            prismAngle = prismRotation / 127f * 360f;
            prevPrismAngle = prismAngle;
        }
    }

    /** Approche avec plafond de vitesse et amorti : rapide au depart, doux a l'arrivee. */
    private static float approach(float current, float target, float maxStep) {
        float delta = target - current;
        if (Math.abs(delta) < 0.001f) {
            return target;
        }
        float eased = delta * 0.45f;
        if (Math.abs(eased) < 0.05f) {
            eased = Math.copySign(Math.min(0.05f, Math.abs(delta)), delta);
        }
        float step = Mth.clamp(eased, -maxStep, maxStep);
        return current + step;
    }

    public float partialPan(float partialTicks) {
        return prevMotorPan + (motorPan - prevMotorPan) * partialTicks;
    }

    public float partialTilt(float partialTicks) {
        return prevMotorTilt + (motorTilt - prevMotorTilt) * partialTicks;
    }

    public boolean isMoving() {
        return Math.abs(motorPan - targetPan) > 0.01f || Math.abs(motorTilt - targetTilt) > 0.01f;
    }

    // ── NBT ──────────────────────────────────────────────────────────────────

    public void write(CompoundTag tag) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("active", active);
        t.putInt("shutter", shutter);
        t.putInt("dimmer16", dimmer16);
        t.putInt("red", red);
        t.putInt("green", green);
        t.putInt("blue", blue);
        t.putInt("prism", prism);
        t.putInt("prismRot", prismRotation);
        t.putInt("frost", frost);
        t.putInt("pan16", pan16);
        t.putInt("tilt16", tilt16);
        t.putInt("speed", speed);
        tag.put("profileHead", t);
    }

    public void read(CompoundTag tag) {
        if (!tag.contains("profileHead")) {
            active = false;
            return;
        }
        CompoundTag t = tag.getCompound("profileHead");
        active = t.getBoolean("active");
        shutter = t.getInt("shutter");
        dimmer16 = t.getInt("dimmer16");
        red = t.getInt("red");
        green = t.getInt("green");
        blue = t.getInt("blue");
        prism = t.getInt("prism");
        prismRotation = t.getInt("prismRot");
        frost = t.getInt("frost");
        pan16 = t.getInt("pan16");
        tilt16 = t.getInt("tilt16");
        speed = t.getInt("speed");
        targetPan = pan16 / 65535f * 360f - 180f;
        targetTilt = tilt16 / 65535f * 270f - 225f;
    }
}
