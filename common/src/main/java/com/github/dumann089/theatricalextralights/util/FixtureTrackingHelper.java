package com.github.dumann089.theatricalextralights.util;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;

public final class FixtureTrackingHelper {

    private static final int ITERATIONS = 5;

    private static final float SAMPLE_DEG = 0.5F;
    private static final float MAX_NEWTON_STEP_DEG = 18.0F;
    
    private static final float TRACKING_RESPONSE = 0.28F;

    public static final float MAX_DEG_PER_TICK = 15.0F;

    private static final double MIN_ERROR = 0.00001;
    private static final double MIN_DERIVATIVE = 1.0E-9;
    private static final double MIN_DETERMINANT = 1.0E-12;
    private static final double AIM_HEIGHT = 0.6;

    private FixtureTrackingHelper() {
    }

    public static void tick(
            ExtraLightsLightBlockEntity fixture,
            ServerLevel level,
            UUID targetId
    ) {
        Entity target = level.getEntity(targetId);

        if (target == null || target.isRemoved() || !target.isAlive()) {
            return;
        }

        if (!fixture.supportsTracking()) {
            fixture.setTrackedEntity(null);
            return;
        }

        float startPan = fixture.hasTrackAngles()
                ? fixture.getTrackPan()
                : fixture.getPan();

        float startTilt = fixture.hasTrackAngles()
                ? fixture.getTrackTilt()
                : fixture.getTilt();

        Vec3 aim = new Vec3(
                target.getX(),
                target.getY(AIM_HEIGHT),
                target.getZ()
        );

        Vec3 desired = aim.subtract(
                beamOrigin(fixture, startPan, startTilt)
        );

        if (desired.lengthSqr() < MIN_ERROR) {
            return;
        }

        desired = desired.normalize();

        float solvedPan = startPan;
        float solvedTilt = startTilt;

        for (int i = 0; i < ITERATIONS; i++) {
            Vec3 forward = beamDirection(
                    fixture,
                    solvedPan,
                    solvedTilt
            ).normalize();

            double forwardDot = Mth.clamp(
                    desired.dot(forward),
                    -1.0,
                    1.0
            );

            Vec3 error = desired.subtract(
                    forward.scale(forwardDot)
            );

            double errorLength = error.length();

            if (errorLength < MIN_ERROR && forwardDot > 0.0) {
                break;
            }

            /*
             * Convierte el error en un vector tangente cuya longitud
             * representa el error angular en radianes.
             */
            if (errorLength > MIN_ERROR) {
                double errorAngle = Math.atan2(
                        errorLength,
                        forwardDot
                );

                error = error.scale(errorAngle / errorLength);
            } else {
                /*
                 * Caso excepcional: objetivo prácticamente detrás
                 * del fixture. Utilizamos la dirección de pan como
                 * referencia para evitar una división inestable.
                 */
                Vec3 fallback = getPanDerivative(
                        fixture,
                        solvedPan,
                        solvedTilt
                );

                if (fallback.lengthSqr() < MIN_DERIVATIVE) {
                    break;
                }

                error = fallback.normalize().scale(Math.PI);
            }

            Vec3 dPan = getPanDerivative(
                    fixture,
                    solvedPan,
                    solvedTilt
            );

            Vec3 dTilt = getTiltDerivative(
                    fixture,
                    solvedPan,
                    solvedTilt
            );

            /*
             * Proyectamos las derivadas sobre el plano tangente
             * perpendicular a la dirección actual del haz.
             */
            dPan = dPan.subtract(
                    forward.scale(dPan.dot(forward))
            );

            dTilt = dTilt.subtract(
                    forward.scale(dTilt.dot(forward))
            );

            double a = dPan.dot(dPan);
            double b = dPan.dot(dTilt);
            double c = dTilt.dot(dTilt);

            double panError = dPan.dot(error);
            double tiltError = dTilt.dot(error);

            if (a < MIN_DERIVATIVE && c < MIN_DERIVATIVE) {
                break;
            }

            double determinant = a * c - b * b;

            float deltaPan;
            float deltaTilt;

            if (determinant > MIN_DETERMINANT) {
                /*
                 * Resolver ambos ejes conjuntamente evita depender
                 * de una referencia fija WORLD_UP, que puede provocar
                 * inestabilidad cuando el haz apunta verticalmente.
                 */
                deltaPan = (float) (
                        (panError * c - tiltError * b)
                                / determinant
                );

                deltaTilt = (float) (
                        (tiltError * a - panError * b)
                                / determinant
                );
            } else if (a >= c && a > MIN_DERIVATIVE) {
                /*
                 * Configuración casi singular: conservar el eje
                 * que todavía tiene una derivada útil.
                 */
                deltaPan = (float) (panError / a);
                deltaTilt = 0.0F;
            } else if (c > MIN_DERIVATIVE) {
                deltaPan = 0.0F;
                deltaTilt = (float) (tiltError / c);
            } else {
                break;
            }

            deltaPan = Mth.clamp(
                    deltaPan,
                    -MAX_NEWTON_STEP_DEG,
                    MAX_NEWTON_STEP_DEG
            );

            deltaTilt = Mth.clamp(
                    deltaTilt,
                    -MAX_NEWTON_STEP_DEG,
                    MAX_NEWTON_STEP_DEG
            );

            if (!Float.isFinite(deltaPan)
                    || !Float.isFinite(deltaTilt)) {
                break;
            }

            solvedPan += deltaPan;
            solvedTilt += deltaTilt;

            if (Math.abs(deltaPan) + Math.abs(deltaTilt) < 0.001F) {
                break;
            }
        }

        /*
         * No enviamos directamente el resultado completo del solver.
         * Aplicamos una respuesta amortiguada para que todos los
         * fixtures se acerquen progresivamente al objetivo.
         */
        float panError = solvedPan - startPan;
        float tiltError = solvedTilt - startTilt;

        float panStep = panError * TRACKING_RESPONSE;
        float tiltStep = tiltError * TRACKING_RESPONSE;

        /*
         * Limitar la velocidad combinada evita movimientos bruscos
         * tanto en desplazamientos grandes como en cambios repentinos
         * de dirección.
         */
        float largestStep = Math.max(
                Math.abs(panStep),
                Math.abs(tiltStep)
        );

        if (largestStep > MAX_DEG_PER_TICK) {
            float scale = MAX_DEG_PER_TICK / largestStep;
            panStep *= scale;
            tiltStep *= scale;
        }

        float finalPan = startPan + panStep;
        float finalTilt = startTilt + tiltStep;

        if (!Float.isFinite(finalPan) || !Float.isFinite(finalTilt)) {
            return;
        }

        fixture.applyTrackedAngles(
                finalPan,
                finalTilt,
                false
        );
    }

