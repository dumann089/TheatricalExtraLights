package com.github.dumann089.theatricalextralights.client.render.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Figure balayee par un laser pour une image : la sortie du faisceau dans le monde et une
 * liste de segments. Chaque segment relie deux directions consecutives du motif ; il porte
 * la nappe balayee entre elles, le faisceau qui s'attarde sur son premier point et, pour le
 * dernier point d'un trace ouvert, celui du second.
 *
 * <p>Tout est en repere monde. Le renderer convertit en repere vue au moment du dessin et
 * envoie le tout au GPU sous forme de texture flottante ({@link LaserRaymarchRenderer}).
 */
public final class LaserFigure {

    /** Egal a TheatricalExtraLightsConfig.MAX_LASER_SEGMENTS et a la boucle du shader. */
    public static final int MAX_SEGMENTS = 512;

    public static final int FLAG_HIT0 = 1;
    public static final int FLAG_HIT1 = 2;
    public static final int FLAG_SHEET = 4;
    public static final int FLAG_BEAM0 = 8;
    public static final int FLAG_BEAM1 = 16;

    public BlockPos fixturePos;
    public Vec3 origin = Vec3.ZERO;

    public int count;
    public final float[] dir0 = new float[MAX_SEGMENTS * 3];
    public final float[] dir1 = new float[MAX_SEGMENTS * 3];
    public final float[] len0 = new float[MAX_SEGMENTS];
    public final float[] len1 = new float[MAX_SEGMENTS];
    public final float[] span = new float[MAX_SEGMENTS];
    public final int[] color = new int[MAX_SEGMENTS];
    public final float[] sheetWeight = new float[MAX_SEGMENTS];
    public final float[] beamWeight0 = new float[MAX_SEGMENTS];
    public final float[] beamWeight1 = new float[MAX_SEGMENTS];
    public final float[] path0 = new float[MAX_SEGMENTS];
    public final float[] path1 = new float[MAX_SEGMENTS];
    public final int[] flags = new int[MAX_SEGMENTS];

    /** Intensite DMX 0..1 deja interpolee. */
    public float intensity;
    /** Angle total balaye par les nappes, radians (1 si aucune nappe). */
    public float totalSpan = 1.0f;
    /** Tangente de la demi-divergence du faisceau. */
    public float divergence;
    /** Position de la tete de balayage sur la figure, 0..1. */
    public float scanHead;
    /** Amplitude de la surbrillance derriere la tete de balayage, 0 = trait continu. */
    public float scanTrail;
    /** Striations radiales des nappes, 0 (scan rapide) .. 1 (scan lent). */
    public float striation;

    public void reset() {
        count = 0;
        totalSpan = 1.0f;
        scanTrail = 0.0f;
        striation = 0.0f;
    }

    /**
     * Ajoute un segment. Les directions doivent etre normalisees.
     *
     * @return l'index du segment, ou -1 si la figure est pleine
     */
    public int add(Vec3 d0, Vec3 d1, float l0, float l1, float spanRad, int rgb,
                   float sheetW, float beamW0, float beamW1, float p0, float p1, int flagBits) {
        if (count >= MAX_SEGMENTS) {
            return -1;
        }
        int i = count++;
        dir0[i * 3] = (float) d0.x;
        dir0[i * 3 + 1] = (float) d0.y;
        dir0[i * 3 + 2] = (float) d0.z;
        dir1[i * 3] = (float) d1.x;
        dir1[i * 3 + 1] = (float) d1.y;
        dir1[i * 3 + 2] = (float) d1.z;
        len0[i] = l0;
        len1[i] = l1;
        span[i] = spanRad;
        color[i] = rgb;
        sheetWeight[i] = sheetW;
        beamWeight0[i] = beamW0;
        beamWeight1[i] = beamW1;
        path0[i] = p0;
        path1[i] = p1;
        flags[i] = flagBits;
        return i;
    }
}
