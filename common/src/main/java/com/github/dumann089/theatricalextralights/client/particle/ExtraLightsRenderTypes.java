package com.github.dumann089.theatricalextralights.client.particle;

import com.github.dumann089.theatricalextralights.client.render.pyro.FlameBloom;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import org.lwjgl.opengl.GL11;

@Environment(EnvType.CLIENT)
public final class ExtraLightsRenderTypes {
    public static final ParticleRenderType FLAME_THROWER_JET = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder builder, TextureManager textureManager) {
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            AbstractTexture atlas = textureManager.getTexture(TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.bindTexture(atlas.getId());
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.enableDepthTest();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            // Dessine le lot, et le bloom des flammes quand il est actif.
            FlameBloom.drawFlameBatch(tesselator.getBuilder());
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString() {
            return "FLAME_THROWER_JET_ADDITIVE";
        }
    };

    private static int co2AtlasId = -1;
    private static int co2PreviousMinFilter;
    private static int co2PreviousMagFilter;

    /** CO₂ : alpha blend classique (fumée translucide, pas de lueur additive). */
    public static final ParticleRenderType CO2_JET = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder builder, TextureManager textureManager) {
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            AbstractTexture atlas = textureManager.getTexture(TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.bindTexture(atlas.getId());
            // Sprites de 64 px etires sur un bloc entier : le filtrage nearest de l'atlas decoupe le
            // bord de la fumee en marches d'escalier. Lineaire pour ce lot seulement, puis retabli.
            co2AtlasId = atlas.getId();
            co2PreviousMinFilter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
            co2PreviousMagFilter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
            if (co2PreviousMinFilter == GL11.GL_NEAREST) {
                RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            }
            RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
            );
            RenderSystem.enableDepthTest();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            if (co2AtlasId >= 0) {
                RenderSystem.bindTexture(co2AtlasId);
                RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, co2PreviousMinFilter);
                RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, co2PreviousMagFilter);
                co2AtlasId = -1;
            }
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString() {
            return "CO2_JET_TRANSLUCENT";
        }
    };

    private ExtraLightsRenderTypes() {
    }

    /** Rendu additif custom ; compatible Shimmer (même pipeline que lance-flammes). */
    public static ParticleRenderType flameThrowerJetRenderType() {
        return FLAME_THROWER_JET;
    }

    /** CO₂ : fumée blanche semi-transparente. */
    public static ParticleRenderType co2JetRenderType() {
        return CO2_JET;
    }
}
