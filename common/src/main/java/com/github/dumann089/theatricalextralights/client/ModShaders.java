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

public class ModShaders {
    public static ShaderInstance goboProjectorShader;
    public static ShaderInstance volumetricBeamShader;
    public static ShaderInstance beamRaymarchShader;

    public static float configDensity = 0.15f;
    public static float configMaxAlpha = 0.25f;
    public static float currentFixtureIntensity = 1.0f;

    public static final RenderStateShard.ShaderStateShard GOBO_SHADER_STATE =
            new RenderStateShard.ShaderStateShard(() -> goboProjectorShader);

    public static final RenderStateShard.ShaderStateShard VOLUMETRIC_SHADER_STATE =
            new RenderStateShard.ShaderStateShard(() -> volumetricBeamShader);

    public static final RenderStateShard.ShaderStateShard RAYMARCH_SHADER_STATE =
            new RenderStateShard.ShaderStateShard(() -> beamRaymarchShader);

    public static final RenderStateShard.TransparencyStateShard ADDITIVE_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard("additive_transparency", () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(
                        GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE
                );
            }, () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            });

    public static final RenderStateShard.TransparencyStateShard PURE_ADDITIVE_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard("pure_additive_transparency", () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(
                        GlStateManager.SourceFactor.ONE,
                        GlStateManager.DestFactor.ONE
                );
            }, () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            });

    public static final RenderStateShard.TexturingStateShard CLAMP_TEXTURING =
            new RenderStateShard.TexturingStateShard("clamp_texturing", () -> {
                RenderSystem.texParameter(
                        GL11.GL_TEXTURE_2D,
                        GL11.GL_TEXTURE_WRAP_S,
                        GL12.GL_CLAMP_TO_EDGE
                );
                RenderSystem.texParameter(
                        GL11.GL_TEXTURE_2D,
                        GL11.GL_TEXTURE_WRAP_T,
                        GL12.GL_CLAMP_TO_EDGE
                );
            }, () -> {
                RenderSystem.texParameter(
                        GL11.GL_TEXTURE_2D,
                        GL11.GL_TEXTURE_WRAP_S,
                        GL11.GL_REPEAT
                );
                RenderSystem.texParameter(
                        GL11.GL_TEXTURE_2D,
                        GL11.GL_TEXTURE_WRAP_T,
                        GL11.GL_REPEAT
                );
            });

    private static final Map<ResourceLocation, RenderType> RENDER_TYPE_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> VOLUMETRIC_TYPE_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> VOLUMETRIC_FALLBACK_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> GOBO_FALLBACK_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> RAYMARCH_TYPE_CACHE = new HashMap<>();

    public static boolean isIrisShaderpackActive() {
        return IrisCompat.isShadersActive();
    }

    public static boolean canUseRaymarch() {
        return beamRaymarchShader != null && !isIrisShaderpackActive();
    }

    public static void updateRaymarchConfig() {
        if (beamRaymarchShader == null) {
            return;
        }

        configDensity = com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig.getVolumetricBeamDensity();
        configMaxAlpha = com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig.getVolumetricBeamMaxAlpha();

        ShaderInstance shader = beamRaymarchShader;

        if (shader.getUniform("Density") != null) {
            shader.getUniform("Density").set(configDensity);
        }

        if (shader.getUniform("MaxAlpha") != null) {
            shader.getUniform("MaxAlpha").set(configMaxAlpha);
        }

        if (shader.getUniform("Anisotropy") != null) {
            shader.getUniform("Anisotropy").set(
                    com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig.getRaymarchAnisotropy()
            );
        }

        if (shader.getUniform("DustAmount") != null) {
            shader.getUniform("DustAmount").set(
                    com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig.getRaymarchDustAmount()
            );
        }

        if (shader.getUniform("RaymarchSteps") != null) {
            shader.getUniform("RaymarchSteps").set(
                    com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig.getRaymarchSteps()
            );
        }

        if (shader.getUniform("MaxBeamsPerFrame") != null) {
            shader.getUniform("MaxBeamsPerFrame").set(
                    com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig.getRaymarchMaxBeamsPerFrame()
            );
        }

        if (shader.getUniform("BeamDistance") != null) {
            shader.getUniform("BeamDistance").set(
                    com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig.getVolumetricBeamDistance()
            );
        }

        if (shader.getUniform("Brightness") != null) {
            shader.getUniform("Brightness").set(
                    com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig.getVolumetricBeamBrightness()
            );
        }

        if (shader.getUniform("FadeLength") != null) {
            shader.getUniform("FadeLength").set(
                    com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig.getVolumetricBeamFadeLength()
            );
        }
    }

    public static RenderType getRaymarchRenderType(ResourceLocation texture) {
        return RAYMARCH_TYPE_CACHE.computeIfAbsent(texture, tex -> {
            RenderType.CompositeState state = RenderType.CompositeState.builder()
                    .setShaderState(RAYMARCH_SHADER_STATE)
                    .setTextureState(
                            new RenderStateShard.TextureStateShard(
                                    tex,
                                    false,
                                    false
                            )
                    )
                    .setTransparencyState(PURE_ADDITIVE_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderStateShard.CULL)
                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false);

            return RenderType.create(
                    "beam_raymarch",
                    DefaultVertexFormat.POSITION_COLOR_TEX,
                    VertexFormat.Mode.QUADS,
                    65536,
                    false,
                    true,
                    state
            );
        });
    }

    public static RenderType getGoboRenderType(ResourceLocation texture) {
        if (ModCompat.SHIMMER || isIrisShaderpackActive()) {
            return getGoboFallbackRenderType(texture);
        }

        return RENDER_TYPE_CACHE.computeIfAbsent(texture, tex -> {
            RenderType.CompositeState state =
                    RenderType.CompositeState.builder()
                            .setShaderState(GOBO_SHADER_STATE)
                            .setTextureState(
                                    new RenderStateShard.TextureStateShard(
                                            tex,
                                            false,
                                            false
                                    )
                            )
                            .setTransparencyState(ADDITIVE_TRANSPARENCY)
                            .setCullState(RenderStateShard.NO_CULL)
                            .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                            .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                            .createCompositeState(false);

            return RenderType.create(
                    "gobo_projector",
                    DefaultVertexFormat.POSITION_COLOR_TEX,
                    VertexFormat.Mode.QUADS,
                    65536,
                    false,
                    true,
                    state
            );
        });
    }

    public static RenderType getGoboFallbackRenderType(ResourceLocation texture) {
        return GOBO_FALLBACK_CACHE.computeIfAbsent(texture, tex -> {
            RenderType.CompositeState state =
                    RenderType.CompositeState.builder()
                            .setShaderState(
                                    RenderStateShard.RENDERTYPE_BEACON_BEAM_SHADER
                            )
                            .setTextureState(
                                    new RenderStateShard.TextureStateShard(
                                            tex,
                                            false,
                                            false
                                    )
                            )
                            .setTexturingState(CLAMP_TEXTURING)
                            .setTransparencyState(ADDITIVE_TRANSPARENCY)
                            .setDepthTestState(
                                    new RenderStateShard.DepthTestStateShard(
                                            "lequal_depth",
                                            515
                                    )
                            )
                            .setCullState(
                                    new RenderStateShard.CullStateShard(false)
                            )
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
        });
    }

    public static RenderType getVolumetricRenderType(ResourceLocation texture) {
        if (ModCompat.SHIMMER || isIrisShaderpackActive()) {
            return getVolumetricFallbackRenderType(texture);
        }

        return VOLUMETRIC_TYPE_CACHE.computeIfAbsent(texture, tex -> {
            RenderType.CompositeState state =
                    RenderType.CompositeState.builder()
                            .setShaderState(VOLUMETRIC_SHADER_STATE)
                            .setTextureState(
                                    new RenderStateShard.TextureStateShard(
                                            tex,
                                            false,
                                            false
                                    )
                            )
                            .setTexturingState(CLAMP_TEXTURING)
                            .setTransparencyState(ADDITIVE_TRANSPARENCY)
                            .setDepthTestState(
                                    new RenderStateShard.DepthTestStateShard(
                                            "lequal_depth",
                                            515
                                    )
                            )
                            .setCullState(
                                    new RenderStateShard.CullStateShard(false)
                            )
                            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                            .createCompositeState(false);

            return RenderType.create(
                    "volumetric_beam",
                    DefaultVertexFormat.POSITION_COLOR_TEX,
                    VertexFormat.Mode.QUADS,
                    65536,
                    false,
                    true,
                    state
            );
        });
    }

    public static RenderType getVolumetricFallbackRenderType(ResourceLocation texture) {
        return VOLUMETRIC_FALLBACK_CACHE.computeIfAbsent(texture, tex -> {
            RenderType.CompositeState state =
                    RenderType.CompositeState.builder()
                            .setShaderState(
                                    RenderStateShard.RENDERTYPE_BEACON_BEAM_SHADER
                            )
                            .setTextureState(
                                    new RenderStateShard.TextureStateShard(
                                            tex,
                                            false,
                                            false
                                    )
                            )
                            .setTexturingState(CLAMP_TEXTURING)
                            .setTransparencyState(ADDITIVE_TRANSPARENCY)
                            .setDepthTestState(
                                    new RenderStateShard.DepthTestStateShard(
                                            "lequal_depth",
                                            515
                                    )
                            )
                            .setCullState(
                                    new RenderStateShard.CullStateShard(false)
                            )
                            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                            .createCompositeState(false);

            return RenderType.create(
                    "volumetric_beam_fallback",
                    DefaultVertexFormat.POSITION_COLOR_TEX,
                    VertexFormat.Mode.QUADS,
                    65536,
                    false,
                    true,
                    state
            );
        });
    }
}