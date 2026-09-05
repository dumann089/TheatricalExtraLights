package com.github.dumann089.theatricalextralights.client.render.beam.raymarch;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;

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

    private SceneDepthCopy() {}

    public static void beginFrame() {
        capturedThisFrame = false;
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