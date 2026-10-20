package com.github.dumann089.theatricalextralights.client.render.pyro;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasFlameStyle;
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
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Flammes de scene en volume ({@code flame_volume}), avec la distorsion de chaleur
 * ({@code flame_heat}) et la dynamique allumage / extinction.
 *
 * <p>Les machines a flammes soumettent leur jet a chaque image depuis leur block entity renderer.
 * L'etat d'allumage vit ici, par position : a l'ouverture la flamme pousse depuis la buse, a la
 * fermeture elle se detache et se consume en montant au lieu de disparaitre d'un coup. Le dessin
 * a lieu dans la phase differee, apres les blocs : profondeur copiee pour l'occlusion, puis la
 * passe de chaleur (copie de l'image deformee) et la flamme elle-meme, du plus loin au plus pres.
 */
public final class FlameVolumeRenderer extends LazyRenderers.LazyRenderer {

    public static final int MAX_FLAMES = 64;
    private static final FlameVolumeRenderer INSTANCE = new FlameVolumeRenderer();

    /** Parametres d'allure par style ({@link HasFlameStyle}). */
    private record Style(float radius0, float radiusPerLength, float radiusBase, float bendPerLength, float bulb,
                         float turbulence, float flowSpeed, float growSpeed, float liftSpeed, float fadeSeconds) {
    }

    private static final Style JET = new Style(0.09f, 0.20f, 0.30f, 0.18f, 0.30f, 0.80f, 7.0f, 16.0f, 6.0f, 0.45f);
    private static final Style BURST = new Style(0.16f, 0.38f, 0.45f, 0.35f, 0.45f, 1.05f, 3.5f, 10.0f, 3.5f, 0.70f);

    private static final class JetState {
        boolean on;
        double onAt;
        double offAt;
        float frontAtOff;
        float length;
        double lastTime;
        long lastSeenTick;
        float seed;
    }

    private static final class Spec {
        Vec3 origin;
        Vec3 dir;
        Vec3 u;
        Vec3 v;
        float length;
        float front;
        float base;
        float fade;
        float intensity;
        float seed;
        Style style;
        float bend;
        double distSq;
    }

    private final Map<BlockPos, JetState> states = new HashMap<>();
    private final Spec[] specs = new Spec[MAX_FLAMES];
    private int count;
    private long lastSweepTick;

    private final Vector4f tmp = new Vector4f();
    private final Matrix4f invProj = new Matrix4f();

    private FlameVolumeRenderer() {
        for (int i = 0; i < MAX_FLAMES; i++) {
            specs[i] = new Spec();
        }
    }

    /** Le rendu en volume est actif : reglage allume, shaders charges, pas de shaderpack. */
    public static boolean isAvailable() {
        return TheatricalExtraLightsConfig.isVolumetricFlamesEnabled() && ModShaders.canUseFlames();
    }

    /**
     * Soumet le jet d'une machine a flammes pour cette image.
     *
     * @param key         position de la machine (cle de l'etat d'allumage)
     * @param originWorld sortie de la buse, monde
     * @param dirWorld    direction du jet, normalisee
     * @param length      longueur visee du jet, blocs
     * @param style       {@link HasFlameStyle#STYLE_JET} ou {@link HasFlameStyle#STYLE_BURST}
     * @param intensity   0..1
     * @param on          la machine crache en ce moment
     */
    public static void submit(BlockPos key, Vec3 originWorld, Vec3 dirWorld, float length, int style,
                              float intensity, boolean on, float partialTick) {
        INSTANCE.enqueue(key, originWorld, dirWorld, length, style, intensity, on, partialTick);
    }

    private void enqueue(BlockPos key, Vec3 origin, Vec3 dir, float length, int styleIndex, float intensity,
                         boolean on, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !isAvailable()) {
            return;
        }
        long tick = mc.level.getGameTime();
        double now = (tick + (double) partialTick) / 20.0;
        Style style = styleIndex == HasFlameStyle.STYLE_BURST ? BURST : JET;

        JetState st = states.get(key);
        if (st == null) {
            if (!on) {
                return;
            }
            st = new JetState();
            st.length = length;
            st.lastTime = now;
            st.seed = seedOf(key);
            states.put(key.immutable(), st);
        }
        st.lastSeenTick = tick;
        double dt = Math.max(0.0, Math.min(0.25, now - st.lastTime));
        st.lastTime = now;
        st.length += (float) ((length - st.length) * Math.min(1.0, dt * 6.0));

        if (on && !st.on) {
            st.on = true;
            st.onAt = now;
        } else if (!on && st.on) {
            st.on = false;
            st.offAt = now;
            st.frontAtOff = Math.min(st.length, (float) ((now - st.onAt) * style.growSpeed));
        }

        float front;
        float base;
        float fade;
        if (st.on) {
            front = Math.min(st.length, (float) ((now - st.onAt) * style.growSpeed));
            base = 0f;
            fade = 1f;
        } else {
            float e = (float) (now - st.offAt);
            base = e * style.liftSpeed;
            front = Math.min(st.length * 1.15f, st.frontAtOff + e * style.liftSpeed * 0.5f);
            fade = 1f - e / style.fadeSeconds;
            if (fade <= 0f || base >= front) {
                states.remove(key);
                return;
            }
        }
        if (front <= 0.02f || count >= MAX_FLAMES) {
            return;
        }

        if (count == 0) {
            SceneDepthCopy.beginFrame();
            LazyRenderers.addLazyRender(this);
        }
        Spec s = specs[count++];
        s.origin = origin;
        s.dir = dir.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        double along = s.dir.dot(up);
        Vec3 side = up.subtract(s.dir.scale(along));
        if (side.lengthSqr() < 1.0e-4) {
            side = Math.abs(s.dir.x) < 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
            side = side.subtract(s.dir.scale(side.dot(s.dir)));
        }
        s.u = side.normalize();
        s.v = s.dir.cross(s.u).normalize();
        s.length = st.length;
        s.front = front;
        s.base = base;
        s.fade = fade;
        s.intensity = intensity;
        s.seed = st.seed;
        s.style = style;
        float tilt = (float) (1.0 - Math.abs(along));
        s.bend = style.bendPerLength * st.length * tilt;

        if (tick - lastSweepTick > 40) {
            lastSweepTick = tick;
            sweep(tick);
        }
    }

    private void sweep(long tick) {
        Iterator<Map.Entry<BlockPos, JetState>> it = states.entrySet().iterator();
        while (it.hasNext()) {
            if (tick - it.next().getValue().lastSeenTick > 40) {
                it.remove();
            }
        }
    }

    private static float seedOf(BlockPos pos) {
        long h = pos.asLong() * 0x9E3779B97F4A7C15L;
        h ^= (h >>> 31);
        return (h & 0xFFFF) / 65535f;
    }

    private static float radiusEnd(Spec s) {
        return s.style.radiusPerLength * s.length + s.style.radiusBase;
    }

    @Override
    public void render(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Camera camera, float partialTick) {
        try {
            if (count == 0) {
                return;
            }
            SceneDepthCopy.flushOpaqueAndCapture(bufferSource);
            bufferSource.endBatch();
            ShaderInstance volume = ModShaders.flameVolumeShader;
            if (!SceneDepthCopy.hasDepth() || volume == null) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            invProj.set(RenderSystem.getProjectionMatrix()).invert();
            var main = mc.getMainRenderTarget();
            Vec3 cam = camera.getPosition();
            float time = (float) (((mc.level != null ? mc.level.getGameTime() % 240000L : 0L) + partialTick) / 20.0);

            poseStack.pushPose();
            poseStack.translate(-cam.x, -cam.y, -cam.z);
            Matrix4f view = new Matrix4f(poseStack.last().pose());
            poseStack.popPose();

            for (int i = 0; i < count; i++) {
                Spec s = specs[i];
                Vec3 mid = s.origin.add(s.dir.scale(s.front * 0.5));
                s.distSq = mid.distanceToSqr(cam);
            }
            sortFarToNear();

            int depthTex = SceneDepthCopy.getDepthTextureId();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableCull();
            RenderSystem.enableBlend();

            ShaderInstance heat = ModShaders.flameHeatShader;
            if (heat != null && TheatricalExtraLightsConfig.isFlameHeatDistortionEnabled()) {
                int colorTex = SceneColorCopy.capture();
                if (colorTex != 0) {
                    float strength = TheatricalExtraLightsConfig.getFlameHeatStrength();
                    for (int i = 0; i < count; i++) {
                        Spec s = specs[i];
                        setCommon(heat, s, view, main.width, main.height, time);
                        heat.safeGetUniform("Strength").set(strength);
                        RenderSystem.setShader(() -> heat);
                        RenderSystem.setShaderTexture(0, colorTex);
                        heat.setSampler("Sampler0", colorTex);
                        RenderSystem.setShaderTexture(1, depthTex);
                        heat.setSampler("Sampler1", depthTex);
                        drawBox(view, s, true);
                    }
                }
            }

            int baseSteps = TheatricalExtraLightsConfig.getFlameSteps();
            int octaves = TheatricalExtraLightsConfig.getFlameNoiseOctaves();
            float brightness = TheatricalExtraLightsConfig.getFlameBrightness();
            for (int i = 0; i < count; i++) {
                Spec s = specs[i];
                setCommon(volume, s, view, main.width, main.height, time);
                transformDir(view, s.u, tmp);
                volume.safeGetUniform("FlameU").set(tmp.x, tmp.y, tmp.z);
                transformDir(view, s.v, tmp);
                volume.safeGetUniform("FlameV").set(tmp.x, tmp.y, tmp.z);
                volume.safeGetUniform("FlameOriginW").set((float) s.origin.x, (float) s.origin.y, (float) s.origin.z);
                volume.safeGetUniform("Bend").set(s.bend);
                volume.safeGetUniform("Bulb").set(s.style.bulb);
                volume.safeGetUniform("Turbulence").set(s.style.turbulence);
                volume.safeGetUniform("FlowSpeed").set(s.style.flowSpeed);
                volume.safeGetUniform("Intensity").set(s.intensity);
                volume.safeGetUniform("Brightness").set(brightness);
                double dist = Math.sqrt(s.distSq);
                int steps = dist > 96 ? Math.max(6, baseSteps * 2 / 5) : dist > 48 ? Math.max(8, baseSteps * 3 / 5) : baseSteps;
                volume.safeGetUniform("StepCount").set(steps);
                volume.safeGetUniform("NoiseOctaves").set(dist > 64 ? Math.max(2, octaves - 1) : octaves);
                RenderSystem.setShader(() -> volume);
                RenderSystem.setShaderTexture(1, depthTex);
                volume.setSampler("Sampler1", depthTex);
                drawBox(view, s, false);
            }

            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        } finally {
            count = 0;
        }
    }

    private void setCommon(ShaderInstance shader, Spec s, Matrix4f view, float w, float h, float time) {
        shader.safeGetUniform("InvProjMat").set(invProj);
        shader.safeGetUniform("ScreenSize").set(w, h);
        tmp.set((float) s.origin.x, (float) s.origin.y, (float) s.origin.z, 1f);
        view.transform(tmp);
        shader.safeGetUniform("FlameOrigin").set(tmp.x, tmp.y, tmp.z);
        transformDir(view, s.dir, tmp);
        shader.safeGetUniform("FlameDir").set(tmp.x, tmp.y, tmp.z);
        shader.safeGetUniform("Length").set(s.length);
        shader.safeGetUniform("Front").set(s.front);
        shader.safeGetUniform("Base").set(s.base);
        shader.safeGetUniform("Radius0").set(s.style.radius0);
        shader.safeGetUniform("RadiusEnd").set(radiusEnd(s));
        shader.safeGetUniform("Fade").set(s.fade);
        shader.safeGetUniform("Seed").set(s.seed);
        shader.safeGetUniform("Time").set(time);
    }

    private static void transformDir(Matrix4f m, Vec3 d, Vector4f out) {
        out.set((float) d.x, (float) d.y, (float) d.z, 0f);
        m.transform(out);
        float l = (float) Math.sqrt(out.x * out.x + out.y * out.y + out.z * out.z);
        if (l > 1.0e-6f) {
            out.x /= l;
            out.y /= l;
            out.z /= l;
        }
    }

    private void sortFarToNear() {
        for (int i = 1; i < count; i++) {
            Spec key = specs[i];
            int j = i - 1;
            while (j >= 0 && specs[j].distSq < key.distSq) {
                specs[j + 1] = specs[j];
                j--;
            }
            specs[j + 1] = key;
        }
    }

    /**
     * Boite alignee sur le monde autour du volume (ou du panache de chaleur), faces dessinees de
     * l'interieur comme les faisceaux raymarches : chaque pixel est traite une fois, camera
     * dedans ou dehors.
     */
    private static void drawBox(Matrix4f view, Spec s, boolean heat) {
        float r = radiusEnd(s) * (1f + s.style.bulb) + 0.4f;
        float reach = heat ? s.front + 0.6f * s.length + 1.0f : s.front + r;
        float width = heat ? r * 1.6f + 0.6f * s.length * 0.35f + 0.3f : r;
        Vec3 a = s.origin.add(s.dir.scale(s.base - 0.3));
        Vec3 b = s.origin.add(s.dir.scale(reach)).add(s.u.scale(s.bend));
        float minX = (float) Math.min(a.x, b.x) - width, maxX = (float) Math.max(a.x, b.x) + width;
        float minY = (float) Math.min(a.y, b.y) - width, maxY = (float) Math.max(a.y, b.y) + width;
        float minZ = (float) Math.min(a.z, b.z) - width, maxZ = (float) Math.max(a.z, b.z) + width;

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        quad(bb, view, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, minY, maxZ, minX, minY, maxZ);
        quad(bb, view, maxX, maxY, minZ, minX, maxY, minZ, minX, minY, minZ, maxX, minY, minZ);
        quad(bb, view, maxX, maxY, maxZ, maxX, maxY, minZ, maxX, minY, minZ, maxX, minY, maxZ);
        quad(bb, view, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ, minX, minY, minZ);
        quad(bb, view, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ);
        quad(bb, view, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ);
        BufferUploader.drawWithShader(bb.end());
    }

    private static void quad(BufferBuilder bb, Matrix4f m,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        bb.vertex(m, x0, y0, z0).color(255, 255, 255, 255).uv(0f, 0f).endVertex();
        bb.vertex(m, x1, y1, z1).color(255, 255, 255, 255).uv(1f, 0f).endVertex();
        bb.vertex(m, x2, y2, z2).color(255, 255, 255, 255).uv(1f, 1f).endVertex();
        bb.vertex(m, x3, y3, z3).color(255, 255, 255, 255).uv(0f, 1f).endVertex();
    }

    @Override
    public Vec3 getPos(float partialTick) {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    }

    @SuppressWarnings("unused")
    private static Vector3f toVector(Vec3 v) {
        return new Vector3f((float) v.x, (float) v.y, (float) v.z);
    }
}
