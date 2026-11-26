package com.github.dumann089.theatricalextralights.client.gobo;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL31;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class ShimmerBBSCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String LIGHT_MANAGER_CLASS =
            "com.lowdragmc.shimmer.client.light.LightManager";

    private static final String SHIMMER_IMPORT =
            "#moj_import <shimmer.glsl>";

    private static final String LIGHTING_CALL =
            "lightMapColor = color_light(IViewRotMat * Position, lightMapColor);";

    private static final Map<ShaderInstance, Boolean> BOUND_SHADERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static volatile boolean initialized;
    private static volatile boolean available;
    private static volatile boolean bindingWarningReported;

    private static Object lightManager;
    private static Method bindProgramMethod;
    private static Field lightUboField;
    private static Field envUboField;

    private ShimmerBBSCompat() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;

        try {
            Class<?> managerClass = Class.forName(LIGHT_MANAGER_CLASS);

            Field instanceField = managerClass.getField("INSTANCE");
            lightManager = instanceField.get(null);

            bindProgramMethod = managerClass.getMethod(
                    "bindRbProgram",
                    int.class
            );

            lightUboField = managerClass.getDeclaredField("lightUBO");
            envUboField = managerClass.getDeclaredField("envUBO");

            lightUboField.setAccessible(true);
            envUboField.setAccessible(true);

            available = lightManager != null
                    && bindProgramMethod != null
                    && lightUboField != null
                    && envUboField != null;

            if (available) {
                LOGGER.info(
                        "[TheatricalExtraLights] Shimmer BBS compatibility initialized"
                );
            }
        } catch (ClassNotFoundException exception) {
            available = false;
        } catch (ReflectiveOperationException | LinkageError exception) {
            available = false;

            LOGGER.warn(
                    "[TheatricalExtraLights] Could not initialize Shimmer BBS compatibility",
                    exception
            );
        }
    }

    public static boolean isAvailable() {
        initialize();
        return available;
    }

    public static String patchVertex(String source) {
        if (source == null || !isAvailable()) {
            return source;
        }

        if (!source.contains("in vec3 Position;")
                || !source.contains("uniform mat3 IViewRotMat;")
                || !source.contains("uniform mat3 NormalMat;")
                || !source.contains("out vec4 lightMapColor;")
                || !source.contains("lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);")
                || !source.contains("void main()")) {
            return source;
        }

        if (source.contains(LIGHTING_CALL)) {
            return source;
        }

        if (!source.contains(SHIMMER_IMPORT)) {
            source = insertImport(source, SHIMMER_IMPORT);
        }

        int mainIndex = source.indexOf("void main()");
        int mainOpen = source.indexOf('{', mainIndex);
        int mainClose = source.lastIndexOf('}');

        if (mainOpen < 0 || mainClose <= mainOpen) {
            LOGGER.warn(
                    "[TheatricalExtraLights] Could not locate BBS vertex main()"
            );
            return source;
        }

        return source.substring(0, mainClose)
                + "\n    "
                + LIGHTING_CALL
                + "\n"
                + source.substring(mainClose);
    }

    private static String insertImport(String source, String importLine) {
        int versionIndex = source.indexOf("#version");

        if (versionIndex < 0) {
            return importLine + "\n" + source;
        }

        int lineEnd = source.indexOf('\n', versionIndex);

        if (lineEnd < 0) {
            return source + "\n" + importLine + "\n";
        }

        return source.substring(0, lineEnd + 1)
                + importLine
                + "\n"
                + source.substring(lineEnd + 1);
    }

    public static boolean isBbsModelShader(ShaderInstance shader) {
        if (shader == null) {
            return false;
        }

        return shader.getUniform("GoboTileSize") != null
                && shader.getUniform("NormalMat") != null
                && shader.getUniform("IViewRotMat") != null;
    }

    public static void bind(ShaderInstance shader) {
        if (!isAvailable() || !isBbsModelShader(shader)) {
            return;
        }

        synchronized (BOUND_SHADERS) {
            if (BOUND_SHADERS.containsKey(shader)) {
                return;
            }
        }

        int programId = shader.getId();

        if (programId <= 0) {
            return;
        }

        int lightsIndex = GL31.glGetUniformBlockIndex(programId, "Lights");
        int envIndex = GL31.glGetUniformBlockIndex(programId, "Env");

        if (lightsIndex == -1 || envIndex == -1) {
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
            if (!bindingWarningReported) {
                bindingWarningReported = true;

                LOGGER.warn(
                        "[TheatricalExtraLights] Could not bind Shimmer buffers to BBS shader",
                        exception
                );
            }
        }
    }
}