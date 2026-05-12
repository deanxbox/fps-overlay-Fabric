package net.hicham.fps_overlay;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MetricOrderScreen extends Screen {
    private static final int ROW_HEIGHT = 30;
    private static final int LIST_TOP = 72;
    private static final int MAX_LIST_WIDTH = 660;
    private static final int TOGGLE_WIDTH = 72;
    private static final int RESET_WIDTH = 48;
    private static final int MAX_EDIT_WIDTH = 136;
    private static final int MIN_EDIT_WIDTH = 64;
    private static final int UPDATE_WIDTH = 76;
    private static final int HANDLE_WIDTH = 52;
    private static final int ORDER_WIDTH = 28;
    private static final int METRIC_LABEL_AREA_WIDTH = 180;
    private static final int ROW_CONTROL_GAP_WIDTH = 64;
    private static final int COMPACT_LIST_WIDTH = ORDER_WIDTH + TOGGLE_WIDTH + HANDLE_WIDTH
            + METRIC_LABEL_AREA_WIDTH + ROW_CONTROL_GAP_WIDTH;
    private static final int FULL_LIST_WIDTH = ORDER_WIDTH + TOGGLE_WIDTH + RESET_WIDTH + UPDATE_WIDTH + HANDLE_WIDTH
            + METRIC_LABEL_AREA_WIDTH + ROW_CONTROL_GAP_WIDTH + MIN_EDIT_WIDTH;
    private static final int TOAST_DURATION_TICKS = 40;

    private static final int TEXT_PRIMARY = 0xFFE8EEF2;
    private static final int TEXT_MUTED = 0xFF7A9AB0;
    private static final int TEXT_HEADER = 0xFFA8C0CE;
    private static final int TEXT_GREEN = 0xFF4DE89A;
    private static final int TEXT_RED = 0xFFFF8888;
    private static final int TEXT_ACCENT = 0xFF5BBDE4;

    private static final int COL_BG_NORMAL = 0xAA141B22;
    private static final int COL_BG_HOVER = 0xAA1A2535;
    private static final int COL_BG_DRAG = 0xCC2A4A62;
    private static final int COL_BG_SCREEN = 0xFF0D1117;
    private static final int COL_BORDER = 0xFF2A3A4A;
    private static final int COL_BORDER_HI = 0xFF3D5A72;
    private static final int COL_BORDER_ACC = 0xFF6FB8E0;
    private static final int COL_HOVER_STRIP = 0xAA5BBDE4;

    private final Screen parent;
    private final ModConfig config;
    private final List<OverlayMetric> order;
    private final Map<OverlayMetric, MetricRowWidgets> rowWidgets = new EnumMap<>(OverlayMetric.class);

    private int draggingIndex = -1;
    private int hoverIndex = -1;
    private double scrollAmount = 0;
    private int maxScroll = 0;
    private String toastMessage = "";
    private int toastTicks = 0;

    private Button resetAllButton;
    private Button doneButton;

    public MetricOrderScreen(Screen parent, ModConfig config) {
        super(Component.translatable("screen.fps_overlay.metric_order"));
        this.parent = parent;
        this.config = config;
        this.order = new ArrayList<>(OverlayMetric.sanitizeOrder(config.hud.metricOrder));
    }

    @Override
    protected void init() {
        clearWidgets();
        rowWidgets.clear();

        for (OverlayMetric metric : OverlayMetric.values()) {
            rowWidgets.put(metric, createRowWidgets(metric));
        }

        updateMaxScroll();
        clampScroll();
        layoutRowWidgets();

        resetAllButton = addRenderableWidget(
                Button.builder(Component.translatable("button.fps_overlay.reset_all_metrics"), button -> resetAllMetrics())
                        .bounds(0, this.height - 28, 120, 20)
                        .build());

        doneButton = addRenderableWidget(
                Button.builder(Component.translatable("gui.done"), button -> onClose())
                        .bounds(0, this.height - 28, 155, 20)
                        .build());
        layoutFooterButtons();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.scrollAmount -= verticalAmount * 15;
        clampScroll();
        layoutRowWidgets();
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }

        int row = rowAt(click.x(), click.y());
        if (row >= 0 && row < order.size() && click.button() == 0 && isHandleHit(click.x())) {
            draggingIndex = row;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (draggingIndex >= 0 && click.button() == 0) {
            int target = rowAt(getListLeft() + getListWidth() - 2, click.y());
            if (target >= 0 && target < order.size() && target != draggingIndex) {
                OverlayMetric metric = order.remove(draggingIndex);
                order.add(target, metric);
                draggingIndex = target;
                layoutRowWidgets();
            }
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (click.button() == 0 && draggingIndex >= 0) {
            saveOrder();
            draggingIndex = -1;
            showToast("screen.fps_overlay.toast_saved");
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        hoverIndex = rowAt(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public void tick() {
        super.tick();
        if (toastTicks > 0) {
            toastTicks--;
        }
    }

    @Override
    public void onClose() {
        saveOrder();
        FpsOverlayMod.saveConfigForCurrentContext(config);
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, COL_BG_SCREEN);

        int listLeft = getListLeft();
        int listWidth = getListWidth();
        int editWidth = getEditWidth();
        boolean compactLayout = isCompactLayout();
        int viewportTop = viewportTop();
        int viewportBottom = viewportBottom();
        int headerY = LIST_TOP - 13;

        guiGraphics.enableScissor(0, viewportTop, this.width, viewportBottom);
        for (int i = 0; i < order.size(); i++) {
            int rowY = (int) (LIST_TOP + (i * ROW_HEIGHT) - scrollAmount);
            if (rowY + ROW_HEIGHT < viewportTop || rowY > viewportBottom) {
                continue;
            }

            OverlayMetric metric = order.get(i);
            boolean isDragging = i == draggingIndex;
            boolean isHovered = i == hoverIndex && draggingIndex < 0;
            String defaultName = Component.translatable(metric.getDisplayNameKey()).getString();
            String customName = config.hud.getCustomMetricName(metric);

            int backgroundColor = isDragging ? COL_BG_DRAG : isHovered ? COL_BG_HOVER : COL_BG_NORMAL;
            int borderColor = isDragging ? COL_BORDER_ACC : isHovered ? COL_BORDER_HI : COL_BORDER;

            guiGraphics.fill(listLeft, rowY, listLeft + listWidth, rowY + 24, backgroundColor);
            drawBorder(guiGraphics, listLeft, rowY, listLeft + listWidth, rowY + 24, borderColor);

            if (isHovered || isDragging) {
                guiGraphics.fill(listLeft, rowY, listLeft + 2, rowY + 24, COL_HOVER_STRIP);
            }

            int badgeX = listLeft + 4;
            guiGraphics.fill(badgeX, rowY + 6, badgeX + 14, rowY + 18, COL_BORDER);
            guiGraphics.drawCenteredString(font, String.valueOf(i + 1), badgeX + 7, rowY + 8, TEXT_MUTED);

            int nameX = listLeft + ORDER_WIDTH + TOGGLE_WIDTH + 6;
            int nameRight = compactLayout
                    ? listLeft + listWidth - HANDLE_WIDTH - 8
                    : listLeft + listWidth - HANDLE_WIDTH - UPDATE_WIDTH - RESET_WIDTH - editWidth - 18;
            String visibleName = font.plainSubstrByWidth(defaultName, Math.max(24, nameRight - nameX));
            guiGraphics.drawString(font, visibleName, nameX, rowY + 4, TEXT_PRIMARY, true);

            if (!compactLayout && !customName.isBlank()) {
                String preview = Component.translatable("text.fps_overlay.metric_rename_preview", customName).getString();
                guiGraphics.drawString(font, font.plainSubstrByWidth(preview, Math.max(24, nameRight - nameX)),
                        nameX, rowY + 14, TEXT_MUTED, false);
            }

            drawGripIndicator(guiGraphics, listLeft + listWidth - (HANDLE_WIDTH / 2), rowY + 6,
                    isDragging ? TEXT_ACCENT : TEXT_MUTED);

            if (isDragging) {
                guiGraphics.drawCenteredString(font,
                        Component.translatable("text.fps_overlay.dragging"),
                        listLeft + listWidth - HANDLE_WIDTH / 2,
                        rowY + 3, TEXT_ACCENT);
            }
        }
        guiGraphics.disableScissor();

        int cornerX = listLeft;
        int cornerWidth = listWidth;
        int cornerTop = viewportTop - 2;
        int cornerBottom = viewportBottom + 2;
        int cornerSize = 8;

        guiGraphics.fill(cornerX, cornerTop, cornerX + cornerSize, cornerTop + 1, TEXT_ACCENT);
        guiGraphics.fill(cornerX, cornerTop, cornerX + 1, cornerTop + cornerSize, TEXT_ACCENT);
        guiGraphics.fill(cornerX + cornerWidth - cornerSize, cornerTop, cornerX + cornerWidth, cornerTop + 1, TEXT_ACCENT);
        guiGraphics.fill(cornerX + cornerWidth - 1, cornerTop, cornerX + cornerWidth, cornerTop + cornerSize, TEXT_ACCENT);
        guiGraphics.fill(cornerX, cornerBottom - cornerSize, cornerX + 1, cornerBottom, TEXT_ACCENT);
        guiGraphics.fill(cornerX, cornerBottom - 1, cornerX + cornerSize, cornerBottom, TEXT_ACCENT);
        guiGraphics.fill(cornerX + cornerWidth - 1, cornerBottom - cornerSize, cornerX + cornerWidth, cornerBottom, TEXT_ACCENT);
        guiGraphics.fill(cornerX + cornerWidth - cornerSize, cornerBottom - 1, cornerX + cornerWidth, cornerBottom, TEXT_ACCENT);

        if (maxScroll > 0) {
            int trackX = listLeft + listWidth + 3;
            int trackHeight = viewportBottom - viewportTop;
            float ratio = (float) trackHeight / (order.size() * ROW_HEIGHT);
            int thumbHeight = Math.max(16, (int) (trackHeight * ratio));
            int thumbY = viewportTop + (int) ((scrollAmount / maxScroll) * (trackHeight - thumbHeight));

            guiGraphics.fill(trackX, viewportTop, trackX + 3, viewportTop + trackHeight, COL_BORDER);
            guiGraphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, COL_BORDER_ACC);
        }

        guiGraphics.enableScissor(0, viewportTop, this.width, viewportBottom);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.disableScissor();

        guiGraphics.fill(0, 0, this.width, LIST_TOP - 14, COL_BG_SCREEN);
        guiGraphics.fillGradient(0, LIST_TOP - 14, this.width, viewportTop, COL_BG_SCREEN, 0x0010141A);
        guiGraphics.fillGradient(0, viewportBottom, this.width, viewportBottom + 7, 0x0010141A, COL_BG_SCREEN);
        guiGraphics.fill(0, viewportBottom + 7, this.width, this.height, COL_BG_SCREEN);

        guiGraphics.drawCenteredString(font, title, this.width / 2, 16, TEXT_PRIMARY);
        guiGraphics.drawCenteredString(font,
                Component.translatable("text.fps_overlay.metric_order_hint"),
                this.width / 2, 28, TEXT_HEADER);

        guiGraphics.drawString(font, "#", listLeft + 4, headerY, TEXT_MUTED, false);
        guiGraphics.drawString(font,
                Component.translatable("text.fps_overlay.metric_column_visibility").getString(),
                listLeft + ORDER_WIDTH + 10, headerY, TEXT_MUTED, false);
        guiGraphics.drawString(font,
                Component.translatable("text.fps_overlay.metric_column_name").getString(),
                listLeft + ORDER_WIDTH + TOGGLE_WIDTH + 12, headerY, TEXT_MUTED, false);
        if (!compactLayout) {
            guiGraphics.drawString(font,
                    Component.translatable("text.fps_overlay.metric_column_custom").getString(),
                    listLeft + listWidth - HANDLE_WIDTH - UPDATE_WIDTH - RESET_WIDTH - editWidth - 12, headerY, TEXT_MUTED, false);
            guiGraphics.drawString(font,
                    Component.translatable("text.fps_overlay.metric_column_refresh").getString(),
                    listLeft + listWidth - HANDLE_WIDTH - UPDATE_WIDTH - 4, headerY, TEXT_MUTED, false);
        }
        guiGraphics.drawString(font,
                Component.translatable("text.fps_overlay.metric_column_drag").getString(),
                listLeft + listWidth - HANDLE_WIDTH + 6, headerY, TEXT_MUTED, false);

        guiGraphics.drawCenteredString(font,
                Component.translatable("text.fps_overlay.metric_order_footer"),
                this.width / 2, this.height - 40, TEXT_MUTED);

        resetAllButton.render(guiGraphics, mouseX, mouseY, partialTick);
        doneButton.render(guiGraphics, mouseX, mouseY, partialTick);

        if (toastTicks > 0) {
            float alpha = Math.min(1.0f, toastTicks / 8.0f);
            int alphaChannel = ((int) (alpha * 255.0f)) & 0xFF;
            int toastAlpha = alphaChannel << 24;
            String message = Component.translatable(toastMessage).getString();
            int toastWidth = font.width(message) + 20;
            int toastX = (this.width - toastWidth) / 2;
            int toastY = this.height - 60;

            guiGraphics.fill(toastX, toastY, toastX + toastWidth, toastY + 14,
                    toastAlpha | (COL_BG_DRAG & 0x00FFFFFF));
            drawBorder(guiGraphics, toastX, toastY, toastX + toastWidth, toastY + 14,
                    toastAlpha | (TEXT_ACCENT & 0x00FFFFFF));
            guiGraphics.drawCenteredString(font, message, this.width / 2, toastY + 3,
                    toastAlpha | (TEXT_ACCENT & 0x00FFFFFF));
        }
    }

    private MetricRowWidgets createRowWidgets(OverlayMetric metric) {
        Button toggleButton = Button.builder(getVisibilityText(metric), button -> {
            config.hud.setMetricEnabled(metric, !config.hud.isMetricEnabled(metric));
            button.setMessage(getVisibilityText(metric));
        }).bounds(0, 0, TOGGLE_WIDTH, 20).build();

        EditBox renameBox = new EditBox(this.font, 0, 0, MAX_EDIT_WIDTH, 20,
                Component.translatable("text.fps_overlay.custom_label"));
        renameBox.setMaxLength(20);
        renameBox.setHint(Component.translatable("text.fps_overlay.custom_label_placeholder"));
        renameBox.setValue(config.hud.getCustomMetricName(metric));
        renameBox.setResponder(value -> config.hud.setCustomMetricName(metric, value));

        Button updateButton = Button.builder(getRefreshText(metric), button -> {
            cycleRefreshInterval(metric);
            button.setMessage(getRefreshText(metric));
        }).bounds(0, 0, UPDATE_WIDTH, 20).build();

        Button resetButton = Button.builder(Component.translatable("button.fps_overlay.reset_metric_name"), button -> {
            config.hud.setCustomMetricName(metric, "");
            renameBox.setValue("");
        }).bounds(0, 0, RESET_WIDTH, 20).build();

        addRenderableWidget(toggleButton);
        addRenderableWidget(renameBox);
        addRenderableWidget(updateButton);
        addRenderableWidget(resetButton);
        return new MetricRowWidgets(toggleButton, renameBox, updateButton, resetButton);
    }

    private void layoutRowWidgets() {
        int listLeft = getListLeft();
        int listWidth = getListWidth();
        int editWidth = getEditWidth();
        boolean compactLayout = isCompactLayout();
        int viewportTop = viewportTop();
        int viewportBottom = viewportBottom();

        for (int i = 0; i < order.size(); i++) {
            OverlayMetric metric = order.get(i);
            MetricRowWidgets widgets = rowWidgets.get(metric);
            if (widgets == null) {
                continue;
            }

            int rowY = (int) (LIST_TOP + (i * ROW_HEIGHT) - scrollAmount);
            boolean visible = rowY + ROW_HEIGHT > viewportTop && rowY < viewportBottom;
            int widgetY = visible ? rowY + 2 : -200;

            widgets.toggleButton().setX(listLeft + ORDER_WIDTH + 4);
            widgets.toggleButton().setY(widgetY);
            widgets.toggleButton().visible = visible;

            widgets.renameBox().setX(listLeft + listWidth - HANDLE_WIDTH - UPDATE_WIDTH - RESET_WIDTH - editWidth - 10);
            widgets.renameBox().setY(widgetY);
            widgets.renameBox().setWidth(editWidth);
            widgets.renameBox().visible = visible && !compactLayout;

            widgets.resetButton().setX(listLeft + listWidth - HANDLE_WIDTH - UPDATE_WIDTH - RESET_WIDTH - 6);
            widgets.resetButton().setY(widgetY);
            widgets.resetButton().visible = visible && !compactLayout;

            widgets.updateButton().setX(listLeft + listWidth - HANDLE_WIDTH - UPDATE_WIDTH - 2);
            widgets.updateButton().setY(widgetY);
            widgets.updateButton().visible = visible && !compactLayout;

            if (!visible || compactLayout) {
                widgets.renameBox().setFocused(false);
                if (getFocused() == widgets.renameBox()) {
                    setFocused(null);
                }
            }
        }
    }

    private Component getVisibilityText(OverlayMetric metric) {
        boolean enabled = config.hud.isMetricEnabled(metric);
        return Component.literal(
                enabled
                        ? Component.translatable("button.fps_overlay.metric_visible").getString()
                        : Component.translatable("button.fps_overlay.metric_hidden").getString())
                .withColor(enabled ? TEXT_GREEN : TEXT_RED);
    }

    private void resetAllMetrics() {
        ModConfig defaults = new ModConfig();
        order.clear();
        order.addAll(OverlayMetric.sanitizeOrder(defaults.hud.metricOrder));

        if (config.hud.metricDisplayNames == null) {
            config.hud.metricDisplayNames = new LinkedHashMap<>();
        } else {
            config.hud.metricDisplayNames.clear();
        }
        if (config.hud.metricUpdateIntervals == null) {
            config.hud.metricUpdateIntervals = new LinkedHashMap<>();
        } else {
            config.hud.metricUpdateIntervals.clear();
        }

        for (OverlayMetric metric : OverlayMetric.values()) {
            config.hud.setMetricEnabled(metric, defaults.hud.isMetricEnabled(metric));
            MetricRowWidgets widgets = rowWidgets.get(metric);
            if (widgets != null) {
                widgets.renameBox().setValue("");
                widgets.toggleButton().setMessage(getVisibilityText(metric));
                widgets.updateButton().setMessage(getRefreshText(metric));
            }
        }

        saveOrder();
        layoutRowWidgets();
        showToast("screen.fps_overlay.toast_reset");
    }

    private void drawBorder(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1, y1, x2, y1 + 1, color);
        guiGraphics.fill(x1, y2 - 1, x2, y2, color);
        guiGraphics.fill(x1, y1, x1 + 1, y2, color);
        guiGraphics.fill(x2 - 1, y1, x2, y2, color);
    }

    private void drawGripIndicator(GuiGraphics guiGraphics, int centerX, int topY, int color) {
        int startX = centerX - 5;
        for (int row = 0; row < 3; row++) {
            int y = topY + (row * 5);
            guiGraphics.fill(startX, y, startX + 2, y + 2, color);
            guiGraphics.fill(startX + 5, y, startX + 7, y + 2, color);
        }
    }

    private int rowAt(double mouseX, double mouseY) {
        int listLeft = getListLeft();
        if (mouseX < listLeft || mouseX > listLeft + getListWidth()) {
            return -1;
        }
        if (mouseY < viewportTop() || mouseY > viewportBottom()) {
            return -1;
        }

        int relative = (int) (mouseY - LIST_TOP + scrollAmount);
        if (relative < 0) {
            return -1;
        }

        int row = relative / ROW_HEIGHT;
        return row < order.size() ? row : -1;
    }

    private boolean isHandleHit(double mouseX) {
        int handleLeft = getListLeft() + getListWidth() - HANDLE_WIDTH;
        return mouseX >= handleLeft && mouseX <= handleLeft + HANDLE_WIDTH;
    }

    private int getListLeft() {
        return (this.width - getListWidth()) / 2;
    }

    private int getListWidth() {
        int availableWidth = Math.max(0, this.width - 24);
        return Math.max(Math.min(COMPACT_LIST_WIDTH, availableWidth), Math.min(MAX_LIST_WIDTH, availableWidth));
    }

    private int getEditWidth() {
        if (isCompactLayout()) {
            return 0;
        }

        int fixedWidth = ORDER_WIDTH + TOGGLE_WIDTH + RESET_WIDTH + UPDATE_WIDTH + HANDLE_WIDTH
                + METRIC_LABEL_AREA_WIDTH + ROW_CONTROL_GAP_WIDTH;
        return Math.max(MIN_EDIT_WIDTH, Math.min(MAX_EDIT_WIDTH, getListWidth() - fixedWidth));
    }

    private boolean isCompactLayout() {
        return getListWidth() < FULL_LIST_WIDTH;
    }

    private void layoutFooterButtons() {
        if (resetAllButton == null || doneButton == null) {
            return;
        }

        int gap = 8;
        int availableWidth = Math.max(0, this.width - 8);
        int buttonWidth = Math.min(135, Math.max(36, (availableWidth - gap) / 2));
        int left = Math.max(4, (this.width - (buttonWidth * 2) - gap) / 2);
        int y = this.height - 28;
        resetAllButton.setX(left);
        resetAllButton.setY(y);
        resetAllButton.setWidth(buttonWidth);
        doneButton.setX(left + buttonWidth + gap);
        doneButton.setY(y);
        doneButton.setWidth(buttonWidth);
    }

    private int viewportTop() {
        return LIST_TOP;
    }

    private int viewportBottom() {
        return this.height - 52;
    }

    private void updateMaxScroll() {
        int listHeight = order.size() * ROW_HEIGHT;
        int viewportHeight = viewportBottom() - viewportTop();
        this.maxScroll = Math.max(0, listHeight - viewportHeight);
    }

    private void clampScroll() {
        this.scrollAmount = Math.max(0, Math.min(this.scrollAmount, this.maxScroll));
    }

    private void saveOrder() {
        List<String> ids = new ArrayList<>();
        for (OverlayMetric metric : order) {
            ids.add(metric.getId());
        }
        config.hud.metricOrder = ids;
    }

    private Component getRefreshText(OverlayMetric metric) {
        int interval = config.hud.getMetricUpdateInterval(metric, config.general.updateIntervalMs);
        return Component.literal(interval <= 0 ? "Live" : interval + "ms").withColor(TEXT_ACCENT);
    }

    private void cycleRefreshInterval(OverlayMetric metric) {
        int[] options = {0, 16, 50, 100, 250, 500, 1000};
        int current = config.hud.getMetricUpdateInterval(metric, config.general.updateIntervalMs);
        int index = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i] == current) {
                index = i;
                break;
            }
        }
        config.hud.setMetricUpdateInterval(metric, options[(index + 1) % options.length]);
    }

    private void showToast(String translationKey) {
        this.toastMessage = translationKey;
        this.toastTicks = TOAST_DURATION_TICKS;
    }

    private record MetricRowWidgets(Button toggleButton, EditBox renameBox, Button updateButton, Button resetButton) {
    }
}
