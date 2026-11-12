package com.github.dumann089.theatricalextralights.client.render.pyro;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.client.IrisCompat;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.imabad.theatrical.compat.ModCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * Bloom des flammes sans shaderpack.
 *
 * <p>Le lot de particules de flamme est dessine deux fois : dans l'image, puis dans une cible
 * de lueur qui partage la profondeur de la scene (une flamme cachee par un mur ne rayonne
 * pas). La lueur est reduite en demi-resolution, floutee en deux allers-retours gaussiens de
 * pas differents, puis ajoutee a l'image. Tout passe par les post-shaders vanilla
 * ({@link PostChain}), donc ni Iris ni un shaderpack ne sont necessaires ; quand l'un d'eux
 * est actif, on s'efface, il fait deja son propre bloom.
 */
public final class FlameBloom {

    private static final ResourceLocation CHAIN_JSON =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "shaders/post/flame_bloom.json");

    private static PostChain chain;
    private static RenderTarget glow;
    private static RenderTarget halfA;
    private static RenderTarget halfB;
    private static PostPass blurH1;
    private static PostPass blurV1;
    private static PostPass blurH2;
    private static PostPass blurV2;
    private static int width = -1;
    private static int height = -1;
    private static boolean failed;

    private static VertexBuffer scratch;

    private FlameBloom() {
    }

    /** Vrai quand le bloom doit tourner : active en config, pas de shaderpack, shaders charges. */
    public static boolean isEnabled() {
        return TheatricalExtraLightsConfig.isFlameBloomEnabled()
                && !failed
                && !ModCompat.SHIMMER
                && !IrisCompat.isShadersActive();
    }

    /**
     * Remplace {@code tesselator.end()} pour le type de particule des flammes : dessine le lot
     * dans l'image et, si le bloom est actif, dans la cible de lueur puis compose le halo.
     * L'etat de melange et le depth mask sont laisses tels que le type de particule les a
     * poses ; l'appelant les remet ensuite.
     */
    public static void drawFlameBatch(BufferBuilder builder) {
        BufferBuilder.RenderedBuffer rendered = builder.end();
        ShaderInstance shader = RenderSystem.getShader();
        if (shader == null) {
            BufferUploader.drawWithShader(rendered);
            return;
        }
        Matrix4f modelView = RenderSystem.getModelViewMatrix();
        Matrix4f projection = RenderSystem.getProjectionMatrix();

        if (scratch == null) {
            scratch = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        scratch.bind();
        scratch.upload(rendered);
        scratch.drawWithShader(modelView, projection, shader);

        if (isEnabled() && ensureTargets()) {
            Minecraft mc = Minecraft.getInstance();
            RenderTarget main = mc.getMainRenderTarget();

            glow.setClearColor(0f, 0f, 0f, 0f);
            glow.clear(Minecraft.ON_OSX);
            glow.copyDepthFrom(main);
            glow.bindWrite(true);
            scratch.drawWithShader(modelView, projection, shader);
            main.bindWrite(true);

            VertexBuffer.unbind();
            composite(mc, main);
            return;
        }
        VertexBuffer.unbind();
    }

    private static void composite(Minecraft mc, RenderTarget main) {
        Matrix4f savedProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting savedSorting = RenderSystem.getVertexSorting();

        chain.process(mc.getFrameTime());

        // Composition additive de la demi-resolution floutee sur l'image.
        main.bindWrite(true);
        Matrix4f ortho = new Matrix4f().setOrtho(0f, main.width, main.height, 0f, 1000f, 3000f);
        RenderSystem.setProjectionMatrix(ortho, VertexSorting.ORTHOGRAPHIC_Z);
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.setIdentity();
        modelViewStack.translate(0f, 0f, -2000f);
        RenderSystem.applyModelViewMatrix();

        float strength = TheatricalExtraLightsConfig.getFlameBloomStrength();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, halfA.getColorTextureId());
        RenderSystem.setShaderColor(strength, strength, strength, 1f);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        float w = main.width;
        float h = main.height;
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.vertex(0.0, h, 0.0).uv(0f, 0f).endVertex();
        bb.vertex(w, h, 0.0).uv(1f, 0f).endVertex();
        bb.vertex(w, 0.0, 0.0).uv(1f, 1f).endVertex();
        bb.vertex(0.0, 0.0, 0.0).uv(0f, 1f).endVertex();
        BufferUploader.drawWithShader(bb.end());

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(savedProjection, savedSorting);
        // Le moteur de particules attend le test de profondeur actif pour les types suivants.
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getParticleShader);
    }

    private static boolean ensureTargets() {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        if (main == null || main.width <= 0 || main.height <= 0) {
            return false;
        }
        if (chain != null && main.width == width && main.height == height) {
            return true;
        }
        close();
        try {
            chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, CHAIN_JSON);
            int hw = Math.max(1, main.width / 2);
            int hh = Math.max(1, main.height / 2);
            chain.addTempTarget("glow", main.width, main.height);
            chain.addTempTarget("half_a", hw, hh);
            chain.addTempTarget("half_b", hw, hh);
            glow = chain.getTempTarget("glow");
            halfA = chain.getTempTarget("half_a");
            halfB = chain.getTempTarget("half_b");
            glow.setFilterMode(GL11.GL_LINEAR);
            halfA.setFilterMode(GL11.GL_LINEAR);
            halfB.setFilterMode(GL11.GL_LINEAR);

            chain.addPass("tel_bloom_down", glow, halfA);
            blurH1 = chain.addPass("tel_bloom_blur", halfA, halfB);
            blurV1 = chain.addPass("tel_bloom_blur", halfB, halfA);
            blurH2 = chain.addPass("tel_bloom_blur", halfA, halfB);
            blurV2 = chain.addPass("tel_bloom_blur", halfB, halfA);
            setBlur(blurH1, 1f, 0f, 1.0f);
            setBlur(blurV1, 0f, 1f, 1.0f);
            setBlur(blurH2, 1f, 0f, 2.6f);
            setBlur(blurV2, 0f, 1f, 2.6f);

            width = main.width;
            height = main.height;
            return true;
        } catch (Exception e) {
            TheatricalExtraLights.LOGGER.warn("Flame bloom disabled: post shaders failed to load", e);
            close();
            failed = true;
            return false;
        }
    }

    private static void setBlur(PostPass pass, float dx, float dy, float step) {
        pass.getEffect().safeGetUniform("BlurDir").set(dx, dy);
        pass.getEffect().safeGetUniform("Step").set(step);
    }

    /** Libere les cibles (changement de taille, fermeture). */
    public static void close() {
        if (chain != null) {
            chain.close();
            chain = null;
        }
        glow = null;
        halfA = null;
        halfB = null;
        width = -1;
        height = -1;
    }
}
