package com.github.dumann089.theatricalextralights.client.gui;

import com.github.dumann089.theatricalextralights.blockentities.LedFacadeBlockEntity;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;
import com.github.dumann089.theatricalextralights.net.SetLedFacadeConfigPacket;
import com.github.dumann089.theatricalextralights.net.SetLedFacadePixelsPacket;
import dev.imabad.theatrical.TheatricalClient;
import dev.imabad.theatrical.util.UUIDUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;

/**
 * Éditeur de façade LED : quadrillage cliquable (clic gauche = dessiner, droit = effacer,
 * molette = zoom, clic-molette = pan) + réglages compacts (résolution, univers/adresse de base,
 * réseau, pinceau). Vue complète par défaut ; adressage compact 4 canaux/pixel (dimmer/R/G/B).
 */
public class LedFacadeScreen extends Screen {

    private static final int MAX_CANVAS_SIZE = 208;
    private static final int MIN_CANVAS_SIZE = 96;
    private static final int CONTROLS_WIDTH = 180;
    private static final int GAP = 12;
    private static final int PADDING = 12;
    private static final int TITLE_H = 16;
    private static final int WIDGET_HEIGHT = 20;
    private static final int LABEL_H = 11;
    private static final int ROW_GAP = 6;
    private static final int COL_GAP = 8;
    private static final int MIN_VIEW = 8;
    private static final int[] BRUSHES = {1, 2, 3, 5};

    private static final int COLOR_PANEL_BG = 0xFFC6C6C6;
    private static final int COLOR_PANEL_BORDER = 0xFF1F1F1F;
    private static final int COLOR_TEXT = 0x404040;
    private static final int COLOR_FOOTPRINT = 0x505050;
    private static final int COLOR_WARNING = 0xB84000;
    private static final int COLOR_CANVAS_BG = 0xFF101010;
    private static final int COLOR_CELL_OFF = 0xFF262626;
    private static final int COLOR_CELL_ON = 0xFFFFFFFF;
    private static final int COLOR_GRID_LINE = 0x40FFFFFF;

    private final LedFacadeBlockEntity blockEntity;
    private final BlockPos pos;

    private int workingResolution;
    private BitSet workingPixels;
    private int workingSmoothing;

    private List<UUID> networkIds = List.of(UUIDUtil.NULL);
    private int currentNetworkIndex;

    private EditBox universeField;
    private EditBox addressField;

    private int panelLeft, panelTop, panelWidth, panelHeight;
    private int canvasX, canvasY, canvasSize, contentHeight;
    private int controlsX;

    // Offsets verticaux (relatifs au haut de la zone de contenu) calculés en layout.
    private int oResLabel, oRes, oIoLabel, oIo, oNetLabel, oNet, oTools, oFoot, oButtons, stackHeight;

    // Viewport (zoom / pan).
    private int viewSize;
    private int panCol, panRow;
    private int brushIndex;

    // Interaction souris.
    private boolean painting;
    private boolean paintValue;
    private boolean panningView;
    private boolean pixelsDirty;

    public LedFacadeScreen(LedFacadeBlockEntity blockEntity, BlockPos pos) {
        super(Component.translatable("block.theatricalextralights.led_facade"));
        this.blockEntity = blockEntity;
        this.pos = pos;
        this.workingResolution = blockEntity.getResolution();
        this.workingPixels = (BitSet) blockEntity.getActivePixels().clone();
        this.workingSmoothing = blockEntity.getSmoothing();
    }

    @Override
    protected void init() {
        super.init();
        if (blockEntity == null) {
            Minecraft.getInstance().setScreen(null);
            return;
        }

        computeControlLayout();

        // Canevas carré, dimensionné pour laisser une marge confortable en hauteur (barre des tâches, etc.).
        int maxByHeight = (height - 90);
        int maxByWidth = (width - 24) - (PADDING * 2 + GAP + CONTROLS_WIDTH);
        canvasSize = Mth.clamp(Math.min(maxByHeight, maxByWidth), MIN_CANVAS_SIZE, MAX_CANVAS_SIZE);

        contentHeight = Math.max(canvasSize, stackHeight);
        panelWidth = PADDING * 2 + canvasSize + GAP + CONTROLS_WIDTH;
        panelHeight = PADDING + TITLE_H + contentHeight + PADDING;
        panelLeft = (width - panelWidth) / 2;
        panelTop = Mth.clamp((height - panelHeight) / 2, 4, Math.max(4, height - panelHeight - 4));
        canvasX = panelLeft + PADDING;
        canvasY = panelTop + PADDING + TITLE_H;
        controlsX = canvasX + canvasSize + GAP;

        resetView();
        setupNetworks();
        buildWidgets();
    }

