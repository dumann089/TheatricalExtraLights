package com.github.dumann089.theatricalextralights.client.render;

import com.github.dumann089.theatricalextralights.blockentities.FollowspotBlockEntity;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import dev.imabad.theatrical.client.LazyRenderers;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.WeakHashMap;

public final class FixtureRenderAngleContext {

    private static final float SPRING_OMEGA = 1.75F;
    private static final float QUATERNION_MATCH = 0.99998F;
    private static final float PIVOT_EPSILON = 0.0001F;

    private static final Map<BaseLightBlockEntity, AngleState> STATES =
            new WeakHashMap<>();

    private static final Map<LazyRenderers.LazyRenderer, BaseLightBlockEntity> LAZY_FIXTURES =
            new WeakHashMap<>();

    private static final ThreadLocal<Deque<RenderContext>> ACTIVE =
            ThreadLocal.withInitial(ArrayDeque::new);

    private FixtureRenderAngleContext() {
    }

    public static void push(BaseLightBlockEntity blockEntity, float partialTicks) {
        float partial = Float.isFinite(partialTicks)
                ? Mth.clamp(partialTicks, 0.0F, 1.0F)
                : 0.0F;

        ACTIVE.get().push(new RenderContext(blockEntity, partial));
    }

    public static void pop() {
        Deque<RenderContext> stack = ACTIVE.get();

        if (stack.isEmpty()) {
            return;
        }

        stack.pop();

        if (stack.isEmpty()) {
            ACTIVE.remove();
        }
    }

    public static void captureLazyRenderer(LazyRenderers.LazyRenderer renderer) {
        if (renderer == null) {
            return;
        }

        RenderContext context = current();

        if (context == null || !shouldInterpolate(context.blockEntity)) {
            return;
        }

        LAZY_FIXTURES.put(renderer, context.blockEntity);
    }

    public static boolean pushLazyRenderer(
            LazyRenderers.LazyRenderer renderer,
            float partialTicks
    ) {
        BaseLightBlockEntity blockEntity = LAZY_FIXTURES.remove(renderer);

        if (blockEntity == null) {
            return false;
        }

        push(blockEntity, partialTicks);
        return true;
    }

    private static RenderContext current() {
        Deque<RenderContext> stack = ACTIVE.get();
        return stack.isEmpty() ? null : stack.peek();
    }

    private static boolean shouldInterpolate(BaseLightBlockEntity blockEntity) {
        return blockEntity != null
                && blockEntity.getLevel() != null
                && !(blockEntity instanceof FollowspotBlockEntity);
    }

    /**
     * Detecta cuando el renderer acaba de trasladarse al pivot de Pan o Tilt.
     */
    public static void onPoseTranslate(double x, double y, double z) {
        RenderContext context = current();

        if (context == null || !shouldInterpolate(context.blockEntity)) {
            return;
        }

        if (Math.abs(x) + Math.abs(y) + Math.abs(z) < 0.000001D) {
            return;
        }

        Fixture fixture;

        try {
            fixture = context.blockEntity.getFixture();
        } catch (RuntimeException exception) {
            return;
        }

        if (fixture == null) {
            return;
        }

        if (matchesPivot(
                fixture.getPanRotationPosition(),
                x,
                y,
                z
        )) {
            context.panPivotPending = true;
        }

        if (matchesPivot(
                fixture.getTiltRotationPosition(),
                x,
                y,
                z
        )) {
            context.tiltPivotPending = true;
        }
    }

    private static boolean matchesPivot(
            float[] pivot,
            double x,
            double y,
            double z
    ) {
        if (pivot == null || pivot.length < 3) {
            return false;
        }

        return Math.abs(pivot[0] - x) < PIVOT_EPSILON
                && Math.abs(pivot[1] - y) < PIVOT_EPSILON
                && Math.abs(pivot[2] - z) < PIVOT_EPSILON;
    }

    /**
     * Sustituye exclusivamente las rotaciones de Pan y Tilt que coinciden
     * con los valores interpolados originalmente por cada renderer.
     */
    public static Quaternionf adjustQuaternion(Quaternionf original) {
        if (original == null) {
            return null;
        }

        RenderContext context = current();

        if (context == null || !shouldInterpolate(context.blockEntity)) {
            return original;
        }

        BaseLightBlockEntity blockEntity = context.blockEntity;
        Angles smooth = sample(context);

        if (context.panPivotPending) {
            float vanillaPan = Mth.lerp(
                    context.partialTicks,
                    blockEntity.getPrevPan(),
                    blockEntity.getPan()
            );

            Quaternionf replacement = replaceRotation(
                    original,
                    vanillaPan,
                    smooth.pan,
                    true
            );

            if (replacement != null) {
                context.panPivotPending = false;
                return replacement;
            }
        }

        if (context.tiltPivotPending) {
            float vanillaTilt = Mth.lerp(
                    context.partialTicks,
                    blockEntity.getPrevTilt(),
                    blockEntity.getTilt()
            );

            Quaternionf replacement = replaceRotation(
                    original,
                    vanillaTilt,
                    smooth.tilt,
                    false
            );

            if (replacement != null) {
                context.tiltPivotPending = false;
                return replacement;
            }
        }

        return original;
    }

