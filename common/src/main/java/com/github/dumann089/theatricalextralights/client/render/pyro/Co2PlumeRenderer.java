package com.github.dumann089.theatricalextralights.client.render.pyro;

import com.github.dumann089.theatricalextralights.client.ModShaders;
import com.github.dumann089.theatricalextralights.client.render.beam.raymarch.SceneDepthCopy;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.imabad.theatrical.client.LazyRenderers;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Panache CO2 volumetrique : un cone de brouillard raymarche dans le shader {@code co2_plume},
 * dessine en alpha premultiplie par-dessus la scene, occlusion par la copie de profondeur.
 * Meme pipeline que les faisceaux raymarch (boite proxy, profondeur copiee), mais la fumee
 * couvre au lieu d'ajouter de la lumiere.
 */
public final class Co2PlumeRenderer extends LazyRenderers.LazyRenderer {
    public static final int MAX_PLUMES = 64;

    /** Rayon a la bouche : la buse fait 2/16 de bloc. */
    public static final float NOZZLE_RADIUS = 0.06f;
    /** Rayon apres la detente eclair, un demi-bloc de large. */
    public static final float FLASH_RADIUS = 0.24f;
    /** Tangente du demi-angle du cone, 6 degres. */
    public static final float CONE_TAN = 0.105f;

    private static final Co2PlumeRenderer INSTANCE = new Co2PlumeRenderer();

    private final Slot[] slots = new Slot[MAX_PLUMES];
    private final int[] drawOrder = new int[MAX_PLUMES];
    private int count;

    private final Matrix4f invProj = new Matrix4f();
    private final Vector4f tmp = new Vector4f();
    private final Vector3f nozzleV = new Vector3f();
    private final Vector3f axisV = new Vector3f();
    private final Vector3f sideUV = new Vector3f();
    private final Vector3f sideVV = new Vector3f();
    private final Vector3f upV = new Vector3f();

    private Co2PlumeRenderer() {
        for (int i = 0; i < MAX_PLUMES; i++) {
            slots[i] = new Slot();
        }
    }

    /** Vrai quand le panache volumetrique peut etre dessine ; sinon les particules prennent le relais. */
    public static boolean available() {
        return TheatricalExtraLightsConfig.isVolumetricBeamEnabled() && ModShaders.canUseCo2Plume();
    }

    /**
     * @param nozzle   bouche de la buse, monde
     * @param axis     axe du jet, unitaire
     * @param length   distance du front depuis la buse, blocs
     * @param cutFront front de coupure depuis la buse, blocs ; negatif tant que la vanne est ouverte
     * @param pressure 0..1
     * @param dissipate 0 vanne ouverte .. 1 nuage dissipe, apres la fermeture
     * @param flowClock horloge du gaz en ticks, qui ralentit une fois la vanne fermee
     * @param exitSpeed vitesse de sortie du gaz, blocs/tick
     * @param baseFlow  debit courant a la buse rapporte a la pression du panache, 0..1
     */
    public static void submit(Vec3 nozzle, Vector3f axis, float length, float cutFront, float pressure, float dissipate, float flowClock, float exitSpeed, float baseFlow) {
        INSTANCE.enqueue(nozzle, axis, length, cutFront, pressure, dissipate, flowClock, exitSpeed, baseFlow);
    }

    private void enqueue(Vec3 nozzle, Vector3f axis, float length, float cutFront, float pressure, float dissipate, float flowClock, float exitSpeed, float baseFlow) {
        if (!available() || length <= 0.05f) {
            return;
        }
        if (count == 0) {
            SceneDepthCopy.beginFrame();
            LazyRenderers.addLazyRender(this);
        }
        if (count >= MAX_PLUMES) {
            return;
        }
        Slot s = slots[count++];
        s.nozzleX = (float) nozzle.x;
        s.nozzleY = (float) nozzle.y;
        s.nozzleZ = (float) nozzle.z;
        s.axisX = axis.x();
        s.axisY = axis.y();
        s.axisZ = axis.z();
        s.length = length;
        s.cutFront = cutFront;
        s.pressure = pressure;
        s.dissipate = dissipate;
        s.flowClock = flowClock;
        s.exitSpeed = exitSpeed;
        s.baseFlow = baseFlow;
    }

