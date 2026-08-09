package com.github.dumann089.theatricalextralights.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import dev.imabad.theatrical.client.TheatricalRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Rendu blinder : rectangle coloré sur la face — l'éclairage au sol via lumière dynamique Theatrical/Shimmer. */
public final class BlinderRenderHelper {

    public record FaceQuad(float x, float y, float z, float halfW, float halfH) {
        public static final FaceQuad STANDARD = new FaceQuad(0.5f, 0.68f, 0.422f, 0.3125f, 0.625f);
        public static final FaceQuad DOUBLE_2X2 = new FaceQuad(0.5f, 0.415f, 0.422f, 0.3125f, 0.284375f);
        public static final FaceQuad COMPACT = new FaceQuad(0.5f, 0.40625f, 0.34375f, 0.35f, 0.35f);
    }

    private BlinderRenderHelper() {
    }

    public static void renderFace(
            BaseLightBlockEntity blockEntity,
            MultiBufferSource.BufferSource bufferSource,
            PoseStack poseStack,
            float partialTicks,
            FaceQuad face
    ) {
        float intensity = StrobeRenderHelper.renderedIntensity(blockEntity, partialTicks);
        if (intensity <= 0f) {
            return;
        }

        int color = blockEntity.getColour();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) intensity;

        VertexConsumer beamConsumer = bufferSource.getBuffer(TheatricalRenderTypes.BEAM);
        poseStack.pushPose();
        poseStack.translate(face.x(), face.y(), face.z());
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        addVertex(beamConsumer, matrix, normal, r, g, b, a, -face.halfW(), face.halfH(), 0f);
        addVertex(beamConsumer, matrix, normal, r, g, b, a, face.halfW(), face.halfH(), 0f);
        addVertex(beamConsumer, matrix, normal, r, g, b, a, face.halfW(), -face.halfH(), 0f);
        addVertex(beamConsumer, matrix, normal, r, g, b, a, -face.halfW(), -face.halfH(), 0f);
        poseStack.popPose();
    }

    public static Vec3 lazyRenderPos(BaseLightBlockEntity blockEntity) {
        return blockEntity.getBlockPos().getCenter();
    }

    private static void addVertex(
            VertexConsumer builder,
            Matrix4f matrix,
            Matrix3f normal,
            int r,
            int g,
            int b,
            int a,
            float x,
            float y,
            float z
    ) {
        builder.vertex(matrix, x, y, z).color(r, g, b, a).endVertex();
    }
}
