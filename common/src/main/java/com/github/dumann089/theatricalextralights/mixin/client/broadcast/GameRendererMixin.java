package com.github.dumann089.theatricalextralights.mixin.client.broadcast;

import com.github.dumann089.theatricalextralights.client.broadcast.BroadcastCameraSession;
import com.github.dumann089.theatricalextralights.client.broadcast.AspectBars;
import com.github.dumann089.theatricalextralights.client.broadcast.BroadcastCameraHud;
import com.github.dumann089.theatricalextralights.client.broadcast.JumbotronFeed;
import com.github.dumann089.theatricalextralights.client.broadcast.SwitcherView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom da lente (campo de visão) e sem a mão do jogador na imagem da câmera. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    /**
     * Barras de proporção e HUD da câmera, no fim do quadro. Não dependem do hideGui (que fica ligado para
     * esconder a hotbar e o resto do HUD do Minecraft), então apertar Y esconde só o HUD da câmera.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void tel$drawCameraOutputOverlay(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        BroadcastCameraSession session = BroadcastCameraSession.getActive();
        Minecraft minecraft = Minecraft.getInstance();
        if (session == null || minecraft.screen != null) {
            return;
        }
        // No fim do quadro o Minecraft já desfez a preparação do desenho 2D (projeção e o recuo de 2000 em Z
        // que deixam a interface visível): sem refazer, nada aparece.
        Window window = minecraft.getWindow();
        float guiWidth = (float) ((double) window.getWidth() / window.getGuiScale());
        float guiHeight = (float) ((double) window.getHeight() / window.getGuiScale());
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0f, guiWidth, guiHeight, 0f, 1000f, 21000f),
                VertexSorting.ORTHOGRAPHIC_Z);
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        modelView.translate(0.0f, 0.0f, -2000.0f);
        RenderSystem.applyModelViewMatrix();
        try {
            GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
            AspectBars.draw(graphics, graphics.guiWidth(), graphics.guiHeight(), session.aspectRatio());
            if (!session.isHudHidden()) {
                BroadcastCameraHud.render(graphics, partialTicks);
            }
            graphics.flush();
        } finally {
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
        }
    }

    /** Mesa de corte, fade: antes do mundo principal, desenha o da câmera que está saindo. */
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void tel$renderOutgoingFeed(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        SwitcherView.renderExtraPasses((GameRenderer) (Object) this, partialTicks, nanoTime);
        JumbotronFeed.beforeLevel((GameRenderer) (Object) this, partialTicks, nanoTime);
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void tel$broadcastZoom(Camera camera, float partialTick, boolean useFovSetting,
                                   CallbackInfoReturnable<Double> cir) {
        BroadcastCameraSession session = BroadcastCameraSession.getActive();
        double multiplier;
        if (session != null) {
            multiplier = session.fovMultiplier();
        } else if (SwitcherView.isActive() || SwitcherView.isExtraPass()) {
            multiplier = SwitcherView.fovMultiplier();
        } else {
            return;
        }
        // Escala a tangente do meio-ângulo: um zoom 4x enxerga 1/4 da largura, não 1/4 do ângulo.
        double halfRad = Math.toRadians(cir.getReturnValue() * 0.5);
        double zoomed = 2.0 * Math.atan(Math.tan(halfRad) * multiplier);
        cir.setReturnValue(Math.toDegrees(zoomed));
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void tel$noHandInBroadcastCamera(PoseStack poseStack, Camera camera, float partialTick, CallbackInfo ci) {
        if (BroadcastCameraSession.isActive() || SwitcherView.isActive() || SwitcherView.isExtraPass()) {
            ci.cancel();
        }
    }
}
