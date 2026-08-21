package com.github.dumann089.theatricalextralights.client.gobo;

import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.Map;
import java.util.WeakHashMap;

public class GoboWheelManager {
    private static final Map<BlockEntity, GoboWheelAnimator> ANIMATORS = new WeakHashMap<>();

    public static GoboWheelAnimator getAnimator(BlockEntity be) {
        return ANIMATORS.computeIfAbsent(be, k -> new GoboWheelAnimator());
    }
}