    @Override
    public void render(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Camera camera, float partialTick) {
        try {
            if (count == 0) {
                return;
            }
            SceneDepthCopy.flushOpaqueAndCapture(bufferSource);
            bufferSource.endBatch();
            ShaderInstance shader = ModShaders.co2PlumeShader;
            if (!SceneDepthCopy.hasDepth() || shader == null) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            invProj.set(RenderSystem.getProjectionMatrix());
            invProj.invert();
            float time = (mc.level != null ? mc.level.getGameTime() : 0) + partialTick;
            var main = mc.getMainRenderTarget();
            int steps = Math.max(16, TheatricalExtraLightsConfig.getRaymarchSteps());
            Vec3 camPos = camera.getPosition();

            sortFarToNear(camPos);

            poseStack.pushPose();
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
            Matrix4f viewMat = new Matrix4f(poseStack.last().pose());
            int depthTex = SceneDepthCopy.getDepthTextureId();
            RenderType renderType = ModShaders.getCo2PlumeRenderType();

            for (int n = 0; n < count; n++) {
                Slot s = slots[drawOrder[n]];

                // Base fixe dans le monde, perpendiculaire a l'axe : le bruit du shader y est stable.
                Vector3f axis = new Vector3f(s.axisX, s.axisY, s.axisZ);
                Vector3f helper = Math.abs(axis.y()) < 0.9f ? new Vector3f(0f, 1f, 0f) : new Vector3f(1f, 0f, 0f);
                Vector3f sideU = new Vector3f(axis).cross(helper).normalize();
                Vector3f sideV = new Vector3f(axis).cross(sideU).normalize();

                transformPoint(viewMat, s.nozzleX, s.nozzleY, s.nozzleZ, nozzleV);
                transformDir(viewMat, s.axisX, s.axisY, s.axisZ, axisV);
                transformDir(viewMat, sideU.x(), sideU.y(), sideU.z(), sideUV);
                transformDir(viewMat, sideV.x(), sideV.y(), sideV.z(), sideVV);
                transformDir(viewMat, 0f, 1f, 0f, upV);

                shader.safeGetUniform("InvProjMat").set(invProj);
                shader.safeGetUniform("ScreenSize").set((float) main.width, (float) main.height);
                shader.safeGetUniform("StepCount").set(steps);
                shader.safeGetUniform("Time").set(time);
                shader.safeGetUniform("Nozzle").set(nozzleV.x, nozzleV.y, nozzleV.z);
                shader.safeGetUniform("Axis").set(axisV.x, axisV.y, axisV.z);
                shader.safeGetUniform("SideU").set(sideUV.x, sideUV.y, sideUV.z);
                shader.safeGetUniform("SideV").set(sideVV.x, sideVV.y, sideVV.z);
                shader.safeGetUniform("UpV").set(upV.x, upV.y, upV.z);
                shader.safeGetUniform("PlumeLength").set(s.length);
                shader.safeGetUniform("CutFront").set(s.cutFront);
                shader.safeGetUniform("Pressure").set(s.pressure);
                shader.safeGetUniform("Dissipate").set(s.dissipate);
                shader.safeGetUniform("FlowClock").set(s.flowClock);
                shader.safeGetUniform("ExitSpeed").set(s.exitSpeed);
                shader.safeGetUniform("BaseFlow").set(s.baseFlow);
                shader.safeGetUniform("NozzleRadius").set(NOZZLE_RADIUS);
                shader.safeGetUniform("FlashRadius").set(FLASH_RADIUS);
                shader.safeGetUniform("ConeTan").set(CONE_TAN);
                shader.safeGetUniform("Brightness").set(1.0f);

                renderType.setupRenderState();
                RenderSystem.setShader(() -> shader);
                RenderSystem.setShaderTexture(1, depthTex);
                shader.setSampler("Sampler1", depthTex);
                shader.apply();

                Tesselator tess = Tesselator.getInstance();
                BufferBuilder bb = tess.getBuilder();
                bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
                drawProxy(bb, viewMat, s);
                BufferUploader.drawWithShader(bb.end());
                renderType.clearRenderState();
            }
            poseStack.popPose();
        } finally {
            count = 0;
        }
    }

