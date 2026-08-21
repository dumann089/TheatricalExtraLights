package com.github.dumann089.theatricalextralights.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.imabad.theatrical.compat.ModCompat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ModShaders {

    public static ShaderInstance goboProjectorShader;
    public static ShaderInstance volumetricBeamShader;

    public static final RenderStateShard.ShaderStateShard GOBO_SHADER_STATE =
            new RenderStateShard.ShaderStateShard(() -> goboProjectorShader);

    public static final RenderStateShard.ShaderStateShard VOLUMETRIC_SHADER_STATE =
            new RenderStateShard.ShaderStateShard(() -> volumetricBeamShader);

    public static final RenderStateShard.TransparencyStateShard ADDITIVE_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard(
                    "additive_transparency",
                    () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(
                                GlStateManager.SourceFactor.SRC_ALPHA,
                                GlStateManager.DestFactor.ONE
                        );
                    },
                    () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }
            );

    public static final RenderStateShard.TexturingStateShard CLAMP_TEXTURING =
            new RenderStateShard.TexturingStateShard(
                    "clamp_texturing",
                    () -> {
                        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                    },
                    () -> {
                        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
                        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
                    }
            );

    private static final Map<ResourceLocation, RenderType> RENDER_TYPE_CACHE = new HashMap<>();
    private static final Map<DualTextureKey, RenderType> DUAL_RENDER_TYPE_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> VOLUMETRIC_TYPE_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> VOLUMETRIC_FALLBACK_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> GOBO_FALLBACK_CACHE = new HashMap<>();
    private static final Map<DualTextureKey, RenderType> DUAL_VOLUMETRIC_TYPE_CACHE = new HashMap<>();

    public static boolean isIrisShaderpackActive() {
        try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object apiInstance = irisApiClass.getMethod("getInstance").invoke(null);
            return (Boolean) irisApiClass.getMethod("isShaderPackInUse").invoke(apiInstance);
        } catch (Exception e) {
            return false;
        }
    }

    public static RenderType getGoboRenderType(ResourceLocation tex0, ResourceLocation tex1) {
        return getGoboTransitionRenderType(tex0, tex1);
    }

    public static RenderType getGoboTransitionRenderType(ResourceLocation tex0, ResourceLocation tex1) {
        if (isIrisShaderpackActive()) {
            return getGoboFallbackRenderType(tex0);
        }

        DualTextureKey key = new DualTextureKey(tex0, tex1);

        return DUAL_RENDER_TYPE_CACHE.computeIfAbsent(
                key,
                k -> {
                    RenderStateShard.TexturingStateShard dualTextureState =
                            new RenderStateShard.TexturingStateShard(
                                    "dual_gobo_textures",
                                    () -> {
                                        RenderSystem.setShaderTexture(0, tex0);
                                        RenderSystem.setShaderTexture(1, tex1);
                                        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                                        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                                    },
                                    () -> {}
                            );

                    RenderType.CompositeState state =
                            RenderType.CompositeState.builder()
                                    .setShaderState(GOBO_SHADER_STATE)
                                    .setTextureState(new RenderStateShard.TextureStateShard(tex0, false, false))
                                    .setTexturingState(dualTextureState)
                                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                                    .setCullState(RenderStateShard.NO_CULL) // Culling manejado en shader
                                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST) // Profundidad leída del Sampler2
                                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                                    .createCompositeState(false);

                    return RenderType.create(
                            "gobo_wheel_transition",
                            DefaultVertexFormat.POSITION_COLOR_TEX,
                            VertexFormat.Mode.QUADS,
                            256,
                            false,
                            true,
                            state
                    );
                }
        );
    }

    public static RenderType getGoboFallbackRenderType(ResourceLocation texture) {
        return GOBO_FALLBACK_CACHE.computeIfAbsent(
                texture,
                tex -> {
                    RenderType.CompositeState state =
                            RenderType.CompositeState.builder()
                                    .setShaderState(RenderStateShard.RENDERTYPE_BEACON_BEAM_SHADER)
                                    .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                                    .setTexturingState(CLAMP_TEXTURING)
                                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST) // Mantiene profundidad para fallback
                                    .setCullState(RenderStateShard.NO_CULL)
                                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                                    .createCompositeState(false);

                    return RenderType.create(
                            "gobo_projector_fallback",
                            DefaultVertexFormat.POSITION_COLOR_TEX,
                            VertexFormat.Mode.QUADS,
                            256,
                            false,
                            true,
                            state
                    );
                }
        );
    }

    public static RenderType getVolumetricRenderType(ResourceLocation texture) {
        if (ModCompat.SHIMMER || isIrisShaderpackActive()) {
            return getVolumetricFallbackRenderType(texture);
        }

        return VOLUMETRIC_TYPE_CACHE.computeIfAbsent(
                texture,
                tex -> {
                    RenderType.CompositeState state =
                            RenderType.CompositeState.builder()
                                    .setShaderState(VOLUMETRIC_SHADER_STATE)
                                    .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                                    .setTexturingState(CLAMP_TEXTURING)
                                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                                    .setCullState(RenderStateShard.NO_CULL) // Culling manejado en shader
                                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST) // Profundidad leída del Sampler2
                                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                                    .createCompositeState(false);

                    return RenderType.create(
                            "volumetric_beam",
                            DefaultVertexFormat.POSITION_COLOR_TEX,
                            VertexFormat.Mode.QUADS,
                            256,
                            false,
                            true,
                            state
                    );
                }
        );
    }

    public static RenderType getVolumetricFallbackRenderType(ResourceLocation texture) {
        return VOLUMETRIC_FALLBACK_CACHE.computeIfAbsent(
                texture,
                tex -> {
                    RenderType.CompositeState state =
                            RenderType.CompositeState.builder()
                                    .setShaderState(RenderStateShard.RENDERTYPE_BEACON_BEAM_SHADER)
                                    .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                                    .setTexturingState(CLAMP_TEXTURING)
                                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST) // Mantiene profundidad para fallback
                                    .setCullState(RenderStateShard.NO_CULL)
                                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                                    .createCompositeState(false);

                    return RenderType.create(
                            "volumetric_beam_fallback",
                            DefaultVertexFormat.POSITION_COLOR_TEX,
                            VertexFormat.Mode.QUADS,
                            256,
                            false,
                            true,
                            state
                    );
                }
        );
    }

    public static RenderType getDualVolumetricRenderType(ResourceLocation tex0, ResourceLocation tex1) {
        return getVolumetricTransitionRenderType(tex0, tex1);
    }

    public static RenderType getVolumetricTransitionRenderType(ResourceLocation tex0, ResourceLocation tex1) {
        if (ModCompat.SHIMMER || isIrisShaderpackActive()) {
            return getVolumetricFallbackRenderType(tex0);
        }

        DualTextureKey key = new DualTextureKey(tex0, tex1);

        return DUAL_VOLUMETRIC_TYPE_CACHE.computeIfAbsent(
                key,
                k -> {
                    RenderStateShard.TexturingStateShard dualTextureState =
                            new RenderStateShard.TexturingStateShard(
                                    "dual_volumetric_textures",
                                    () -> {
                                        RenderSystem.setShaderTexture(0, tex0);
                                        RenderSystem.setShaderTexture(1, tex1);
                                        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                                        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                                    },
                                    () -> {}
                            );

                    RenderType.CompositeState state =
                            RenderType.CompositeState.builder()
                                    .setShaderState(VOLUMETRIC_SHADER_STATE)
                                    .setTextureState(new RenderStateShard.TextureStateShard(tex0, false, false))
                                    .setTexturingState(dualTextureState)
                                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                                    .setCullState(RenderStateShard.NO_CULL) // Culling manejado en shader
                                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST) // Profundidad leída del Sampler2
                                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                                    .createCompositeState(false);

                    return RenderType.create(
                            "volumetric_wheel_transition",
                            DefaultVertexFormat.POSITION_COLOR_TEX,
                            VertexFormat.Mode.QUADS,
                            256,
                            false,
                            true,
                            state
                    );
                }
        );
    }

    public static RenderType getDualVolumetricFallbackRenderType(ResourceLocation tex0, ResourceLocation tex1) {
        return getVolumetricFallbackRenderType(tex0);
    }

    private static class DualTextureKey {
        final ResourceLocation t0;
        final ResourceLocation t1;

        DualTextureKey(ResourceLocation t0, ResourceLocation t1) {
            this.t0 = t0;
            this.t1 = t1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DualTextureKey that = (DualTextureKey) o;
            return t0.equals(that.t0) && t1.equals(that.t1);
        }

        @Override
        public int hashCode() {
            return Objects.hash(t0, t1);
        }
    }
}