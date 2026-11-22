package com.github.dumann089.theatricalextralights.client.gobo;

public class GoboWheelAnimator {
    private int targetPhysicalSlot = 0;
    private float currentVirtualSlot = 0f;
    private float targetVirtualSlot = 0f;
    private long startTime = 0;
    private long duration = 1;
    private boolean isAnimating = false;
    private long lastSnapshotFrameKey = Long.MIN_VALUE;
    private float cachedVirtualSlot = 0f;

    public void updateTarget(GoboLibrary library, int newTargetSlot) {
        updateTarget(library, newTargetSlot, 150L, true);
    }

    public void updateTarget(
            GoboLibrary library,
            int newTargetSlot,
            long durationPerSlotMs,
            boolean shortestPath
    ) {
        int slotCount = library.getSlotCount();
        if (slotCount <= 1) {
            targetPhysicalSlot = 0;
            currentVirtualSlot = 0.0F;
            targetVirtualSlot = 0.0F;
            isAnimating = false;
            return;
        }

        newTargetSlot = Math.max(0, Math.min(slotCount - 1, newTargetSlot));
        if (targetPhysicalSlot == newTargetSlot) {
            return;
        }

        currentVirtualSlot = isAnimating ? getCurrentVirtualSlot() : targetVirtualSlot;
        targetPhysicalSlot = newTargetSlot;

        float baseVirtual = (float) Math.floor(currentVirtualSlot);
        int basePhysical = shortestPath
                ? Math.floorMod((int) baseVirtual, slotCount)
                : Math.max(0, Math.min(slotCount - 1, (int) baseVirtual));

        if (shortestPath) {
            int forwardDistance = (newTargetSlot - basePhysical + slotCount) % slotCount;
            int backwardDistance = (basePhysical - newTargetSlot + slotCount) % slotCount;
            int steps = Math.min(forwardDistance, backwardDistance);
            float direction = forwardDistance <= backwardDistance ? 1.0F : -1.0F;
            targetVirtualSlot = baseVirtual + direction * steps;
        } else {
            targetVirtualSlot = newTargetSlot;
        }

        long safeDurationPerSlot = Math.max(1L, durationPerSlotMs);
        duration = Math.max(
                250L,
                Math.round(Math.abs(targetVirtualSlot - currentVirtualSlot) * safeDurationPerSlot)
        );
        startTime = System.currentTimeMillis();
        isAnimating = duration > 0L && Math.abs(targetVirtualSlot - currentVirtualSlot) > 0.0001F;
        if (!isAnimating) {
            currentVirtualSlot = targetVirtualSlot;
        }
        lastSnapshotFrameKey = Long.MIN_VALUE;
    }

    public float getCurrentVirtualSlot() {
        if (!isAnimating) {
            return targetVirtualSlot;
        }

        float progress = (float) (System.currentTimeMillis() - startTime) / (float) duration;
        if (progress >= 1.0F) {
            isAnimating = false;
            currentVirtualSlot = targetVirtualSlot;
            return targetVirtualSlot;
        }

        progress = Math.max(0.0F, progress);
        float ease = progress * progress * (3.0F - 2.0F * progress);
        return currentVirtualSlot + (targetVirtualSlot - currentVirtualSlot) * ease;
    }

    public float snapshotVirtualSlot() {
        cachedVirtualSlot = getCurrentVirtualSlot();
        return cachedVirtualSlot;
    }

    public int getOutgoingSlot(GoboLibrary library, float virtualSlot) {
        int slotCount = library.getSlotCount();
        if (slotCount <= 1) {
            return 0;
        }
        int slot = (int) Math.floor(virtualSlot);
        return Math.floorMod(slot, slotCount);
    }

    public int getIncomingSlot(GoboLibrary library, float virtualSlot) {
        int slotCount = library.getSlotCount();
        if (slotCount <= 1) {
            return 0;
        }
        return (getOutgoingSlot(library, virtualSlot) + 1) % slotCount;
    }

    public float getShaderProgress(GoboLibrary library, float virtualSlot) {
        if (library.getSlotCount() <= 1) {
            return 0.0F;
        }
        return virtualSlot - (float) Math.floor(virtualSlot);
    }

    public int getOutgoingSlot(GoboLibrary library) {
        return getOutgoingSlot(library, getCurrentVirtualSlot());
    }

    public int getIncomingSlot(GoboLibrary library) {
        return getIncomingSlot(library, getCurrentVirtualSlot());
    }

    public float getShaderProgress(GoboLibrary library) {
        return getShaderProgress(library, getCurrentVirtualSlot());
    }
}
