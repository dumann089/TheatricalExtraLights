package com.github.dumann089.theatricalextralights.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.imabad.theatrical.client.TheatricalRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Rectangle coloré sur la face du strobe — l'éclairage au sol passe par la lumière dynamique Theatrical/Shimmer. */
public final class StrobeVisualEffects {

    private static final float SOURCE_BEAM_HALF_WIDTH = 0.4375f;
    private static final float SOURCE_BEAM_HALF_HEIGHT = 0.21875f;
    private static final float FACE_X = 0.5f;
    private static final float FACE_Y = 0.65f;
    private static final float FACE_Z = 0.37f;

    private StrobeVisualEffects() {
    }

    /** Pose déjà positionnée sur le fixture (preparePoseStack appliqué). */
    public static void renderFace(
            MultiBufferSource.BufferSource bufferSource,
            PoseStack poseStack,
            int r,
            int g,
            int b,
            int a
    ) {
        VertexConsumer faceConsumer = bufferSource.getBuffer(TheatricalRenderTypes.BEAM);
        poseStack.pushPose();
        poseStack.translate(FACE_X, FACE_Y, FACE_Z);
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        addVertex(faceConsumer, matrix, normal, r, g, b, a,
                -SOURCE_BEAM_HALF_WIDTH, SOURCE_BEAM_HALF_HEIGHT, 0f);
        addVertex(faceConsumer, matrix, normal, r, g, b, a,
                SOURCE_BEAM_HALF_WIDTH, SOURCE_BEAM_HALF_HEIGHT, 0f);
        addVertex(faceConsumer, matrix, normal, r, g, b, a,
                SOURCE_BEAM_HALF_WIDTH, -SOURCE_BEAM_HALF_HEIGHT, 0f);
        addVertex(faceConsumer, matrix, normal, r, g, b, a,
                -SOURCE_BEAM_HALF_WIDTH, -SOURCE_BEAM_HALF_HEIGHT, 0f);
        poseStack.popPose();
    }

    private static void addVertex(
            VertexConsumer consumer,
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
        consumer.vertex(matrix, x, y, z).color(r, g, b, a).endVertex();
    }
}
