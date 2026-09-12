package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.MiniSpotGobosBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.MovingVL2CBeamsBlockEntity;
import com.github.dumann089.theatricalextralights.client.gobo.GoboWheelAnimator;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.github.dumann089.theatricalextralights.client.Beam2DRenderTypes;
import com.github.dumann089.theatricalextralights.client.CustomGoboLoader;
import com.github.dumann089.theatricalextralights.client.blockentities.GoboGPUProjector;
import com.github.dumann089.theatricalextralights.client.gobo.FakeVolumetricBeamPattern;
import com.github.dumann089.theatricalextralights.client.gobo.GoboLibrary;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.client.render.beam.BeamRenderData;
import com.github.dumann089.theatricalextralights.client.render.beam.VolumetricBeamRenderer;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import com.github.dumann089.theatricalextralights.client.render.beam.FramingShutterRender;

import java.util.Optional;
import java.util.Map;
import java.util.WeakHashMap;

public class MiniSpotGobosRenderer extends ExtraLightsFixtureRenderer<MiniSpotGobosBlockEntity> {
    private BakedModel cachedPanModel, cachedTiltModel, cachedStaticModel;
    private final Double beamOpacity = TheatricalConfig.INSTANCE.CLIENT.beamOpacity;

    private final WeakHashMap<MiniSpotGobosBlockEntity, GoboGPUProjector> goboProjectors = new WeakHashMap<>();

    private static class SmoothingState {
        float smoothPan = 0f;
        float smoothTilt = 0f;
        long lastUpdateTime = -1;
    }
    private final Map<MiniSpotGobosBlockEntity, SmoothingState> smoothingStates = new WeakHashMap<>();
    private static final float SMOOTH_SPEED = 10f;

    private final WeakHashMap<MiniSpotGobosBlockEntity, VolumetricBeamRenderer> volumetricRenderers = new WeakHashMap<>();

    private final Map<MiniSpotGobosBlockEntity, float[]> structuralCache = new WeakHashMap<>();
    private final Map<MiniSpotGobosBlockEntity, Long> structuralCacheTicks = new WeakHashMap<>();

    private final java.util.Map<MiniSpotGobosBlockEntity, GoboWheelAnimator> goboAnimators = new java.util.WeakHashMap<>();


