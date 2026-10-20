
package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.verticalscrollerBlockEntity;
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

public class verticalscrollerRenderer
        extends ExtraLightsRenderer<verticalscrollerBlockEntity> {

    private BakedModel cachedPanModel;
    private BakedModel cachedTiltModel;
    private BakedModel cachedStaticModel;

    /*
     * Área física de la tira: dos bloques de ancho por uno de alto.
     * GelWidth controla el ancho de cada franja coloreada.
     */
    private static final float TAPE_MIN_X = 0.0F;
    private static final float TAPE_MAX_X = 1.0F;
    private static final float TAPE_MIN_Y = 0.0F;
    private static final float TAPE_MAX_Y = 1.9F;
    private static final float TAPE_Z = 0.4F;
    private static final float GEL_GAP = 0.012F;

    private static final int GEL_ALPHA = 100;
    private static final float GEL_DARKNESS = 0.1F;

    private static final double RAY_START_OFFSET = 0.15D;
    private static final double RAY_DISTANCE = 64.0D;
    private static final double HIT_OFFSET = 0.035D;

    private static final float[][] BEAM_POSITIONS = {
            {0.687F, 0.390F, 0.468F},
            {0.687F, 0.765F, 0.468F},
            {0.687F, 1.140F, 0.468F},
            {0.687F, 1.515F, 0.468F},
            {0.250F, 0.390F, 0.468F},
            {0.250F, 0.765F, 0.468F},
            {0.250F, 1.140F, 0.468F},
            {0.250F, 1.515F, 0.468F}
    };

    private static final class GelTapeRenderTypes extends RenderStateShard {
        private GelTapeRenderTypes() {
            super("horizontal_scroller_gel_tape_state", () -> {}, () -> {});
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
                    "theatricalextralights:horizontal_scroller_gel_tape",
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
        private final verticalscrollerBlockEntity owner;
        private float offset;
        private long lastUpdateNanos;

        private ScrollerMotion(
                verticalscrollerBlockEntity owner,
                float offset,
                long now
        ) {
            this.owner = owner;
            this.offset = offset;
            this.lastUpdateNanos = now;
        }
    }

    private static final class ShimmerLights {
        private final verticalscrollerBlockEntity owner;
        private final ColorPointLight[] lights = new ColorPointLight[8];

        private ShimmerLights(verticalscrollerBlockEntity owner) {
            this.owner = owner;
        }

        private void remove() {
            for (int i = 0; i < lights.length; i++) {
                if (lights[i] != null) {
                    lights[i].remove();
                    lights[i] = null;
                }
            }
        }
    }

    private static final Map<ShimmerKey, ScrollerMotion> MOTIONS =
            new HashMap<>();

    private static final Map<ShimmerKey, ShimmerLights> SHIMMER_LIGHTS =
            new HashMap<>();

    private static long lastCleanupNanos;

    public verticalscrollerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    /*
     * El canal 2 define el destino. La velocidad del BlockEntity determina
     * la rapidez con la que la tira llega al destino.
     */
    private static float getScrollerOffset(
            verticalscrollerBlockEntity blockEntity
    ) {
        float gelWidth = blockEntity.getGelWidth();
        int gelCount = blockEntity.getGelCount();

        if (gelWidth <= 0.0F || gelCount <= 0) {
            return 0.0F;
        }

        float centerX = (TAPE_MIN_X + TAPE_MAX_X) * 0.5F;
        float baseOffset = centerX / gelWidth - 0.5F;

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
        ScrollerMotion motion = MOTIONS.get(key);

        if (motion == null || motion.owner != blockEntity) {
            motion = new ScrollerMotion(blockEntity, targetOffset, now);
            MOTIONS.put(key, motion);
            return targetOffset;
        }

        float deltaSeconds = Math.min(
                Math.max(
                        0.0F,
                        (now - motion.lastUpdateNanos) / 1_000_000_000.0F
                ),
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

    /*
     * La misma fórmula se usa para el gel visible, cada beam y Shimmer.
     */
    private static int getGelIndexAtX(
            float x,
            float tapeOffset,
            float gelWidth
    ) {
        return (int) Math.floor(x / gelWidth - tapeOffset);
    }

    private static int[] getGelColor(
            verticalscrollerBlockEntity blockEntity,
            int index
    ) {
        return blockEntity.getGelColorAtIndex(index);
    }

    private static int darken(int channel) {
        return Math.max(
                0,
                Math.min(255, Math.round(channel * GEL_DARKNESS))
        );
    }

    private static int makeLightColor(int[] rgb, int intensity) {
        int level = Math.max(0, Math.min(255, intensity));
        int r = rgb[0] * level / 255;
        int g = rgb[1] * level / 255;
        int b = rgb[2] * level / 255;

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    public void renderModel(
            verticalscrollerBlockEntity blockEntity,
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
            verticalscrollerBlockEntity blockEntity,
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
                float intensity = blockEntity.getDisplayIntensity(partialTick);

                if (!scrollerMode) {
                    removeShimmerLights(blockEntity);

                    if (intensity <= 0) {
                        return;
                    }
                }

                Vec3 cameraOffset = Vec3.atLowerCornerOf(
                        blockEntity.getBlockPos()).subtract(camera.getPosition());

                float tapeOffset = scrollerMode
                        ? getScrollerOffset(blockEntity)
                        : 0.0F;

                if (scrollerMode) {
                    updateShimmerLights(
                            blockEntity,
                            facing,
                            partialTick,
                            isFlipped,
                            blockstate,
                            isHanging,
                            tapeOffset,
                            intensity
                    );

                    renderGelTape(
                            blockEntity,
                            bufferSource,
                            renderStack,
                            cameraOffset,
                            facing,
                            partialTick,
                            isFlipped,
                            blockstate,
                            isHanging,
                            tapeOffset
                    );

                    bufferSource.endBatch(GEL_TAPE_RENDER_TYPE);
                }

                VertexConsumer beamConsumer =
                        bufferSource.getBuffer(TheatricalRenderTypes.BEAM);

                int rgbColor = blockEntity.getDisplayColour(partialTick);
                int rgbR = (rgbColor >> 16) & 255;
                int rgbG = (rgbColor >> 8) & 255;
                int rgbB = rgbColor & 255;
                int alpha = Math.max(0, Math.min(255, (int) intensity));

                int[][] beamColors = new int[BEAM_POSITIONS.length][3];
                int[] beamAlphas = new int[BEAM_POSITIONS.length];

                for (int i = 0; i < BEAM_POSITIONS.length; i++) {
                    float[] pos = BEAM_POSITIONS[i];

                    int r;
                    int g;
                    int b;

                    if (scrollerMode) {
                        int gelIndex = getGelIndexAtX(
                                pos[0],
                                tapeOffset,
                                blockEntity.getGelWidth()
                        );

                        int[] gel = getGelColor(blockEntity, gelIndex);
                        r = gel[0];
                        g = gel[1];
                        b = gel[2];
                    } else {
                        r = rgbR;
                        g = rgbG;
                        b = rgbB;
                    }

                    beamColors[i][0] = r;
                    beamColors[i][1] = g;
                    beamColors[i][2] = b;
                    beamAlphas[i] = alpha;

                    if (alpha <= 0) {
                        continue;
                    }

                    int wr = LensBloomCompat.whiten(r, 0.80F);
                    int wg = LensBloomCompat.whiten(g, 0.80F);
                    int wb = LensBloomCompat.whiten(b, 0.80F);

                    renderStack.pushPose();
                    renderStack.translate(
                            cameraOffset.x,
                            cameraOffset.y,
                            cameraOffset.z
                    );

                    preparePoseStack(
                            blockEntity, renderStack, facing, partialTick,
                            isFlipped, blockstate, isHanging);

                    renderStack.translate(pos[0], pos[1], pos[2]);

                    Matrix4f matrix = renderStack.last().pose();
                    Matrix3f normal = renderStack.last().normal();

                    addVertex(beamConsumer, matrix, normal, wr, wg, wb, alpha,
                            -0.09375F, 0.09375F, 0F);
                    addVertex(beamConsumer, matrix, normal, wr, wg, wb, alpha,
                            0.09375F, 0.09375F, 0F);
                    addVertex(beamConsumer, matrix, normal, wr, wg, wb, alpha,
                            0.09375F, -0.09375F, 0F);
                    addVertex(beamConsumer, matrix, normal, wr, wg, wb, alpha,
                            -0.09375F, -0.09375F, 0F);

                    renderStack.popPose();
                }

                LensBloomCompat.renderBloomedQuads(renderStack, (bloomConsumer, bloomStack) -> {
                    for (int i = 0; i < BEAM_POSITIONS.length; i++) {
                        if (beamAlphas[i] <= 0) {
                            continue;
                        }

                        float[] pos = BEAM_POSITIONS[i];

                        int r = beamColors[i][0] * beamAlphas[i] / 255;
                        int g = beamColors[i][1] * beamAlphas[i] / 255;
                        int b = beamColors[i][2] * beamAlphas[i] / 255;

                        bloomStack.pushPose();
                        bloomStack.translate(
                                cameraOffset.x,
                                cameraOffset.y,
                                cameraOffset.z
                        );

                        preparePoseStack(
                                blockEntity, bloomStack, facing, partialTick,
                                isFlipped, blockstate, isHanging);

                        bloomStack.translate(pos[0], pos[1], pos[2]);

                        LensBloomCompat.emitQuad(
                                bloomConsumer,
                                bloomStack,
                                r, g, b,
                                0.09375F, 0.09375F
                        );

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
            verticalscrollerBlockEntity blockEntity,
            MultiBufferSource.BufferSource bufferSource,
            PoseStack poseStack,
            Vec3 cameraOffset,
            Direction facing,
            float partialTick,
            boolean isFlipped,
            BlockState blockstate,
            boolean isHanging,
            float tapeOffset
    ) {
        VertexConsumer tapeConsumer =
                bufferSource.getBuffer(GEL_TAPE_RENDER_TYPE);

        float gelWidth = blockEntity.getGelWidth();

        if (gelWidth <= 0.0F || blockEntity.getGelCount() <= 0) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(cameraOffset.x, cameraOffset.y, cameraOffset.z);

        preparePoseStack(
                blockEntity, poseStack, facing, partialTick,
                isFlipped, blockstate, isHanging);

        Matrix4f matrix = poseStack.last().pose();

        int first = (int) Math.floor(
                TAPE_MIN_X / gelWidth - tapeOffset) - 1;

        int last = (int) Math.ceil(
                TAPE_MAX_X / gelWidth - tapeOffset) + 1;

        for (int segment = first; segment <= last; segment++) {
            float x0 = segment * gelWidth + tapeOffset * gelWidth;
            float x1 = x0 + gelWidth;

            if (x1 < TAPE_MIN_X || x0 > TAPE_MAX_X) {
                continue;
            }

            float clippedX0 = Math.max(x0, TAPE_MIN_X) + GEL_GAP / 2;
            float clippedX1 = Math.min(x1, TAPE_MAX_X) - GEL_GAP / 2;

            if (clippedX1 <= clippedX0) {
                continue;
            }

            int[] gel = getGelColor(blockEntity, segment);

            tapeQuad(
                    tapeConsumer,
                    matrix,
                    clippedX0,
                    TAPE_MIN_Y,
                    TAPE_Z,
                    clippedX1,
                    TAPE_MAX_Y,
                    darken(gel[0]),
                    darken(gel[1]),
                    darken(gel[2]),
                    GEL_ALPHA
            );

            float dividerX = Math.min(x1, TAPE_MAX_X);

            if (dividerX >= TAPE_MIN_X && dividerX <= TAPE_MAX_X) {
                tapeQuad(
                        tapeConsumer,
                        matrix,
                        dividerX - 0.004F,
                        TAPE_MIN_Y,
                        TAPE_Z + 0.001F,
                        dividerX + 0.004F,
                        TAPE_MAX_Y,
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

    private void updateShimmerLights(
            verticalscrollerBlockEntity blockEntity,
            Direction facing,
            float partialTick,
            boolean isFlipped,
            BlockState blockstate,
            boolean isHanging,
            float tapeOffset,
            float intensity
    ) {
        if (!(blockEntity.getLevel() instanceof ClientLevel level)) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos().immutable();
        ShimmerKey key = new ShimmerKey(level, pos);

        cleanupLights();

        ShimmerLights set = SHIMMER_LIGHTS.get(key);

        if (set == null || set.owner != blockEntity) {
            if (set != null) {
                set.remove();
            }

            set = new ShimmerLights(blockEntity);
            SHIMMER_LIGHTS.put(key, set);
        }

        if (intensity <= 0) {
            disableLights(set);
            return;
        }

        Vec3 direction = BaseLightBlockEntity.rayTraceDir(blockEntity).normalize();

        if (direction.lengthSqr() < 1.0E-8D) {
            disableLights(set);
            return;
        }

        float radius = (float) blockEntity.getFixture().getLightRadius();
        int lightIntensity = Math.max(0, Math.min(255, (int) intensity));

        for (int i = 0; i < BEAM_POSITIONS.length; i++) {
            ColorPointLight light = set.lights[i];
            float[] beam = BEAM_POSITIONS[i];

            PoseStack sourceStack = new PoseStack();

            preparePoseStack(
                    blockEntity,
                    sourceStack,
                    facing,
                    partialTick,
                    isFlipped,
                    blockstate,
                    isHanging
            );

            // Origen transformado del beam en coordenadas del mundo.
            Vector3f local = sourceStack.last().pose().transformPosition(
                    beam[0], beam[1], beam[2], new Vector3f());

            Vec3 source = new Vec3(
                    pos.getX() + local.x,
                    pos.getY() + local.y,
                    pos.getZ() + local.z
            ).add(direction.scale(RAY_START_OFFSET));

            Vec3 end = source.add(direction.scale(RAY_DISTANCE));

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
                    hit.getDirection().getStepX() * HIT_OFFSET,
                    hit.getDirection().getStepY() * HIT_OFFSET,
                    hit.getDirection().getStepZ() * HIT_OFFSET
            );

            // Exactamente el mismo gel que utiliza el beam en esta coordenada X.
            int gelIndex = getGelIndexAtX(
                    beam[0], tapeOffset, blockEntity.getGelWidth());

            int[] gel = getGelColor(blockEntity, gelIndex);
            int color = makeLightColor(gel, lightIntensity);

            Vector3f hitPosition = new Vector3f(
                    (float) impact.x,
                    (float) impact.y,
                    (float) impact.z
            );

            if (light == null || light.isRemoved()) {
                light = LightManager.INSTANCE.addLight(
                        hitPosition, color, radius, false);
                set.lights[i] = light;
            }

            if (light != null) {
                light.setColor(color);
                light.setPos(hitPosition.x, hitPosition.y, hitPosition.z);
                light.radius = radius;
                light.setEnable(true);
            }
        }
    }

    private static void disableLights(ShimmerLights set) {
        for (ColorPointLight light : set.lights) {
            if (light != null && !light.isRemoved()) {
                light.setEnable(false);
            }
        }
    }

    public static void removeShimmerLights(Level level, BlockPos pos) {
        if (!(level instanceof ClientLevel clientLevel)) {
            return;
        }

        ShimmerKey key = new ShimmerKey(clientLevel, pos.immutable());

        ShimmerLights lights = SHIMMER_LIGHTS.remove(key);
        if (lights != null) {
            lights.remove();
        }

        MOTIONS.remove(key);
    }

    private static void removeShimmerLights(
            verticalscrollerBlockEntity blockEntity
    ) {
        if (blockEntity.getLevel() != null) {
            removeShimmerLights(
                    blockEntity.getLevel(), blockEntity.getBlockPos());
        }
    }

    private static void cleanupLights() {
        long now = System.nanoTime();

        if (now - lastCleanupNanos < 1_000_000_000L) {
            return;
        }

        lastCleanupNanos = now;

        Minecraft minecraft = Minecraft.getInstance();

        Iterator<Map.Entry<ShimmerKey, ShimmerLights>> lightIterator =
                SHIMMER_LIGHTS.entrySet().iterator();

        while (lightIterator.hasNext()) {
            Map.Entry<ShimmerKey, ShimmerLights> entry = lightIterator.next();
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
                MOTIONS.remove(key);
                lightIterator.remove();
            }
        }

        Iterator<Map.Entry<ShimmerKey, ScrollerMotion>> motionIterator =
                MOTIONS.entrySet().iterator();

        while (motionIterator.hasNext()) {
            Map.Entry<ShimmerKey, ScrollerMotion> entry = motionIterator.next();
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
            verticalscrollerBlockEntity blockEntity,
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