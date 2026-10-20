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

/**
 * Coupure de vanne : le panache se detache de la buse. Un front de coupure remonte le jet a la
 * vitesse du gaz ; derriere lui la fumee s'eteint, devant lui elle continue de monter et se dilue
 * avec sa propre duree de vie.
 */
@Environment(EnvType.CLIENT)
public final class Flow2JetDissipation {
    /** Vitesse du front de coupure, blocs/tick. */
    private static final float FRONT_SPEED = 0.55f;
    private static final float FRONT_SOFTNESS = 1.2f;
    private static final float PLUME_RADIUS = 2.2f;
    private static final float MAX_PLUME_ALONG = 12f;
    private static final float LINGER_TICKS = 12f;

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

    /** 1 = intacte, 0 = derriere le front de coupure. */
    public static float alphaMultiplier(double x, double y, double z, long gameTime) {
        float multiplier = 1f;
        Iterator<Map.Entry<BlockPos, JetPlume>> iterator = STOPPED.entrySet().iterator();
        while (iterator.hasNext()) {
            JetPlume plume = iterator.next().getValue();
            if (gameTime - plume.stopGameTime > MAX_PLUME_ALONG / FRONT_SPEED + LINGER_TICKS) {
                iterator.remove();
                continue;
            }
            multiplier = Math.min(multiplier, plume.fadeFor(x, y, z, gameTime));
        }
        return multiplier;
    }

    /** Derriere le front, le gaz n'est plus pousse : il ralentit. */
    public static float motionDamping(float alphaMultiplier) {
        if (alphaMultiplier >= 0.98f) {
            return 1f;
        }
        return 0.6f + 0.4f * alphaMultiplier;
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
            float elapsed = gameTime - stopGameTime;
            if (elapsed <= 0f) {
                return 1f;
            }
            double relX = x - nozzle.x;
            double relY = y - nozzle.y;
            double relZ = z - nozzle.z;
            float along = (float) (relX * axis.x() + relY * axis.y() + relZ * axis.z());
            if (along < -0.6f || along > MAX_PLUME_ALONG) {
                return 1f;
            }
            double perpX = relX - axis.x() * along;
            double perpY = relY - axis.y() * along;
            double perpZ = relZ - axis.z() * along;
            if (perpX * perpX + perpY * perpY + perpZ * perpZ > PLUME_RADIUS * PLUME_RADIUS) {
                return 1f;
            }
            float front = elapsed * FRONT_SPEED;
            float behind = Mth.clamp((front - along) / FRONT_SOFTNESS + 0.5f, 0f, 1f);
            return 1f - behind;
        }
    }
}
