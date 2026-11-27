package com.github.dumann089.theatricalextralights.client.gui;

import com.github.dumann089.theatricalextralights.blockentities.JumbotronBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.VideoSwitcherBlockEntity;
import com.github.dumann089.theatricalextralights.net.JumbotronConfigPacket;
import com.github.dumann089.theatricalextralights.net.ModNetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/** Configuração do jumbotron: tamanho da tela, qualidade da imagem e qual mesa de corte ele segue. */
public class JumbotronScreen extends Screen {

    private final BlockPos pos;
    private int tvWidth;
    private int tvHeight;
    private int quality;
    private BlockPos link;

    public JumbotronScreen(BlockPos pos, JumbotronBlockEntity jumbotron) {
        super(Component.translatable("screen.theatricalextralights.jumbotron.title"));
        this.pos = pos;
        this.tvWidth = jumbotron.getWidth();
        this.tvHeight = jumbotron.getHeight();
        this.quality = jumbotron.getQuality();
        this.link = jumbotron.getLinkedSwitcher();
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int w = 220;
        int left = cx - w / 2;
        int y = this.height / 2 - 80;

        addRenderableWidget(new IntSlider(left, y, w, 20, 1, JumbotronBlockEntity.MAX_WIDTH, tvWidth,
                v -> Component.translatable("screen.theatricalextralights.jumbotron.width", v), v -> tvWidth = v));
        y += 24;
        addRenderableWidget(new IntSlider(left, y, w, 20, 1, JumbotronBlockEntity.MAX_HEIGHT, tvHeight,
                v -> Component.translatable("screen.theatricalextralights.jumbotron.height", v), v -> tvHeight = v));
        y += 24;

        // Atalhos de proporção a partir da largura atual.
        int third = (w - 8) / 3;
        addRenderableWidget(Button.builder(Component.literal("16:9"), b -> preset(16, 9)).bounds(left, y, third, 20).build());
        addRenderableWidget(Button.builder(Component.literal("21:9"), b -> preset(21, 9)).bounds(left + third + 4, y, third, 20).build());
        addRenderableWidget(Button.builder(Component.literal("4:3"), b -> preset(4, 3)).bounds(left + 2 * (third + 4), y, third, 20).build());
        y += 28;

        addRenderableWidget(Button.builder(qualityLabel(), b -> {
            quality = (quality + 1) % JumbotronBlockEntity.QUALITY_COUNT;
            b.setMessage(qualityLabel());
        }).bounds(left, y, w, 20).build());
        y += 24;

        addRenderableWidget(Button.builder(linkLabel(), b -> {
            link = link == null ? nearestSwitcher() : null;
            b.setMessage(linkLabel());
        }).bounds(left, y, w, 20).build());
        y += 32;

        int half = (w - 4) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
            ModNetworkHandler.CHANNEL.sendToServer(new JumbotronConfigPacket(pos, tvWidth, tvHeight, quality, link));
            onClose();
        }).bounds(left, y, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(left + half + 4, y, half, 20).build());
    }

    /** Ajusta a altura para a proporção, mantendo a largura (e respeitando os limites). */
    private void preset(int ratioW, int ratioH) {
        tvHeight = Mth.clamp(Math.round(tvWidth * (float) ratioH / ratioW), 1, JumbotronBlockEntity.MAX_HEIGHT);
        rebuildWidgets();
    }

    private Component qualityLabel() {
        return Component.translatable("screen.theatricalextralights.jumbotron.quality",
                Component.translatable("screen.theatricalextralights.jumbotron.quality." + quality));
    }

    private Component linkLabel() {
        return link == null
                ? Component.translatable("screen.theatricalextralights.jumbotron.link.auto")
                : Component.translatable("screen.theatricalextralights.jumbotron.link.fixed",
                        link.getX(), link.getY(), link.getZ());
    }

    /** A mesa de corte carregada mais próxima deste jumbotron (null se não houver). */
    private BlockPos nearestSwitcher() {
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos best = null;
        double bestDist = JumbotronBlockEntity.AUTO_LINK_RANGE * JumbotronBlockEntity.AUTO_LINK_RANGE;
        for (VideoSwitcherBlockEntity desk : VideoSwitcherBlockEntity.all()) {
            if (desk.getLevel() != minecraft.level || desk.isRemoved()) {
                continue;
            }
            double dist = desk.getBlockPos().distSqr(pos);
            if (dist <= bestDist) {
                bestDist = dist;
                best = desk.getBlockPos();
            }
        }
        return best;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, this.width / 2, this.height / 2 - 104, 0xFFFFFF);
        int area = tvWidth * tvHeight;
        Component info = Component.translatable("screen.theatricalextralights.jumbotron.area", area,
                JumbotronBlockEntity.MAX_AREA);
        graphics.drawCenteredString(font, info, this.width / 2, this.height / 2 - 92, area > JumbotronBlockEntity.MAX_AREA ? 0xFF6666 : 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Slider de inteiros (vanilla não tem um pronto). */
    private static final class IntSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final IntFunction<Component> label;
        private final IntConsumer onChange;

        IntSlider(int x, int y, int w, int h, int min, int max, int value, IntFunction<Component> label, IntConsumer onChange) {
            super(x, y, w, h, label.apply(value), (double) (value - min) / (max - min));
            this.min = min;
            this.max = max;
            this.label = label;
            this.onChange = onChange;
        }

        private int current() {
            return min + (int) Math.round(this.value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(label.apply(current()));
        }

        @Override
        protected void applyValue() {
            onChange.accept(current());
        }
    }
}