    private static Vec3 getPanDerivative(
            BaseLightBlockEntity fixture,
            float pan,
            float tilt
    ) {
        return beamDirection(fixture, pan + SAMPLE_DEG, tilt)
                .subtract(
                        beamDirection(fixture, pan - SAMPLE_DEG, tilt)
                )
                .scale(1.0 / (2.0 * SAMPLE_DEG));
    }

    private static Vec3 getTiltDerivative(
            BaseLightBlockEntity fixture,
            float pan,
            float tilt
    ) {
        return beamDirection(fixture, pan, tilt + SAMPLE_DEG)
                .subtract(
                        beamDirection(fixture, pan, tilt - SAMPLE_DEG)
                )
                .scale(1.0 / (2.0 * SAMPLE_DEG));
    }

    /*
     * Geometría del haz.
     * Mantiene las transformaciones de Theatrical para fixtures
     * colgantes, invertidos y montados lateralmente.
     */
    static Vec3 beamDirection(
            BaseLightBlockEntity fixture,
            float pan,
            float tilt
    ) {
        BlockState state = fixture.getBlockState();

        Direction hangDirection = state.getValue(
                BaseLightBlock.HANG_DIRECTION
        );

        Direction facing = state.getValue(
                BaseLightBlock.FACING
        );

        boolean hanging = state.getValue(
                BaseLightBlock.HANGING
        );

        boolean hangingNonVertically = hanging
                && hangDirection != Direction.DOWN
                && hangDirection != Direction.UP;

        Fixture def = fixture.getFixture();

        if (!hangingNonVertically) {
            float t = tilt;

            if (fixture.isUpsideDown() || def.invertTilt()) {
                t = -t;
            }

            float p = facing.toYRot() - pan;

            if (facing.getAxis() == Direction.Axis.X) {
                p -= 180.0F;
            }

            if (def.invertPan()) {
                p = -p;
            }

            if (fixture.isUpsideDown()) {
                if (facing.getAxis() == Direction.Axis.X) {
                    p = facing.getOpposite().toYRot() + pan;
                } else {
                    p = facing.toYRot() + pan;
                }
            }

            float f = t * Mth.DEG_TO_RAD;
            float g = -p * Mth.DEG_TO_RAD;

            return new Vec3(
                    Mth.sin(g) * Mth.cos(f),
                    -Mth.sin(f),
                    Mth.cos(g) * Mth.cos(f)
            );
        }

        Direction opposite = hangDirection.getOpposite();
        int step = opposite.getAxisDirection().getStep();

        float toRad = Mth.DEG_TO_RAD;

        float p = (fixture.getBasePan() + pan) * step * toRad;
        float t = tilt * -step * toRad;

        float x = Mth.sin(p) * Mth.cos(t);
        float y = Mth.sin(t);
        float z = Mth.cos(p) * Mth.cos(t);

        AxisCycle cycle = AxisCycle.VALUES[
                (opposite.getAxis().ordinal() + 2)
                        % AxisCycle.VALUES.length
                ];

        return new Vec3(
                cycle.cycle(x, y, z, Direction.Axis.X),
                cycle.cycle(x, y, z, Direction.Axis.Y),
                cycle.cycle(x, y, z, Direction.Axis.Z)
        ).normalize();
    }

