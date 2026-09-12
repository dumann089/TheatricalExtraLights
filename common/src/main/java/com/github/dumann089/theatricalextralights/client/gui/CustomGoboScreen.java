package com.github.dumann089.theatricalextralights.client.gui;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasGobo;
import com.github.dumann089.theatricalextralights.client.CustomGoboLoader;
import com.github.dumann089.theatricalextralights.client.gobo.GoboLibrary;
import com.github.dumann089.theatricalextralights.net.ModNetworking;
import com.github.dumann089.theatricalextralights.util.GlobalGoboManager;
import com.github.dumann089.theatricalextralights.util.GoboFileManager;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gobos personnalises : a gauche la grille des PNG locaux, a droite la roue du projecteur
 * (un emplacement par slot) avec l'apercu du slot choisi.
 */
public class CustomGoboScreen extends TelScaledScreen {

    private static final int PANEL_W = 560;
    private static final int PANEL_H = 330;
    private static final int PAD = 14;
    private static final int GAP = 10;
    private static final int HEADER_H = 26;
    private static final int TILE = 56;
    private static final int TILE_GAP = 6;
    private static final int SLOT = 24;
    private static final int SLOT_GAP = 4;
    private static final int PREVIEW_MAX = 96;
    private static final int FOOTER_H = 20 + 6 + 20;   // restaurer + actions
    private static final int CAPTION_H = 30;           // slot + statut

    private final Screen parent;
    private final GoboLibrary library;

    private int selectedSlot;
    private final List<String> availableFiles = new ArrayList<>();

    /** null = garder l'actuel | "" = restaurer l'original | "x.png" = nouvelle texture */
    private String selectedFile = null;

    private int currentPage = 0;

    // Geometrie calculee dans init()
    private int panelX, panelY, panelW, panelH;
    private int leftX, leftY, leftW, leftH;
    private int rightX, rightY, rightW, rightH;
    private int gridX, gridY, gridCols, gridRows;
    private int slotsX, slotsY, slotCols;
    private int previewX, previewY, previewSize;

    private TelUi.FlatButton prevPageButton;
    private TelUi.FlatButton nextPageButton;
    private TelUi.FlatButton restoreButton;
    private TelUi.FlatButton saveButton;

    public CustomGoboScreen(Screen parent, HasGobo fixture) {
        super(Component.translatable("screen.extralightsconfig.custom_gobos_title"));
        this.parent = parent;
        this.library = fixture.getGoboLibrary();
        this.selectedSlot = Math.max(0, Math.min(fixture.getGobo(), library.getSlotCount() - 1));
    }

    @Override
    protected void init() {
        super.init();
        loadAvailableFiles();
        fitToScreen(PANEL_W, PANEL_H);
        layout();
        buildWidgets();
    }

    private void layout() {
        panelW = Math.min(vw - 16, PANEL_W);
        panelH = Math.min(vh - 16, PANEL_H);
        panelX = (vw - panelW) / 2;
        panelY = (vh - panelH) / 2;

        int innerTop = panelY + HEADER_H + PAD;
        int innerH = panelH - HEADER_H - PAD * 2;

        rightW = 190;
        leftX = panelX + PAD;
        leftY = innerTop;
        leftW = panelW - PAD * 2 - GAP - rightW;
        leftH = innerH;

        rightX = leftX + leftW + GAP;
        rightY = innerTop;
        rightH = innerH;

        // Grille de fichiers : titre (12) + grille + pagination (20)
        gridX = leftX + 6;
        gridY = leftY + 18;
        gridCols = Math.max(1, (leftW - 12 + TILE_GAP) / (TILE + TILE_GAP));
        int gridAvailH = leftH - 18 - 6 - 20 - 6;
        gridRows = Math.max(1, (gridAvailH + TILE_GAP) / (TILE + TILE_GAP));

        // Roue : titre (12) + rangees de slots
        slotsX = rightX + 6;
        slotsY = rightY + 18;
        slotCols = Math.max(1, (rightW - 12 + SLOT_GAP) / (SLOT + SLOT_GAP));

        int slotRows = (library.getSlotCount() + slotCols - 1) / slotCols;
        int afterSlots = slotsY + slotRows * (SLOT + SLOT_GAP) + 6;
        int remaining = rightY + rightH - 6 - FOOTER_H - CAPTION_H - afterSlots;
        previewSize = Math.max(32, Math.min(PREVIEW_MAX, remaining));
        previewX = rightX + (rightW - previewSize) / 2;
        previewY = afterSlots;
    }

