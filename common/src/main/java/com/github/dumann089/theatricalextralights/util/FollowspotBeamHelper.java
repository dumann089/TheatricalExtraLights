package com.github.dumann089.theatricalextralights.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Optional;

/**
 * Geometrie du faisceau pour la poursuite.
 *
 * <p>La <b>direction</b> suit exactement la formule de Theatrical ({@code BaseLightBlockEntity.rayTraceDir}),
 * c'est elle qui place la tache lumineuse dans le monde et que voient les autres joueurs. La
 * transformation du modele 3D n'est pas la meme pour une lyre tete en bas ; la camera et le
 * faisceau operateur suivaient autrefois le modele, d'ou un faisceau qui ne partait pas la ou
 * regardait la camera. L'<b>origine</b> (la lentille) vient toujours du modele.
 */
public final class FollowspotBeamHelper {

    /** Decalage lateral de l'oeil de l'operateur par rapport a la lentille, en blocs. */
    private static final float DEFAULT_OPERATOR_EYE_SIDE = -0.85f;
    /** Recul de l'oeil derriere la lentille, le long du faisceau. */
    private static final float OPERATOR_EYE_BACK = 0.25f;
    /** Legere surelevation de l'oeil. */
    private static final float OPERATOR_EYE_UP = 0.05f;

    private static final Vec3 WORLD_UP = new Vec3(0, 1, 0);

    private FollowspotBeamHelper() {
    }

    // ── Origine (lentille, depuis le modele) ─────────────────────────────────

    public static Vec3 getBeamOrigin(BaseLightBlockEntity fixture) {
        return getBeamOrigin(fixture, fixture.getPan(), fixture.getTilt());
    }

    public static Vec3 getBeamOrigin(BaseLightBlockEntity fixture, float pan, float tilt) {
        BlockPos blockPos = fixture.getBlockPos();
        PoseStack poseStack = createFixturePose(fixture, pan, tilt);
        float[] beam = fixture.getFixture().getBeamStartPosition();
        poseStack.translate(beam[0], beam[1], beam[2]);
        Vector3f local = new Vector3f(0f, 0f, 0f);
        local.mulPosition(poseStack.last().pose());
        return new Vec3(blockPos.getX() + local.x, blockPos.getY() + local.y, blockPos.getZ() + local.z);
    }

    // ── Direction (formule Theatrical) ───────────────────────────────────────

