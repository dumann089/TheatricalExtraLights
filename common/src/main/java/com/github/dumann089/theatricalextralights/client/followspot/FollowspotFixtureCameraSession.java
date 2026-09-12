package com.github.dumann089.theatricalextralights.client.followspot;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.FollowspotConsoleBlockEntity;
import com.github.dumann089.theatricalextralights.net.FollowspotConsoleControlPacket;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;
import com.github.dumann089.theatricalextralights.util.FollowspotAimMapper;
import com.github.dumann089.theatricalextralights.util.FollowspotBeamHelper;
import com.github.dumann089.theatricalextralights.util.FollowspotDmxHelper;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Mode operateur : camera a cote de la lyre, visee a la souris (la rotation du joueur sert
 * de capteur, remise a zero a chaque lecture), molette pour l'intensite, Maj + molette pour
 * le focus, Espace pour le blackout, Echap pour sortir. Le sens des commandes est resolu
 * dans le repere ecran par {@link FollowspotAimMapper}, donc identique quelle que soit
 * l'accroche de la machine.
 */
public final class FollowspotFixtureCameraSession {

    private static final int EXIT_GRACE_TICKS = 15;
    private static final float PAN_MIN = -90f;
    private static final float PAN_MAX = 90f;
    private static final float TILT_MIN = -45f;
    private static final float TILT_MAX = 45f;
    private static final int WHEEL_STEP = 8;

    private static FollowspotFixtureCameraSession active;
    private static boolean forgeCameraHookActive;

    private static BlockPos exitFixturePos = BlockPos.ZERO;
    private static int exitPan;
    private static int exitTilt;
    private static int exitGraceTicks;

    private final BlockPos consolePos;
    private final BlockPos fixturePos;
    private final boolean panTiltOnly;

    private int intensity;
    private int red;
    private int green;
    private int blue;
    private int focus;
    private float panAngle;
    private float tiltAngle;

    /** Intensite memorisee pendant un blackout (0 = pas de blackout en cours). */
    private int blackoutRestore = -1;

    private float savedPlayerYaw;
    private float savedPlayerPitch;
    private boolean savedMouseGrabbed;

    private int controlSendCooldown;
    private boolean dirty;
    private long startedAtMillis;

    private FollowspotFixtureCameraSession(BlockPos consolePos, BlockPos fixturePos, boolean panTiltOnly,
                                           int intensity, int red, int green, int blue, int focus,
                                           float pan, float tilt) {
        this.consolePos = consolePos;
        this.fixturePos = fixturePos;
        this.panTiltOnly = panTiltOnly;
        this.intensity = intensity;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.focus = focus;
        this.panAngle = pan;
        this.tiltAngle = tilt;
    }

    // ── Etat statique ────────────────────────────────────────────────────────

    public static boolean isActive() {
        return active != null;
    }

    public static FollowspotFixtureCameraSession getActive() {
        return active;
    }

    public static boolean isControlling(BlockPos fixturePos) {
        return active != null && active.fixturePos.equals(fixturePos);
    }

    public static boolean usesForgeCameraHook() {
        return forgeCameraHookActive;
    }

    public static boolean shouldPreserveExitAngles(BlockPos fixturePos) {
        return exitGraceTicks > 0 && exitFixturePos.equals(fixturePos);
    }

    public static int getExitPan(BlockPos fixturePos) {
        return shouldPreserveExitAngles(fixturePos) ? exitPan : 0;
    }

    public static int getExitTilt(BlockPos fixturePos) {
        return shouldPreserveExitAngles(fixturePos) ? exitTilt : 0;
    }

    public static void tickExitGrace() {
        if (exitGraceTicks <= 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.level != null) {
            BlockEntity be = minecraft.level.getBlockEntity(exitFixturePos);
            if (be instanceof ExtraLightsLightBlockEntity extra) {
                extra.syncOperatorAngles(exitPan, exitTilt);
            }
        }
        exitGraceTicks--;
    }

