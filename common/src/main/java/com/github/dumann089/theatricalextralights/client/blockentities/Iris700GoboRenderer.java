package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.*;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.github.dumann089.theatricalextralights.blockentities.Iris700GoboBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.Iris700GoboBlockEntity;
import com.github.dumann089.theatricalextralights.client.Beam2DRenderTypes;
import com.github.dumann089.theatricalextralights.client.CustomGoboLoader;
import com.github.dumann089.theatricalextralights.client.gobo.FakeVolumetricBeamPattern;
import com.github.dumann089.theatricalextralights.client.gobo.GoboLibrary;
import com.github.dumann089.theatricalextralights.client.render.beam.BeamRenderData;
import com.github.dumann089.theatricalextralights.client.render.beam.VolumetricBeamRenderer;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.util.GlobalGoboManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.imabad.theatrical.TheatricalExpectPlatform;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.client.LazyRenderers;
import dev.imabad.theatrical.config.TheatricalConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public class Iris700GoboRenderer extends ExtraLightsFixtureRenderer<Iris700GoboBlockEntity> {
    private BakedModel cachedPanModel, cachedTiltModel, cachedStaticModel;
    private final Double beamOpacity = TheatricalConfig.INSTANCE.CLIENT.beamOpacity;
    /** One projector per BlockEntity so the geometry cache isn't shared across
     *  multiple Iris700 fixtures (which would thrash the cache and cause
     *  visual blinking when more than one fixture was active). */
    private final WeakHashMap<Iris700GoboBlockEntity, GoboGPUProjector> goboProjectors = new WeakHashMap<>();

    private static class SmoothingState {
        float smoothPan = 0f;
        float smoothTilt = 0f;
        long lastUpdateTime = -1;
    }
    private final Map<Iris700GoboBlockEntity, Iris700GoboRenderer.SmoothingState> smoothingStates = new WeakHashMap<>();
    private static final float SMOOTH_SPEED = 10f;
    
    private final WeakHashMap<Iris700GoboBlockEntity, VolumetricBeamRenderer> volumetricRenderers = new WeakHashMap<>();


    private final Map<Iris700GoboBlockEntity, float[]> structuralCache = new WeakHashMap<>();
    private final Map<Iris700GoboBlockEntity, Long> structuralCacheTicks = new WeakHashMap<>();

    public Iris700GoboRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    private float[] getThrottledStructuralTransforms(Iris700GoboBlockEntity be, BlockState state) {
        long currentTick = be.getLevel().getGameTime();
        Long lastTick = structuralCacheTicks.get(be);

        if (lastTick == null || currentTick != lastTick || !structuralCache.containsKey(be)) {
            float[] transforms;
            Optional<BlockState> optionalSupport = be.getSupportingStructure();
            if (optionalSupport.isPresent() && be.getFixture() != null) {
                transforms = be.getFixture().getTransforms(state, optionalSupport.get());
            } else {
                transforms = new float[]{0f, 0.19f, 0f};
            }
            structuralCache.put(be, transforms);
            structuralCacheTicks.put(be, currentTick);
            return transforms;
        }
        return structuralCache.get(be);
    }

    @Override
    public void renderModel(Iris700GoboBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging, int packedLight, int packedOverlay) {
        if(cachedStaticModel == null){
            cachedStaticModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getStaticModel());
        }
        if (cachedPanModel == null){
            cachedPanModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getPanModel());
        }
        if (cachedTiltModel == null){
            cachedTiltModel = TheatricalExpectPlatform.getBakedModel(blockEntity.getFixture().getTiltModel());
        }

        poseStack.pushPose();
        poseStack.translate(0.5F, 0, .5F);
        if(isHanging){
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0, 0.5, 0F);
            if(hangDirection.getAxis() != Direction.Axis.Y){
                if(hangDirection.getAxis() == Direction.Axis.Z){
                    if(hangDirection == Direction.SOUTH) {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                    } else {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                        poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    }
                } else {
                    if(hangDirection == Direction.EAST) {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(-90));
                    } else {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(90));
                    }
                }
            }
            poseStack.translate(0, -0.5, 0F);
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        poseStack.translate(-0.5F, 0, -.5F);
        if (isHanging) {
            float[] transforms = getThrottledStructuralTransforms(blockEntity, blockState);
            poseStack.translate(transforms[0], transforms[1], transforms[2]);
            poseStack.translate(0, -0.08, 0);
        }
        if (isFlipped) {
            poseStack.translate(0.5F, 0.5, .5F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            poseStack.translate(-0.5F, -0.5, -.5F);
        }

        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedStaticModel, packedLight, packedOverlay);
        Iris700GoboRenderer.SmoothingState state = smoothingStates.computeIfAbsent(blockEntity, k -> new Iris700GoboRenderer.SmoothingState());

        long now = System.nanoTime();
        if (state.lastUpdateTime < 0) state.lastUpdateTime = now;
        float deltaTime = (now - state.lastUpdateTime) / 1_000_000_000f;
        state.lastUpdateTime = now;
        deltaTime = Math.min(deltaTime, 0.1f);

        float targetPan = blockEntity.getPrevPan() + (blockEntity.getPan() - blockEntity.getPrevPan()) * partialTicks;
        float targetTilt = blockEntity.getPrevTilt() + (blockEntity.getTilt() - blockEntity.getPrevTilt()) * partialTicks;

        float alpha = 1f - (float) Math.exp(-SMOOTH_SPEED * deltaTime);
        state.smoothPan = state.smoothPan + (targetPan - state.smoothPan) * alpha;
        state.smoothTilt = state.smoothTilt + (targetTilt - state.smoothTilt) * alpha;

        float[] pans = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(pans[0], pans[1], pans[2]);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.smoothPan));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedPanModel, packedLight, packedOverlay);

        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tilts[0], tilts[1], tilts[2]);
        if (isFlipped) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-180));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(state.smoothTilt));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedTiltModel, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public void beforeRenderBeam(
            Iris700GoboBlockEntity blockEntity,
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            MultiBufferSource multiBufferSource,
            Direction facing,
            float partialTicks,
            boolean isFlipped,
            BlockState blockstate,
            boolean isHanging,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getIntensity() <= 0) return;

        if (blockEntity.getGobo() >= 0) {
            Vec3 localLensOffset = new Vec3(0.5f, 1.031f, 0.0f);
            float[] panPivot = blockEntity.getFixture().getPanRotationPosition();
            float[] tiltPivot = blockEntity.getFixture().getTiltRotationPosition();
            float[] structuralTransform = getThrottledStructuralTransforms(blockEntity, blockstate);

            Iris700GoboRenderer.SmoothingState state = smoothingStates.computeIfAbsent(blockEntity, k -> new Iris700GoboRenderer.SmoothingState());

            goboProjectors.computeIfAbsent(blockEntity, k -> new GoboGPUProjector()).render(
                    blockEntity,
                    multiBufferSource,
                    facing,
                    partialTicks,
                    isFlipped,
                    blockstate,
                    isHanging,
                    localLensOffset,
                    panPivot,
                    tiltPivot,
                    structuralTransform,
                    1.0f,
                    12.0f,
                    state.smoothPan,
                    state.smoothTilt
            );
            
            if (TheatricalExtraLightsConfig.isVolumetricBeamEnabled()) {
                PoseStack localStack = new PoseStack();
                preparePoseStack(blockEntity, localStack, facing, partialTicks, isFlipped, blockstate, isHanging);
                localStack.translate(localLensOffset.x, localLensOffset.y, localLensOffset.z);

                Matrix4f headMatrix = localStack.last().pose();

                Vec3 origin  = new Vec3(headMatrix.m30(), headMatrix.m31(), headMatrix.m32());
                Vec3 axisU   = new Vec3(headMatrix.m00(), headMatrix.m01(), headMatrix.m02()).normalize();
                Vec3 axisV   = new Vec3(headMatrix.m10(), headMatrix.m11(), headMatrix.m12()).normalize();
                Vec3 beamDir = new Vec3(-headMatrix.m20(), -headMatrix.m21(), -headMatrix.m22()).normalize();

                float zoomNorm      = blockEntity.getPartialZoom(partialTicks) / 255.0f;
                float coneHalfAngle = 1.0f + zoomNorm * (19.0f - 1.0f);
                float tanHalfAngle  = (float) Math.tan(Math.toRadians(coneHalfAngle));

                ResourceLocation goboTex = null;
                String customFileName = GlobalGoboManager.getCustomGobo(blockEntity.getGoboLibrary(), blockEntity.getGobo());

                if (customFileName != null) {
                    goboTex = CustomGoboLoader.getOrCreateCustomGobo(customFileName);
                }
                if (goboTex == null) {
                    goboTex = blockEntity.getGoboLibrary().getTexture(blockEntity.getGobo());
                }
                if (goboTex == null) {
                    goboTex = new ResourceLocation("theatricalextralights", "textures/empty_fallback.png");
                }

                // NUEVO: Instancia corregida con baseRadius
                BeamRenderData renderData = new BeamRenderData(
                        blockEntity.getBlockPos(),
                        origin,
                        beamDir,
                        axisU,
                        axisV,
                        zoomNorm,
                        (float) blockEntity.getDistance(),
                        tanHalfAngle,
                        blockEntity.getColour(),
                        blockEntity.getIntensity() / 255.0f,
                        goboTex,
                        (float) blockEntity.getGoboRotation(),
                        blockEntity.getLevel(),
                        1.0f, // widthScale
                        1.0f,
                        0.15f
                );

                volumetricRenderers.computeIfAbsent(blockEntity, k -> new VolumetricBeamRenderer())
                        .render(renderData, poseStack);
            }
        }

        LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {

            @Override
            public void render(MultiBufferSource.BufferSource bufferSource,
                               PoseStack poseStack, Camera camera, float partialTick) {

                poseStack.pushPose();
                Vec3 offset = Vec3.atLowerCornerOf(blockEntity.getBlockPos())
                        .subtract(camera.getPosition());
                poseStack.translate(offset.x, offset.y, offset.z);

                preparePoseStack(blockEntity, poseStack, facing, partialTick,
                        isFlipped, blockstate, isHanging);

                float intensity = blockEntity.getPrevIntensity()
                        + (blockEntity.getIntensity() - blockEntity.getPrevIntensity()) * partialTick;
                int   color     = blockEntity.getColour();
                float alpha     = (intensity / 255f) * beamOpacity.floatValue();

                VertexConsumer builder  = multiBufferSource.getBuffer(Beam2DRenderTypes.getBeam());
                int            goboSlot = blockEntity.getGobo();

                // ── Beam (gobo 0 = open) ──────────────────────────────
                if (goboSlot == 0) {
                    poseStack.pushPose();
                    poseStack.translate(0.5f, 1.031f, 0.0f);
                    if (TheatricalExtraLightsConfig.shouldRender2DBeam()) {
                        renderLightBeam2D(builder, poseStack, blockEntity, camera,
                                alpha, 0.00f, (float) blockEntity.getDistance(), color, 0.0f);
                    } else {
                        renderLightBeam4D(builder, poseStack, blockEntity, partialTick,
                                alpha, 0.00f, (float) blockEntity.getDistance(), color, 0.0f);
                    }
                    poseStack.popPose();
                }

                // ── Fake Volumetric Beams
                renderFakeVolumetricBeams(builder, poseStack, blockEntity, camera,
                        partialTick, alpha, color, goboSlot);

                // ── Lens glow & lens cap ─────────────────────────────────────────
                poseStack.pushPose();
                poseStack.translate(0.5f, 1.031, 0.0f);
                renderLensGlow(builder, poseStack, color, 0.13f);
                poseStack.popPose();

                renderLens(bufferSource, poseStack, alpha, color,
                        0.13f, 0.5f, 1.031f, 0.0f);

                poseStack.popPose();
            }

            @Override
            public Vec3 getPos(float partialTick) {
                return blockEntity.getBlockPos().getCenter();
            }
        });
    }

    private void renderFakeVolumetricBeams(
            VertexConsumer builder,
            PoseStack poseStack,
            Iris700GoboBlockEntity blockEntity,
            Camera camera,
            float partialTick,
            float alpha,
            int color,
            int goboSlot
    ) {
        FakeVolumetricBeamPattern pattern = GoboLibrary.SpotXtreme.getPattern(goboSlot);

        final float LENS_X = 0.5f;
        final float LENS_Y = 1.031f;
        final float LENS_Z = 0.0f;

        float beamLength = (float) blockEntity.getDistance();

        poseStack.pushPose();
        poseStack.translate(LENS_X, LENS_Y, LENS_Z);
        poseStack.mulPose(new org.joml.Quaternionf().rotateZ((float) Math.toRadians(blockEntity.getGoboRotation())));
        float zoomFactor = (blockEntity.getPartialZoom(partialTick) / 255f) * 2f;

        for (FakeVolumetricBeamPattern.BeamTransform t : pattern.getTransforms()) {

            float beamAlpha     = alpha * t.alphaMult();
            float beamThickness = 0.0f * t.thicknessMult();
            float startThick    = 0.0f * t.thicknessMult();

            poseStack.pushPose();
            poseStack.mulPose(new org.joml.Quaternionf().rotateZ((float) Math.toRadians(t.panDeg())));
            poseStack.mulPose(new org.joml.Quaternionf().rotateX((float) Math.toRadians(t.tiltDeg() * zoomFactor)));

            if (TheatricalExtraLightsConfig.shouldRender2DBeam()) {
                renderLightBeam2D(builder, poseStack, blockEntity, camera,
                        beamAlpha, beamThickness, beamLength, color, startThick);
            } else {
                renderLightBeam4D(builder, poseStack, blockEntity, partialTick,
                        beamAlpha, beamThickness, beamLength, color, startThick);
            }

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    @Override
    public void preparePoseStack(Iris700GoboBlockEntity blockEntity, PoseStack poseStack, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging) {
        FixtureMountTransform.apply(poseStack, blockEntity);
        poseStack.translate(0.5F, 0, .5F);
        if(isHanging){
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0, 0.5, 0F);
            if(hangDirection.getAxis() != Direction.Axis.Y){
                if(hangDirection.getAxis() == Direction.Axis.Z){
                    if(hangDirection == Direction.SOUTH) {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                    } else {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                        poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    }
                } else {
                    if(hangDirection == Direction.EAST) {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(-90));
                    } else {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(90));
                    }
                }
            }
            poseStack.translate(0, -0.5, 0F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        poseStack.translate(-0.5F, 0, -.5F);
        if (isHanging) {
            float[] transforms = getThrottledStructuralTransforms(blockEntity, blockState);
            poseStack.translate(transforms[0], transforms[1], transforms[2]);
            poseStack.translate(0, -0.08, 0);
        }
        if (isFlipped) {
            poseStack.translate(0.5F, 0.5, .5F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            poseStack.translate(-0.5F, -0.5, -.5F);
        }
        Iris700GoboRenderer.SmoothingState state = smoothingStates.computeIfAbsent(blockEntity, k -> new Iris700GoboRenderer.SmoothingState());

        float[] pans = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(pans[0], pans[1], pans[2]);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.smoothPan));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);

        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tilts[0], tilts[1], tilts[2]);
        if (isFlipped) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-180));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
        }
        int prevTilt = blockEntity.getPrevTilt();
        int tilt = blockEntity.getTilt();
        poseStack.mulPose(Axis.XP.rotationDegrees(state.smoothTilt));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
    }
}