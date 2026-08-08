package com.github.dumann089.theatricalextralights.client.render;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * RenderTypes de la surface d'une façade LED : quad texturé plein-bright (lightmap forcé),
 * sans face culling, alpha translucide. Un RenderType est mémoïsé par texture (surface dynamique
 * par BlockEntity + icône roue crantée statique).
 */
public class LedFacadeRenderTypes {

    private static final Map<ResourceLocation, RenderType> CACHE = new HashMap<>();

    public static final ResourceLocation GEAR_TEXTURE =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "textures/gui/led_facade_gear.png");

    public static RenderType surface(ResourceLocation texture) {
        return CACHE.computeIfAbsent(texture, LedFacadeRenderTypes::build);
    }

    public static RenderType gear() {
        return surface(GEAR_TEXTURE);
    }

    private static RenderType build(ResourceLocation texture) {
        return RenderType.create(
                "led_facade_surface",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(
                                GameRenderer::getPositionColorTexLightmapShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false)
        );
    }
}
