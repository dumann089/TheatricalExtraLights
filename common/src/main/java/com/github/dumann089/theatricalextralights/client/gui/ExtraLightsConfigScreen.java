package com.github.dumann089.theatricalextralights.client.gui;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasGobo;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasPersonality;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;
import com.github.dumann089.theatricalextralights.net.SetFixturePositionPacket;
import com.github.dumann089.theatricalextralights.net.SetPersonalityPacket;
import com.github.dumann089.theatricalextralights.util.ConfigurationCardHelper;
import com.github.dumann089.theatricalextralights.util.DmxPatchConflictHelper;
import dev.imabad.theatrical.TheatricalClient;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity;
import dev.imabad.theatrical.net.UpdateDMXFixture;
import dev.imabad.theatrical.net.UpdateNetworkId;
import dev.imabad.theatrical.util.UUIDUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Configuration DMX d'un projecteur : panneau sombre en sections (patch, position,
 * reglages), champs et boutons plats. Les sous-classes ajoutent leurs widgets via
 * {@link #buildExtraWidgets(int)} / {@link #renderExtraLabels(GuiGraphics)}.
 */
public class ExtraLightsConfigScreen extends TelScaledScreen {

    protected static final int PANEL_WIDTH = 320;
    protected static final int PANEL_PADDING = 14;
    protected static final int WIDGET_HEIGHT = TelUi.WIDGET_H;
    protected static final int LABEL_GAP = 11;
    protected static final int ROW_GAP = 8;
    protected static final int SECTION_GAP = 14;
    protected static final int HEADER_HEIGHT = 26;
    protected static final int COL_GAP = 10;

    protected static final int COLOR_FOOTPRINT = TelUi.SUB;

    protected final BaseDMXConsumerLightBlockEntity blockEntity;
    protected final BlockPos pos;
    private final boolean showPositionControls;

    private EditBox dmxAddressField;
    private EditBox dmxUniverseField;
    private PanTiltSlider tiltSlider;
    private PanTiltSlider panSlider;
    private TelUi.FlatButton pastePositionButton;
    private final List<TelUi.FlatButton> personalityButtons = new ArrayList<>();
    private TelUi.FlatButton personalityCycleButton;

    private List<DMXPersonality> personalities = List.of();
    private int currentPersonalityIndex;

    private List<UUID> networkIds = List.of(UUIDUtil.NULL);
    private int currentNetworkIndex;

    protected int panelLeft;
    protected int panelTop;
    protected int panelHeight;
    protected int contentLeft;
    protected int contentWidth;

    /** Cadres a dessiner derriere les champs texte (les EditBox sont sans bordure). */
    private final List<FieldFrame> fieldFrames = new ArrayList<>();

    private record FieldFrame(EditBox box, int x, int y, int w, int h) {
    }

    private int patchSectionY;
    private int dmxAddressLabelY;
    private int footprintLabelY;
    private int conflictLabelY;
    private int positionSectionY;
    private int tiltLabelY;
    private int panLabelY;
    private int settingsSectionY;
    private int personalityLabelY;
    private int networkLabelY;

    public ExtraLightsConfigScreen(BaseDMXConsumerLightBlockEntity blockEntity, BlockPos pos, String title) {
        this(blockEntity, pos, title, true);
    }

    public ExtraLightsConfigScreen(BaseDMXConsumerLightBlockEntity blockEntity, BlockPos pos, String title,
                                   boolean showPositionControls) {
        super(Component.translatable(title));
        this.blockEntity = blockEntity;
        this.pos = pos;
        this.showPositionControls = showPositionControls;
    }

    @Override
    protected void init() {
        super.init();

        if (blockEntity == null) {
            Minecraft.getInstance().setScreen(null);
            return;
        }

        setupState();

        // Premiere passe pour mesurer, puis echelle si l'ecran est trop petit, puis centrage.
        resetScale();
        panelLeft = (vw - PANEL_WIDTH) / 2;
        panelTop = 0;
        contentLeft = panelLeft + PANEL_PADDING;
        contentWidth = PANEL_WIDTH - PANEL_PADDING * 2;
        panelHeight = buildWidgets();
        fitToScreen(PANEL_WIDTH, panelHeight);
        panelLeft = (vw - PANEL_WIDTH) / 2;
        contentLeft = panelLeft + PANEL_PADDING;
        panelTop = Math.max(4, (vh - panelHeight) / 2);
        panelHeight = buildWidgets();
    }

    private void setupState() {
        if (blockEntity instanceof HasPersonality hp) {
            personalities = blockEntity.getFixture().getDMXPersonalities();
            if (personalities == null) {
                personalities = List.of();
            }
            currentPersonalityIndex = Mth.clamp(hp.getActivePersonality(), 0, Math.max(personalities.size() - 1, 0));
        } else {
            personalities = List.of();
            currentPersonalityIndex = 0;
        }

        ArrayList<UUID> availableNetworks = new ArrayList<>();
        availableNetworks.add(UUIDUtil.NULL);
        if (TheatricalClient.getArtNetManager() != null) {
            for (UUID networkId : TheatricalClient.getArtNetManager().getKnownNetworks().keySet()) {
                if (!availableNetworks.contains(networkId)) {
                    availableNetworks.add(networkId);
                }
            }
        }
        networkIds = availableNetworks;
        currentNetworkIndex = Math.max(networkIds.indexOf(blockEntity.getNetworkId()), 0);
    }

    // ── Hooks pour les sous-classes ──────────────────────────────────────────

    /** Conserve pour compatibilite ; la hauteur est maintenant mesuree. */
    protected int extraLayoutRows() {
        return 0;
    }

    /** Insere des widgets supplementaires avant les reglages ; retourne le prochain Y. */
    protected int buildExtraWidgets(int y) {
        return y;
    }

    protected void renderExtraLabels(GuiGraphics guiGraphics) {
    }

    protected void commitExtraChanges() {
    }

    /** Cree un champ texte plat et enregistre son cadre. */
    protected EditBox createField(int x, int y, int w, int h, Component label) {
        EditBox box = TelUi.field(font, x, y, w, h, label);
        fieldFrames.add(new FieldFrame(box, x, y, w, h));
        addRenderableWidget(box);
        return box;
    }

    protected void drawSectionLabel(GuiGraphics g, Component label, int y) {
        TelUi.sectionLabel(g, font, label, contentLeft, y, contentWidth);
    }

    protected void drawFieldLabel(GuiGraphics guiGraphics, Component label, int y) {
        TelUi.label(guiGraphics, font, label, contentLeft, y);
    }

    // ── Construction ─────────────────────────────────────────────────────────

    /** Construit tous les widgets a partir de {@link #panelTop} et retourne la hauteur du panneau. */
    private int buildWidgets() {
        clearWidgets();
        fieldFrames.clear();
        personalityButtons.clear();
        personalityCycleButton = null;

        int y = panelTop + HEADER_HEIGHT + PANEL_PADDING;

        // ── Patch ──
        patchSectionY = y;
        y += LABEL_GAP + 3;

        int half = (contentWidth - COL_GAP) / 2;
        dmxAddressLabelY = y;
        y += LABEL_GAP;
        dmxAddressField = createField(contentLeft, y, half, WIDGET_HEIGHT, Component.translatable("fixture.dmxStart"));
        dmxAddressField.setFilter(this::isIntegerInput);
        dmxAddressField.setValue(Integer.toString(blockEntity.getChannelStart()));

        dmxUniverseField = createField(contentLeft + half + COL_GAP, y, half, WIDGET_HEIGHT,
                Component.translatable("artneti.dmxUniverse"));
        dmxUniverseField.setFilter(this::isIntegerInput);
        dmxUniverseField.setValue(Integer.toString(blockEntity.getUniverse()));
        y += WIDGET_HEIGHT + ROW_GAP;

        footprintLabelY = y;
        y += 11;
        conflictLabelY = y;
        y += 11 + SECTION_GAP;

        // ── Position ──
        if (showPositionControls) {
            positionSectionY = y;
            y += LABEL_GAP + 3;

            tiltLabelY = y;
            y += LABEL_GAP;
            tiltSlider = addRenderableWidget(new PanTiltSlider(contentLeft, y, contentWidth,
                    blockEntity.getTilt(), -90, 90, this::applyTilt));
            y += WIDGET_HEIGHT + ROW_GAP;

            panLabelY = y;
            y += LABEL_GAP;
            panSlider = addRenderableWidget(new PanTiltSlider(contentLeft, y, contentWidth,
                    blockEntity.getPan(), -180, 180, this::applyPan));
            y += WIDGET_HEIGHT + ROW_GAP;

            addRenderableWidget(new TelUi.FlatButton(contentLeft, y, half, WIDGET_HEIGHT,
                    Component.translatable("screen.extralightsconfig.copy_position"),
                    TelUi.ButtonStyle.NORMAL, b -> copyPosition()));
            pastePositionButton = addRenderableWidget(new TelUi.FlatButton(contentLeft + half + COL_GAP, y, half,
                    WIDGET_HEIGHT, Component.translatable("screen.extralightsconfig.paste_position"),
                    TelUi.ButtonStyle.NORMAL, b -> pastePosition()));
            pastePositionButton.active = FixturePositionClipboard.hasValue();
            y += WIDGET_HEIGHT + SECTION_GAP;
        } else {
            tiltSlider = null;
            panSlider = null;
            pastePositionButton = null;
        }

        y = buildExtraWidgets(y);

        // ── Reglages ──
        settingsSectionY = y;
        y += LABEL_GAP + 3;

        if (hasPersonalityOptions()) {
            personalityLabelY = y;
            y += LABEL_GAP;
            if (personalities.size() <= 3) {
                // Segments : toutes les personnalites visibles d'un coup.
                int n = personalities.size();
                int segW = (contentWidth - (n - 1) * 4) / n;
                for (int i = 0; i < n; i++) {
                    final int index = i;
                    TelUi.FlatButton seg = new TelUi.FlatButton(contentLeft + i * (segW + 4), y, segW, WIDGET_HEIGHT,
                            Component.literal(personalities.get(i).getDescription()), TelUi.ButtonStyle.GHOST,
                            b -> selectPersonality(index));
                    seg.setSelected(i == currentPersonalityIndex);
                    personalityButtons.add(addRenderableWidget(seg));
                }
            } else {
                personalityCycleButton = addRenderableWidget(new TelUi.FlatButton(contentLeft, y, contentWidth,
                        WIDGET_HEIGHT, getPersonalityValue(), TelUi.ButtonStyle.NORMAL,
                        b -> selectPersonality((currentPersonalityIndex + 1) % personalities.size())));
            }
            y += WIDGET_HEIGHT + ROW_GAP;
        }

        networkLabelY = y;
        y += LABEL_GAP;
        int networkW = blockEntity instanceof HasGobo ? half : contentWidth;
        addRenderableWidget(new TelUi.FlatButton(contentLeft, y, networkW, WIDGET_HEIGHT, getNetworkValue(),
                TelUi.ButtonStyle.NORMAL, b -> {
                    currentNetworkIndex = (currentNetworkIndex + 1) % networkIds.size();
                    b.setMessage(getNetworkValue());
                }));
        if (blockEntity instanceof HasGobo hasGobo) {
            addRenderableWidget(new TelUi.FlatButton(contentLeft + half + COL_GAP, y, half, WIDGET_HEIGHT,
                    Component.translatable("screen.extralightsconfig.custom_gobos"), TelUi.ButtonStyle.NORMAL,
                    b -> Minecraft.getInstance().setScreen(new CustomGoboScreen(this, hasGobo))));
        }
        y += WIDGET_HEIGHT + SECTION_GAP;

        // ── Pied ──
        int buttonW = 96;
        int footerRight = contentLeft + contentWidth;
        addRenderableWidget(new TelUi.FlatButton(footerRight - buttonW * 2 - COL_GAP, y, buttonW, WIDGET_HEIGHT,
                Component.translatable("gui.cancel"), TelUi.ButtonStyle.NORMAL, b -> onClose()));
        addRenderableWidget(new TelUi.FlatButton(footerRight - buttonW, y, buttonW, WIDGET_HEIGHT,
                Component.translatable("artneti.save"), TelUi.ButtonStyle.PRIMARY, b -> {
                    commitChanges();
                    onClose();
                }));
        y += WIDGET_HEIGHT + PANEL_PADDING;

        return y - panelTop;
    }

    private void selectPersonality(int index) {
        currentPersonalityIndex = index;
        for (int i = 0; i < personalityButtons.size(); i++) {
            personalityButtons.get(i).setSelected(i == index);
        }
        if (personalityCycleButton != null) {
            personalityCycleButton.setMessage(getPersonalityValue());
        }
    }

    private boolean hasPersonalityOptions() {
        return personalities.size() > 1;
    }

    private boolean isIntegerInput(String value) {
        return value.isEmpty() || value.matches("\\d+");
    }

    private Component getPersonalityValue() {
        return Component.literal(personalities.get(currentPersonalityIndex).getDescription());
    }

    private Component getNetworkValue() {
        UUID networkId = networkIds.get(currentNetworkIndex);
        if (networkId.equals(UUIDUtil.NULL)) {
            return Component.literal("—");
        }
        if (TheatricalClient.getArtNetManager() == null) {
            return Component.translatable("screen.artnetconfig.network.unknown");
        }
        String name = TheatricalClient.getArtNetManager().getKnownNetworks().get(networkId);
        return Component.literal(name != null ? name : "Unknown");
    }

    private int getSelectedChannelCount() {
        if (hasPersonalityOptions()) {
            return personalities.get(currentPersonalityIndex).getChannelCount();
        }
        return blockEntity.getChannelCount();
    }

    // ── Statuts ──────────────────────────────────────────────────────────────

    private record Status(Component text, boolean warning) {
    }

    private Status getFootprintStatus() {
        int address = parseOrDefault(dmxAddressField, blockEntity.getChannelStart());
        int universe = parseOrDefault(dmxUniverseField, blockEntity.getUniverse());
        int channelCount = getSelectedChannelCount();

        if (channelCount <= 0 || address < 1) {
            return new Status(Component.translatable("screen.extralightsconfig.footprint_invalid"), true);
        }

        int endChannel = address + channelCount - 1;
        ConfigurationCardHelper.DmxPatch resolved =
                ConfigurationCardHelper.resolvePatch(universe, address, channelCount);

        if (resolved.universe() != universe || resolved.address() != address) {
            return new Status(Component.translatable(
                    "screen.extralightsconfig.footprint_overflow",
                    Integer.toString(universe),
                    Integer.toString(address),
                    Integer.toString(resolved.universe()),
                    Integer.toString(resolved.address()),
                    Integer.toString(channelCount)
            ), true);
        }

        return new Status(Component.translatable(
                "screen.extralightsconfig.footprint",
                Integer.toString(universe),
                Integer.toString(address),
                Integer.toString(endChannel),
                Integer.toString(channelCount)
        ), false);
    }

    private UUID getSelectedNetworkId() {
        return networkIds.get(currentNetworkIndex);
    }

    private Status getConflictStatus() {
        int address = parseOrDefault(dmxAddressField, blockEntity.getChannelStart());
        int universe = parseOrDefault(dmxUniverseField, blockEntity.getUniverse());
        int channelCount = getSelectedChannelCount();

        if (channelCount <= 0 || address < 1 || minecraft.level == null) {
            return new Status(null, false);
        }

        List<DmxPatchConflictHelper.DmxConflict> conflicts = DmxPatchConflictHelper.findConflicts(
                minecraft.level, pos, getSelectedNetworkId(), universe, address, channelCount);

        if (conflicts.isEmpty()) {
            return new Status(null, false);
        }

        if (conflicts.size() == 1) {
            DmxPatchConflictHelper.DmxConflict conflict = conflicts.get(0);
            return new Status(Component.translatable(
                    "screen.extralightsconfig.conflict",
                    conflict.fixtureName(),
                    Integer.toString(conflict.startAddress()),
                    Integer.toString(conflict.endAddress())
            ), true);
        }

        DmxPatchConflictHelper.DmxConflict example = conflicts.get(0);
        return new Status(Component.translatable(
                "screen.extralightsconfig.conflicts",
                Integer.toString(conflicts.size()),
                example.fixtureName(),
                Integer.toString(example.startAddress()),
                Integer.toString(example.endAddress())
        ), true);
    }

    // ── Interactions ─────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            commitChanges();
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void applyTilt(int value) {
        blockEntity.setTilt(value);
        sendPositionUpdate();
    }

    private void applyPan(int value) {
        blockEntity.setPan(value);
        sendPositionUpdate();
    }

    private void copyPosition() {
        int pan = panSlider != null ? panSlider.getIntValue() : blockEntity.getPan();
        int tilt = tiltSlider != null ? tiltSlider.getIntValue() : blockEntity.getTilt();
        FixturePositionClipboard.copy(pan, tilt);
        if (pastePositionButton != null) {
            pastePositionButton.active = true;
        }
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.translatable("screen.extralightsconfig.position_copied",
                            Integer.toString(pan), Integer.toString(tilt)), true);
        }
    }

    private void pastePosition() {
        if (!FixturePositionClipboard.hasValue()) {
            return;
        }
        int pan = FixturePositionClipboard.getPan();
        int tilt = FixturePositionClipboard.getTilt();
        blockEntity.setPan(pan);
        blockEntity.setTilt(tilt);
        if (panSlider != null) {
            panSlider.setIntValue(pan);
        }
        if (tiltSlider != null) {
            tiltSlider.setIntValue(tilt);
        }
        sendPositionUpdate();
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.translatable("screen.extralightsconfig.position_pasted",
                            Integer.toString(pan), Integer.toString(tilt)), true);
        }
    }

    private void sendPositionUpdate() {
        ModNetworkHandler.CHANNEL.sendToServer(
                new SetFixturePositionPacket(pos, blockEntity.getTilt(), blockEntity.getPan()));
    }

    private void commitChanges() {
        int dmxAddress = parseOrDefault(dmxAddressField, blockEntity.getChannelStart());
        int dmxUniverse = parseOrDefault(dmxUniverseField, blockEntity.getUniverse());

        new UpdateDMXFixture(pos, Mth.clamp(dmxAddress, 0, 512), Math.max(0, dmxUniverse)).sendToServer();
        new UpdateNetworkId(pos, networkIds.get(currentNetworkIndex)).sendToServer();
        sendPositionUpdate();

        if (hasPersonalityOptions() && blockEntity instanceof HasPersonality) {
            ModNetworkHandler.CHANNEL.sendToServer(new SetPersonalityPacket(pos, currentPersonalityIndex));
        }

        commitExtraChanges();
    }

    protected int parseOrDefault(EditBox field, int fallbackValue) {
        try {
            return Integer.parseInt(field.getValue());
        } catch (NumberFormatException ignored) {
            return fallbackValue;
        }
    }

    // ── Rendu ────────────────────────────────────────────────────────────────

    @Override
    protected void renderScaled(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        TelUi.panel(g, panelLeft, panelTop, PANEL_WIDTH, panelHeight);

        // En-tete : titre + pastille de canaux
        g.fill(panelLeft, panelTop + 2, panelLeft + PANEL_WIDTH, panelTop + HEADER_HEIGHT, TelUi.HEADER);
        TelUi.hairline(g, panelLeft, panelTop + HEADER_HEIGHT, PANEL_WIDTH);
        int titleMaxW = contentWidth - 60;
        String titleText = TelUi.ellipsize(font, title.getString(), titleMaxW);
        g.drawString(font, titleText, contentLeft, panelTop + 9, TelUi.TITLE, false);
        TelUi.pill(g, font, Component.literal(getSelectedChannelCount() + " ch"),
                contentLeft + contentWidth, panelTop + 7, TelUi.ACCENT);

        // Cadres des champs (derriere les EditBox)
        for (FieldFrame frame : fieldFrames) {
            TelUi.fieldFrame(g, frame.box(), frame.x(), frame.y(), frame.w(), frame.h());
        }

        // Patch
        drawSectionLabel(g, Component.translatable("screen.extralightsconfig.section_patch"), patchSectionY);
        int half = (contentWidth - COL_GAP) / 2;
        drawFieldLabel(g, Component.translatable("fixture.dmxStart"), dmxAddressLabelY);
        TelUi.label(g, font, Component.translatable("artneti.dmxUniverse"), contentLeft + half + COL_GAP, dmxAddressLabelY);

        Status footprint = getFootprintStatus();
        TelUi.text(g, font, footprint.text(), contentLeft, footprintLabelY,
                footprint.warning() ? TelUi.WARN : TelUi.SUB);

        Status conflict = getConflictStatus();
        if (conflict.text() != null) {
            TelUi.text(g, font, conflict.text(), contentLeft, conflictLabelY, TelUi.WARN);
        } else {
            TelUi.text(g, font, Component.translatable("screen.extralightsconfig.no_conflict"),
                    contentLeft, conflictLabelY, TelUi.OK);
        }

        // Position
        if (showPositionControls) {
            drawSectionLabel(g, Component.translatable("fixture.position"), positionSectionY);
            drawFieldLabel(g, Component.translatable("fixture.tilt"), tiltLabelY);
            drawFieldLabel(g, Component.translatable("fixture.pan"), panLabelY);
        }

        renderExtraLabels(g);

        // Reglages
        drawSectionLabel(g, Component.translatable("screen.extralightsconfig.section_settings"), settingsSectionY);
        if (hasPersonalityOptions()) {
            drawFieldLabel(g, Component.translatable("fixture.personality"), personalityLabelY);
        }
        drawFieldLabel(g, Component.translatable("screen.artnetconfig.network"), networkLabelY);
        if (blockEntity instanceof HasGobo) {
            TelUi.label(g, font, Component.translatable("screen.extralightsconfig.gobos_label"),
                    contentLeft + half + COL_GAP, networkLabelY);
        }

        renderWidgets(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class PanTiltSlider extends TelUi.FlatSlider {

        private final int minValue;
        private final int maxValue;
        private final java.util.function.IntConsumer onChange;

        private PanTiltSlider(int x, int y, int width, int value, int minValue, int maxValue,
                              java.util.function.IntConsumer onChange) {
            super(x, y, width, WIDGET_HEIGHT, (value - minValue) / (double) (maxValue - minValue));
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.onChange = onChange;
            updateMessage();
        }

        private int getIntValue() {
            return Mth.clamp((int) Math.round(minValue + (value * (maxValue - minValue))), minValue, maxValue);
        }

        private void setIntValue(int newValue) {
            int clamped = Mth.clamp(newValue, minValue, maxValue);
            setRawValue((clamped - minValue) / (double) (maxValue - minValue));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(getIntValue() + "°"));
        }

        @Override
        protected void applyValue() {
            onChange.accept(getIntValue());
        }
    }
}
