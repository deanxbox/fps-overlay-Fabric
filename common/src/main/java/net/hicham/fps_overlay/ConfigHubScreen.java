package net.hicham.fps_overlay;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigHubScreen extends Screen {
    private static final int PANEL_WIDTH = 292;
    private static final int PANEL_PADDING = 14;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;
    private static final int SUBTITLE_GAP = 18;
    private static final int HALF_BUTTON_WIDTH = 128;
    private static final int WIDE_BUTTON_WIDTH = 264;
    private static final int TOP_CONTENT_Y = 18;
    private static final int TOP_SUBTITLE_Y = 32;

    private final Screen parent;

    public ConfigHubScreen(Screen parent) {
        super(Component.translatable("screen.fps_overlay.config_hub"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        HubLayout layout = computeLayout();
        int contentX = layout.contentX();
        int leftX = contentX;
        int rightX = layout.panelX() + PANEL_WIDTH - PANEL_PADDING - HALF_BUTTON_WIDTH;
        int y = layout.toolsButtonsY();

        addRenderableWidget(Button.builder(Component.translatable("button.fps_overlay.open_settings"), button -> {
            if (minecraft != null) {
                minecraft.setScreen(ConfigScreenFactory.createSettingsScreen(this));
            }
        }).bounds(contentX, y, WIDE_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + BUTTON_GAP;

        addRenderableWidget(Button.builder(Component.translatable("button.fps_overlay.edit_position"), button -> {
            if (minecraft != null) {
                minecraft.setScreen(new PositionEditorScreen(this, FpsOverlayMod.getConfigForEditing()));
            }
        }).bounds(leftX, y, HALF_BUTTON_WIDTH, BUTTON_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("button.fps_overlay.arrange_metrics"), button -> {
            if (minecraft != null) {
                minecraft.setScreen(new MetricOrderScreen(this, FpsOverlayMod.getConfigForEditing()));
            }
        }).bounds(rightX, y, HALF_BUTTON_WIDTH, BUTTON_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(this.width / 2 - 72, layout.doneButtonY(), 144, 20).build());
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xD0121820, 0xE0091016);

        HubLayout layout = computeLayout();
        int centerX = this.width / 2;
        int contentX = layout.contentX();

        guiGraphics.drawCenteredString(font, title, centerX, TOP_CONTENT_Y, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(font, Component.translatable("text.fps_overlay.config_hub_hint"),
                centerX, TOP_SUBTITLE_Y, 0xFFB7C6D1);

        drawPanel(guiGraphics, layout.panelX(), layout.panelY(), PANEL_WIDTH, layout.panelHeight());

        drawSection(guiGraphics, contentX, layout.toolsSectionY(),
                "text.fps_overlay.tools_hint", "text.fps_overlay.tools_subtitle");

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int background = 0xC919232D;
        int border = 0xFF334A5C;
        guiGraphics.fill(x, y, x + width, y + height, background);
        guiGraphics.fill(x, y, x + width, y + 1, border);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, border);
        guiGraphics.fill(x, y, x + 1, y + height, border);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, border);
    }

    private void drawSection(GuiGraphics guiGraphics, int x, int y, String titleKey, String subtitleKey) {
        guiGraphics.drawString(font, Component.translatable(titleKey).getString(), x, y, 0xFFE8EEF2, false);
        guiGraphics.drawString(font, Component.translatable(subtitleKey).getString(), x, y + 11, 0xFF8FB0C2, false);
    }

    private HubLayout computeLayout() {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int contentX = panelX + PANEL_PADDING;
        int toolsSectionY = 0;
        int toolsButtonsY = toolsSectionY + SUBTITLE_GAP + 6;
        int panelContentHeight = toolsButtonsY + (BUTTON_HEIGHT * 2) + BUTTON_GAP - toolsSectionY;
        int panelHeight = (PANEL_PADDING * 2) + panelContentHeight;
        int minimumPanelY = TOP_SUBTITLE_Y + 26;
        int maximumPanelY = Math.max(minimumPanelY, this.height - panelHeight - 48);
        int panelY = Math.min(maximumPanelY, minimumPanelY + 34);
        int doneButtonY = Math.min(this.height - 28, panelY + panelHeight + 18);

        return new HubLayout(
                panelX,
                panelY,
                contentX,
                panelHeight,
                panelY + PANEL_PADDING + toolsSectionY,
                panelY + PANEL_PADDING + toolsButtonsY,
                doneButtonY);
    }

    private record HubLayout(int panelX, int panelY, int contentX, int panelHeight,
                             int toolsSectionY, int toolsButtonsY,
                             int doneButtonY) {
    }
}
