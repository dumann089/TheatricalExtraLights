package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.blockentities.LedFacadeBlockEntity;
import com.github.dumann089.theatricalextralights.client.render.LedFacadeRenderTypes;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Rendu de la façade LED (calqué sur les panneaux du mod) :
 * <ul>
 *   <li><b>Éteints</b> : {@link RenderType#entityCutout} + lumière du monde → surface de bloc
 *       normale (invisible dans le noir).</li>
 *   <li><b>Allumés</b> : plein-bright sans ombrage (couleurs vives) + émission Shimmer.</li>
 * </ul>
 * Le <b>lissage</b> n'est pas un filtre de texture mais un traitement morphologique : le motif
 * est suréchantillonné (résolution virtuelle {@code res*scale}) puis les pixels sont arrondis /
 * reliés (flou box du masque + seuil, couleur moyennée sur les LED voisines). Le niveau contrôle
 * la résolution virtuelle, le rayon de connexion et le seuil.
 */
public class LedFacadeRenderer extends ExtraLightsRenderer<LedFacadeBlockEntity> {

    private static final int STALE_FRAMES = 600;
    private static final float OUT = 0.02f;
    private static final float LIT_OUT = 0.026f;
    private static final int OFF_GREY = 0xFF2A2A2A; // gris LED éteint (natif ABGR, symétrique)
    private static final int MAX_VRES = 256;        // borne la texture virtuelle
    private static final ResourceLocation GEAR_TEXTURE =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "textures/gui/led_facade_gear.png");

    private final Map<BlockPos, Panel> panels = new HashMap<>();
    private long frame;

    public LedFacadeRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LedFacadeBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource multiBufferSource, int packedLight, int packedOverlay) {
        Direction front = be.getBlockState().getValue(HorizontalDirectionalBlock.FACING).getOpposite();
        Matrix4f matrix = poseStack.last().pose();
        int worldLight = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos());

        if (be.hasActivePixels()) {
            Panel panel = acquirePanel(be);
            VertexConsumer off = multiBufferSource.getBuffer(RenderType.entityCutout(panel.offLocation));
            emitFace(off, matrix, worldLight, true, front, OUT, 0f, 0f, 1f, 1f, 1f, 0f, 0f, 1f);
            VertexConsumer lit = multiBufferSource.getBuffer(LedFacadeRenderTypes.surface(panel.litLocation));
            emitFace(lit, matrix, LightTexture.FULL_BRIGHT, false, front, LIT_OUT, 0f, 0f, 1f, 1f, 1f, 0f, 0f, 1f);
        } else {
            VertexConsumer vc = multiBufferSource.getBuffer(RenderType.entityCutout(GEAR_TEXTURE));
            emitFace(vc, matrix, worldLight, true, front, OUT, 0.34f, 0.34f, 0.66f, 0.66f, 0f, 0f, 1f, 1f);
        }

        sweep();
    }

    private void emitFace(VertexConsumer vc, Matrix4f m, int light, boolean entityFormat, Direction facing, float depth,
                          float pu0, float pv0, float pu1, float pv1,
                          float tu0, float tv0, float tu1, float tv1) {
        float ox, oy, oz, rx, rz, nx, nz;
        switch (facing) {
            case SOUTH -> { ox = 0;          oy = 1; oz = 1 - depth; rx = 1;  rz = 0;  nx = 0;  nz = 1; }
            case WEST  -> { ox = depth;      oy = 1; oz = 0;         rx = 0;  rz = 1;  nx = -1; nz = 0; }
            case EAST  -> { ox = 1 - depth;  oy = 1; oz = 1;         rx = 0;  rz = -1; nx = 1;  nz = 0; }
            default    -> { ox = 1;          oy = 1; oz = depth;     rx = -1; rz = 0;  nx = 0;  nz = -1; } // NORTH
        }
        vertex(vc, m, light, entityFormat, nx, nz, ox + rx * pu0, oy - pv0, oz + rz * pu0, tu0, tv0); // haut-gauche
        vertex(vc, m, light, entityFormat, nx, nz, ox + rx * pu1, oy - pv0, oz + rz * pu1, tu1, tv0); // haut-droite
        vertex(vc, m, light, entityFormat, nx, nz, ox + rx * pu1, oy - pv1, oz + rz * pu1, tu1, tv1); // bas-droite
        vertex(vc, m, light, entityFormat, nx, nz, ox + rx * pu0, oy - pv1, oz + rz * pu0, tu0, tv1); // bas-gauche
    }

    /**
     * {@code entityFormat=true} → format entité (ombrage diffus) pour les éteints ; {@code false}
     * → POSITION_COLOR_TEX_LIGHTMAP (sans ombrage) pour les allumés (couleurs pleines).
     */
    private void vertex(VertexConsumer vc, Matrix4f m, int light, boolean entityFormat, float nx, float nz,
                        float x, float y, float z, float u, float v) {
        var vb = vc.vertex(m, x, y, z).color(255, 255, 255, 255).uv(u, v);
        if (entityFormat) {
            vb.overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx, 0f, nz).endVertex();
        } else {
            vb.uv2(light).endVertex();
        }
    }

    // Requis par la classe abstraite mais inutilisés (le rendu passe entièrement par render()).
    @Override
    public void renderModel(LedFacadeBlockEntity be, PoseStack poseStack, VertexConsumer vertexConsumer,
                            Direction facing, float partialTicks, boolean isFlipped, BlockState blockState,
                            boolean isHanging, int packedLight, int packedOverlay) {
    }

    @Override
    public void preparePoseStack(LedFacadeBlockEntity be, PoseStack poseStack, Direction facing,
                                 float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging) {
    }

    // ─── Paramètres de lissage par niveau ──────────────────────────────────────

    private static int scaleFor(int smoothing) {
        return switch (smoothing) {
            case 1 -> 4;   // doux  (res*4, ex. 32 → 128)
            case 2 -> 8;   // très doux (res*8, ex. 32 → 256)
            default -> 1;  // net
        };
    }

    private static int radiusFor(int smoothing) {
        return switch (smoothing) {
            case 1 -> 3;
            case 2 -> 6;
            default -> 0;
        };
    }

    private static float thresholdFor(int smoothing) {
        return switch (smoothing) {
            case 1 -> 0.45f;
            case 2 -> 0.42f;
            default -> 0.5f;
        };
    }

    // ─── Textures dynamiques par façade (éteinte + allumée) ────────────────────

    private Panel acquirePanel(LedFacadeBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        int res = be.getResolution();
        int sm = be.getSmoothing();
        int scale = scaleFor(sm);
        int vRes = Math.min(res * scale, MAX_VRES);

        Panel panel = panels.get(pos);
        if (panel == null || panel.vRes != vRes) {
            if (panel != null) {
                release(panel);
            }
            panel = new Panel();
            panel.resolution = res;
            panel.vRes = vRes;
            String key = "led_facade/" + Long.toHexString(pos.asLong());
            panel.offLocation = new ResourceLocation(TheatricalExtraLights.MOD_ID, key + "_off");
            panel.litLocation = new ResourceLocation(TheatricalExtraLights.MOD_ID, key + "_lit");
            panel.offTexture = new DynamicTexture(vRes, vRes, false);
            panel.litTexture = new DynamicTexture(vRes, vRes, false);
            Minecraft.getInstance().getTextureManager().register(panel.offLocation, panel.offTexture);
            Minecraft.getInstance().getTextureManager().register(panel.litLocation, panel.litTexture);
            panel.offSig = Integer.MIN_VALUE;
            panel.litSig = Integer.MIN_VALUE;
            panels.put(pos, panel);
        }

        int radius = radiusFor(sm);
        float threshold = thresholdFor(sm);
        boolean linear = sm >= 1;

        int offSig = (res * 31 + be.getActivePixels().hashCode()) * 31 + sm;
        if (offSig != panel.offSig) {
            writeInts(panel.offTexture, buildSmoothed(be, res, scale, vRes, radius, threshold, false), vRes);
            panel.offTexture.upload();
            panel.offTexture.setFilter(linear, false);
            panel.offSig = offSig;
        }
        int litSig = offSig * 31 + be.getColorVersion();
        if (litSig != panel.litSig) {
            writeInts(panel.litTexture, buildSmoothed(be, res, scale, vRes, radius, threshold, true), vRes);
            panel.litTexture.upload();
            panel.litTexture.setFilter(linear, false);
            panel.litSig = litSig;
        }
        panel.lastAccess = frame;
        return panel;
    }

    /**
     * Génère la texture (natif ABGR, taille {@code vRes}) par lissage morphologique du motif :
     * suréchantillonnage puis flou box du masque + seuil (arrondit/relie), couleur moyennée sur
     * les LED contributrices. {@code lit=false} → gris éteint ; {@code lit=true} → couleur DMX.
     */
    private int[] buildSmoothed(LedFacadeBlockEntity be, int res, int scale, int vRes,
                                int radius, float threshold, boolean lit) {
        float[] mask = new float[vRes * vRes];
        float[] mr = lit ? new float[vRes * vRes] : null;
        float[] mg = lit ? new float[vRes * vRes] : null;
        float[] mb = lit ? new float[vRes * vRes] : null;

        for (int vy = 0; vy < vRes; vy++) {
            for (int vx = 0; vx < vRes; vx++) {
                int idx = (vy / scale) * res + (vx / scale);
                boolean on;
                if (lit) {
                    int argb = be.getPixelArgb(idx);
                    on = be.isPixelActive(idx) && (argb & 0xFFFFFF) != 0;
                    if (on) {
                        int p = vy * vRes + vx;
                        mask[p] = 1f;
                        mr[p] = (argb >> 16) & 0xFF;
                        mg[p] = (argb >> 8) & 0xFF;
                        mb[p] = argb & 0xFF;
                    }
                } else {
                    on = be.isPixelActive(idx);
                    if (on) {
                        mask[vy * vRes + vx] = 1f;
                    }
                }
            }
        }

        int[] out = new int[vRes * vRes];
        if (scale == 1 || radius <= 0) {
            // Net : aucun lissage.
            for (int p = 0; p < out.length; p++) {
                if (mask[p] > 0) {
                    out[p] = lit ? nativeAbgr((int) mr[p], (int) mg[p], (int) mb[p]) : OFF_GREY;
                }
            }
            return out;
        }

        // Sommes fenêtrées séparables (préfixes) : coût indépendant du rayon.
        float[] sMask = boxSum(mask, vRes, radius);
        float[] sR = lit ? boxSum(mr, vRes, radius) : null;
        float[] sG = lit ? boxSum(mg, vRes, radius) : null;
        float[] sB = lit ? boxSum(mb, vRes, radius) : null;

        for (int vy = 0; vy < vRes; vy++) {
            int loy = Math.max(0, vy - radius), hiy = Math.min(vRes - 1, vy + radius);
            for (int vx = 0; vx < vRes; vx++) {
                int o = vy * vRes + vx;
                int lox = Math.max(0, vx - radius), hix = Math.min(vRes - 1, vx + radius);
                int count = (hix - lox + 1) * (hiy - loy + 1);
                float coverage = sMask[o] / count;
                if (coverage >= threshold) {
                    if (lit) {
                        float wsum = sMask[o];
                        out[o] = wsum > 0
                                ? nativeAbgr((int) (sR[o] / wsum), (int) (sG[o] / wsum), (int) (sB[o] / wsum))
                                : nativeAbgr(255, 255, 255);
                    } else {
                        out[o] = OFF_GREY;
                    }
                }
            }
        }
        return out;
    }

    /** Somme box (2r+1)² séparable via préfixes — O(V²), indépendant du rayon. */
    private float[] boxSum(float[] src, int vRes, int r) {
        float[] h = new float[vRes * vRes];
        float[] pre = new float[vRes + 1];
        for (int y = 0; y < vRes; y++) {
            int row = y * vRes;
            for (int x = 0; x < vRes; x++) {
                pre[x + 1] = pre[x] + src[row + x];
            }
            for (int x = 0; x < vRes; x++) {
                int lo = Math.max(0, x - r), hi = Math.min(vRes - 1, x + r);
                h[row + x] = pre[hi + 1] - pre[lo];
            }
        }
        float[] out = new float[vRes * vRes];
        for (int x = 0; x < vRes; x++) {
            pre[0] = 0;
            for (int y = 0; y < vRes; y++) {
                pre[y + 1] = pre[y] + h[y * vRes + x];
            }
            for (int y = 0; y < vRes; y++) {
                int lo = Math.max(0, y - r), hi = Math.min(vRes - 1, y + r);
                out[y * vRes + x] = pre[hi + 1] - pre[lo];
            }
        }
        return out;
    }

    private static int nativeAbgr(int r, int g, int b) {
        return 0xFF000000 | (b << 16) | (g << 8) | r; // ARGB → ABGR natif
    }

    private void writeInts(DynamicTexture texture, int[] px, int vRes) {
        NativeImage image = texture.getPixels();
        if (image == null) {
            return;
        }
        for (int y = 0; y < vRes; y++) {
            for (int x = 0; x < vRes; x++) {
                image.setPixelRGBA(x, y, px[y * vRes + x]);
            }
        }
    }

    private void sweep() {
        frame++;
        if (frame % 200 != 0 || panels.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Panel>> it = panels.entrySet().iterator();
        while (it.hasNext()) {
            Panel panel = it.next().getValue();
            if (frame - panel.lastAccess > STALE_FRAMES) {
                release(panel);
                it.remove();
            }
        }
    }

    private void release(Panel panel) {
        var tm = Minecraft.getInstance().getTextureManager();
        if (panel.offLocation != null) tm.release(panel.offLocation);
        if (panel.litLocation != null) tm.release(panel.litLocation);
        if (panel.offTexture != null) panel.offTexture.close();
        if (panel.litTexture != null) panel.litTexture.close();
    }

    private static final class Panel {
        DynamicTexture offTexture, litTexture;
        ResourceLocation offLocation, litLocation;
        int resolution;
        int vRes;
        int offSig, litSig;
        long lastAccess;
    }
}
