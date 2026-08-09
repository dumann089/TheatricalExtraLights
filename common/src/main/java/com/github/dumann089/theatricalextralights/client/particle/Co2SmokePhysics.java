package com.github.dumann089.theatricalextralights.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Utilitaires de trajectoire pour le jet CO₂ (cône, disque perpendiculaire). */
@Environment(EnvType.CLIENT)
public final class Co2SmokePhysics {
    private Co2SmokePhysics() {
    }

    public static Vec3 randomUnitCone(Vector3f axis, float halfAngleDegrees, RandomSource random) {
        Vec3 normal = normalize(new Vec3(axis.x(), axis.y(), axis.z()));
        Vec3 helper = Math.abs(normal.y) < 0.92 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 tangent = normalize(normal.cross(helper));
        Vec3 bitangent = normalize(normal.cross(tangent));

        double halfAngle = Math.toRadians(halfAngleDegrees);
        double cosMax = Math.cos(halfAngle);
        double cosTheta = cosMax + random.nextDouble() * (1.0 - cosMax);
        double sinTheta = Math.sqrt(Math.max(0.0, 1.0 - cosTheta * cosTheta));
        double phi = random.nextDouble() * Mth.TWO_PI;

        return tangent.scale(Math.cos(phi) * sinTheta)
                .add(bitangent.scale(Math.sin(phi) * sinTheta))
                .add(normal.scale(cosTheta))
                .normalize();
    }

    public static Vec3 randomDisk(Vector3f axis, RandomSource random, float radius) {
        if (radius <= 0f) {
            return Vec3.ZERO;
        }
        Vec3 normal = normalize(new Vec3(axis.x(), axis.y(), axis.z()));
        Vec3 helper = Math.abs(normal.y) < 0.92 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 tangent = normalize(normal.cross(helper));
        Vec3 bitangent = normalize(normal.cross(tangent));
        double angle = random.nextDouble() * Mth.TWO_PI;
        double dist = Math.sqrt(random.nextDouble()) * radius;
        return tangent.scale(Math.cos(angle) * dist).add(bitangent.scale(Math.sin(angle) * dist));
    }

    private static Vec3 normalize(Vec3 vector) {
        if (vector.lengthSqr() < 1.0e-8) {
            return new Vec3(0.0, 1.0, 0.0);
        }
        return vector.normalize();
    }
}
