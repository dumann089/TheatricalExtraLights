package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.blockentities.ConfettiCannonBlockEntity;
import com.github.dumann089.theatricalextralights.client.model.ConfettiCannonModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.blocks.light.MovingLightBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class ConfettiCannonRenderer extends ExtraLightsRenderer<ConfettiCannonBlockEntity> {
    public static final ResourceLocation TEXTURE =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "textures/block/confetti_cannon.png");

    private final ConfettiCannonModel model;

    public ConfettiCannonRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        this.model = new ConfettiCannonModel(context.bakeLayer(ConfettiCannonModel.LAYER_LOCATION));
    }

    @Override
    public void render(ConfettiCannonBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource multiBufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        BlockState blockState = blockEntity.getBlockState();
        boolean isFlipped = blockEntity.isUpsideDown();
        boolean isHanging = ((HangableBlock) blockState.getBlock()).isHanging(blockEntity.getLevel(), blockEntity.getBlockPos());
        Direction facing = blockState.getValue(MovingLightBlock.FACING);
        VertexConsumer consumer = multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        renderModel(blockEntity, poseStack, consumer, facing, partialTick, isFlipped, blockState, isHanging, packedLight, packedOverlay);
        poseStack.popPose();
    }

   @Override
   public void renderModel(ConfettiCannonBlockEntity blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer,
                           Direction facing, float partialTicks, boolean isFlipped, BlockState blockState,
                           boolean isHanging, int packedLight, int packedOverlay) {
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
   }

    @Override
    public void preparePoseStack(ConfettiCannonBlockEntity blockEntity, PoseStack poseStack, Direction facing,
                                 float partialTicks, boolean isFlipped, BlockState blockState, boolean isHanging) {
    }
}
