package com.github.dumann089.theatricalextralights.client.render.beam;

import com.github.dumann089.theatricalextralights.client.IrisCompat;
import com.github.dumann089.theatricalextralights.client.ModShaders;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.imabad.theatrical.client.LazyRenderers;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class VolumetricBeamRenderer extends LazyRenderers.LazyRenderer {

    private static final int MAX_BEAMS_PER_FIXTURE = 32;
    private static final int SLICE_MULTIPLIER = 4;

    private final float[][] cachedVertsSlots = new float[MAX_BEAMS_PER_FIXTURE][16384];
    private final int[] cachedQuadCountSlots = new int[MAX_BEAMS_PER_FIXTURE];
    private final int[] cachedHashSlots = new int[MAX_BEAMS_PER_FIXTURE];

    private final int[] beamR = new int[MAX_BEAMS_PER_FIXTURE];
    private final int[] beamG = new int[MAX_BEAMS_PER_FIXTURE];
    private final int[] beamB = new int[MAX_BEAMS_PER_FIXTURE];
    private final float[] beamAlphaScale = new float[MAX_BEAMS_PER_FIXTURE];
    private final float[] wheelProgressSlots = new float[MAX_BEAMS_PER_FIXTURE];
    private final RenderType[] beamRenderTypes = new RenderType[MAX_BEAMS_PER_FIXTURE];

    private int activeBeamCount = 0;
    private BlockPos currentPos;

    public void render(BeamRenderData data, PoseStack poseStack) {
        if (!TheatricalExtraLightsConfig.isVolumetricBeamEnabled() || data.intensity() <= 0.0f) {
            return;
        }

        if (activeBeamCount == 0) {
            currentPos = data.fixturePos();
            LazyRenderers.addLazyRender(this);
        }

        int slot = activeBeamCount;
        if (slot >= MAX_BEAMS_PER_FIXTURE) {
            return;
        }

        int baseSlices = TheatricalExtraLightsConfig.getVolumetricBeamSlices();
        float maxDist = TheatricalExtraLightsConfig.getVolumetricBeamDistance();

        if (baseSlices < 2 || maxDist <= 0.0f) {
            return;
        }

        boolean hitBlock = data.scanLen() < maxDist;
        float scanLen = Math.min(hitBlock ? data.scanLen() + 2.5f : maxDist, maxDist);

        if (scanLen <= 0.001f) {
            return;
        }

        float slicesPerMeter = (float) baseSlices / maxDist;
        int dynamicSlices = Math.max(32, Math.min(baseSlices, Math.round(scanLen * slicesPerMeter)));

        float density = TheatricalExtraLightsConfig.getVolumetricBeamDensity();
        float maxAlpha = TheatricalExtraLightsConfig.getVolumetricBeamMaxAlpha();
        float fadeLen = TheatricalExtraLightsConfig.getVolumetricBeamFadeLength();

        int currentHash = 1;
        currentHash = 31 * currentHash + data.generateStateHash(dynamicSlices);
        currentHash = 31 * currentHash + Float.floatToIntBits(scanLen);
        currentHash = 31 * currentHash + Float.floatToIntBits(density);
        currentHash = 31 * currentHash + Float.floatToIntBits(maxAlpha);
        currentHash = 31 * currentHash + Float.floatToIntBits(fadeLen);
        currentHash = 31 * currentHash + Float.floatToIntBits(TheatricalExtraLightsConfig.getVolumetricBeamBrightness());
        currentHash = 31 * currentHash + dynamicSlices;
        currentHash = 31 * currentHash + (hitBlock ? 1231 : 1237);

        if (currentHash != cachedHashSlots[slot]) {
            rebuildGeometry(slot, data, dynamicSlices, scanLen, density, maxAlpha, fadeLen, hitBlock);
            cachedHashSlots[slot] = currentHash;
        }

        beamR[slot] = (data.color() >> 16) & 0xFF;
        beamG[slot] = (data.color() >> 8) & 0xFF;
        beamB[slot] = data.color() & 0xFF;

        float rawIntensity = Math.min(data.intensity() * TheatricalExtraLightsConfig.getVolumetricBeamBrightness(), 1.0f);
        beamAlphaScale[slot] = (float) Math.sqrt(rawIntensity);
        wheelProgressSlots[slot] = data.wheelProgress();

        if (IrisCompat.isShadersActive()) {
            beamRenderTypes[slot] = ModShaders.getDualVolumetricFallbackRenderType(data.tex0(), data.tex1());
        } else {
            beamRenderTypes[slot] = ModShaders.getDualVolumetricRenderType(data.tex0(), data.tex1());
        }

        activeBeamCount++;
    }

    @Override
    public void render(MultiBufferSource.BufferSource bufferSource, PoseStack ps, Camera camera, float partialTick) {
        if (activeBeamCount <= 0) {
            return;
        }

        final double camX = camera.getPosition().x;
        final double camY = camera.getPosition().y;
        final double camZ = camera.getPosition().z;

        final org.joml.Vector3f cameraLook = camera.getLookVector();
        final double lx = cameraLook.x();
        final double ly = cameraLook.y();
        final double lz = cameraLook.z();

        final double blockX = currentPos.getX();
        final double blockY = currentPos.getY();
        final double blockZ = currentPos.getZ();

        ps.pushPose();

        Vec3 offset = Vec3.atLowerCornerOf(currentPos).subtract(camera.getPosition());
        ps.translate(offset.x, offset.y, offset.z);

        Matrix4f mat = ps.last().pose();

        for (int b = 0; b < activeBeamCount; b++) {
            RenderType renderType = beamRenderTypes[b];
            if (renderType == null) {
                continue;
            }

            VertexConsumer vc = bufferSource.getBuffer(renderType);

            int quadCount = cachedQuadCountSlots[b];
            if (quadCount <= 0) {
                continue;
            }

            float[] verts = cachedVertsSlots[b];
            int r = beamR[b];
            int g = beamG[b];
            int bl = beamB[b];
            float alphaScale = beamAlphaScale[b];

            float progress = wheelProgressSlots[b];
            float totalWidth = 1.15f; // 1.0 ancho de textura + 0.15 gap mecánico

            int lastOffset = (quadCount - 1) * 24;

            double sx = blockX + verts[0] - camX;
            double sy = blockY + verts[1] - camY;
            double sz = blockZ + verts[2] - camZ;
            double sDot = sx * lx + sy * ly + sz * lz;
            double sDistSq = sx * sx + sy * sy + sz * sz;
            boolean startVisible = (sDot >= -4.0 && sDot < 0.0) || (sDot >= 0.0 && sDot * sDot >= sDistSq * 0.05);

            double ex = blockX + verts[lastOffset] - camX;
            double ey = blockY + verts[lastOffset + 1] - camY;
            double ez = blockZ + verts[lastOffset + 2] - camZ;
            double eDot = ex * lx + ey * ly + ez * lz;
            double eDistSq = ex * ex + ey * ey + ez * ez;
            boolean endVisible = (eDot >= -4.0 && eDot < 0.0) || (eDot >= 0.0 && eDot * eDot >= eDistSq * 0.05);

            if (!startVisible && !endVisible) {
                continue;
            }

            for (int i = 0; i < quadCount; i++) {
                int offsetVert = i * 24;

                double sliceX = blockX + verts[offsetVert] - camX;
                double sliceY = blockY + verts[offsetVert + 1] - camY;
                double sliceZ = blockZ + verts[offsetVert + 2] - camZ;

                if (sliceX * lx + sliceY * ly + sliceZ * lz < -0.4) {
                    continue;
                }

                double dSq = sliceX * sliceX + sliceY * sliceY + sliceZ * sliceZ;
                float proximityFactor = 1.0f;

                if (dSq < 25.0) {
                    if (dSq <= 2.25) {
                        continue;
                    }
                    proximityFactor = (float) ((Math.sqrt(dSq) - 1.5) / 3.5);
                    proximityFactor = Math.max(0.0f, Math.min(1.0f, proximityFactor));
                }

                float baseAlpha = verts[offsetVert + 5];
                float finalAlpha = baseAlpha * proximityFactor * alphaScale;

                if (finalAlpha <= 0.001f) {
                    continue;
                }

                int alphaInt = Math.min(255, Math.max(0, (int) (finalAlpha * 255.0f)));

                for (int v = 0; v < 4; v++) {
                    int vOffset = offsetVert + v * 6;

                    float baseU = verts[vOffset + 3];
                    float baseV = verts[vOffset + 4];

                    float finalU = baseU + (progress * totalWidth);

                    vc.vertex(mat, verts[vOffset], verts[vOffset + 1], verts[vOffset + 2])
                            .color(r, g, bl, alphaInt)
                            .uv(finalU, baseV)
                            .endVertex();
                }
            }

            bufferSource.endBatch(renderType);
        }

        ps.popPose();
        activeBeamCount = 0;
    }

    @Override
    public Vec3 getPos(float partialTick) {
        return currentPos != null ? Vec3.atCenterOf(currentPos) : Vec3.ZERO;
    }

    private void rebuildGeometry(
            int slot,
            BeamRenderData data,
            int slices,
            float scanLen,
            float density,
            float maxAlpha,
            float fadeLen,
            boolean hitBlock
    ) {
        int totalSlices = slices * SLICE_MULTIPLIER;
        int totalSegments = totalSlices - 1;
        int requiredSize = totalSegments * 24;

        if (cachedVertsSlots[slot].length < requiredSize) {
            cachedVertsSlots[slot] = new float[requiredSize + 512];
        }

        cachedQuadCountSlots[slot] = 0;

        double ox = data.origin().x;
        double oy = data.origin().y;
        double oz = data.origin().z;

        double bx = data.beamDir().x;
        double by = data.beamDir().y;
        double bz = data.beamDir().z;

        double rad = Math.toRadians(data.goboRotation());

        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double ux = data.axisU().x * cos - data.axisV().x * sin;
        double uy = data.axisU().y * cos - data.axisV().y * sin;
        double uz = data.axisU().z * cos - data.axisV().z * sin;

        double vx = data.axisU().x * sin + data.axisV().x * cos;
        double vy = data.axisU().y * sin + data.axisV().y * cos;
        double vz = data.axisU().z * sin + data.axisV().z * cos;

        float[] verts = cachedVertsSlots[slot];
        int idx = 0;

        float originalStep = scanLen / Math.max(1, slices - 1);
        float uniformStep = scanLen / Math.max(1, totalSlices - 1);
        float alphaFactor = uniformStep / originalStep;

        for (int i = 0; i < totalSegments; i++) {
            float tCurr = (float) i / (totalSlices - 1);
            float distCurr = tCurr * scanLen;

            float tNext = (float) (i + 1) / (totalSlices - 1);
            float distNext = tNext * scanLen;

            double cxC = ox + bx * distCurr;
            double cyC = oy + by * distCurr;
            double czC = oz + bz * distCurr;

            float sliceAlphaC = (float) Math.exp(-(distCurr / scanLen) * density) * maxAlpha * alphaFactor;

            if (fadeLen > 0.0f && !hitBlock) {
                float distanceLeft = scanLen - distCurr;
                if (distanceLeft < fadeLen) {
                    float fadeRatio = Math.max(0.0f, Math.min(1.0f, distanceLeft / fadeLen));
                    sliceAlphaC *= fadeRatio * fadeRatio * fadeRatio;
                }
            }

            float radiusC = data.tanHalfAngle() < 0.001f
                    ? data.baseRadius()
                    : Math.max(data.baseRadius(), distCurr * data.tanHalfAngle());

            float radiusWC = radiusC * data.widthScale();
            float radiusHC = radiusC * data.heightScale();

            double cxN = ox + bx * distNext;
            double cyN = oy + by * distNext;
            double czN = oz + bz * distNext;

            float sliceAlphaN = (float) Math.exp(-(distNext / scanLen) * density) * maxAlpha * alphaFactor;

            if (fadeLen > 0.0f && !hitBlock) {
                float distanceLeft = scanLen - distNext;
                if (distanceLeft < fadeLen) {
                    float fadeRatio = Math.max(0.0f, Math.min(1.0f, distanceLeft / fadeLen));
                    sliceAlphaN *= fadeRatio * fadeRatio * fadeRatio;
                }
            }

            float radiusN = data.tanHalfAngle() < 0.001f
                    ? data.baseRadius()
                    : Math.max(data.baseRadius(), distNext * data.tanHalfAngle());

            float radiusWN = radiusN * data.widthScale();
            float radiusHN = radiusN * data.heightScale();

            if (sliceAlphaC <= 0.001f && sliceAlphaN <= 0.001f) {
                continue;
            }

            // V0 (UV constante: 0.0, 0.0)
            verts[idx++] = (float) (cxC - ux * radiusWC + vx * radiusHC);
            verts[idx++] = (float) (cyC - uy * radiusWC + vy * radiusHC);
            verts[idx++] = (float) (czC - uz * radiusWC + vz * radiusHC);
            verts[idx++] = 0.0f;
            verts[idx++] = 0.0f;
            verts[idx++] = sliceAlphaC;

            // V1 (UV constante: 1.0, 0.0)
            verts[idx++] = (float) (cxC + ux * radiusWC + vx * radiusHC);
            verts[idx++] = (float) (cyC + uy * radiusWC + vy * radiusHC);
            verts[idx++] = (float) (czC + uz * radiusWC + vz * radiusHC);
            verts[idx++] = 1.0f;
            verts[idx++] = 0.0f;
            verts[idx++] = sliceAlphaC;

            // V2 (UV constante: 1.0, 1.0)
            verts[idx++] = (float) (cxN + ux * radiusWN - vx * radiusHN);
            verts[idx++] = (float) (cyN + uy * radiusWN - vy * radiusHN);
            verts[idx++] = (float) (czN + uz * radiusWN - vz * radiusHN);
            verts[idx++] = 1.0f;
            verts[idx++] = 1.0f;
            verts[idx++] = sliceAlphaN;

            // V3 (UV constante: 0.0, 1.0)
            verts[idx++] = (float) (cxN - ux * radiusWN - vx * radiusHN);
            verts[idx++] = (float) (cyN - uy * radiusWN - vy * radiusHN);
            verts[idx++] = (float) (czN - uz * radiusWN - vz * radiusHN);
            verts[idx++] = 0.0f;
            verts[idx++] = 1.0f;
            verts[idx++] = sliceAlphaN;

            cachedQuadCountSlots[slot]++;
        }
    }
}