package com.github.dumann089.theatricalextralights.util;

import net.minecraft.util.Mth;

/**
 * Moteur de strobe type Martin Atomic : intensite, duree de flash, cadence et canal d'effets.
 * Le niveau ne depend que du temps, du seed et des valeurs DMX : client et serveur calculent la
 * meme chose sans rien synchroniser de plus.
 *
 * <p>Canal d'effets, plages calquees sur l'Atomic 3000 :
 * 0-9 strobe simple, 10-39 blinder (allume en continu), 40-69 montee, 70-99 descente,
 * 100-129 aller-retour, 130-159 aleatoire, 160-189 eclairs, 190-219 pics, 220-255 scintillement
 * (par segment sur les modes pixel).
 */
public final class AtomicStrobeEngine {
    public static final int EFFECT_NONE = 0;
    public static final int EFFECT_BLINDER = 1;
    public static final int EFFECT_RAMP_UP = 2;
    public static final int EFFECT_RAMP_DOWN = 3;
    public static final int EFFECT_RAMP_UP_DOWN = 4;
    public static final int EFFECT_RANDOM = 5;
    public static final int EFFECT_LIGHTNING = 6;
    public static final int EFFECT_SPIKES = 7;
    public static final int EFFECT_SPARKLE = 8;

    /** Duree de flash : 12 ms a 650 ms, courbe carree pour de la finesse sur les flashs courts. */
    private static final float MIN_FLASH_SECONDS = 0.012f;
    private static final float MAX_FLASH_SECONDS = 0.65f;
    /** Cadence : 0.5 a 25 Hz ; 0 = pas de flash, comme sur l'appareil. */
    private static final float MIN_RATE_HZ = 0.5f;
    private static final float MAX_RATE_HZ = 25f;
    /** Un tick de jeu, fenetre d'integration de la lumiere dynamique. */
    public static final double TICK_SECONDS = 0.05;
    /** Une image a 60 Hz : un flash plus court qu'une image doit quand meme s'afficher une image. */
    public static final double FRAME_SECONDS = 1.0 / 60.0;

    private AtomicStrobeEngine() {
    }

    public static int effectOf(int dmx) {
        if (dmx < 10) return EFFECT_NONE;
        if (dmx < 40) return EFFECT_BLINDER;
        if (dmx < 70) return EFFECT_RAMP_UP;
        if (dmx < 100) return EFFECT_RAMP_DOWN;
        if (dmx < 130) return EFFECT_RAMP_UP_DOWN;
        if (dmx < 160) return EFFECT_RANDOM;
        if (dmx < 190) return EFFECT_LIGHTNING;
        if (dmx < 220) return EFFECT_SPIKES;
        return EFFECT_SPARKLE;
    }

    public static float flashSeconds(int duration) {
        float d = Mth.clamp(duration, 0, 255) / 255f;
        return MIN_FLASH_SECONDS + (MAX_FLASH_SECONDS - MIN_FLASH_SECONDS) * d * d;
    }

    public static float rateHz(int rate) {
        if (rate <= 0) return 0f;
        float r = Mth.clamp(rate, 0, 255) / 255f;
        return MIN_RATE_HZ + (MAX_RATE_HZ - MIN_RATE_HZ) * r * r;
    }

    /** Vrai si la lampe peut s'allumer avec ces valeurs : sinon, inutile de rafraichir le rendu. */
    public static boolean canLight(int intensity, int rate, int effect) {
        return intensity > 0 && (rateHz(rate) > 0f || effectOf(effect) == EFFECT_BLINDER);
    }

    /** Niveau 0..1 de la lampe entiere a l'instant donne. */
    public static float level(int intensity, int duration, int rate, int effect, double seconds, long seed) {
        return segmentLevel(intensity, duration, rate, effect, seconds, seed, 0, 1);
    }

    /**
     * Niveau 0..1 maximal sur une fenetre de temps : un flash de 12 ms tombe entre deux images ou
     * deux ticks si on ne regarde qu'un instant. Cinq echantillons suffisent pour ne rien rater.
     */
    public static float levelOverWindow(int intensity, int duration, int rate, int effect,
                                        double seconds, double window, long seed, int index, int count) {
        float best = 0f;
        for (int k = 0; k < 5; k++) {
            best = Math.max(best, segmentLevel(intensity, duration, rate, effect, seconds + window * k / 5.0, seed, index, count));
            if (best >= 1f) break;
        }
        return best;
    }

