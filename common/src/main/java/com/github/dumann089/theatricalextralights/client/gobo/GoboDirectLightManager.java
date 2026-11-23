package com.github.dumann089.theatricalextralights.client.gobo;

import com.github.dumann089.theatricalextralights.client.render.beam.BeamRenderData;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.util.FramingShutterState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class GoboDirectLightManager {
    public static final int MAX_PROJECTORS = 2048;
    public static final int MAX_PER_TILE = 8;
    public static final int TILE_SIZE = 64;

    private static int saturatedTileCount;

    private static final int GOBO_DATA_TEXTURE_UNIT = 8;
    private static final int GOBO_TILE_TEXTURE_UNIT = 9;
    private static final int GOBO_ATLAS_TEXTURE_UNIT = 10;

    private static final int DATA_COLUMNS = 12;
    private static final int DATA_FLOATS_PER_PROJECTOR = DATA_COLUMNS * 4;
    private static final int SCREEN_BOUND_SAMPLES = 32;
    private static final float[] SCREEN_BOUND_COS = new float[SCREEN_BOUND_SAMPLES];
    private static final float[] SCREEN_BOUND_SIN = new float[SCREEN_BOUND_SAMPLES];
    private static final float[] PROJECTOR_DATA =
            new float[MAX_PROJECTORS * DATA_FLOATS_PER_PROJECTOR];

    private static final float[] CLIP_W_RANGE = new float[2];
    private static final float[] SCREEN_BOUNDS = new float[4];

    private static final Vector4f SCRATCH_POSITION = new Vector4f();
    private static final Vector4f SCRATCH_DIRECTION = new Vector4f();
    private static final Vector4f SCRATCH_AXIS_U = new Vector4f();
    private static final Vector4f SCRATCH_AXIS_V = new Vector4f();
    private static final Vector4f SCRATCH_OCCLUSION_POSITION = new Vector4f();
    private static final Vector4f SCRATCH_OCCLUSION_NORMAL = new Vector4f();
    private static final Vector4f SCRATCH_CLIP_POINT = new Vector4f();
    private static final Vector4f SCRATCH_CLIP_CENTER = new Vector4f();
    private static final Vector4f SCRATCH_CLIP_U = new Vector4f();
    private static final Vector4f SCRATCH_CLIP_V = new Vector4f();

    private static final ViewProjector VIEW_PROJECTOR_SCRATCH = new ViewProjector();

    private static final Comparator<Projector> PROJECTOR_IMPORTANCE_ORDER =
            Comparator.comparingDouble((Projector projector) -> projector.importance).reversed();

    private static int[][] cpuTileLists = new int[0][];
    private static int[] cpuTileCounts = new int[0];

    private static FloatBuffer projectorUploadBuffer;
    private static FloatBuffer tileUploadBuffer;

    static {
        for (int i = 0; i < SCREEN_BOUND_SAMPLES; i++) {
            double angle = Math.PI * 2.0 * i / SCREEN_BOUND_SAMPLES;
            SCREEN_BOUND_COS[i] = (float) Math.cos(angle);
            SCREEN_BOUND_SIN[i] = (float) Math.sin(angle);
        }
    }

    private static final List<Projector> ACTIVE = new ArrayList<>(MAX_PROJECTORS);
    private static final List<Projector> PENDING = new ArrayList<>(MAX_PROJECTORS);

    private static int dataTexture;
    private static int tileTexture;

    // Allocated tile texture capacity. It only grows; the uploaded width is per-frame.
    private static int tileTextureWidth;
    private static int tileTextureHeight;

    private static int screenWidth;
    private static int screenHeight;
    private static int tilesX;
    private static int tilesY;

    private static int preparedProjectorCount;
    private static int preparedMaxPerTile;

    private static long frameSerial;
    private static long preparedSerial = -1L;

    private GoboDirectLightManager() {
    }

    private static final class Projector {
        Vec3 origin;
        Vec3 direction;
        Vec3 axisU;
        Vec3 axisV;

        Vec3 occlusionPosition;
        Vec3 occlusionNormal;

        float baseRadius;
        float tanHalfAngle;
        float maxDistance;
        float intensity;
        float rotationRadians;
        float focus;
        float wheelTransition;
        float animationAngle;
        float animationOffset;
        float frameRotation;
        float importance;

        int color;
        int outgoingLayer;
        int incomingLayer;
        int animationLayer;

        boolean animationEnabled;
        boolean occlusionEnabled;

        final float[] shutterA = new float[4];
        final float[] shutterB = new float[4];
    }

    private static final class ViewProjector {
        float px;
        float py;
        float pz;

        float dx;
        float dy;
        float dz;

        float ux;
        float uy;
        float uz;

        float vx;
        float vy;
        float vz;

        float opx;
        float opy;
        float opz;

        float onx;
        float ony;
        float onz;

        boolean occlusionEnabled;
        Projector source;
    }

    public static boolean isEnabled() {
        return true;
    }

    public static void beginFrame() {
        ACTIVE.clear();
        ACTIVE.addAll(PENDING);
        PENDING.clear();

        frameSerial++;
        preparedSerial = -1L;

        prepareGpuData();
    }

    public static void submit(
            Vec3 origin,
            Vec3 direction,
            Vec3 axisU,
            Vec3 axisV,
            float baseRadius,
            float tanHalfAngle,
            float maxDistance,
            float rotationDegrees,
            int color,
            float intensity,
            ResourceLocation outgoingTexture,
            ResourceLocation incomingTexture,
            float wheelTransition,
            float focus,
            FramingShutterState.Snapshot shutters,
            BeamRenderData.Animation animation,
            boolean occlusionEnabled,
            Vec3 occlusionPosition,
            Vec3 occlusionNormal
    ) {
        if (!isFinite(origin)
                || !isFinite(direction)
                || !isFinite(axisU)
                || !isFinite(axisV)) {
            return;
        }

        if (!Float.isFinite(intensity)
                || !Float.isFinite(maxDistance)
                || !Float.isFinite(tanHalfAngle)
                || intensity <= 0.001f
                || maxDistance <= 0.01f
                || tanHalfAngle <= 0.0f
                || direction.lengthSqr() <= 1.0E-10
                || axisU.lengthSqr() <= 1.0E-10
                || axisV.lengthSqr() <= 1.0E-10) {
            return;
        }

        Projector projector = new Projector();

        projector.origin = origin;
        projector.direction = direction.normalize();
        projector.axisU = axisU.normalize();
        projector.axisV = axisV.normalize();

        projector.baseRadius = Math.max(0.001f, Float.isFinite(baseRadius) ? baseRadius : 0.001f);
        projector.tanHalfAngle = tanHalfAngle;
        projector.maxDistance = maxDistance;
        projector.rotationRadians = (float) Math.toRadians(rotationDegrees);
        projector.intensity = intensity;
        projector.focus = clamp(focus, 0.0f, 1.0f);
        projector.wheelTransition = clamp(wheelTransition, 0.0f, 1.0f);
        projector.color = color & 0xFFFFFF;

        projector.outgoingLayer = GoboTextureArray.getLayer(outgoingTexture);
        projector.incomingLayer = GoboTextureArray.getLayer(
                incomingTexture != null ? incomingTexture : outgoingTexture
        );

        if (animation != null && animation.texture() != null) {
            projector.animationLayer = GoboTextureArray.getLayer(animation.texture());
            projector.animationAngle = (float) Math.toRadians(animation.angleDeg());
            projector.animationOffset = animation.offset();
            projector.animationEnabled = true;
        }

        if (shutters != null && shutters.isActive()) {
            float[] a = shutters.insertionA();
            float[] b = shutters.insertionB();

            if (a != null && a.length >= 4) {
                System.arraycopy(a, 0, projector.shutterA, 0, 4);
            }

            if (b != null && b.length >= 4) {
                System.arraycopy(b, 0, projector.shutterB, 0, 4);
            }

            projector.frameRotation = shutters.frameRotation();
        }

        projector.occlusionEnabled = occlusionEnabled
                && isFinite(occlusionPosition)
                && isFinite(occlusionNormal)
                && occlusionNormal.lengthSqr() > 1.0E-10;

        projector.occlusionPosition = projector.occlusionEnabled
                ? occlusionPosition
                : Vec3.ZERO;

        projector.occlusionNormal = projector.occlusionEnabled
                ? occlusionNormal.normalize()
                : Vec3.ZERO;

        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer != null
                ? minecraft.gameRenderer.getMainCamera()
                : null;

        Vec3 cameraPosition = camera != null ? camera.getPosition() : origin;
        double distanceSquared = origin.distanceToSqr(cameraPosition);

        float colorBrightness = Math.max(
                (projector.color >> 16 & 255) / 255.0f,
                Math.max(
                        (projector.color >> 8 & 255) / 255.0f,
                        (projector.color & 255) / 255.0f
                )
        );

        projector.importance = intensity * Math.max(0.05f, colorBrightness)
                / (1.0f + (float) distanceSquared * 0.0005f);

        if (PENDING.size() < MAX_PROJECTORS) {
            PENDING.add(projector);
            return;
        }

        int leastImportantIndex = 0;
        float leastImportant = Float.MAX_VALUE;

        for (int i = 0; i < PENDING.size(); i++) {
            float value = PENDING.get(i).importance;

            if (value < leastImportant) {
                leastImportant = value;
                leastImportantIndex = i;
            }
        }

        if (projector.importance > leastImportant) {
            PENDING.set(leastImportantIndex, projector);
        }
    }

    public static void bindAtlas(ShaderInstance shader) {
        if (shader == null || shader.getUniform("GoboTileSize") == null) {
            return;
        }

        int atlasTexture = GoboTextureArray.finishUploads();

        int uniformLocation = GL20.glGetUniformLocation(
                shader.getId(),
                "GoboAtlas"
        );

        if (uniformLocation < 0) {
            return;
        }

        int textureUnit = GL20.glGetUniformi(shader.getId(), uniformLocation);
        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        try {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + textureUnit);
            GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, atlasTexture);
        } finally {
            RenderSystem.activeTexture(previousActiveTexture);
        }
    }

    public static void apply(ShaderInstance shader) {
        if (shader == null || shader.getUniform("GoboTileSize") == null) {
            return;
        }

        RenderSystem.assertOnRenderThread();

        if (preparedSerial != frameSerial) {
            shader.safeGetUniform("GoboProjectorCount").set(0.0f);
            shader.safeGetUniform("GoboMaxPerTile").set(0.0f);
            return;
        }

        shader.safeGetUniform("GoboProjectorCount").set((float) preparedProjectorCount);
        shader.safeGetUniform("GoboTileSize").set((float) TILE_SIZE);
        shader.safeGetUniform("GoboTilesX").set((float) tilesX);
        shader.safeGetUniform("GoboTilesY").set((float) tilesY);
        shader.safeGetUniform("GoboMaxPerTile").set((float) preparedMaxPerTile);
        shader.safeGetUniform("GoboScreenSize").set((float) screenWidth, (float) screenHeight);
        shader.safeGetUniform("GoboBrightness").set(TheatricalExtraLightsConfig.getGoboBrightness());
    }

    public static void bindSamplers(ShaderInstance shader) {
        if (shader == null || shader.getUniform("GoboTileSize") == null) {
            return;
        }

        if (preparedSerial != frameSerial
                || dataTexture == 0
                || tileTexture == 0
                || GoboTextureArray.getTextureId() == 0) {
            return;
        }

        int programId = shader.getId();
        int currentProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);

        if (currentProgram != programId) {
            return;
        }

        int maxUnits = GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS);

        if (maxUnits < 16) {
            throw new IllegalStateException(
                    "[TheatricalExtraLights/Gobo] Not enough fragment texture units: " + maxUnits
            );
        }

        int dataUnit = maxUnits - 3;
        int tileUnit = maxUnits - 2;
        int atlasUnit = maxUnits - 1;

        int dataLocation = GL20.glGetUniformLocation(programId, "GoboData");
        int tileLocation = GL20.glGetUniformLocation(programId, "GoboTileIndices");
        int atlasLocation = GL20.glGetUniformLocation(programId, "GoboAtlas");

        if (dataLocation < 0 || tileLocation < 0 || atlasLocation < 0) {
            return;
        }

        int previousActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        try {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + dataUnit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, dataTexture);
            GL20.glUniform1i(dataLocation, dataUnit);

            GL13.glActiveTexture(GL13.GL_TEXTURE0 + tileUnit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tileTexture);
            GL20.glUniform1i(tileLocation, tileUnit);

            GL13.glActiveTexture(GL13.GL_TEXTURE0 + atlasUnit);
            GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, GoboTextureArray.getTextureId());
            GL20.glUniform1i(atlasLocation, atlasUnit);
        } finally {
            GL13.glActiveTexture(previousActive);
        }
    }

    public static void bindExternalProgram(int programId) {
        if (programId <= 0
                || preparedSerial != frameSerial
                || dataTexture == 0
                || tileTexture == 0
                || GoboTextureArray.getTextureId() == 0) {
            return;
        }

        if (GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) != programId) {
            return;
        }

        int maxUnits = GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS);

        if (maxUnits <= GOBO_ATLAS_TEXTURE_UNIT) {
            return;
        }

        int dataLocation = GL20.glGetUniformLocation(programId, "GoboData");
        int tileLocation = GL20.glGetUniformLocation(programId, "GoboTileIndices");
        int atlasLocation = GL20.glGetUniformLocation(programId, "GoboAtlas");

        int countLocation = GL20.glGetUniformLocation(programId, "GoboProjectorCount");
        int tileSizeLocation = GL20.glGetUniformLocation(programId, "GoboTileSize");
        int tilesXLocation = GL20.glGetUniformLocation(programId, "GoboTilesX");
        int tilesYLocation = GL20.glGetUniformLocation(programId, "GoboTilesY");
        int maxPerTileLocation = GL20.glGetUniformLocation(programId, "GoboMaxPerTile");
        int screenSizeLocation = GL20.glGetUniformLocation(programId, "GoboScreenSize");
        int brightnessLocation = GL20.glGetUniformLocation(programId, "GoboBrightness");

        if (dataLocation < 0
                || tileLocation < 0
                || atlasLocation < 0
                || countLocation < 0
                || tileSizeLocation < 0
                || tilesXLocation < 0
                || tilesYLocation < 0
                || maxPerTileLocation < 0
                || screenSizeLocation < 0
                || brightnessLocation < 0) {
            return;
        }

        int previousActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        try {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + GOBO_DATA_TEXTURE_UNIT);
            RenderSystem.bindTexture(dataTexture);
            GL20.glUniform1i(dataLocation, GOBO_DATA_TEXTURE_UNIT);

            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + GOBO_TILE_TEXTURE_UNIT);
            RenderSystem.bindTexture(tileTexture);
            GL20.glUniform1i(tileLocation, GOBO_TILE_TEXTURE_UNIT);

            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + GOBO_ATLAS_TEXTURE_UNIT);
            GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, GoboTextureArray.getTextureId());
            GL20.glUniform1i(atlasLocation, GOBO_ATLAS_TEXTURE_UNIT);

            GL20.glUniform1i(countLocation, preparedProjectorCount);
            GL20.glUniform1i(tileSizeLocation, TILE_SIZE);
            GL20.glUniform1i(tilesXLocation, tilesX);
            GL20.glUniform1i(tilesYLocation, tilesY);
            GL20.glUniform1i(maxPerTileLocation, preparedMaxPerTile);

            GL20.glUniform2f(screenSizeLocation, screenWidth, screenHeight);
            GL20.glUniform1f(
                    brightnessLocation,
                    TheatricalExtraLightsConfig.getGoboBrightness()
            );
        } finally {
            RenderSystem.activeTexture(previousActive);
        }
    }

    private static boolean isCameraInsideBeam(Projector projector, Vec3 cameraPosition) {
        Vec3 fromLight = cameraPosition.subtract(projector.origin);

        double forwardDistance = fromLight.dot(projector.direction);

        if (forwardDistance <= 0.0 || forwardDistance >= projector.maxDistance) {
            return false;
        }

        double radius = projector.baseRadius + forwardDistance * projector.tanHalfAngle;
        double u = fromLight.dot(projector.axisU);
        double v = fromLight.dot(projector.axisV);

        return u * u + v * v <= radius * radius;
    }

    private static void prepareGpuData() {
        if (preparedSerial == frameSerial) {
            return;
        }

        preparedProjectorCount = 0;
        preparedMaxPerTile = 0;

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.gameRenderer == null
                || minecraft.getMainRenderTarget() == null) {
            screenWidth = 0;
            screenHeight = 0;
            tilesX = 0;
            tilesY = 0;
            preparedSerial = frameSerial;
            return;
        }

        screenWidth = minecraft.getMainRenderTarget().width;
        screenHeight = minecraft.getMainRenderTarget().height;

        if (screenWidth <= 0 || screenHeight <= 0) {
            tilesX = 0;
            tilesY = 0;
            preparedSerial = frameSerial;
            return;
        }

        tilesX = (screenWidth + TILE_SIZE - 1) / TILE_SIZE;
        tilesY = (screenHeight + TILE_SIZE - 1) / TILE_SIZE;

        long tileCountLong = (long) tilesX * tilesY;
        int maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);

        if (tileCountLong <= 0 || tileCountLong > maxTextureSize) {
            throw new IllegalStateException(
                    "[TheatricalExtraLights/Gobo] Invalid tile texture height: " + tileCountLong
                            + " (GPU limit: " + maxTextureSize + ")"
            );
        }

        int tileCount = (int) tileCountLong;
        Camera camera = minecraft.gameRenderer.getMainCamera();

        if (camera == null) {
            preparedSerial = frameSerial;
            return;
        }

        PoseStack viewStack = new PoseStack();
        viewStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        viewStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0f));

        Matrix4f viewMatrix = new Matrix4f(viewStack.last().pose());
        Matrix4f projectionMatrix = new Matrix4f(RenderSystem.getProjectionMatrix());

        if (cpuTileLists.length != tileCount) {
            cpuTileLists = Arrays.copyOf(cpuTileLists, tileCount);
            cpuTileCounts = Arrays.copyOf(cpuTileCounts, tileCount);
        }

        Arrays.fill(cpuTileCounts, 0);
        saturatedTileCount = 0;

        ACTIVE.sort(PROJECTOR_IMPORTANCE_ORDER);

        for (Projector projector : ACTIVE) {
            if (preparedProjectorCount >= MAX_PROJECTORS
                    || saturatedTileCount >= tileCount) {
                break;
            }

            ViewProjector view = toViewSpace(
                    projector,
                    camera.getPosition(),
                    viewMatrix,
                    VIEW_PROJECTOR_SCRATCH
            );

            int projectorIndex = preparedProjectorCount;

            packProjector(PROJECTOR_DATA, projectorIndex, view);

            addToScreenTiles(
                    view,
                    projectionMatrix,
                    projectorIndex,
                    cpuTileLists,
                    cpuTileCounts,
                    camera.getPosition()
            );

            preparedProjectorCount++;
        }

        for (int count : cpuTileCounts) {
            if (count > preparedMaxPerTile) {
                preparedMaxPerTile = count;
            }
        }

        uploadTextures(
                PROJECTOR_DATA,
                cpuTileLists,
                cpuTileCounts,
                tileCount,
                preparedMaxPerTile
        );

        preparedSerial = frameSerial;
    }

    private static ViewProjector toViewSpace(
            Projector projector,
            Vec3 cameraPosition,
            Matrix4f viewMatrix,
            ViewProjector result
    ) {
        result.source = projector;

        SCRATCH_POSITION.set(
                (float) (projector.origin.x - cameraPosition.x),
                (float) (projector.origin.y - cameraPosition.y),
                (float) (projector.origin.z - cameraPosition.z),
                1.0f
        );

        viewMatrix.transform(SCRATCH_POSITION);

        result.px = SCRATCH_POSITION.x;
        result.py = SCRATCH_POSITION.y;
        result.pz = SCRATCH_POSITION.z;

        transformDirection(viewMatrix, projector.direction, SCRATCH_DIRECTION);
        result.dx = SCRATCH_DIRECTION.x;
        result.dy = SCRATCH_DIRECTION.y;
        result.dz = SCRATCH_DIRECTION.z;

        transformDirection(viewMatrix, projector.axisU, SCRATCH_AXIS_U);
        result.ux = SCRATCH_AXIS_U.x;
        result.uy = SCRATCH_AXIS_U.y;
        result.uz = SCRATCH_AXIS_U.z;

        transformDirection(viewMatrix, projector.axisV, SCRATCH_AXIS_V);
        result.vx = SCRATCH_AXIS_V.x;
        result.vy = SCRATCH_AXIS_V.y;
        result.vz = SCRATCH_AXIS_V.z;

        result.occlusionEnabled = projector.occlusionEnabled;

        result.opx = 0.0f;
        result.opy = 0.0f;
        result.opz = 0.0f;
        result.onx = 0.0f;
        result.ony = 0.0f;
        result.onz = 0.0f;

        if (projector.occlusionEnabled) {
            SCRATCH_OCCLUSION_POSITION.set(
                    (float) (projector.occlusionPosition.x - cameraPosition.x),
                    (float) (projector.occlusionPosition.y - cameraPosition.y),
                    (float) (projector.occlusionPosition.z - cameraPosition.z),
                    1.0f
            );

            viewMatrix.transform(SCRATCH_OCCLUSION_POSITION);

            transformDirection(
                    viewMatrix,
                    projector.occlusionNormal,
                    SCRATCH_OCCLUSION_NORMAL
            );

            result.opx = SCRATCH_OCCLUSION_POSITION.x;
            result.opy = SCRATCH_OCCLUSION_POSITION.y;
            result.opz = SCRATCH_OCCLUSION_POSITION.z;

            result.onx = SCRATCH_OCCLUSION_NORMAL.x;
            result.ony = SCRATCH_OCCLUSION_NORMAL.y;
            result.onz = SCRATCH_OCCLUSION_NORMAL.z;
        }

        return result;
    }

    private static void transformDirection(
            Matrix4f matrix,
            Vec3 direction,
            Vector4f result
    ) {
        result.set(
                (float) direction.x,
                (float) direction.y,
                (float) direction.z,
                0.0f
        );

        matrix.transform(result);

        float length = (float) Math.sqrt(
                result.x * result.x
                        + result.y * result.y
                        + result.z * result.z
        );

        if (length > 0.00001f) {
            result.x /= length;
            result.y /= length;
            result.z /= length;
        }
    }

    private static void packProjector(float[] data, int index, ViewProjector view) {
        Projector projector = view.source;
        int offset = index * DATA_FLOATS_PER_PROJECTOR;

        put4(data, offset, view.px, view.py, view.pz, projector.baseRadius);
        put4(data, offset + 4, view.dx, view.dy, view.dz, projector.tanHalfAngle);
        put4(data, offset + 8, view.ux, view.uy, view.uz, projector.maxDistance);
        put4(data, offset + 12, view.vx, view.vy, view.vz, projector.intensity);

        put4(
                data,
                offset + 16,
                (projector.color >> 16 & 255) / 255.0f,
                (projector.color >> 8 & 255) / 255.0f,
                (projector.color & 255) / 255.0f,
                projector.rotationRadians
        );

        put4(
                data,
                offset + 20,
                projector.outgoingLayer,
                projector.incomingLayer,
                projector.animationLayer,
                projector.focus
        );

        put4(
                data,
                offset + 24,
                projector.wheelTransition,
                projector.animationAngle,
                projector.animationOffset,
                projector.animationEnabled ? 1.0f : 0.0f
        );

        put4(
                data,
                offset + 28,
                projector.shutterA[0],
                projector.shutterA[1],
                projector.shutterA[2],
                projector.shutterA[3]
        );

        put4(
                data,
                offset + 32,
                projector.shutterB[0],
                projector.shutterB[1],
                projector.shutterB[2],
                projector.shutterB[3]
        );

        put4(
                data,
                offset + 36,
                projector.frameRotation,
                view.occlusionEnabled ? 1.0f : 0.0f,
                0.0f,
                0.0f
        );

        put4(data, offset + 40, view.opx, view.opy, view.opz, 0.0f);
        put4(data, offset + 44, view.onx, view.ony, view.onz, 0.0f);
    }

    private static void appendProjectorToTile(
            int projectorIndex,
            int tileIndex,
            int[][] tileLists,
            int[] tileCounts
    ) {
        int count = tileCounts[tileIndex];

        if (count >= MAX_PROJECTORS) {
            return;
        }

        int[] list = tileLists[tileIndex];

        if (list == null) {
            list = new int[Math.min(MAX_PER_TILE, MAX_PROJECTORS)];
            tileLists[tileIndex] = list;
        } else if (count >= list.length) {
            int newCapacity = Math.min(
                    MAX_PROJECTORS,
                    Math.max(count + 1, list.length * 2)
            );

            list = Arrays.copyOf(list, newCapacity);
            tileLists[tileIndex] = list;
        }

        list[count] = projectorIndex;
        tileCounts[tileIndex] = count + 1;
    }

    private static void addToScreenTiles(
            ViewProjector view,
            Matrix4f projection,
            int projectorIndex,
            int[][] tileLists,
            int[] tileCounts,
            Vec3 cameraPosition
    ) {
        Projector projector = view.source;

        if (isCameraInsideBeam(projector, cameraPosition)) {
            for (int tile = 0; tile < tileCounts.length; tile++) {
                appendProjectorToTile(
                        projectorIndex,
                        tile,
                        tileLists,
                        tileCounts
                );
            }

            return;
        }

        CLIP_W_RANGE[0] = Float.POSITIVE_INFINITY;
        CLIP_W_RANGE[1] = Float.NEGATIVE_INFINITY;

        accumulateClipWRange(
                view,
                projection,
                0.0f,
                projector.baseRadius,
                CLIP_W_RANGE
        );

        float endDistance = projector.maxDistance;
        float endRadius = projector.baseRadius
                + endDistance * projector.tanHalfAngle;

        accumulateClipWRange(
                view,
                projection,
                endDistance,
                endRadius,
                CLIP_W_RANGE
        );

        // The whole projector volume is behind the camera.
        if (CLIP_W_RANGE[1] <= 0.001f) {
            return;
        }

        // The volume crosses the camera plane. Use conservative full-screen coverage.
        if (CLIP_W_RANGE[0] <= 0.001f) {
            addProjectorToAllTiles(
                    projectorIndex,
                    tileLists,
                    tileCounts
            );
            return;
        }

        SCREEN_BOUNDS[0] = Float.POSITIVE_INFINITY;
        SCREEN_BOUNDS[1] = Float.POSITIVE_INFINITY;
        SCREEN_BOUNDS[2] = Float.NEGATIVE_INFINITY;
        SCREEN_BOUNDS[3] = Float.NEGATIVE_INFINITY;

        for (int ring = 0; ring < 2; ring++) {
            float distance = ring == 0 ? 0.0f : endDistance;
            float radius = ring == 0 ? projector.baseRadius : endRadius;

            float centerX = view.px + view.dx * distance;
            float centerY = view.py + view.dy * distance;
            float centerZ = view.pz + view.dz * distance;

            for (int i = 0; i < SCREEN_BOUND_SAMPLES; i++) {
                float cos = SCREEN_BOUND_COS[i];
                float sin = SCREEN_BOUND_SIN[i];

                float x = centerX + (view.ux * cos + view.vx * sin) * radius;
                float y = centerY + (view.uy * cos + view.vy * sin) * radius;
                float z = centerZ + (view.uz * cos + view.vz * sin) * radius;

                if (!accumulateProjectedPoint(x, y, z, projection, SCREEN_BOUNDS)) {
                    addProjectorToAllTiles(
                            projectorIndex,
                            tileLists,
                            tileCounts
                    );
                    return;
                }
            }
        }

        if (!Float.isFinite(SCREEN_BOUNDS[0])
                || !Float.isFinite(SCREEN_BOUNDS[1])
                || !Float.isFinite(SCREEN_BOUNDS[2])
                || !Float.isFinite(SCREEN_BOUNDS[3])) {
            addProjectorToAllTiles(
                    projectorIndex,
                    tileLists,
                    tileCounts
            );
            return;
        }

        if (SCREEN_BOUNDS[2] < 0.0f
                || SCREEN_BOUNDS[3] < 0.0f
                || SCREEN_BOUNDS[0] > screenWidth
                || SCREEN_BOUNDS[1] > screenHeight) {
            return;
        }

        // Covers the gaps between the sampled circumference points and rasterization edges.
        float margin = 16.0f;

        int minTileX = clampInt(
                (int) Math.floor((SCREEN_BOUNDS[0] - margin) / TILE_SIZE),
                0,
                tilesX - 1
        );

        int minTileY = clampInt(
                (int) Math.floor((SCREEN_BOUNDS[1] - margin) / TILE_SIZE),
                0,
                tilesY - 1
        );

        int maxTileX = clampInt(
                (int) Math.floor((SCREEN_BOUNDS[2] + margin) / TILE_SIZE),
                0,
                tilesX - 1
        );

        int maxTileY = clampInt(
                (int) Math.floor((SCREEN_BOUNDS[3] + margin) / TILE_SIZE),
                0,
                tilesY - 1
        );

        for (int y = minTileY; y <= maxTileY; y++) {
            for (int x = minTileX; x <= maxTileX; x++) {
                int tileIndex = y * tilesX + x;

                appendProjectorToTile(
                        projectorIndex,
                        tileIndex,
                        tileLists,
                        tileCounts
                );
            }
        }
    }

    private static void addProjectorToAllTiles(
            int projectorIndex,
            int[][] tileLists,
            int[] tileCounts
    ) {
        for (int tile = 0; tile < tileCounts.length; tile++) {
            appendProjectorToTile(
                    projectorIndex,
                    tile,
                    tileLists,
                    tileCounts
            );
        }
    }

    private static void accumulateClipWRange(
            ViewProjector view,
            Matrix4f projection,
            float distance,
            float radius,
            float[] range
    ) {
        SCRATCH_CLIP_CENTER.set(
                view.px + view.dx * distance,
                view.py + view.dy * distance,
                view.pz + view.dz * distance,
                1.0f
        );

        SCRATCH_CLIP_U.set(view.ux, view.uy, view.uz, 0.0f);
        SCRATCH_CLIP_V.set(view.vx, view.vy, view.vz, 0.0f);

        projection.transform(SCRATCH_CLIP_CENTER);
        projection.transform(SCRATCH_CLIP_U);
        projection.transform(SCRATCH_CLIP_V);

        float radialW = radius * (float) Math.sqrt(
                SCRATCH_CLIP_U.w * SCRATCH_CLIP_U.w
                        + SCRATCH_CLIP_V.w * SCRATCH_CLIP_V.w
        );

        range[0] = Math.min(range[0], SCRATCH_CLIP_CENTER.w - radialW);
        range[1] = Math.max(range[1], SCRATCH_CLIP_CENTER.w + radialW);
    }

    private static boolean accumulateProjectedPoint(
            float x,
            float y,
            float z,
            Matrix4f projection,
            float[] bounds
    ) {
        SCRATCH_CLIP_POINT.set(x, y, z, 1.0f);
        projection.transform(SCRATCH_CLIP_POINT);

        if (!Float.isFinite(SCRATCH_CLIP_POINT.w)
                || SCRATCH_CLIP_POINT.w <= 0.001f) {
            return false;
        }

        float ndcX = SCRATCH_CLIP_POINT.x / SCRATCH_CLIP_POINT.w;
        float ndcY = SCRATCH_CLIP_POINT.y / SCRATCH_CLIP_POINT.w;

        if (!Float.isFinite(ndcX) || !Float.isFinite(ndcY)) {
            return false;
        }

        float screenX = (ndcX * 0.5f + 0.5f) * screenWidth;
        float screenY = (ndcY * 0.5f + 0.5f) * screenHeight;

        bounds[0] = Math.min(bounds[0], screenX);
        bounds[1] = Math.min(bounds[1], screenY);
        bounds[2] = Math.max(bounds[2], screenX);
        bounds[3] = Math.max(bounds[3], screenY);

        return true;
    }

    private static void uploadTextures(
            float[] projectorData,
            int[][] tileLists,
            int[] tileCounts,
            int tileCount,
            int maxPerTile
    ) {
        RenderSystem.assertOnRenderThread();

        int uploadWidth = Math.max(1, maxPerTile);
        int maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);

        if (tileCount <= 0
                || tileCount > maxTextureSize
                || uploadWidth > maxTextureSize) {
            throw new IllegalStateException(
                    "[TheatricalExtraLights/Gobo] Invalid tile texture dimensions: "
                            + uploadWidth + "x" + tileCount
                            + " (GPU limit: " + maxTextureSize + ")"
            );
        }

        if (projectorData.length != DATA_COLUMNS * MAX_PROJECTORS * 4) {
            throw new IllegalArgumentException(
                    "[TheatricalExtraLights/Gobo] Invalid projector buffer size: "
                            + projectorData.length
            );
        }

        if (tileLists.length != tileCount || tileCounts.length != tileCount) {
            throw new IllegalArgumentException(
                    "[TheatricalExtraLights/Gobo] Invalid tile buffer dimensions"
            );
        }

        GoboTextureArray.finishUploads();

        int previousActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);

        int previousBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        try (GoboPixelUnpackState unpackState = new GoboPixelUnpackState()) {
            ensureDataTexture();
            ensureTileTexture(tileCount, uploadWidth);

            if (preparedProjectorCount > 0) {
                int projectorFloatCount = preparedProjectorCount * DATA_FLOATS_PER_PROJECTOR;

                if (projectorUploadBuffer == null
                        || projectorUploadBuffer.capacity() < projectorFloatCount) {
                    projectorUploadBuffer = BufferUtils.createFloatBuffer(projectorFloatCount);
                }

                projectorUploadBuffer.clear();
                projectorUploadBuffer.put(projectorData, 0, projectorFloatCount);
                projectorUploadBuffer.flip();

                bindTexture2D(dataTexture, "uploadProjectorData");

                GL11.glTexSubImage2D(
                        GL11.GL_TEXTURE_2D,
                        0,
                        0,
                        0,
                        DATA_COLUMNS,
                        preparedProjectorCount,
                        GL11.GL_RGBA,
                        GL11.GL_FLOAT,
                        projectorUploadBuffer
                );

                checkUploadError("projector data");
            }

            long tileValueCount = (long) tileCount * uploadWidth;

            if (tileValueCount > Integer.MAX_VALUE) {
                throw new IllegalStateException(
                        "[TheatricalExtraLights/Gobo] Tile index buffer is too large: "
                                + tileValueCount
                );
            }

            if (tileUploadBuffer == null
                    || tileUploadBuffer.capacity() < (int) tileValueCount) {
                tileUploadBuffer = BufferUtils.createFloatBuffer((int) tileValueCount);
            }

            tileUploadBuffer.clear();

            for (int tile = 0; tile < tileCount; tile++) {
                int count = tileCounts[tile];

                if (count < 0 || count > uploadWidth) {
                    throw new IllegalStateException(
                            "[TheatricalExtraLights/Gobo] Invalid projector count in tile "
                                    + tile + ": " + count
                    );
                }

                int[] list = tileLists[tile];

                if (count > 0 && (list == null || count > list.length)) {
                    throw new IllegalStateException(
                            "[TheatricalExtraLights/Gobo] Missing projector indices for tile "
                                    + tile
                    );
                }

                for (int slot = 0; slot < count; slot++) {
                    tileUploadBuffer.put((float) list[slot]);
                }

                for (int slot = count; slot < uploadWidth; slot++) {
                    tileUploadBuffer.put(-1.0f);
                }
            }

            tileUploadBuffer.flip();

            if (tileUploadBuffer.remaining() != tileValueCount) {
                throw new IllegalStateException(
                        "[TheatricalExtraLights/Gobo] Tile FloatBuffer size mismatch"
                );
            }

            bindTexture2D(tileTexture, "uploadTileIndices");

            // R32F stores one float per projector index instead of wasting RGBA channels.
            GL11.glTexSubImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    0,
                    0,
                    uploadWidth,
                    tileCount,
                    GL11.GL_RED,
                    GL11.GL_FLOAT,
                    tileUploadBuffer
            );

            checkUploadError("tile indices");
        } finally {
            bindTexture2D(previousBinding, "restoreTextureBinding");
            RenderSystem.activeTexture(previousActive);
        }
    }

    private static void bindTexture2D(int texture, String operation) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        RenderSystem.bindTexture(0);
        RenderSystem.bindTexture(texture);

        int actualBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int error = GL11.glGetError();

        if (actualBinding != texture || error != GL11.GL_NO_ERROR) {
            throw new IllegalStateException(
                    "[TheatricalExtraLights/Gobo] Texture binding failed during "
                            + operation
                            + " | requested=" + texture
                            + " | actual=" + actualBinding
                            + " | error=0x" + Integer.toHexString(error)
                            + " | atlas=" + GoboTextureArray.getTextureId()
            );
        }
    }

    private static void checkUploadError(String operation) {
        int error = GL11.glGetError();

        if (error != GL11.GL_NO_ERROR) {
            throw new IllegalStateException(
                    "[TheatricalExtraLights/Gobo] OpenGL error during "
                            + operation
                            + ": 0x" + Integer.toHexString(error)
            );
        }
    }

    private static void ensureDataTexture() {
        if (dataTexture != 0) {
            bindTexture2D(dataTexture, "ensureDataTexture");
            return;
        }

        int maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);

        if (MAX_PROJECTORS > maxTextureSize) {
            throw new IllegalStateException(
                    "[TheatricalExtraLights/Gobo] GPU texture limit is too small for "
                            + MAX_PROJECTORS + " projectors: " + maxTextureSize
            );
        }

        dataTexture = GL11.glGenTextures();
        bindTexture2D(dataTexture, "createDataTexture");

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL30.GL_RGBA32F,
                DATA_COLUMNS,
                MAX_PROJECTORS,
                0,
                GL11.GL_RGBA,
                GL11.GL_FLOAT,
                (ByteBuffer) null
        );

        checkUploadError("create projector data texture");
    }

    private static void ensureTileTexture(int tileCount, int requiredWidth) {
        if (tileTexture == 0) {
            tileTexture = GL11.glGenTextures();
        }

        bindTexture2D(tileTexture, "ensureTileTexture");

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        int maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);

        int minimumWidth = Math.max(MAX_PER_TILE, requiredWidth);
        int newWidth = tileTextureWidth;

        if (newWidth < minimumWidth) {
            newWidth = nextPowerOfTwo(minimumWidth);
        }

        if (newWidth > maxTextureSize || tileCount > maxTextureSize) {
            throw new IllegalStateException(
                    "[TheatricalExtraLights/Gobo] Tile texture exceeds GPU limits: "
                            + newWidth + "x" + tileCount
                            + " (GPU limit: " + maxTextureSize + ")"
            );
        }

        if (tileTextureWidth != newWidth || tileTextureHeight != tileCount) {
            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL30.GL_R32F,
                    newWidth,
                    tileCount,
                    0,
                    GL11.GL_RED,
                    GL11.GL_FLOAT,
                    (ByteBuffer) null
            );

            checkUploadError("resize tile index texture");

            tileTextureWidth = newWidth;
            tileTextureHeight = tileCount;
        }
    }

    private static int nextPowerOfTwo(int value) {
        if (value <= 1) {
            return 1;
        }

        int highest = Integer.highestOneBit(value - 1);

        if (highest >= (1 << 30)) {
            return value;
        }

        return highest << 1;
    }

    private static boolean isFinite(Vec3 vector) {
        return vector != null
                && Double.isFinite(vector.x)
                && Double.isFinite(vector.y)
                && Double.isFinite(vector.z);
    }

    private static void put4(
            float[] data,
            int offset,
            float x,
            float y,
            float z,
            float w
    ) {
        data[offset] = x;
        data[offset + 1] = y;
        data[offset + 2] = z;
        data[offset + 3] = w;
    }

    private static float clamp(float value, float min, float max) {
        if (!Float.isFinite(value)) {
            return min;
        }

        return Math.max(min, Math.min(max, value));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}