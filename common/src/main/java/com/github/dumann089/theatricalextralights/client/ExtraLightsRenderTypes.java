package com.github.dumann089.theatricalextralights.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * Render types partages des faisceaux plats d'Extra Lights.
 *
 * <p>{@link #BEAM} reprend le TheatricalBeam de Theatrical (position + couleur, melange
 * translucide) mais n'ecrit pas la profondeur : un quad de faisceau qui ecrit le depth buffer
 * troue les faisceaux raymarch et les lasers qui passent derriere lui, puisque ces derniers
 * lisent la profondeur de la scene pour s'occulter.
 */
public final class ExtraLightsRenderTypes {

    public static final RenderType BEAM = RenderType.create(
            "extralights_beam",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.CULL)
                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                    .setOutputState(RenderStateShard.TRANSLUCENT_TARGET)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false)
    );

    /**
     * Halo additif des sources (strobes) : s'ajoute a la scene au lieu de la recouvrir, sans
     * ecrire la profondeur ni cacher les faces arriere pour qu'un billboard reste visible.
     */
    public static final RenderType GLOW = RenderType.create(
            "extralights_glow",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            1024,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                    .setOutputState(RenderStateShard.TRANSLUCENT_TARGET)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false)
    );

    /**
     * Face LED allumee (pixels, segments) : opaque et pleine luminosite, elle recouvre la texture
     * de l'appareil au lieu de s'y ajouter, et ecrit la profondeur comme une surface.
     */
    public static final RenderType LED_FACE = RenderType.create(
            "extralights_led_face",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            4096,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(false)
    );

    private ExtraLightsRenderTypes() {
    }
}