    /*
     * Posición mundial del lente para los ángulos indicados.
     * Conserva la cadena de transformaciones del renderer.
     */
    static Vec3 beamOrigin(
            BaseLightBlockEntity fixture,
            float pan,
            float tilt
    ) {
        BlockState state = fixture.getBlockState();

        BlockPos blockPos = fixture.getBlockPos();

        Fixture def = fixture.getFixture();

        boolean isHanging = state.getValue(
                BaseLightBlock.HANGING
        );

        boolean isFlipped = fixture.isUpsideDown();

        Direction facing = state.getValue(
                HangableBlock.FACING
        );

        Matrix4f matrix = new Matrix4f();

        matrix.translate(0.5F, 0.0F, 0.5F);

        if (isHanging) {
            Direction hangDirection = state.getValue(
                    HangableBlock.HANG_DIRECTION
            );

            matrix.translate(0.0F, 0.5F, 0.0F);

            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    if (hangDirection == Direction.SOUTH) {
                        matrix.rotateZ(rad(90.0F));
                        matrix.rotateX(rad(-90.0F));
                    } else {
                        matrix.rotateZ(rad(90.0F));
                        matrix.rotateX(rad(90.0F));
                    }
                } else if (hangDirection == Direction.EAST) {
                    matrix.rotateZ(rad(90.0F));
                } else {
                    matrix.rotateZ(rad(-90.0F));
                }
            } else if (hangDirection == Direction.UP) {
                matrix.rotateX(rad(180.0F));
            }

            matrix.translate(0.0F, -0.5F, 0.0F);
        }

        matrix.rotateY(rad(facing.toYRot()));
        matrix.translate(-0.5F, 0.0F, -0.5F);

        if (isHanging) {
            Optional<BlockState> support =
                    fixture.getSupportingStructure();

            if (support.isPresent()) {
                float[] transforms = def.getTransforms(
                        state,
                        support.get()
                );

                matrix.translate(
                        transforms[0],
                        transforms[1],
                        transforms[2]
                );
            } else {
                matrix.translate(0.0F, 0.19F, 0.0F);
            }

            matrix.translate(0.0F, -0.08F, 0.0F);
        }

        if (isFlipped) {
            matrix.translate(0.5F, 0.5F, 0.5F);
            matrix.rotateZ(rad(180.0F));
            matrix.translate(-0.5F, -0.5F, -0.5F);
        }

        float[] pans = def.getPanRotationPosition();

        matrix.translate(pans[0], pans[1], pans[2]);
        matrix.rotateY(rad(pan));
        matrix.translate(-pans[0], -pans[1], -pans[2]);

        float[] tilts = def.getTiltRotationPosition();

        matrix.translate(tilts[0], tilts[1], tilts[2]);
        matrix.rotateX(rad(180.0F));
        matrix.rotateX(rad(tilt));
        matrix.translate(-tilts[0], -tilts[1], -tilts[2]);

        float[] beam = def.getBeamStartPosition();

        matrix.translate(
                beam[0],
                beam[1],
                beam[2]
        );

        Vector3f local = matrix.getTranslation(new Vector3f());

        return new Vec3(
                blockPos.getX() + local.x,
                blockPos.getY() + local.y,
                blockPos.getZ() + local.z
        );
    }

    private static float rad(float degrees) {
        return degrees * Mth.DEG_TO_RAD;
    }
}