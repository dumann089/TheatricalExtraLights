package com.github.dumann089.theatricalextralights.client.render.beam.raymarch;

import com.github.dumann089.theatricalextralights.client.ModShaders;
import com.github.dumann089.theatricalextralights.client.render.beam.BeamRenderData;
import com.github.dumann089.theatricalextralights.client.render.beam.VolumetricBeamRenderer;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.imabad.theatrical.client.LazyRenderers;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Screen-space cone raymarcher: one immediate proxy AABB draw per beam so uniforms never collide.
 */
public class RaymarchBeamRenderer extends LazyRenderers.LazyRenderer {

    public static final int MAX_BEAMS = 128;
    private static final RaymarchBeamRenderer INSTANCE = new RaymarchBeamRenderer();
    private static final ResourceLocation OPEN_GOBO =
            new ResourceLocation("theatricalextralights", "textures/gobos/generic_1/open.png");

    private final BeamSlot[] slots = new BeamSlot[MAX_BEAMS];
    private final int[] drawOrder = new int[MAX_BEAMS];
    private int activeBeamCount = 0;

    private final Vector4f tmpVec = new Vector4f();
    private final Matrix4f invProj = new Matrix4f();
    private final Vector3f originVS = new Vector3f();
    private final Vector3f dirVS = new Vector3f();
    private final Vector3f uVS = new Vector3f();
    private final Vector3f vVS = new Vector3f();

    private RaymarchBeamRenderer() {
        for (int i = 0; i < MAX_BEAMS; i++) {
            slots[i] = new BeamSlot();
        }
    }

    /**
     * Periode de repli des coordonnees monde envoyees au bruit volumetrique. Loin du spawn,
     * une coordonnee brute epuise la precision du flottant et le bruit devient grossier ;
     * repliee, elle reste petite. Le repli porte sur l'origine du faisceau, constante pour
     * un projecteur fixe : il ne peut donc pas sauter quand le joueur se deplace.
     */
    private static final float WORLD_NOISE_PERIOD = 4096.0f;

    private static float wrapWorld(float v) {
        float w = v % WORLD_NOISE_PERIOD;
        return w < 0.0f ? w + WORLD_NOISE_PERIOD : w;
    }

    public static void submit(BeamRenderData data) {
        INSTANCE.enqueue(data);
    }

    private void enqueue(BeamRenderData data) {
        if (!TheatricalExtraLightsConfig.isVolumetricBeamEnabled() || data.intensity() <= 0.0f) {
            return;
        }
        if (!ModShaders.canUseRaymarch()) {
            return;
        }

        if (this.activeBeamCount == 0) {
            SceneDepthCopy.beginFrame();
            LazyRenderers.addLazyRender(this);
        }

        int maxBeams = Math.min(MAX_BEAMS, Math.max(1, TheatricalExtraLightsConfig.getRaymarchMaxBeamsPerFrame()));
        if (this.activeBeamCount >= maxBeams) {
            return;
        }

        float maxDist = TheatricalExtraLightsConfig.getVolumetricBeamDistance();
        boolean hitBlock = data.scanLen() < maxDist;
        float scanLen = hitBlock ? data.scanLen() + 2.5f : maxDist;
        if (scanLen <= 0.0f) {
            return;
        }

        BeamSlot s = slots[this.activeBeamCount];
        s.originX = (float) (data.fixturePos().getX() + data.origin().x);
        s.originY = (float) (data.fixturePos().getY() + data.origin().y);
        s.originZ = (float) (data.fixturePos().getZ() + data.origin().z);
        s.dirX = (float) data.beamDir().x;
        s.dirY = (float) data.beamDir().y;
        s.dirZ = (float) data.beamDir().z;
        s.uX = (float) data.axisU().x;
        s.uY = (float) data.axisU().y;
        s.uZ = (float) data.axisU().z;
        s.vX = (float) data.axisV().x;
        s.vY = (float) data.axisV().y;
        s.vZ = (float) data.axisV().z;
        s.tanHalfAngle = data.tanHalfAngle();
        s.scanLen = scanLen;
        s.baseRadius = data.baseRadius();
        s.widthScale = data.widthScale();
        s.heightScale = data.heightScale();
        s.color = data.color();
        s.intensity = data.intensity();
        s.goboTexture = data.goboTexture() != null ? data.goboTexture() : OPEN_GOBO;
        s.goboRotation = data.goboRotation();
        s.hitBlock = hitBlock;
        s.fixturePos = data.fixturePos();
        s.localOriginX = (float) data.origin().x;
        s.localOriginY = (float) data.origin().y;
        s.localOriginZ = (float) data.origin().z;
        this.activeBeamCount++;
    }

