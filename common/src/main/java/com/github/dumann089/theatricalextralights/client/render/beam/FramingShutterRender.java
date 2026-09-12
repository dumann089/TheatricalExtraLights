package com.github.dumann089.theatricalextralights.client.render.beam;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasFramingShutters;
import com.github.dumann089.theatricalextralights.util.FramingShutterState;
import net.minecraft.client.renderer.ShaderInstance;

/** Pont entre l'etat couteaux d'un block entity et les donnees de rendu / uniforms shader. */
public final class FramingShutterRender {

    private FramingShutterRender() {
    }

    /** Photo interpolee si le projecteur a des lames engagees, sinon null. */
    public static FramingShutterState.Snapshot snapshot(Object blockEntity, float partialTicks) {
        if (blockEntity instanceof HasFramingShutters hfs && hfs.hasActiveFramingShutters()) {
            return hfs.getFramingShutters().snapshot(partialTicks);
        }
        return null;
    }

    /** Retourne {@code data} enrichi de l'etat couteaux du block entity (ou tel quel si aucun). */
    public static BeamRenderData attach(BeamRenderData data, Object blockEntity, float partialTicks) {
        FramingShutterState.Snapshot snap = snapshot(blockEntity, partialTicks);
        return snap == null ? data : data.withShutters(snap);
    }

    /** True si le block entity a des lames engagees (pour couper les faisceaux non compatibles). */
    public static boolean isActive(Object blockEntity) {
        return blockEntity instanceof HasFramingShutters hfs && hfs.hasActiveFramingShutters();
    }

    /** Pousse les uniforms {@code BladeA}, {@code BladeB}, {@code FrameRotation}, {@code ShutterEnabled}. */
    public static void applyUniforms(ShaderInstance shader, FramingShutterState.Snapshot snap) {
        if (snap == null || !snap.isActive()) {
            shader.safeGetUniform("ShutterEnabled").set(0.0f);
            shader.safeGetUniform("BladeA").set(0.0f, 0.0f, 0.0f, 0.0f);
            shader.safeGetUniform("BladeB").set(0.0f, 0.0f, 0.0f, 0.0f);
            shader.safeGetUniform("FrameRotation").set(0.0f);
            return;
        }
        float[] a = snap.insertionA();
        float[] b = snap.insertionB();
        shader.safeGetUniform("ShutterEnabled").set(1.0f);
        shader.safeGetUniform("BladeA").set(a[0], a[1], a[2], a[3]);
        shader.safeGetUniform("BladeB").set(b[0], b[1], b[2], b[3]);
        shader.safeGetUniform("FrameRotation").set(snap.frameRotation());
    }
}
