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

    private ExtraLightsRenderTypes() {
    }
}
