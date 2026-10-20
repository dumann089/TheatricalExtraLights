package com.github.dumann089.theatricalextralights.client.render.beam.raymarch;

import com.github.dumann089.theatricalextralights.client.ExtraLightsRenderTypes;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.world.inventory.InventoryMenu;

import java.lang.reflect.Method;

/**
 * One depth-buffer snapshot per frame for screen-space raymarch occlusion.
 */
public final class SceneDepthCopy {
    private static TextureTarget depthCopy;
    private static boolean capturedThisFrame;
    private static boolean depthCopyStencil;
    private static int lastWidth = -1;
    private static int lastHeight = -1;
    /** Image (tick + fraction) de la derniere copie, pour ne pas la jeter dans la meme image. */
    private static long captureFrameKey = Long.MIN_VALUE;

    private SceneDepthCopy() {}

    /**
     * Invalide la copie pour une nouvelle image. Sans effet si la copie a deja ete faite dans
     * l'image courante : un fixture qui s'enregistre apres la capture (rendu differe, seconde
     * passe) ne doit pas forcer une recapture, elle contiendrait alors les faisceaux plats
     * deja dessines et trouerait tout ce qui passe derriere.
     */
    public static void beginFrame() {
        if (capturedThisFrame && captureFrameKey == frameKey()) {
            return;
        }
        capturedThisFrame = false;
    }

    private static long frameKey() {
        Minecraft mc = Minecraft.getInstance();
        long tick = mc.level != null ? mc.level.getGameTime() : 0L;
        return tick * 4096L + (long) (mc.getFrameTime() * 4095.0f);
    }

    /**
     * Vide les tampons de geometrie opaque des block entities (corps des projecteurs, modeles)
     * puis copie la profondeur, sans vider les render types custom : les quads de faisceau 2D
     * (ExtraLightsRenderTypes.BEAM) ecrivent la profondeur et, s'ils sont copies, ils trouent
     * les faisceaux raymarch et les lasers qui passent derriere eux.
     */
    public static void flushOpaqueAndCapture(MultiBufferSource.BufferSource buffers) {
        if (capturedThisFrame) return;
        buffers.endBatch(RenderType.solid());
        buffers.endBatch(RenderType.cutoutMipped());
        buffers.endBatch(RenderType.cutout());
        buffers.endBatch(RenderType.translucent());
        buffers.endBatch(Sheets.solidBlockSheet());
        buffers.endBatch(Sheets.cutoutBlockSheet());
        buffers.endBatch(Sheets.translucentCullBlockSheet());
        buffers.endBatch(RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS));
        buffers.endBatch(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
        buffers.endBatch(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        buffers.endBatch(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));
        capture();
    }

    public static void capture() {
        if (capturedThisFrame) return;

        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();

        if (main == null || main.getDepthTextureId() == 0) {
            capturedThisFrame = false;
            return;
        }

        boolean mainStencil = isStencilEnabled(main);
        ensureSize(main.width, main.height, mainStencil);

        depthCopy.copyDepthFrom(main);
        main.bindWrite(false);

        capturedThisFrame = true;
        captureFrameKey = frameKey();
    }

    public static int getDepthTextureId() {
        if (depthCopy == null || !capturedThisFrame) return 0;
        return depthCopy.getDepthTextureId();
    }

    public static boolean hasDepth() {
        return depthCopy != null
                && capturedThisFrame
                && depthCopy.getDepthTextureId() != 0;
    }

    private static void ensureSize(int width, int height, boolean stencil) {
        if (depthCopy != null
                && lastWidth == width
                && lastHeight == height
                && depthCopyStencil == stencil) {
            return;
        }

        if (depthCopy != null) {
            depthCopy.destroyBuffers();
            depthCopy = null;
        }

        depthCopy = new TextureTarget(width, height, true, Minecraft.ON_OSX);

        if (stencil) {
            enableStencil(depthCopy);
        }

        depthCopy.setClearColor(0f, 0f, 0f, 0f);

        depthCopyStencil = stencil;
        lastWidth = width;
        lastHeight = height;
    }

    private static boolean isStencilEnabled(RenderTarget target) {
        try {
            Method method = target.getClass().getMethod("isStencilEnabled");
            Object result = method.invoke(target);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void enableStencil(RenderTarget target) {
        try {
            Method method = target.getClass().getMethod("enableStencil");
            method.invoke(target);
        } catch (Throwable ignored) {
        }
    }
}