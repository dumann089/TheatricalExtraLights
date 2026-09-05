package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasGobo;
import com.github.dumann089.theatricalextralights.client.CustomGoboLoader;
import com.github.dumann089.theatricalextralights.client.ModShaders;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.util.GlobalGoboManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.client.LazyRenderers;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class GoboGPUProjector {
    private final Vector4f tmpPos = new Vector4f();
    private final Vector4f tmpDir = new Vector4f();
    private final Vector4f tmpU = new Vector4f();
    private final Vector4f tmpV = new Vector4f();
    public <T extends BlockEntity & HasGobo> void render(T be, MultiBufferSource multiBufferSource, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging, Vec3 localLensOffset, float[] panPivot, float[] tiltPivot, float[] structuralTransform, float minAngle, float maxAngle, float smoothPan, float smoothTilt, float baseRadius) {
        if (be.getLevel() == null) return;
        float intensity = be.getPartialIntensity(partialTicks) / 255.0f;
        if (intensity <= 0.0f) return;
        int goboSlot = be.getGobo();
        if (goboSlot < 0) return;
        ResourceLocation texture = resolveGoboTexture(be, goboSlot);
        if (texture == null) return;
        float zoomNorm = be.getPartialZoom(partialTicks) / 255.0f;
        float coneHalfAngle = minAngle + zoomNorm * (maxAngle - minAngle);
        float tanHalfAngle = (float)Math.tan(Math.toRadians(coneHalfAngle));
        float maxDistance = TheatricalExtraLightsConfig.getMaxGoboDistance();
        final float finalBaseRadius = baseRadius;

        PoseStack fixtureStack = new PoseStack();
        applyFixtureOrientation(fixtureStack, facing, isFlipped, blockState, isHanging, smoothPan, smoothTilt, panPivot, tiltPivot, structuralTransform);
        fixtureStack.translate(localLensOffset.x, localLensOffset.y, localLensOffset.z);
        Matrix4f fixtureMatrix = new Matrix4f(fixtureStack.last().pose());

        tmpPos.set(0f, 0f, 0f, 1f);
        fixtureMatrix.transform(tmpPos);
        tmpDir.set(0f, 0f, -1f, 0f);
        fixtureMatrix.transform(tmpDir);
        tmpDir.normalize();
        tmpU.set(1f, 0f, 0f, 0f);
        fixtureMatrix.transform(tmpU);
        tmpU.normalize();
        tmpV.set(0f, 1f, 0f, 0f);
        fixtureMatrix.transform(tmpV);
        tmpV.normalize();

        final Vec3 origin = Vec3.atLowerCornerOf(be.getBlockPos()).add(tmpPos.x(), tmpPos.y(), tmpPos.z());
        final Vec3 beamDir = new Vec3(tmpDir.x(), tmpDir.y(), tmpDir.z()).normalize();
        final Vec3 axisU = new Vec3(tmpU.x(), tmpU.y(), tmpU.z()).normalize();
        final Vec3 axisV = new Vec3(tmpV.x(), tmpV.y(), tmpV.z()).normalize();

        // Raycast: detecta la obstrucción, pero NO recorta MaxLen.
        Vec3 rayStart = origin.add(beamDir.scale(0.35f));
        Vec3 rayEnd = origin.add(beamDir.scale(maxDistance));
        BlockHitResult hitResult = be.getLevel().clip(new ClipContext(rayStart, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));

        final boolean hasOcclusion = hitResult.getType() != HitResult.Type.MISS;
        final Vec3 finalOcclusionPos = hasOcclusion ? hitResult.getLocation() : Vec3.ZERO;

        Direction hitDirection = hasOcclusion ? hitResult.getDirection() : Direction.UP;
        final Vec3 finalOcclusionNormal = new Vec3(hitDirection.getStepX(), hitDirection.getStepY(), hitDirection.getStepZ()).normalize();

        final float finalFocus = be.getFocus() / 255.0f;
        final float finalIntensity = intensity;
        final float finalTanHalfAngle = tanHalfAngle;
        final float finalMaxDistance = maxDistance;
        final float finalRotation = be.getGoboRotation();
        final ResourceLocation finalTexture = texture;
        final BlockPos finalBlockPos = be.getBlockPos();
        final int finalColor = be.getColour() == 0 ? 0xFFFFFF : be.getColour();

        LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {
            @Override
            public void render(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Camera camera, float partialTick) {
                Minecraft mc = Minecraft.getInstance();
                ShaderInstance shader = ModShaders.goboProjectorShader;
                if (shader == null || mc.getMainRenderTarget() == null) return;

                PoseStack viewStack = new PoseStack();
                viewStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
                viewStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
                Matrix4f viewMatrix = viewStack.last().pose();
                Vec3 cameraPos = camera.getPosition();

                tmpPos.set((float)(origin.x - cameraPos.x), (float)(origin.y - cameraPos.y), (float)(origin.z - cameraPos.z), 1.0f);
                viewMatrix.transform(tmpPos);
                tmpDir.set((float)beamDir.x, (float)beamDir.y, (float)beamDir.z, 0.0f);
                viewMatrix.transform(tmpDir);
                tmpDir.normalize();
                tmpU.set((float)axisU.x, (float)axisU.y, (float)axisU.z, 0.0f);
                viewMatrix.transform(tmpU);
                tmpU.normalize();
                tmpV.set((float)axisV.x, (float)axisV.y, (float)axisV.z, 0.0f);
                viewMatrix.transform(tmpV);
                tmpV.normalize();

                shader.safeGetUniform("LightPos").set(tmpPos.x(), tmpPos.y(), tmpPos.z());
                shader.safeGetUniform("LightDir").set(tmpDir.x(), tmpDir.y(), tmpDir.z());
                shader.safeGetUniform("AxisU").set(tmpU.x(), tmpU.y(), tmpU.z());
                shader.safeGetUniform("AxisV").set(tmpV.x(), tmpV.y(), tmpV.z());
                shader.safeGetUniform("TanHalfAngle").set(finalTanHalfAngle);
                shader.safeGetUniform("MaxLen").set(finalMaxDistance);
                shader.safeGetUniform("MaxGoboDist").set(finalMaxDistance);
                shader.safeGetUniform("GoboRotation").set(finalRotation);
                shader.safeGetUniform("Intensity").set(finalIntensity);
                shader.safeGetUniform("Focus").set(finalFocus);
                shader.safeGetUniform("BaseRadius").set(finalBaseRadius);

                // Posición del obstáculo en view-space.
                if (hasOcclusion) {
                    tmpPos.set((float)(finalOcclusionPos.x - cameraPos.x), (float)(finalOcclusionPos.y - cameraPos.y), (float)(finalOcclusionPos.z - cameraPos.z), 1.0f);
                    viewMatrix.transform(tmpPos);
                    shader.safeGetUniform("OcclusionPos").set(tmpPos.x(), tmpPos.y(), tmpPos.z());

                    tmpDir.set((float)finalOcclusionNormal.x, (float)finalOcclusionNormal.y, (float)finalOcclusionNormal.z, 0.0f);
                    viewMatrix.transform(tmpDir);
                    tmpDir.normalize();
                    shader.safeGetUniform("OcclusionNormal").set(tmpDir.x(), tmpDir.y(), tmpDir.z());
                    shader.safeGetUniform("OcclusionEnabled").set(1.0f);
                } else {
                    shader.safeGetUniform("OcclusionPos").set(0.0f, 0.0f, 0.0f);
                    shader.safeGetUniform("OcclusionNormal").set(0.0f, 1.0f, 0.0f);
                    shader.safeGetUniform("OcclusionEnabled").set(0.0f);
                }

                int r = (finalColor >> 16) & 0xFF;
                int g = (finalColor >> 8) & 0xFF;
                int b = finalColor & 0xFF;
                shader.safeGetUniform("LightColor").set(r / 255.0f, g / 255.0f, b / 255.0f);
                shader.safeGetUniform("ScreenSize").set((float)mc.getMainRenderTarget().width, (float)mc.getMainRenderTarget().height);

                Matrix4f inverseProjection = new Matrix4f(RenderSystem.getProjectionMatrix()).invert();
                shader.safeGetUniform("InvProjMat").set(inverseProjection);

                RenderType renderType = ModShaders.getGoboRenderType(finalTexture);
                renderType.setupRenderState();
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.setShader(() -> shader);

                int goboTexture = mc.getTextureManager().getTexture(finalTexture).getId();
                RenderSystem.setShaderTexture(0, goboTexture);
                shader.setSampler("Sampler0", goboTexture);

                int depthTexture = mc.getMainRenderTarget().getDepthTextureId();
                RenderSystem.setShaderTexture(1, depthTexture);
                shader.setSampler("Sampler1", depthTexture);
                shader.apply();

                Tesselator tess = Tesselator.getInstance();
                BufferBuilder bb = tess.getBuilder();
                bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
                bb.vertex(-1.0f, -1.0f, 0.0f).color(255, 255, 255, 255).uv(0.0f, 0.0f).endVertex();
                bb.vertex(1.0f, -1.0f, 0.0f).color(255, 255, 255, 255).uv(1.0f, 0.0f).endVertex();
                bb.vertex(1.0f, 1.0f, 0.0f).color(255, 255, 255, 255).uv(1.0f, 1.0f).endVertex();
                bb.vertex(-1.0f, 1.0f, 0.0f).color(255, 255, 255, 255).uv(0.0f, 1.0f).endVertex();
                BufferUploader.drawWithShader(bb.end());

                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
                renderType.clearRenderState();
            }

            @Override
            public Vec3 getPos(float partialTick) {
                return finalBlockPos.getCenter();
            }
        });
    }

    private ResourceLocation resolveGoboTexture(HasGobo be, int slot) {
        String customFileName = GlobalGoboManager.getCustomGobo(be.getGoboLibrary(), slot);
        if (customFileName != null) {
            ResourceLocation custom = CustomGoboLoader.getOrCreateCustomGobo(customFileName);
            if (custom != null) return custom;
        }
        ResourceLocation texture = be.getGoboLibrary().getTexture(slot);
        if (texture != null) return texture;
        return new ResourceLocation("theatricalextralights", "textures/gobos/generic_1/open.png");
    }

    private static void applyFixtureOrientation(PoseStack ps, Direction facing, boolean isFlipped, BlockState blockState, boolean isHanging, float panDeg, float tiltDeg, float[] pans, float[] tilts, float[] structuralTransform) {
        ps.translate(0.5f, 0f, 0.5f);
        if (isHanging) {
            Direction hangDir = Direction.UP;
            try {
                hangDir = blockState.getValue(HangableBlock.HANG_DIRECTION);
            } catch (Exception ignored) {}
            ps.translate(0, 0.5, 0);
            if (hangDir.getAxis() != Direction.Axis.Y) {
                if (hangDir.getAxis() == Direction.Axis.Z) {
                    ps.mulPose(Axis.ZP.rotationDegrees(90));
                    ps.mulPose(hangDir == Direction.SOUTH ? Axis.XP.rotationDegrees(-90) : Axis.XP.rotationDegrees(90));
                } else {
                    ps.mulPose(Axis.ZN.rotationDegrees(-90));
                }
            }
            ps.translate(0, -0.5, 0);
        }
        ps.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        ps.translate(-0.5f, 0f, -0.5f);
        if (isHanging) {
            ps.translate(structuralTransform[0], structuralTransform[1], structuralTransform[2]);
            ps.translate(0, -0.08, 0);
        }
        if (isFlipped) {
            ps.translate(0.5f, 0.5f, 0.5f);
            ps.mulPose(Axis.ZP.rotationDegrees(180));
            ps.translate(-0.5f, -0.5f, -0.5f);
        }
        ps.translate(pans[0], pans[1], pans[2]);
        ps.mulPose(Axis.YP.rotationDegrees(panDeg));
        ps.translate(-pans[0], -pans[1], -pans[2]);
        ps.translate(tilts[0], tilts[1], tilts[2]);
        ps.mulPose(isFlipped ? Axis.XP.rotationDegrees(-180) : Axis.XP.rotationDegrees(180));
        ps.mulPose(Axis.XP.rotationDegrees(tiltDeg));
        ps.translate(-tilts[0], -tilts[1], -tilts[2]);
    }
}