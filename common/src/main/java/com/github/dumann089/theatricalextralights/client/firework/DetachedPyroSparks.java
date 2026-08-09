package com.github.dumann089.theatricalextralights.client.firework;

import com.github.dumann089.theatricalextralights.client.LensRenderTypes;
import com.github.dumann089.theatricalextralights.firework.FireworkRenderDistances;
import com.github.dumann089.theatricalextralights.firework.Spark;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Traînée orpheline + micro-chase pyro après suppression de l'entité serveur.
 */
public final class DetachedPyroSparks {
    private static final List<SparkGroup> SPARK_GROUPS = new ArrayList<>();
    private static final List<ChaseGroup> CHASE_GROUPS = new ArrayList<>();

    private static final class SparkGroup {
        private final List<Spark> sparks;
        private final int color;
        private final int maxAge;
        private int age;

        private SparkGroup(List<Spark> sparks, int color, int maxAge) {
            this.sparks = sparks;
            this.color = color;
            this.maxAge = maxAge;
        }

        private void tick() {
            for (Spark spark : sparks) {
                spark.tick();
            }
            sparks.removeIf(Spark::isDead);
            age++;
        }

        private boolean isDead() {
            return (sparks.isEmpty() && age > 8) || age >= maxAge;
        }
    }

    /** Perle fixe le long du parcours — s'éteint en douceur (effet chase). */
    private static final class ChaseBead {
        private final double x;
        private final double y;
        private final double z;
        private final int color;
        private final float scale;
        private final int maxAge;
        private final int startDelay;
        private int age;

        private ChaseBead(double x, double y, double z, int color, float scale, int maxAge, int startDelay) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.color = color;
            this.scale = scale;
            this.maxAge = maxAge;
            this.startDelay = startDelay;
        }

        private void tick() {
            age++;
        }

        private boolean isDead() {
            return age >= startDelay + maxAge;
        }

