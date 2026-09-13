package com.github.dumann089.theatricalextralights.client.gui;

import com.github.dumann089.theatricalextralights.blockentities.FollowspotConsoleBlockEntity;
import com.github.dumann089.theatricalextralights.client.followspot.FollowspotFixtureCameraSession;
import com.github.dumann089.theatricalextralights.client.followspot.FollowspotInputHelper;
import com.github.dumann089.theatricalextralights.net.FollowspotConsoleControlPacket;
import com.github.dumann089.theatricalextralights.net.FollowspotConsolePatchPacket;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;
import com.github.dumann089.theatricalextralights.util.FollowspotAimMapper;
import com.github.dumann089.theatricalextralights.util.FollowspotDmxHelper;
import com.github.dumann089.theatricalextralights.util.FollowspotTargetHelper;
import dev.imabad.theatrical.TheatricalClient;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import dev.imabad.theatrical.util.UUIDUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntConsumer;

/**
 * Pupitre followspot : patch de la machine suivie, mode, niveaux et prise de controle.
 * Meme habillage que les autres ecrans (TelUi), mise a l'echelle automatique.
 */
public class FollowspotConsoleScreen extends TelScaledScreen {

    private static final int PANEL_W = 340;
    private static final int PAD = 14;
    private static final int HEADER_H = 26;
    private static final int LABEL_GAP = 11;
    private static final int ROW_GAP = 8;
    private static final int SECTION_GAP = 14;
    private static final int COL_GAP = 10;
    private static final int WIDGET_H = TelUi.WIDGET_H;
    private static final int LEVEL_LABEL_W = 58;

    private final BlockPos consolePos;
    private final FollowspotConsoleBlockEntity console;

    private EditBox universeField;
    private EditBox addressField;
    private TelUi.FlatButton networkButton;
    private final List<TelUi.FlatButton> modeButtons = new ArrayList<>();
    private TelUi.FlatButton controlButton;

    private LevelSlider intensitySlider;
    private LevelSlider focusSlider;
    private LevelSlider redSlider;
    private LevelSlider greenSlider;
    private LevelSlider blueSlider;
    private final List<LevelSlider> levelSliders = new ArrayList<>();

    private List<UUID> networkIds = List.of(UUIDUtil.NULL);
    private int currentNetworkIndex;

    private int intensity;
    private int red;
    private int green;
    private int blue;
    private int focus;
    private int pan;
    private int tilt;
    /** La console vise seulement ; intensite / RGB / focus restent sur le pupitre ou le logiciel. */
    private boolean panTiltOnly;

    private int panelX;
    private int panelY;
    private int panelH;
    private int contentX;
    private int contentW;

    private int patchSectionY;
    private int patchLabelY;
    private int statusY;
    private int modeSectionY;
    private int modeHintY;
    private int levelsSectionY;
    private final int[] levelRowY = new int[5];
    private int aimSectionY;
    private int aimTextY;

    private final List<FieldFrame> fieldFrames = new ArrayList<>();

    private record FieldFrame(EditBox box, int x, int y, int w, int h) {
    }

    private int controlSendCooldown;
    private BlockPos linkedFixturePos;
    private String linkedFixtureName;
    private int networkRefreshCooldown;

    private boolean moveUpHeld;
    private boolean moveDownHeld;
    private boolean moveLeftHeld;
    private boolean moveRightHeld;

    private UUID syncedNetworkId;
    private int syncedUniverse;
    private int syncedDmxAddress;
    private boolean syncedPanTiltOnly;
    private int patchDraftHash;

    public FollowspotConsoleScreen(FollowspotConsoleBlockEntity console, BlockPos consolePos) {
        super(Component.translatable("screen.followspot_console.title"));
        this.console = console;
        this.consolePos = consolePos;
    }

    // ── Init / layout ────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        loadFromConsole();
        setupNetworks();