    /** Niveau 0..1 d'un segment : identique a la lampe entiere sauf pour le scintillement. */
    public static float segmentLevel(int intensity, int duration, int rate, int effect, double seconds, long seed, int index, int count) {
        float dim = Mth.clamp(intensity, 0, 255) / 255f;
        if (dim <= 0f) return 0f;
        int fx = effectOf(effect);
        if (fx == EFFECT_BLINDER) return dim;
        float hz = rateHz(rate);
        if (hz <= 0f) return 0f;
        double period = 1.0 / hz;
        double flash = Math.min(flashSeconds(duration), period * 0.85);
        switch (fx) {
            case EFFECT_RAMP_UP:
                return pulse(seconds, period, flash) * dim * envelope(seconds, period, 0);
            case EFFECT_RAMP_DOWN:
                return pulse(seconds, period, flash) * dim * envelope(seconds, period, 1);
            case EFFECT_RAMP_UP_DOWN:
                return pulse(seconds, period, flash) * dim * envelope(seconds, period, 2);
            case EFFECT_RANDOM:
                return randomFlash(seconds, period, flash, seed) * dim;
            case EFFECT_LIGHTNING:
                return lightning(seconds, period, flash, seed) * dim;
            case EFFECT_SPIKES:
                return spikes(seconds, period, flash, seed) * dim;
            case EFFECT_SPARKLE:
                return sparkle(seconds, period, flash, seed, index) * dim;
            default:
                return pulse(seconds, period, flash) * dim;
        }
    }

    private static float pulse(double seconds, double period, double flash) {
        double phase = seconds - Math.floor(seconds / period) * period;
        return phase < flash ? 1f : 0f;
    }

    /** Enveloppe sur huit flashs (au moins une seconde) : montee, descente ou aller-retour. */
    private static float envelope(double seconds, double period, int shape) {
        double env = Math.max(1.0, period * 8.0);
        float t = (float) ((seconds - Math.floor(seconds / env) * env) / env);
        if (shape == 0) return 0.08f + 0.92f * t;
        if (shape == 1) return 1.0f - 0.92f * t;
        return 0.08f + 0.92f * (t < 0.5f ? t * 2f : 2f - t * 2f);
    }

    /** Un flash sur trois environ saute, et il tombe n'importe ou dans sa periode. */
    private static float randomFlash(double seconds, double period, double flash, long seed) {
        long bin = (long) Math.floor(seconds / period);
        if (hash(seed, bin, 1) > 0.65f) return 0f;
        double start = hash(seed, bin, 2) * (period - flash);
        double phase = seconds - bin * period;
        return phase >= start && phase < start + flash ? 1f : 0f;
    }

    /** Rafales de trois a six flashs irreguliers, puis silence. */
    private static float lightning(double seconds, double period, double flash, long seed) {
        double burst = Math.max(0.8, period * 4.0);
        long bin = (long) Math.floor(seconds / burst);
        double local = seconds - bin * burst;
        int flashes = 3 + (int) (hash(seed, bin, 3) * 4f);
        float best = 0f;
        for (int k = 0; k < flashes; k++) {
            double start = hash(seed, bin, 10 + k) * burst * 0.45;
            double dur = flash * (0.4 + 0.6 * hash(seed, bin, 30 + k));
            if (local >= start && local < start + dur) {
                best = Math.max(best, 0.5f + 0.5f * hash(seed, bin, 50 + k));
            }
        }
        return best;
    }

    /** Flashs tres courts a la cadence, d'intensite aleatoire. */
    private static float spikes(double seconds, double period, double flash, long seed) {
        long bin = (long) Math.floor(seconds / period);
        double phase = seconds - bin * period;
        double dur = Math.min(flash, 0.03);
        return phase < dur ? 0.35f + 0.65f * hash(seed, bin, 4) : 0f;
    }

    /** Chaque segment scintille pour lui-meme, au double de la cadence, un tiers du temps. */
    private static float sparkle(double seconds, double period, double flash, long seed, int index) {
        double sub = period * 0.5;
        double shifted = seconds + index * sub * 0.37;
        long bin = (long) Math.floor(shifted / sub);
        if (hash(seed, bin, 100 + index) > 0.35f) return 0f;
        double phase = shifted - bin * sub;
        return phase < Math.min(flash, sub * 0.8) ? 1f : 0f;
    }

    /** Hash deterministe dans [0, 1). */
    public static float hash(long seed, long a, long b) {
        long x = seed * 0x9E3779B97F4A7C15L + a * 0xBF58476D1CE4E5B9L + b * 0x94D049BB133111EBL;
        x ^= (x >>> 30);
        x *= 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 27);
        x *= 0x94D049BB133111EBL;
        x ^= (x >>> 31);
        return (x >>> 40) / (float) (1L << 24);
    }
}
