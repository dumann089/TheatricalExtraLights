package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.client.Beam2DRenderTypes;
import com.github.dumann089.theatricalextralights.client.CustomGoboLoader;
import com.github.dumann089.theatricalextralights.client.LensRenderTypes;
import com.github.dumann089.theatricalextralights.client.gobo.GoboLibrary;
import com.github.dumann089.theatricalextralights.client.render.beam.BeamRenderData;
import com.github.dumann089.theatricalextralights.client.render.beam.VolumetricBeamRenderer;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.util.GlobalGoboManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.blocks.light.MovingLightBlock;
import dev.imabad.theatrical.client.blockentities.FixtureRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import net.minecraft.core.BlockPos;
import org.joml.Vector4f;
import org.joml.Vector3f;

import java.util.WeakHashMap;

public abstract class ExtraLightsFixtureRenderer<T extends BaseLightBlockEntity> extends FixtureRenderer<T> {
    /**
     * Per-block-entity volumetric renderers. Stored here in the base class so every
     * subclass gets cache isolation between multiple placed fixtures for free.
     */
    private final WeakHashMap<T, java.util.Map<Integer, VolumetricBeamRenderer>> volumetricRenderers = new WeakHashMap<>();

    private final Double beamOpacity = dev.imabad.theatrical.config.TheatricalConfig.INSTANCE.CLIENT.beamOpacity;

    public ExtraLightsFixtureRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    /** Beam is managed in {@code beforeRenderBeam} — avoids the double-render from Theatrical. */
    @Override
    public boolean shouldRenderBeam(T blockEntity) {
        return false;
    }

