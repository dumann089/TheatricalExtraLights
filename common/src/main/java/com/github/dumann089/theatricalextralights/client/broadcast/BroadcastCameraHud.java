package com.github.dumann089.theatricalextralights.client.broadcast;

import com.github.dumann089.theatricalextralights.client.ModKeybinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Visor da câmera: cantos, área segura, REC piscando, zoom e dicas de controle. */
public final class BroadcastCameraHud {

    private static final int WHITE = 0xFFFFFFFF;
    private static final int SOFT = 0x99FFFFFF;
    private static final int RED = 0xFFFF2A2A;

    private BroadcastCameraHud() {
    }

    public static void render(GuiGraphics g, float partialTick) {
        BroadcastCameraSession session = BroadcastCameraSession.getActive();
        Minecraft minecraft = Minecraft.getInstance();
        if (session == null || minecraft == null) {
            return;
        }
        int w = g.guiWidth();
        int h = g.guiHeight();
        if (session.isHudHidden()) {
            return;
        }
        Font font = minecraft.font;

        drawCorners(g, w, h);
        drawSafeArea(g, w, h, 0.90f);
        drawSafeArea(g, w, h, 0.80f);
        drawCenterMark(g, w / 2, h / 2);

        int margin = Math.max(14, (int) (Math.min(w, h) * 0.07f));
        boolean blink = (minecraft.level != null ? minecraft.level.getGameTime() / 10 : 0) % 2 == 0;
        if (blink) {
            g.fill(margin, margin + 1, margin + 7, margin + 8, RED);
        }
        g.drawString(font, Component.translatable("hud.theatricalextralights.broadcast_camera.rec"),
                margin + 11, margin, WHITE, true);
        if (session.isRecording()) {
            g.drawString(font, Component.translatable("hud.theatricalextralights.recorder.recording",
                    CameraRecording.clock(session.recordingSeconds())), margin, margin + 11, RED, true);
        } else if (session.isPlaying()) {
            g.drawString(font, Component.translatable("hud.theatricalextralights.recorder.playing",
                            session.playbackName(), CameraRecording.clock(session.playbackSeconds()),
                            CameraRecording.clock(session.playbackDuration())),
                    margin, margin + 11, 0xFF7CFF7C, true);
        }

        Component zoom = Component.translatable("hud.theatricalextralights.broadcast_camera.zoom",
                String.format(Locale.ROOT, "%.1f", session.zoomFactor()));
        g.drawString(font, zoom, w - margin - font.width(zoom), margin, WHITE, true);
        if (session.isQuickZoomMode()) {
            Component quick = Component.translatable("hud.theatricalextralights.broadcast_camera.quick");
            g.drawString(font, quick, w - margin - font.width(quick), margin + 11, 0xFFFFD84A, true);
        }

        Component aim = Component.translatable("hud.theatricalextralights.broadcast_camera.aim",
                Math.round(session.getYaw()), Math.round(-session.getPitch()));
        g.drawString(font, aim, margin, h - margin - 10, SOFT, true);

        // Centro, no alto: exposição (se mexida) e aviso de mouse solto.
        int top = margin;
        float ev = session.getExposureEv();
        if (Math.abs(ev) > 0.01f) {
            Component exposure = Component.translatable("hud.theatricalextralights.broadcast_camera.exposure",
                    String.format(Locale.ROOT, "%+.1f", ev));
            g.drawString(font, exposure, (w - font.width(exposure)) / 2, top, WHITE, true);
            top += 11;
        }
        if (session.isMouseFree()) {
            Component free = Component.translatable("hud.theatricalextralights.broadcast_camera.mouse_free");
            g.drawString(font, free, (w - font.width(free)) / 2, top, RED, true);
        }

        Component hint = Component.translatable(session.isJib()
                        ? "hud.theatricalextralights.jib_camera.hint"
                        : "hud.theatricalextralights.broadcast_camera.hint",
                ModKeybinds.BROADCAST_ZOOM_IN.getTranslatedKeyMessage(),
                ModKeybinds.BROADCAST_ZOOM_OUT.getTranslatedKeyMessage());
        Component hint2 = Component.translatable("hud.theatricalextralights.broadcast_camera.hint2");
        g.drawString(font, hint, w - margin - font.width(hint), h - margin - 21, SOFT, true);
        g.drawString(font, hint2, w - margin - font.width(hint2), h - margin - 10, SOFT, true);
        Component recorderHint = Component.translatable(session.isRecording() || session.isPlaying()
                ? "hud.theatricalextralights.recorder.hint_stop"
                : "hud.theatricalextralights.recorder.hint");
        g.drawString(font, recorderHint, w - margin - font.width(recorderHint), h - margin - 32, SOFT, true);
    }

    private static void drawCorners(GuiGraphics g, int w, int h) {
        int m = Math.max(10, (int) (Math.min(w, h) * 0.05f));
        int len = Math.max(12, (int) (Math.min(w, h) * 0.05f));
        // superior esquerdo, superior direito, inferior esquerdo, inferior direito
        g.fill(m, m, m + len, m + 1, WHITE);
        g.fill(m, m, m + 1, m + len, WHITE);
        g.fill(w - m - len, m, w - m, m + 1, WHITE);
        g.fill(w - m - 1, m, w - m, m + len, WHITE);
        g.fill(m, h - m - 1, m + len, h - m, WHITE);
        g.fill(m, h - m - len, m + 1, h - m, WHITE);
        g.fill(w - m - len, h - m - 1, w - m, h - m, WHITE);
        g.fill(w - m - 1, h - m - len, w - m, h - m, WHITE);
    }

    private static void drawSafeArea(GuiGraphics g, int w, int h, float fraction) {
        int x0 = (int) (w * (1f - fraction) / 2f);
        int y0 = (int) (h * (1f - fraction) / 2f);
        int x1 = w - x0;
        int y1 = h - y0;
        int c = 0x44FFFFFF;
        g.fill(x0, y0, x1, y0 + 1, c);
        g.fill(x0, y1 - 1, x1, y1, c);
        g.fill(x0, y0, x0 + 1, y1, c);
        g.fill(x1 - 1, y0, x1, y1, c);
    }

    private static void drawCenterMark(GuiGraphics g, int cx, int cy) {
        g.fill(cx - 6, cy, cx - 2, cy + 1, SOFT);
        g.fill(cx + 3, cy, cx + 7, cy + 1, SOFT);
        g.fill(cx, cy - 6, cx + 1, cy - 2, SOFT);
        g.fill(cx, cy + 3, cx + 1, cy + 7, SOFT);
    }
}
