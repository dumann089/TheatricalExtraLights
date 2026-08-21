package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasGobo;
import com.github.dumann089.theatricalextralights.client.ModShaders;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class GoboGPUProjector {
    private final Vector4f originVec = new Vector4f();
    private final Vector4f directionVec = new Vector4f();
    private final Vector4f axisUVec = new Vector4f();
    private final Vector4f axisVVec = new Vector4f();
    private final PoseStack projectorPoseStack = new PoseStack();

    private static final Matrix4f cachedProjMatrix = new Matrix4f();
    private static final Matrix4f cachedInvProjMatrix = new Matrix4f();
    private static long lastTimeMillis = 0L;
    private static float cachedTime = 0f;

    public <T extends BlockEntity & HasGobo> void render(
            T be,
            MultiBufferSource multiBufferSource,
            ResourceLocation tex0,
            ResourceLocation tex1,
            float wheelProgress,
            Direction facing,
            float partialTicks,
            boolean isFlipped,
            BlockState blockState,
            boolean isHanging,
            Vec3 localLensOffset,
            float[] panPivot,
            float[] tiltPivot,
            float[] structuralTransform,
            float minAngle,
            float maxAngle,
            float smoothPan,
            float smoothTilt) {

        if (be.getLevel() == null) return;

        float intensity =
                be.getPartialIntensity(partialTicks) / 255f;

        if (intensity <= 0.0001f) return;

        int colour =
                be.getColour() == 0
                        ? 0xFFFFFF
                        : be.getColour();

        float zoom =
                be.getPartialZoom(partialTicks) / 255f;

        float coneAngle =
                minAngle +
                        zoom * (maxAngle - minAngle);

        float tanHalfAngle =
                (float)Math.tan(
                        Math.toRadians(coneAngle)
                );

        float maxLen =
                TheatricalExtraLightsConfig
                        .getMaxGoboDistance();

        if (maxLen <= 0.01f) return;

        projectorPoseStack.setIdentity();

        applyFixtureOrientation(
                projectorPoseStack,
                be,
                facing,
                isFlipped,
                blockState,
                isHanging,
                smoothPan,
                smoothTilt,
                panPivot,
                tiltPivot,
                structuralTransform
        );

        projectorPoseStack.translate(
                localLensOffset.x,
                localLensOffset.y,
                localLensOffset.z
        );

        Matrix4f fixtureMatrix =
                new Matrix4f(
                        projectorPoseStack.last().pose()
                );

        originVec.set(
                0f,
                0f,
                0f,
                1f
        );

        fixtureMatrix.transform(originVec);

        directionVec.set(
                0f,
                0f,
                -1f,
                0f
        );

        fixtureMatrix.transform(directionVec);

        Vec3 blockOrigin =
                Vec3.atLowerCornerOf(
                        be.getBlockPos()
                );

        Vec3 origin =
                blockOrigin.add(
                        originVec.x(),
                        originVec.y(),
                        originVec.z()
                );

        Vec3 beamDir =
                new Vec3(
                        directionVec.x(),
                        directionVec.y(),
                        directionVec.z()
                ).normalize();

        Vec3 rawU =
                new Vec3(
                        fixtureMatrix.m00(),
                        fixtureMatrix.m01(),
                        fixtureMatrix.m02()
                ).normalize();

        Vec3 rawV =
                new Vec3(
                        fixtureMatrix.m10(),
                        fixtureMatrix.m11(),
                        fixtureMatrix.m12()
                ).normalize();

        Vec3 axisU =
                rawU.subtract(
                        beamDir.scale(
                                rawU.dot(beamDir)
                        )
                );

        if (axisU.lengthSqr() < 0.000001) return;

        axisU = axisU.normalize();

        Vec3 axisV =
                beamDir.cross(axisU);

        if (axisV.lengthSqr() < 0.000001) return;

        axisV = axisV.normalize();

        if (axisV.dot(rawV) < 0.0) {
            axisV = axisV.scale(-1.0);
        }

        float goboRotation =
                be.getGoboRotation();

        if (Math.abs(goboRotation) > 0.001f) {
            double rad =
                    Math.toRadians(goboRotation);

            double cos =
                    Math.cos(rad);

            double sin =
                    Math.sin(rad);

            Vec3 oldU = axisU;
            Vec3 oldV = axisV;

            axisU =
                    oldU.scale(cos)
                            .add(oldV.scale(-sin))
                            .normalize();

            axisV =
                    oldU.scale(sin)
                            .add(oldV.scale(cos))
                            .normalize();
        }

        final int finalColour = colour;
        final float finalIntensity = intensity;
        final BlockPos finalBlockPos = be.getBlockPos();
        final Vec3 finalOrigin = origin;
        final Vec3 finalBeamDir = beamDir;
        final Vec3 finalAxisU = axisU;
        final Vec3 finalAxisV = axisV;
        final float finalMaxLen = maxLen;
        final float finalTanHalfAngle = tanHalfAngle;
        final ResourceLocation finalTex0 = tex0;
        final ResourceLocation finalTex1 = tex1;
        final float finalWheelProgress = wheelProgress;

        LazyRenderers.addLazyRender(
                new LazyRenderers.LazyRenderer() {
                    @Override
                    public void render(
                            MultiBufferSource.BufferSource bufferSource,
                            PoseStack poseStack,
                            Camera camera,
                            float partialTick) {

                        poseStack.pushPose();

                        Matrix4f viewMatrix =
                                new Matrix4f(
                                        poseStack.last().pose()
                                );

                        Vec3 cameraPos =
                                camera.getPosition();

                        Vec3 relativeOrigin =
                                finalOrigin.subtract(
                                        cameraPos
                                );

                        originVec.set(
                                (float)relativeOrigin.x,
                                (float)relativeOrigin.y,
                                (float)relativeOrigin.z,
                                1f
                        );

                        viewMatrix.transform(originVec);

                        directionVec.set(
                                (float)finalBeamDir.x,
                                (float)finalBeamDir.y,
                                (float)finalBeamDir.z,
                                0f
                        );

                        viewMatrix.transform(directionVec);

                        axisUVec.set(
                                (float)finalAxisU.x,
                                (float)finalAxisU.y,
                                (float)finalAxisU.z,
                                0f
                        );

                        viewMatrix.transform(axisUVec);

                        axisVVec.set(
                                (float)finalAxisV.x,
                                (float)finalAxisV.y,
                                (float)finalAxisV.z,
                                0f
                        );

                        viewMatrix.transform(axisVVec);

                        ShaderInstance shader =
                                ModShaders.goboProjectorShader;

                        if (shader != null) {
                            Matrix4f currentProj =
                                    RenderSystem.getProjectionMatrix();

                            if (!currentProj.equals(
                                    cachedProjMatrix)) {

                                cachedProjMatrix.set(
                                        currentProj
                                );

                                cachedInvProjMatrix
                                        .set(currentProj)
                                        .invert();
                            }

                            shader.safeGetUniform(
                                    "InvProjMat"
                            ).set(
                                    cachedInvProjMatrix
                            );

                            float lDir =
                                    (float)Math.sqrt(
                                            directionVec.x() * directionVec.x() +
                                                    directionVec.y() * directionVec.y() +
                                                    directionVec.z() * directionVec.z()
                                    );

                            float lU =
                                    (float)Math.sqrt(
                                            axisUVec.x() * axisUVec.x() +
                                                    axisUVec.y() * axisUVec.y() +
                                                    axisUVec.z() * axisUVec.z()
                                    );

                            float lV =
                                    (float)Math.sqrt(
                                            axisVVec.x() * axisVVec.x() +
                                                    axisVVec.y() * axisVVec.y() +
                                                    axisVVec.z() * axisVVec.z()
                                    );

                            shader.safeGetUniform(
                                    "LightPos"
                            ).set(
                                    originVec.x(),
                                    originVec.y(),
                                    originVec.z()
                            );

                            shader.safeGetUniform(
                                    "LightDir"
                            ).set(
                                    directionVec.x() / lDir,
                                    directionVec.y() / lDir,
                                    directionVec.z() / lDir
                            );

                            shader.safeGetUniform(
                                    "AxisU"
                            ).set(
                                    axisUVec.x() / lU,
                                    axisUVec.y() / lU,
                                    axisUVec.z() / lU
                            );

                            shader.safeGetUniform(
                                    "AxisV"
                            ).set(
                                    axisVVec.x() / lV,
                                    axisVVec.y() / lV,
                                    axisVVec.z() / lV
                            );

                            shader.safeGetUniform(
                                    "TanHalfAngle"
                            ).set(
                                    finalTanHalfAngle
                            );

                            shader.safeGetUniform(
                                    "MaxLen"
                            ).set(
                                    finalMaxLen
                            );

                            shader.safeGetUniform(
                                    "WheelProgress"
                            ).set(
                                    finalWheelProgress
                            );

                            Minecraft mc =
                                    Minecraft.getInstance();

                            shader.safeGetUniform(
                                    "ScreenSize"
                            ).set(
                                    (float)mc.getMainRenderTarget().width,
                                    (float)mc.getMainRenderTarget().height
                            );

                            RenderSystem.setShaderTexture(
                                    2,
                                    mc.getMainRenderTarget()
                                            .getDepthTextureId()
                            );

                            long now =
                                    System.currentTimeMillis();

                            if (now - lastTimeMillis >= 16L) {
                                lastTimeMillis = now;
                                cachedTime =
                                        (now % 100000L) / 1000.0f;
                            }

                            shader.safeGetUniform(
                                    "Time"
                            ).set(cachedTime);

                            shader.safeGetUniform(
                                    "RaymarchingIntensity"
                            ).set(
                                    TheatricalExtraLightsConfig
                                            .getGoboRaymarchingIntensity()
                            );

                            shader.safeGetUniform(
                                    "SmokeNoiseAmount"
                            ).set(
                                    TheatricalExtraLightsConfig
                                            .getGoboSmokeNoiseAmount()
                            );
                        }

                        int r =
                                (finalColour >> 16) & 0xFF;

                        int g =
                                (finalColour >> 8) & 0xFF;

                        int b =
                                finalColour & 0xFF;

                        int alpha =
                                (int)(
                                        finalIntensity *
                                                255f
                                );

                        RenderType renderType =
                                ModShaders.getGoboRenderType(
                                        finalTex0,
                                        finalTex1
                                );

                        VertexConsumer vc =
                                bufferSource.getBuffer(
                                        renderType
                                );

                        float nearDist = 0.05f;
                        float farDist = finalMaxLen;

                        float nearRadius =
                                nearDist *
                                        finalTanHalfAngle;

                        float farRadius =
                                farDist *
                                        finalTanHalfAngle;

                        Vec3 nearOrigin =
                                relativeOrigin.add(
                                        finalBeamDir.scale(
                                                nearDist
                                        )
                                );

                        Vec3 farOrigin =
                                relativeOrigin.add(
                                        finalBeamDir.scale(
                                                farDist
                                        )
                                );

                        Vec3 n0 =
                                nearOrigin
                                        .subtract(
                                                finalAxisU.scale(
                                                        nearRadius
                                                )
                                        )
                                        .subtract(
                                                finalAxisV.scale(
                                                        nearRadius
                                                )
                                        );

                        Vec3 n1 =
                                nearOrigin
                                        .add(
                                                finalAxisU.scale(
                                                        nearRadius
                                                )
                                        )
                                        .subtract(
                                                finalAxisV.scale(
                                                        nearRadius
                                                )
                                        );

                        Vec3 n2 =
                                nearOrigin
                                        .add(
                                                finalAxisU.scale(
                                                        nearRadius
                                                )
                                        )
                                        .add(
                                                finalAxisV.scale(
                                                        nearRadius
                                                )
                                        );

                        Vec3 n3 =
                                nearOrigin
                                        .subtract(
                                                finalAxisU.scale(
                                                        nearRadius
                                                )
                                        )
                                        .add(
                                                finalAxisV.scale(
                                                        nearRadius
                                                )
                                        );

                        Vec3 f0 =
                                farOrigin
                                        .subtract(
                                                finalAxisU.scale(
                                                        farRadius
                                                )
                                        )
                                        .subtract(
                                                finalAxisV.scale(
                                                        farRadius
                                                )
                                        );

                        Vec3 f1 =
                                farOrigin
                                        .add(
                                                finalAxisU.scale(
                                                        farRadius
                                                )
                                        )
                                        .subtract(
                                                finalAxisV.scale(
                                                        farRadius
                                                )
                                        );

                        Vec3 f2 =
                                farOrigin
                                        .add(
                                                finalAxisU.scale(
                                                        farRadius
                                                )
                                        )
                                        .add(
                                                finalAxisV.scale(
                                                        farRadius
                                                )
                                        );

                        Vec3 f3 =
                                farOrigin
                                        .subtract(
                                                finalAxisU.scale(
                                                        farRadius
                                                )
                                        )
                                        .add(
                                                finalAxisV.scale(
                                                        farRadius
                                                )
                                        );

                        writeQuad(
                                vc,
                                viewMatrix,
                                f0,
                                f1,
                                f2,
                                f3,
                                r,
                                g,
                                b,
                                alpha
                        );

                        writeQuad(
                                vc,
                                viewMatrix,
                                n3,
                                n2,
                                n1,
                                n0,
                                r,
                                g,
                                b,
                                alpha
                        );

                        writeQuad(
                                vc,
                                viewMatrix,
                                n0,
                                n1,
                                f1,
                                f0,
                                r,
                                g,
                                b,
                                alpha
                        );

                        writeQuad(
                                vc,
                                viewMatrix,
                                n3,
                                f3,
                                f2,
                                n2,
                                r,
                                g,
                                b,
                                alpha
                        );

                        writeQuad(
                                vc,
                                viewMatrix,
                                n0,
                                f0,
                                f3,
                                n3,
                                r,
                                g,
                                b,
                                alpha
                        );

                        writeQuad(
                                vc,
                                viewMatrix,
                                n1,
                                n2,
                                f2,
                                f1,
                                r,
                                g,
                                b,
                                alpha
                        );

                        poseStack.popPose();
                    }

                    @Override
                    public Vec3 getPos(
                            float partialTick) {

                        return finalBlockPos
                                .getCenter();
                    }
                }
        );
    }

    private static void writeQuad(
            VertexConsumer vc,
            Matrix4f matrix,
            Vec3 p1,
            Vec3 p2,
            Vec3 p3,
            Vec3 p4,
            int r,
            int g,
            int b,
            int a) {

        vc.vertex(
                matrix,
                (float)p1.x,
                (float)p1.y,
                (float)p1.z
        ).color(
                r, g, b, a
        ).uv(
                0f, 0f
        ).endVertex();

        vc.vertex(
                matrix,
                (float)p2.x,
                (float)p2.y,
                (float)p2.z
        ).color(
                r, g, b, a
        ).uv(
                1f, 0f
        ).endVertex();

        vc.vertex(
                matrix,
                (float)p3.x,
                (float)p3.y,
                (float)p3.z
        ).color(
                r, g, b, a
        ).uv(
                1f, 1f
        ).endVertex();

        vc.vertex(
                matrix,
                (float)p4.x,
                (float)p4.y,
                (float)p4.z
        ).color(
                r, g, b, a
        ).uv(
                0f, 1f
        ).endVertex();
    }

    private static <T extends BlockEntity & HasGobo>
    void applyFixtureOrientation(
            PoseStack ps,
            T be,
            Direction facing,
            boolean isFlipped,
            BlockState blockState,
            boolean isHanging,
            float panDeg,
            float tiltDeg,
            float[] pans,
            float[] tilts,
            float[] structuralTransform) {

        ps.translate(
                0.5f,
                0f,
                0.5f
        );

        if (isHanging) {
            Direction hangDir =
                    Direction.UP;

            try {
                hangDir =
                        blockState.getValue(
                                HangableBlock.HANG_DIRECTION
                        );
            } catch (Exception ignored) {
            }

            ps.translate(
                    0f,
                    0.5f,
                    0f
            );

            if (hangDir.getAxis() != Direction.Axis.Y) {
                if (hangDir.getAxis() ==
                        Direction.Axis.Z) {

                    ps.mulPose(
                            Axis.ZP.rotationDegrees(90f)
                    );

                    ps.mulPose(
                            hangDir == Direction.SOUTH
                                    ? Axis.XP.rotationDegrees(-90f)
                                    : Axis.XP.rotationDegrees(90f)
                    );

                } else {
                    ps.mulPose(
                            Axis.ZN.rotationDegrees(-90f)
                    );
                }
            }

            ps.translate(
                    0f,
                    -0.5f,
                    0f
            );
        }

        ps.mulPose(
                Axis.YP.rotationDegrees(
                        facing.toYRot()
                )
        );

        if (be instanceof ExtraLightsLightBlockEntity light
                && light.hasMountTransform()) {

            ps.translate(
                    light.getMountOffsetX(),
                    light.getMountOffsetY(),
                    light.getMountOffsetZ()
            );

            if (light.getMountYaw() != 0f) {
                ps.mulPose(
                        Axis.YP.rotationDegrees(
                                light.getMountYaw()
                        )
                );
            }

            if (light.getMountPitch() != 0f) {
                ps.mulPose(
                        Axis.XP.rotationDegrees(
                                light.getMountPitch()
                        )
                );
            }

            if (light.getMountRoll() != 0f) {
                ps.mulPose(
                        Axis.ZP.rotationDegrees(
                                light.getMountRoll()
                        )
                );
            }
        }

        ps.translate(
                -0.5f,
                0f,
                -0.5f
        );

        if (isHanging) {
            if (structuralTransform != null
                    && structuralTransform.length >= 3) {

                ps.translate(
                        structuralTransform[0],
                        structuralTransform[1],
                        structuralTransform[2]
                );

            } else {
                ps.translate(
                        0f,
                        0.19f,
                        0f
                );
            }

            ps.translate(
                    0f,
                    -0.08f,
                    0f
            );
        }

        if (isFlipped) {
            ps.translate(
                    0.5f,
                    0.5f,
                    0.5f
            );

            ps.mulPose(
                    Axis.ZP.rotationDegrees(180f)
            );

            ps.translate(
                    -0.5f,
                    -0.5f,
                    -0.5f
            );
        }

        if (pans != null &&
                pans.length >= 3) {

            ps.translate(
                    pans[0],
                    pans[1],
                    pans[2]
            );

            ps.mulPose(
                    Axis.YP.rotationDegrees(
                            panDeg
                    )
            );

            ps.translate(
                    -pans[0],
                    -pans[1],
                    -pans[2]
            );
        }

        if (tilts != null &&
                tilts.length >= 3) {

            ps.translate(
                    tilts[0],
                    tilts[1],
                    tilts[2]
            );

            ps.mulPose(
                    isFlipped
                            ? Axis.XP.rotationDegrees(-180f)
                            : Axis.XP.rotationDegrees(180f)
            );

            ps.mulPose(
                    Axis.XP.rotationDegrees(
                            tiltDeg
                    )
            );

            ps.translate(
                    -tilts[0],
                    -tilts[1],
                    -tilts[2]
            );
        }
    }
}