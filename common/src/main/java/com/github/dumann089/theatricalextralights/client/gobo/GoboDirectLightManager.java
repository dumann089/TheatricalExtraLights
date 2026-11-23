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
import java.util.List;

public final class GoboDirectLightManager {
    public static final int INITIAL_TILE_CAPACITY = 8;
    public static final int TILE_SIZE = 64;

    public static final int FLAG_OCCLUSION = 1;
    public static final int FLAG_WHEEL_TRANSITION = 2;
    public static final int FLAG_SHUTTERS = 4;
    public static final int FLAG_ANIMATION = 8;

    private static final int DATA_COLUMNS = 16;
    private static final int DATA_FLOATS_PER_PROJECTOR = DATA_COLUMNS * 4;
    private static float[] PROJECTOR_DATA = new float[2048 * DATA_FLOATS_PER_PROJECTOR];

    private static final float[] SCREEN_BOUNDS = new float[4];
    private static final float[] PROJECTED_AXIS_BOUNDS = new float[2];

    private static final Vector4f SCRATCH_POSITION = new Vector4f();
    private static final Vector4f SCRATCH_DIRECTION = new Vector4f();
    private static final Vector4f SCRATCH_AXIS_U = new Vector4f();
    private static final Vector4f SCRATCH_AXIS_V = new Vector4f();
    private static final Vector4f SCRATCH_OCCLUSION_POSITION = new Vector4f();
    private static final Vector4f SCRATCH_OCCLUSION_NORMAL = new Vector4f();

    private static final ViewProjector VIEW_PROJECTOR_SCRATCH = new ViewProjector();

    private static int[][] cpuTileLists = new int[0][];
    private static int[] cpuTileCounts = new int[0];
    private static boolean[] dirtyTileRows = new boolean[0];

    private static int[][] uploadedTileLists = new int[0][];
    private static int[] uploadedTileCounts = new int[0];

    private static FloatBuffer projectorUploadBuffer;
    private static FloatBuffer tileUploadBuffer;

    private static final List<Projector> ACTIVE = new ArrayList<>(2048);
    private static final List<Projector> PENDING = new ArrayList<>(2048);

    private static int dataTexture;
    private static int tileTexture;
    private static int dataTextureHeight;
    private static int tileTextureWidth;
    private static int tileTextureHeight;

    private static int screenWidth;
    private static int screenHeight;
    private static int activeTileSize = TILE_SIZE;
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
        float blurLod;
        float wheelTransition;
        float animationAngle;
        float animationOffset;
        float frameRotation;

        int color;
        int outgoingLayer;
        int incomingLayer;
        int animationLayer;

        boolean animationEnabled;
        boolean wheelTransitionEnabled;
        boolean shuttersEnabled;
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
        projector.blurLod = clamp(
                (float) (Math.log(1.0 + projector.focus * projector.focus * 11.52) / Math.log(2.0)),
                0.0f,
                4.0f
        );
        projector.wheelTransition = clamp(wheelTransition, 0.0f, 1.0f);
        projector.color = color & 0xFFFFFF;

        projector.outgoingLayer = GoboTextureArray.getLayer(outgoingTexture);
        projector.incomingLayer = GoboTextureArray.getLayer(
                incomingTexture != null ? incomingTexture : outgoingTexture
        );

        projector.wheelTransitionEnabled = incomingTexture != null
                && projector.wheelTransition > 0.0001f
                && projector.incomingLayer > 0;

        if (animation != null && animation.texture() != null) {
            projector.animationLayer = GoboTextureArray.getLayer(animation.texture());
            projector.animationAngle = (float) Math.toRadians(animation.angleDeg());
            projector.animationOffset = animation.offset();
            projector.animationEnabled = projector.animationLayer > 0;
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
            projector.shuttersEnabled = true;
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

        PENDING.add(projector);
    }

    public static void bindAtlas(ShaderInstance shader) {
        if (shader == null || shader.getUniform("GoboTileSize") == null) {
            return;
        }

        int atlasTexture = GoboTextureArray.finishUploads();
        int uniformLocation = GL20.glGetUniformLocation(shader.getId(), "GoboAtlas");

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
        shader.safeGetUniform("GoboTileSize").set((float) activeTileSize);
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
        if (GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) != programId) {
            return;
        }

