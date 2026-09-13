package com.github.dumann089.theatricalextralights.client;

import java.lang.reflect.Method;

/**
 * Iris / Oculus detection. Prefer IrisApi — Oculus 1.8 does not expose
 * IrisRenderingPipeline.isShadersEnabled() as a static method.
 *
 * <p>Everything reflective is resolved exactly once in the static initialiser. The
 * "shader pack in use" answer is polled through the cached {@link Method}s at most every
 * {@link #POLL_INTERVAL_NANOS}: it can change at runtime (the user toggles a pack), but
 * asking Iris once per fixture per frame was measurable on large stages.
 */
public final class IrisCompat {

    private IrisCompat() {}

    private static final long POLL_INTERVAL_NANOS = 250_000_000L;

    private static final boolean IRIS_PRESENT;
    private static final Object IRIS_API_INSTANCE;
    private static final Method IS_SHADER_PACK_IN_USE;

    private static volatile boolean cachedShadersActive;
    private static volatile long lastPollNanos = Long.MIN_VALUE;

    static {
        Object instance = null;
        Method inUse = null;
        String[] apiClasses = {
                "net.irisshaders.iris.api.v0.IrisApi",
                "net.coderbot.iris.api.v0.IrisApi"
        };
        for (String cls : apiClasses) {
            try {
                Class<?> irisApi = Class.forName(cls);
                instance = irisApi.getMethod("getInstance").invoke(null);
                inUse = irisApi.getMethod("isShaderPackInUse");
                break;
            } catch (Throwable ignored) {
                // API absent or incompatible: try the next name.
            }
        }

        boolean present = instance != null && inUse != null;
        if (!present) {
            // Iris without a usable API (very old builds): remember it is there, but we
            // cannot ask about the pack, so treat shaders as inactive.
            String[] coreClasses = {"net.irisshaders.iris.Iris", "net.coderbot.iris.Iris"};
            for (String cls : coreClasses) {
                try {
                    Class.forName(cls);
                    present = true;
                    break;
                } catch (ClassNotFoundException ignored) {
                }
            }
        }

        IRIS_PRESENT = present;
        IRIS_API_INSTANCE = instance;
        IS_SHADER_PACK_IN_USE = inUse;
    }

    public static boolean isIrisPresent() {
        return IRIS_PRESENT;
    }

    /** True when Iris / Oculus is loaded and a shader pack is currently in use. */
    public static boolean isShadersActive() {
        if (IS_SHADER_PACK_IN_USE == null) {
            return false;
        }
        long now = System.nanoTime();
        if (now - lastPollNanos >= POLL_INTERVAL_NANOS) {
            cachedShadersActive = pollShaderPackInUse();
            lastPollNanos = now;
        }
        return cachedShadersActive;
    }

    /** Forces the next {@link #isShadersActive()} call to ask Iris again. */
    public static void invalidate() {
        lastPollNanos = Long.MIN_VALUE;
    }

    private static boolean pollShaderPackInUse() {
        try {
            return (boolean) IS_SHADER_PACK_IN_USE.invoke(IRIS_API_INSTANCE);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
