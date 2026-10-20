package com.github.dumann089.theatricalextralights.client.render.beam.raymarch;

import com.github.dumann089.theatricalextralights.client.ModShaders;
import com.github.dumann089.theatricalextralights.client.render.beam.BeamRenderData;
import com.github.dumann089.theatricalextralights.client.render.beam.FramingShutterRender;
import com.github.dumann089.theatricalextralights.client.render.beam.VolumetricBeamRenderer;
import com.github.dumann089.theatricalextralights.client.render.beam.shadow.BeamShadowOccluders;
import com.github.dumann089.theatricalextralights.util.FramingShutterState;
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

public class RaymarchBeamRenderer extends LazyRenderers.LazyRenderer {

    public static final int MAX_BEAMS = 512;
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

    // --- otimizacoes (sem efeito visual) ---------------------------------------------------
    /** Lados do poligono do volume proxy. Par, para poder triangular as tampas em quads. */
    private static final int HULL_N = 12;
    /** Fator para o poligono circunscrever (e nao so inscrever) a elipse da secao do cone. */
    private static final float HULL_CIRC = (float) (1.0 / Math.cos(Math.PI / HULL_N));
    private static final float[] HULL_COS = new float[HULL_N];
    private static final float[] HULL_SIN = new float[HULL_N];
    static {
        for (int i = 0; i < HULL_N; i++) {
            double a = Math.PI * 2.0 * i / HULL_N;
            HULL_COS[i] = (float) Math.cos(a);
            HULL_SIN[i] = (float) Math.sin(a);
        }
    }
    private static final String[] OCC_MIN = new String[BeamShadowOccluders.MAX_BOXES];
    private static final String[] OCC_MAX = new String[BeamShadowOccluders.MAX_BOXES];
    static {
        for (int i = 0; i < BeamShadowOccluders.MAX_BOXES; i++) {
            OCC_MIN[i] = "OccMin" + i;
            OCC_MAX[i] = "OccMax" + i;
        }
    }
    /** Vertices do proxy em espaco de view: 2 aneis de HULL_N pontos (xyz). */
    private final float[] hullView = new float[2 * HULL_N * 3];
    private final double[] sortDist = new double[MAX_BEAMS];
    private final Matrix4f projMat = new Matrix4f();

    private RaymarchBeamRenderer() {
        for (int i = 0; i < MAX_BEAMS; i++) {
            slots[i] = new BeamSlot();
        }
    }

    public static void submit(BeamRenderData data) {
        INSTANCE.enqueue(data);
    }

    /**
     * Faisceau dont la couleur varie le long de sa largeur : {@code colorRampTexture} est une
     * texture GL d'une ligne, lue par le shader selon la position en U (nappes des barres LED).
     */
    public static void submit(BeamRenderData data, int colorRampTexture) {
        submit(data, colorRampTexture, 0f);
    }

    /**
     * @param maxLength longueur voulue du faisceau (reglage de l'appareil), 0 = jusqu'a l'obstacle.
     *                  Plus courte que l'obstacle, le faisceau s'eteint en fondu a cette longueur au
     *                  lieu de s'arreter net contre un bloc.
     */
    public static void submit(BeamRenderData data, int colorRampTexture, float maxLength) {
        BeamSlot slot = INSTANCE.enqueue(data);
        if (slot == null) {
            return;
        }
        slot.colorRampTexture = colorRampTexture;
        if (maxLength > 0f && data.scanLen() > maxLength) {
            slot.scanLen = maxLength;
            slot.hitBlock = false;
            slot.fadeLength = Math.max(0.5f, maxLength * 0.45f);
        }
    }

