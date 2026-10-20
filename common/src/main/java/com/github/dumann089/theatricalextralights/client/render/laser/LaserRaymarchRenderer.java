package com.github.dumann089.theatricalextralights.client.render.laser;

import com.github.dumann089.theatricalextralights.client.ModShaders;
import com.github.dumann089.theatricalextralights.client.render.beam.raymarch.SceneDepthCopy;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

/**
 * Dessine les lasers avec le shader {@code laser_raymarch} : une passe ecran par fixture,
 * bornee au rectangle que couvre la figure, avec la profondeur de la scene pour l'occlusion
 * et les impacts. La figure est envoyee dans une texture RGBA32F de 8 texels par segment.
 *
 * <p>Les fixtures soumettent leur {@link LaserFigure} pendant le rendu des block entities ;
 * le dessin a lieu dans la phase differee de Theatrical, apres les blocs, quand le depth
 * buffer est complet.
 */
public final class LaserRaymarchRenderer extends LazyRenderers.LazyRenderer {

    public static final int MAX_FIGURES = 64;
    private static final int TEXELS_PER_SEGMENT = 8;
    private static final float EXTINCTION_PER_METER = 0.02f;

    private static final LaserRaymarchRenderer INSTANCE = new LaserRaymarchRenderer();

    private final LaserFigure[] figures = new LaserFigure[MAX_FIGURES];
    private int activeCount;

    private int dataTexture;
    private final FloatBuffer upload = BufferUtils.createFloatBuffer(
            LaserFigure.MAX_SEGMENTS * TEXELS_PER_SEGMENT * 4);

    private final Matrix4f invProj = new Matrix4f();
    private final Matrix4f viewProj = new Matrix4f();
    private final Vector4f tmp = new Vector4f();

    private LaserRaymarchRenderer() {
        for (int i = 0; i < MAX_FIGURES; i++) {
            figures[i] = new LaserFigure();
        }
    }

    /** Vrai quand le moteur realiste peut dessiner : shader charge, pas de shaderpack Iris. */
    public static boolean isAvailable() {
        return TheatricalExtraLightsConfig.isLaserRealistic() && ModShaders.canUseLaserRaymarch();
    }

    /**
     * Reserve une figure pour cette image. Le renderer la dessinera puis la recyclera ; le
     * fixture la remplit tout de suite.
     *
     * @return la figure vide a remplir, ou null si le quota est atteint
     */
    public static LaserFigure begin() {
        return INSTANCE.acquire();
    }

    private LaserFigure acquire() {
        if (activeCount >= MAX_FIGURES) {
            return null;
        }
        if (activeCount == 0) {
            SceneDepthCopy.beginFrame();
            LazyRenderers.addLazyRender(this);
        }
        LaserFigure f = figures[activeCount++];
        f.reset();
        return f;
    }