        private float getAlpha(float partialTick) {
            float life = age + partialTick;
            if (life < startDelay) {
                return 0.0f;
            }
            float t = (life - startDelay) / maxAge;
            if (t >= 1.0f) {
                return 0.0f;
            }
            float remaining = 1.0f - t;
            return remaining * remaining * (3.0f - 2.0f * remaining);
        }
    }

    private static final class ChaseGroup {
        private final List<ChaseBead> beads;
        private int age;

        private ChaseGroup(List<ChaseBead> beads) {
            this.beads = beads;
        }

        private void tick() {
            Iterator<ChaseBead> iterator = beads.iterator();
            while (iterator.hasNext()) {
                ChaseBead bead = iterator.next();
                bead.tick();
                if (bead.isDead()) {
                    iterator.remove();
                }
            }
            age++;
        }

        private boolean isDead() {
            return beads.isEmpty();
        }
    }

    private DetachedPyroSparks() {
    }

    public static void adoptComet(
            List<Spark> sparks,
            List<Vec3> path,
            double headX,
            double headY,
            double headZ,
            int color,
            float headStrength
    ) {
        if (sparks != null && !sparks.isEmpty()) {
            SPARK_GROUPS.add(new SparkGroup(sparks, color, 120));
        }

        List<ChaseBead> beads = new ArrayList<>();
        if (path != null && !path.isEmpty()) {
            int count = path.size();
            for (int i = 0; i < count; i++) {
                Vec3 point = path.get(i);
                float along = i / (float) Math.max(1, count - 1);
                float scale = 0.14f + 0.22f * along;
                int delay = (count - 1 - i) * 2;
                int lifetime = 44 + (int) (along * 24.0f);
                beads.add(new ChaseBead(point.x, point.y, point.z, color, scale, lifetime, delay));
            }
        }
        float headScale = 0.32f + 0.28f * Mth.clamp(headStrength, 0.0f, 1.0f);
        int headLife = 58 + (int) (headStrength * 28.0f);
        beads.add(new ChaseBead(headX, headY, headZ, color, headScale, headLife, 0));
        if (!beads.isEmpty()) {
            CHASE_GROUPS.add(new ChaseGroup(beads));
        }
    }

    public static void tick() {
        tickSparkGroups();
        tickChaseGroups();
    }

    private static void tickSparkGroups() {
        if (SPARK_GROUPS.isEmpty()) {
            return;
        }
        Iterator<SparkGroup> iterator = SPARK_GROUPS.iterator();
        while (iterator.hasNext()) {
            SparkGroup group = iterator.next();
            group.tick();
            if (group.isDead()) {
                iterator.remove();
            }
        }
    }

    private static void tickChaseGroups() {
        if (CHASE_GROUPS.isEmpty()) {
            return;
        }
        Iterator<ChaseGroup> iterator = CHASE_GROUPS.iterator();
        while (iterator.hasNext()) {
            ChaseGroup group = iterator.next();
            group.tick();
            if (group.isDead()) {
                iterator.remove();
            }
        }
    }

    public static void render(PoseStack poseStack, MultiBufferSource.BufferSource buffers, Camera camera, float partialTick) {
        if (SPARK_GROUPS.isEmpty() && CHASE_GROUPS.isEmpty()) {
            return;
        }
        Vec3 cameraPos = camera.getPosition();
        double maxDistSq = FireworkRenderDistances.clientSparkRangeSq();
        VertexConsumer consumer = buffers.getBuffer(LensRenderTypes.LENS);

        for (SparkGroup group : SPARK_GROUPS) {
            for (Spark spark : group.sparks) {
                float alpha = spark.getAlpha(partialTick);
                if (alpha <= 0.0f) {
                    continue;
                }
                Vec3 worldPos = spark.getPosition(partialTick);
                if (worldPos.distanceToSqr(cameraPos) > maxDistSq) {
                    continue;
                }
                float scale = spark.getScale(partialTick);
                renderWorldHalo(poseStack, consumer, camera, cameraPos, worldPos, group.color, alpha, scale);
            }
        }

        for (ChaseGroup group : CHASE_GROUPS) {
            for (ChaseBead bead : group.beads) {
                float alpha = bead.getAlpha(partialTick);
                if (alpha <= 0.0f) {
                    continue;
                }
                Vec3 worldPos = new Vec3(bead.x, bead.y, bead.z);
                if (worldPos.distanceToSqr(cameraPos) > maxDistSq) {
                    continue;
                }
                renderWorldHalo(poseStack, consumer, camera, cameraPos, worldPos, bead.color, alpha, bead.scale);
                renderWorldHalo(poseStack, consumer, camera, cameraPos, worldPos, bead.color, alpha * 0.55f, bead.scale * 1.55f);
            }
        }
    }

    private static void renderWorldHalo(
            PoseStack poseStack,
            VertexConsumer consumer,
            Camera camera,
            Vec3 cameraPos,
            Vec3 worldPos,
            int color,
            float alpha,
            float scale
    ) {
        poseStack.pushPose();
        poseStack.translate(
                worldPos.x - cameraPos.x,
                worldPos.y - cameraPos.y,
                worldPos.z - cameraPos.z
        );
        poseStack.mulPose(camera.rotation());
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
        renderHaloQuad(poseStack, consumer, color, alpha, scale);
        poseStack.popPose();
    }

    private static void renderHaloQuad(PoseStack poseStack, VertexConsumer consumer, int color, float alpha, float size) {
        Matrix4f matrix = poseStack.last().pose();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = Math.max(0, Math.min(255, (int) (alpha * 255.0f)));
        consumer.vertex(matrix, -size, size, 0.0f).color(r, g, b, a).uv(0.0f, 0.0f).uv2(LightTexture.FULL_BRIGHT).endVertex();
        consumer.vertex(matrix, size, size, 0.0f).color(r, g, b, a).uv(1.0f, 0.0f).uv2(LightTexture.FULL_BRIGHT).endVertex();
        consumer.vertex(matrix, size, -size, 0.0f).color(r, g, b, a).uv(1.0f, 1.0f).uv2(LightTexture.FULL_BRIGHT).endVertex();
        consumer.vertex(matrix, -size, -size, 0.0f).color(r, g, b, a).uv(0.0f, 1.0f).uv2(LightTexture.FULL_BRIGHT).endVertex();
    }
}