    public MiniSpotGobosRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    private float[] getThrottledStructuralTransforms(MiniSpotGobosBlockEntity be, BlockState state) {
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
    public void renderModel(MiniSpotGobosBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging, int packedLight, int packedOverlay) {
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
        SmoothingState state = smoothingStates.computeIfAbsent(blockEntity, k -> new SmoothingState());

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
            MiniSpotGobosBlockEntity blockEntity,
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
            GoboWheelAnimator animator = goboAnimators.computeIfAbsent(blockEntity, k -> new GoboWheelAnimator());

            Vec3 localLensOffset = new Vec3(0.5f, 0.406F, 0.312F);
            float[] panPivot = blockEntity.getFixture().getPanRotationPosition();
            float[] tiltPivot = blockEntity.getFixture().getTiltRotationPosition();
            float[] structuralTransform = getThrottledStructuralTransforms(blockEntity, blockstate);

            MiniSpotGobosRenderer.SmoothingState state = smoothingStates.computeIfAbsent(blockEntity, k -> new MiniSpotGobosRenderer.SmoothingState());

            animator.updateTarget(blockEntity.getGoboLibrary(), blockEntity.getGobo());

            // 2. Extraemos los datos para el Shader Dual
            int slot0 = animator.getOutgoingSlot(blockEntity.getGoboLibrary());
            int slot1 = animator.getIncomingSlot(blockEntity.getGoboLibrary());
            float wheelProgress = animator.getShaderProgress(blockEntity.getGoboLibrary());

            // 3. Resolvemos las dos texturas
            ResourceLocation tex0 = resolveGoboTexture(blockEntity, slot0);
            ResourceLocation tex1 = resolveGoboTexture(blockEntity, slot1);

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
                    1.0f,   // minAngle
                    19.0f,  // maxAngle
                    state.smoothPan,
                    state.smoothTilt,
                    0.0f    // baseRadius
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

                // --- NUEVO: Obtener datos de transición de la rueda ---
                animator.updateTarget(blockEntity.getGoboLibrary(), blockEntity.getGobo());
                float virtualSlot = animator.snapshotVirtualSlot();

                int outgoingSlot = animator.getOutgoingSlot(blockEntity.getGoboLibrary(), virtualSlot);
                int incomingSlot = animator.getIncomingSlot(blockEntity.getGoboLibrary(), virtualSlot);
                float wheelTransition = animator.getShaderProgress(blockEntity.getGoboLibrary(), virtualSlot);

                // 🛡️ Obtención segura de textura principal (Gobo A - outgoing)
                ResourceLocation goboTex = null;
                String customFileNameOut = GlobalGoboManager.getCustomGobo(blockEntity.getGoboLibrary(), outgoingSlot);

                if (customFileNameOut != null) {
                    goboTex = CustomGoboLoader.getOrCreateCustomGobo(customFileNameOut);
                }
                if (goboTex == null) {
                    goboTex = blockEntity.getGoboLibrary().getTexture(outgoingSlot);
                }
                if (goboTex == null) {
                    goboTex = new ResourceLocation("theatricalextralights", "textures/empty_fallback.png");
                }

                // 🛡️ Obtención segura de textura secundaria (Gobo B - incoming)
                ResourceLocation nextGoboTex = null;
                String customFileNameIn = GlobalGoboManager.getCustomGobo(blockEntity.getGoboLibrary(), incomingSlot);

                if (customFileNameIn != null) {
                    nextGoboTex = CustomGoboLoader.getOrCreateCustomGobo(customFileNameIn);
                }
                if (nextGoboTex == null) {
                    nextGoboTex = blockEntity.getGoboLibrary().getTexture(incomingSlot);
                }
                if (nextGoboTex == null) {
                    nextGoboTex = goboTex; // Fallback al gobo principal
                }

                // NUEVO: Instancia corregida con nextGoboTex y wheelTransition
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
                        nextGoboTex,     // Añadido
                        (float) blockEntity.getGoboRotation(),
                        wheelTransition, // Añadido
                        blockEntity.getLevel(),
                        1.0f,
                        1.0f,
                        0.12f
                );

                // Cone pilote par le zoom (1 a 19 deg) : la tache doit suivre.
                renderData = FramingShutterRender.attach(renderData, blockEntity, partialTicks);
                publishCone(blockEntity, renderData);

                volumetricRenderers.computeIfAbsent(blockEntity, k -> new VolumetricBeamRenderer())
                        .render(renderData, poseStack);
            }
        }

        // ==========================================================================================

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

                if (goboSlot == 0 && !FramingShutterRender.isActive(blockEntity)) {
                    poseStack.pushPose();
                    poseStack.translate(0.5f, 0.406F, 0.312F);
                    if (TheatricalExtraLightsConfig.shouldRender2DBeam()) {
                        renderLightBeam2D(builder, poseStack, blockEntity, camera,
                                alpha, 0.00f, (float) blockEntity.getDistance(), color, 0.007f);
                    } else {
                        renderLightBeam4D(builder, poseStack, blockEntity, partialTick,
                                alpha, 0.00f, (float) blockEntity.getDistance(), color, 0.007f);
                    }
                    poseStack.popPose();
                }

                renderFakeVolumetricBeams(builder, poseStack, blockEntity, camera,
                        partialTick, alpha, color, goboSlot);

                poseStack.pushPose();
                poseStack.translate(0.5f, 0.406F, 0.312F);
                renderLensGlow(builder, poseStack, color, 0.065f);
                poseStack.popPose();

                renderLens(bufferSource, poseStack, alpha, color,
                        0.065f, 0.5f, 0.406F, 0.312F);

                poseStack.popPose();
            }

            @Override
            public Vec3 getPos(float partialTick) {
                return blockEntity.getBlockPos().getCenter();
            }
        });
    }

    private ResourceLocation resolveGoboTexture(MiniSpotGobosBlockEntity blockEntity, int slot) {
        if (slot < 0) {
            return new ResourceLocation("theatricalextralights", "textures/empty_fallback.png");
        }

        String customFileName = GlobalGoboManager.getCustomGobo(blockEntity.getGoboLibrary(), slot);
        if (customFileName != null) {
            return CustomGoboLoader.getOrCreateCustomGobo(customFileName);
        }

        ResourceLocation tex = blockEntity.getGoboLibrary().getTexture(slot);
        if (tex != null) {
            return tex;
        }

        return new ResourceLocation("theatricalextralights", "textures/empty_fallback.png");
    }

    private void renderFakeVolumetricBeams(
            VertexConsumer builder, PoseStack poseStack,
            MiniSpotGobosBlockEntity blockEntity, Camera camera,
            float partialTick, float alpha, int color, int goboSlot
    ) {
        FakeVolumetricBeamPattern pattern = GoboLibrary.VL2C.getPattern(goboSlot);

        final float LENS_X = 0.5f;
        final float LENS_Y = 0.781f;
        final float LENS_Z = 0.2f;

        float beamLength = (float) blockEntity.getDistance();

        poseStack.pushPose();
        poseStack.translate(LENS_X, LENS_Y, LENS_Z);
        poseStack.mulPose(new org.joml.Quaternionf().rotateZ((float) Math.toRadians(blockEntity.getGoboRotation())));
        float zoomFactor = (blockEntity.getPartialZoom(partialTick) / 255f) * 2f;

        for (FakeVolumetricBeamPattern.BeamTransform t : pattern.getTransforms()) {

            float beamAlpha     = alpha * t.alphaMult();
            float beamThickness = 0.00f * t.thicknessMult();
            float startThick    = 0.000f * t.thicknessMult();

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
    public void preparePoseStack(MiniSpotGobosBlockEntity blockEntity, PoseStack poseStack, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging) {
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
        SmoothingState state = smoothingStates.computeIfAbsent(blockEntity, k -> new SmoothingState());

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