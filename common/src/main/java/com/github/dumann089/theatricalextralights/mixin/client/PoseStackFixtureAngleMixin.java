package com.github.dumann089.theatricalextralights.mixin.client;

import com.github.dumann089.theatricalextralights.client.render.FixtureRenderAngleContext;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PoseStack.class)
public abstract class PoseStackFixtureAngleMixin {

    @Inject(
            method = "translate(DDD)V",
            at = @At("HEAD"),
            require = 0
    )
    private void extralights$detectDoublePivot(
            double x,
            double y,
            double z,
            CallbackInfo ci
    ) {
        FixtureRenderAngleContext.onPoseTranslate(x, y, z);
    }

    @Inject(
            method = "translate(FFF)V",
            at = @At("HEAD"),
            require = 0
    )
    private void extralights$detectFloatPivot(
            float x,
            float y,
            float z,
            CallbackInfo ci
    ) {
        FixtureRenderAngleContext.onPoseTranslate(x, y, z);
    }

    @ModifyVariable(
            method = "mulPose(Lorg/joml/Quaternionf;)V",
            at = @At("HEAD"),
            argsOnly = true,
            require = 0
    )
    private Quaternionf extralights$replaceFixtureRotation(
            Quaternionf original
    ) {
        return FixtureRenderAngleContext.adjustQuaternion(original);
    }
}