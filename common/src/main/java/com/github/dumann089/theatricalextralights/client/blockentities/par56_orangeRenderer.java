package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.par56_orangeBlockEntity;
import com.github.dumann089.theatricalextralights.client.CustomGoboLoader;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.github.dumann089.theatricalextralights.client.Beam2DRenderTypes;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Optional;
import java.util.WeakHashMap;

public class par56_orangeRenderer extends ExtraLightsFixtureRenderer<par56_orangeBlockEntity> {
    private BakedModel cachedPanModel, cachedTiltModel, cachedStaticModel;

    private final WeakHashMap<par56_orangeBlockEntity, GoboGPUProjector> goboProjectors = new WeakHashMap<>();
    private final WeakHashMap<par56_orangeBlockEntity, VolumetricBeamRenderer> volumetricRenderers = new WeakHashMap<>();
    private final Double beamOpacity = TheatricalConfig.INSTANCE.CLIENT.beamOpacity;

    public par56_orangeRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void renderModel(par56_orangeBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging, int packedLight, int packedOverlay) {
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

        //#region Fixture Hanging
        poseStack.translate(0.5F, 0, .5F);
        if(isHanging){
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0, 0.5, 0F);
            if(hangDirection.getAxis() != Direction.Axis.Y){
                if(hangDirection.getAxis() == Direction.Axis.Z){
                    if(hangDirection == Direction.SOUTH) {
                        poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    } else {
                        poseStack.mulPose(Axis.XN.rotationDegrees(90));
                    }
                } else {
                    if(hangDirection == Direction.EAST) {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(90));
                    } else {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(-90));
                    }
                }
            }
            poseStack.translate(0, -0.5, 0F);
        }
        //#endregion
        if(facing.getAxis() == Direction.Axis.X){
            poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(facing.getOpposite().toYRot()));
        }

        FixtureMountTransform.apply(poseStack, blockEntity);

        poseStack.translate(-0.5F, 0, -.5F);
        if (isHanging) {
            Optional<BlockState> optionalSupport = blockEntity.getSupportingStructure();
            if (optionalSupport.isPresent()) {
                float[] transforms = blockEntity.getFixture().getTransforms(blockState, optionalSupport.get());
                poseStack.translate(transforms[0], transforms[1], transforms[2]);
            } else {
                poseStack.translate(0, 0.19, 0);
            }
        }
        // Static Model Render
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedStaticModel, packedLight, packedOverlay);

        //#region Model Pan
        float[] pans = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(pans[0], pans[1], pans[2]);
        int prevPan = blockEntity.getPrevPan();
        int pan = blockEntity.getPan();
        float currentPan = prevPan + (pan - prevPan) * partialTicks;
        poseStack.mulPose(Axis.YN.rotationDegrees(currentPan));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedPanModel, packedLight, packedOverlay);
        //#endregion

        //#region Model Tilt
        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tilts[0], tilts[1], tilts[2]);
        int prevTilt = blockEntity.getPrevTilt();
        int tilt = blockEntity.getTilt();
        float currentTilt = prevTilt + (tilt - prevTilt) * partialTicks;
        poseStack.mulPose(Axis.XP.rotationDegrees(currentTilt));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
        minecraftRenderModel(poseStack, vertexConsumer, blockState, cachedTiltModel,  packedLight, packedOverlay);
        //#endregion

        poseStack.popPose();
    }

    @Override
    public void beforeRenderBeam(par56_orangeBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer, MultiBufferSource multiBufferSource, Direction facing, float partialTicks, boolean isFlipped, BlockState blockstate, boolean isHanging, int packedLight, int packedOverlay) {
        if(blockEntity.getIntensity() <= 0) return;

        Vec3 localLensOffset = new Vec3(0.5f, 1.57f, 0.15f);
        float[] panPivot = blockEntity.getFixture().getPanRotationPosition();
        float[] tiltPivot = blockEntity.getFixture().getTiltRotationPosition();

        float[] structuralTransform;
        Optional<BlockState> optionalSupport = blockEntity.getSupportingStructure();
        if (optionalSupport.isPresent() && blockEntity.getFixture() != null) {
            structuralTransform = blockEntity.getFixture().getTransforms(blockstate, optionalSupport.get());
        } else {
            structuralTransform = new float[]{0f, 0.19f, 0f};
        }

        float currentPan = blockEntity.getPrevPan() + (blockEntity.getPan() - blockEntity.getPrevPan()) * partialTicks;
        float currentTilt = blockEntity.getPrevTilt() + (blockEntity.getTilt() - blockEntity.getPrevTilt()) * partialTicks;

        // 1. Calculamos la dirección con la misma lógica del modelo 3D
        Direction projectorFacing = (facing.getAxis() == Direction.Axis.X) ? facing : facing.getOpposite();

        // 2. Le pasamos 'projectorFacing' en lugar del 'facing' original
        goboProjectors.computeIfAbsent(blockEntity,k->new GoboGPUProjector()).render(
                blockEntity,multiBufferSource,projectorFacing,partialTicks,isFlipped,blockstate,isHanging,
                localLensOffset,panPivot,tiltPivot,structuralTransform,35.0f,65.0f,
                currentPan,currentTilt+180f,3.55f
        );

        // Renderizado Volumétrico
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
            float coneHalfAngle = 1.0f + zoomNorm * (55.0f - 1.0f);
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

            BeamRenderData renderData = new BeamRenderData(
                    blockEntity.getBlockPos(), origin, beamDir, axisU, axisV, zoomNorm,
                    (float) blockEntity.getDistance(), tanHalfAngle, blockEntity.getColour(),
                    blockEntity.getIntensity() / 255.0f, goboTex, (float) blockEntity.getGoboRotation(),
                    blockEntity.getLevel(), 0.0f, 0.0f, 0.02f
            );

            volumetricRenderers.computeIfAbsent(blockEntity, k -> new VolumetricBeamRenderer())
                    .render(renderData, poseStack);
        }

        // Renderizado Fallback 2D / Fake Volumetric
        LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {
            @Override
            public void render(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Camera camera, float partialTick) {
                poseStack.pushPose();
                Vec3 offset = Vec3.atLowerCornerOf(blockEntity.getBlockPos()).subtract(camera.getPosition());
                poseStack.translate(offset.x, offset.y, offset.z);

                preparePoseStack(blockEntity, poseStack, facing, partialTick, isFlipped, blockstate, isHanging);

                float intensity = blockEntity.getPrevIntensity() + ((blockEntity.getIntensity()) - blockEntity.getPrevIntensity()) * partialTicks;
                int color = blockEntity.getColour();
                float alpha = (intensity / 255f) * beamOpacity.floatValue();

                VertexConsumer builder = multiBufferSource.getBuffer(Beam2DRenderTypes.getBeam());
                int goboSlot = blockEntity.getGobo();

                if (goboSlot == 0) {
                    poseStack.pushPose();
                    poseStack.translate(localLensOffset.x, localLensOffset.y, localLensOffset.z);
                    if (TheatricalExtraLightsConfig.shouldRender2DBeam()) {
                        renderLightBeam2D(builder, poseStack, blockEntity, camera,
                                alpha, 0.00f, (float) blockEntity.getDistance(), color, 0.007f);
                    } else {
                        renderLightBeam4D(builder, poseStack, blockEntity, partialTick,
                                alpha, 0.00f, (float) blockEntity.getDistance(), color, 0.007f);
                    }
                    poseStack.popPose();
                }

                renderFakeVolumetricBeams(builder, poseStack, blockEntity, camera, partialTick, alpha, color, goboSlot);

                poseStack.pushPose();
                poseStack.translate(localLensOffset.x, localLensOffset.y, localLensOffset.z);
                renderLensGlow(builder, poseStack, color, 0.25f);
                poseStack.popPose();

                renderLens(bufferSource, poseStack, alpha, color, 0.25f, (float)localLensOffset.x, (float)localLensOffset.y, (float)localLensOffset.z);
                poseStack.popPose();
            }

            @Override
            public Vec3 getPos(float partialTick) {
                return blockEntity.getBlockPos().getCenter();
            }
        });
    }

    private void renderFakeVolumetricBeams(VertexConsumer builder, PoseStack poseStack, par56_orangeBlockEntity blockEntity, Camera camera, float partialTick, float alpha, int color, int goboSlot) {
        FakeVolumetricBeamPattern pattern = GoboLibrary.VL2C.getPattern(goboSlot);
        float beamLength = (float) blockEntity.getDistance();

        poseStack.pushPose();
        poseStack.translate(0.5f, 1.75f, 0.28f);
        poseStack.mulPose(new org.joml.Quaternionf().rotateZ((float) Math.toRadians(blockEntity.getGoboRotation())));
        float zoomFactor = (blockEntity.getPartialZoom(partialTick) / 255f) * 2f;

        for (FakeVolumetricBeamPattern.BeamTransform t : pattern.getTransforms()) {
            float beamAlpha = alpha * t.alphaMult();
            float beamThickness = 0.00f * t.thicknessMult();
            float startThick = 0.000f * t.thicknessMult();

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
    public void preparePoseStack(par56_orangeBlockEntity blockEntity, PoseStack poseStack, Direction facing, float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging) {
        //#region Fixture Hanging
        poseStack.translate(0.5F, 0, .5F);
        if(isHanging){
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0, 0.5, 0F);
            if(hangDirection.getAxis() != Direction.Axis.Y){
                if(hangDirection.getAxis() == Direction.Axis.Z){
                    if(hangDirection == Direction.SOUTH) {
                        poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    } else {
                        poseStack.mulPose(Axis.XN.rotationDegrees(90));
                    }
                } else {
                    if(hangDirection == Direction.EAST) {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(90));
                    } else {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(-90));
                    }
                }
            } else {
                //TODO: Handle hanging up
            }
            poseStack.translate(0, -0.5, 0F);
        }
        //#endregion
        if(facing.getAxis() == Direction.Axis.X){
            poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(facing.getOpposite().toYRot()));
        }

        FixtureMountTransform.apply(poseStack, blockEntity);

        poseStack.translate(-0.5F, 0, -.5F);
        if (isHanging) {
            Optional<BlockState> optionalSupport = blockEntity.getSupportingStructure();
            if (optionalSupport.isPresent()) {
                float[] transforms = blockEntity.getFixture().getTransforms(blockState, optionalSupport.get());
                poseStack.translate(transforms[0], transforms[1], transforms[2]);
            } else {
                poseStack.translate(0, 0.19, 0);
            }
        }
        //#region Model Pan
        float[] pans = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(pans[0], pans[1], pans[2]);
        int prevPan = blockEntity.getPrevPan();
        int pan = blockEntity.getPan();
        poseStack.mulPose(Axis.YN.rotationDegrees((prevPan + (pan - prevPan) * partialTicks)));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);
        //#endregion
        //#region Model Tilt
        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tilts[0], tilts[1], tilts[2]);
        int prevTilt = blockEntity.getPrevTilt();
        int tilt = blockEntity.getTilt();
        poseStack.mulPose(Axis.XP.rotationDegrees((prevTilt + (tilt - prevTilt) * partialTicks)));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
        //#endregion
    }
}