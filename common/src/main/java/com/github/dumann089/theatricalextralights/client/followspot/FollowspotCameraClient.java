package com.github.dumann089.theatricalextralights.client.followspot;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class FollowspotCameraClient {

    private FollowspotCameraClient() {
    }

    public static void init() {
        ClientTickEvent.CLIENT_PRE.register(FollowspotCameraClient::onClientPreTick);
        ClientTickEvent.CLIENT_POST.register(FollowspotCameraClient::onClientPostTick);
        ClientGuiEvent.RENDER_HUD.register(FollowspotHud::render);
        ClientRawInputEvent.MOUSE_SCROLLED.register(FollowspotCameraClient::onMouseScrolled);
        ClientRawInputEvent.MOUSE_CLICKED_PRE.register(FollowspotCameraClient::onMouseClicked);
        ClientRawInputEvent.KEY_PRESSED.register(FollowspotCameraClient::onKeyPressed);
    }

    private static void onClientPreTick(Minecraft minecraft) {
        FollowspotFixtureCameraSession.tickExitGrace();
        if (!FollowspotFixtureCameraSession.isActive()) {
            return;
        }
        FollowspotFixtureCameraSession.getActive().tick(minecraft);
    }

    private static void onClientPostTick(Minecraft minecraft) {
        if (!FollowspotFixtureCameraSession.isActive()
                || FollowspotFixtureCameraSession.usesForgeCameraHook()
                || minecraft.gameRenderer == null) {
            return;
        }
        FollowspotFixtureCameraSession.getActive().applyCamera(minecraft.gameRenderer.getMainCamera());
    }

    private static EventResult onMouseScrolled(Minecraft minecraft, double amount) {
        FollowspotFixtureCameraSession session = FollowspotFixtureCameraSession.getActive();
        if (session == null || minecraft.screen != null) {
            return EventResult.pass();
        }
        session.onMouseScroll(minecraft, amount);
        return EventResult.interruptFalse();
    }

    private static EventResult onMouseClicked(Minecraft minecraft, int button, int action, int mods) {
        if (!FollowspotFixtureCameraSession.isActive() || minecraft.screen != null) {
            return EventResult.pass();
        }
        // Pas de casse de bloc ni d'utilisation d'objet pendant qu'on pilote la poursuite.
        return EventResult.interruptFalse();
    }

    private static EventResult onKeyPressed(Minecraft minecraft, int keyCode, int scanCode, int action, int modifiers) {
        FollowspotFixtureCameraSession session = FollowspotFixtureCameraSession.getActive();
        if (session == null || minecraft.screen != null) {
            return EventResult.pass();
        }
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            if (action == GLFW.GLFW_PRESS) {
                session.toggleBlackout();
            }
            return EventResult.interruptFalse();
        }
        // Les touches qui ouvriraient un ecran (inventaire, chat) couperaient la session : on les avale.
        if (minecraft.options.keyInventory.matches(keyCode, scanCode)
                || minecraft.options.keyChat.matches(keyCode, scanCode)
                || minecraft.options.keyCommand.matches(keyCode, scanCode)
                || minecraft.options.keyDrop.matches(keyCode, scanCode)
                || minecraft.options.keySwapOffhand.matches(keyCode, scanCode)) {
            return EventResult.interruptFalse();
        }
        return EventResult.pass();
    }
}
