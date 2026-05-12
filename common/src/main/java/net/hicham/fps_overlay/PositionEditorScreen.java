package net.hicham.fps_overlay;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PositionEditorScreen extends Screen {
    private static final int COLOR_PANEL_WIDTH = 212;
    private static final int COLOR_PANEL_HEIGHT = 182;
    private final Screen parent;
    private final ModConfig config;

    private boolean dragging;
    private double previewScreenX;
    private double previewScreenY;
    private ColorTarget selectedColorTarget = ColorTarget.BACKGROUND;
    private ChannelSlider redSlider;
    private ChannelSlider greenSlider;
    private ChannelSlider blueSlider;
    private ChannelSlider alphaSlider;

    public PositionEditorScreen(Screen parent, ModConfig config) {
        super(Component.translatable("screen.fps_overlay.position_editor"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        int footerY = this.height - 28;
        int colorPanelX = this.width - COLOR_PANEL_WIDTH - 20;
        int colorContentX = colorPanelX + 10;
        int colorWidth = COLOR_PANEL_WIDTH - 20;
        int colorY = 34;

        addRenderableWidget(CycleButton.<ModConfig.OverlayPosition>builder(ModConfig.OverlayPosition::getDisplayText,
                        config.appearance.position)
                .withValues(ModConfig.OverlayPosition.values())
                .create(20, 20, 160, 20, Component.translatable("option.fps_overlay.position"),
                        (button, value) -> config.appearance.position = value));

        addRenderableWidget(Button.builder(Component.translatable("button.fps_overlay.reset_offset"), button -> {
            config.appearance.xOffset = 0;
            config.appearance.yOffset = 0;
        }).bounds(190, 20, 110, 20).tooltip(Tooltip.create(Component.translatable("tooltip.fps_overlay.resetOffset")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("-"), button ->
        {
            config.appearance.hudScale = Math.max(0.2f, config.appearance.hudScale - 0.05f);
        })
                .bounds(310, 20, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button ->
        {
            config.appearance.hudScale = Math.min(1.5f, config.appearance.hudScale + 0.05f);
        })
                .bounds(428, 20, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("button.fps_overlay.reset_scale"), button ->
        {
            config.appearance.hudScale = new ModConfig().appearance.hudScale;
        })
                .bounds(456, 20, 90, 20).build());

        addRenderableWidget(CycleButton.<ColorTarget>builder(ColorTarget::getDisplayText, selectedColorTarget)
                .withValues(ColorTarget.values())
                .create(colorContentX, colorY, colorWidth, 20, Component.translatable("option.fps_overlay.previewColorTarget"),
                        (button, value) -> {
                            selectedColorTarget = value;
                            syncColorSliders();
                        }));

        redSlider = addRenderableWidget(new ChannelSlider(colorContentX, colorY + 30, colorWidth, Channel.RED));
        greenSlider = addRenderableWidget(new ChannelSlider(colorContentX, colorY + 54, colorWidth, Channel.GREEN));
        blueSlider = addRenderableWidget(new ChannelSlider(colorContentX, colorY + 78, colorWidth, Channel.BLUE));
        alphaSlider = addRenderableWidget(new ChannelSlider(colorContentX, colorY + 102, colorWidth, Channel.ALPHA));

        addRenderableWidget(Button.builder(Component.translatable("button.fps_overlay.reset_selected_color"), button -> {
            setSelectedColor(getDefaultColor(selectedColorTarget));
            syncColorSliders();
        }).bounds(colorContentX, colorY + 130, colorWidth, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(this.width / 2 - 75, footerY, 150, 20).build());

        syncColorSliders();
    }

    @Override
    public void onClose() {
        FpsOverlayMod.saveConfigForCurrentContext(config);
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        boolean handledByWidget = super.mouseClicked(click, doubled);
        if (handledByWidget) {
            return true;
        }

        if (click.button() == 0) {
            OverlayRenderer.LayoutBounds bounds = OverlayRenderer.getPreviewBounds(this.width, this.height, config);
            if (bounds.contains(click.x(), click.y())) {
                dragging = true;
                previewScreenX = bounds.x();
                previewScreenY = bounds.y();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (dragging && click.button() == 0) {
            previewScreenX += deltaX;
            previewScreenY += deltaY;

            OverlayRenderer.LayoutBounds screenBounds = OverlayRenderer.getPreviewBounds(this.width, this.height, config);
            double maxX = Math.max(0, this.width - screenBounds.width());
            double maxY = Math.max(0, this.height - screenBounds.height());
            previewScreenX = Math.max(0, Math.min(previewScreenX, maxX));
            previewScreenY = Math.max(0, Math.min(previewScreenY, maxY));

            float scale = config.appearance.hudScale;
            OverlayRenderer.LayoutBounds logicalBounds =
                    OverlayRenderer.getPreviewLogicalBounds(this.width, this.height, config);
            int logicalWidth = Math.max(1, Math.round(this.width / scale));
            int logicalHeight = Math.max(1, Math.round(this.height / scale));
            OverlayRenderer.AnchorPoint anchor = OverlayRenderer.getAnchorPoint(
                    logicalWidth, logicalHeight, logicalBounds.width(), logicalBounds.height(), config.appearance.position);

            int targetLogicalX = (int) Math.round(previewScreenX / scale);
            int targetLogicalY = (int) Math.round(previewScreenY / scale);
            config.appearance.xOffset = targetLogicalX - anchor.x();
            config.appearance.yOffset = targetLogicalY - anchor.y();
            return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (click.button() == 0) {
            dragging = false;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xC0182028, 0xD010141A);
        OverlayRenderer.renderPreview(guiGraphics, minecraft, config, this.width, this.height);
        renderColorPanel(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(font, title, this.width / 2, 48, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(font, Component.translatable("text.fps_overlay.position_editor_hint"),
                this.width / 2, 62, 0xFFB7C6D1);
        guiGraphics.drawString(font, Component.translatable("option.fps_overlay.hudScale"),
                338, 26, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, (int) (config.appearance.hudScale * 100) + "%",
                395, 26, 0xFF7FB9D7, false);
    }

    private void renderColorPanel(GuiGraphics guiGraphics) {
        int x = this.width - COLOR_PANEL_WIDTH - 20;
        int y = 16;
        int border = 0xFF334A5C;
        int background = 0xD018232D;
        int swatchColor = getSelectedColor();
        int swatchX = x + 10;
        int swatchY = y + 16;
        int swatchWidth = COLOR_PANEL_WIDTH - 20;
        int swatchHeight = 14;

        guiGraphics.fill(x, y, x + COLOR_PANEL_WIDTH, y + COLOR_PANEL_HEIGHT, background);
        guiGraphics.fill(x, y, x + COLOR_PANEL_WIDTH, y + 1, border);
        guiGraphics.fill(x, y + COLOR_PANEL_HEIGHT - 1, x + COLOR_PANEL_WIDTH, y + COLOR_PANEL_HEIGHT, border);
        guiGraphics.fill(x, y, x + 1, y + COLOR_PANEL_HEIGHT, border);
        guiGraphics.fill(x + COLOR_PANEL_WIDTH - 1, y, x + COLOR_PANEL_WIDTH, y + COLOR_PANEL_HEIGHT, border);

        guiGraphics.drawString(font, Component.translatable("text.fps_overlay.color_editor").getString(),
                x + 10, y + 6, 0xFFE8EEF2, false);
        guiGraphics.drawString(font, Component.translatable("text.fps_overlay.color_editor_hint").getString(),
                x + 10, y + 20, 0xFF8FB0C2, false);

        guiGraphics.fill(swatchX, swatchY + 22, swatchX + swatchWidth, swatchY + 22 + swatchHeight, swatchColor);
        guiGraphics.fill(swatchX, swatchY + 22, swatchX + swatchWidth, swatchY + 23, 0xFF000000);
        guiGraphics.fill(swatchX, swatchY + 22 + swatchHeight - 1, swatchX + swatchWidth, swatchY + 22 + swatchHeight, 0xFF000000);
        guiGraphics.fill(swatchX, swatchY + 22, swatchX + 1, swatchY + 22 + swatchHeight, 0xFF000000);
        guiGraphics.fill(swatchX + swatchWidth - 1, swatchY + 22, swatchX + swatchWidth, swatchY + 22 + swatchHeight, 0xFF000000);

        guiGraphics.drawString(font, String.format("#%08X", swatchColor),
                x + 10, y + 56, 0xFFB7C6D1, false);
    }

    private void syncColorSliders() {
        if (redSlider == null) {
            return;
        }

        int color = getSelectedColor();
        redSlider.setChannelValue((color >> 16) & 0xFF);
        greenSlider.setChannelValue((color >> 8) & 0xFF);
        blueSlider.setChannelValue(color & 0xFF);
        alphaSlider.setChannelValue((color >>> 24) & 0xFF);
    }

    private int getSelectedColor() {
        return switch (selectedColorTarget) {
            case BACKGROUND -> withAlpha(config.appearance.backgroundColor, config.appearance.backgroundOpacity);
            case LABEL -> config.appearance.labelColor;
            case VALUE -> config.appearance.valueColor;
            case UNIT -> config.appearance.unitColor;
            case DIVIDER -> config.appearance.dividerColor;
            case GOOD -> config.appearance.goodColor;
            case WARNING -> config.appearance.warningColor;
            case BAD -> config.appearance.badColor;
        };
    }

    private void setSelectedColor(int color) {
        switch (selectedColorTarget) {
            case BACKGROUND -> {
                config.appearance.backgroundColor = color & 0x00FFFFFF;
                config.appearance.backgroundOpacity = (color >>> 24) & 0xFF;
            }
            case LABEL -> config.appearance.labelColor = color;
            case VALUE -> config.appearance.valueColor = color;
            case UNIT -> config.appearance.unitColor = color;
            case DIVIDER -> config.appearance.dividerColor = color;
            case GOOD -> config.appearance.goodColor = color;
            case WARNING -> config.appearance.warningColor = color;
            case BAD -> config.appearance.badColor = color;
        }
    }

    private int getDefaultColor(ColorTarget target) {
        ModConfig.Appearance defaults = new ModConfig().appearance;
        return switch (target) {
            case BACKGROUND -> withAlpha(defaults.backgroundColor, defaults.backgroundOpacity);
            case LABEL -> defaults.labelColor;
            case VALUE -> defaults.valueColor;
            case UNIT -> defaults.unitColor;
            case DIVIDER -> defaults.dividerColor;
            case GOOD -> defaults.goodColor;
            case WARNING -> defaults.warningColor;
            case BAD -> defaults.badColor;
        };
    }

    private int getChannelValue(Channel channel) {
        int color = getSelectedColor();
        return switch (channel) {
            case RED -> (color >> 16) & 0xFF;
            case GREEN -> (color >> 8) & 0xFF;
            case BLUE -> color & 0xFF;
            case ALPHA -> (color >>> 24) & 0xFF;
        };
    }

    private void setChannelValue(Channel channel, int value) {
        int color = getSelectedColor();
        int alpha = (color >>> 24) & 0xFF;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        switch (channel) {
            case RED -> red = value;
            case GREEN -> green = value;
            case BLUE -> blue = value;
            case ALPHA -> alpha = value;
        }

        setSelectedColor(((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF));
    }

    private static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    private enum Channel {
        RED("R"),
        GREEN("G"),
        BLUE("B"),
        ALPHA("A");

        private final String shortLabel;

        Channel(String shortLabel) {
            this.shortLabel = shortLabel;
        }
    }

    private enum ColorTarget {
        BACKGROUND("option.fps_overlay.backgroundColor"),
        LABEL("option.fps_overlay.labelColor"),
        VALUE("option.fps_overlay.valueColor"),
        UNIT("option.fps_overlay.unitColor"),
        DIVIDER("option.fps_overlay.dividerColor"),
        GOOD("option.fps_overlay.goodColor"),
        WARNING("option.fps_overlay.warningColor"),
        BAD("option.fps_overlay.badColor");

        private final String translationKey;

        ColorTarget(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component getDisplayText() {
            return Component.translatable(translationKey);
        }
    }

    // Keeps one ARGB channel in sync with the selected preview color.
    private class ChannelSlider extends AbstractSliderButton {
        private final Channel channel;

        protected ChannelSlider(int x, int y, int width, Channel channel) {
            super(x, y, width, 20, Component.empty(), 0.0);
            this.channel = channel;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(channel.shortLabel + ": " + getChannelValue(channel)));
        }

        @Override
        protected void applyValue() {
            PositionEditorScreen.this.setChannelValue(channel, (int) Math.round(this.value * 255.0));
            updateMessage();
        }

        void setChannelValue(int value) {
            this.value = Math.max(0.0, Math.min(1.0, value / 255.0));
            updateMessage();
        }
    }
}
