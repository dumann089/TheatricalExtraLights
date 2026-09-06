package com.github.dumann089.theatricalextralights.client;

import com.github.dumann089.theatricalextralights.client.blockentities.ConfettiCannonRenderer;
import com.github.dumann089.theatricalextralights.client.model.ConfettiCannonModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ConfettiCannonItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final ConfettiCannonModel model;

    public ConfettiCannonItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
        this.model = new ConfettiCannonModel(models.bakeLayer(ConfettiCannonModel.LAYER_LOCATION));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (displayContext == ItemDisplayContext.GUI) {
            poseStack.translate(0.5F, 0.55F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(20.0F));
            poseStack.scale(0.85F, 0.85F, 0.85F);
        } else {
            poseStack.translate(0.5F, 0.5F, 0.5F);
        }
        ConfettiCannonRenderTransforms.applyBlockbenchEntityTransform(poseStack);
        model.renderToBuffer(
                poseStack,
                buffer.getBuffer(RenderType.entityCutoutNoCull(ConfettiCannonRenderer.TEXTURE)),
                packedLight,
                packedOverlay,
                1.0F, 1.0F, 1.0F, 1.0F
        );
        poseStack.popPose();
    }
}
