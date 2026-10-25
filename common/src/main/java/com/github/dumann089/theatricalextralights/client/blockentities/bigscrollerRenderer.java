
package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.bigscrollerBlockEntity;
import com.github.dumann089.theatricalextralights.client.LensBloomCompat;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.lowdragmc.shimmer.client.light.ColorPointLight;
import com.lowdragmc.shimmer.client.light.LightManager;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.imabad.theatrical.TheatricalExpectPlatform;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.client.LazyRenderers;
import dev.imabad.theatrical.client.TheatricalRenderTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public class bigscrollerRenderer extends ExtraLightsRenderer<bigscrollerBlockEntity> {
    private BakedModel cachedPanModel;
    private BakedModel cachedTiltModel;
    private BakedModel cachedStaticModel;

    /*
     * Apertura visible: exactamente 2 bloques de ancho por 3 de alto.
     * La altura individual de cada gel se configura en el BlockEntity.
     */
    private static final float TAPE_MIN_X = -1.0F;
    private static final float TAPE_MAX_X = 1.0F;
    private static final float TAPE_MIN_Y = -1.03125F;
    private static final float TAPE_MAX_Y = 1.96875F;
    private static final float TAPE_Z = 0.55F;
    private static final float GEL_GAP = 0.012F;

    private static final int GEL_ALPHA = 100;
    private static final float GEL_DARKNESS = 0.1F;

    private static final double SHIMMER_RAY_START_OFFSET = 0.15D;
    private static final double SHIMMER_RAY_DISTANCE = 64.0D;
    private static final double SHIMMER_HIT_OFFSET = 0.035D;
    private static final float SHIMMER_LOCAL_X = 0.0F;
    private static final float SHIMMER_SOURCE_Z = 0.812F;

    /*
     * Cada luz representa una sección de dos filas.
     */
    private static final float[] SECTION_CENTER_Y = {
            -0.40625F,
            0.46875F,
            1.34375F
    };

    private static final float[][] BEAM_POSITIONS = {
            {0.625F, -0.625F, 0.812F},
            {0.625F, -0.1875F, 0.812F},
            {0.625F, 0.25F, 0.812F},
            {0.625F, 0.6875F, 0.812F},
            {0.625F, 1.125F, 0.812F},
            {0.625F, 1.5625F, 0.812F},

            {0.0F, -0.625F, 0.812F},
            {0.0F, -0.1875F, 0.812F},
            {0.0F, 0.25F, 0.812F},
            {0.0F, 0.6875F, 0.812F},
            {0.0F, 1.125F, 0.812F},
            {0.0F, 1.5625F, 0.812F},

            {-0.618F, -0.625F, 0.812F},
            {-0.618F, -0.1875F, 0.812F},
            {-0.618F, 0.25F, 0.812F},
            {-0.618F, 0.6875F, 0.812F},
            {-0.618F, 1.125F, 0.812F},
            {-0.618F, 1.5625F, 0.812F}
    };

    private static final class GelTapeRenderTypes extends RenderStateShard {
        private GelTapeRenderTypes() {
            super("bigscroller_gel_tape_state", () -> {}, () -> {});
        }

        private static RenderType create() {
            RenderType.CompositeState state = RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOutputState(MAIN_TARGET)
                    .createCompositeState(false);

            return RenderType.create(
                    "theatricalextralights:bigscroller_gel_tape",
                    DefaultVertexFormat.POSITION_COLOR,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    true,
                    state
            );
        }
    }

    private static final RenderType GEL_TAPE_RENDER_TYPE =
            GelTapeRenderTypes.create();

    private record ShimmerKey(ClientLevel level, BlockPos pos) {}

    private static final class ScrollerMotion {
        private final bigscrollerBlockEntity owner;
        private float offset;
        private long lastUpdateNanos;

        private ScrollerMotion(
                bigscrollerBlockEntity owner,
                float offset,
                long now
        ) {
            this.owner = owner;
            this.offset = offset;
            this.lastUpdateNanos = now;
        }
    }

    private static final class SectionShimmerLights {
        private final bigscrollerBlockEntity owner;
        private final ColorPointLight[] lights = new ColorPointLight[3];
        private final int[] lastColors = {
                Integer.MIN_VALUE,
                Integer.MIN_VALUE,
                Integer.MIN_VALUE
        };

        private SectionShimmerLights(bigscrollerBlockEntity owner) {
            this.owner = owner;
        }

        private void remove() {
            for (int i = 0; i < lights.length; i++) {
                if (lights[i] != null) {
                    lights[i].remove();
                    lights[i] = null;
                }
                lastColors[i] = Integer.MIN_VALUE;
            }
        }
    }

    private static final Map<ShimmerKey, ScrollerMotion> SCROLLER_MOTIONS =
            new HashMap<>();

    private static final Map<ShimmerKey, SectionShimmerLights> SHIMMER_LIGHTS =
            new HashMap<>();

    private static long lastCleanupNanos;

    public bigscrollerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    /*
     * Canal 4 = posición objetivo.
     * La velocidad configurada determina cuánto tarda en llegar a ella.
     */
    private static float getScrollerOffset(bigscrollerBlockEntity blockEntity) {
        float gelHeight = blockEntity.getGelHeight();
        int gelCount = blockEntity.getGelCount();

        if (gelHeight <= 0.0F || gelCount <= 0) {
            return 0.0F;
        }

        float windowCenter = (TAPE_MIN_Y + TAPE_MAX_Y) * 0.5F;
        float baseOffset = windowCenter / gelHeight - 0.5F;

        float targetOffset = baseOffset
                - (blockEntity.getScrollerPosition() / 255.0F)
                * (gelCount - 1);

        if (!(blockEntity.getLevel() instanceof ClientLevel level)) {
            return targetOffset;
        }

        ShimmerKey key = new ShimmerKey(
                level,
                blockEntity.getBlockPos().immutable()
        );

        long now = System.nanoTime();
        ScrollerMotion motion = SCROLLER_MOTIONS.get(key);

        if (motion == null || motion.owner != blockEntity) {
            motion = new ScrollerMotion(blockEntity, targetOffset, now);
            SCROLLER_MOTIONS.put(key, motion);
            return targetOffset;
        }

        float deltaSeconds = Math.min(
                Math.max(0.0F,
                        (now - motion.lastUpdateNanos) / 1_000_000_000.0F),
                0.1F
        );

        motion.lastUpdateNanos = now;

        float difference = targetOffset - motion.offset;
        float maxStep = blockEntity.getScrollerSpeed() * deltaSeconds;

        if (Math.abs(difference) <= maxStep) {
            motion.offset = targetOffset;
        } else if (maxStep > 0.0F) {
            motion.offset += Math.copySign(maxStep, difference);
        }

        return motion.offset;
    }

    private static int getGelIndexAtY(
            float y,
            float tapeOffset,
            float gelHeight
    ) {
        return (int) Math.floor(y / gelHeight - tapeOffset);
    }

    private static int[] getGelColor(
            bigscrollerBlockEntity blockEntity,
            int index
    ) {
        return blockEntity.getGelColorAtIndex(index);
    }

    private static int darken(int channel) {
        return Math.max(0,
                Math.min(255, Math.round(channel * GEL_DARKNESS)));
    }

    /*
     * Shimmer usa colores ARGB: 0xAARRGGBB.
     */
    private static int makeLightColor(int[] rgb, int intensity) {
        int level = Math.max(0, Math.min(255, intensity));
        int r = rgb[0] * level / 255;
        int g = rgb[1] * level / 255;
        int b = rgb[2] * level / 255;

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /*
     * Muestrea el centro de cada zona directamente.
     * No mezcla RGB de las dos filas, porque produciría colores inventados
     * que no corresponden a ningún gel de la lista.
     */

    private static int[] getSectionGelColor(
            bigscrollerBlockEntity blockEntity,
            int section,
            float tapeOffset
    ) {
        float gelHeight = blockEntity.getGelHeight();

        // La distribución original está ordenada por columna y luego por fila.
        int firstRowIndex = section * 2;
        int secondRowIndex = firstRowIndex + 1;

        int firstGelIndex = getGelIndexAtY(
                BEAM_POSITIONS[firstRowIndex][1],
                tapeOffset,
                gelHeight
        );

        int secondGelIndex = getGelIndexAtY(
                BEAM_POSITIONS[secondRowIndex][1],
                tapeOffset,
                gelHeight
        );

        int[] first = getGelColor(blockEntity, firstGelIndex);
        int[] second = getGelColor(blockEntity, secondGelIndex);

        return new int[] {
                (first[0] + second[0]) / 2,
                (first[1] + second[1]) / 2,
                (first[2] + second[2]) / 2
        };
    }

    @Override
    public void renderModel(
            bigscrollerBlockEntity blockEntity,
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            Direction facing,
            float partialTicks,
            boolean isFlipped,
            BlockState blockState,
            boolean isHanging,
            int packedLight,
            int packedOverlay
    ) {
        if (cachedStaticModel == null) {
            cachedStaticModel = TheatricalExpectPlatform.getBakedModel(
                    blockEntity.getFixture().getStaticModel());
        }
        if (cachedPanModel == null) {
            cachedPanModel = TheatricalExpectPlatform.getBakedModel(
                    blockEntity.getFixture().getPanModel());
        }
        if (cachedTiltModel == null) {
            cachedTiltModel = TheatricalExpectPlatform.getBakedModel(
                    blockEntity.getFixture().getTiltModel());
        }

        poseStack.translate(0.5F, 0, 0.5F);

        if (isHanging) {
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0, 0.5, 0F);

            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    poseStack.mulPose(hangDirection == Direction.SOUTH
                            ? Axis.XP.rotationDegrees(90)
                            : Axis.XN.rotationDegrees(90));
                } else {
                    poseStack.mulPose(Axis.ZN.rotationDegrees(
                            hangDirection == Direction.EAST ? 90 : -90));
                }
            }

            poseStack.translate(0, -0.35, 0F);
        }

        if (facing.getAxis() == Direction.Axis.X) {
            poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(
                    facing.getOpposite().toYRot()));
        }

        poseStack.translate(-0.5F, 0, -0.5F);

        if (isHanging) {
            Optional<BlockState> support = blockEntity.getSupportingStructure();
            if (support.isPresent()) {
                float[] transforms = blockEntity.getFixture().getTransforms(
                        blockState, support.get());
                poseStack.translate(transforms[0], transforms[1], transforms[2]);
            } else {
                poseStack.translate(0, 0.19, 0);
            }
        }

        minecraftRenderModel(
                poseStack, vertexConsumer, blockState,
                cachedStaticModel, packedLight, packedOverlay);

        float[] pans = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(pans[0], pans[1], pans[2]);

        int prevPan = blockEntity.getPrevPan();
        int pan = blockEntity.getPan();

        poseStack.mulPose(Axis.YN.rotationDegrees(
                prevPan + (pan - prevPan) * partialTicks));

        poseStack.translate(-pans[0], -pans[1], -pans[2]);

        minecraftRenderModel(
                poseStack, vertexConsumer, blockState,
                cachedPanModel, packedLight, packedOverlay);

        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tilts[0], tilts[1], tilts[2]);

        int prevTilt = blockEntity.getPrevTilt();
        int tilt = blockEntity.getTilt();

        poseStack.mulPose(Axis.XP.rotationDegrees(
                prevTilt + (tilt - prevTilt) * partialTicks));

        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);

        minecraftRenderModel(
                poseStack, vertexConsumer, blockState,
                cachedTiltModel, packedLight, packedOverlay);
    }

    @Override
    public void beforeRenderBeam(
            bigscrollerBlockEntity blockEntity,
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
        LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {
            @Override
            public void render(
                    MultiBufferSource.BufferSource bufferSource,
                    PoseStack renderStack,
                    Camera camera,
                    float partialTick
            ) {
                boolean scrollerMode = blockEntity.isScrollerMode();
                float globalIntensity = blockEntity.getDisplayIntensity(partialTick);

                if (!scrollerMode) {
                    removeSectionShimmerLights(blockEntity);
                    if (globalIntensity <= 0) {
                        return;
                    }
                }

                Vec3 offset = Vec3.atLowerCornerOf(
                        blockEntity.getBlockPos()).subtract(camera.getPosition());

                float tapeOffset = scrollerMode
                        ? getScrollerOffset(blockEntity)
                        : 0.0F;

                if (scrollerMode) {
                    updateSectionShimmerLights(
                            blockEntity, facing, partialTick,
                            isFlipped, blockstate, isHanging, tapeOffset);

                    renderGelTape(
                            blockEntity, bufferSource, renderStack, offset,
                            facing, partialTick, isFlipped,
                            blockstate, isHanging, tapeOffset);

                    bufferSource.endBatch(GEL_TAPE_RENDER_TYPE);
                }

                VertexConsumer beamConsumer =
                        bufferSource.getBuffer(TheatricalRenderTypes.BEAM);

                int[][] beamColors = new int[BEAM_POSITIONS.length][3];
                int[] beamAlpha = new int[BEAM_POSITIONS.length];

                int rgbColor = blockEntity.getDisplayColour(partialTick);
                int rgbR = (rgbColor >> 16) & 255;
                int rgbG = (rgbColor >> 8) & 255;
                int rgbB = rgbColor & 255;

                for (int i = 0; i < BEAM_POSITIONS.length; i++) {
                    float[] pos = BEAM_POSITIONS[i];
                    int section = (i % 6) / 2;

                    int r;
                    int g;
                    int b;
                    int alpha;

                    if (scrollerMode) {
                        int gelIndex = getGelIndexAtY(
                                pos[1], tapeOffset, blockEntity.getGelHeight());

                        int[] gel = getGelColor(blockEntity, gelIndex);
                        r = gel[0];
                        g = gel[1];
                        b = gel[2];
                        alpha = blockEntity.getSectionIntensity(section);
                    } else {
                        r = rgbR;
                        g = rgbG;
                        b = rgbB;
                        alpha = (int) globalIntensity;
                    }

                    beamColors[i][0] = r;
                    beamColors[i][1] = g;
                    beamColors[i][2] = b;
                    beamAlpha[i] = alpha;

                    if (alpha <= 0) {
                        continue;
                    }

                    int wr = LensBloomCompat.whiten(r, 0.80F);
                    int wg = LensBloomCompat.whiten(g, 0.80F);
                    int wb = LensBloomCompat.whiten(b, 0.80F);

                    renderStack.pushPose();
                    renderStack.translate(offset.x, offset.y, offset.z);

                    preparePoseStack(
                            blockEntity, renderStack, facing, partialTick,
                            isFlipped, blockstate, isHanging);

                    renderStack.translate(pos[0], pos[1], pos[2]);

                    Matrix4f matrix = renderStack.last().pose();
                    Matrix3f normal = renderStack.last().normal();

                    addVertex(beamConsumer, matrix, normal,
                            wr, wg, wb, alpha, -0.1333F, 0.1333F, 0F);
                    addVertex(beamConsumer, matrix, normal,
                            wr, wg, wb, alpha, 0.1333F, 0.1333F, 0F);
                    addVertex(beamConsumer, matrix, normal,
                            wr, wg, wb, alpha, 0.1333F, -0.1333F, 0F);
                    addVertex(beamConsumer, matrix, normal,
                            wr, wg, wb, alpha, -0.1333F, -0.1333F, 0F);

                    renderStack.popPose();
                }

                LensBloomCompat.renderBloomedQuads(renderStack, (bloomConsumer, bloomStack) -> {
                    for (int i = 0; i < BEAM_POSITIONS.length; i++) {
                        int alpha = beamAlpha[i];
                        if (alpha <= 0) {
                            continue;
                        }

                        float[] pos = BEAM_POSITIONS[i];
                        int r = beamColors[i][0] * alpha / 255;
                        int g = beamColors[i][1] * alpha / 255;
                        int b = beamColors[i][2] * alpha / 255;

                        bloomStack.pushPose();
                        bloomStack.translate(offset.x, offset.y, offset.z);

                        preparePoseStack(
                                blockEntity, bloomStack, facing, partialTick,
                                isFlipped, blockstate, isHanging);

                        bloomStack.translate(pos[0], pos[1], pos[2]);

                        LensBloomCompat.emitQuad(
                                bloomConsumer, bloomStack, r, g, b,
                                0.1333F, 0.1333F);

                        bloomStack.popPose();
                    }
                });
            }

            @Override
            public Vec3 getPos(float partialTick) {
                return blockEntity.getBlockPos().getCenter();
            }
        });
    }

    private void renderGelTape(
            bigscrollerBlockEntity blockEntity,
            MultiBufferSource.BufferSource bufferSource,
            PoseStack poseStack,
            Vec3 offset,
            Direction facing,
            float partialTick,
            boolean isFlipped,
            BlockState blockstate,
            boolean isHanging,
            float tapeOffset
    ) {
        VertexConsumer tapeConsumer =
                bufferSource.getBuffer(GEL_TAPE_RENDER_TYPE);

        float gelHeight = blockEntity.getGelHeight();

        if (gelHeight <= 0.0F || blockEntity.getGelCount() <= 0) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(offset.x, offset.y, offset.z);

        preparePoseStack(
                blockEntity, poseStack, facing, partialTick,
                isFlipped, blockstate, isHanging);

        Matrix4f matrix = poseStack.last().pose();

        int first = (int) Math.floor(TAPE_MIN_Y / gelHeight - tapeOffset) - 1;
        int last = (int) Math.ceil(TAPE_MAX_Y / gelHeight - tapeOffset) + 1;

        for (int segment = first; segment <= last; segment++) {
            float y0 = segment * gelHeight + tapeOffset * gelHeight;
            float y1 = y0 + gelHeight;

            if (y1 < TAPE_MIN_Y || y0 > TAPE_MAX_Y) {
                continue;
            }

            float clippedY0 = Math.max(y0, TAPE_MIN_Y) + GEL_GAP / 2;
            float clippedY1 = Math.min(y1, TAPE_MAX_Y) - GEL_GAP / 2;

            if (clippedY1 <= clippedY0) {
                continue;
            }

            int[] gel = getGelColor(blockEntity, segment);

            tapeQuad(
                    tapeConsumer, matrix,
                    TAPE_MIN_X, clippedY0, TAPE_Z,
                    TAPE_MAX_X, clippedY1,
                    darken(gel[0]), darken(gel[1]), darken(gel[2]),
                    GEL_ALPHA
            );

            float dividerY = Math.min(y1, TAPE_MAX_Y);
            if (dividerY >= TAPE_MIN_Y && dividerY <= TAPE_MAX_Y) {
                tapeQuad(
                        tapeConsumer, matrix,
                        TAPE_MIN_X, dividerY - 0.004F, TAPE_Z + 0.001F,
                        TAPE_MAX_X, dividerY + 0.004F,
                        12, 12, 12, 230
                );
            }
        }

        poseStack.popPose();
    }

    private void tapeQuad(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x0,
            float y0,
            float z,
            float x1,
            float y1,
            int r,
            int g,
            int b,
            int alpha
    ) {
        consumer.vertex(matrix, x0, y1, z).color(r, g, b, alpha).endVertex();
        consumer.vertex(matrix, x1, y1, z).color(r, g, b, alpha).endVertex();
        consumer.vertex(matrix, x1, y0, z).color(r, g, b, alpha).endVertex();
        consumer.vertex(matrix, x0, y0, z).color(r, g, b, alpha).endVertex();
    }

    private void updateSectionShimmerLights(
            bigscrollerBlockEntity blockEntity,
            Direction facing,
            float partialTick,
            boolean isFlipped,
            BlockState blockstate,
            boolean isHanging,
            float tapeOffset
    ) {
        if (!(blockEntity.getLevel() instanceof ClientLevel level)) {
            return;
        }

        BlockPos blockPos = blockEntity.getBlockPos().immutable();
        ShimmerKey key = new ShimmerKey(level, blockPos);

        cleanupLights();

        SectionShimmerLights set = SHIMMER_LIGHTS.get(key);
        if (set == null || set.owner != blockEntity) {
            if (set != null) {
                set.remove();
            }

            set = new SectionShimmerLights(blockEntity);
            SHIMMER_LIGHTS.put(key, set);
        }

        Vec3 direction = BaseLightBlockEntity.rayTraceDir(blockEntity).normalize();

        if (direction.lengthSqr() < 1.0E-8D) {
            disableLights(set);
            return;
        }

        float radius = (float) blockEntity.getFixture().getLightRadius();

        for (int section = 0; section < 3; section++) {
            int intensity = blockEntity.getSectionIntensity(section);
            ColorPointLight light = set.lights[section];

            if (intensity <= 0) {
                if (light != null && !light.isRemoved()) {
                    light.setEnable(false);
                }
                continue;
            }

            PoseStack sourceStack = new PoseStack();

            preparePoseStack(
                    blockEntity, sourceStack, facing, partialTick,
                    isFlipped, blockstate, isHanging);

            Vector3f sourceLocal = sourceStack.last().pose().transformPosition(
                    SHIMMER_LOCAL_X,
                    SECTION_CENTER_Y[section],
                    SHIMMER_SOURCE_Z,
                    new Vector3f()
            );

            Vec3 source = new Vec3(
                    blockPos.getX() + sourceLocal.x,
                    blockPos.getY() + sourceLocal.y,
                    blockPos.getZ() + sourceLocal.z
            ).add(direction.scale(SHIMMER_RAY_START_OFFSET));

            Vec3 end = source.add(direction.scale(SHIMMER_RAY_DISTANCE));

            BlockHitResult hit = level.clip(new ClipContext(
                    source,
                    end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    null
            ));

            if (hit.getType() != HitResult.Type.BLOCK || hit.isInside()) {
                if (light != null && !light.isRemoved()) {
                    light.setEnable(false);
                }
                continue;
            }

            Vec3 impact = hit.getLocation().add(
                    hit.getDirection().getStepX() * SHIMMER_HIT_OFFSET,
                    hit.getDirection().getStepY() * SHIMMER_HIT_OFFSET,
                    hit.getDirection().getStepZ() * SHIMMER_HIT_OFFSET
            );

            /*
             * Cada luz utiliza exclusivamente el gel que corresponde
             * al centro de su propia zona.
             */

            int[] sectionGel = getSectionGelColor(
                    blockEntity,
                    section,
                    tapeOffset
            );

            int color = makeLightColor(sectionGel, intensity);

            Vector3f impactPosition = new Vector3f(
                    (float) impact.x,
                    (float) impact.y,
                    (float) impact.z
            );

            if (light == null || light.isRemoved()) {
                light = LightManager.INSTANCE.addLight(
                        impactPosition,
                        color,
                        radius,
                        false
                );

                set.lights[section] = light;
            }

            if (light != null) {
                // Actualiza el RGB en cada render: no conserva un color rojo en caché.
                light.setColor(color);
                light.setPos(
                        impactPosition.x,
                        impactPosition.y,
                        impactPosition.z
                );
                light.radius = radius;
                light.setEnable(true);
            }
        }
    }

    private static void disableLights(SectionShimmerLights set) {
        for (ColorPointLight light : set.lights) {
            if (light != null && !light.isRemoved()) {
                light.setEnable(false);
            }
        }
    }

    public static void removeSectionShimmerLights(Level level, BlockPos pos) {
        if (!(level instanceof ClientLevel clientLevel)) {
            return;
        }

        ShimmerKey key = new ShimmerKey(clientLevel, pos.immutable());

        SectionShimmerLights lights = SHIMMER_LIGHTS.remove(key);
        if (lights != null) {
            lights.remove();
        }

        SCROLLER_MOTIONS.remove(key);
    }

    private static void removeSectionShimmerLights(
            bigscrollerBlockEntity blockEntity
    ) {
        if (blockEntity.getLevel() != null) {
            removeSectionShimmerLights(
                    blockEntity.getLevel(),
                    blockEntity.getBlockPos()
            );
        }
    }

    private static void cleanupLights() {
        long now = System.nanoTime();
        if (now - lastCleanupNanos < 1_000_000_000L) {
            return;
        }
        lastCleanupNanos = now;

        Minecraft minecraft = Minecraft.getInstance();

        Iterator<Map.Entry<ShimmerKey, SectionShimmerLights>> lightIterator =
                SHIMMER_LIGHTS.entrySet().iterator();

        while (lightIterator.hasNext()) {
            Map.Entry<ShimmerKey, SectionShimmerLights> entry =
                    lightIterator.next();

            ShimmerKey key = entry.getKey();

            boolean remove = minecraft.level != key.level();

            if (!remove) {
                if (!key.level().hasChunkAt(key.pos())) {
                    remove = true;
                } else if (key.level().getBlockEntity(key.pos())
                        != entry.getValue().owner) {
                    remove = true;
                }
            }

            if (remove) {
                entry.getValue().remove();
                SCROLLER_MOTIONS.remove(key);
                lightIterator.remove();
            }
        }

        Iterator<Map.Entry<ShimmerKey, ScrollerMotion>> motionIterator =
                SCROLLER_MOTIONS.entrySet().iterator();

        while (motionIterator.hasNext()) {
            Map.Entry<ShimmerKey, ScrollerMotion> entry =
                    motionIterator.next();

            ShimmerKey key = entry.getKey();

            if (minecraft.level != key.level()
                    || !key.level().hasChunkAt(key.pos())
                    || key.level().getBlockEntity(key.pos())
                    != entry.getValue().owner) {
                motionIterator.remove();
            }
        }
    }

    @Override
    public void preparePoseStack(
            bigscrollerBlockEntity blockEntity,
            PoseStack poseStack,
            Direction facing,
            float partialTicks,
            boolean isFlipped,
            BlockState blockState,
            boolean isHanging
    ) {
        FixtureMountTransform.apply(poseStack, blockEntity);
        poseStack.translate(0.5F, 0, 0.5F);

        if (isHanging) {
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0, 0.5, 0F);

            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    poseStack.mulPose(hangDirection == Direction.SOUTH
                            ? Axis.XP.rotationDegrees(90)
                            : Axis.XN.rotationDegrees(90));
                } else {
                    poseStack.mulPose(Axis.ZN.rotationDegrees(
                            hangDirection == Direction.EAST ? 90 : -90));
                }
            }

            poseStack.translate(0, -0.35, 0F);
        }

        if (facing.getAxis() == Direction.Axis.X) {
            poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(
                    facing.getOpposite().toYRot()));
        }

        poseStack.translate(-0.5F, 0, -0.5F);

        if (isHanging) {
            Optional<BlockState> support = blockEntity.getSupportingStructure();

            if (support.isPresent()) {
                float[] transforms = blockEntity.getFixture().getTransforms(
                        blockState, support.get());

                poseStack.translate(transforms[0], transforms[1], transforms[2]);
            } else {
                poseStack.translate(0, 0.19, 0);
            }
        }

        float[] pans = blockEntity.getFixture().getPanRotationPosition();
        poseStack.translate(pans[0], pans[1], pans[2]);

        int prevPan = blockEntity.getPrevPan();
        int pan = blockEntity.getPan();

        poseStack.mulPose(Axis.YN.rotationDegrees(
                prevPan + (pan - prevPan) * partialTicks));

        poseStack.translate(-pans[0], -pans[1], -pans[2]);

        float[] tilts = blockEntity.getFixture().getTiltRotationPosition();
        poseStack.translate(tilts[0], tilts[1], tilts[2]);

        int prevTilt = blockEntity.getPrevTilt();
        int tilt = blockEntity.getTilt();

        poseStack.mulPose(Axis.XP.rotationDegrees(
                prevTilt + (tilt - prevTilt) * partialTicks));

        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
    }
}