        int maxUnits = GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS);
        if (maxUnits < 3) {
            return;
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
        if (maxUnits <= 10) {
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

        if (dataLocation < 0 || tileLocation < 0 || atlasLocation < 0
                || countLocation < 0 || tileSizeLocation < 0
                || tilesXLocation < 0 || tilesYLocation < 0
                || maxPerTileLocation < 0 || screenSizeLocation < 0
                || brightnessLocation < 0) {
            return;
        }

        int previousActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        try {
            RenderSystem.activeTexture(GL13.GL_TEXTURE8);
            RenderSystem.bindTexture(dataTexture);
            GL20.glUniform1i(dataLocation, 8);

            RenderSystem.activeTexture(GL13.GL_TEXTURE9);
            RenderSystem.bindTexture(tileTexture);
            GL20.glUniform1i(tileLocation, 9);

            RenderSystem.activeTexture(GL13.GL_TEXTURE10);
            GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, GoboTextureArray.getTextureId());
            GL20.glUniform1i(atlasLocation, 10);

            GL20.glUniform1i(countLocation, preparedProjectorCount);
            GL20.glUniform1i(tileSizeLocation, activeTileSize);
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

        int maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        activeTileSize = TILE_SIZE;

        long tileCountLong;
        while (true) {
            tilesX = (screenWidth + activeTileSize - 1) / activeTileSize;
            tilesY = (screenHeight + activeTileSize - 1) / activeTileSize;
            tileCountLong = (long) tilesX * tilesY;

            if (tileCountLong <= maxTextureSize) {
                break;
            }

            activeTileSize *= 2;
            if (activeTileSize > 4096) {
                throw new IllegalStateException(
                        "[TheatricalExtraLights/Gobo] Screen tile grid exceeds GPU limits: "
                                + screenWidth + "x" + screenHeight
                );
            }
        }

        int tileCount = (int) tileCountLong;
        if (ACTIVE.size() > maxTextureSize) {
            throw new IllegalStateException(
                    "[TheatricalExtraLights/Gobo] Active projector count " + ACTIVE.size()
                            + " exceeds GL_MAX_TEXTURE_SIZE=" + maxTextureSize
                            + ". No projectors were silently discarded."
            );
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (camera == null) {
            preparedSerial = frameSerial;
            return;
        }

        ensureProjectorDataCapacity(ACTIVE.size());
        ensureCpuTileCapacity(tileCount);
        Arrays.fill(cpuTileCounts, 0);

        PoseStack viewStack = new PoseStack();
        viewStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        viewStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0f));

        Matrix4f viewMatrix = new Matrix4f(viewStack.last().pose());
        Matrix4f projectionMatrix = new Matrix4f(RenderSystem.getProjectionMatrix());

        for (Projector projector : ACTIVE) {
            int projectorIndex = preparedProjectorCount;
            ViewProjector view = toViewSpace(
                    projector,
                    camera.getPosition(),
                    viewMatrix,
                    VIEW_PROJECTOR_SCRATCH
            );

            packProjector(PROJECTOR_DATA, projectorIndex, view);
            addToScreenTiles(view, projectionMatrix, projectorIndex);
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

    private static void ensureCpuTileCapacity(int tileCount) {
        if (cpuTileLists.length == tileCount) {
            return;
        }

        cpuTileLists = Arrays.copyOf(cpuTileLists, tileCount);
        cpuTileCounts = Arrays.copyOf(cpuTileCounts, tileCount);
        dirtyTileRows = new boolean[tileCount];

        uploadedTileLists = new int[tileCount][];
        uploadedTileCounts = new int[tileCount];
        Arrays.fill(uploadedTileCounts, -1);
    }

    private static void ensureProjectorDataCapacity(int projectorCount) {
        long requiredLong = (long) projectorCount * DATA_FLOATS_PER_PROJECTOR;
        if (requiredLong > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "[TheatricalExtraLights/Gobo] Projector CPU buffer is too large: " + requiredLong
            );
        }

        int required = (int) requiredLong;
        if (PROJECTOR_DATA.length >= required) {
            return;
        }

        int newCapacity = PROJECTOR_DATA.length;
        while (newCapacity < required) {
            int next = newCapacity <= Integer.MAX_VALUE / 2
                    ? newCapacity * 2
                    : Integer.MAX_VALUE;
            if (next <= newCapacity) {
                throw new IllegalStateException(
                        "[TheatricalExtraLights/Gobo] Cannot grow projector CPU buffer"
                );
            }
            newCapacity = next;
        }

        PROJECTOR_DATA = Arrays.copyOf(PROJECTOR_DATA, newCapacity);
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

        int featureFlags = 0;
        if (projector.occlusionEnabled) {
            featureFlags |= FLAG_OCCLUSION;
        }
        if (projector.wheelTransitionEnabled) {
            featureFlags |= FLAG_WHEEL_TRANSITION;
        }
        if (projector.shuttersEnabled) {
            featureFlags |= FLAG_SHUTTERS;
        }
        if (projector.animationEnabled) {
            featureFlags |= FLAG_ANIMATION;
        }

        put4(
                data,
                offset + 36,
                view.occlusionEnabled ? 1.0f : 0.0f,
                0.0f,
                0.0f,
                0.0f
        );
        put4(data, offset + 40, view.opx, view.opy, view.opz, 0.0f);
        put4(data, offset + 44, view.onx, view.ony, view.onz, 0.0f);
        put4(
                data,
                offset + 48,
                (float) featureFlags,
                projector.blurLod,
                projector.frameRotation,
                0.0f
        );
        put4(data, offset + 52, 0.0f, 0.0f, 0.0f, 0.0f);
        put4(data, offset + 56, 0.0f, 0.0f, 0.0f, 0.0f);
        put4(data, offset + 60, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    private static void addToScreenTiles(
            ViewProjector view,
            Matrix4f projection,
            int projectorIndex
    ) {
        Projector projector = view.source;
        float length = projector.maxDistance;
        float endRadius = projector.baseRadius + length * projector.tanHalfAngle;

        float centerAlongAxis = (length * length + endRadius * endRadius
                - projector.baseRadius * projector.baseRadius) / (2.0f * length);
        centerAlongAxis = clamp(centerAlongAxis, 0.0f, length);

        float centerX = view.px + view.dx * centerAlongAxis;
        float centerY = view.py + view.dy * centerAlongAxis;
        float centerZ = view.pz + view.dz * centerAlongAxis;

        float startSphereRadiusSquared = centerAlongAxis * centerAlongAxis
                + projector.baseRadius * projector.baseRadius;
        float endOffset = length - centerAlongAxis;
        float endSphereRadiusSquared = endOffset * endOffset + endRadius * endRadius;
        float sphereRadius = (float) Math.sqrt(
                Math.max(startSphereRadiusSquared, endSphereRadiusSquared)
        );

        float projectionA = projection.m22() - 1.0f;
        float nearDistance = Math.abs(projectionA) > 1.0E-6f
                ? projection.m32() / projectionA
                : 0.05f;
        if (!Float.isFinite(nearDistance) || nearDistance <= 0.0001f) {
            nearDistance = 0.05f;
        }

        float centerDepth = -centerZ;
        if (centerDepth + sphereRadius < nearDistance) {
            return;
        }

        if (!projectSphereAxisBounds(
                centerX,
                centerDepth,
                sphereRadius,
                nearDistance,
                projection.m00(),
                -projection.m20(),
                screenWidth,
                PROJECTED_AXIS_BOUNDS
        )) {
            return;
        }

        SCREEN_BOUNDS[0] = PROJECTED_AXIS_BOUNDS[0];
        SCREEN_BOUNDS[2] = PROJECTED_AXIS_BOUNDS[1];

        if (!projectSphereAxisBounds(
                centerY,
                centerDepth,
                sphereRadius,
                nearDistance,
                projection.m11(),
                -projection.m21(),
                screenHeight,
                PROJECTED_AXIS_BOUNDS
        )) {
            return;
        }

        SCREEN_BOUNDS[1] = PROJECTED_AXIS_BOUNDS[0];
        SCREEN_BOUNDS[3] = PROJECTED_AXIS_BOUNDS[1];

        float minPixelX = SCREEN_BOUNDS[0] - 1.0f;
        float minPixelY = SCREEN_BOUNDS[1] - 1.0f;
        float maxPixelX = SCREEN_BOUNDS[2] + 1.0f;
        float maxPixelY = SCREEN_BOUNDS[3] + 1.0f;

        if (maxPixelX < 0.0f || maxPixelY < 0.0f
                || minPixelX > screenWidth - 1.0f
                || minPixelY > screenHeight - 1.0f) {
            return;
        }

        minPixelX = Math.max(0.0f, minPixelX);
        minPixelY = Math.max(0.0f, minPixelY);
        maxPixelX = Math.min(screenWidth - 1.0f, maxPixelX);
        maxPixelY = Math.min(screenHeight - 1.0f, maxPixelY);

        int minTileX = clampInt(
                (int) Math.floor(minPixelX / activeTileSize),
                0,
                tilesX - 1
        );
        int minTileY = clampInt(
                (int) Math.floor(minPixelY / activeTileSize),
                0,
                tilesY - 1
        );
        int maxTileX = clampInt(
                (int) Math.floor(maxPixelX / activeTileSize),
                0,
                tilesX - 1
        );
        int maxTileY = clampInt(
                (int) Math.floor(maxPixelY / activeTileSize),
                0,
                tilesY - 1
        );

        for (int y = minTileY; y <= maxTileY; y++) {
            int rowOffset = y * tilesX;
            for (int x = minTileX; x <= maxTileX; x++) {
                appendProjectorToTile(projectorIndex, rowOffset + x);
            }
        }
    }

    private static boolean projectSphereAxisBounds(
            float centerLateral,
            float centerDepth,
            float radius,
            float nearDistance,
            float projectionScale,
            float projectionOffset,
            int viewportSize,
            float[] result
    ) {
        if (viewportSize <= 0 || radius <= 0.0f
                || centerDepth + radius < nearDistance) {
            return false;
        }

        double lateral = centerLateral;
        double depth = centerDepth;
        double sphereRadius = radius;
        double depthSquared = depth * depth;
        double radiusSquared = sphereRadius * sphereRadius;
        double lateralDistanceSquared = lateral * lateral + depthSquared;

        if (lateralDistanceSquared <= radiusSquared
                || depthSquared <= radiusSquared + 1.0E-8) {
            result[0] = 0.0f;
            result[1] = viewportSize - 1.0f;
            return true;
        }

        double denominator = depthSquared - radiusSquared;
        double discriminant = radiusSquared * (lateralDistanceSquared - radiusSquared);
        if (denominator <= 1.0E-8 || discriminant < 0.0) {
            result[0] = 0.0f;
            result[1] = viewportSize - 1.0f;
            return true;
        }

        double root = Math.sqrt(discriminant);
        double lowerSlope = (lateral * depth - root) / denominator;
        double upperSlope = (lateral * depth + root) / denominator;

        if (depth - sphereRadius < nearDistance) {
            double nearDelta = depth - nearDistance;
            double capSquared = radiusSquared - nearDelta * nearDelta;
            if (capSquared < 0.0) {
                return false;
            }

            double capRadius = Math.sqrt(Math.max(0.0, capSquared));
            double lowerContactDepth = (lateral * lowerSlope + depth)
                    / (1.0 + lowerSlope * lowerSlope);
            double upperContactDepth = (lateral * upperSlope + depth)
                    / (1.0 + upperSlope * upperSlope);

            if (lowerContactDepth < nearDistance) {
                lowerSlope = (lateral - capRadius) / nearDistance;
            }
            if (upperContactDepth < nearDistance) {
                upperSlope = (lateral + capRadius) / nearDistance;
            }
        }

        double ndcA = projectionScale * lowerSlope + projectionOffset;
        double ndcB = projectionScale * upperSlope + projectionOffset;
        double pixelA = (Math.min(ndcA, ndcB) * 0.5 + 0.5) * viewportSize;
        double pixelB = (Math.max(ndcA, ndcB) * 0.5 + 0.5) * viewportSize;

        if (!Double.isFinite(pixelA) || !Double.isFinite(pixelB)) {
            result[0] = 0.0f;
            result[1] = viewportSize - 1.0f;
            return true;
        }

        result[0] = (float) pixelA;
        result[1] = (float) pixelB;
        return true;
    }

    private static void appendProjectorToTile(int projectorIndex, int tileIndex) {
        int count = cpuTileCounts[tileIndex];
        int[] list = cpuTileLists[tileIndex];

        if (list == null) {
            list = new int[INITIAL_TILE_CAPACITY];
            cpuTileLists[tileIndex] = list;
        } else if (count >= list.length) {
            int newCapacity = list.length <= Integer.MAX_VALUE / 2
                    ? list.length * 2
                    : Integer.MAX_VALUE;
            if (newCapacity <= count) {
                throw new IllegalStateException(
                        "[TheatricalExtraLights/Gobo] Tile projector list overflow"
                );
            }
            list = Arrays.copyOf(list, newCapacity);
            cpuTileLists[tileIndex] = list;
        }

        list[count] = projectorIndex;
        cpuTileCounts[tileIndex] = count + 1;
    }

    private static void uploadTextures(
            float[] projectorData,
            int[][] tileLists,
            int[] tileCounts,
            int tileCount,
            int maxPerTile
    ) {
        RenderSystem.assertOnRenderThread();

        int maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        if (tileCount <= 0 || tileCount > maxTextureSize || DATA_COLUMNS > maxTextureSize) {
            throw new IllegalStateException(
                    "[TheatricalExtraLights/Gobo] Invalid GPU texture dimensions: "
                            + DATA_COLUMNS + "x" + tileCount
            );
        }

        long requiredProjectorFloats = (long) preparedProjectorCount * DATA_FLOATS_PER_PROJECTOR;
        if (projectorData.length < requiredProjectorFloats) {
            throw new IllegalArgumentException(
                    "[TheatricalExtraLights/Gobo] Projector data buffer is too small"
            );
        }

        if (tileLists.length != tileCount || tileCounts.length != tileCount) {
            throw new IllegalArgumentException(
                    "[TheatricalExtraLights/Gobo] Invalid tile list dimensions"
            );
        }

        GoboTextureArray.finishUploads();

        int previousActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        int previousBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        try (GoboPixelUnpackState unpackState = new GoboPixelUnpackState()) {
            ensureDataTexture(preparedProjectorCount);
            boolean tileTextureChanged = ensureTileTexture(tileCount, maxPerTile);
            if (tileTextureChanged || uploadedTileCounts.length != tileCount) {
                uploadedTileCounts = new int[tileCount];
                Arrays.fill(uploadedTileCounts, -1);
                uploadedTileLists = new int[tileCount][];
            }

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

            uploadDirtyTileRows(tileLists, tileCounts, tileCount);
        } finally {
            bindTexture2D(previousBinding, "restoreTextureBinding");
            RenderSystem.activeTexture(previousActive);
        }
    }

    private static void uploadDirtyTileRows(
            int[][] tileLists,
            int[] tileCounts,
            int tileCount
    ) {
        int floatsPerTileRow = tileTextureWidth * 4;
        long requiredFloatsLong = (long) tileCount * floatsPerTileRow;
        if (requiredFloatsLong > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "[TheatricalExtraLights/Gobo] Tile upload buffer is too large: "
                            + requiredFloatsLong
            );
        }

        int requiredFloats = (int) requiredFloatsLong;
        if (tileUploadBuffer == null || tileUploadBuffer.capacity() < requiredFloats) {
            tileUploadBuffer = BufferUtils.createFloatBuffer(requiredFloats);
        }
        tileUploadBuffer.clear();

        if (dirtyTileRows.length != tileCount) {
            dirtyTileRows = new boolean[tileCount];
        }
        Arrays.fill(dirtyTileRows, false);

        for (int tile = 0; tile < tileCount; tile++) {
            int count = tileCounts[tile];
            int[] list = tileLists[tile];
            boolean changed = uploadedTileCounts[tile] != count;

            if (!changed && count > 0) {
                int[] uploaded = uploadedTileLists[tile];
                if (uploaded == null || uploaded.length != count) {
                    changed = true;
                } else {
                    for (int slot = 0; slot < count; slot++) {
                        if (uploaded[slot] != list[slot]) {
                            changed = true;
                            break;
                        }
                    }
                }
            }

            if (!changed) {
                continue;
            }

            int base = tile * floatsPerTileRow;
            for (int slot = 0; slot < floatsPerTileRow; slot++) {
                int projectorSlot = slot;
                float value = projectorSlot < count
                        ? (float) list[projectorSlot]
                        : -1.0f;
                tileUploadBuffer.put(base + slot, value);
            }
            dirtyTileRows[tile] = true;
        }

        bindTexture2D(tileTexture, "uploadTileIndices");

        int tile = 0;
        while (tile < tileCount) {
            while (tile < tileCount && !dirtyTileRows[tile]) {
                tile++;
            }
            if (tile >= tileCount) {
                break;
            }

            int start = tile;
            while (tile < tileCount && dirtyTileRows[tile]) {
                tile++;
            }
            int end = tile;

            tileUploadBuffer.clear();
            tileUploadBuffer.limit(end * floatsPerTileRow);
            tileUploadBuffer.position(start * floatsPerTileRow);

            GL11.glTexSubImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    0,
                    start,
                    tileTextureWidth,
                    end - start,
                    GL11.GL_RGBA,
                    GL11.GL_FLOAT,
                    tileUploadBuffer
            );
            checkUploadError("tile indices");
        }

        tileUploadBuffer.clear();

        for (int i = 0; i < tileCount; i++) {
            if (!dirtyTileRows[i]) {
                continue;
            }

            int count = tileCounts[i];
            uploadedTileCounts[i] = count;
            uploadedTileLists[i] = count > 0
                    ? Arrays.copyOf(tileLists[i], count)
                    : null;
        }
    }

    private static void ensureDataTexture(int requiredHeight) {
        int maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        int required = Math.max(1, requiredHeight);

        if (required > maxTextureSize) {
            throw new IllegalStateException(
                    "[TheatricalExtraLights/Gobo] " + required
                            + " projectors exceed GL_MAX_TEXTURE_SIZE=" + maxTextureSize
            );
        }

        if (dataTexture == 0) {
            dataTexture = GL11.glGenTextures();
            bindTexture2D(dataTexture, "createDataTexture");
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        } else {
            bindTexture2D(dataTexture, "ensureDataTexture");
        }

        if (dataTextureHeight < required) {
            int newHeight = Math.min(nextPowerOfTwo(required), maxTextureSize);
            if (newHeight < required) {
                newHeight = required;
            }

            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL30.GL_RGBA32F,
                    DATA_COLUMNS,
                    newHeight,
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_FLOAT,
                    (ByteBuffer) null
            );
            checkUploadError("resize projector data texture");
            dataTextureHeight = newHeight;
        }
    }

    private static boolean ensureTileTexture(int tileCount, int maxPerTile) {
        if (tileTexture == 0) {
            tileTexture = GL11.glGenTextures();
        }

        bindTexture2D(tileTexture, "ensureTileTexture");
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        int maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        int requiredWidth = Math.max(1, (maxPerTile + 3) / 4);
        int newWidth = Math.max(tileTextureWidth, requiredWidth);

        if (newWidth > maxTextureSize || tileCount > maxTextureSize) {
            throw new IllegalStateException(
                    "[TheatricalExtraLights/Gobo] Tile texture exceeds GPU limits: "
                            + newWidth + "x" + tileCount
                            + " (GPU limit: " + maxTextureSize + ")"
            );
        }

        boolean changed = tileTextureWidth != newWidth || tileTextureHeight != tileCount;
        if (changed) {
            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL30.GL_RGBA32F,
                    newWidth,
                    tileCount,
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_FLOAT,
                    (ByteBuffer) null
            );
            checkUploadError("resize RGBA tile index texture");
            tileTextureWidth = newWidth;
            tileTextureHeight = tileCount;
        }
        return changed;
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
                            + operation + ": 0x" + Integer.toHexString(error)
            );
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