    @Override
    public void render(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Camera camera, float partialTick) {
        try {
            if (activeBeamCount == 0) {
                return;
            }

            bufferSource.endBatch();
            SceneDepthCopy.capture();
            if (!SceneDepthCopy.hasDepth() || ModShaders.beamRaymarchShader == null) {
                drawStackedFallback(bufferSource, poseStack, camera);
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            invProj.set(RenderSystem.getProjectionMatrix());
            invProj.invert();

            float time = (mc.level != null ? mc.level.getGameTime() : 0) + partialTick;
            float daylight = mc.level != null
                    ? 1.0f - Math.min(1.0f, mc.level.getSkyDarken() / 11.0f)
                    : 0.0f;
            int steps = TheatricalExtraLightsConfig.getRaymarchSteps();
            float density = TheatricalExtraLightsConfig.getVolumetricBeamDensity();
            float maxAlpha = TheatricalExtraLightsConfig.getVolumetricBeamMaxAlpha();
            float brightness = TheatricalExtraLightsConfig.getVolumetricBeamBrightness();
            float anisotropy = TheatricalExtraLightsConfig.getRaymarchAnisotropy();
            float dust = TheatricalExtraLightsConfig.getRaymarchDustAmount();
            float fadeLen = TheatricalExtraLightsConfig.getVolumetricBeamFadeLength();
            var main = mc.getMainRenderTarget();
            float screenW = main.width;
            float screenH = main.height;

            Vec3 camPos = camera.getPosition();
            Vector3f look = camera.getLookVector();

            sortFarToNear(camPos);

            poseStack.pushPose();
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
            Matrix4f viewMat = new Matrix4f(poseStack.last().pose());

            for (int n = 0; n < activeBeamCount; n++) {
                BeamSlot s = slots[drawOrder[n]];

                double midX = s.originX + s.dirX * s.scanLen * 0.5;
                double midY = s.originY + s.dirY * s.scanLen * 0.5;
                double midZ = s.originZ + s.dirZ * s.scanLen * 0.5;
                double dx = midX - camPos.x;
                double dy = midY - camPos.y;
                double dz = midZ - camPos.z;
                if (dx * look.x() + dy * look.y() + dz * look.z() < -s.scanLen) {
                    continue;
                }

                double distSq = dx * dx + dy * dy + dz * dz;
                int beamSteps = steps;
                if (distSq > 96.0 * 96.0) {
                    beamSteps = Math.max(6, steps / 3);
                } else if (distSq > 48.0 * 48.0) {
                    beamSteps = Math.max(8, steps / 2);
                }

                ShaderInstance shader = ModShaders.beamRaymarchShader;
                if (shader == null) {
                    continue;
                }

                transformPoint(viewMat, s.originX, s.originY, s.originZ, originVS);
                transformDir(viewMat, s.dirX, s.dirY, s.dirZ, dirVS);
                transformDir(viewMat, s.uX, s.uY, s.uZ, uVS);
                transformDir(viewMat, s.vX, s.vY, s.vZ, vVS);

                float endRadius = Math.max(s.baseRadius, s.scanLen * Math.max(s.tanHalfAngle, 1.0e-4f));
                endRadius *= Math.max(s.widthScale, s.heightScale);

                shader.safeGetUniform("InvProjMat").set(invProj);
                shader.safeGetUniform("BeamOrigin").set(originVS.x, originVS.y, originVS.z);
                shader.safeGetUniform("BeamDir").set(dirVS.x, dirVS.y, dirVS.z);
                shader.safeGetUniform("AxisU").set(uVS.x, uVS.y, uVS.z);
                shader.safeGetUniform("AxisV").set(vVS.x, vVS.y, vVS.z);
                // Le meme repere, mais en coordonnees monde : le shader s'en sert pour
                // reancrer la poussiere volumetrique dans l'environnement. Sans lui, la
                // marche se faisant en espace vue, la fumee resterait collee a la camera.
                shader.safeGetUniform("BeamOriginW")
                        .set(wrapWorld(s.originX), wrapWorld(s.originY), wrapWorld(s.originZ));
                shader.safeGetUniform("BeamDirW").set(s.dirX, s.dirY, s.dirZ);
                shader.safeGetUniform("AxisUW").set(s.uX, s.uY, s.uZ);
                shader.safeGetUniform("AxisVW").set(s.vX, s.vY, s.vZ);
                shader.safeGetUniform("TanHalfAngle").set(s.tanHalfAngle);
                shader.safeGetUniform("BeamLength").set(s.scanLen);
                shader.safeGetUniform("BaseRadius").set(s.baseRadius);
                shader.safeGetUniform("WidthScale").set(s.widthScale);
                shader.safeGetUniform("HeightScale").set(s.heightScale);
                float cr = ((s.color >> 16) & 0xFF) / 255f;
                float cg = ((s.color >> 8) & 0xFF) / 255f;
                float cb = (s.color & 0xFF) / 255f;
                shader.safeGetUniform("BeamColor").set(cr, cg, cb);
                shader.safeGetUniform("Intensity").set(s.intensity);
                shader.safeGetUniform("Density").set(density);
                shader.safeGetUniform("MaxAlpha").set(maxAlpha);
                shader.safeGetUniform("Brightness").set(brightness);
                shader.safeGetUniform("Anisotropy").set(anisotropy);
                shader.safeGetUniform("FadeLength").set(s.hitBlock ? 0.0f : fadeLen);
                shader.safeGetUniform("DustAmount").set(dust);
                shader.safeGetUniform("GoboRotation").set(s.goboRotation);
                shader.safeGetUniform("Time").set(time);
                shader.safeGetUniform("Ambient").set(daylight);
                shader.safeGetUniform("ScreenSize").set(screenW, screenH);
                shader.safeGetUniform("StepCount").set(beamSteps);

                int depthTex = SceneDepthCopy.getDepthTextureId();
                RenderType renderType = ModShaders.getRaymarchRenderType(s.goboTexture);
                renderType.setupRenderState();
                RenderSystem.setShader(() -> shader);
                RenderSystem.setShaderTexture(1, depthTex);
                shader.setSampler("Sampler1", depthTex);
                shader.apply();

                Tesselator tess = Tesselator.getInstance();
                BufferBuilder bb = tess.getBuilder();
                bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
                drawConeProxy(bb, viewMat, s, endRadius);
                BufferUploader.drawWithShader(bb.end());
                renderType.clearRenderState();
            }

            poseStack.popPose();
        } finally {
            this.activeBeamCount = 0;
        }
    }

    private void drawStackedFallback(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Camera camera) {
        for (int i = 0; i < activeBeamCount; i++) {
            VolumetricBeamRenderer.renderFallbackSlot(bufferSource, poseStack, camera, slots[i]);
        }
    }

    private void sortFarToNear(Vec3 camPos) {
        for (int i = 0; i < activeBeamCount; i++) {
            drawOrder[i] = i;
        }
        for (int i = 1; i < activeBeamCount; i++) {
            int key = drawOrder[i];
            double keyD = distSq(slots[key], camPos);
            int j = i - 1;
            while (j >= 0 && distSq(slots[drawOrder[j]], camPos) < keyD) {
                drawOrder[j + 1] = drawOrder[j];
                j--;
            }
            drawOrder[j + 1] = key;
        }
    }

    private static double distSq(BeamSlot s, Vec3 camPos) {
        double mx = s.originX + s.dirX * s.scanLen * 0.5 - camPos.x;
        double my = s.originY + s.dirY * s.scanLen * 0.5 - camPos.y;
        double mz = s.originZ + s.dirZ * s.scanLen * 0.5 - camPos.z;
        return mx * mx + my * my + mz * mz;
    }

    private void transformPoint(Matrix4f mat, float x, float y, float z, Vector3f out) {
        tmpVec.set(x, y, z, 1f);
        mat.transform(tmpVec);
        out.set(tmpVec.x, tmpVec.y, tmpVec.z);
    }

    private void transformDir(Matrix4f mat, float x, float y, float z, Vector3f out) {
        tmpVec.set(x, y, z, 0f);
        mat.transform(tmpVec);
        out.set(tmpVec.x, tmpVec.y, tmpVec.z);
        if (out.lengthSquared() > 1.0e-12f) {
            out.normalize();
        }
    }

    private static void drawConeProxy(BufferBuilder vc, Matrix4f mat, BeamSlot s, float endRadius) {
        float startR = Math.max(s.baseRadius, 0.05f) * Math.max(s.widthScale, s.heightScale);
        float pad = Math.max(startR, endRadius) * 1.15f + 0.25f;

        float ox = s.originX;
        float oy = s.originY;
        float oz = s.originZ;
        float ex = ox + s.dirX * s.scanLen;
        float ey = oy + s.dirY * s.scanLen;
        float ez = oz + s.dirZ * s.scanLen;

        float minX = Math.min(ox, ex) - pad;
        float minY = Math.min(oy, ey) - pad;
        float minZ = Math.min(oz, ez) - pad;
        float maxX = Math.max(ox, ex) + pad;
        float maxY = Math.max(oy, ey) + pad;
        float maxZ = Math.max(oz, ez) + pad;

        emitQuad(vc, mat, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, minY, maxZ, minX, minY, maxZ);
        emitQuad(vc, mat, maxX, maxY, minZ, minX, maxY, minZ, minX, minY, minZ, maxX, minY, minZ);
        emitQuad(vc, mat, maxX, maxY, maxZ, maxX, maxY, minZ, maxX, minY, minZ, maxX, minY, maxZ);
        emitQuad(vc, mat, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ, minX, minY, minZ);
        emitQuad(vc, mat, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ);
        emitQuad(vc, mat, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ);
    }

    private static void emitQuad(BufferBuilder vc, Matrix4f mat,
                                 float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3) {
        vc.vertex(mat, x0, y0, z0).color(255, 255, 255, 255).uv(0f, 0f).endVertex();
        vc.vertex(mat, x1, y1, z1).color(255, 255, 255, 255).uv(1f, 0f).endVertex();
        vc.vertex(mat, x2, y2, z2).color(255, 255, 255, 255).uv(1f, 1f).endVertex();
        vc.vertex(mat, x3, y3, z3).color(255, 255, 255, 255).uv(0f, 1f).endVertex();
    }

    @Override
    public Vec3 getPos(float partialTick) {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    }

    public static final class BeamSlot {
        public float originX, originY, originZ;
        public float dirX, dirY, dirZ;
        public float uX, uY, uZ;
        public float vX, vY, vZ;
        public float tanHalfAngle;
        public float scanLen;
        public float baseRadius;
        public float widthScale;
        public float heightScale;
        public int color;
        public float intensity;
        public ResourceLocation goboTexture;
        public float goboRotation;
        public boolean hitBlock;
        public net.minecraft.core.BlockPos fixturePos;
        public float localOriginX, localOriginY, localOriginZ;
    }
}