    private static Quaternionf replaceRotation(
            Quaternionf original,
            float originalDegrees,
            float smoothDegrees,
            boolean pan
    ) {
        /*
         * Pan utiliza el eje Y y Tilt utiliza el eje X en los modelos
         * normales de Theatrical.
         *
         * Se comprueban ambos sentidos para soportar Axis.YN / Axis.XN.
         */
        float axisX = pan ? 0.0F : 1.0F;
        float axisY = pan ? 1.0F : 0.0F;

        for (int sign = 1; sign >= -1; sign -= 2) {
            Quaternionf expected = new Quaternionf().rotationAxis(
                    originalDegrees * sign * Mth.DEG_TO_RAD,
                    axisX,
                    axisY,
                    0.0F
            );

            float similarity = Math.abs(original.dot(expected));

            if (similarity >= QUATERNION_MATCH) {
                return new Quaternionf().rotationAxis(
                        smoothDegrees * sign * Mth.DEG_TO_RAD,
                        axisX,
                        axisY,
                        0.0F
                );
            }
        }

        return null;
    }

    private static Angles sample(RenderContext context) {
        BaseLightBlockEntity blockEntity = context.blockEntity;
        Level level = blockEntity.getLevel();

        float targetPan = blockEntity.getPan();
        float targetTilt = blockEntity.getTilt();

        double frameTime = level.getGameTime() + context.partialTicks;

        AngleState state = STATES.computeIfAbsent(
                blockEntity,
                key -> new AngleState()
        );

        if (!state.initialized || state.level != level) {
            state.pan = Mth.lerp(
                    context.partialTicks,
                    blockEntity.getPrevPan(),
                    targetPan
            );

            state.tilt = Mth.lerp(
                    context.partialTicks,
                    blockEntity.getPrevTilt(),
                    targetTilt
            );

            state.velocityPan = 0.0F;
            state.velocityTilt = 0.0F;

            state.lastFrameTime = frameTime;
            state.level = level;
            state.initialized = true;

            return new Angles(state.pan, state.tilt);
        }

        /*
         * Actualizar los objetivos no reinicia la posición visual ni
         * la velocidad. Esto evita los saltos cuando llegan paquetes
         * de Pan/Tilt durante el movimiento.
         */
        state.targetPan = targetPan;
        state.targetTilt = targetTilt;

        double delta = frameTime - state.lastFrameTime;

        if (delta < 0.0D) {
            state.lastFrameTime = frameTime;
            delta = 0.0D;
        }

        if (delta > 1.0D) {
            delta = 1.0D;
        }

        if (delta > 0.0D) {
            state.advance(delta);
            state.lastFrameTime = frameTime;
        }

        return new Angles(state.pan, state.tilt);
    }

    private record Angles(float pan, float tilt) {
    }

    private static final class RenderContext {
        private final BaseLightBlockEntity blockEntity;
        private final float partialTicks;

        private boolean panPivotPending;
        private boolean tiltPivotPending;

        private RenderContext(
                BaseLightBlockEntity blockEntity,
                float partialTicks
        ) {
            this.blockEntity = blockEntity;
            this.partialTicks = partialTicks;
        }
    }

    private static final class AngleState {
        private Level level;

        private boolean initialized;

        private float pan;
        private float tilt;

        private float targetPan;
        private float targetTilt;

        private float velocityPan;
        private float velocityTilt;

        private double lastFrameTime;

        private void advance(double delta) {
            double omega = SPRING_OMEGA;
            double decay = Math.exp(-omega * delta);

            double panOffset = pan - targetPan;
            double panImpulse = velocityPan + omega * panOffset;

            pan = (float) (
                    targetPan
                            + (panOffset + panImpulse * delta) * decay
            );

            velocityPan = (float) (
                    (velocityPan - omega * panImpulse * delta) * decay
            );

            double tiltOffset = tilt - targetTilt;
            double tiltImpulse = velocityTilt + omega * tiltOffset;

            tilt = (float) (
                    targetTilt
                            + (tiltOffset + tiltImpulse * delta) * decay
            );

            velocityTilt = (float) (
                    (velocityTilt - omega * tiltImpulse * delta) * decay
            );

            if (!Float.isFinite(pan)) {
                pan = targetPan;
                velocityPan = 0.0F;
            }

            if (!Float.isFinite(tilt)) {
                tilt = targetTilt;
                velocityTilt = 0.0F;
            }

            if (Math.abs(targetPan - pan) < 0.001F) {
                pan = targetPan;
                velocityPan = 0.0F;
            }

            if (Math.abs(targetTilt - tilt) < 0.001F) {
                tilt = targetTilt;
                velocityTilt = 0.0F;
            }
        }
    }
}