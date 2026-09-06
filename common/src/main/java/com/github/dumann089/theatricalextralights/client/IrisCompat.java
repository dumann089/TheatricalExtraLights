package com.github.dumann089.theatricalextralights.client;

/**
 * Iris / Oculus detection. Prefer IrisApi — Oculus 1.8 does not expose
 * IrisRenderingPipeline.isShadersEnabled() as a static method.
 */
public final class IrisCompat {

    private IrisCompat() {}

    private static final boolean IRIS_PRESENT;

    static {
        boolean found = false;
        String[] classesToProbe = {
                "net.irisshaders.iris.api.v0.IrisApi",
                "net.coderbot.iris.api.v0.IrisApi",
                "net.irisshaders.iris.Iris",
                "net.coderbot.iris.Iris"
        };
        for (String cls : classesToProbe) {
            try {
                Class.forName(cls);
                found = true;
                break;
            } catch (ClassNotFoundException ignored) {
            }
        }
        IRIS_PRESENT = found;
    }

    public static boolean isIrisPresent() {
        return IRIS_PRESENT;
    }

    public static boolean isShadersActive() {
        if (!IRIS_PRESENT) {
            return false;
        }
        if (probeIrisApi("net.irisshaders.iris.api.v0.IrisApi")) {
            return true;
        }
        return probeIrisApi("net.coderbot.iris.api.v0.IrisApi");
    }

    private static boolean probeIrisApi(String className) {
        try {
            Class<?> irisApi = Class.forName(className);
            Object instance = irisApi.getMethod("getInstance").invoke(null);
            return (boolean) irisApi.getMethod("isShaderPackInUse").invoke(instance);
        } catch (Exception ignored) {
            return false;
        }
    }
}