    public static void start(FollowspotConsoleBlockEntity console, BlockPos consolePos, BlockPos fixturePos,
                             int intensity, int red, int green, int blue, int focus, int pan, int tilt) {
        exitGraceTicks = 0;
        active = new FollowspotFixtureCameraSession(consolePos, fixturePos, console.isPanTiltOnly(),
                intensity, red, green, blue, focus, pan, tilt);
        active.snapAnglesToDmx();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.setScreen(null);
            active.captureInput(minecraft);
            active.applyLocalFixtureState();
            active.sendControlNow();
            active.startedAtMillis = System.currentTimeMillis();
        }
        registerPlatformCameraHook();
    }

    private static void registerPlatformCameraHook() {
        forgeCameraHookActive = false;
        try {
            Class<?> forgeHook = Class.forName("com.github.dumann089.theatricalextralights.forge.FollowspotCameraForge");
            forgeHook.getMethod("ensureRegistered").invoke(null);
            forgeCameraHookActive = true;
        } catch (ReflectiveOperationException ignored) {
            // Fabric : camera appliquee par le hook commun a chaque tick
        }
    }

    public static void stop() {
        if (active != null) {
            active.finalizeSession();
            exitFixturePos = active.fixturePos;
            exitPan = active.getPan();
            exitTilt = active.getTilt();
            exitGraceTicks = EXIT_GRACE_TICKS;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                active.restoreInput(minecraft);
            }
        }
        active = null;
        com.github.dumann089.theatricalextralights.client.blockentities.FollowspotRenderer.resetBeamLengthSmoothing();
    }

    // ── Entree souris / clavier ──────────────────────────────────────────────

    private void captureInput(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player != null) {
            savedPlayerYaw = player.getYRot();
            savedPlayerPitch = player.getXRot();
            resetPlayerRotation(player);
        }
        savedMouseGrabbed = minecraft.mouseHandler.isMouseGrabbed();
        if (!savedMouseGrabbed) {
            minecraft.mouseHandler.grabMouse();
        }
    }

    private void restoreInput(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player != null) {
            player.setYRot(savedPlayerYaw);
            player.setXRot(savedPlayerPitch);
            player.yRotO = savedPlayerYaw;
            player.xRotO = savedPlayerPitch;
            player.yHeadRot = savedPlayerYaw;
            player.yHeadRotO = savedPlayerYaw;
        }
    }

    /** La rotation du joueur sert de capteur souris : on la remet au repere de depart apres lecture. */
    private void resetPlayerRotation(LocalPlayer player) {
        player.setYRot(savedPlayerYaw);
        player.setXRot(0f);
        player.yRotO = savedPlayerYaw;
        player.xRotO = 0f;
    }

    /** Lit le deplacement souris accumule depuis le dernier appel et le convertit en pan/tilt. */
    public void consumeMouse(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            return;
        }
        float dYaw = Mth.wrapDegrees(player.getYRot() - savedPlayerYaw);
        float dPitch = player.getXRot();
        if (Math.abs(dYaw) < 1.0e-4f && Math.abs(dPitch) < 1.0e-4f) {
            return;
        }
        resetPlayerRotation(player);
        // Souris a droite : yaw augmente ; souris en bas : pitch augmente (regard vers le bas).
        aimScreen(dYaw, -dPitch);
    }

    /** Deplace la tache de {@code rightDeg} vers la droite et {@code upDeg} vers le haut de l'ecran. */
    private void aimScreen(double rightDeg, double upDeg) {
        BaseLightBlockEntity fixture = getFixture();
        if (fixture == null) {
            return;
        }
        float[] delta = FollowspotAimMapper.solveDegrees(fixture, panAngle, tiltAngle, rightDeg, upDeg);
        if (delta[0] == 0f && delta[1] == 0f) {
            return;
        }
        panAngle = Mth.clamp(panAngle + delta[0], PAN_MIN, PAN_MAX);
        tiltAngle = Mth.clamp(tiltAngle + delta[1], TILT_MIN, TILT_MAX);
        dirty = true;
    }

    private void handleMovementKeys(Minecraft minecraft) {
        double step = FollowspotDmxHelper.PAN_TILT_STEP;
        double right = 0;
        double up = 0;
        if (FollowspotInputHelper.isKeyDown(minecraft.options.keyUp)) up += step;
        if (FollowspotInputHelper.isKeyDown(minecraft.options.keyDown)) up -= step;
        if (FollowspotInputHelper.isKeyDown(minecraft.options.keyLeft)) right -= step;
        if (FollowspotInputHelper.isKeyDown(minecraft.options.keyRight)) right += step;
        if (right != 0 || up != 0) {
            aimScreen(right, up);
        }
    }

    /** Molette : intensite, Maj + molette : focus. Retourne true si consomme. */
    public boolean onMouseScroll(Minecraft minecraft, double amount) {
        if (panTiltOnly || amount == 0) {
            return true; // on avale quand meme pour ne pas changer d'item
        }
        int step = amount > 0 ? WHEEL_STEP : -WHEEL_STEP;
        boolean shift = minecraft.options.keyShift.isDown()
                || FollowspotInputHelper.isKeyDown(minecraft.options.keyShift);
        if (shift) {
            focus = Mth.clamp(focus + step, 0, 255);
        } else {
            if (blackoutRestore >= 0) {
                intensity = blackoutRestore;
                blackoutRestore = -1;
            }
            intensity = Mth.clamp(intensity + step, 0, 255);
        }
        dirty = true;
        sendControlIfReady();
        return true;
    }

    /** Espace : blackout (intensite a 0, memorisee) ou retour. */
    public void toggleBlackout() {
        if (panTiltOnly) {
            return;
        }
        if (blackoutRestore >= 0) {
            intensity = blackoutRestore;
            blackoutRestore = -1;
        } else {
            blackoutRestore = intensity;
            intensity = 0;
        }
        dirty = true;
        sendControlNow();
    }

    public boolean isBlackout() {
        return blackoutRestore >= 0;
    }

    // ── Tick / frame ─────────────────────────────────────────────────────────

    public void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || getFixture() == null) {
            stop();
            return;
        }
        if (FollowspotInputHelper.isEscapeDown(minecraft) || minecraft.screen != null) {
            stop();
            return;
        }

        consumeMouse(minecraft);
        handleMovementKeys(minecraft);
        if (dirty) {
            snapAnglesToDmx();
            applyLocalFixtureState();
            sendControlIfReady();
        }
        applyLocalFixtureState();

        LocalPlayer player = minecraft.player;
        player.setDeltaMovement(0, 0, 0);

        if (controlSendCooldown > 0) {
            controlSendCooldown--;
            if (controlSendCooldown == 0 && dirty) {
                sendControlNow();
            }
        }
    }

    /** Appele a chaque frame par le HUD : visee fluide entre deux ticks. */
    public void frame(Minecraft minecraft) {
        consumeMouse(minecraft);
        if (dirty) {
            applyLocalFixtureState();
        }
    }

    public record CameraState(Vec3 position, float yaw, float pitch) {
    }

    public CameraState getCameraState() {
        BaseLightBlockEntity fixture = getFixture();
        if (fixture == null) {
            return null;
        }
        float[] look = FollowspotBeamHelper.getLookAngles(fixture, panAngle, tiltAngle);
        return new CameraState(FollowspotBeamHelper.getCameraPosition(fixture, panAngle, tiltAngle), look[0], look[1]);
    }

    public void applyCamera(Camera camera) {
        CameraState state = getCameraState();
        if (state == null) {
            return;
        }
        FollowspotCameraAccess.tryApplyCameraState(camera, state.position(), state.yaw(), state.pitch());
    }

    // ── Accesseurs pour le HUD ───────────────────────────────────────────────

    public BlockPos getFixturePos() { return fixturePos; }
    public float getPanAngle() { return panAngle; }
    public float getTiltAngle() { return tiltAngle; }
    public int getPan() { return FollowspotDmxHelper.quantizePan(panAngle); }
    public int getTilt() { return FollowspotDmxHelper.quantizeTilt(tiltAngle); }
    public int getIntensity() { return intensity; }
    public int getFocus() { return focus; }
    public int getColour() { return (red << 16) | (green << 8) | blue; }
    public boolean isPanTiltOnly() { return panTiltOnly; }
    public long getStartedAtMillis() { return startedAtMillis; }

    public BaseLightBlockEntity getFixture() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        BlockEntity be = minecraft.level.getBlockEntity(fixturePos);
        return be instanceof BaseLightBlockEntity light ? light : null;
    }

    /** Distance du faisceau jusqu'au premier bloc touche, en blocs (0 si aucun). */
    public float getBeamDistance() {
        BaseLightBlockEntity fixture = getFixture();
        return fixture == null ? 0f : FollowspotBeamHelper.getBeamLength(fixture, panAngle, tiltAngle);
    }

    // ── Interne ──────────────────────────────────────────────────────────────

    private void finalizeSession() {
        snapAnglesToDmx();
        applyLocalFixtureState();
        sendControlNow();
    }

    private void snapAnglesToDmx() {
        panAngle = FollowspotDmxHelper.quantizePanAngle(panAngle);
        tiltAngle = FollowspotDmxHelper.quantizeTiltAngle(tiltAngle);
    }

    private void applyLocalFixtureState() {
        BaseLightBlockEntity fixture = getFixture();
        if (fixture == null) {
            return;
        }
        if (fixture instanceof ExtraLightsLightBlockEntity extra) {
            extra.syncOperatorAngles(panAngle, tiltAngle);
        } else {
            fixture.setPan(getPan());
            fixture.setTilt(getTilt());
        }
    }

    private void sendControlIfReady() {
        if (controlSendCooldown > 0) {
            return;
        }
        sendControlNow();
        controlSendCooldown = 2;
    }

    private void sendControlNow() {
        dirty = false;
        ModNetworkHandler.CHANNEL.sendToServer(new FollowspotConsoleControlPacket(
                consolePos, intensity, red, green, blue, focus, getPan(), getTilt()));
    }
}
