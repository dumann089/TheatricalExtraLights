package dev.imabad.theatrical.client.blockentities;

import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.blocks.light.MovingLightBlock;
import dev.imabad.theatrical.client.LazyRenderers;
import dev.imabad.theatrical.client.TheatricalRenderTypes;
import dev.imabad.theatrical.config.TheatricalConfig;
import net.minecraft.class_1087;
import net.minecraft.class_1921;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_5614;
import net.minecraft.class_827;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public abstract class FixtureRenderer<T extends BaseLightBlockEntity> implements class_827<T> {
    private final Double beamOpacity = TheatricalConfig.INSTANCE.CLIENT.beamOpacity;

    public FixtureRenderer(class_5614.class_5615 context) {
    }
    @Override
    public void render(T blockEntity, float partialTick, class_4587 poseStack, class_4597 multiBufferSource, int packedLight, int packedOverlay) {
        poseStack.method_22903();
        class_4588 vertexConsumer = multiBufferSource.getBuffer(class_1921.method_23581());
        class_2680 blockState = blockEntity.method_11010();
        boolean isFlipped = blockEntity.isUpsideDown();
        boolean isHanging = ((HangableBlock) blockState.method_26204()).isHanging(blockEntity.method_10997(), blockEntity.method_11016());
        class_2350 facing = blockState.method_11654(MovingLightBlock.field_11177);
        renderModel(blockEntity, poseStack, vertexConsumer, facing, partialTick, isFlipped, blockState, isHanging, packedLight, packedOverlay);
        beforeRenderBeam(blockEntity, poseStack, vertexConsumer, multiBufferSource, facing, partialTick, isFlipped, blockState, isHanging, packedLight, packedOverlay);
        if(shouldRenderBeam(blockEntity)){
            LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {
                @Override
                public void render(class_4597.class_4598 bufferSource, class_4587 poseStack, class_4184 camera, float partialTick) {
                    poseStack.method_22903();
                    class_243 offset = class_243.method_24954(blockEntity.method_11016()).method_1020(camera.method_19326());
                    poseStack.method_22904(offset.field_1352, offset.field_1351, offset.field_1350);
                    preparePoseStack(blockEntity, poseStack, facing, partialTick, isFlipped, blockState, isHanging);
                    class_4588 beamConsumer = bufferSource.getBuffer(TheatricalRenderTypes.BEAM);
                    poseStack.method_46416(blockEntity.getFixture().getBeamStartPosition()[0], blockEntity.getFixture().getBeamStartPosition()[1], blockEntity.getFixture().getBeamStartPosition()[2]);
                    float intensity = blockEntity.getIntensity();
                    int color = blockEntity.getColour();
                    if(color != 0) {
                        renderLightBeam(beamConsumer, poseStack, blockEntity, partialTick, (float) ((intensity * beamOpacity) / 255f), blockEntity.getFixture().getBeamWidth(), (float) blockEntity.getDistance(), color);
                    }
                    poseStack.method_22909();
                }

                @Override
                public class_243 getPos(float partialTick) {
                    return blockEntity.method_11016().method_46558();
                }
            });
        }
        poseStack.method_22909();
    }

    public abstract void renderModel(T blockEntity, class_4587 poseStack, class_4588 vertexConsumer, class_2350 facing, float partialTicks, boolean isFlipped, class_2680 blockState, boolean isHanging, int packedLight, int packedOverlay);

    public abstract void preparePoseStack(T blockEntity, class_4587 poseStack, class_2350 facing, float partialTicks, boolean isFlipped, class_2680 blockState, boolean isHanging);

    public void beforeRenderBeam(T blockEntity, class_4587 poseStack, class_4588 vertexConsumer,
                                 class_4597 multiBufferSource, class_2350 facing, float partialTicks,
                                 boolean isFlipped, class_2680 blockstate, boolean isHanging, int packedLight,
                                 int packedOverlay) {}

    public boolean shouldRenderBeam(T blockEntity){
        return blockEntity.getIntensity() > 0 && blockEntity.getFixture().hasBeam();
    }

    protected void minecraftRenderModel(class_4587 poseStack, class_4588 vertexConsumer, class_2680 blockState, class_1087 model, int packedLight, int packedOverlay){
        class_310.method_1551().method_1541().method_3350().method_3367(poseStack.method_23760(), vertexConsumer, blockState, model, 1, 1, 1, packedLight, packedOverlay);
    }

    protected void renderLightBeam(class_4588 builder, class_4587 stack, T tileEntityFixture, float partialTicks, float alpha, float beamSize, float length, int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (alpha * 255);
        Matrix4f m = stack.method_23760().method_23761();
        Matrix3f normal = stack.method_23760().method_23762();
        float endMultiplier = beamSize * tileEntityFixture.getFocus();
        addVertex(builder, m, normal, r, g, b, 0, beamSize * endMultiplier, beamSize * endMultiplier, -length);
        addVertex(builder, m, normal, r, g, b, a,  beamSize, beamSize, 0);
        addVertex(builder, m, normal, r, g, b, a, beamSize, -beamSize, 0);
        addVertex(builder, m, normal, r, g, b, 0,beamSize * endMultiplier, -beamSize * endMultiplier, -length);

        addVertex(builder, m, normal, r, g, b, 0, -beamSize * endMultiplier, -beamSize * endMultiplier, -length);
        addVertex(builder, m, normal, r, g, b, a, -beamSize, -beamSize, 0);
        addVertex(builder, m, normal, r, g, b, a, -beamSize, beamSize, 0);
        addVertex(builder, m, normal, r, g, b, 0, -beamSize * endMultiplier, beamSize * endMultiplier, -length);

        addVertex(builder, m, normal, r, g, b, 0, -beamSize * endMultiplier, beamSize * endMultiplier, -length);
        addVertex(builder, m, normal, r, g, b, a, -beamSize, beamSize, 0);
        addVertex(builder, m, normal, r, g, b, a, beamSize, beamSize, 0);
        addVertex(builder, m, normal, r, g, b, 0, beamSize * endMultiplier, beamSize * endMultiplier, -length);

        addVertex(builder, m, normal, r, g, b, 0, beamSize * endMultiplier, -beamSize * endMultiplier, -length);
        addVertex(builder, m, normal, r, g, b, a, beamSize, -beamSize, 0);
        addVertex(builder, m, normal, r, g, b, a, -beamSize, -beamSize, 0);
        addVertex(builder, m, normal, r, g, b, 0, -beamSize * endMultiplier, -beamSize * endMultiplier, -length);
    }

    protected void addVertex(class_4588 builder, Matrix4f matrix4f, Matrix3f matrix3f, int r, int g, int b, int a, float x, float y, float z) {
        builder.method_22918(matrix4f, x, y, z).method_1336(r, g, b, a).method_1344();
    }

    @Override
    public boolean shouldRenderOffScreen(T blockEntity) {
        return true;
    }

    @Override
    public int method_33893() {
        return TheatricalConfig.INSTANCE.CLIENT.renderDistance;
    }

    @Override
    public boolean shouldRender(T blockEntity, class_243 cameraPos) {
        return true;
    }
}
