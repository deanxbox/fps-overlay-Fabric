package net.hicham.fps_overlay;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MetricOrderScreen extends Screen {

    // ── Layout constants ─────────────────────────────────────────────────────
    private static final int ROW_HEIGHT = 30;
    private static final int LIST_TOP = 72;
    private static final int LIST_WIDTH = 540;
    private static final int TOGGLE_WIDTH = 72;
    private static final int RESET_WIDTH = 48;
    private static final int EDIT_WIDTH = 176;
    private static final int HANDLE_WIDTH = 52;
    private static final int ORDER_WIDTH = 28;

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int TEXT_PRIMARY = 0xFFE8EEF2;
    private static final int TEXT_MUTED = 0xFF7A9AB0;
    private static final int TEXT_HEADER = 0xFFA8C0CE;
    private static final int TEXT_GREEN = 0xFF4DE89A;
    private static final int TEXT_RED = 0xFFFF8888;
    private static final int TEXT_ACCENT = 0xFF5BBDE4;

    private static final int COL_BG_NORMAL = 0xAA141B22;
    private static final int COL_BG_HOVER = 0xAA1A2535;
    private static final int COL_BG_DRAG = 0xCC2A4A62;
    private static final int COL_BORDER = 0xFF2A3A4A;
    private static final int COL_BORDER_HI = 0xFF3D5A72;
    private static final int COL_BORDER_ACC = 0xFF6FB8E0; // dragging border
    private static final int COL_BG_SCREEN = 0xFF0D1117;

    // ── Toast ─────────────────────────────────────────────────────────────────
    private static final int TOAST_DURATION_TICKS = 40; // 2 s at 20 tps
    private String toastMessage = "";
    private int toastTicks = 0;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;
    private final ModConfig config;
    private final List<OverlayMetric> order;
    private final Map<OverlayMetric, MetricRowWidgets> rowWidgets = new EnumMap<>(OverlayMetric.class);

    private int draggingIndex = -1;
    private int hoverIndex = -1;
    private double scrollAmount = 0;
    private int maxScroll = 0;
    private Button resetAllButton;
    private Button doneButton;

    // ── Constructor ───────────────────────────────────────────────────────────
    public MetricOrderScreen(Screen parent, ModConfig config) {
        super(Component.translatable("screen.fps_overlay.metric_order"));
        this.parent = parent;
        this.config = config;
        this.order = new ArrayList<>(OverlayMetric.sanitizeOrder(config.hud.metricOrder));
    }

    // ── init ──────────────────────────────────────────────────────────────────
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

        // Reset all button
        resetAllButton = addRenderableWidget(
                Button.builder(Component.translatable("button.fps_overlay.reset_all_metrics"),
                        button -> resetAllMetrics())
                        .bounds(this.width / 2 - 165, this.height - 28, 120, 20)
                        .build());

        // Done button
        doneButton = addRenderableWidget(
                Button.builder(Component.translatable("gui.done"), button -> onClose())
                        .bounds(this.width / 2 + 10, this.height - 28, 155, 20)
                        .build());
    }

    // ── Viewport helpers ──────────────────────────────────────────────────────
    /** Pixel-exact top of the scrollable row area. */
    private int viewportTop() {
        return LIST_TOP;
    }

    /** Pixel-exact bottom of the scrollable row area (above footer). */
    private int viewportBottom() {
        return this.height - 52;
    }

    // ── Scroll helpers ────────────────────────────────────────────────────────
    private void updateMaxScroll() {
        int listHeight = order.size() * ROW_HEIGHT;
        int viewportHeight = viewportBottom() - viewportTop();
        this.maxScroll = Math.max(0, listHeight - viewportHeight);
    }

    private void clampScroll() {
        this.scrollAmount = Math.max(0, Math.min(this.scrollAmount, this.maxScroll));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
            double horizontalAmount, double verticalAmount) {
        this.scrollAmount -= verticalAmount * 15;
        clampScroll();
        layoutRowWidgets();
        return true;
    }

    // ── Mouse interaction ─────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (super.mouseClicked(click, doubled))
            return true;

        int row = rowAt(click.x(), click.y());
        if (row >= 0 && row < order.size()
                && click.button() == 0
                && isHandleHit(click.x())) {
            draggingIndex = row;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click,
            double deltaX, double deltaY) {
        if (draggingIndex >= 0 && click.button() == 0) {
            int target = rowAt(getListLeft() + LIST_WIDTH - 2, click.y());
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

    // ── Close ─────────────────────────────────────────────────────────────────
    @Override
    public void onClose() {
        saveOrder();
        ConfigManager.saveConfig();
        if (minecraft != null)
            minecraft.setScreen(parent);
    }

    // ── Tick ─────────────────────────────────────────────────────────────────
    @Override
    public void tick() {
        super.tick();
        if (toastTicks > 0)
            toastTicks--;
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Background
        g.fill(0, 0, this.width, this.height, COL_BG_SCREEN);

        int listLeft = getListLeft();
        int vTop = viewportTop();
        int vBottom = viewportBottom();
        int headerY = LIST_TOP - 13;

        // ── Rows ─────────────────────────────────────────────────────────────
        g.enableScissor(0, vTop, this.width, vBottom);
        for (int i = 0; i < order.size(); i++) {
            int rowY = (int) (LIST_TOP + (i * ROW_HEIGHT) - scrollAmount);
            if (rowY + ROW_HEIGHT < vTop || rowY > vBottom)
                continue;

            OverlayMetric metric = order.get(i);
            boolean isDragging = (i == draggingIndex);
            boolean isHovered = (i == hoverIndex && draggingIndex < 0);
            String defaultName = Component.translatable(metric.getDisplayNameKey()).getString();
            String customName = config.hud.getCustomMetricName(metric);

            int bgColor = isDragging ? COL_BG_DRAG
                    : isHovered ? COL_BG_HOVER
                            : COL_BG_NORMAL;
            int borderColor = isDragging ? COL_BORDER_ACC
                    : isHovered ? COL_BORDER_HI
                            : COL_BORDER;

            // Row background + border
            g.fill(listLeft, rowY, listLeft + LIST_WIDTH, rowY + 24, bgColor);
            drawBorder(g, listLeft, rowY, listLeft + LIST_WIDTH, rowY + 24, borderColor);

            // Order badge (left of toggle, inside row)
            String badge = String.valueOf(i + 1);
            int badgeX = listLeft + 4;
            g.fill(badgeX, rowY + 6, badgeX + 14, rowY + 18, COL_BORDER);
            g.drawCenteredString(font, badge, badgeX + 7, rowY + 8, TEXT_MUTED);

            // Metric name
            g.drawString(font, defaultName,
                    listLeft + ORDER_WIDTH + TOGGLE_WIDTH + 6, rowY + 4, TEXT_PRIMARY, true);

            // Custom name preview ▶ alias
            if (!customName.isBlank()) {
                String preview = Component.translatable(
                        "text.fps_overlay.metric_rename_preview", customName).getString();
                g.drawString(font, preview,
                        listLeft + ORDER_WIDTH + TOGGLE_WIDTH + 6, rowY + 14,
                        TEXT_MUTED, false);
            }

            // Drag handle label
            String handleLabel = isDragging
                    ? Component.translatable("text.fps_overlay.dragging").getString()
                    : ":::";
            g.drawCenteredString(font, handleLabel,
                    listLeft + LIST_WIDTH - HANDLE_WIDTH / 2,
                    rowY + 8, isDragging ? TEXT_ACCENT : TEXT_MUTED);
        }
        g.disableScissor();

        // ── Corner accent brackets ────────────────────────────────────────────
        int bx = listLeft, bw = LIST_WIDTH, byt = vTop - 2, byb = vBottom + 2;
        int cs = 8;
        // top-left
        g.fill(bx, byt, bx + cs, byt + 1, TEXT_ACCENT);
        g.fill(bx, byt, bx + 1, byt + cs, TEXT_ACCENT);
        // top-right
        g.fill(bx + bw - cs, byt, bx + bw, byt + 1, TEXT_ACCENT);
        g.fill(bx + bw - 1, byt, bx + bw, byt + cs, TEXT_ACCENT);
        // bottom-left
        g.fill(bx, byb - cs, bx + 1, byb, TEXT_ACCENT);
        g.fill(bx, byb - 1, bx + cs, byb, TEXT_ACCENT);
        // bottom-right
        g.fill(bx + bw - 1, byb - cs, bx + bw, byb, TEXT_ACCENT);
        g.fill(bx + bw - cs, byb - 1, bx + bw, byb, TEXT_ACCENT);

        // ── Scrollbar ─────────────────────────────────────────────────────────
        if (maxScroll > 0) {
            int trackX = listLeft + LIST_WIDTH + 3;
            int trackTop = vTop;
            int trackH = vBottom - vTop;
            float ratio = (float) trackH / (order.size() * ROW_HEIGHT);
            int thumbH = Math.max(16, (int) (trackH * ratio));
            int thumbY = trackTop + (int) ((scrollAmount / maxScroll) * (trackH - thumbH));
            g.fill(trackX, trackTop, trackX + 3, trackTop + trackH, COL_BORDER);
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, COL_BORDER_ACC);
        }

        // ── Column headers ────────────────────────────────────────────────────
        // (drawn before widgets but will be redrawn after fades below)

        // ── Widgets (scissored so buttons/editboxes clip to viewport) ─────────
        g.enableScissor(0, vTop, this.width, vBottom);
        super.render(g, mouseX, mouseY, partialTick);
        g.disableScissor();

        // ── Redraw fade overlays on top of widgets ────────────────────────────
        g.fill(0, 0, this.width, LIST_TOP - 14, COL_BG_SCREEN);
        g.fillGradient(0, LIST_TOP - 14, this.width, vTop, COL_BG_SCREEN, 0x0010141A);
        g.fillGradient(0, vBottom, this.width, vBottom + 7, 0x0010141A, COL_BG_SCREEN);
        g.fill(0, vBottom + 7, this.width, this.height, COL_BG_SCREEN);

        // ── Title & subtitle (on top of everything) ───────────────────────────
        g.drawCenteredString(font, title, this.width / 2, 16, TEXT_PRIMARY);
        g.drawCenteredString(font,
                Component.translatable("text.fps_overlay.metric_order_hint"),
                this.width / 2, 28, TEXT_HEADER);

        // ── Column headers ────────────────────────────────────────────────────
        g.drawString(font, "#",
                listLeft + 4, headerY, TEXT_MUTED, false);
        g.drawString(font,
                Component.translatable("text.fps_overlay.metric_column_visibility").getString(),
                listLeft + ORDER_WIDTH + 10, headerY, TEXT_MUTED, false);
        g.drawString(font,
                Component.translatable("text.fps_overlay.metric_column_name").getString(),
                listLeft + ORDER_WIDTH + TOGGLE_WIDTH + 12, headerY, TEXT_MUTED, false);
        g.drawString(font,
                Component.translatable("text.fps_overlay.metric_column_custom").getString(),
                listLeft + LIST_WIDTH - HANDLE_WIDTH - RESET_WIDTH - EDIT_WIDTH - 8,
                headerY, TEXT_MUTED, false);
        g.drawString(font,
                Component.translatable("text.fps_overlay.metric_column_drag").getString(),
                listLeft + LIST_WIDTH - HANDLE_WIDTH + 6, headerY, TEXT_MUTED, false);

        // ── Footer hint ───────────────────────────────────────────────────────
        g.drawCenteredString(font,
                Component.translatable("text.fps_overlay.metric_order_footer"),
                this.width / 2, this.height - 40, TEXT_MUTED);

        // ── Footer buttons (must be last — above fade overlay) ────────────────
        resetAllButton.render(g, mouseX, mouseY, partialTick);
        doneButton.render(g, mouseX, mouseY, partialTick);

        // ── Toast ─────────────────────────────────────────────────────────────
        if (toastTicks > 0) {
            float alpha = Math.min(1f, toastTicks / 8f);
            int toastA = (int) (alpha * 0xFF) << 24;
            String msg = Component.translatable(toastMessage).getString();
            int tw = font.width(msg) + 20;
            int tx = (this.width - tw) / 2;
            int ty = this.height - 60;
            g.fill(tx, ty, tx + tw, ty + 14, (toastA & 0xFF000000) | (COL_BG_DRAG & 0x00FFFFFF));
            drawBorder(g, tx, ty, tx + tw, ty + 14, (toastA & 0xFF000000) | (TEXT_ACCENT & 0x00FFFFFF));
            g.drawCenteredString(font, msg, this.width / 2, ty + 3,
                    (toastA & 0xFF000000) | (TEXT_ACCENT & 0x00FFFFFF));
        }
    }

    // ── Row widget creation ───────────────────────────────────────────────────
    private MetricRowWidgets createRowWidgets(OverlayMetric metric) {
        Button toggleButton = Button.builder(getVisibilityText(metric), button -> {
            config.hud.setMetricEnabled(metric, !config.hud.isMetricEnabled(metric));
            button.setMessage(getVisibilityText(metric));
        }).bounds(0, 0, TOGGLE_WIDTH, 20).build();

        EditBox renameBox = new EditBox(this.font, 0, 0, EDIT_WIDTH, 20,
                Component.translatable("text.fps_overlay.custom_label"));
        renameBox.setMaxLength(20);
        renameBox.setHint(Component.translatable("text.fps_overlay.custom_label_placeholder"));
        renameBox.setValue(config.hud.getCustomMetricName(metric));
        renameBox.setResponder(value -> config.hud.setCustomMetricName(metric, value));

        Button resetButton = Button.builder(
                Component.translatable("button.fps_overlay.reset_metric_name"), button -> {
                    config.hud.setCustomMetricName(metric, "");
                    renameBox.setValue("");
                }).bounds(0, 0, RESET_WIDTH, 20).build();

        addRenderableWidget(toggleButton);
        addRenderableWidget(renameBox);
        addRenderableWidget(resetButton);
        return new MetricRowWidgets(toggleButton, renameBox, resetButton);
    }

    // ── Layout row widgets ────────────────────────────────────────────────────
    private void layoutRowWidgets() {
        int listLeft = getListLeft();
        int vTop = viewportTop();
        int vBottom = viewportBottom();

        for (int i = 0; i < order.size(); i++) {
            OverlayMetric metric = order.get(i);
            MetricRowWidgets wids = rowWidgets.get(metric);
            if (wids == null)
                continue;

            int rowY = (int) (LIST_TOP + (i * ROW_HEIGHT) - scrollAmount);
            boolean visible = rowY + ROW_HEIGHT > vTop && rowY < vBottom;

            // When not visible, park widgets far off-screen so Minecraft doesn't
            // render or accept input for them (visible=false alone doesn't stop
            // EditBox from drawing outside the scissor region).
            int widgetY = visible ? rowY + 2 : -200;

            wids.toggleButton().setX(listLeft + ORDER_WIDTH + 4);
            wids.toggleButton().setY(widgetY);
            wids.toggleButton().visible = visible;

            wids.renameBox().setX(
                    listLeft + LIST_WIDTH - HANDLE_WIDTH - RESET_WIDTH - EDIT_WIDTH - 6);
            wids.renameBox().setY(widgetY);
            wids.renameBox().visible = visible;

            wids.resetButton().setX(
                    listLeft + LIST_WIDTH - HANDLE_WIDTH - RESET_WIDTH - 2);
            wids.resetButton().setY(widgetY);
            wids.resetButton().visible = visible;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Component getVisibilityText(OverlayMetric metric) {
        boolean enabled = config.hud.isMetricEnabled(metric);
        return Component.literal(
                enabled ? Component.translatable("button.fps_overlay.metric_visible").getString()
                        : Component.translatable("button.fps_overlay.metric_hidden").getString())
                .withColor(enabled ? TEXT_GREEN : TEXT_RED);
    }

    private void resetAllMetrics() {
        ModConfig defaults = new ModConfig();
        order.clear();
        order.addAll(OverlayMetric.sanitizeOrder(defaults.hud.metricOrder));
        config.hud.metricDisplayNames.clear();

        for (OverlayMetric metric : OverlayMetric.values()) {
            config.hud.setMetricEnabled(metric, defaults.hud.isMetricEnabled(metric));
            MetricRowWidgets wids = rowWidgets.get(metric);
            if (wids != null) {
                wids.renameBox().setValue("");
                wids.toggleButton().setMessage(getVisibilityText(metric));
            }
        }

        saveOrder();
        layoutRowWidgets();
        showToast("screen.fps_overlay.toast_reset");
    }

    /** Draw a 1-px rectangle border without filling the interior. */
    private void drawBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y1 + 1, color); // top
        g.fill(x1, y2 - 1, x2, y2, color); // bottom
        g.fill(x1, y1, x1 + 1, y2, color); // left
        g.fill(x2 - 1, y1, x2, y2, color); // right
    }

    private int rowAt(double mouseX, double mouseY) {
        int listLeft = getListLeft();
        if (mouseX < listLeft || mouseX > listLeft + LIST_WIDTH)
            return -1;
        if (mouseY < viewportTop() || mouseY > viewportBottom())
            return -1;

        int relative = (int) (mouseY - LIST_TOP + scrollAmount);
        if (relative < 0)
            return -1;

        int row = relative / ROW_HEIGHT;
        return row < order.size() ? row : -1;
    }

    private boolean isHandleHit(double mouseX) {
        int handleLeft = getListLeft() + LIST_WIDTH - HANDLE_WIDTH;
        return mouseX >= handleLeft && mouseX <= handleLeft + HANDLE_WIDTH;
    }

    private int getListLeft() {
        return (this.width - LIST_WIDTH) / 2;
    }

    private void saveOrder() {
        List<String> ids = new ArrayList<>();
        for (OverlayMetric m : order)
            ids.add(m.getId());
        config.hud.metricOrder = ids;
    }

    private void showToast(String translationKey) {
        this.toastMessage = translationKey;
        this.toastTicks = TOAST_DURATION_TICKS;
    }

    // ── Inner record ──────────────────────────────────────────────────────────
    private record MetricRowWidgets(Button toggleButton, EditBox renameBox, Button resetButton) {
    }
}