    /** Calcule les offsets verticaux de la colonne de contrôles + la hauteur totale du bloc. */
    private void computeControlLayout() {
        int y = 0;
        oResLabel = y; y += LABEL_H; oRes = y; y += WIDGET_HEIGHT + ROW_GAP;
        oIoLabel = y;  y += LABEL_H; oIo = y;  y += WIDGET_HEIGHT + ROW_GAP; // univers + adresse (même ligne)
        oNetLabel = y; y += LABEL_H; oNet = y; y += WIDGET_HEIGHT + ROW_GAP;
        oTools = y;    y += WIDGET_HEIGHT + ROW_GAP;                          // pinceau + effacer (même ligne)
        oFoot = y;     y += 12 + ROW_GAP;
        oButtons = y;  y += WIDGET_HEIGHT;                                    // save + annuler
        stackHeight = y;
    }

    private void resetView() {
        viewSize = workingResolution; // vue complète : le quadrillage couvre toute la façade
        panCol = 0;
        panRow = 0;
        clampView();
    }

    private void clampView() {
        viewSize = Mth.clamp(viewSize, Math.min(MIN_VIEW, workingResolution), workingResolution);
        panCol = Mth.clamp(panCol, 0, Math.max(0, workingResolution - viewSize));
        panRow = Mth.clamp(panRow, 0, Math.max(0, workingResolution - viewSize));
    }

    private void setupNetworks() {
        ArrayList<UUID> nets = new ArrayList<>();
        nets.add(UUIDUtil.NULL);
        if (TheatricalClient.getArtNetManager() != null) {
            for (UUID id : TheatricalClient.getArtNetManager().getKnownNetworks().keySet()) {
                if (!nets.contains(id)) {
                    nets.add(id);
                }
            }
        }
        networkIds = nets;
        currentNetworkIndex = Math.max(networkIds.indexOf(blockEntity.getNetworkId()), 0);
    }

    private void buildWidgets() {
        clearWidgets();
        int x = controlsX;
        int w = CONTROLS_WIDTH;
        int half = (w - COL_GAP) / 2;
        int top = canvasY;

        addRenderableWidget(Button.builder(smoothingLabel(), b -> {
            workingSmoothing = (workingSmoothing + 1) % (LedFacadeBlockEntity.MAX_SMOOTHING + 1);
            b.setMessage(smoothingLabel());
        }).bounds(x, top + oRes, w, WIDGET_HEIGHT).build());

        universeField = new EditBox(font, x, top + oIo, half, WIDGET_HEIGHT, Component.translatable("artneti.dmxUniverse"));
        universeField.setFilter(this::isIntegerInput);
        universeField.setValue(Integer.toString(blockEntity.getUniverse()));
        addRenderableWidget(universeField);

        addressField = new EditBox(font, x + half + COL_GAP, top + oIo, half, WIDGET_HEIGHT, Component.translatable("fixture.dmxStart"));
        addressField.setFilter(this::isIntegerInput);
        addressField.setValue(Integer.toString(Math.max(1, blockEntity.getChannelStart())));
        addRenderableWidget(addressField);

        addRenderableWidget(Button.builder(networkLabel(), b -> {
            currentNetworkIndex = (currentNetworkIndex + 1) % networkIds.size();
            b.setMessage(networkLabel());
        }).bounds(x, top + oNet, w, WIDGET_HEIGHT).build());

        addRenderableWidget(Button.builder(brushLabel(), b -> {
            brushIndex = (brushIndex + 1) % BRUSHES.length;
            b.setMessage(brushLabel());
        }).bounds(x, top + oTools, half, WIDGET_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.led_facade.clear"), b -> {
            workingPixels.clear();
            pixelsDirty = true;
        }).bounds(x + half + COL_GAP, top + oTools, half, WIDGET_HEIGHT).build());

        int buttonsY = top + contentHeight - WIDGET_HEIGHT; // ancré en bas de la zone de contenu
        addRenderableWidget(Button.builder(Component.translatable("artneti.save"), b -> {
            commit();
            onClose();
        }).bounds(x, buttonsY, half, WIDGET_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(x + half + COL_GAP, buttonsY, half, WIDGET_HEIGHT).build());
    }