    @Override
    public void render(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Camera camera, float partialTick) {
        try {
            if (activeCount == 0) {
                return;
            }
            SceneDepthCopy.flushOpaqueAndCapture(bufferSource);
            bufferSource.endBatch();
            ShaderInstance shader = ModShaders.laserRaymarchShader;
            if (!SceneDepthCopy.hasDepth() || shader == null) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            Matrix4f proj = RenderSystem.getProjectionMatrix();
            invProj.set(proj);
            invProj.invert();

            var main = mc.getMainRenderTarget();
            float screenW = main.width;
            float screenH = main.height;
            // proj.m11 = 1 / tan(fovY / 2) : angle couvert par un pixel en hauteur.
            float pixelAngle = 2.0f / (Math.max(proj.m11(), 1.0e-3f) * Math.max(screenH, 1.0f));

            float time = (mc.level != null ? mc.level.getGameTime() : 0) + partialTick;
            float daylight = mc.level != null
                    ? 1.0f - Math.min(1.0f, mc.level.getSkyDarken() / 11.0f)
                    : 0.0f;

            float haze = TheatricalExtraLightsConfig.getLaserHaze();
            float brightness = TheatricalExtraLightsConfig.getLaserBrightness();
            float radius = TheatricalExtraLightsConfig.getLaserBeamRadiusCm() * 0.01f;
            float impacts = TheatricalExtraLightsConfig.isLaserImpactsEnabled() ? 1.0f : 0.0f;
            float anisotropy = Math.max(0.0f, TheatricalExtraLightsConfig.getRaymarchAnisotropy());
            int octaves = switch (TheatricalExtraLightsConfig.getRaymarchQuality().trim().toUpperCase()) {
                case "LOW" -> 1;
                case "MEDIUM" -> 2;
                default -> 3;
            };

            Vec3 camPos = camera.getPosition();
            poseStack.pushPose();
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
            Matrix4f view = new Matrix4f(poseStack.last().pose());
            poseStack.popPose();
            viewProj.set(proj).mul(view);

            RenderType renderType = ModShaders.getLaserRaymarchRenderType();
            int depthTex = SceneDepthCopy.getDepthTextureId();
            ensureDataTexture();

            for (int n = 0; n < activeCount; n++) {
                LaserFigure fig = figures[n];
                if (fig.count == 0 || fig.intensity <= 0.0f) {
                    continue;
                }
                float[] rect = new float[4];
                if (!screenRect(fig, view, rect)) {
                    continue;
                }

                uploadFigure(fig, view);

                transformPoint(view, fig.origin, tmp);
                shader.safeGetUniform("InvProjMat").set(invProj);
                shader.safeGetUniform("ScreenSize").set(screenW, screenH);
                shader.safeGetUniform("SegCount").set(fig.count);
                shader.safeGetUniform("OriginV").set(tmp.x, tmp.y, tmp.z);
                shader.safeGetUniform("OriginW").set((float) fig.origin.x, (float) fig.origin.y, (float) fig.origin.z);
                shader.safeGetUniform("PixelAngle").set(pixelAngle);
                shader.safeGetUniform("BeamRadius").set(radius);
                shader.safeGetUniform("Divergence").set(fig.divergence);
                shader.safeGetUniform("HazeDensity").set(haze);
                shader.safeGetUniform("Brightness").set(brightness);
                shader.safeGetUniform("Intensity").set(fig.intensity);
                shader.safeGetUniform("Anisotropy").set(anisotropy);
                shader.safeGetUniform("Extinction").set(EXTINCTION_PER_METER);
                shader.safeGetUniform("TotalSpan").set(Math.max(fig.totalSpan, 1.0e-3f));
                shader.safeGetUniform("ScanHead").set(fig.scanHead);
                shader.safeGetUniform("ScanTrail").set(fig.scanTrail);
                shader.safeGetUniform("ImpactEnabled").set(impacts);
                shader.safeGetUniform("Striation").set(fig.striation);
                shader.safeGetUniform("NoiseOctaves").set(octaves);
                shader.safeGetUniform("Time").set(time);
                shader.safeGetUniform("Ambient").set(daylight);

                renderType.setupRenderState();
                RenderSystem.setShader(() -> shader);
                RenderSystem.setShaderTexture(1, depthTex);
                shader.setSampler("Sampler1", depthTex);
                RenderSystem.setShaderTexture(2, dataTexture);
                shader.setSampler("Sampler2", dataTexture);
                shader.apply();

                Tesselator tess = Tesselator.getInstance();
                BufferBuilder bb = tess.getBuilder();
                bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
                bb.vertex(rect[0], rect[3], 0.0).color(255, 255, 255, 255).uv(0f, 0f).endVertex();
                bb.vertex(rect[2], rect[3], 0.0).color(255, 255, 255, 255).uv(1f, 0f).endVertex();
                bb.vertex(rect[2], rect[1], 0.0).color(255, 255, 255, 255).uv(1f, 1f).endVertex();
                bb.vertex(rect[0], rect[1], 0.0).color(255, 255, 255, 255).uv(0f, 1f).endVertex();
                BufferUploader.drawWithShader(bb.end());
                renderType.clearRenderState();
            }
        } finally {
            activeCount = 0;
        }
    }

    /**
     * Rectangle ecran (NDC, minX minY maxX maxY) couvert par la figure, avec une marge pour le
     * halo. Plein ecran des qu'un point passe derriere la camera.
     */
    private boolean screenRect(LaserFigure fig, Matrix4f view, float[] out) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        boolean full = false;

