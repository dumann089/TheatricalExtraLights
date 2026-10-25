package com.github.dumann089.theatricalextralights.client;

import com.lowdragmc.shimmer.client.postprocessing.PostProcessing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.imabad.theatrical.compat.ModCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class LensBloomCompat {
    private LensBloomCompat() {}

    private static final ResourceLocation WHITE_SPRITE = new ResourceLocation("minecraft", "block/white_concrete");
    private static TextureAtlasSprite whiteSprite;

    private static TextureAtlasSprite whiteSprite() {
        if (whiteSprite == null) {
            whiteSprite = Minecraft.getInstance()
                    .getModelManager()
                    .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .getSprite(WHITE_SPRITE);
        }
        return whiteSprite;
    }

    /** Blends a color channel toward white. amount=0 keeps the color, 1 = pure white. */
    public static int whiten(int channel, float amount) {
        return (int) Math.min(255, channel + (255 - channel) * amount);
    }

    /** Um quad único com bloom, na origem local do poseStack (fixtures de lente única). */
    public static void renderBloomedQuad(PoseStack poseStack, int r, int g, int b, float halfW, float halfH) {
        renderBloomedQuads(poseStack, (consumer, ps) -> emitQuad(consumer, ps, r, g, b, halfW, halfH));
    }

    public static void renderBloomedQuad(PoseStack poseStack, int r, int g, int b, float size) {
        renderBloomedQuad(poseStack, r, g, b, size, size);
    }

    /** Vários quads num único passe de bloom — use pra fixtures multi-lente. */
    public static void renderBloomedQuads(PoseStack poseStack, QuadBatch batch) {
        if (!ModCompat.SHIMMER) return;

        PoseStack bloomStack = deepCopyPoseStack(poseStack);

        PostProcessing.BLOOM_UNREAL.postEntityForce(bufferSource -> {
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());
            batch.draw(consumer, bloomStack);
        });
    }

    /** Compõe o bloom acumulado no frame (chamado uma vez por frame, via TheatricalClientMixin). */
    public static void flushBloomPost() {
        if (!ModCompat.SHIMMER) return;
        PostProcessing.BLOOM_UNREAL.renderEntityPost(Minecraft.getInstance().getProfiler());
    }

    /** Cópia de verdade da matriz atual (valores, não a referência), pra sobreviver até o flush no fim do frame. */
    private static PoseStack deepCopyPoseStack(PoseStack poseStack) {
        PoseStack copy = new PoseStack();
        copy.last().pose().set(poseStack.last().pose());
        copy.last().normal().set(poseStack.last().normal());
        return copy;
    }

    /** Desenha um quad na cor pura, na origem local do poseStack passado (dentro de um lote já aberto). */
    public static void emitQuad(VertexConsumer consumer, PoseStack poseStack, int r, int g, int b,
                                 float halfW, float halfH) {
        Matrix4f m = poseStack.last().pose();
        TextureAtlasSprite sprite = whiteSprite();
        float u = (sprite.getU0() + sprite.getU1()) / 2f;
        float v = (sprite.getV0() + sprite.getV1()) / 2f;

        emitVertex(consumer, m, r, g, b, -halfW,  halfH, 0f, u, v);
        emitVertex(consumer, m, r, g, b,  halfW,  halfH, 0f, u, v);
        emitVertex(consumer, m, r, g, b,  halfW, -halfH, 0f, u, v);
        emitVertex(consumer, m, r, g, b, -halfW, -halfH, 0f, u, v);
    }

    /**
     * Mesma coisa que emitQuad, mas com a ordem dos vértices invertida (winding oposto).
     * Use quando o quad aparecer virado pro lado errado com RenderType.cutout(), que faz
     * culling de face — diferente do render type de beam normal (NO_CULL), aqui a ordem importa.
     */
    public static void emitQuadReversed(VertexConsumer consumer, PoseStack poseStack, int r, int g, int b,
                                         float halfW, float halfH) {
        Matrix4f m = poseStack.last().pose();
        TextureAtlasSprite sprite = whiteSprite();
        float u = (sprite.getU0() + sprite.getU1()) / 2f;
        float v = (sprite.getV0() + sprite.getV1()) / 2f;

        emitVertex(consumer, m, r, g, b, -halfW, -halfH, 0f, u, v);
        emitVertex(consumer, m, r, g, b,  halfW, -halfH, 0f, u, v);
        emitVertex(consumer, m, r, g, b,  halfW,  halfH, 0f, u, v);
        emitVertex(consumer, m, r, g, b, -halfW,  halfH, 0f, u, v);
    }

    private static void emitVertex(VertexConsumer vc, Matrix4f m, int r, int g, int b,
                                    float x, float y, float z, float u, float v) {
        vc.vertex(m, x, y, z)
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
