package com.github.dumann089.theatricalextralights.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import com.github.dumann089.theatricalextralights.client.render.beam.BeamRenderData;

/** Publishes active beam transforms to a targeted Iris shader-pack fork. */
public final class BeamShaderData {
    public static final int MAX_BEAMS = 32;
    public static final int TEXELS_PER_BEAM = 8;
    public static final ResourceLocation TEXTURE =
            new ResourceLocation("theatricalextralights", "beam_data");

    private static final int WIDTH = MAX_BEAMS * TEXELS_PER_BEAM;
    private static final float POSITION_RANGE = 512.0f;
    private static NativeImage pixels;
    private static DynamicTexture texture;
    private static int nextBeam;

    private BeamShaderData() {
    }

    public static void init() {
        if (texture != null) return;
        pixels = new NativeImage(NativeImage.Format.RGBA, WIDTH, 1, false);
        texture = new DynamicTexture(pixels);
        Minecraft.getInstance().getTextureManager().register(TEXTURE, texture);
    }
    private static void ensureRegistered() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getTextureManager().getTexture(TEXTURE) != texture) {
            minecraft.getTextureManager().register(TEXTURE, texture);
        }
    }


    public static void beginFrame() {
        if (texture == null) init();
        ensureRegistered();
        nextBeam = 0;
        for (int x = 0; x < WIDTH; x++) {
            pixels.setPixelRGBA(x, 0, 0);
        }
        texture.upload();
    }

    public static void submit(BeamRenderData data) {
        if (texture == null) init();
        if (nextBeam >= MAX_BEAMS || data.level() == null) return;

        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 worldOrigin = Vec3.atLowerCornerOf(data.fixturePos()).add(data.origin());
        Vec3 origin = worldOrigin.subtract(camera);
        int base = nextBeam++ * TEXELS_PER_BEAM;

        put(base, encodeSigned((float) origin.x), encodeSigned((float) origin.y),
                encodeSigned((float) origin.z), encodeUnsigned(data.scanLen(), 512.0f));
        put(base + 1, encodeSigned((float) data.beamDir().x), encodeSigned((float) data.beamDir().y),
                encodeSigned((float) data.beamDir().z), encodeUnsigned(data.tanHalfAngle(), 4.0f));
        put(base + 2, channel(data.color(), 16), channel(data.color(), 8),
                channel(data.color(), 0), clampByte(data.intensity()));
        put(base + 3, clampByte(data.widthScale()), clampByte(data.heightScale()), 255, 0);
        put(base + 4, 0, 0, 0, 0);
        put(base + 5, 0, 0, 0, 0);
        put(base + 6, 0, 0, 0, 0);
        put(base + 7, 0, 0, 0, 0);
        texture.upload();
    }

    private static void put(int texel, int r, int g, int b, int a) {
        pixels.setPixelRGBA(texel, 0, (a << 24) | (b << 16) | (g << 8) | r);
    }

    private static int encodeSigned(float value) {
        return Math.round(Math.max(0.0f, Math.min(1.0f, value / POSITION_RANGE * 0.5f + 0.5f)) * 255.0f);
    }

    private static int encodeUnsigned(float value, float range) {
        return Math.round(Math.max(0.0f, Math.min(1.0f, value / range)) * 255.0f);
    }

    private static int clampByte(float value) {
        return Math.round(Math.max(0.0f, Math.min(1.0f, value)) * 255.0f);
    }

    private static int channel(int color, int shift) {
        return (color >> shift) & 0xFF;
    }
}