        full |= !project(fig.origin.x, fig.origin.y, fig.origin.z);
        if (!full) {
            minX = Math.min(minX, tmp.x); maxX = Math.max(maxX, tmp.x);
            minY = Math.min(minY, tmp.y); maxY = Math.max(maxY, tmp.y);
        }
        for (int i = 0; i < fig.count && !full; i++) {
            for (int e = 0; e < 2; e++) {
                float[] d = e == 0 ? fig.dir0 : fig.dir1;
                float l = e == 0 ? fig.len0[i] : fig.len1[i];
                double x = fig.origin.x + d[i * 3] * l;
                double y = fig.origin.y + d[i * 3 + 1] * l;
                double z = fig.origin.z + d[i * 3 + 2] * l;
                if (!project(x, y, z)) {
                    full = true;
                    break;
                }
                minX = Math.min(minX, tmp.x); maxX = Math.max(maxX, tmp.x);
                minY = Math.min(minY, tmp.y); maxY = Math.max(maxY, tmp.y);
            }
        }
        if (full) {
            out[0] = -1f; out[1] = -1f; out[2] = 1f; out[3] = 1f;
            return true;
        }
        float pad = 0.06f;
        out[0] = Math.max(-1f, minX - pad);
        out[1] = Math.max(-1f, minY - pad);
        out[2] = Math.min(1f, maxX + pad);
        out[3] = Math.min(1f, maxY + pad);
        return out[2] > out[0] && out[3] > out[1];
    }

    /** Projette un point monde en NDC dans {@link #tmp}; faux s'il est derriere la camera. */
    private boolean project(double x, double y, double z) {
        tmp.set((float) x, (float) y, (float) z, 1f);
        viewProj.transform(tmp);
        if (tmp.w < 0.05f) {
            return false;
        }
        tmp.x /= tmp.w;
        tmp.y /= tmp.w;
        return true;
    }

    private void ensureDataTexture() {
        if (dataTexture != 0) {
            return;
        }
        dataTexture = TextureUtil.generateTextureId();
        GlStateManager._bindTexture(dataTexture);
        resetUnpackState();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, TEXELS_PER_SEGMENT,
                LaserFigure.MAX_SEGMENTS, 0, GL11.GL_RGBA, GL11.GL_FLOAT, (FloatBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    }

    /**
     * Texels par segment :
     * 0 dir0 vue + len0 · 1 dir1 vue + len1 · 2 dir0 monde + flags · 3 dir1 monde + span ·
     * 4 couleur + poids nappe · 5 poids faisceau 0, 1 + chemin 0, 1 · 6, 7 libres.
     */
    private void uploadFigure(LaserFigure fig, Matrix4f view) {
        upload.clear();
        for (int i = 0; i < fig.count; i++) {
            transformDir(view, fig.dir0[i * 3], fig.dir0[i * 3 + 1], fig.dir0[i * 3 + 2], tmp);
            upload.put(tmp.x).put(tmp.y).put(tmp.z).put(fig.len0[i]);
            transformDir(view, fig.dir1[i * 3], fig.dir1[i * 3 + 1], fig.dir1[i * 3 + 2], tmp);
            upload.put(tmp.x).put(tmp.y).put(tmp.z).put(fig.len1[i]);
            upload.put(fig.dir0[i * 3]).put(fig.dir0[i * 3 + 1]).put(fig.dir0[i * 3 + 2]).put((float) fig.flags[i]);
            upload.put(fig.dir1[i * 3]).put(fig.dir1[i * 3 + 1]).put(fig.dir1[i * 3 + 2]).put(fig.span[i]);
            int c = fig.color[i];
            upload.put(((c >> 16) & 0xFF) / 255f).put(((c >> 8) & 0xFF) / 255f).put((c & 0xFF) / 255f).put(fig.sheetWeight[i]);
            upload.put(fig.beamWeight0[i]).put(fig.beamWeight1[i]).put(fig.path0[i]).put(fig.path1[i]);
            upload.put(0f).put(0f).put(0f).put(0f);
            upload.put(0f).put(0f).put(0f).put(0f);
        }
        upload.flip();
        GlStateManager._bindTexture(dataTexture);
        resetUnpackState();
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, TEXELS_PER_SEGMENT, fig.count,
                GL11.GL_RGBA, GL11.GL_FLOAT, upload);
    }

    /**
     * Remet l'etat de depaquetage des pixels a la valeur par defaut avant un envoi de texture.
     *
     * <p>Les envois vanilla (NativeImage : skins, atlas) fixent GL_UNPACK_ROW_LENGTH, SKIP_ROWS,
     * SKIP_PIXELS et ALIGNMENT a la taille de leur image et ne les remettent jamais. Si un skin de
     * 64 px vient d'etre charge, notre texture de 8 texels de large serait lue avec un pas de 64 :
     * le pilote lit huit fois trop loin dans le tampon et plante en natif (EXCEPTION_ACCESS_VIOLATION
     * dans nvoglv64.dll, vu a la connexion a un serveur). Un eventuel pixel unpack buffer lie par
     * un autre mod changerait de meme le sens du pointeur.
     */
    private static void resetUnpackState() {
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);
    }

    private void transformPoint(Matrix4f mat, Vec3 p, Vector4f out) {
        out.set((float) p.x, (float) p.y, (float) p.z, 1f);
        mat.transform(out);
    }

    private void transformDir(Matrix4f mat, float x, float y, float z, Vector4f out) {
        out.set(x, y, z, 0f);
        mat.transform(out);
        float l = (float) Math.sqrt(out.x * out.x + out.y * out.y + out.z * out.z);
        if (l > 1.0e-6f) {
            out.x /= l;
            out.y /= l;
            out.z /= l;
        }
    }

    @Override
    public Vec3 getPos(float partialTick) {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    }
}
