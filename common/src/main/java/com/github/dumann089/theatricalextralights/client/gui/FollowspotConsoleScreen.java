package com.github.dumann089.theatricalextralights.client.gui;

import com.github.dumann089.theatricalextralights.blockentities.FollowspotConsoleBlockEntity;
import com.github.dumann089.theatricalextralights.client.followspot.FollowspotFixtureCameraSession;
import com.github.dumann089.theatricalextralights.client.followspot.FollowspotInputHelper;
import com.github.dumann089.theatricalextralights.net.FollowspotConsoleControlPacket;
import com.github.dumann089.theatricalextralights.net.FollowspotConsolePatchPacket;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;
import com.github.dumann089.theatricalextralights.util.FollowspotDmxHelper;
import com.github.dumann089.theatricalextralights.util.FollowspotOrientationHelper;
import com.github.dumann089.theatricalextralights.util.FollowspotTargetHelper;
import dev.imabad.theatrical.TheatricalClient;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import dev.imabad.theatrical.util.UUIDUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class FollowspotConsoleScreen extends Screen {

    private static final int PANEL_W = 400;
    private static final int PANEL_H = 368;
    private static final int PAD = 14;
    private static final int ROW_H = 18;
    private static final int GAP = 6;
    private static final int LABEL_COL = 72;
    private static final int HEADER_H = 38;

    private static final int BG = 0xFF222228;
    private static final int BORDER = 0xFF08080C;
    private static final int HEADER = 0xFF3A3A44;
    private static final int TITLE = 0xFFF4F4F8;
    private static final int SUB = 0xFF9A9AA8;
    private static final int LABEL = 0xFF80808C;
    private static final int TEXT = 0xFFE4E4EA;
    private static final int ACCENT = 0xFF6AAEF0;
    private static final int WARN = 0xFFEA9468;

    private final BlockPos consolePos;
    private final FollowspotConsoleBlockEntity console;

    private EditBox universeField;
    private EditBox addressField;
    private Button networkButton;
    private Button modeButton;

    private ValueSlider focusSlider;
    private ValueSlider redSlider;
    private ValueSlider greenSlider;
    private ValueSlider blueSlider;
    private ValueSlider intensitySlider;

    private List<UUID> networkIds = List.of(UUIDUtil.NULL);
    private int currentNetworkIndex;

    private int intensity;
    private int red;
    private int green;
    private int blue;
    private int focus;
    private int pan;
    private int tilt;
    /** Console aims only; intensity / RGB / focus stay on the desk / Art-Net. */
    private boolean panTiltOnly;

    private int panelX;
    private int panelY;
    private int contentX;
    private int contentW;
    private int patchFieldsY;
    private int modeButtonY;
    private int slidersY;
    private int buttonsY;

    private int controlSendCooldown;
    private BlockPos linkedFixturePos;
    private int networkRefreshCooldown;

    /** Held via keyPressed/keyReleased — more reliable than polling while a Screen is open. */
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

    @Override
    protected void init() {
        loadFromConsole();
        setupNetworks();

        panelX = (width - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2;
        contentX = panelX + PAD;
        contentW = PANEL_W - PAD * 2;

        patchFieldsY = panelY + HEADER_H + 34;
        modeButtonY = patchFieldsY + ROW_H + 22;
        slidersY = panelY + HEADER_H + 150;
        buttonsY = panelY + PANEL_H - PAD - ROW_H;

        int netW = 98;
        int fieldW = 50;
        int uniX = contentX + netW + 10;
        int addrX = uniX + fieldW + 12;

        networkButton = addRenderableWidget(Button.builder(getNetworkLabel(), b -> {
            currentNetworkIndex = (currentNetworkIndex + 1) % networkIds.size();
            b.setMessage(getNetworkLabel());
            updateLinkPreview();
        }).bounds(contentX, patchFieldsY, netW, ROW_H).build());

        universeField = new EditBox(font, uniX, patchFieldsY, fieldW, ROW_H, Component.literal("U"));
        universeField.setFilter(v -> v.isEmpty() || v.matches("\\d+"));
        universeField.setValue(Integer.toString(console.getUniverse()));
        addRenderableWidget(universeField);

        addressField = new EditBox(font, addrX, patchFieldsY, fieldW, ROW_H, Component.literal("A"));
        addressField.setFilter(v -> v.isEmpty() || v.matches("\\d+"));
        addressField.setValue(Integer.toString(console.getDmxAddress()));
        addRenderableWidget(addressField);

        modeButton = addRenderableWidget(Button.builder(getModeLabel(), b -> {
            panTiltOnly = !panTiltOnly;
            refreshModeUi();
            sendPatch();
        }).bounds(contentX, modeButtonY, contentW, ROW_H).build());

        int sliderW = contentW - LABEL_COL - 30;
        int sx = contentX + LABEL_COL;
        int y = slidersY;
        focusSlider = addSlider(sx, y, sliderW, focus, v -> focus = v);
        y += ROW_H + GAP;
        redSlider = addSlider(sx, y, sliderW, red, v -> red = v);
        y += ROW_H + GAP;
        greenSlider = addSlider(sx, y, sliderW, green, v -> green = v);
        y += ROW_H + GAP;
        blueSlider = addSlider(sx, y, sliderW, blue, v -> blue = v);
        y += ROW_H + GAP;
        intensitySlider = addSlider(sx, y, sliderW, intensity, v -> intensity = v);

        int btnW = (contentW - 12) / 3;
        addRenderableWidget(Button.builder(Component.translatable("screen.followspot_console.link"), b -> sendPatch())
                .bounds(contentX, buttonsY, btnW, ROW_H).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.followspot_console.control"), b -> enterFixtureControl())
                .bounds(contentX + btnW + 6, buttonsY, btnW, ROW_H).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(contentX + (btnW + 6) * 2, buttonsY, btnW, ROW_H).build());

        syncedNetworkId = console.getNetworkId();
        syncedUniverse = console.getUniverse();
        syncedDmxAddress = console.getDmxAddress();
        syncedPanTiltOnly = console.isPanTiltOnly();
        patchDraftHash = computePatchDraftHash();
        updateLinkPreview();
        syncControlsFromLinkedFixture();
        refreshModeUi();
    }

    private ValueSlider addSlider(int x, int y, int w, int initial, java.util.function.IntConsumer onChange) {
        return addRenderableWidget(new ValueSlider(x, y, w, initial, onChange));
    }

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

    private Component getModeLabel() {
        return Component.translatable(panTiltOnly
                ? "screen.followspot_console.mode_pan_tilt"
                : "screen.followspot_console.mode_full");
    }

    private void refreshModeUi() {
        if (modeButton != null) {
            modeButton.setMessage(getModeLabel());
        }
        boolean full = !panTiltOnly;
        if (focusSlider != null) {
            focusSlider.active = full;
            redSlider.active = full;
            greenSlider.active = full;
            blueSlider.active = full;
            intensitySlider.active = full;
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
        // Keep the console's saved network even if ArtNet hasn't advertised it yet —
        // otherwise indexOf fails, UI resets to "—", and WASD pan/tilt never link.
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
        if (name == null) {
            return Component.literal("?");
        }
        return Component.literal(name.length() > 11 ? name.substring(0, 10) + "…" : name);
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
        FollowspotFixtureCameraSession.start(
                console, consolePos, linkedFixturePos,
                intensity, red, green, blue, focus,
                FollowspotDmxHelper.quantizePan(pan),
                FollowspotDmxHelper.quantizeTilt(tilt)
        );
    }

    private void sendControl() {
        ModNetworkHandler.CHANNEL.sendToServer(new FollowspotConsoleControlPacket(
                consolePos, intensity, red, green, blue, focus, pan, tilt
        ));
        controlSendCooldown = 2;
    }

    private void updateLinkPreview() {
        if (minecraft == null || minecraft.level == null) {
            linkedFixturePos = null;
            return;
        }
        Optional<FollowspotTargetHelper.TargetMatch> target = FollowspotTargetHelper.findTarget(
                minecraft.level,
                networkIds.get(currentNetworkIndex),
                parseOrDefault(universeField, console.getUniverse()),
                parseOrDefault(addressField, console.getDmxAddress()),
                consolePos
        );
        linkedFixturePos = target.map(FollowspotTargetHelper.TargetMatch::pos).orElse(null);
    }

    private void syncControlsFromLinkedFixture() {
        if (minecraft == null || minecraft.level == null || linkedFixturePos == null) {
            return;
        }
        Optional<FollowspotTargetHelper.TargetMatch> target = FollowspotTargetHelper.findTarget(
                minecraft.level,
                console.getNetworkId(),
                console.getUniverse(),
                console.getDmxAddress(),
                consolePos
        );
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
        return Objects.hash(
                currentNetworkIndex,
                universeField != null ? universeField.getValue() : "",
                addressField != null ? addressField.getValue() : ""
        );
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
        if (focusSlider == null) {
            return;
        }
        focusSlider.setValue(focus);
        redSlider.setValue(red);
        greenSlider.setValue(green);
        blueSlider.setValue(blue);
        intensitySlider.setValue(intensity);
    }

    private int parseOrDefault(EditBox field, int fallback) {
        try {
            return Integer.parseInt(field.getValue());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

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
        int before = networkIds.size();
        setupNetworks();
        // Prefer keeping the user's current selection when still present
        int idx = networkIds.indexOf(selected);
        if (idx < 0) {
            idx = networkIds.indexOf(console.getNetworkId());
        }
        currentNetworkIndex = Math.max(idx, 0);
        if (networkButton != null && (before != networkIds.size() || idx >= 0)) {
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

    private void handleMovementKeys() {
        if (minecraft == null || minecraft.level == null || !isMovementHeld()) {
            return;
        }
        // Don't let universe/address EditBoxes swallow WASD
        setFocused(null);

        BaseLightBlockEntity fixture = resolveLinkedFixture();
        if (fixture == null && linkedFixturePos == null) {
            // Still allow sending if console already has a valid server-side patch
            updateLinkPreview();
            fixture = resolveLinkedFixture();
        }
        FollowspotOrientationHelper.InputRemap remap = fixture != null
                ? FollowspotOrientationHelper.computeInputRemap(fixture, pan, tilt)
                : new FollowspotOrientationHelper.InputRemap(1f, 1f, 1f, 1f);

        boolean up = moveUpHeld || FollowspotInputHelper.isKeyDown(minecraft.options.keyUp);
        boolean down = moveDownHeld || FollowspotInputHelper.isKeyDown(minecraft.options.keyDown);
        boolean left = moveLeftHeld || FollowspotInputHelper.isKeyDown(minecraft.options.keyLeft);
        boolean right = moveRightHeld || FollowspotInputHelper.isKeyDown(minecraft.options.keyRight);

        boolean changed = false;
        if (up) {
            tilt = FollowspotDmxHelper.quantizeTilt(tilt + (int) (remap.tiltUp() * FollowspotDmxHelper.PAN_TILT_STEP));
            changed = true;
        }
        if (down) {
            tilt = FollowspotDmxHelper.quantizeTilt(tilt + (int) (remap.tiltDown() * FollowspotDmxHelper.PAN_TILT_STEP));
            changed = true;
        }
        if (left) {
            pan = FollowspotDmxHelper.quantizePan(pan + (int) (remap.panLeft() * FollowspotDmxHelper.PAN_TILT_STEP));
            changed = true;
        }
        if (right) {
            pan = FollowspotDmxHelper.quantizePan(pan + (int) (remap.panRight() * FollowspotDmxHelper.PAN_TILT_STEP));
            changed = true;
        }
        if (changed && controlSendCooldown <= 0) {
            sendControl();
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
        // Fallback: server-synced console patch (draft UI may briefly show "—")
        return FollowspotTargetHelper.findTarget(
                minecraft.level,
                console.getNetworkId(),
                console.getUniverse(),
                console.getDmxAddress(),
                consolePos
        ).map(FollowspotTargetHelper.TargetMatch::fixture).orElse(null);
    }

    private boolean updateMovementHeld(int keyCode, int scanCode, boolean down) {
        if (minecraft == null) {
            return false;
        }
        if (minecraft.options.keyUp.matches(keyCode, scanCode)) {
            moveUpHeld = down;
            return true;
        }
        if (minecraft.options.keyDown.matches(keyCode, scanCode)) {
            moveDownHeld = down;
            return true;
        }
        if (minecraft.options.keyLeft.matches(keyCode, scanCode)) {
            moveLeftHeld = down;
            return true;
        }
        if (minecraft.options.keyRight.matches(keyCode, scanCode)) {
            moveRightHeld = down;
            return true;
        }
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

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        renderBackground(g);

        g.fill(panelX - 1, panelY - 1, panelX + PANEL_W + 1, panelY + PANEL_H + 1, BORDER);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, BG);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + HEADER_H, HEADER);

        g.drawCenteredString(font, title, panelX + PANEL_W / 2, panelY + 9, TITLE);
        g.drawCenteredString(font, Component.translatable("screen.followspot_console.subtitle"),
                panelX + PANEL_W / 2, panelY + 22, SUB);

        int netW = 98;
        int fieldW = 50;
        int uniX = contentX + netW + 10;
        int addrX = uniX + fieldW + 12;

        g.drawString(font, Component.translatable("screen.followspot_console.section_patch"),
                contentX, panelY + HEADER_H + 8, LABEL, false);
        g.drawString(font, Component.translatable("screen.artnetconfig.network"), contentX, patchFieldsY - 11, SUB, false);
        g.drawString(font, Component.translatable("artneti.dmxUniverse"), uniX, patchFieldsY - 11, SUB, false);
        g.drawString(font, Component.translatable("fixture.dmxStart"), addrX, patchFieldsY - 11, SUB, false);

        int statusColor = linkedFixturePos != null ? ACCENT : WARN;
        g.drawString(font, getLinkStatus(), contentX, patchFieldsY + ROW_H + 4, statusColor, false);

        if (panTiltOnly) {
            g.drawString(font, Component.translatable("screen.followspot_console.mode_hint"),
                    contentX, modeButtonY + ROW_H + 3, SUB, false);
        }

        if (minecraft != null && linkedFixturePos != null) {
            int infoY = panTiltOnly ? modeButtonY + ROW_H + 14 : modeButtonY + ROW_H + 4;
            g.drawString(font, Component.translatable("screen.followspot_console.pan_tilt",
                    Integer.toString(pan), Integer.toString(tilt)),
                    contentX, infoY, TEXT, false);
            g.drawString(font, Component.translatable("screen.followspot_console.movement_hint",
                            keyLabel(minecraft.options.keyUp), keyLabel(minecraft.options.keyLeft),
                            keyLabel(minecraft.options.keyDown), keyLabel(minecraft.options.keyRight)),
                    contentX, infoY + 12, SUB, false);
        }

        g.drawString(font, Component.translatable("screen.followspot_console.section_control"),
                contentX, slidersY - 11, LABEL, false);

        drawSliderRow(g, "screen.followspot_console.focus", slidersY, focusSlider);
        drawSliderRow(g, "screen.followspot_console.red", slidersY + ROW_H + GAP, redSlider);
        drawSliderRow(g, "screen.followspot_console.green", slidersY + (ROW_H + GAP) * 2, greenSlider);
        drawSliderRow(g, "screen.followspot_console.blue", slidersY + (ROW_H + GAP) * 3, blueSlider);
        drawSliderRow(g, "screen.followspot_console.intensity", slidersY + (ROW_H + GAP) * 4, intensitySlider);

        super.render(g, mouseX, mouseY, pt);
    }

    private void drawSliderRow(GuiGraphics g, String labelKey, int y, ValueSlider slider) {
        Component label = Component.translatable(labelKey);
        int maxLabelW = LABEL_COL - 4;
        String labelText = label.getString();
        if (font.width(labelText) > maxLabelW) {
            while (labelText.length() > 3 && font.width(labelText + "…") > maxLabelW) {
                labelText = labelText.substring(0, labelText.length() - 1);
            }
            labelText = labelText + "…";
        }
        g.drawString(font, labelText, contentX, y + 5, TEXT, false);
        if (slider != null) {
            String value = Integer.toString(slider.getIntValue());
            g.drawString(font, value, contentX + contentW - font.width(value), y + 5, SUB, false);
        }
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
            return Component.translatable("screen.followspot_console.not_found",
                    parseOrDefault(universeField, 0), address);
        }
        return Component.translatable("screen.followspot_console.linked",
                parseOrDefault(universeField, 0), address);
    }

    private static String keyLabel(net.minecraft.client.KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage().getString();
    }

    @Override
    public void onClose() {
        // Persist patch + last control values even if the player hits Cancel / Esc
        if (minecraft != null && minecraft.level != null
                && universeField != null && addressField != null) {
            UUID networkId = networkIds.get(currentNetworkIndex);
            int address = parseOrDefault(addressField, console.getDmxAddress());
            boolean patchChanged = !networkId.equals(console.getNetworkId())
                    || parseOrDefault(universeField, console.getUniverse()) != console.getUniverse()
                    || address != console.getDmxAddress()
                    || panTiltOnly != console.isPanTiltOnly();
            // Save whenever address is valid; NULL network is allowed (clears link)
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

    private class ValueSlider extends AbstractSliderButton {
        private final java.util.function.IntConsumer onChange;

        ValueSlider(int x, int y, int w, int initial, java.util.function.IntConsumer onChange) {
            super(x, y, w, ROW_H, Component.empty(), initial / 255.0);
            this.onChange = onChange;
        }

        void setValue(int value) {
            this.value = Mth.clamp(value / 255.0, 0.0, 1.0);
        }

        int getIntValue() {
            return Mth.clamp((int) Math.round(value * 255.0), 0, 255);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.empty());
        }

        @Override
        protected void applyValue() {
            if (panTiltOnly) {
                return;
            }
            onChange.accept(getIntValue());
            sendControl();
        }
    }
}
