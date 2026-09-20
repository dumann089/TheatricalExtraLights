package com.github.dumann089.theatricalextralights.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import com.github.dumann089.theatricalextralights.TheatricalExtraLightsClient;
import net.minecraft.world.inventory.InventoryMenu;

import static net.minecraft.client.renderer.RenderStateShard.*;

public class Beam2DRenderTypes {

    public static final RenderType FADER = RenderType.create(
            "Fader",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .createCompositeState(false)
    );

    public static final RenderType FLOOR_PATCH = RenderType.create(
            "extra_lights_floor_patch",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES,
            512,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .createCompositeState(false)
    );


    /**
     * Iris replaces mod-provided core shaders.  The vanilla lightning layer is
     * intentionally used for this path because Iris knows how to route it into
     * the active shader pack's translucent pass.
     */
    private static final RenderType IRIS_BEAM = RenderType.lightning();
    private static final ResourceLocation IRIS_VOLUME_TEXTURE =
            new ResourceLocation("theatricalextralights", "textures/gobos/generic_1/open.png");
    private static final RenderType IRIS_VOLUME =
            RenderType.entityTranslucentEmissive(IRIS_VOLUME_TEXTURE);

    public static RenderType getIrisVolumetric() {
        return IRIS_VOLUME;
    }

    // VANILLA
    public static final RenderType BEAM_VANILLA = RenderType.create(
            "beam_vanilla",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .createCompositeState(false)
    );

    public static RenderType getBeam() {
        return isShadersActive() ? IRIS_BEAM : BEAM_VANILLA;
    }

    public static boolean isShadersActive() {
        return IrisCompat.isShadersActive();
    }

    public static void invalidateShadersCache() {
        // no-op (API kept for callers)
    }
}