    /**
     * Direction reelle de la lumiere pour un pan/tilt donnes, portee de
     * {@code BaseLightBlockEntity.rayTraceDir} avec les angles en parametre.
     */
    public static Vec3 getBeamDirection(BaseLightBlockEntity fixture, float pan, float tilt) {
        BlockState state = fixture.getBlockState();
        Direction hangDirection = state.getValue(BaseLightBlock.HANG_DIRECTION);
        Direction facing = state.getValue(BaseLightBlock.FACING);
        boolean hanging = state.getValue(BaseLightBlock.HANGING);
        boolean hangingNonVertically = hanging && hangDirection != Direction.DOWN && hangDirection != Direction.UP;
        Fixture def = fixture.getFixture();

        if (!hangingNonVertically) {
            float t = tilt;
            if (fixture.isUpsideDown() || def.invertTilt()) {
                t = -t;
            }
            float p = facing.toYRot() - pan;
            if (facing.getAxis() == Direction.Axis.X) {
                p -= 180f;
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
            return calculateViewVector(t, p);
        }

        // Accroche laterale (truss) : reprise telle quelle de Theatrical.
        Direction opposite = hangDirection.getOpposite();
        int step = opposite.getAxisDirection().getStep();
        float toRad = (float) (Math.PI / 180.0);
        float p = (fixture.getBasePan() + pan) * step * toRad;
        float t = tilt * -step * toRad;
        float sinPan = Mth.sin(p);
        float cosPan = Mth.cos(p);
        float cosTilt = Mth.cos(t);
        float x = sinPan * cosTilt;
        float y = Mth.sin(t);
        float z = cosPan * cosTilt;
        AxisCycle cycle = AxisCycle.VALUES[(opposite.getAxis().ordinal() + 2) % AxisCycle.VALUES.length];
        return new Vec3(
                cycle.cycle(x, y, z, Direction.Axis.X),
                cycle.cycle(x, y, z, Direction.Axis.Y),
                cycle.cycle(x, y, z, Direction.Axis.Z)
        ).normalize();
    }

    private static Vec3 calculateViewVector(float xRot, float yRot) {
        float f = xRot * 0.017453292F;
        float g = -yRot * 0.017453292F;
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        float j = Mth.cos(f);
        float k = Mth.sin(f);
        return new Vec3(i * j, -k, h * j);
    }

    /** Yaw / pitch camera (convention Minecraft) regardant le long du faisceau. */
    public static float[] getLookAngles(BaseLightBlockEntity fixture, float pan, float tilt) {
        return directionToLookAngles(getBeamDirection(fixture, pan, tilt));
    }

    // ── Camera operateur ─────────────────────────────────────────────────────

    public static Vec3 getCameraPosition(BaseLightBlockEntity fixture, float pan, float tilt) {
        return getCameraPosition(fixture, pan, tilt, DEFAULT_OPERATOR_EYE_SIDE);
    }

    /** Oeil un peu en retrait de la lentille, decale sur le cote, dans le repere du faisceau. */
    public static Vec3 getCameraPosition(BaseLightBlockEntity fixture, float pan, float tilt, float eyeSide) {
        Vec3 origin = getBeamOrigin(fixture, pan, tilt);
        Vec3 dir = getBeamDirection(fixture, pan, tilt);
        Vec3 right = dir.cross(WORLD_UP);
        if (right.lengthSqr() < 1.0e-4) {
            right = Vec3.atLowerCornerOf(fixture.getBlockState().getValue(BaseLightBlock.FACING).getClockWise().getNormal());
        }
        right = right.normalize();
        Vec3 up = right.cross(dir).normalize();
        return origin
                .subtract(dir.scale(OPERATOR_EYE_BACK))
                .add(right.scale(eyeSide))
                .add(up.scale(OPERATOR_EYE_UP));
    }

    public static float getBeamLength(BaseLightBlockEntity fixture, float pan, float tilt) {
        if (fixture.getLevel() == null) {
            return (float) (fixture.getFixture().getLightRadius() * 4.0);
        }
        Vec3 origin = getBeamOrigin(fixture, pan, tilt);
        Vec3 direction = getBeamDirection(fixture, pan, tilt);
        double maxReach = fixture.getFixture().getLightRadius() * 6.0;
        Vec3 end = origin.add(direction.scale(maxReach));
        BlockHitResult hit = fixture.getLevel().clip(new ClipContext(
                origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return (float) Math.max(origin.distanceTo(hit.getLocation()), 1.0);
        }
        return (float) maxReach;
    }

    // ── Poses ────────────────────────────────────────────────────────────────

    /**
     * Place la pose a l'origine du faisceau (relative au bloc) avec le -Z local oriente le
     * long de la direction reelle : le faisceau operateur dessine vers -Z part exactement la
     * ou regarde la camera.
     */
    public static void applyBeamPose(PoseStack poseStack, BaseLightBlockEntity fixture, float pan, float tilt) {
        BlockPos blockPos = fixture.getBlockPos();
        Vec3 origin = getBeamOrigin(fixture, pan, tilt);
        Vec3 dir = getBeamDirection(fixture, pan, tilt);
        poseStack.translate(origin.x - blockPos.getX(), origin.y - blockPos.getY(), origin.z - blockPos.getZ());
        Quaternionf rotation = new Quaternionf().rotationTo(0f, 0f, -1f, (float) dir.x, (float) dir.y, (float) dir.z);
        poseStack.mulPose(rotation);
    }

    /** Transformation du modele (tete de la lyre), conservee pour le rendu du corps. */
    public static void applyFixtureTransforms(PoseStack poseStack, BaseLightBlockEntity fixture,
                                              BlockState blockState, float pan, float tilt) {
        applyFixtureTransformsInternal(poseStack, fixture, blockState, pan, tilt);
    }

    private static PoseStack createFixturePose(BaseLightBlockEntity fixture, float pan, float tilt) {
        PoseStack poseStack = new PoseStack();
        applyFixtureTransformsInternal(poseStack, fixture, fixture.getBlockState(), pan, tilt);
        return poseStack;
    }

    private static float[] directionToLookAngles(Vec3 direction) {
        Vec3 d = direction.normalize();
        float pitch = (float) Math.toDegrees(-Math.asin(Mth.clamp(d.y, -1.0, 1.0)));
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        return new float[]{yaw, pitch};
    }

    private static void applyFixtureTransformsInternal(PoseStack poseStack, BaseLightBlockEntity fixture,
                                                       BlockState blockState, float pan, float tilt) {
        Fixture fixtureDef = fixture.getFixture();
        boolean isHanging = blockState.getValue(BaseLightBlock.HANGING);
        boolean isFlipped = fixture.isUpsideDown();
        Direction facing = blockState.getValue(HangableBlock.FACING);

        poseStack.translate(0.5F, 0, 0.5F);
        if (isHanging) {
            Direction hangDirection = blockState.getValue(HangableBlock.HANG_DIRECTION);
            poseStack.translate(0, 0.5, 0F);
            if (hangDirection.getAxis() != Direction.Axis.Y) {
                if (hangDirection.getAxis() == Direction.Axis.Z) {
                    if (hangDirection == Direction.SOUTH) {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                    } else {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                        poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    }
                } else {
                    if (hangDirection == Direction.EAST) {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(-90));
                    } else {
                        poseStack.mulPose(Axis.ZN.rotationDegrees(90));
                    }
                }
            } else if (hangDirection == Direction.UP) {
                poseStack.mulPose(Axis.XP.rotationDegrees(180));
            }
            poseStack.translate(0, -0.5, 0F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        poseStack.translate(-0.5F, 0, -0.5F);
        if (isHanging) {
            Optional<BlockState> optionalSupport = fixture.getSupportingStructure();
            if (optionalSupport.isPresent()) {
                float[] transforms = fixtureDef.getTransforms(blockState, optionalSupport.get());
                poseStack.translate(transforms[0], transforms[1], transforms[2]);
            } else {
                poseStack.translate(0, 0.19, 0);
            }
            poseStack.translate(0, -0.08, 0);
        }
        if (isFlipped) {
            poseStack.translate(0.5F, 0.5, 0.5F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            poseStack.translate(-0.5F, -0.5, -0.5F);
        }

        float[] pans = fixtureDef.getPanRotationPosition();
        poseStack.translate(pans[0], pans[1], pans[2]);
        poseStack.mulPose(Axis.YP.rotationDegrees(pan));
        poseStack.translate(-pans[0], -pans[1], -pans[2]);

        float[] tilts = fixtureDef.getTiltRotationPosition();
        poseStack.translate(tilts[0], tilts[1], tilts[2]);
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        poseStack.mulPose(Axis.XP.rotationDegrees(tilt));
        poseStack.translate(-tilts[0], -tilts[1], -tilts[2]);
    }
}
