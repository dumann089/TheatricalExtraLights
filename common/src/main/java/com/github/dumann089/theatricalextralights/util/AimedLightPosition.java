package com.github.dumann089.theatricalextralights.util;

import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Where the dynamic light of an aimed flood fixture (blinders) should sit.
 *
 * <p>Theatrical already traces the beam along the fixture orientation including pan, tilt and
 * hanging direction. Putting the light at that impact point makes the pan/tilt faders paint the
 * floor where the fixture points instead of leaving a pool pinned to the block.
 */
public final class AimedLightPosition {

    /** Long enough to reach the stage from a flown truss. */
    public static final float TRACE_DISTANCE = 64.0f;

    /** When nothing is hit, keep the light near the fixture along the same direction. */
    public static final float FALLBACK_DISTANCE = 8.0f;

    private AimedLightPosition() {
    }

    public static Vector3f forFixture(BaseLightBlockEntity blockEntity) {
        if (blockEntity.getDistance() < TRACE_DISTANCE - 0.05) {
            BlockPos emission = blockEntity.getEmissionBlock();
            if (emission != null) {
                return Vec3.atCenterOf(emission).toVector3f();
            }
        }
        Vec3 start = Vec3.atCenterOf(blockEntity.getBlockPos());
        Vec3 direction = BaseLightBlockEntity.rayTraceDir(blockEntity);
        return start.add(direction.scale(FALLBACK_DISTANCE)).toVector3f();
    }
}
