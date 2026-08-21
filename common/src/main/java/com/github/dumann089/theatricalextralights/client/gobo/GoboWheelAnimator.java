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
        int N = library.getSlotCount();
        if (N <= 1) {
            this.targetPhysicalSlot = newTargetSlot;
            this.currentVirtualSlot = newTargetSlot;
            this.targetVirtualSlot = newTargetSlot;
            this.isAnimating = false;
            return;
        }

        if (this.targetPhysicalSlot == newTargetSlot) return;

        if (!isAnimating) {
            this.currentVirtualSlot = this.targetPhysicalSlot;
        } else {
            this.currentVirtualSlot = getCurrentVirtualSlot();
        }

        this.targetPhysicalSlot = newTargetSlot;

        float baseVirtual = (float) Math.floor(this.currentVirtualSlot);
        int basePhysical = ((int) baseVirtual % N + N) % N;

        int forwardDistance = (newTargetSlot - basePhysical + N) % N;
        int backwardDistance = (basePhysical - newTargetSlot + N) % N;

        int steps;
        float direction;
        if (forwardDistance <= backwardDistance) {
            steps = forwardDistance;
            direction = 1f;
        } else {
            steps = backwardDistance;
            direction = -1f;
        }

        this.targetVirtualSlot = baseVirtual + (direction * steps);

        this.duration = Math.max(250, (long)(Math.abs(this.targetVirtualSlot - this.currentVirtualSlot) * 150L));
        this.startTime = System.currentTimeMillis();
        this.isAnimating = true;
        this.lastSnapshotFrameKey = Long.MIN_VALUE;
    }
    public float getCurrentVirtualSlot() {
        if (!isAnimating) return targetVirtualSlot;

        long now = System.currentTimeMillis();
        float progress = (float) (now - startTime) / duration;

        if (progress >= 1.0f) {
            isAnimating = false;
            targetVirtualSlot = targetPhysicalSlot;
            return targetVirtualSlot;
        }

        float ease = progress * progress * (3.0f - 2.0f * progress);
        return currentVirtualSlot + (targetVirtualSlot - currentVirtualSlot) * ease;
    }
    public float snapshotVirtualSlot() {
        cachedVirtualSlot = getCurrentVirtualSlot();
        return cachedVirtualSlot;
    }

    public int getOutgoingSlot(GoboLibrary library, float virtualSlot) {
        int N = library.getSlotCount();
        if (N <= 1) return targetPhysicalSlot;

        int slot = (int) Math.floor(virtualSlot);
        return (slot % N + N) % N;
    }

    public int getIncomingSlot(GoboLibrary library, float virtualSlot) {
        int N = library.getSlotCount();
        if (N <= 1) return targetPhysicalSlot;

        return (getOutgoingSlot(library, virtualSlot) + 1) % N;
    }

    public float getShaderProgress(GoboLibrary library, float virtualSlot) {
        int N = library.getSlotCount();
        if (N <= 1) return 0.0f;

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