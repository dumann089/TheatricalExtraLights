
package com.github.dumann089.theatricalextralights.client;

import com.lowdragmc.shimmer.client.postprocessing.PostProcessing;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import dev.imabad.theatrical.compat.ModCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.slf4j.Logger;

public final class LensBloomCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation WHITE_SPRITE =
            new ResourceLocation("minecraft", "block/white_concrete");

    private static TextureAtlasSprite whiteSprite;

    private static RenderTarget lastBloomTarget;
    private static int lastFramebufferId = -1;
    private static int lastColorTextureId = -1;
    private static int lastDepthTextureId = -1;
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    private static boolean refreshWarningReported;

    private LensBloomCompat() {
    }

    private static TextureAtlasSprite whiteSprite() {
        if (whiteSprite == null) {
            whiteSprite = Minecraft.getInstance()
                    .getModelManager()
                    .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .getSprite(WHITE_SPRITE);
        }

        return whiteSprite;
    }

    /**
     * Detects framebuffer replacement and reallocation performed by BBS FS.
     *
     * Shimmer caches its post-processing chain and depth/color attachments.
     * Those resources must be recreated when the active main target changes.
     */
    private static void refreshBloomFramebuffer() {
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget target = minecraft.getMainRenderTarget();

        if (target == null) {
            return;
        }

        int framebufferId = target.frameBufferId;
        int colorTextureId = target.getColorTextureId();
        int depthTextureId = target.getDepthTextureId();
        int width = target.width;
        int height = target.height;

        boolean changed = target != lastBloomTarget
                || framebufferId != lastFramebufferId
                || colorTextureId != lastColorTextureId
                || depthTextureId != lastDepthTextureId
                || width != lastWidth
                || height != lastHeight;

        if (!changed) {
            return;
        }

        try {
            PostProcessing.BLOOM_UNREAL.onResourceManagerReload(
                    minecraft.getResourceManager()
            );

            lastBloomTarget = target;
            lastFramebufferId = framebufferId;
            lastColorTextureId = colorTextureId;
            lastDepthTextureId = depthTextureId;
            lastWidth = width;
            lastHeight = height;

            refreshWarningReported = false;
        } catch (RuntimeException exception) {
            if (!refreshWarningReported) {
                refreshWarningReported = true;

                LOGGER.warn(
                        "[TheatricalExtraLights] Could not refresh Shimmer bloom framebuffer",
                        exception
                );
            }
        }
    }

    /** Blends a color channel toward white. amount=0 keeps the color, 1 = pure white. */
    public static int whiten(int channel, float amount) {
        return (int) Math.min(255, channel + (255 - channel) * amount);
    }

    /** Single bloom quad for fixtures with one lens. */
    public static void renderBloomedQuad(
            PoseStack poseStack,
            int r,
            int g,
            int b,
            float halfW,
            float halfH
    ) {
        renderBloomedQuads(
                poseStack,
                (consumer, ps) -> emitQuad(
                        consumer,
                        ps,
                        r,
                        g,
                        b,
                        halfW,
                        halfH
                )
        );
    }

    public static void renderBloomedQuad(
            PoseStack poseStack,
            int r,
            int g,
            int b,
            float size
    ) {
        renderBloomedQuad(
                poseStack,
                r,
                g,
                b,
                size,
                size
        );
    }

    /** Multiple bloom quads in one post-processing submission. */
    public static void renderBloomedQuads(
            PoseStack poseStack,
            QuadBatch batch
    ) {
        if (!ModCompat.SHIMMER) {
            return;
        }

        PoseStack bloomStack = deepCopyPoseStack(poseStack);

        PostProcessing.BLOOM_UNREAL.postEntityForce(bufferSource -> {
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());
            batch.draw(consumer, bloomStack);
        });
    }

    /**
     * Composes accumulated fixture bloom.
     * Called once after Theatrical finishes rendering its world content.
     */
    public static void flushBloomPost() {
        if (!ModCompat.SHIMMER) {
            return;
        }

        refreshBloomFramebuffer();

        PostProcessing.BLOOM_UNREAL.renderEntityPost(
                Minecraft.getInstance().getProfiler()
        );
    }

    /** Copies the actual matrix values so the pose survives until the bloom flush. */
    private static PoseStack deepCopyPoseStack(PoseStack poseStack) {
        PoseStack copy = new PoseStack();

        copy.last().pose().set(poseStack.last().pose());
        copy.last().normal().set(poseStack.last().normal());

        return copy;
    }

    /** Emits a quad in the local origin of the supplied pose stack. */
    public static void emitQuad(
            VertexConsumer consumer,
            PoseStack poseStack,
            int r,
            int g,
            int b,
            float halfW,
            float halfH
    ) {
        Matrix4f matrix = poseStack.last().pose();

        TextureAtlasSprite sprite = whiteSprite();

        float u = (sprite.getU0() + sprite.getU1()) / 2f;
        float v = (sprite.getV0() + sprite.getV1()) / 2f;

        emitVertex(consumer, matrix, r, g, b, -halfW, halfH, 0f, u, v);
        emitVertex(consumer, matrix, r, g, b, halfW, halfH, 0f, u, v);
        emitVertex(consumer, matrix, r, g, b, halfW, -halfH, 0f, u, v);
        emitVertex(consumer, matrix, r, g, b, -halfW, -halfH, 0f, u, v);
    }

    /** Emits the same quad with reversed vertex winding. */
    public static void emitQuadReversed(
            VertexConsumer consumer,
            PoseStack poseStack,
            int r,
            int g,
            int b,
            float halfW,
            float halfH
    ) {
        Matrix4f matrix = poseStack.last().pose();

        TextureAtlasSprite sprite = whiteSprite();

        float u = (sprite.getU0() + sprite.getU1()) / 2f;
        float v = (sprite.getV0() + sprite.getV1()) / 2f;

        emitVertex(consumer, matrix, r, g, b, -halfW, -halfH, 0f, u, v);
        emitVertex(consumer, matrix, r, g, b, halfW, -halfH, 0f, u, v);
        emitVertex(consumer, matrix, r, g, b, halfW, halfH, 0f, u, v);
        emitVertex(consumer, matrix, r, g, b, -halfW, halfH, 0f, u, v);
    }

    private static void emitVertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            int r,
            int g,
            int b,
            float x,
            float y,
            float z,
            float u,
            float v
    ) {
        consumer.vertex(matrix, x, y, z)
                .color(r, g, b, 255)
                .uv(u, v)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();
    }

    @FunctionalInterface
    public interface QuadBatch {
        void draw(VertexConsumer consumer, PoseStack poseStack);
    }
}
