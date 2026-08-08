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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CustomGoboScreen extends Screen {
    private final Screen parent;
    private final GoboLibrary library;

    private int selectedSlot = 0;
    private final List<String> availableFiles = new ArrayList<>();

    // null = show currently saved | "" = restore original | "logo.png" = new custom texture
    private String selectedFile = null;

    // Pagination variables for the left list
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 5;

    public CustomGoboScreen(Screen parent, HasGobo fixture) {
        super(Component.translatable("screen.extralightsconfig.custom_gobos_title"));
        this.parent = parent;
        this.library = fixture.getGoboLibrary();
    }

    @Override
    protected void init() {
        super.init();
        loadAvailableFiles();
        buildUI(); // Use a separate method to rebuild the UI when changing pages or slots
    }

    private void buildUI() {
        clearWidgets();

        int leftWidth = width / 2;
        int rightCenterX = leftWidth + (leftWidth / 2);
        int centerY = height / 2;

        // ==========================================
        // RIGHT PANEL: FIXTURE SLOTS
        // ==========================================

        // Button: Previous Slot
        addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
            selectedSlot = (selectedSlot - 1 + library.getSlotCount()) % library.getSlotCount();
            selectedFile = null; // Reset temporary selection when changing slots
            buildUI();
        }).bounds(rightCenterX - 60, centerY - 80, 20, 20).build());

        // Current Slot Indicator (Disabled button used just for display)
        Button slotIndicator = Button.builder(Component.literal("Slot " + selectedSlot), btn -> {}).bounds(rightCenterX - 35, centerY - 80, 70, 20).build();
        slotIndicator.active = false;
        addRenderableWidget(slotIndicator);

        // Button: Next Slot
        addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
            selectedSlot = (selectedSlot + 1) % library.getSlotCount();
            selectedFile = null; // Reset temporary selection
            buildUI();
        }).bounds(rightCenterX + 40, centerY - 80, 20, 20).build());

        // Button: Restore Original Gobo
        addRenderableWidget(Button.builder(Component.literal("Restore Original"), btn -> {
            selectedFile = ""; // Empty string means delete/restore
            buildUI();
        }).bounds(rightCenterX - 75, centerY + 35, 150, 20).build());

        // Button: Save
        addRenderableWidget(Button.builder(Component.translatable("artneti.save"), btn -> {
            saveChanges();
        }).bounds(rightCenterX - 75, height - 40, 150, 20).build());

        // Button: Cancel/Back
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), btn -> {
            Minecraft.getInstance().setScreen(parent);
        }).bounds(rightCenterX - 75, height - 15, 150, 20).build());


        // ==========================================
        // LEFT PANEL: GOBO LIST (.PNG)
        // ==========================================

        int totalPages = Math.max(1, (int) Math.ceil((double) availableFiles.size() / ITEMS_PER_PAGE));
        if (currentPage >= totalPages) currentPage = Math.max(0, totalPages - 1);

        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, availableFiles.size());

        int yOffset = 40;
        int leftMargin = 20;
        int listWidth = leftWidth - 40;

        for (int i = startIdx; i < endIdx; i++) {
            String fileName = availableFiles.get(i);

            // Create a wide button, leaving space on the left to draw the thumbnail
            addRenderableWidget(Button.builder(Component.literal("      " + fileName), btn -> {
                selectedFile = fileName;
            }).bounds(leftMargin, yOffset, listWidth, 32).build());

            yOffset += 36;
        }

        // Pagination Controls
        addRenderableWidget(Button.builder(Component.literal("<- Prev"), btn -> {
            if (currentPage > 0) { currentPage--; buildUI(); }
        }).bounds(leftMargin, height - 30, 50, 20).build());

        Button pageIndicator = Button.builder(Component.literal(String.format("Page %d/%d", currentPage + 1, totalPages)), btn -> {}).bounds(leftMargin + 55, height - 30, listWidth - 110, 20).build();
        pageIndicator.active = false;
        addRenderableWidget(pageIndicator);

        addRenderableWidget(Button.builder(Component.literal("Next ->"), btn -> {
            if (currentPage < totalPages - 1) { currentPage++; buildUI(); }
        }).bounds(leftMargin + listWidth - 50, height - 30, 50, 20).build());
    }

    private void loadAvailableFiles() {
        availableFiles.clear();
        File dir = GoboFileManager.getLocalConfigDir();
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
        if (files != null) {
            for (File f : files) {
                availableFiles.add(f.getName());
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int leftWidth = width / 2;
        int leftCenterX = leftWidth / 2;
        int rightCenterX = leftWidth + (leftWidth / 2);
        int centerY = height / 2;

        // Vertical divider line in the middle
        guiGraphics.fill(leftWidth, 10, leftWidth + 1, height - 10, 0x88FFFFFF);

        // Titles
        guiGraphics.drawCenteredString(font, Component.literal("Local Files (.png)"), leftCenterX, 15, 0xFFFFFF);
        guiGraphics.drawCenteredString(font, Component.literal("Library: " + library.name()), rightCenterX, 15, 0xAAAAAA);

        // --- DRAW THUMBNAILS ON THE LEFT LIST ---
        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, availableFiles.size());
        int yOffset = 40;
        int leftMargin = 20;

        for (int i = startIdx; i < endIdx; i++) {
            String fileName = availableFiles.get(i);
            ResourceLocation tex = CustomGoboLoader.getOrCreateCustomGobo(fileName);

            // Draw a green border if this is the currently selected file
            if (fileName.equals(selectedFile)) {
                guiGraphics.fill(leftMargin - 2, yOffset - 2, leftMargin + (leftWidth - 40) + 2, yOffset + 34, 0xFF00FF00); // Outer border
                guiGraphics.fill(leftMargin - 1, yOffset - 1, leftMargin + (leftWidth - 40) + 1, yOffset + 33, 0xFF000000); // Inner background so it doesn't cover the button
            }

            // Draw the 28x28 pixel thumbnail on the left side of the button
            if (tex != null) {
                guiGraphics.blit(tex, leftMargin + 4, yOffset + 2, 0, 0, 28, 28, 28, 28);
            } else {
                // Gray placeholder box if the texture hasn't loaded yet or failed
                guiGraphics.fill(leftMargin + 4, yOffset + 2, leftMargin + 32, yOffset + 30, 0x55FFFFFF);
            }

            yOffset += 36;
        }

        // --- DRAW LARGE PREVIEW ON THE RIGHT ---
        ResourceLocation previewTex = getTextureForSlot(selectedSlot);
        if (previewTex != null) {
            // Dark background box to make it pop
            guiGraphics.fill(rightCenterX - 34, centerY - 42, rightCenterX + 34, centerY + 22, 0x88000000);
            // Render the Gobo in large size (64x64)
            guiGraphics.blit(previewTex, rightCenterX - 32, centerY - 40, 0, 0, 64, 64, 64, 64);
        }

        // Status Text
        String statusText;
        if (selectedFile == null) {
            statusText = "Status: (Current Gobo)";
        } else if (selectedFile.isEmpty()) {
            statusText = "Status: Restore Original";
        } else {
            statusText = "Selection: " + selectedFile;
        }
        guiGraphics.drawCenteredString(font, Component.literal(statusText), rightCenterX, centerY + 25, 0xFFFF00);
    }

    private ResourceLocation getTextureForSlot(int slot) {
        if (selectedFile != null) {
            if (selectedFile.isEmpty()) {
                return library.getTexture(slot); // Show original if "Restore" is selected
            }
            return CustomGoboLoader.getOrCreateCustomGobo(selectedFile);
        }

        String savedCustom = GlobalGoboManager.getCustomGobo(library, slot);
        if (savedCustom != null) {
            return CustomGoboLoader.getOrCreateCustomGobo(savedCustom);
        }

        return library.getTexture(slot);
    }

    private void saveChanges() {
        if (selectedFile == null) return;

        if (selectedFile.isEmpty()) {
            // Handle "Restore Original" (empty file name)
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUtf(library.name());
            buf.writeInt(selectedSlot);
            buf.writeUtf(""); // Empty filename signals restore
            buf.writeInt(1);  // Total chunks
            buf.writeInt(0);  // Chunk index
            buf.writeByteArray(new byte[0]);
            NetworkManager.sendToServer(ModNetworking.UPLOAD_GOBO, buf);
        } else {
            // Handle custom file upload
            byte[] imageData = GoboFileManager.readLocalGobo(selectedFile);
            if (imageData.length > 0) {
                int chunkSize = 20000;
                int totalChunks = (int) Math.ceil((double) imageData.length / chunkSize);

                for (int i = 0; i < totalChunks; i++) {
                    int start = i * chunkSize;
                    int length = Math.min(chunkSize, imageData.length - start);
                    byte[] chunk = new byte[length];
                    System.arraycopy(imageData, start, chunk, 0, length);

                    // Declare buf INSIDE the loop so it is unique for every chunk
                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                    buf.writeUtf(library.name());
                    buf.writeInt(selectedSlot);
                    buf.writeUtf(selectedFile);
                    buf.writeInt(totalChunks);
                    buf.writeInt(i);
                    buf.writeByteArray(chunk);

                    // Now 'buf' is resolved and can be sent
                    NetworkManager.sendToServer(ModNetworking.UPLOAD_GOBO, buf);
                }
            }
        }
    }
}