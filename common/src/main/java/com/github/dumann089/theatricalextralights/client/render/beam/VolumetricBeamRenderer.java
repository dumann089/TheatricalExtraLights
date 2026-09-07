package com.github.dumann089.theatricalextralights.client.render.beam;

import com.github.dumann089.theatricalextralights.client.IrisCompat;
import com.github.dumann089.theatricalextralights.client.ModShaders;
import com.github.dumann089.theatricalextralights.client.render.beam.raymarch.RaymarchBeamRenderer;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.imabad.theatrical.client.LazyRenderers;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class VolumetricBeamRenderer extends LazyRenderers.LazyRenderer {

    private static final int MAX_BEAMS_PER_FIXTURE = 32;
    private static final VolumetricBeamRenderer FALLBACK_ONE_SHOT = new VolumetricBeamRenderer();

    private final float[][] cachedVertsSlots = new float[MAX_BEAMS_PER_FIXTURE][16384];
    private final float[][] cachedCenterSlots = new float[MAX_BEAMS_PER_FIXTURE][64 * StackedVolumeMesh.CENTER_STRIDE];
    private final int[] cachedQuadCountSlots = new int[MAX_BEAMS_PER_FIXTURE];
    private final int[] cachedCenterCountSlots = new int[MAX_BEAMS_PER_FIXTURE];
    private final int[] cachedHashSlots = new int[MAX_BEAMS_PER_FIXTURE];

    private final int[] beamR = new int[MAX_BEAMS_PER_FIXTURE];
    private final int[] beamG = new int[MAX_BEAMS_PER_FIXTURE];
    private final int[] beamB = new int[MAX_BEAMS_PER_FIXTURE];
    private final float[] beamAlphaScale = new float[MAX_BEAMS_PER_FIXTURE];
    private final float[] beamScanLen = new float[MAX_BEAMS_PER_FIXTURE];
    private final float[] beamMidX = new float[MAX_BEAMS_PER_FIXTURE];
    private final float[] beamMidY = new float[MAX_BEAMS_PER_FIXTURE];
    private final float[] beamMidZ = new float[MAX_BEAMS_PER_FIXTURE];
    private final RenderType[] beamRenderTypes = new RenderType[MAX_BEAMS_PER_FIXTURE];
    private final boolean[] processedSlots = new boolean[MAX_BEAMS_PER_FIXTURE];

    private int activeBeamCount = 0;
    private BlockPos currentPos;

    public void render(BeamRenderData data, PoseStack poseStack) {
        if (!TheatricalExtraLightsConfig.isVolumetricBeamEnabled() || data.intensity() <= 0.0f) {
            return;
        }

        if (TheatricalExtraLightsConfig.isRaymarchEngine() && ModShaders.canUseRaymarch()) {
            RaymarchBeamRenderer.submit(data);
            return;
        }

        enqueueStacked(data);
    }

    /**
     * Used when raymarch cannot draw (missing depth / shader): still show a filled 3D volume.
     */
    public static void renderFallbackSlot(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack,
                                          Camera camera, RaymarchBeamRenderer.BeamSlot slot) {
        if (slot.fixturePos == null) {
            return;
        }
        float rawScan = slot.hitBlock
                ? Math.max(0.01f, slot.scanLen - 2.5f)
                : TheatricalExtraLightsConfig.getVolumetricBeamDistance();
        BeamRenderData data = new BeamRenderData(
                slot.fixturePos,
                new Vec3(slot.localOriginX, slot.localOriginY, slot.localOriginZ),
                new Vec3(slot.dirX, slot.dirY, slot.dirZ),
                new Vec3(slot.uX, slot.uY, slot.uZ),
                new Vec3(slot.vX, slot.vY, slot.vZ),
                0.0f,
                rawScan,
                slot.tanHalfAngle,
                slot.color,
                slot.intensity,
                slot.goboTexture,
                slot.nextGoboTexture, // <-- AÑADIDO
                slot.goboRotation,
                slot.wheelTransition, // <-- AÑADIDO
                null,
                slot.widthScale,
                slot.heightScale,
                slot.baseRadius
        );

        FALLBACK_ONE_SHOT.activeBeamCount = 0;
        FALLBACK_ONE_SHOT.enqueueStacked(data, false);
        FALLBACK_ONE_SHOT.render(bufferSource, poseStack, camera, 0.0f);
    }

    private void enqueueStacked(BeamRenderData data) {
        enqueueStacked(data, true);
    }

    private void enqueueStacked(BeamRenderData data, boolean registerLazy) {
        if (this.activeBeamCount == 0) {
            this.currentPos = data.fixturePos();
            if (registerLazy) {
                LazyRenderers.addLazyRender(this);
            }
        }

        int slot = this.activeBeamCount;
        if (slot >= MAX_BEAMS_PER_FIXTURE) {
            return;
        }

        float maxDist = TheatricalExtraLightsConfig.getVolumetricBeamDistance();
        boolean hitBlock = data.scanLen() < maxDist;
        float scanLen = hitBlock ? data.scanLen() + 2.5f : maxDist;
        if (scanLen <= 0.0f) {
            return;
        }

        float density = TheatricalExtraLightsConfig.getVolumetricBeamDensity();
        float maxAlpha = TheatricalExtraLightsConfig.getVolumetricBeamMaxAlpha();
        float fadeLen = TheatricalExtraLightsConfig.getVolumetricBeamFadeLength();
        int discs = StackedVolumeMesh.discCount(scanLen);

        int currentHash = 1;
        currentHash = 31 * currentHash + data.generateStateHash(discs);
        currentHash = 31 * currentHash + Float.floatToIntBits(scanLen);
        currentHash = 31 * currentHash + Float.floatToIntBits(density);
        currentHash = 31 * currentHash + Float.floatToIntBits(maxAlpha);
        currentHash = 31 * currentHash + Float.floatToIntBits(fadeLen);
        currentHash = 31 * currentHash + discs;
        currentHash = 31 * currentHash + (hitBlock ? 1231 : 1237);

        if (currentHash != cachedHashSlots[slot]) {
            int needed = StackedVolumeMesh.estimateFloats(discs);
            if (cachedVertsSlots[slot].length < needed) {
                cachedVertsSlots[slot] = new float[needed + 512];
            }
            cachedQuadCountSlots[slot] = StackedVolumeMesh.build(
                    cachedVertsSlots[slot], data, scanLen, density, maxAlpha, fadeLen, hitBlock, discs);
            cachedCenterCountSlots[slot] = StackedVolumeMesh.buildCenters(
                    cachedCenterSlots[slot], data, scanLen, density, maxAlpha, fadeLen, hitBlock, discs);
            cachedHashSlots[slot] = currentHash;
        }

        this.beamR[slot] = (data.color() >> 16) & 0xFF;
        this.beamG[slot] = (data.color() >> 8) & 0xFF;
        this.beamB[slot] = data.color() & 0xFF;

        float rawIntensity = Math.min(data.intensity() * TheatricalExtraLightsConfig.getVolumetricBeamBrightness(), 1.0f);
        this.beamAlphaScale[slot] = (float) Math.pow(rawIntensity, 0.45);
        this.beamScanLen[slot] = scanLen;
        this.beamMidX[slot] = (float) (data.origin().x + data.beamDir().x * scanLen * 0.5);
        this.beamMidY[slot] = (float) (data.origin().y + data.beamDir().y * scanLen * 0.5);
        this.beamMidZ[slot] = (float) (data.origin().z + data.beamDir().z * scanLen * 0.5);

        ResourceLocation gobo = data.goboTexture() != null
                ? data.goboTexture()
                : new ResourceLocation("theatricalextralights", "textures/gobos/generic_1/open.png");
        if (IrisCompat.isShadersActive()) {
            this.beamRenderTypes[slot] = ModShaders.getVolumetricFallbackRenderType(gobo);
        } else {
            this.beamRenderTypes[slot] = ModShaders.getVolumetricRenderType(gobo);
        }
        this.activeBeamCount++;
    }

    @Override
    public void render(MultiBufferSource.BufferSource bufferSource, PoseStack ps, Camera camera, float partialTick) {
        if (activeBeamCount == 0 || this.currentPos == null) {
            return;
        }

        final double camX = camera.getPosition().x;
        final double camY = camera.getPosition().y;
        final double camZ = camera.getPosition().z;
        final Vector3f look = camera.getLookVector();
        final float lx = look.x();
        final float ly = look.y();
        final float lz = look.z();

        final double blockX = this.currentPos.getX();
        final double blockY = this.currentPos.getY();
        final double blockZ = this.currentPos.getZ();

        ps.pushPose();
        Vec3 offset = Vec3.atLowerCornerOf(this.currentPos).subtract(camera.getPosition());
        ps.translate(offset.x, offset.y, offset.z);
        Matrix4f mat = ps.last().pose();

        java.util.Arrays.fill(this.processedSlots, 0, this.activeBeamCount, false);

        for (int b = 0; b < activeBeamCount; b++) {
            if (processedSlots[b]) {
                continue;
            }

            RenderType targetRenderType = beamRenderTypes[b];
            VertexConsumer vc = bufferSource.getBuffer(targetRenderType);

            for (int k = b; k < activeBeamCount; k++) {
                if (processedSlots[k] || beamRenderTypes[k] != targetRenderType) {
                    continue;
                }
                processedSlots[k] = true;

                if (!coneVisible(blockX, blockY, blockZ, k, camX, camY, camZ, lx, ly, lz)) {
                    continue;
                }

                int r = beamR[k];
                int g = beamG[k];
                int bl = beamB[k];
                float alphaScale = beamAlphaScale[k];

                StackedVolumeMesh.emitCachedQuads(vc, mat, cachedVertsSlots[k], cachedQuadCountSlots[k],
                        r, g, bl, alphaScale);
                StackedVolumeMesh.emitCameraDiscs(vc, mat, cachedCenterSlots[k], cachedCenterCountSlots[k],
                        blockX, blockY, blockZ, camX, camY, camZ, r, g, bl, alphaScale);
            }
        }

        ps.popPose();
        this.activeBeamCount = 0;
    }

    private boolean coneVisible(double blockX, double blockY, double blockZ, int slot,
                                double camX, double camY, double camZ,
                                float lx, float ly, float lz) {
        double mx = blockX + beamMidX[slot] - camX;
        double my = blockY + beamMidY[slot] - camY;
        double mz = blockZ + beamMidZ[slot] - camZ;
        double behind = mx * lx + my * ly + mz * lz;
        return behind >= -beamScanLen[slot];
    }

    @Override
    public Vec3 getPos(float partialTick) {
        return this.currentPos != null ? Vec3.atCenterOf(this.currentPos) : Vec3.ZERO;
    }
}