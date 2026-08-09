package com.github.dumann089.theatricalextralights.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Fade des particules CO₂ sur place dès que le trigger s'arrête. */
@Environment(EnvType.CLIENT)
public final class Flow2JetDissipation {
    private static final float FADE_DURATION_TICKS = 7f;
    private static final float COLUMN_RADIUS = 1.35f;
    private static final float MAX_COLUMN_ALONG = 3.2f;

    private static final Map<BlockPos, JetPlume> STOPPED = new ConcurrentHashMap<>();

    private Flow2JetDissipation() {
    }

    public static void markRunning(BlockPos blockPos) {
        STOPPED.remove(blockPos);
    }

    public static boolean isDissipating(BlockPos blockPos) {
        return STOPPED.containsKey(blockPos);
    }

    public static void beginStop(BlockPos blockPos, Vec3 nozzle, Vector3f jetDirection, long gameTime) {
        Vector3f axis = new Vector3f(jetDirection);
        if (axis.lengthSquared() < 1.0e-8f) {
            axis.set(0f, 1f, 0f);
        } else {
            axis.normalize();
        }
        STOPPED.put(blockPos, new JetPlume(nozzle, axis, gameTime));
    }

    public static void clear(BlockPos blockPos) {
        STOPPED.remove(blockPos);
    }

    public static float alphaMultiplier(double x, double y, double z, long gameTime) {
        float multiplier = 1f;
        Iterator<Map.Entry<BlockPos, JetPlume>> iterator = STOPPED.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, JetPlume> entry = iterator.next();
            JetPlume plume = entry.getValue();
            float fade = plume.fadeFor(x, y, z, gameTime);
            if (fade < 0.001f && gameTime - plume.stopGameTime > FADE_DURATION_TICKS + 4) {
                iterator.remove();
                continue;
            }
            if (fade < multiplier) {
                multiplier = fade;
            }
        }
        return multiplier;
    }

    public static float motionDamping(double x, double y, double z, long gameTime) {
        float alpha = alphaMultiplier(x, y, z, gameTime);
        if (alpha >= 0.98f) {
            return 1f;
        }
        return 0.04f + alpha * alpha * 0.2f;
    }

    private static final class JetPlume {
        private final Vec3 nozzle;
        private final Vector3f axis;
        private final long stopGameTime;

        private JetPlume(Vec3 nozzle, Vector3f axis, long stopGameTime) {
            this.nozzle = nozzle;
            this.axis = axis;
            this.stopGameTime = stopGameTime;
        }

        private float fadeFor(double x, double y, double z, long gameTime) {
            if (!isInsidePlume(x, y, z)) {
                return 1f;
            }

            float elapsed = gameTime - stopGameTime;
            if (elapsed <= 0f) {
                return 1f;
            }

            float t = Mth.clamp(elapsed / FADE_DURATION_TICKS, 0f, 1f);
            return 1f - t * t * t;
        }

        private boolean isInsidePlume(double x, double y, double z) {
            double relX = x - nozzle.x;
            double relY = y - nozzle.y;
            double relZ = z - nozzle.z;
            float along = (float) (relX * axis.x() + relY * axis.y() + relZ * axis.z());
            if (along < -0.2f || along > MAX_COLUMN_ALONG) {
                return false;
            }

            double axisX = axis.x();
            double axisY = axis.y();
            double axisZ = axis.z();
            double perpX = relX - axisX * along;
            double perpY = relY - axisY * along;
            double perpZ = relZ - axisZ * along;
            return perpX * perpX + perpY * perpY + perpZ * perpZ <= COLUMN_RADIUS * COLUMN_RADIUS;
        }
    }
}
