package com.github.dumann089.theatricalextralights.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class LensRenderTypes {

    public static final RenderType LENS = RenderType.create(
            "Lens", // namespace único
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, // agrega lightmap
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(
                            GameRenderer::getPositionColorTexLightmapShader // shader con lightmap
                    ))
                    .setTextureState(new RenderStateShard.TextureStateShard(
                            new ResourceLocation("theatricalextralights", "textures/misc/lens.png"),
                            false,
                            false
                    ))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE) // sin depth write para transparencia
                    .createCompositeState(false)
    );
    public static final RenderType FLAME = RenderType.create(
            "flame", // namespace único
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, // agrega lightmap
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(
                            GameRenderer::getPositionColorTexLightmapShader // shader con lightmap
                    ))
                    .setTextureState(new RenderStateShard.TextureStateShard(
                            new ResourceLocation("theatricalextralights", "textures/particle/firework_core.png"),
                            false,
                            false
                    ))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE) // sin depth write para transparencia
                    .createCompositeState(false)
    );
}