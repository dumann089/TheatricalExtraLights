package com.github.dumann089.theatricalextralights.client.gobo;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.slf4j.Logger;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class ShimmerBBSFSCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String LIGHT_MANAGER_CLASS =
            "com.lowdragmc.shimmer.client.light.LightManager";

    private static final String LIGHTMAP_ASSIGNMENT =
            "lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);";

    private static final String SHIMMER_POSITION_VARIABLE =
            "extralightsShimmerWorldPosition";

    private static final Map<ShaderInstance, Boolean> BOUND_SHADERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static volatile boolean initialized;
    private static volatile boolean available;
    private static volatile boolean warningReported;

    private static Object lightManager;
    private static Field lightUboField;
    private static Field envUboField;
    private static Method bindProgramMethod;

    private static String shimmerShaderSource;

    private ShimmerBBSFSCompat() {
    }

    private static synchronized void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;

        try {
            Class<?> managerClass = Class.forName(LIGHT_MANAGER_CLASS);

            Field instanceField = managerClass.getField("INSTANCE");
            lightManager = instanceField.get(null);

            lightUboField = managerClass.getDeclaredField("lightUBO");
            envUboField = managerClass.getDeclaredField("envUBO");

            lightUboField.setAccessible(true);
            envUboField.setAccessible(true);

            bindProgramMethod = managerClass.getMethod(
                    "bindRbProgram",
                    int.class
            );

            shimmerShaderSource = loadShimmerShader(managerClass);

            available = lightManager != null
                    && lightUboField != null
                    && envUboField != null
                    && bindProgramMethod != null
                    && shimmerShaderSource != null
                    && !shimmerShaderSource.isBlank();

            if (available) {
                LOGGER.info(
                        "[TheatricalExtraLights] Shimmer BBS FS compatibility initialized"
                );
            }
        } catch (Exception | LinkageError exception) {
            available = false;

            LOGGER.debug(
                    "[TheatricalExtraLights] Shimmer is not available for BBS FS",
                    exception
            );
        }
    }

    private static String loadShimmerShader(Class<?> managerClass) throws Exception {
        String resourcePath =
                "/assets/minecraft/shaders/include/shimmer.glsl";

        try (InputStream input = managerClass.getResourceAsStream(resourcePath)) {
            if (input == null) {
                LOGGER.warn(
                        "[TheatricalExtraLights] Shimmer GLSL resource was not found"
                );
                return null;
            }

            String source = new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            source = source.replace("#version 150", "");

            if (isShimmerAdditiveBlendEnabled()) {
                source = "#define ADDITIVE\n" + source;
            }

            return source;
        }
    }

    private static boolean isShimmerAdditiveBlendEnabled() {
        try {
            Class<?> servicesClass = Class.forName(
                    "com.lowdragmc.shimmer.platform.Services"
            );

            Field platformField = servicesClass.getField("PLATFORM");
            Object platform = platformField.get(null);

            Method method = platform.getClass().getMethod("isAdditiveBlend");

            return Boolean.TRUE.equals(method.invoke(platform));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return false;
        }
    }

    public static boolean isAvailable() {
        initialize();
        return available;
    }

    public static String patchVertexShader(String source) {
        if (source == null || !isAvailable()) {
            return source;
        }

        if (source.contains(SHIMMER_POSITION_VARIABLE)) {
            return source;
        }

        if (!source.contains("in vec3 Position;")
                || !source.contains("uniform mat4 ModelViewMat;")
                || !source.contains("uniform mat3 IViewRotMat;")
                || !source.contains("out vec4 lightMapColor;")
                || !source.contains(LIGHTMAP_ASSIGNMENT)) {
            return source;
        }

        if (source.contains("struct Light")
                || source.contains("vec4 color_light(")) {
            return source;
        }

        int mainIndex = source.indexOf("void main()");

        if (mainIndex < 0) {
            return source;
        }

        source = source.substring(0, mainIndex)
                + shimmerShaderSource
                + "\n"
                + source.substring(mainIndex);

        String replacement = LIGHTMAP_ASSIGNMENT + """


                    vec3 extralightsShimmerWorldPosition =
                            IViewRotMat * (ModelViewMat * vec4(Position, 1.0)).xyz;

                    lightMapColor = color_light(
                            extralightsShimmerWorldPosition,
                            lightMapColor
                    );
                """;

        return source.replace(
                LIGHTMAP_ASSIGNMENT,
                replacement
        );
    }

    public static boolean isBbsFsModelShader(ShaderInstance shader) {
        if (shader == null) {
            return false;
        }

        Uniform passMode = shader.getUniform("PassMode");
        Uniform normalMat = shader.getUniform("NormalMat");
        Uniform inverseViewRotation = shader.getUniform("IViewRotMat");

        return passMode != null
                && normalMat != null
                && inverseViewRotation != null;
    }

    public static void bind(ShaderInstance shader) {
        if (!isAvailable() || !isBbsFsModelShader(shader)) {
            return;
        }

        synchronized (BOUND_SHADERS) {
            if (BOUND_SHADERS.containsKey(shader)) {
                return;
            }
        }

        int programId = shader.getId();

        if (programId <= 0
                || GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) != programId) {
            return;
        }

        int lightsIndex = GL31.glGetUniformBlockIndex(programId, "Lights");
        int envIndex = GL31.glGetUniformBlockIndex(programId, "Env");

        if (lightsIndex < 0 || envIndex < 0) {
            return;
        }

        try {
            Object lightUbo = lightUboField.get(lightManager);
            Object envUbo = envUboField.get(lightManager);

            if (lightUbo == null || envUbo == null) {
                return;
            }

            bindProgramMethod.invoke(lightManager, programId);

            synchronized (BOUND_SHADERS) {
                BOUND_SHADERS.put(shader, Boolean.TRUE);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!warningReported) {
                warningReported = true;

                LOGGER.warn(
                        "[TheatricalExtraLights] Could not bind Shimmer UBOs to BBS FS model shader",
                        exception
                );
            }
        }
    }
}