    private BeamSlot enqueue(BeamRenderData data) {
        if (!TheatricalExtraLightsConfig.isVolumetricBeamEnabled() || data.intensity() <= 0.0f) {
            return null;
        }
        if (!ModShaders.canUseRaymarch()) {
            return null;
        }

        if (this.activeBeamCount == 0) {
            SceneDepthCopy.beginFrame();
            LazyRenderers.addLazyRender(this);
        }

        int maxBeams = Math.min(MAX_BEAMS, Math.max(1, TheatricalExtraLightsConfig.getRaymarchMaxBeamsPerFrame()));
        if (this.activeBeamCount >= maxBeams) {
            return null;
        }

        float maxDist = TheatricalExtraLightsConfig.getVolumetricBeamDistance();
        boolean hitBlock = data.scanLen() < maxDist;
        float scanLen = hitBlock ? data.scanLen() + 2.5f : maxDist;
        if (scanLen <= 0.0f) {
            return null;
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
        s.nextGoboTexture = data.nextGoboTexture() != null ? data.nextGoboTexture() : OPEN_GOBO;
        s.goboRotation = data.goboRotation();
        s.wheelTransition = data.wheelTransition();
        s.shutters = data.hasShutters() ? data.shutters() : null;
        s.animation = data.hasAnimation() ? data.animation() : null;
        s.hitBlock = hitBlock;
        s.fixturePos = data.fixturePos();
        s.localOriginX = (float) data.origin().x;
        s.localOriginY = (float) data.origin().y;
        s.localOriginZ = (float) data.origin().z;
        s.colorRampTexture = 0;
        s.fadeLength = -1f;
        this.activeBeamCount++;
        return s;
    }

    @Override
    public void render(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Camera camera, float partialTick) {
        try {
            if (activeBeamCount == 0) {
                return;
            }

            SceneDepthCopy.flushOpaqueAndCapture(bufferSource);
            bufferSource.endBatch();
            if (!SceneDepthCopy.hasDepth() || ModShaders.beamRaymarchShader == null) {
                drawStackedFallback(bufferSource, poseStack, camera);
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            projMat.set(RenderSystem.getProjectionMatrix());
            invProj.set(projMat);
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

                // Volume proxy justo ao cone. Se os eixos nao forem ortonormais, cai na caixa antiga.
                boolean hullOk = computeHull(s, viewMat);
                // Feixe inteiramente fora do frustum: a GPU descartaria tudo mesmo, entao nem
                // gastamos CPU com uniforms, sombras e estado.
                if (hullOk && hullOutsideFrustum()) {
                    continue;
                }

                int beamSteps = lodStepCount(steps, s, camPos);

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
                // Repere monde : la haze est fixe dans la salle, c'est le faisceau qui la traverse.
                shader.safeGetUniform("BeamOriginW").set((float) s.originX, (float) s.originY, (float) s.originZ);
                shader.safeGetUniform("BeamDirW").set((float) s.dirX, (float) s.dirY, (float) s.dirZ);
                shader.safeGetUniform("AxisUW").set((float) s.uX, (float) s.uY, (float) s.uZ);
                shader.safeGetUniform("AxisVW").set((float) s.vX, (float) s.vY, (float) s.vZ);
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
                shader.safeGetUniform("FadeLength").set(s.hitBlock ? 0.0f : (s.fadeLength >= 0f ? s.fadeLength : fadeLen));
                shader.safeGetUniform("DustAmount").set(dust);
                shader.safeGetUniform("GoboRotation").set(s.goboRotation);
                shader.safeGetUniform("ColorRamp").set(s.colorRampTexture != 0 ? 1.0f : 0.0f);
                shader.safeGetUniform("WheelTransition").set(s.wheelTransition);
                FramingShutterRender.applyUniforms(shader, s.shutters);
                applyAnimationUniforms(shader, s.animation);
                BeamShadowOccluders.Snapshot shadows = null;
                if (TheatricalExtraLightsConfig.isBeamShadowsEnabled() && mc.level != null) {
                    shadows = BeamShadowOccluders.get(mc.level, s.fixturePos,
                            new Vec3(s.originX, s.originY, s.originZ), new Vec3(s.dirX, s.dirY, s.dirZ),
                            s.scanLen, endRadius);
                }
                // Grade sem nenhum bloco solido e sem entidades: o shader leria zero em todas as
                // amostras e devolveria 1.0 (sem sombra). ShadowEnabled=0 devolve o mesmo 1.0, mas
                // sem as ate 32 leituras de textura por passo do raymarch.
                if (shadows != null && !shadows.solidAny() && shadows.boxes().isEmpty()) {
                    shadows = null;
                }
                applyShadowUniforms(shader, shadows);
                shader.safeGetUniform("Time").set(time);
                shader.safeGetUniform("Ambient").set(daylight);
                shader.safeGetUniform("ScreenSize").set(screenW, screenH);
                shader.safeGetUniform("StepCount").set(beamSteps);

                int depthTex = SceneDepthCopy.getDepthTextureId();

                RenderType renderType = ModShaders.getRaymarchRenderType(s.goboTexture);
                renderType.setupRenderState();

                RenderSystem.setShader(() -> shader);

                RenderSystem.setShaderTexture(0, s.goboTexture);
                int goboTex = RenderSystem.getShaderTexture(0);
                shader.setSampler("Sampler0", goboTex);

                RenderSystem.setShaderTexture(1, depthTex);
                shader.setSampler("Sampler1", depthTex);

                RenderSystem.setShaderTexture(2, s.nextGoboTexture);
                int nextGoboTex = RenderSystem.getShaderTexture(2);
                shader.setSampler("Sampler2", nextGoboTex);

                RenderSystem.setShaderTexture(3, s.animation != null ? s.animation.texture() : OPEN_GOBO);
                shader.setSampler("Sampler3", RenderSystem.getShaderTexture(3));

                if (shadows != null) {
                    RenderSystem.setShaderTexture(4, shadows.textureId());
                    shader.setSampler("Sampler4", shadows.textureId());
                } else {
                    RenderSystem.setShaderTexture(4, OPEN_GOBO);
                    shader.setSampler("Sampler4", RenderSystem.getShaderTexture(4));
                }

                // Rampe de couleur des barres LED ; sinon la texture du gobo, pour un sampler valide.
                int rampTex = s.colorRampTexture != 0 ? s.colorRampTexture : goboTex;
                RenderSystem.setShaderTexture(5, rampTex);
                shader.setSampler("Sampler5", rampTex);

                shader.apply();

                Tesselator tess = Tesselator.getInstance();
                BufferBuilder bb = tess.getBuilder();
                bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
                if (hullOk) {
                    emitHull(bb);
                } else {
                    drawConeProxy(bb, viewMat, s, endRadius);
                }
                BufferUploader.drawWithShader(bb.end());
                renderType.clearRenderState();
            }

            poseStack.popPose();
        } finally {
            this.activeBeamCount = 0;
        }
    }

    /** Uniforms de la roue d'animation : {@code AnimEnabled}, {@code AnimAngle}, {@code AnimOffset}. */
    public static void applyAnimationUniforms(ShaderInstance shader, BeamRenderData.Animation anim) {
        if (anim == null) {
            shader.safeGetUniform("AnimEnabled").set(0.0f);
            shader.safeGetUniform("AnimAngle").set(0.0f);
            shader.safeGetUniform("AnimOffset").set(0.0f);
            return;
        }
        shader.safeGetUniform("AnimEnabled").set(1.0f);
        shader.safeGetUniform("AnimAngle").set((float) Math.toRadians(anim.angleDeg()));
        shader.safeGetUniform("AnimOffset").set(anim.offset());
    }

    /** Uniforms des ombres : grille de blocs et boites d'entites, ou desactive si {@code snap} est null. */
    public static void applyShadowUniforms(ShaderInstance shader, BeamShadowOccluders.Snapshot snap) {
        if (snap == null) {
            shader.safeGetUniform("ShadowEnabled").set(0.0f);
            shader.safeGetUniform("OccCount").set(0.0f);
            return;
        }
        shader.safeGetUniform("ShadowEnabled").set(1.0f);
        shader.safeGetUniform("SolidAny").set(snap.solidAny() ? 1.0f : 0.0f);
        shader.safeGetUniform("SolidMin").set((float) snap.solidMin().x, (float) snap.solidMin().y, (float) snap.solidMin().z);
        shader.safeGetUniform("SolidMax").set((float) snap.solidMax().x, (float) snap.solidMax().y, (float) snap.solidMax().z);
        shader.safeGetUniform("VoxelOrigin").set((float) snap.gridOrigin().x, (float) snap.gridOrigin().y, (float) snap.gridOrigin().z);
        shader.safeGetUniform("VoxelCell").set(snap.cell());
        shader.safeGetUniform("VoxelSize").set((float) snap.size());
        int n = Math.min(BeamShadowOccluders.MAX_BOXES, snap.boxes().size());
        shader.safeGetUniform("OccCount").set((float) n);
        for (int i = 0; i < n; i++) {
            var b = snap.boxes().get(i);
            shader.safeGetUniform(OCC_MIN[i]).set((float) b.minX, (float) b.minY, (float) b.minZ);
            shader.safeGetUniform(OCC_MAX[i]).set((float) b.maxX, (float) b.maxY, (float) b.maxZ);
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
            sortDist[i] = distSq(slots[i], camPos);
        }
        for (int i = 1; i < activeBeamCount; i++) {
            int key = drawOrder[i];
            double keyD = sortDist[key];
            int j = i - 1;
            while (j >= 0 && sortDist[drawOrder[j]] < keyD) {
                drawOrder[j + 1] = drawOrder[j];
                j--;
            }
            drawOrder[j + 1] = key;
        }
    }

    /**
     * Quality setting, then distance LOD. Close to the cone the fullscreen pass
     * covers most pixels so samples drop; far beams drop too because they are thin.
     */
    private static int lodStepCount(int qualitySteps, BeamSlot s, Vec3 camPos) {
        double ox = camPos.x - (double) s.originX;
        double oy = camPos.y - (double) s.originY;
        double oz = camPos.z - (double) s.originZ;
        double along = ox * (double) s.dirX + oy * (double) s.dirY + oz * (double) s.dirZ;
        double t = Math.max(0.0, Math.min((double) s.scanLen, along));
        double px = s.originX + s.dirX * t - camPos.x;
        double py = s.originY + s.dirY * t - camPos.y;
        double pz = s.originZ + s.dirZ * t - camPos.z;
        double radial = Math.sqrt(px * px + py * py + pz * pz);
        float scale = Math.max(s.widthScale, s.heightScale);
        double radius = Math.max((double) s.baseRadius, t * (double) Math.max(s.tanHalfAngle, 1.0e-4f)) * (double) scale;
        double distToVolume = Math.max(0.0, radial - radius);

        int beamSteps = qualitySteps;
        // Une nappe de barre (rampe de couleur) n'est marchee que sur son epaisseur : la marche est
        // courte quelle que soit la distance, et la reduction de proximite ne faisait que degrader
        // la haze quand on s'approche.
        if (s.colorRampTexture == 0) {
            if (distToVolume < 2.0) {
                beamSteps = Math.max(4, qualitySteps / 4);
            } else if (distToVolume < 8.0) {
                beamSteps = Math.max(4, (qualitySteps * 2) / 5);
            }
        }

        double mx = s.originX + s.dirX * s.scanLen * 0.5 - camPos.x;
        double my = s.originY + s.dirY * s.scanLen * 0.5 - camPos.y;
        double mz = s.originZ + s.dirZ * s.scanLen * 0.5 - camPos.z;
        double distSq = mx * mx + my * my + mz * mz;
        if (distSq > 96.0 * 96.0) {
            beamSteps = Math.min(beamSteps, Math.max(6, qualitySteps / 3));
        } else if (distSq > 48.0 * 48.0) {
            beamSteps = Math.min(beamSteps, Math.max(8, qualitySteps / 2));
        }
        return beamSteps;
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

    /**
     * Calcula, em espaco de view, um tronco convexo de {@link #HULL_N} lados que contem toda a
     * regiao onde o shader pode produzir cor: zDist em [0, scanLen] e raio ate
     * max(baseRadius, z * tan) * escala (elipse U/V). Rasterizar so este volume, em vez da caixa
     * alinhada aos eixos do mundo, poupa o fragment shader de rodar em milhoes de pixels que ele
     * descartaria. O shader nao muda, entao os pixels cobertos saem identicos.
     *
     * @return false se a base U/V/dir nao for ortonormal (usa-se entao a caixa antiga)
     */
    private boolean computeHull(BeamSlot s, Matrix4f viewMat) {
        float dl = (float) Math.sqrt(s.dirX * s.dirX + s.dirY * s.dirY + s.dirZ * s.dirZ);
        float ul = (float) Math.sqrt(s.uX * s.uX + s.uY * s.uY + s.uZ * s.uZ);
        float vl = (float) Math.sqrt(s.vX * s.vX + s.vY * s.vY + s.vZ * s.vZ);
        if (dl < 1.0e-4f || ul < 1.0e-4f || vl < 1.0e-4f) {
            return false;
        }
        float dx = s.dirX / dl, dy = s.dirY / dl, dz = s.dirZ / dl;
        float ux = s.uX / ul, uy = s.uY / ul, uz = s.uZ / ul;
        float vx = s.vX / vl, vy = s.vY / vl, vz = s.vZ / vl;
        if (Math.abs(ux * dx + uy * dy + uz * dz) > 0.02f
                || Math.abs(vx * dx + vy * dy + vz * dz) > 0.02f
                || Math.abs(ux * vx + uy * vy + uz * vz) > 0.02f) {
            return false;
        }

        float k = Math.max(s.tanHalfAngle, 1.0e-5f);
        float r0 = Math.max(s.baseRadius, 0.0f);
        float wS = Math.max(s.widthScale, 0.05f);
        float hS = Math.max(s.heightScale, 0.05f);
        float pad = 0.1f;

        for (int ring = 0; ring < 2; ring++) {
            float z = ring == 0 ? -pad : s.scanLen + pad;
            // Reta r0 + k*z deslocada por pad: majora max(baseRadius, z*k) em toda a faixa.
            float r = Math.max(r0 + k * z, 0.0f) + pad;
            float cx = s.originX + dx * z;
            float cy = s.originY + dy * z;
            float cz = s.originZ + dz * z;
            for (int i = 0; i < HULL_N; i++) {
                float a = r * wS * HULL_CIRC * HULL_COS[i];
                float b = r * hS * HULL_CIRC * HULL_SIN[i];
                tmpVec.set(cx + ux * a + vx * b, cy + uy * a + vy * b, cz + uz * a + vz * b, 1.0f);
                viewMat.transform(tmpVec);
                int o = (ring * HULL_N + i) * 3;
                hullView[o] = tmpVec.x;
                hullView[o + 1] = tmpVec.y;
                hullView[o + 2] = tmpVec.z;
            }
        }
        return true;
    }

    /** true se todos os vertices do tronco estao do lado de fora de um mesmo plano do frustum. */
    private boolean hullOutsideFrustum() {
        int allOut = 0x3F;
        for (int i = 0; i < 2 * HULL_N; i++) {
            tmpVec.set(hullView[i * 3], hullView[i * 3 + 1], hullView[i * 3 + 2], 1.0f);
            projMat.transform(tmpVec);
            float w = tmpVec.w;
            int bits = 0;
            if (tmpVec.x < -w) bits |= 1;
            if (tmpVec.x > w) bits |= 2;
            if (tmpVec.y < -w) bits |= 4;
            if (tmpVec.y > w) bits |= 8;
            if (tmpVec.z < -w) bits |= 16;
            if (tmpVec.z > w) bits |= 32;
            allOut &= bits;
            if (allOut == 0) {
                return false;
            }
        }
        return allOut != 0;
    }

    /**
     * Emite o tronco com as faces voltadas para dentro (so as faces de tras sao rasterizadas,
     * como na caixa antiga), de modo que cada pixel dentro da silhueta recebe um unico fragmento,
     * inclusive com a camera dentro do volume.
     */
    private void emitHull(BufferBuilder vc) {
        float[] h = hullView;
        float gx = 0, gy = 0, gz = 0;
        int total = 2 * HULL_N;
        for (int i = 0; i < total; i++) {
            gx += h[i * 3];
            gy += h[i * 3 + 1];
            gz += h[i * 3 + 2];
        }
        gx /= total;
        gy /= total;
        gz /= total;

        // Laterais.
        for (int i = 0; i < HULL_N; i++) {
            int j = (i + 1) % HULL_N;
            emitFaceInward(vc, gx, gy, gz, i, j, HULL_N + j, HULL_N + i);
        }
        // Tampas (leque de quads: v0, v(2m-1), v(2m), v(2m+1)).
        for (int m = 1; m + 2 < HULL_N + 1; m += 2) {
            emitFaceInward(vc, gx, gy, gz, 0, m, m + 1, m + 2);
            emitFaceInward(vc, gx, gy, gz, HULL_N, HULL_N + m, HULL_N + m + 1, HULL_N + m + 2);
        }
    }

    private void emitFaceInward(BufferBuilder vc, float gx, float gy, float gz, int a, int b, int c, int d) {
        float[] h = hullView;
        float ax = h[a * 3], ay = h[a * 3 + 1], az = h[a * 3 + 2];
        float bx = h[b * 3], by = h[b * 3 + 1], bz = h[b * 3 + 2];
        float cx = h[c * 3], cy = h[c * 3 + 1], cz = h[c * 3 + 2];
        float dx = h[d * 3], dy = h[d * 3 + 1], dz = h[d * 3 + 2];
        float nx = (by - ay) * (cz - az) - (bz - az) * (cy - ay);
        float ny = (bz - az) * (cx - ax) - (bx - ax) * (cz - az);
        float nz = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
        float fx = (ax + bx + cx + dx) * 0.25f;
        float fy = (ay + by + cy + dy) * 0.25f;
        float fz = (az + bz + cz + dz) * 0.25f;
        // Normal (regra da mao direita) apontando para o centro = face voltada para dentro.
        boolean inward = nx * (gx - fx) + ny * (gy - fy) + nz * (gz - fz) > 0.0f;
        if (inward) {
            hullVertex(vc, ax, ay, az, 0f, 0f);
            hullVertex(vc, bx, by, bz, 1f, 0f);
            hullVertex(vc, cx, cy, cz, 1f, 1f);
            hullVertex(vc, dx, dy, dz, 0f, 1f);
        } else {
            hullVertex(vc, dx, dy, dz, 0f, 0f);
            hullVertex(vc, cx, cy, cz, 1f, 0f);
            hullVertex(vc, bx, by, bz, 1f, 1f);
            hullVertex(vc, ax, ay, az, 0f, 1f);
        }
    }

    private static void hullVertex(BufferBuilder vc, float x, float y, float z, float u, float v) {
        vc.vertex((double) x, (double) y, (double) z).color(255, 255, 255, 255).uv(u, v).endVertex();
    }

    /**
     * Boite qui declenche le shader : orientee le long du faisceau quand ses axes forment un
     * repere orthonorme, sinon la boite alignee sur le monde d'origine. Le shader ne retient que
     * les echantillons a 0 <= z <= longueur et |u| <= r * WidthScale, |v| <= r * HeightScale
     * avec r = max(BaseRadius, z * tan) : tout pixel qui contribue voit son rayon traverser
     * cette boite, les autres etaient rejetes par le shader. Meme image, beaucoup moins de
     * pixels traites pour un faisceau en diagonale.
     */
    private static void drawConeProxy(BufferBuilder vc, Matrix4f mat, BeamSlot s, float endRadius) {
        if (drawOrientedProxy(vc, mat, s)) {
            return;
        }
        drawAlignedProxy(vc, mat, s, endRadius);
    }

    private static boolean drawOrientedProxy(BufferBuilder vc, Matrix4f mat, BeamSlot s) {
        double dl = Math.sqrt(s.dirX * s.dirX + s.dirY * s.dirY + s.dirZ * s.dirZ);
        double ul = Math.sqrt(s.uX * s.uX + s.uY * s.uY + s.uZ * s.uZ);
        double vl = Math.sqrt(s.vX * s.vX + s.vY * s.vY + s.vZ * s.vZ);
        if (dl < 1.0e-6 || ul < 1.0e-6 || vl < 1.0e-6) {
            return false;
        }
        double dx = s.dirX / dl, dy = s.dirY / dl, dz = s.dirZ / dl;
        double ux = s.uX / ul, uy = s.uY / ul, uz = s.uZ / ul;
        double vx = s.vX / vl, vy = s.vY / vl, vz = s.vZ / vl;
        // Hors repere orthonorme, la zone du shader n'est plus un pave : boite du monde.
        if (Math.abs(dx * ux + dy * uy + dz * uz) > 1.0e-3
                || Math.abs(dx * vx + dy * vy + dz * vz) > 1.0e-3
                || Math.abs(ux * vx + uy * vy + uz * vz) > 1.0e-3) {
            return false;
        }
        // Repere direct pour garder le sens des faces (elles sont dessinees de l'interieur).
        double det = ux * (vy * dz - vz * dy) - uy * (vx * dz - vz * dx) + uz * (vx * dy - vy * dx);
        if (det < 0.0) {
            ux = -ux;
            uy = -uy;
            uz = -uz;
        }
        double r = Math.max(s.baseRadius, s.scanLen * Math.max(s.tanHalfAngle, 1.0e-5f));
        double hu = r * Math.max(s.widthScale, 0.05f);
        double hv = r * Math.max(s.heightScale, 0.05f);
        double pad = 0.25 + 0.05 * Math.max(hu, hv);
        hu += pad;
        hv += pad;
        double z0 = -pad;
        double z1 = s.scanLen + pad;

        float[][] c = new float[8][];
        int i = 0;
        for (int iz = 0; iz < 2; iz++) {
            for (int iy = 0; iy < 2; iy++) {
                for (int ix = 0; ix < 2; ix++) {
                    double a = ix == 0 ? -hu : hu;
                    double b = iy == 0 ? -hv : hv;
                    double z = iz == 0 ? z0 : z1;
                    c[i++] = new float[]{
                            (float) (s.originX + ux * a + vx * b + dx * z),
                            (float) (s.originY + uy * a + vy * b + dy * z),
                            (float) (s.originZ + uz * a + vz * b + dz * z)};
                }
            }
        }
        // Memes six faces, meme sens que la boite alignee : x -> U, y -> V, z -> direction.
        // Indice d'un coin : x + 2 y + 4 z (0 = min, 1 = max).
        emitQuad(vc, mat, c[6], c[7], c[5], c[4]);
        emitQuad(vc, mat, c[3], c[2], c[0], c[1]);
        emitQuad(vc, mat, c[7], c[3], c[1], c[5]);
        emitQuad(vc, mat, c[2], c[6], c[4], c[0]);
        emitQuad(vc, mat, c[2], c[3], c[7], c[6]);
        emitQuad(vc, mat, c[4], c[5], c[1], c[0]);
        return true;
    }

    private static void emitQuad(BufferBuilder vc, Matrix4f mat, float[] p0, float[] p1, float[] p2, float[] p3) {
        emitQuad(vc, mat, p0[0], p0[1], p0[2], p1[0], p1[1], p1[2], p2[0], p2[1], p2[2], p3[0], p3[1], p3[2]);
    }

    private static void drawAlignedProxy(BufferBuilder vc, Matrix4f mat, BeamSlot s, float endRadius) {
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
        public ResourceLocation nextGoboTexture;
        public float goboRotation;
        /** Texture GL d'une ligne lue le long de la largeur (barres LED), 0 = aucune. */
        public int colorRampTexture;
        public float wheelTransition;
        /** Fondu de fin propre au faisceau, -1 = reglage general. */
        public float fadeLength = -1f;
        public FramingShutterState.Snapshot shutters;
        public BeamRenderData.Animation animation;
        public boolean hitBlock;
        public net.minecraft.core.BlockPos fixturePos;
        public float localOriginX, localOriginY, localOriginZ;
    }
}