    private int itemsPerPage() {
        return gridCols * gridRows;
    }

    private int totalPages() {
        return Math.max(1, (availableFiles.size() + itemsPerPage() - 1) / itemsPerPage());
    }

    private void buildWidgets() {
        clearWidgets();
        currentPage = Math.min(currentPage, totalPages() - 1);

        // Pagination en bas de la carte de gauche
        int pagY = leftY + leftH - 6 - 20;
        prevPageButton = addRenderableWidget(new TelUi.FlatButton(leftX + 6, pagY, 24, 20,
                Component.literal("<"), TelUi.ButtonStyle.NORMAL, b -> {
                    if (currentPage > 0) {
                        currentPage--;
                        buildWidgets();
                    }
                }));
        nextPageButton = addRenderableWidget(new TelUi.FlatButton(leftX + leftW - 6 - 24, pagY, 24, 20,
                Component.literal(">"), TelUi.ButtonStyle.NORMAL, b -> {
                    if (currentPage < totalPages() - 1) {
                        currentPage++;
                        buildWidgets();
                    }
                }));
        prevPageButton.active = currentPage > 0;
        nextPageButton.active = currentPage < totalPages() - 1;

        // Actions a droite
        int footerY = rightY + rightH - 20;
        restoreButton = addRenderableWidget(new TelUi.FlatButton(rightX + 6, footerY - 20 - 6, rightW - 12, 20,
                Component.translatable("screen.customgobo.restore"), TelUi.ButtonStyle.GHOST, b -> {
                    selectedFile = "".equals(selectedFile) ? null : "";
                    refreshButtons();
                }));

        int half = (rightW - 12 - 6) / 2;
        addRenderableWidget(new TelUi.FlatButton(rightX + 6, footerY, half, 20,
                Component.translatable("gui.cancel"), TelUi.ButtonStyle.NORMAL,
                b -> Minecraft.getInstance().setScreen(parent)));
        saveButton = addRenderableWidget(new TelUi.FlatButton(rightX + 6 + half + 6, footerY, half, 20,
                Component.translatable("artneti.save"), TelUi.ButtonStyle.PRIMARY, b -> {
                    saveChanges();
                    Minecraft.getInstance().setScreen(parent);
                }));
        refreshButtons();
    }

    private void refreshButtons() {
        boolean hasCustom = GlobalGoboManager.getCustomGobo(library, selectedSlot) != null;
        restoreButton.active = hasCustom || "".equals(selectedFile);
        restoreButton.setSelected("".equals(selectedFile));
        saveButton.active = selectedFile != null;
    }

