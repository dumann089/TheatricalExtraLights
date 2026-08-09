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

    // SHADERS
    public static final RenderType BEAM_SHADERS = RenderType.create(
            "beam_shaders",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(
                            GameRenderer::getPositionColorShader
                    ))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .createCompositeState(false)
    );

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

    private static Boolean shadersActiveCache = null;

    public static RenderType getBeam() {
        return isShadersActive() ? BEAM_SHADERS : BEAM_VANILLA;
    }

    public static boolean isShadersActive() {
        if (shadersActiveCache == null) {
            try {
                Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Object instance = irisApi.getMethod("getInstance").invoke(null);
                shadersActiveCache = (boolean) instance.getClass()
                        .getMethod("isShaderPackInUse").invoke(instance);
            } catch (Exception e) {
                shadersActiveCache = false;
            }
        }
        return shadersActiveCache;
    }
    public static void invalidateShadersCache() {
        shadersActiveCache = null;
    }
}