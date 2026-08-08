package com.github.dumann089.theatricalextralights.client.entities;

import com.github.dumann089.theatricalextralights.client.LensRenderTypes;
import com.github.dumann089.theatricalextralights.entities.FireworkRocketEntity;
import com.github.dumann089.theatricalextralights.firework.BurstPattern;
import com.github.dumann089.theatricalextralights.firework.Spark;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class FireworkRocketRenderer extends EntityRenderer<FireworkRocketEntity> {
    private static final ResourceLocation LENS_TEXTURE = new ResourceLocation("theatricalextralights", "textures/particle/firework_core.png");

    public FireworkRocketRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FireworkRocketEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        VertexConsumer lensConsumer = buffer.getBuffer(LensRenderTypes.FLAME);
        BurstPattern pattern = entity.getPreset().getPattern();
        int color = entity.getLaunchColor();
        Vec3 entityPos = new Vec3(
                Mth.lerp(partialTick, entity.xOld, entity.getX()),
                Mth.lerp(partialTick, entity.yOld, entity.getY()),
                Mth.lerp(partialTick, entity.zOld, entity.getZ())
        );

        renderRocketHalo(entity, pattern, color, partialTick, poseStack, lensConsumer);
        renderSparks(entity, pattern, entityPos, partialTick, poseStack, lensConsumer);
    }

    private void renderRocketHalo(FireworkRocketEntity entity, BurstPattern pattern, int color, float partialTick, PoseStack poseStack, VertexConsumer consumer) {
        if (entity.isExploded()) {
            if (pattern.isDaytimePowder()) {
                return;
            }
            renderBurstFlash(entity, pattern, color, partialTick, poseStack, consumer);
            return;
        }

        if (pattern.hasInvisibleFlight()) {
            return;
        }

        float innerSize;
        float outerSize;
        float alpha;

        if (entity.isFading()) {
            int fadeMax = pattern.getCometFadeTicks();
            float fade = fadeMax > 0 ? Mth.clamp((entity.getFadeTicks() - partialTick) / fadeMax, 0.0f, 1.0f) : 0.0f;
            innerSize = pattern.getFlightHaloInnerSize() * (0.55f + 0.45f * fade);
            outerSize = pattern.getFlightHaloOuterSize() * (0.45f + 0.55f * fade);
            alpha = 0.95f * fade * fade;
            if (alpha <= 0.001f) {
                return;
            }
            poseStack.pushPose();
            faceCamera(poseStack);
            renderHaloQuad(poseStack, consumer, color, alpha * 0.55f, outerSize);
            renderHaloQuad(poseStack, consumer, color, alpha, innerSize);
            poseStack.popPose();
            return;
        }

        innerSize = pattern.getFlightHaloInnerSize();
        outerSize = pattern.getFlightHaloOuterSize();
        if (pattern.isDaytimePowder()) {
            return;
        }
        alpha = pattern.isBurst() ? 0.55f : 0.95f;
        Vec3 delta = entity.getDeltaMovement();
        Vec3 direction = delta.lengthSqr() > 1.0E-4 ? delta.normalize() : new Vec3(0.0, 1.0, 0.0);
        poseStack.pushPose();
        poseStack.translate(direction.x * 0.45, direction.y * 0.45, direction.z * 0.45);
        faceCamera(poseStack);
        renderHaloQuad(poseStack, consumer, color, alpha * 0.55f, outerSize);
        if (!pattern.isBurst()) {
            renderHaloQuad(poseStack, consumer, 0xFFFFFF, alpha, innerSize * 0.55f);
        } else {
            renderHaloQuad(poseStack, consumer, color, alpha, innerSize);
        }
        poseStack.popPose();
    }

    private void renderBurstFlash(FireworkRocketEntity entity, BurstPattern pattern, int color, float partialTick, PoseStack poseStack, VertexConsumer consumer) {
        if (pattern.isAerialStrobe()) {
            renderAerialStrobeFlash(entity, pattern, color, partialTick, poseStack, consumer);
            return;
        }
        int tickIndex = entity.getBurstTickIndex();
        float t = (tickIndex + partialTick) / 8.0f;
        if (t >= 1.0f) {
            return;
        }
        float fade = Math.max(0.0f, 1.0f - t);
        float quadratic = fade * fade;
        float baseSize = 22.0f;
        float size = baseSize * (0.55f + t * 0.85f);

        poseStack.pushPose();
        faceCamera(poseStack);
        renderHaloQuad(poseStack, consumer, color, quadratic * 0.35f, size * 1.7f);
        renderHaloQuad(poseStack, consumer, color, quadratic * 0.70f, size);
        renderHaloQuad(poseStack, consumer, 0xFFFFFF, quadratic, size * 0.45f);
        poseStack.popPose();
    }

    private void renderAerialStrobeFlash(FireworkRocketEntity entity, BurstPattern pattern, int color, float partialTick, PoseStack poseStack, VertexConsumer consumer) {
        int tickIndex = entity.getBurstTickIndex();
        if (tickIndex >= pattern.getBurstDuration()) {
            return;
        }
        int phase = (int) Math.floor(tickIndex + partialTick * 2.0f);
        boolean on = phase % 4 < 2;
        float alpha = on ? 0.95f : 0.12f;
        float inner = pattern.getBurstHaloInnerSize(tickIndex);
        float outer = pattern.getBurstHaloOuterSize(tickIndex);

        poseStack.pushPose();
        faceCamera(poseStack);
        renderHaloQuad(poseStack, consumer, color, alpha * 0.35f, outer * 1.6f);
        renderHaloQuad(poseStack, consumer, color, alpha * 0.70f, outer);
        renderHaloQuad(poseStack, consumer, 0xFFFFFF, alpha, inner);
        poseStack.popPose();
    }

    private void renderSparks(FireworkRocketEntity entity, BurstPattern pattern, Vec3 entityPos, float partialTick, PoseStack poseStack, VertexConsumer consumer) {
        double maxRenderDistSq = 160.0 * 160.0;
        for (Spark spark : entity.getSparks()) {
            float alpha = spark.getAlpha(partialTick);
            if (alpha <= 0.0f) {
                continue;
            }
            Vec3 worldPos = spark.getPosition(partialTick);
            Vec3 offset = worldPos.subtract(entityPos);
            if (offset.lengthSqr() > maxRenderDistSq) {
                continue;
            }
            float scale = spark.getScale(partialTick);

            poseStack.pushPose();
            poseStack.translate(offset.x, offset.y, offset.z);
            faceCamera(poseStack);
            if (pattern.isDaytimePowder()) {
                renderPowderCloud(poseStack, consumer, spark.color, alpha, scale);
            } else {
                renderHaloQuad(poseStack, consumer, spark.color, alpha * 0.55f, scale * 1.6f);
                renderHaloQuad(poseStack, consumer, 0xFFFFFF, alpha * 0.95f, scale * 0.55f);
                renderHaloQuad(poseStack, consumer, spark.color, alpha, scale);
            }
            poseStack.popPose();
        }
    }

    private void faceCamera(PoseStack poseStack) {
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
    }

    @Override
    public ResourceLocation getTextureLocation(FireworkRocketEntity entity) {
        return LENS_TEXTURE;
    }

    private static void renderPowderCloud(PoseStack poseStack, VertexConsumer consumer, int color, float alpha, float scale) {
        renderHaloQuad(poseStack, consumer, color, alpha * 0.35f, scale * 2.4f);
        renderHaloQuad(poseStack, consumer, color, alpha * 0.55f, scale * 1.6f);
        renderHaloQuad(poseStack, consumer, color, alpha * 0.85f, scale);
    }

    private static void renderHaloQuad(PoseStack poseStack, VertexConsumer consumer, int color, float alpha, float size) {
        Matrix4f matrix = poseStack.last().pose();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = Math.max(0, Math.min(255, (int) (alpha * 255.0f)));

        addVertex(consumer, matrix, r, g, b, a, -size, size, 0.0f, 0.0f, 0.0f);
        addVertex(consumer, matrix, r, g, b, a, size, size, 0.0f, 1.0f, 0.0f);
        addVertex(consumer, matrix, r, g, b, a, size, -size, 0.0f, 1.0f, 1.0f);
        addVertex(consumer, matrix, r, g, b, a, -size, -size, 0.0f, 0.0f, 1.0f);
    }

    private static void addVertex(
            VertexConsumer vc,
            Matrix4f m,
            int r, int g, int b, int a,
            float x, float y, float z,
            float u, float v
    ) {
        vc.vertex(m, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .uv2(LightTexture.FULL_BRIGHT)
                .endVertex();
    }
}
