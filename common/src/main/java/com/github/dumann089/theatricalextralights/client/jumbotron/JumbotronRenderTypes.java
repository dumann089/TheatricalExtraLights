package com.github.dumann089.theatricalextralights.client.jumbotron;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.client.broadcast.JumbotronFeed;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Render types da tela do jumbotron: imagem opaca, imagem com transparência (fade) e fundo liso. */
public final class JumbotronRenderTypes extends RenderType {

    private static final RenderType[] OPAQUE = new RenderType[JumbotronFeed.MAX_CHANNELS];
    private static final RenderType[] BLEND = new RenderType[JumbotronFeed.MAX_CHANNELS];
    private static RenderType solid;

    /** Imagem do canal do slot, opaca (sem mistura): a mais barata. */
    public static RenderType opaque(int slot) {
        if (OPAQUE[slot] == null) {
            OPAQUE[slot] = build(slot, false);
        }
        return OPAQUE[slot];
    }

    /** Imagem do canal do slot com transparência, para a dissolução entre câmeras. */
    public static RenderType blend(int slot) {
        if (BLEND[slot] == null) {
            BLEND[slot] = build(slot, true);
        }
        return BLEND[slot];
    }

    /** Fundo escuro para "sem sinal". */
    public static RenderType solid() {
        if (solid == null) {
            solid = RenderType.create(
                    "tel_jumbotron_solid",
                    DefaultVertexFormat.POSITION_COLOR,
                    VertexFormat.Mode.QUADS,
                    256, false, false,
                    RenderType.CompositeState.builder()
                            .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                            .setTransparencyState(NO_TRANSPARENCY)
                            .setDepthTestState(LEQUAL_DEPTH_TEST)
                            .setCullState(NO_CULL)
                            .setLightmapState(NO_LIGHTMAP)
                            .setWriteMaskState(COLOR_DEPTH_WRITE)
                            .createCompositeState(false));
        }
        return solid;
    }

    private static RenderType build(int slot, boolean blend) {
        ResourceLocation id = new ResourceLocation(TheatricalExtraLights.MOD_ID, "jumbotron_feed/" + slot);
        // Registra a "textura" do slot (uma vez por render type; registrar de novo só substitui).
        Minecraft.getInstance().getTextureManager()
                .register(id, new JumbotronFeedTexture(() -> JumbotronFeed.textureIdForSlot(slot)));
        return RenderType.create(
                "tel_jumbotron_feed_" + slot + (blend ? "_blend" : ""),
                DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS,
                256, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorTexShader))
                        .setTextureState(new TextureStateShard(id, true, false))
                        .setTransparencyState(blend ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setWriteMaskState(blend ? COLOR_WRITE : COLOR_DEPTH_WRITE)
                        .createCompositeState(false));
    }

    private JumbotronRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                 boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState,
                                 Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
}