    // ─── Grille / souris ──────────────────────────────────────────────────────

    private float cellPx() {
        return canvasSize / (float) viewSize;
    }

    private int cellIndexAt(double mouseX, double mouseY) {
        if (mouseX < canvasX || mouseX >= canvasX + canvasSize || mouseY < canvasY || mouseY >= canvasY + canvasSize) {
            return -1;
        }
        float cp = cellPx();
        int col = panCol + (int) ((mouseX - canvasX) / cp);
        int row = panRow + (int) ((mouseY - canvasY) / cp);
        if (col < 0 || col >= workingResolution || row < 0 || row >= workingResolution) {
            return -1;
        }
        return row * workingResolution + col;
    }

    private void paintAt(double mouseX, double mouseY, boolean value) {
        int index = cellIndexAt(mouseX, mouseY);
        if (index < 0) {
            return;
        }
        int brush = BRUSHES[brushIndex];
        int cc = index % workingResolution;
        int cr = index / workingResolution;
        int off = (brush - 1) / 2;
        for (int dr = 0; dr < brush; dr++) {
            for (int dc = 0; dc < brush; dc++) {
                int r = cr - off + dr;
                int c = cc - off + dc;
                if (r >= 0 && r < workingResolution && c >= 0 && c < workingResolution) {
                    int idx = r * workingResolution + c;
                    if (workingPixels.get(idx) != value) {
                        workingPixels.set(idx, value);
                        pixelsDirty = true;
                    }
                }
            }
        }
    }

