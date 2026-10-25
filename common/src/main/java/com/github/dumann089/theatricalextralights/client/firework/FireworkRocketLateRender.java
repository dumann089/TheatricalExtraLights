package com.github.dumann089.theatricalextralights.client.firework;

import com.github.dumann089.theatricalextralights.client.entities.FireworkRocketRenderer;
import com.github.dumann089.theatricalextralights.entities.FireworkRocketEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Desenha halos/faíscas dos foguetes no fim do frame (depois de block entities e partículas),
 * para o depth buffer já conter as fixtures.
 */
public final class FireworkRocketLateRender {
    private static volatile boolean active;

    private FireworkRocketLateRender() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void render(PoseStack poseStack, MultiBufferSource.BufferSource buffers, Camera camera,
                              float partialTick, @Nullable Frustum frustum) {
        active = true;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        Vec3 cameraPos = camera.getPosition();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof FireworkRocketEntity rocket)) {
                continue;
            }
            EntityRenderer<? super FireworkRocketEntity> renderer = minecraft.getEntityRenderDispatcher().getRenderer(rocket);
            if (!(renderer instanceof FireworkRocketRenderer rocketRenderer)) {
                continue;
            }
            if (frustum != null && !rocketRenderer.shouldRender(rocket, frustum, cameraPos.x, cameraPos.y, cameraPos.z)) {
                continue;
            }
            double x = Mth.lerp(partialTick, rocket.xOld, rocket.getX()) - cameraPos.x;
            double y = Mth.lerp(partialTick, rocket.yOld, rocket.getY()) - cameraPos.y;
            double z = Mth.lerp(partialTick, rocket.zOld, rocket.getZ()) - cameraPos.z;
            poseStack.pushPose();
            poseStack.translate(x, y, z);
            rocketRenderer.renderEffects(rocket, partialTick, poseStack, buffers);
            poseStack.popPose();
        }
    }
}