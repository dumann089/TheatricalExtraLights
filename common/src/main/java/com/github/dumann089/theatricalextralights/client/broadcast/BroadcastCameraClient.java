package com.github.dumann089.theatricalextralights.client.broadcast;

import com.github.dumann089.theatricalextralights.client.ModKeybinds;
import com.github.dumann089.theatricalextralights.entities.BroadcastCameraEntity;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import com.github.dumann089.theatricalextralights.blockentities.VideoSwitcherBlockEntity;
import com.github.dumann089.theatricalextralights.blocks.VideoSwitcherBlock;
import com.github.dumann089.theatricalextralights.items.AnimationSynchronizerItem;
import com.github.dumann089.theatricalextralights.net.SyncCameraListPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import org.lwjgl.glfw.GLFW;

/** Liga a sessão de câmera de broadcast aos eventos de tick, mouse, teclado e HUD. */
public final class BroadcastCameraClient {

    private BroadcastCameraClient() {
    }

    public static void init() {
        BroadcastCameraEntity.clientControlHook = BroadcastCameraSession::controlVelocity;
        BroadcastCameraEntity.clientSettingsHook =
                camera -> Minecraft.getInstance().setScreen(new BroadcastCameraSettingsScreen(camera));
        AnimationSynchronizerItem.clientOpenHook = () -> Minecraft.getInstance().setScreen(new AnimationSyncScreen());
        SyncCameraListPacket.clientHook = list -> {
            AnimationSyncScreen.onCameraList(list);
            VideoSwitcherScreen.onCameraList(list);
        };
        VideoSwitcherBlock.clientOpenHook = pos -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof VideoSwitcherBlockEntity desk) {
                minecraft.setScreen(new VideoSwitcherScreen(pos, desk.getConfig().copy()));
            }
        };
        com.github.dumann089.theatricalextralights.blocks.JumbotronBlock.clientOpenHook = pos -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null && minecraft.level.getBlockEntity(pos)
                    instanceof com.github.dumann089.theatricalextralights.blockentities.JumbotronBlockEntity jumbotron) {
                minecraft.setScreen(new com.github.dumann089.theatricalextralights.client.gui.JumbotronScreen(pos, jumbotron));
            }
        };
        ClientTickEvent.CLIENT_PRE.register(JumbotronFeed::tick);
        dev.architectury.event.events.client.ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> JumbotronFeed.disposeAll());
        ClientTickEvent.CLIENT_PRE.register(BroadcastCameraSession::clientTick);
        ClientTickEvent.CLIENT_PRE.register(SwitcherView::tick);
        ClientRawInputEvent.MOUSE_SCROLLED.register(BroadcastCameraClient::onMouseScrolled);
        ClientRawInputEvent.MOUSE_CLICKED_PRE.register(BroadcastCameraClient::onMouseClicked);
        ClientRawInputEvent.KEY_PRESSED.register(BroadcastCameraClient::onKeyPressed);
    }

    private static EventResult onMouseScrolled(Minecraft minecraft, double amount) {
        BroadcastCameraSession session = BroadcastCameraSession.getActive();
        if (session == null || minecraft.screen != null) {
            return EventResult.pass();
        }
        session.onMouseScroll(amount);
        return EventResult.interruptFalse();
    }

    private static EventResult onMouseClicked(Minecraft minecraft, int button, int action, int mods) {
        // Só o aperto é engolido. O soltar tem que chegar ao Minecraft, senão o botão que montou
        // na câmera fica "preso" e clica sozinho depois de sair.
        if (!BroadcastCameraSession.isActive() || minecraft.screen != null || action != GLFW.GLFW_PRESS) {
            return EventResult.pass();
        }
        // Sem quebrar bloco nem usar item; e, com o mouse solto (U), o clique não o captura de volta.
        return EventResult.interruptFalse();
    }

    private static EventResult onKeyPressed(Minecraft minecraft, int keyCode, int scanCode, int action, int modifiers) {
        BroadcastCameraSession session = BroadcastCameraSession.getActive();
        // No Forge este evento chega depois do Minecraft: o Esc já abriu o menu de pausa e a tela deixaria
        // de ser null, então o Esc nunca saía da câmera. Aqui sai e fecha o menu de pausa.
        if (session != null && keyCode == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS
                && minecraft.screen instanceof PauseScreen) {
            BroadcastCameraSession.requestExit();
            minecraft.setScreen(null);
            return EventResult.interruptFalse();
        }
        if (session == null || minecraft.screen != null) {
            return EventResult.pass();
        }
        boolean pressed = action == GLFW.GLFW_PRESS;
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                if (pressed) {
                    BroadcastCameraSession.requestExit();
                }
                // Engole o Esc: sai da câmera sem abrir o menu de pausa por cima.
                return EventResult.interruptFalse();
            }
            case GLFW.GLFW_KEY_Y -> {
                if (pressed) session.toggleHud();
                return EventResult.interruptFalse();
            }
            case GLFW.GLFW_KEY_U -> {
                if (pressed) session.toggleMouse();
                return EventResult.interruptFalse();
            }
            case GLFW.GLFW_KEY_R -> {
                if (pressed) session.onRecordKey();
                return EventResult.interruptFalse();
            }
            case GLFW.GLFW_KEY_Z -> {
                if (pressed) session.toggleQuickZoom();
                return EventResult.interruptFalse();
            }
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> {
                // Também no auto-repeat, para segurar a seta e ir corrigindo a exposição.
                if (action != GLFW.GLFW_RELEASE) {
                    session.adjustExposure(keyCode == GLFW.GLFW_KEY_UP ? 1 : -1);
                }
                return EventResult.interruptFalse();
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (pressed) session.resetExposure();
                return EventResult.interruptFalse();
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                return EventResult.interruptFalse();
            }
            default -> {
            }
        }
        // Teclas de zoom (configuráveis): não deixam outra ação ligada à mesma tecla acontecer.
        if (ModKeybinds.BROADCAST_ZOOM_IN.matches(keyCode, scanCode)
                || ModKeybinds.BROADCAST_ZOOM_OUT.matches(keyCode, scanCode)) {
            return EventResult.interruptFalse();
        }
        // Teclas que abririam uma tela ou mudariam a câmera/inventário.
        if (minecraft.options.keyInventory.matches(keyCode, scanCode)
                || minecraft.options.keyChat.matches(keyCode, scanCode)
                || minecraft.options.keyCommand.matches(keyCode, scanCode)
                || minecraft.options.keyDrop.matches(keyCode, scanCode)
                || minecraft.options.keySwapOffhand.matches(keyCode, scanCode)
                || minecraft.options.keyTogglePerspective.matches(keyCode, scanCode)) {
            return EventResult.interruptFalse();
        }
        for (var hotbarKey : minecraft.options.keyHotbarSlots) {
            if (hotbarKey.matches(keyCode, scanCode)) {
                return EventResult.interruptFalse();
            }
        }
        return EventResult.pass();
    }
}