    private boolean inCanvas(double mouseX, double mouseY) {
        return mouseX >= canvasX && mouseX < canvasX + canvasSize
                && mouseY >= canvasY && mouseY < canvasY + canvasSize;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inCanvas(mouseX, mouseY)) {
            if (button == 2) {
                panningView = true;
                return true;
            }
            if (button == 0 || button == 1) {
                paintValue = button == 0;
                painting = true;
                paintAt(mouseX, mouseY, paintValue);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (panningView && button == 2) {
            float cp = cellPx();
            panCol -= (int) Math.round(dragX / cp);
            panRow -= (int) Math.round(dragY / cp);
            clampView();
            return true;
        }
        if (painting && (button == 0 || button == 1)) {
            paintAt(mouseX, mouseY, paintValue);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 2) {
            panningView = false;
        }
        if (button == 0 || button == 1) {
            painting = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (inCanvas(mouseX, mouseY) && delta != 0) {
            float cp = cellPx();
            int cursorCol = panCol + (int) ((mouseX - canvasX) / cp);
            int cursorRow = panRow + (int) ((mouseY - canvasY) / cp);

            int newView = delta > 0 ? (viewSize * 3) / 4 : (viewSize * 4) / 3;
            newView = Mth.clamp(newView, Math.min(MIN_VIEW, workingResolution), workingResolution);
            if (newView == viewSize) {
                return true;
            }
            viewSize = newView;
            float ncp = cellPx();
            panCol = cursorCol - (int) ((mouseX - canvasX) / ncp);
            panRow = cursorRow - (int) ((mouseY - canvasY) / ncp);
            clampView();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && !universeField.isFocused() && !addressField.isFocused()) {
            commit();
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ─── Commit ─────────────────────────────────────────────────────────────

    private void commit() {
        int universe = parseOrDefault(universeField, blockEntity.getUniverse());
        int address = parseOrDefault(addressField, Math.max(1, blockEntity.getChannelStart()));
        ModNetworkHandler.CHANNEL.sendToServer(new SetLedFacadeConfigPacket(
                pos, workingResolution, Math.max(0, universe), Math.max(1, address),
                networkIds.get(currentNetworkIndex), workingSmoothing));
        ModNetworkHandler.CHANNEL.sendToServer(new SetLedFacadePixelsPacket(pos, workingPixels.toByteArray()));
        pixelsDirty = false;
    }

    // ─── Rendu ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        g.fill(panelLeft - 2, panelTop - 2, panelLeft + panelWidth + 2, panelTop + panelHeight + 2, COLOR_PANEL_BORDER);
        g.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, COLOR_PANEL_BG);
        g.drawCenteredString(font, title, panelLeft + panelWidth / 2, panelTop + PADDING, COLOR_TEXT);

        renderCanvas(g);
        renderControlLabels(g);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderCanvas(GuiGraphics g) {
        g.fill(canvasX - 1, canvasY - 1, canvasX + canvasSize + 1, canvasY + canvasSize + 1, COLOR_PANEL_BORDER);
        g.fill(canvasX, canvasY, canvasX + canvasSize, canvasY + canvasSize, COLOR_CANVAS_BG);

        int res = workingResolution;
        int endCol = Math.min(res, panCol + viewSize);
        int endRow = Math.min(res, panRow + viewSize);

        for (int row = panRow; row < endRow; row++) {
            int y0 = canvasY + (row - panRow) * canvasSize / viewSize;
            int y1 = canvasY + (row - panRow + 1) * canvasSize / viewSize;
            int rowBase = row * res;
            for (int col = panCol; col < endCol; col++) {
                boolean on = workingPixels.get(rowBase + col);
                int x0 = canvasX + (col - panCol) * canvasSize / viewSize;
                int x1 = canvasX + (col - panCol + 1) * canvasSize / viewSize;
                g.fill(x0, y0, x1, y1, on ? cellColor(rowBase + col) : COLOR_CELL_OFF);
            }
        }

        int cellPx = canvasSize / viewSize;
        if (cellPx >= 6) {
            for (int i = 1; i < viewSize; i++) {
                int gx = canvasX + i * canvasSize / viewSize;
                int gy = canvasY + i * canvasSize / viewSize;
                g.fill(gx, canvasY, gx + 1, canvasY + canvasSize, COLOR_GRID_LINE);
                g.fill(canvasX, gy, canvasX + canvasSize, gy + 1, COLOR_GRID_LINE);
            }
        }
    }

    /** Couleur d'affichage d'un pixel actif : couleur DMX live si présente, sinon aperçu clair. */
    private int cellColor(int index) {
        int argb = blockEntity.getPixelArgb(index);
        if ((argb & 0xFFFFFF) != 0) {
            return 0xFF000000 | (argb & 0xFFFFFF);
        }
        return COLOR_CELL_ON;
    }

    private void renderControlLabels(GuiGraphics g) {
        int x = controlsX;
        int half = (CONTROLS_WIDTH - COL_GAP) / 2;
        int top = canvasY;
        g.drawString(font, Component.translatable("screen.led_facade.smoothing"), x, top + oResLabel, COLOR_TEXT, false);
        g.drawString(font, Component.translatable("screen.led_facade.universe"), x, top + oIoLabel, COLOR_TEXT, false);
        g.drawString(font, Component.translatable("screen.led_facade.address"), x + half + COL_GAP, top + oIoLabel, COLOR_TEXT, false);
        g.drawString(font, Component.translatable("screen.artnetconfig.network"), x, top + oNetLabel, COLOR_TEXT, false);

        int drawn = workingPixels.cardinality();
        int channels = drawn * LedFacadeBlockEntity.CHANNELS_PER_PIXEL;
        int address = parseOrDefault(addressField, Math.max(1, blockEntity.getChannelStart()));
        int universes = drawn == 0 ? 0
                : ((address - 1) + channels - 1) / LedFacadeBlockEntity.DMX_CHANNELS_PER_UNIVERSE + 1;
        Component footprint = Component.translatable("screen.led_facade.footprint",
                Integer.toString(drawn), Integer.toString(channels), Integer.toString(universes));
        int footY = top + contentHeight - WIDGET_HEIGHT - 14; // juste au-dessus de Save/Annuler
        g.drawString(font, footprint, x, footY, universes > 1 ? COLOR_WARNING : COLOR_FOOTPRINT, false);
    }

    // ─── Utilitaires ───────────────────────────────────────────────────────────

    private Component smoothingLabel() {
        return Component.translatable("screen.led_facade.smoothing." + workingSmoothing);
    }

    private Component brushLabel() {
        return Component.translatable("screen.led_facade.brush", Integer.toString(BRUSHES[brushIndex]));
    }

    private Component networkLabel() {
        UUID id = networkIds.get(currentNetworkIndex);
        if (id.equals(UUIDUtil.NULL)) {
            return Component.literal("—");
        }
        if (TheatricalClient.getArtNetManager() == null) {
            return Component.translatable("screen.artnetconfig.network.unknown");
        }
        String name = TheatricalClient.getArtNetManager().getKnownNetworks().get(id);
        return Component.literal(name != null ? name : "Unknown");
    }

    private boolean isIntegerInput(String value) {
        return value.isEmpty() || value.matches("\\d+");
    }

    private int parseOrDefault(EditBox field, int fallback) {
        try {
            return Integer.parseInt(field.getValue());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