    /** Pas de faisceau volumétrique parent (disques devant le bloc). */
    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource multiBufferSource,
                       int packedLight, int packedOverlay) {
        poseStack.pushPose();
        VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.cutout());
        BlockState blockState = blockEntity.getBlockState();
        boolean isFlipped = blockEntity.isUpsideDown();
        boolean isHanging = ((HangableBlock) blockState.getBlock()).isHanging(blockEntity.getLevel(), blockEntity.getBlockPos());
        Direction facing = blockState.getValue(MovingLightBlock.FACING);
        renderModel(blockEntity, poseStack, vertexConsumer, facing, partialTick, isFlipped, blockState, isHanging, packedLight, packedOverlay);
        beforeRenderBeam(blockEntity, poseStack, vertexConsumer, multiBufferSource, facing, partialTick, isFlipped, blockState, isHanging, packedLight, packedOverlay);
        poseStack.popPose();
    }

    // ── Central volumetric beam helper ──────────────────────────────────────

    /**
     * Builds a {@link BeamRenderData} from the fixture's current pan/tilt/zoom state and
     * submits it to this block-entity's {@link VolumetricBeamRenderer}.
     *
     * <p>Call this inside your {@code beforeRenderBeam} after the early intensity check.
     * The PoseStack passed in is the one already translated to world-space for this block
     * (i.e. the one you received in {@code beforeRenderBeam}), <strong>before</strong>
     * calling {@code preparePoseStack}.
     *
     * @param blockEntity   the fixture being rendered
     * @param facing        block facing direction
     * @param partialTicks  interpolation factor
     * @param isFlipped     whether the fixture is mounted flipped
     * @param blockState    current block state
     * @param isHanging     whether the fixture is hanging
     * @param lensOffset    local-space offset from the block origin to the lens exit point
     * @param minAngleDeg   minimum cone half-angle in degrees (zoom at 0)
     * @param maxAngleDeg   maximum cone half-angle in degrees (zoom at 255)
     * @param goboLibrary   GoboLibrary entry for this fixture (used for the gobo texture)
     * @param goboSlot      current gobo slot index from the block entity
     * @param widthScale    U-axis scale — 1.0f for circular spots, >1.0f for bar fixtures
     * @param heightScale   V-axis scale — 1.0f for circular spots, <1.0f for bar fixtures
     */

    protected void submitVolumetricBeam(
            T blockEntity,
            PoseStack beamPose,      // ← es PoseStack, NO Direction
            float partialTicks,
            float minAngleDeg,
            float maxAngleDeg,
            ResourceLocation tex0,         // ← Reemplazamos GoboLibrary
            ResourceLocation tex1,         // ← Reemplazamos goboSlot
            float wheelProgress,           // ← Nuevo parámetro de progreso
            float focusNorm,
            float widthScale,
            float heightScale,
            int beamIndex,
            int customColor,
            float customIntensity,
            float baseRadius
    ) {
        if (!TheatricalExtraLightsConfig.isVolumetricBeamEnabled() || customIntensity <= 0.0f) return;

        org.joml.Matrix4f headMatrix = beamPose.last().pose();
        Vec3 origin = new Vec3(headMatrix.m30(), headMatrix.m31(), headMatrix.m32());
        Vec3 axisU = new Vec3(headMatrix.m00(), headMatrix.m01(), headMatrix.m02()).normalize();
        Vec3 axisV = new Vec3(headMatrix.m10(), headMatrix.m11(), headMatrix.m12()).normalize();
        Vec3 beamDir = new Vec3(-headMatrix.m20(), -headMatrix.m21(), -headMatrix.m22()).normalize();

        float tanHalfAngle = (float) Math.tan(Math.toRadians(minAngleDeg + focusNorm * (maxAngleDeg - minAngleDeg)));

        // Instanciamos el renderData usando los nuevos parámetros de texturas duales y progreso
        BeamRenderData renderData = new BeamRenderData(
                blockEntity.getBlockPos(), origin, beamDir, axisU, axisV, focusNorm,
                (float) blockEntity.getDistance(), tanHalfAngle, customColor,
                customIntensity, tex0, tex1, wheelProgress, 0.0f, blockEntity.getLevel(), widthScale, heightScale, baseRadius
        );

        volumetricRenderers.computeIfAbsent(blockEntity, k -> new java.util.HashMap<>())
                .computeIfAbsent(beamIndex, k -> new VolumetricBeamRenderer())
                .render(renderData, new PoseStack());
    }


    // ── Vertex helpers ──────────────────────────────────────────────────────

    // BEAM_VANILLA
    private static void addVertexPC(VertexConsumer vc, Matrix4f m,
                                    int r, int g, int b, int a,
                                    float x, float y, float z) {
        vc.vertex(m, x, y, z)
                .color(r, g, b, a)
                .endVertex();
    }

    // BEAM_SHADERS
    private static void addVertexPCTL(VertexConsumer vc, Matrix4f m,
                                      int r, int g, int b, int a,
                                      float x, float y, float z) {
        vc.vertex(m, x, y, z)
                .color(r, g, b, a)
                .endVertex();
    }

    private static void addBeamVertex(VertexConsumer vc, Matrix4f m,
                                      int r, int g, int b, int a,
                                      float x, float y, float z) {
        if (Beam2DRenderTypes.isShadersActive()) {
            addVertexPCTL(vc, m, r, g, b, a, x, y, z);
        } else {
            addVertexPC(vc, m, r, g, b, a, x, y, z);
        }
    }

    private static void addLensVertex(VertexConsumer vc, Matrix4f m,
                                      int r, int g, int b, int a,
                                      float x, float y, float z,
                                      float u, float v) {
        vc.vertex(m, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .uv2(LightTexture.FULL_BRIGHT)
                .endVertex();
    }

    // ── Beam 2D ─────────────────────────────────────────────────────────────

    protected void renderLightBeam2D(VertexConsumer builder, PoseStack stack, T tileEntityFixture, Camera camera, float alpha, float beamSize, float length, int color, float focusMultiplier) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        float intensity = 0.65f;
        int a = (int) (alpha * 255 * intensity);

        length += 2.5f;
        float endSize = beamSize + (tileEntityFixture.getFocus() * focusMultiplier);

        stack.pushPose();

        Matrix4f inverseMatrix = new Matrix4f(stack.last().pose()).invert();
        org.joml.Vector4f toCameraLocal = inverseMatrix.transform(new org.joml.Vector4f(0, 0, 0, 1));

        float angle = 0;
        if (Math.abs(toCameraLocal.x) > 0.001f || Math.abs(toCameraLocal.y) > 0.001f) {
            angle = (float) Math.atan2(toCameraLocal.y, toCameraLocal.x);
        }

        stack.mulPose(new org.joml.Quaternionf().rotateZ(angle - (float)(Math.PI / 2)));

        Matrix4f m = stack.last().pose();

        addBeamVertex(builder, m, r, g, b, a, -beamSize, 0,  0);
        addBeamVertex(builder, m, r, g, b, a,  beamSize, 0,  0);
        addBeamVertex(builder, m, r, g, b, 0,  endSize,  0, -length);
        addBeamVertex(builder, m, r, g, b, 0, -endSize,  0, -length);

        addBeamVertex(builder, m, r, g, b, 0, -endSize,  0, -length);
        addBeamVertex(builder, m, r, g, b, 0,  endSize,  0, -length);
        addBeamVertex(builder, m, r, g, b, a,  beamSize, 0,  0);
        addBeamVertex(builder, m, r, g, b, a, -beamSize, 0,  0);

        stack.popPose();
    }

    protected void renderLightBeam2DForwardOnly(VertexConsumer builder, PoseStack stack, T tileEntityFixture,
                                                Camera camera, float alpha, float beamSize, float length,
                                                int color, float focusMultiplier) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        float intensity = 0.65f;
        int a = (int) (alpha * 255 * intensity);

        length += 2.5f;
        float endSize = beamSize + (tileEntityFixture.getFocus() * focusMultiplier);
        float nearClip = 0.35f;
        float nearSize = beamSize * 0.55f;

        stack.pushPose();

        Matrix4f inverseMatrix = new Matrix4f(stack.last().pose()).invert();
        org.joml.Vector4f toCameraLocal = inverseMatrix.transform(new org.joml.Vector4f(0, 0, 0, 1));

        float angle = 0;
        if (Math.abs(toCameraLocal.x) > 0.001f || Math.abs(toCameraLocal.y) > 0.001f) {
            angle = (float) Math.atan2(toCameraLocal.y, toCameraLocal.x);
        }

        stack.mulPose(new org.joml.Quaternionf().rotateZ(angle - (float) (Math.PI / 2)));

        Matrix4f m = stack.last().pose();

        addBeamVertex(builder, m, r, g, b, a, -nearSize, 0, -nearClip);
        addBeamVertex(builder, m, r, g, b, a,  nearSize, 0, -nearClip);
        addBeamVertex(builder, m, r, g, b, 0,  endSize,  0, -length);
        addBeamVertex(builder, m, r, g, b, 0, -endSize,  0, -length);

        addBeamVertex(builder, m, r, g, b, 0, -endSize,  0, -length);
        addBeamVertex(builder, m, r, g, b, 0,  endSize,  0, -length);
        addBeamVertex(builder, m, r, g, b, a,  nearSize, 0, -nearClip);
        addBeamVertex(builder, m, r, g, b, a, -nearSize, 0, -nearClip);

        stack.popPose();
    }

    protected void renderLightBeam2DFixedForward(VertexConsumer builder, PoseStack stack, T tileEntityFixture,
                                                 float alpha, float beamSize, float length,
                                                 int color, float focusMultiplier) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        float intensity = 0.65f;
        int a = (int) (alpha * 255 * intensity);

        length += 2.5f;
        float endSize = beamSize + (tileEntityFixture.getFocus() * focusMultiplier);
        float nearClip = 0.35f;
        float nearSize = beamSize * 0.55f;

        Matrix4f m = stack.last().pose();

        addBeamVertex(builder, m, r, g, b, a, -nearSize, 0, -nearClip);
        addBeamVertex(builder, m, r, g, b, a,  nearSize, 0, -nearClip);
        addBeamVertex(builder, m, r, g, b, 0,  endSize,  0, -length);
        addBeamVertex(builder, m, r, g, b, 0, -endSize,  0, -length);

        addBeamVertex(builder, m, r, g, b, 0, -endSize,  0, -length);
        addBeamVertex(builder, m, r, g, b, 0,  endSize,  0, -length);
        addBeamVertex(builder, m, r, g, b, a,  nearSize, 0, -nearClip);
        addBeamVertex(builder, m, r, g, b, a, -nearSize, 0, -nearClip);
    }

    protected void renderLightBeam4DForwardOnly(VertexConsumer builder, PoseStack stack, T tileEntityFixture,
                                                float partialTicks, float alpha, float beamSize,
                                                float length, int color, float focusMultiplier) {
        float endMultiplier = 1 + tileEntityFixture.getFocus() * length * focusMultiplier;

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        float intensity = 0.65f;
        int a = (int) (alpha * 255 * intensity);

        Matrix4f m = stack.last().pose();

        length += 4.0f;
        float end = endMultiplier;
        float nearClip = 0.35f;
        float near = beamSize * 0.55f;
        float nearEnd = near * end;

        addBeamVertex(builder, m, r, g, b, 0,  nearEnd,  nearEnd, -length);
        addBeamVertex(builder, m, r, g, b, a,  near,     near,    -nearClip);
        addBeamVertex(builder, m, r, g, b, a,  near,    -near,    -nearClip);
        addBeamVertex(builder, m, r, g, b, 0,  nearEnd, -nearEnd, -length);

        addBeamVertex(builder, m, r, g, b, 0, -nearEnd, -nearEnd, -length);
        addBeamVertex(builder, m, r, g, b, a, -near,    -near,    -nearClip);
        addBeamVertex(builder, m, r, g, b, a, -near,     near,    -nearClip);
        addBeamVertex(builder, m, r, g, b, 0, -nearEnd,  nearEnd, -length);

        addBeamVertex(builder, m, r, g, b, 0, -nearEnd,  nearEnd, -length);
        addBeamVertex(builder, m, r, g, b, a, -near,     near,    -nearClip);
        addBeamVertex(builder, m, r, g, b, a,  near,     near,    -nearClip);
        addBeamVertex(builder, m, r, g, b, 0,  nearEnd,  nearEnd, -length);

        addBeamVertex(builder, m, r, g, b, 0,  nearEnd, -nearEnd, -length);
        addBeamVertex(builder, m, r, g, b, a,  near,    -near,    -nearClip);
        addBeamVertex(builder, m, r, g, b, a, -near,    -near,    -nearClip);
        addBeamVertex(builder, m, r, g, b, 0, -nearEnd, -nearEnd, -length);
    }

    // ── Beam 4D ─────────────────────────────────────────────────────────────

    protected void renderLightBeam4D(VertexConsumer builder, PoseStack stack, T tileEntityFixture,
                                     float partialTicks, float alpha, float beamSize,
                                     float length, int color, float focusMultiplier) {

        float endMultiplier = 1 + tileEntityFixture.getFocus() * length * focusMultiplier;

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        float intensity = 0.65f;
        int a = (int) (alpha * 255 * intensity);

        Matrix4f m = stack.last().pose();

        length += 4.0f;
        float end = endMultiplier;

        addBeamVertex(builder, m, r, g, b, 0,  beamSize * end,  beamSize * end, -length);
        addBeamVertex(builder, m, r, g, b, a,  beamSize,        beamSize,        0);
        addBeamVertex(builder, m, r, g, b, a,  beamSize,       -beamSize,        0);
        addBeamVertex(builder, m, r, g, b, 0,  beamSize * end, -beamSize * end, -length);

        addBeamVertex(builder, m, r, g, b, 0, -beamSize * end, -beamSize * end, -length);
        addBeamVertex(builder, m, r, g, b, a, -beamSize,       -beamSize,        0);
        addBeamVertex(builder, m, r, g, b, a, -beamSize,        beamSize,        0);
        addBeamVertex(builder, m, r, g, b, 0, -beamSize * end,  beamSize * end, -length);

        addBeamVertex(builder, m, r, g, b, 0, -beamSize * end,  beamSize * end, -length);
        addBeamVertex(builder, m, r, g, b, a, -beamSize,        beamSize,        0);
        addBeamVertex(builder, m, r, g, b, a,  beamSize,        beamSize,        0);
        addBeamVertex(builder, m, r, g, b, 0,  beamSize * end,  beamSize * end, -length);

        addBeamVertex(builder, m, r, g, b, 0,  beamSize * end, -beamSize * end, -length);
        addBeamVertex(builder, m, r, g, b, a,  beamSize,       -beamSize,        0);
        addBeamVertex(builder, m, r, g, b, a, -beamSize,       -beamSize,        0);
        addBeamVertex(builder, m, r, g, b, 0, -beamSize * end, -beamSize * end, -length);
    }

    // ── Gobo ────────────────────────────────────────────────────────────────

    protected void renderGoboBeams(VertexConsumer builder, PoseStack stack,
                                   T tileEntityFixture, Camera camera,
                                   float alpha, float beamSize, float length, int color,
                                   float focusMultiplier, int beamCount, float spreadAngle) {

        float goboBeamSize = beamSize * 0.4f;

        for (int i = 0; i < beamCount; i++) {
            float baseAngle = ((float) i / beamCount) * (float)(Math.PI * 2);

            stack.pushPose();
            stack.mulPose(new org.joml.Quaternionf().rotateZ(baseAngle));
            stack.mulPose(new org.joml.Quaternionf().rotateX(spreadAngle));

            if (TheatricalExtraLightsConfig.shouldRender2DBeam()) {
                renderLightBeam2D(builder, stack, tileEntityFixture, camera, alpha, goboBeamSize, length, color, focusMultiplier);
            } else {
                renderLightBeam4D(builder, stack, tileEntityFixture, 0f, alpha, goboBeamSize, length, color, focusMultiplier);
            }

            stack.popPose();
        }
    }

    // ── Lens ────────────────────────────────────────────────────────────────

    protected void renderLens(MultiBufferSource multiBufferSource, PoseStack poseStack, float alpha, int color, float size, float translateX, float translateY, float translateZ) {
        if (!TheatricalExtraLightsConfig.shouldRenderLens()) return;

        VertexConsumer lensConsumer = multiBufferSource.getBuffer(LensRenderTypes.LENS);

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a  = (int) (alpha * 255 * 0.90f);
        int lr = (int)(r * 0.90f);
        int lg = (int)(g * 0.90f);
        int lb = (int)(b * 0.90f);

        poseStack.pushPose();
        poseStack.translate(translateX, translateY, translateZ);

        Matrix4f m1 = poseStack.last().pose();

        addLensVertex(lensConsumer, m1, lr, lg, lb, a, -size,  size, 0f, 0f, 0f);
        addLensVertex(lensConsumer, m1, lr, lg, lb, a,  size,  size, 0f, 1f, 0f);
        addLensVertex(lensConsumer, m1, lr, lg, lb, a,  size, -size, 0f, 1f, 1f);
        addLensVertex(lensConsumer, m1, lr, lg, lb, a, -size, -size, 0f, 0f, 1f);

        poseStack.popPose();
    }

    // ── Lens Glow ───────────────────────────────────────────────────────────

    protected void renderLensGlow(VertexConsumer builder, PoseStack stack, int color, float size) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        Matrix4f m = stack.last().pose();

        addBeamVertex(builder, m, r, g, b, 255, -size,  size, 0f);
        addBeamVertex(builder, m, r, g, b, 255,  size,  size, 0f);
        addBeamVertex(builder, m, r, g, b, 255,  size, -size, 0f);
        addBeamVertex(builder, m, r, g, b, 255, -size, -size, 0f);
    }
}