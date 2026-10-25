package com.github.dumann089.theatricalextralights.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class LensRenderTypes {

    public static final RenderType LENS = RenderType.create(
            "lens",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true, // Al ser 'true' (sortOnUpload), el buffer ordenará los quads correctamente al activar Depth Write
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(
                            GameRenderer::getPositionColorTexLightmapShader
                    ))
                    .setTextureState(new RenderStateShard.TextureStateShard(
                            new ResourceLocation("theatricalextralights", "textures/misc/lens.png"),
                            false,
                            false
                    ))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    // 1. AÑADIDO: Fuerza el renderizado al búfer correcto en Gráficos Fabulosos
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    // 2. ELIMINADO: .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    // Al eliminarlo, usará COLOR_DEPTH_WRITE por defecto para respetar el agua en Gráficos Rápido/Detallado
                    .createCompositeState(false)
    );
    public static final RenderType FLAME = RenderType.create(
            "flame",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true, // Al ser 'true' (sortOnUpload), el buffer ordenará los quads correctamente al activar Depth Write
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(
                            GameRenderer::getPositionColorTexLightmapShader
                    ))
                    .setTextureState(new RenderStateShard.TextureStateShard(
                            new ResourceLocation("theatricalextralights", "textures/particle/firework_core.png"),
                            false,
                            false
                    ))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    // 1. AÑADIDO: Fuerza el renderizado al búfer correcto en Gráficos Fabulosos
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    // 2. ELIMINADO: .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    // Al eliminarlo, usará COLOR_DEPTH_WRITE por defecto para respetar el agua en Gráficos Rápido/Detallado
                    .createCompositeState(false)
    );
}