    private void loadAvailableFiles() {
        availableFiles.clear();
        File dir = GoboFileManager.getLocalConfigDir();
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".png"));
        if (files != null) {
            for (File f : files) {
                availableFiles.add(f.getName());
            }
            availableFiles.sort(String.CASE_INSENSITIVE_ORDER);
        }
    }

    // ── Souris ───────────────────────────────────────────────────────────────

    @Override
    protected boolean scaledMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int tile = tileAt(mouseX, mouseY);
            if (tile >= 0) {
                String file = availableFiles.get(tile);
                selectedFile = file.equals(selectedFile) ? null : file;
                refreshButtons();
                return true;
            }
            int slot = slotAt(mouseX, mouseY);
            if (slot >= 0) {
                if (slot != selectedSlot) {
                    selectedSlot = slot;
                    selectedFile = null;
                    refreshButtons();
                }
                return true;
            }
        }
        return super.scaledMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean scaledMouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= leftX && mouseX < leftX + leftW && mouseY >= leftY && mouseY < leftY + leftH) {
            int target = currentPage + (delta < 0 ? 1 : -1);
            if (target >= 0 && target < totalPages()) {
                currentPage = target;
                buildWidgets();
            }
            return true;
        }
        return super.scaledMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int tileAt(double mx, double my) {
        int start = currentPage * itemsPerPage();
        for (int i = 0; i < itemsPerPage(); i++) {
            int idx = start + i;
            if (idx >= availableFiles.size()) {
                break;
            }
            int x = gridX + (i % gridCols) * (TILE + TILE_GAP);
            int y = gridY + (i / gridCols) * (TILE + TILE_GAP);
            if (mx >= x && mx < x + TILE && my >= y && my < y + TILE) {
                return idx;
            }
        }
        return -1;
    }

    private int slotAt(double mx, double my) {
        for (int s = 0; s < library.getSlotCount(); s++) {
            int x = slotsX + (s % slotCols) * (SLOT + SLOT_GAP);
            int y = slotsY + (s / slotCols) * (SLOT + SLOT_GAP);
            if (mx >= x && mx < x + SLOT && my >= y && my < y + SLOT) {
                return s;
            }
        }
        return -1;
    }

    // ── Rendu ────────────────────────────────────────────────────────────────

    @Override
    protected void renderScaled(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        TelUi.panel(g, panelX, panelY, panelW, panelH);
        g.fill(panelX, panelY + 2, panelX + panelW, panelY + HEADER_H, TelUi.HEADER);
        TelUi.hairline(g, panelX, panelY + HEADER_H, panelW);
        g.drawString(font, title, panelX + PAD, panelY + 9, TelUi.TITLE, false);
        TelUi.pill(g, font, Component.literal(library.name()), panelX + panelW - PAD, panelY + 7, TelUi.ACCENT);

        renderFileGrid(g, mouseX, mouseY);
        renderWheel(g, mouseX, mouseY);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderFileGrid(GuiGraphics g, int mouseX, int mouseY) {
        TelUi.card(g, leftX, leftY, leftW, leftH);
        TelUi.text(g, font, Component.translatable("screen.customgobo.local_files"), leftX + 6, leftY + 5, TelUi.LABEL);

        if (availableFiles.isEmpty()) {
            Component empty = Component.translatable("screen.customgobo.no_files");
            TelUi.centered(g, font, empty, leftX + leftW / 2, leftY + leftH / 2 - 10, TelUi.SUB);
            Component hint = Component.literal(GoboFileManager.getLocalConfigDir().getName() + "/");
            TelUi.centered(g, font, hint, leftX + leftW / 2, leftY + leftH / 2 + 2, TelUi.LABEL);
        }

        int hovered = tileAt(mouseX, mouseY);
        int start = currentPage * itemsPerPage();
        for (int i = 0; i < itemsPerPage(); i++) {
            int idx = start + i;
            if (idx >= availableFiles.size()) {
                break;
            }
            String file = availableFiles.get(idx);
            int x = gridX + (i % gridCols) * (TILE + TILE_GAP);
            int y = gridY + (i / gridCols) * (TILE + TILE_GAP);
            boolean selected = file.equals(selectedFile);

            g.fill(x, y, x + TILE, y + TILE, idx == hovered ? TelUi.BG_RAISED : TelUi.BG);
            TelUi.outline(g, x, y, TILE, TILE, selected ? TelUi.ACCENT : TelUi.BORDER_SOFT);

            ResourceLocation tex = CustomGoboLoader.getOrCreateCustomGobo(file);
            int thumb = TILE - 20;
            int tx = x + (TILE - thumb) / 2;
            int ty = y + 4;
            if (tex != null) {
                g.blit(tex, tx, ty, 0, 0, thumb, thumb, thumb, thumb);
            } else {
                g.fill(tx, ty, tx + thumb, ty + thumb, TelUi.BG_SUNKEN);
            }

            String name = file.endsWith(".png") ? file.substring(0, file.length() - 4) : file;
            name = TelUi.ellipsize(font, name, TILE - 6);
            g.drawString(font, name, x + (TILE - font.width(name)) / 2, y + TILE - 12,
                    selected ? TelUi.TITLE : TelUi.SUB, false);
        }

        // Compteur de page centre entre les fleches
        Component page = Component.translatable("screen.customgobo.page",
                Integer.toString(currentPage + 1), Integer.toString(totalPages()));
        TelUi.centered(g, font, page, leftX + leftW / 2, leftY + leftH - 6 - 14, TelUi.SUB);
    }

    private void renderWheel(GuiGraphics g, int mouseX, int mouseY) {
        TelUi.card(g, rightX, rightY, rightW, rightH);
        TelUi.text(g, font, Component.translatable("screen.customgobo.wheel"), rightX + 6, rightY + 5, TelUi.LABEL);

        int hovered = slotAt(mouseX, mouseY);
        for (int s = 0; s < library.getSlotCount(); s++) {
            int x = slotsX + (s % slotCols) * (SLOT + SLOT_GAP);
            int y = slotsY + (s / slotCols) * (SLOT + SLOT_GAP);
            boolean selected = s == selectedSlot;
            boolean custom = GlobalGoboManager.getCustomGobo(library, s) != null;

            g.fill(x, y, x + SLOT, y + SLOT, s == hovered ? TelUi.BG_RAISED : TelUi.BG);
            TelUi.outline(g, x, y, SLOT, SLOT, selected ? TelUi.ACCENT : TelUi.BORDER_SOFT);

            ResourceLocation tex = savedTextureForSlot(s);
            if (tex != null) {
                g.blit(tex, x + 3, y + 3, 0, 0, SLOT - 6, SLOT - 6, SLOT - 6, SLOT - 6);
            }
            if (custom) {
                g.fill(x + SLOT - 6, y + 2, x + SLOT - 2, y + 6, TelUi.ACCENT);
            }
        }

        // Apercu du slot choisi
        g.fill(previewX - 2, previewY - 2, previewX + previewSize + 2, previewY + previewSize + 2, TelUi.BORDER_SOFT);
        g.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, 0xFF000000);
        ResourceLocation preview = previewTexture();
        if (preview != null) {
            g.blit(preview, previewX, previewY, 0, 0, previewSize, previewSize, previewSize, previewSize);
        }

        Component slotLabel = Component.translatable("screen.customgobo.slot", Integer.toString(selectedSlot));
        TelUi.centered(g, font, slotLabel, rightX + rightW / 2, previewY + previewSize + 6, TelUi.TEXT);

        Component status;
        int statusColor;
        if (selectedFile == null) {
            String saved = GlobalGoboManager.getCustomGobo(library, selectedSlot);
            status = saved != null
                    ? Component.translatable("screen.customgobo.status_custom", TelUi.ellipsize(font, saved, rightW - 60))
                    : Component.translatable("screen.customgobo.status_original");
            statusColor = TelUi.SUB;
        } else if (selectedFile.isEmpty()) {
            status = Component.translatable("screen.customgobo.status_restore");
            statusColor = TelUi.WARN;
        } else {
            status = Component.translatable("screen.customgobo.status_selected", TelUi.ellipsize(font, selectedFile, rightW - 40));
            statusColor = TelUi.ACCENT;
        }
        TelUi.centered(g, font, status, rightX + rightW / 2, previewY + previewSize + 17, statusColor);
    }

    private ResourceLocation savedTextureForSlot(int slot) {
        String saved = GlobalGoboManager.getCustomGobo(library, slot);
        if (saved != null) {
            ResourceLocation custom = CustomGoboLoader.getOrCreateCustomGobo(saved);
            if (custom != null) {
                return custom;
            }
        }
        return library.getTexture(slot);
    }

    private ResourceLocation previewTexture() {
        if (selectedFile != null) {
            if (selectedFile.isEmpty()) {
                return library.getTexture(selectedSlot);
            }
            return CustomGoboLoader.getOrCreateCustomGobo(selectedFile);
        }
        return savedTextureForSlot(selectedSlot);
    }

    // ── Envoi ────────────────────────────────────────────────────────────────

    private void saveChanges() {
        if (selectedFile == null) {
            return;
        }

        if (selectedFile.isEmpty()) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUtf(library.name());
            buf.writeInt(selectedSlot);
            buf.writeUtf("");
            buf.writeInt(1);
            buf.writeInt(0);
            buf.writeByteArray(new byte[0]);
            NetworkManager.sendToServer(ModNetworking.UPLOAD_GOBO, buf);
            return;
        }

        byte[] imageData = GoboFileManager.readLocalGobo(selectedFile);
        if (imageData.length == 0) {
            return;
        }
        int chunkSize = 20000;
        int totalChunks = (int) Math.ceil((double) imageData.length / chunkSize);
        for (int i = 0; i < totalChunks; i++) {
            int start = i * chunkSize;
            int length = Math.min(chunkSize, imageData.length - start);
            byte[] chunk = new byte[length];
            System.arraycopy(imageData, start, chunk, 0, length);

            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUtf(library.name());
            buf.writeInt(selectedSlot);
            buf.writeUtf(selectedFile);
            buf.writeInt(totalChunks);
            buf.writeInt(i);
            buf.writeByteArray(chunk);
            NetworkManager.sendToServer(ModNetworking.UPLOAD_GOBO, buf);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
