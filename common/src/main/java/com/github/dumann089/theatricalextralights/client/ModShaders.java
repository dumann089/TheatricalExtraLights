package com.github.dumann089.theatricalextralights.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.HashMap;
import java.util.Map;

public class ModShaders {

    // Las instancias de los nuevos shaders analíticos
    public static ShaderInstance goboProjectorShader;
    public static ShaderInstance volumetricBeamShader;

    public static final RenderStateShard.ShaderStateShard GOBO_SHADER_STATE =
            new RenderStateShard.ShaderStateShard(() -> goboProjectorShader);

    public static final RenderStateShard.ShaderStateShard VOLUMETRIC_SHADER_STATE =
            new RenderStateShard.ShaderStateShard(() -> volumetricBeamShader);

    public static final RenderStateShard.TransparencyStateShard ADDITIVE_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard("additive_transparency", () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            }, () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            });

    // CRÍTICO para Projective Texturing: Evita que el gobo se repita en mosaico fuera del haz.
    public static final RenderStateShard.TexturingStateShard CLAMP_TEXTURING =
            new RenderStateShard.TexturingStateShard("clamp_texturing", () -> {
                RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            }, () -> {
                RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
                RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            });

    private static final Map<ResourceLocation, RenderType> RENDER_TYPE_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> VOLUMETRIC_TYPE_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> VOLUMETRIC_FALLBACK_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> GOBO_FALLBACK_CACHE = new HashMap<>();

    public static boolean isIrisShaderpackActive() {
        try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object apiInstance = irisApiClass.getMethod("getInstance").invoke(null);
            return (Boolean) irisApiClass.getMethod("isShaderPackInUse").invoke(apiInstance);
        } catch (Exception e) {
            return false;
        }
    }

    public static RenderType getGoboRenderType(ResourceLocation texture) {
        if (isIrisShaderpackActive()) {
            return getGoboFallbackRenderType(texture);
        }

        return RENDER_TYPE_CACHE.computeIfAbsent(texture, tex -> {
            RenderType.CompositeState state = RenderType.CompositeState.builder()
                    .setShaderState(GOBO_SHADER_STATE)
                    .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                    .setTexturingState(CLAMP_TEXTURING) // AGREGADO: Obligatorio para textura proyectiva
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false);

            return RenderType.create("gobo_decal", DefaultVertexFormat.POSITION_COLOR_TEX,
                    VertexFormat.Mode.QUADS, 256, false, true, state);
        });
    }

    public static RenderType getGoboFallbackRenderType(ResourceLocation texture) {
        return GOBO_FALLBACK_CACHE.computeIfAbsent(texture, tex -> {
            RenderType.CompositeState state = RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_BEACON_BEAM_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                    .setTexturingState(CLAMP_TEXTURING)
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("lequal_depth", 515))
                    .setCullState(new RenderStateShard.CullStateShard(false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false);

            return RenderType.create("gobo_projector_fallback", DefaultVertexFormat.POSITION_COLOR_TEX,
                    VertexFormat.Mode.QUADS, 256, false, true, state);
        });
    }

    public static RenderType getVolumetricRenderType(ResourceLocation texture) {
        if (isIrisShaderpackActive()) {
            return getVolumetricFallbackRenderType(texture);
        }

        return VOLUMETRIC_TYPE_CACHE.computeIfAbsent(texture, tex -> {
            RenderType.CompositeState state = RenderType.CompositeState.builder()
                    .setShaderState(VOLUMETRIC_SHADER_STATE)
                    .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                    .setTexturingState(CLAMP_TEXTURING) // AGREGADO: El cono no debe repetir la textura en los bordes
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("lequal_depth", 515))
                    .setCullState(new RenderStateShard.CullStateShard(false)) // Vital para ver el haz desde adentro
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false);

            return RenderType.create("volumetric_beam", DefaultVertexFormat.POSITION_COLOR_TEX,
                    VertexFormat.Mode.QUADS, 256, false, true, state);
        });
    }

    public static RenderType getVolumetricFallbackRenderType(ResourceLocation texture) {
        return VOLUMETRIC_FALLBACK_CACHE.computeIfAbsent(texture, tex -> {
            RenderType.CompositeState state = RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_BEACON_BEAM_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                    .setTexturingState(CLAMP_TEXTURING) // AGREGADO al fallback también
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("lequal_depth", 515))
                    .setCullState(new RenderStateShard.CullStateShard(false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false);

            return RenderType.create("volumetric_beam_fallback", DefaultVertexFormat.POSITION_COLOR_TEX,
                    VertexFormat.Mode.QUADS, 256, false, true, state);
        });
    }
}