    private void sortFarToNear(Vec3 camPos) {
        for (int i = 0; i < count; i++) {
            drawOrder[i] = i;
        }
        for (int i = 1; i < count; i++) {
            int key = drawOrder[i];
            double keyD = distSq(slots[key], camPos);
            int j = i - 1;
            while (j >= 0 && distSq(slots[drawOrder[j]], camPos) < keyD) {
                drawOrder[j + 1] = drawOrder[j];
                j--;
            }
            drawOrder[j + 1] = key;
        }
    }

    private static double distSq(Slot s, Vec3 camPos) {
        double mx = s.nozzleX + s.axisX * s.length * 0.5 - camPos.x;
        double my = s.nozzleY + s.axisY * s.length * 0.5 - camPos.y;
        double mz = s.nozzleZ + s.axisZ * s.length * 0.5 - camPos.z;
        return mx * mx + my * my + mz * mz;
    }

    /** Rayon maximal du panache : la tete gonflee et etalee, avec la marge du bord erode, comme la borne du shader. */
    private static float maxRadius(Slot s) {
        return (FLASH_RADIUS + s.length * CONE_TAN) * 1.7f * (1f + 1.2f * s.dissipate) * 1.6f + 0.1f;
    }

    /** Boite englobante du cone, faces vers l'interieur comme la boite des faisceaux. */
    private static void drawProxy(BufferBuilder vc, Matrix4f mat, Slot s) {
        float pad = maxRadius(s) + 0.2f;
        float reach = s.length + 1.0f;
        float ox = s.nozzleX - s.axisX * 0.15f;
        float oy = s.nozzleY - s.axisY * 0.15f;
        float oz = s.nozzleZ - s.axisZ * 0.15f;
        float ex = s.nozzleX + s.axisX * reach;
        float ey = s.nozzleY + s.axisY * reach;
        float ez = s.nozzleZ + s.axisZ * reach;
        float minX = Math.min(ox, ex) - pad;
        float minY = Math.min(oy, ey) - pad;
        float minZ = Math.min(oz, ez) - pad;
        float maxX = Math.max(ox, ex) + pad;
        float maxY = Math.max(oy, ey) + pad;
        float maxZ = Math.max(oz, ez) + pad;

        emitQuad(vc, mat, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, minY, maxZ, minX, minY, maxZ);
        emitQuad(vc, mat, maxX, maxY, minZ, minX, maxY, minZ, minX, minY, minZ, maxX, minY, minZ);
        emitQuad(vc, mat, maxX, maxY, maxZ, maxX, maxY, minZ, maxX, minY, minZ, maxX, minY, maxZ);
        emitQuad(vc, mat, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ, minX, minY, minZ);
        emitQuad(vc, mat, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ);
        emitQuad(vc, mat, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ);
    }

    private static void emitQuad(BufferBuilder vc, Matrix4f mat,
                                 float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3) {
        vc.vertex(mat, x0, y0, z0).color(255, 255, 255, 255).uv(0f, 0f).endVertex();
        vc.vertex(mat, x1, y1, z1).color(255, 255, 255, 255).uv(1f, 0f).endVertex();
        vc.vertex(mat, x2, y2, z2).color(255, 255, 255, 255).uv(1f, 1f).endVertex();
        vc.vertex(mat, x3, y3, z3).color(255, 255, 255, 255).uv(0f, 1f).endVertex();
    }

    private void transformPoint(Matrix4f mat, float x, float y, float z, Vector3f out) {
        tmp.set(x, y, z, 1f);
        mat.transform(tmp);
        out.set(tmp.x, tmp.y, tmp.z);
    }

    private void transformDir(Matrix4f mat, float x, float y, float z, Vector3f out) {
        tmp.set(x, y, z, 0f);
        mat.transform(tmp);
        out.set(tmp.x, tmp.y, tmp.z);
        if (out.lengthSquared() > 1.0e-12f) {
            out.normalize();
        }
    }

    @Override
    public Vec3 getPos(float partialTick) {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    }

    private static final class Slot {
        float nozzleX, nozzleY, nozzleZ;
        float axisX, axisY, axisZ;
        float length;
        float cutFront;
        float pressure;
        float dissipate;
        float flowClock;
        float exitSpeed;
        float baseFlow;
    }
}