        resetScale();
        panelX = (vw - PANEL_W) / 2;
        panelY = 0;
        panelH = buildWidgets();
        fitToScreen(PANEL_W, panelH);
        panelX = (vw - PANEL_W) / 2;
        panelY = Math.max(4, (vh - panelH) / 2);
        panelH = buildWidgets();

        syncedNetworkId = console.getNetworkId();
        syncedUniverse = console.getUniverse();
        syncedDmxAddress = console.getDmxAddress();
        syncedPanTiltOnly = console.isPanTiltOnly();
        patchDraftHash = computePatchDraftHash();
        updateLinkPreview();
        syncControlsFromLinkedFixture();
        refreshModeUi();
    }

    private int buildWidgets() {
        String universeDraft = universeField != null ? universeField.getValue() : Integer.toString(console.getUniverse());
        String addressDraft = addressField != null ? addressField.getValue() : Integer.toString(console.getDmxAddress());

        clearWidgets();
        fieldFrames.clear();
        modeButtons.clear();
        levelSliders.clear();

        contentX = panelX + PAD;
        contentW = PANEL_W - PAD * 2;
        int y = panelY + HEADER_H + PAD;
        int half = (contentW - COL_GAP) / 2;

        // ── Patch ──
        patchSectionY = y;
        y += LABEL_GAP + 3;
        patchLabelY = y;
        y += LABEL_GAP;
        networkButton = addRenderableWidget(new TelUi.FlatButton(contentX, y, contentW, WIDGET_H, getNetworkLabel(),
                TelUi.ButtonStyle.NORMAL, b -> {
                    currentNetworkIndex = (currentNetworkIndex + 1) % networkIds.size();
                    b.setMessage(getNetworkLabel());
                    updateLinkPreview();
                }));
        y += WIDGET_H + ROW_GAP;

        int fieldLabelY = y;
        y += LABEL_GAP;
        universeField = createField(contentX, y, half, WIDGET_H, Component.translatable("artneti.dmxUniverse"));
        universeField.setFilter(v -> v.isEmpty() || v.matches("\\d+"));
        universeField.setValue(universeDraft);
        addressField = createField(contentX + half + COL_GAP, y, half, WIDGET_H, Component.translatable("fixture.dmxStart"));
        addressField.setFilter(v -> v.isEmpty() || v.matches("\\d+"));
        addressField.setValue(addressDraft);
        patchLabelY = fieldLabelY; // le libelle reseau est dessine au-dessus du bouton, ceux des champs ici
        y += WIDGET_H + ROW_GAP;
        statusY = y;
        y += 11 + SECTION_GAP;

        // ── Mode ──
        modeSectionY = y;
        y += LABEL_GAP + 3;
        TelUi.FlatButton full = new TelUi.FlatButton(contentX, y, half, WIDGET_H,
                Component.translatable("screen.followspot_console.mode_full_short"), TelUi.ButtonStyle.GHOST,
                b -> setMode(false));
        TelUi.FlatButton ptOnly = new TelUi.FlatButton(contentX + half + COL_GAP, y, half, WIDGET_H,
                Component.translatable("screen.followspot_console.mode_pan_tilt_short"), TelUi.ButtonStyle.GHOST,
                b -> setMode(true));
        modeButtons.add(addRenderableWidget(full));
        modeButtons.add(addRenderableWidget(ptOnly));
        y += WIDGET_H + 4;
        modeHintY = y;
        y += 11 + SECTION_GAP;

        // ── Niveaux ──
        levelsSectionY = y;
        y += LABEL_GAP + 3;
        int sliderX = contentX + LEVEL_LABEL_W;
        int sliderW = contentW - LEVEL_LABEL_W;
        intensitySlider = addLevel(0, sliderX, y, sliderW, intensity, v -> intensity = v);
        y += WIDGET_H + 5;
        focusSlider = addLevel(1, sliderX, y, sliderW, focus, v -> focus = v);
        y += WIDGET_H + 5;
        redSlider = addLevel(2, sliderX, y, sliderW, red, v -> red = v);
        y += WIDGET_H + 5;
        greenSlider = addLevel(3, sliderX, y, sliderW, green, v -> green = v);
        y += WIDGET_H + 5;
        blueSlider = addLevel(4, sliderX, y, sliderW, blue, v -> blue = v);
        y += WIDGET_H + SECTION_GAP;

        // ── Visee ──
        aimSectionY = y;
        y += LABEL_GAP + 3;
        aimTextY = y;
        y += 11 * 2 + SECTION_GAP;

        // ── Pied ──
        int btnW = (contentW - COL_GAP * 2) / 3;
        addRenderableWidget(new TelUi.FlatButton(contentX, y, btnW, WIDGET_H, Component.translatable("gui.cancel"),
                TelUi.ButtonStyle.NORMAL, b -> onClose()));
        addRenderableWidget(new TelUi.FlatButton(contentX + btnW + COL_GAP, y, btnW, WIDGET_H,
                Component.translatable("screen.followspot_console.link"), TelUi.ButtonStyle.NORMAL, b -> sendPatch()));
        controlButton = addRenderableWidget(new TelUi.FlatButton(contentX + (btnW + COL_GAP) * 2, y, btnW, WIDGET_H,
                Component.translatable("screen.followspot_console.take_control"), TelUi.ButtonStyle.PRIMARY,
                b -> enterFixtureControl()));
        y += WIDGET_H + PAD;

        return y - panelY;
    }

    private EditBox createField(int x, int y, int w, int h, Component label) {
        EditBox box = TelUi.field(font, x, y, w, h, label);
        fieldFrames.add(new FieldFrame(box, x, y, w, h));
        addRenderableWidget(box);
        return box;
    }

    private LevelSlider addLevel(int row, int x, int y, int w, int initial, IntConsumer onChange) {
        levelRowY[row] = y;
        LevelSlider slider = addRenderableWidget(new LevelSlider(x, y, w, initial, onChange));
        levelSliders.add(slider);
        return slider;
    }

    // ── Etat ─────────────────────────────────────────────────────────────────

    private void loadFromConsole() {
        intensity = console.getIntensity();
        red = console.getRed();
        green = console.getGreen();
        blue = console.getBlue();
        focus = console.getFocus();
        pan = console.getPan();
        tilt = console.getTilt();
        panTiltOnly = console.isPanTiltOnly();
    }

    private void setMode(boolean ptOnly) {
        if (panTiltOnly == ptOnly) {
            return;
        }
        panTiltOnly = ptOnly;
        refreshModeUi();
        sendPatch();
    }

    private void refreshModeUi() {
        if (modeButtons.size() == 2) {
            modeButtons.get(0).setSelected(!panTiltOnly);
            modeButtons.get(1).setSelected(panTiltOnly);
        }
        for (LevelSlider slider : levelSliders) {
            slider.active = !panTiltOnly;
        }
        if (controlButton != null) {
            controlButton.active = linkedFixturePos != null;
        }
    }

    private void setupNetworks() {
        ArrayList<UUID> available = new ArrayList<>();
        available.add(UUIDUtil.NULL);
        if (TheatricalClient.getArtNetManager() != null) {
            for (UUID id : TheatricalClient.getArtNetManager().getKnownNetworks().keySet()) {
                if (!available.contains(id)) {
                    available.add(id);
                }
            }
        }
        UUID saved = console.getNetworkId();
        if (FollowspotTargetHelper.isValidNetwork(saved) && !available.contains(saved)) {
            available.add(saved);
        }
        networkIds = available;
        currentNetworkIndex = Math.max(networkIds.indexOf(saved), 0);
    }

    private void ensureNetworkListed(UUID networkId) {
        if (!FollowspotTargetHelper.isValidNetwork(networkId) || networkIds.contains(networkId)) {
            return;
        }
        ArrayList<UUID> next = new ArrayList<>(networkIds);
        next.add(networkId);
        networkIds = next;
    }

    private Component getNetworkLabel() {
        UUID id = networkIds.get(currentNetworkIndex);
        if (id.equals(UUIDUtil.NULL)) {
            return Component.literal("—");
        }
        if (TheatricalClient.getArtNetManager() == null) {
            return Component.translatable("screen.artnetconfig.network.unknown");
        }
        String name = TheatricalClient.getArtNetManager().getKnownNetworks().get(id);
        return Component.literal(name != null ? name : "?");
    }

    private void sendPatch() {
        UUID networkId = networkIds.get(currentNetworkIndex);
        int universe = parseOrDefault(universeField, console.getUniverse());
        int address = parseOrDefault(addressField, console.getDmxAddress());
        if (!FollowspotDmxHelper.isValidDmxAddress(address)) {
            updateLinkPreview();
            return;
        }
        ModNetworkHandler.CHANNEL.sendToServer(new FollowspotConsolePatchPacket(
                consolePos, networkId, universe, address, panTiltOnly));
        updateLinkPreview();
    }

    private void enterFixtureControl() {
        updateLinkPreview();
        if (linkedFixturePos == null) {
            return;
        }
        sendPatch();
        FollowspotFixtureCameraSession.start(console, consolePos, linkedFixturePos,
                intensity, red, green, blue, focus,
                FollowspotDmxHelper.quantizePan(pan), FollowspotDmxHelper.quantizeTilt(tilt));
    }

    private void sendControl() {
        ModNetworkHandler.CHANNEL.sendToServer(new FollowspotConsoleControlPacket(
                consolePos, intensity, red, green, blue, focus, pan, tilt));
        controlSendCooldown = 2;
    }

    private void updateLinkPreview() {
        if (minecraft == null || minecraft.level == null) {
            linkedFixturePos = null;
            linkedFixtureName = null;
            refreshModeUi();
            return;
        }
        Optional<FollowspotTargetHelper.TargetMatch> target = FollowspotTargetHelper.findTarget(
                minecraft.level, networkIds.get(currentNetworkIndex),
                parseOrDefault(universeField, console.getUniverse()),
                parseOrDefault(addressField, console.getDmxAddress()), consolePos);
        linkedFixturePos = target.map(FollowspotTargetHelper.TargetMatch::pos).orElse(null);
        linkedFixtureName = target.map(t -> Component.translatable(t.translationKey()).getString()).orElse(null);
        refreshModeUi();
    }

    private void syncControlsFromLinkedFixture() {
        if (minecraft == null || minecraft.level == null || linkedFixturePos == null) {
            return;
        }
        Optional<FollowspotTargetHelper.TargetMatch> target = FollowspotTargetHelper.findTarget(
                minecraft.level, console.getNetworkId(), console.getUniverse(), console.getDmxAddress(), consolePos);
        if (target.isEmpty()) {
            return;
        }
        BaseLightBlockEntity light = target.get().fixture();
        pan = light.getPan();
        tilt = light.getTilt();
        focus = light.getFocus();
        intensity = (int) light.getIntensity();
        red = light.getRed();
        green = light.getGreen();
        blue = light.getBlue();
        updateSliders();
    }

    private int computePatchDraftHash() {
        return Objects.hash(currentNetworkIndex,
                universeField != null ? universeField.getValue() : "",
                addressField != null ? addressField.getValue() : "");
    }

    private void syncFromConsoleEntityIfNeeded() {
        UUID networkId = console.getNetworkId();
        int universe = console.getUniverse();
        int address = console.getDmxAddress();
        boolean mode = console.isPanTiltOnly();
        if (networkId.equals(syncedNetworkId) && universe == syncedUniverse
                && address == syncedDmxAddress && mode == syncedPanTiltOnly) {
            return;
        }
        syncedNetworkId = networkId;
        syncedUniverse = universe;
        syncedDmxAddress = address;
        syncedPanTiltOnly = mode;
        ensureNetworkListed(networkId);
        currentNetworkIndex = Math.max(networkIds.indexOf(networkId), 0);
        if (networkButton != null) {
            networkButton.setMessage(getNetworkLabel());
        }
        if (universeField != null) {
            universeField.setValue(Integer.toString(universe));
        }
        if (addressField != null) {
            addressField.setValue(Integer.toString(address));
        }
        loadFromConsole();
        refreshModeUi();
        updateLinkPreview();
        syncControlsFromLinkedFixture();
    }

    private void updateSliders() {
        if (intensitySlider == null) {
            return;
        }
        intensitySlider.setValue(intensity);
        focusSlider.setValue(focus);
        redSlider.setValue(red);
        greenSlider.setValue(green);
        blueSlider.setValue(blue);
    }

    private int parseOrDefault(EditBox field, int fallback) {
        try {
            return Integer.parseInt(field.getValue());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ── Tick / clavier ───────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (networkRefreshCooldown > 0) {
            networkRefreshCooldown--;
        } else {
            networkRefreshCooldown = 20;
            refreshNetworksIfNeeded();
        }
        syncFromConsoleEntityIfNeeded();
        int draftHash = computePatchDraftHash();
        if (draftHash != patchDraftHash) {
            patchDraftHash = draftHash;
            updateLinkPreview();
        }
        if (controlSendCooldown > 0) {
            controlSendCooldown--;
        }
        handleMovementKeys();
    }

    private void refreshNetworksIfNeeded() {
        UUID selected = networkIds.get(currentNetworkIndex);
        setupNetworks();
        int idx = networkIds.indexOf(selected);
        if (idx < 0) {
            idx = networkIds.indexOf(console.getNetworkId());
        }
        currentNetworkIndex = Math.max(idx, 0);
        if (networkButton != null) {
            networkButton.setMessage(getNetworkLabel());
        }
    }

    private boolean isMovementHeld() {
        return moveUpHeld || moveDownHeld || moveLeftHeld || moveRightHeld
                || (minecraft != null && (
                FollowspotInputHelper.isKeyDown(minecraft.options.keyUp)
                        || FollowspotInputHelper.isKeyDown(minecraft.options.keyDown)
                        || FollowspotInputHelper.isKeyDown(minecraft.options.keyLeft)
                        || FollowspotInputHelper.isKeyDown(minecraft.options.keyRight)));
    }

    /** ZQSD / WASD dans l'ecran : la tache se deplace dans le sens vu depuis la machine. */
    private void handleMovementKeys() {
        if (minecraft == null || minecraft.level == null || !isMovementHeld()) {
            return;
        }
        setFocused(null);

        BaseLightBlockEntity fixture = resolveLinkedFixture();
        if (fixture == null) {
            updateLinkPreview();
            fixture = resolveLinkedFixture();
        }
        if (fixture == null) {
            return;
        }

        boolean up = moveUpHeld || FollowspotInputHelper.isKeyDown(minecraft.options.keyUp);
        boolean down = moveDownHeld || FollowspotInputHelper.isKeyDown(minecraft.options.keyDown);
        boolean left = moveLeftHeld || FollowspotInputHelper.isKeyDown(minecraft.options.keyLeft);
        boolean right = moveRightHeld || FollowspotInputHelper.isKeyDown(minecraft.options.keyRight);

        double step = FollowspotDmxHelper.PAN_TILT_STEP;
        double dRight = (right ? step : 0) - (left ? step : 0);
        double dUp = (up ? step : 0) - (down ? step : 0);
        if (dRight == 0 && dUp == 0) {
            return;
        }
        float[] delta = FollowspotAimMapper.solveDegrees(fixture, pan, tilt, dRight, dUp);
        int newPan = FollowspotDmxHelper.quantizePan(Math.round(pan + delta[0]));
        int newTilt = FollowspotDmxHelper.quantizeTilt(Math.round(tilt + delta[1]));
        if (newPan != pan || newTilt != tilt) {
            pan = newPan;
            tilt = newTilt;
            if (controlSendCooldown <= 0) {
                sendControl();
            }
        }
    }

    private BaseLightBlockEntity resolveLinkedFixture() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        if (linkedFixturePos != null
                && minecraft.level.getBlockEntity(linkedFixturePos) instanceof BaseLightBlockEntity light) {
            return light;
        }
        return FollowspotTargetHelper.findTarget(minecraft.level, console.getNetworkId(), console.getUniverse(),
                console.getDmxAddress(), consolePos).map(FollowspotTargetHelper.TargetMatch::fixture).orElse(null);
    }

    private boolean updateMovementHeld(int keyCode, int scanCode, boolean down) {
        if (minecraft == null) {
            return false;
        }
        if (minecraft.options.keyUp.matches(keyCode, scanCode)) { moveUpHeld = down; return true; }
        if (minecraft.options.keyDown.matches(keyCode, scanCode)) { moveDownHeld = down; return true; }
        if (minecraft.options.keyLeft.matches(keyCode, scanCode)) { moveLeftHeld = down; return true; }
        if (minecraft.options.keyRight.matches(keyCode, scanCode)) { moveRightHeld = down; return true; }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        if (updateMovementHeld(keyCode, scanCode, true)) {
            setFocused(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (updateMovementHeld(keyCode, scanCode, false)) {
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    // ── Rendu ────────────────────────────────────────────────────────────────

    @Override
    protected void renderScaled(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        TelUi.panel(g, panelX, panelY, PANEL_W, panelH);
        g.fill(panelX, panelY + 2, panelX + PANEL_W, panelY + HEADER_H, TelUi.HEADER);
        TelUi.hairline(g, panelX, panelY + HEADER_H, PANEL_W);
        g.drawString(font, title, contentX, panelY + 9, TelUi.TITLE, false);
        TelUi.pill(g, font, Component.literal("7 ch"), contentX + contentW, panelY + 7, TelUi.ACCENT);

        for (FieldFrame frame : fieldFrames) {
            TelUi.fieldFrame(g, frame.box(), frame.x(), frame.y(), frame.w(), frame.h());
        }

        int half = (contentW - COL_GAP) / 2;

        // Patch
        TelUi.sectionLabel(g, font, Component.translatable("screen.followspot_console.section_patch"), contentX, patchSectionY, contentW);
        TelUi.label(g, font, Component.translatable("screen.artnetconfig.network"), contentX, patchSectionY + LABEL_GAP + 3);
        TelUi.label(g, font, Component.translatable("artneti.dmxUniverse"), contentX, patchLabelY);
        TelUi.label(g, font, Component.translatable("fixture.dmxStart"), contentX + half + COL_GAP, patchLabelY);
        boolean linked = linkedFixturePos != null;
        Component status = getLinkStatus();
        g.fill(contentX + 1, statusY + 2, contentX + 7, statusY + 8, linked ? TelUi.OK : TelUi.WARN);
        TelUi.text(g, font, status, contentX + 12, statusY, linked ? TelUi.OK : TelUi.WARN);

        // Mode
        TelUi.sectionLabel(g, font, Component.translatable("screen.followspot_console.section_mode"), contentX, modeSectionY, contentW);
        TelUi.text(g, font, Component.translatable(panTiltOnly
                ? "screen.followspot_console.mode_hint"
                : "screen.followspot_console.mode_full_hint"), contentX, modeHintY, TelUi.LABEL);

        // Niveaux
        TelUi.sectionLabel(g, font, Component.translatable("screen.followspot_console.section_levels"), contentX, levelsSectionY, contentW);
        String[] keys = {"screen.followspot_console.intensity", "screen.followspot_console.focus",
                "screen.followspot_console.red", "screen.followspot_console.green", "screen.followspot_console.blue"};
        int[] tints = {TelUi.TEXT, TelUi.TEXT, 0xFFE07070, 0xFF78CC80, 0xFF7AA8F0};
        for (int i = 0; i < 5; i++) {
            String label = TelUi.ellipsize(font, Component.translatable(keys[i]).getString(), LEVEL_LABEL_W - 6);
            g.drawString(font, label, contentX, levelRowY[i] + 6, panTiltOnly ? TelUi.LABEL : tints[i], false);
        }

        // Visee
        TelUi.sectionLabel(g, font, Component.translatable("screen.followspot_console.section_aim"), contentX, aimSectionY, contentW);
        Component panTilt = Component.translatable("screen.followspot_console.pan_tilt", Integer.toString(pan), Integer.toString(tilt));
        TelUi.text(g, font, panTilt, contentX, aimTextY, TelUi.TEXT);
        if (linkedFixtureName != null) {
            String name = TelUi.ellipsize(font, linkedFixtureName, contentW / 2);
            g.drawString(font, name, contentX + contentW - font.width(name), aimTextY, TelUi.SUB, false);
        }
        if (minecraft != null) {
            TelUi.text(g, font, Component.translatable("screen.followspot_console.aim_hint",
                    keyLabel(minecraft.options.keyUp), keyLabel(minecraft.options.keyLeft),
                    keyLabel(minecraft.options.keyDown), keyLabel(minecraft.options.keyRight)),
                    contentX, aimTextY + 11, TelUi.LABEL);
        }

        renderWidgets(g, mouseX, mouseY, partialTick);
    }

    private Component getLinkStatus() {
        if (!FollowspotTargetHelper.isValidNetwork(networkIds.get(currentNetworkIndex))) {
            return Component.translatable("screen.followspot_console.no_network");
        }
        int address = parseOrDefault(addressField, 0);
        if (!FollowspotDmxHelper.isValidDmxAddress(address)) {
            return Component.translatable("screen.followspot_console.invalid_address", FollowspotDmxHelper.MAX_DMX_ADDRESS);
        }
        if (linkedFixturePos == null) {
            return Component.translatable("screen.followspot_console.not_found", parseOrDefault(universeField, 0), address);
        }
        return Component.translatable("screen.followspot_console.linked", parseOrDefault(universeField, 0), address);
    }

    private static String keyLabel(net.minecraft.client.KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage().getString();
    }

    @Override
    public void onClose() {
        if (minecraft != null && minecraft.level != null && universeField != null && addressField != null) {
            UUID networkId = networkIds.get(currentNetworkIndex);
            int address = parseOrDefault(addressField, console.getDmxAddress());
            boolean patchChanged = !networkId.equals(console.getNetworkId())
                    || parseOrDefault(universeField, console.getUniverse()) != console.getUniverse()
                    || address != console.getDmxAddress()
                    || panTiltOnly != console.isPanTiltOnly();
            if (patchChanged && FollowspotDmxHelper.isValidDmxAddress(address)) {
                sendPatch();
            }
            sendControl();
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── Curseur de niveau ────────────────────────────────────────────────────

    private class LevelSlider extends TelUi.FlatSlider {
        private final IntConsumer onChange;

        LevelSlider(int x, int y, int w, int initial, IntConsumer onChange) {
            super(x, y, w, WIDGET_H, initial / 255.0);
            this.onChange = onChange;
            updateMessage();
        }

        void setValue(int value) {
            setRawValue(Mth.clamp(value / 255.0, 0.0, 1.0));
        }

        int getIntValue() {
            return Mth.clamp((int) Math.round(getRawValue() * 255.0), 0, 255);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(Math.round(getIntValue() / 2.55f) + "%"));
        }

        @Override
        protected void applyValue() {
            if (panTiltOnly) {
                return;
            }
            onChange.accept(getIntValue());
            updateMessage();
            sendControl();
        }
    }
}
