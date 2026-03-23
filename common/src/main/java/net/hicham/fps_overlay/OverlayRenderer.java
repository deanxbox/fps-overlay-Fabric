package net.hicham.fps_overlay;

import org.joml.Matrix3x2fStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Main renderer for the FPS Overlay. Uses Mojang mappings.
public class OverlayRenderer {
    private static ModConfig config;

    private static final int COLOR_CLEAN_BG = 0xF0212B36;
    private static final int COLOR_CLEAN_VALUE = 0xFFFFFFFF;
    private static final int COLOR_CLEAN_LABEL = 0xFF839DB1;
    private static final int COLOR_CLEAN_UNIT = 0xFFC99566;
    private static final int COLOR_CLEAN_DIVIDER = 0xFF354451;

    private static final int COLOR_GOOD = 0xFF2ED177;
    private static final int COLOR_MID = 0xFFFFD100;
    private static final int COLOR_BAD = 0xFFFF4545;

    @SuppressWarnings("null")
    private static final ThreadLocal<DecimalFormat> MEMORY_FORMAT = java.util.Objects.requireNonNull(ThreadLocal
            .withInitial(() -> new DecimalFormat("0.0")));

    @SuppressWarnings("null")
    private static final ThreadLocal<DecimalFormat> AVG_FPS_FORMAT = java.util.Objects.requireNonNull(ThreadLocal
            .withInitial(() -> new DecimalFormat("0")));

    private static final Map<String, Component> TEXT_CACHE = new HashMap<>();
    private static boolean textCacheInitialized = false;

    private static final List<OverlayLine> REUSABLE_LINES = new ArrayList<>(7);

    static {
        for (int i = 0; i < 7; i++) {
            REUSABLE_LINES.add(new OverlayLine());
        }
    }

    public static void setConfig(ModConfig configData) {
        config = configData;
    }

    // Main render entry point
    public static void render(GuiGraphics guiGraphics, Minecraft minecraft) {
        if (config == null || !config.general.enabled)
            return;

        ensureTextCacheInitialized();

        PerformanceTracker.getInstance().recordFrame();

        if (config.appearance.autoHideF3 && minecraft.getDebugOverlay().showDebugScreen())
            return;

        List<OverlayLine> linesToRender = prepareLines();
        if (linesToRender.isEmpty())
            return;

        float scale = config.appearance.hudScale;
        Matrix3x2fStack poseStack = guiGraphics.pose();

        poseStack.pushMatrix();
        poseStack.scale(scale, scale);
        renderLines(guiGraphics, minecraft, linesToRender);
        poseStack.popMatrix();
    }

    private static void ensureTextCacheInitialized() {
        if (textCacheInitialized)
            return;
        TEXT_CACHE.put("fps", Component.translatable("text.fps_overlay.fps"));
        TEXT_CACHE.put("avg", Component.translatable("text.fps_overlay.avg_fps"));
        TEXT_CACHE.put("memory", Component.translatable("text.fps_overlay.memory"));
        TEXT_CACHE.put("ping", Component.translatable("text.fps_overlay.ping"));
        TEXT_CACHE.put("ms", Component.translatable("text.fps_overlay.ms"));
        TEXT_CACHE.put("gb", Component.translatable("text.fps_overlay.gb"));
        TEXT_CACHE.put("tps", Component.translatable("text.fps_overlay.tps"));
        TEXT_CACHE.put("mspt", Component.translatable("text.fps_overlay.mspt"));
        TEXT_CACHE.put("low", Component.translatable("text.fps_overlay.1percent_low"));
        textCacheInitialized = true;
    }

    private static List<OverlayLine> prepareLines() {
        List<OverlayLine> active = new ArrayList<>();
        PerformanceTracker t = PerformanceTracker.getInstance();
        int idx = 0;

        if (config.hud.showFps) {
            OverlayLine l = getLine(idx++);
            l.type = "fps";
            l.addPart(Component.literal(String.valueOf(t.getCurrentFps())));
            active.add(l);
        }
        if (config.hud.showAverageFps) {
            OverlayLine l = getLine(idx++);
            l.type = "avg";
            l.addPart(Component.literal(AVG_FPS_FORMAT.get().format(t.getAverageFps())));
            active.add(l);
        }
        if (config.hud.show1PercentLow) {
            OverlayLine l = getLine(idx++);
            l.type = "low";
            l.addPart(Component.literal(String.valueOf(t.getOnePercentLow())));
            active.add(l);
        }
        if (config.hud.showMemory) {
            OverlayLine l = getLine(idx++);
            l.type = "memory";
            if (t.getMaxMemory() == 0) {
                l.addPart(Component.literal("N/A"));
            } else {
                double gb = t.getUsedMemory() / (1024.0 * 1024.0 * 1024.0);
                l.addPart(Component.literal(MEMORY_FORMAT.get().format(gb)));
            }
            active.add(l);
        }
        if (config.hud.showPing) {
            OverlayLine l = getLine(idx++);
            l.type = "ping";
            l.addPart(Component.literal(String.valueOf(t.getCurrentPing())));
            active.add(l);
        }
        if (config.hud.showMspt) {
            OverlayLine l = getLine(idx++);
            l.type = "mspt";
            l.addPart(Component.literal(MEMORY_FORMAT.get().format(t.getMspt())));
            active.add(l);
        }
        if (config.hud.showTps) {
            OverlayLine l = getLine(idx++);
            l.type = "tps";
            l.addPart(Component.literal(MEMORY_FORMAT.get().format(t.getTps())));
            active.add(l);
        }
        return active;
    }

    private static OverlayLine getLine(int idx) {
        OverlayLine l = idx < REUSABLE_LINES.size() ? REUSABLE_LINES.get(idx) : new OverlayLine();
        l.reset();
        return l;
    }

    private static void renderLines(GuiGraphics guiGraphics, Minecraft minecraft, List<OverlayLine> segments) {
        float scale = config.appearance.hudScale;
        int sw = (int) (guiGraphics.guiWidth() / scale);
        int sh = (int) (guiGraphics.guiHeight() / scale);

        if (config.appearance.overlayStyle == ModConfig.OverlayStyle.NAVBAR) {
            renderNavbar(guiGraphics, minecraft, segments, sw, sh);
        } else {
            renderDefault(guiGraphics, minecraft, segments, sw, sh);
        }
    }

    private static void renderNavbar(GuiGraphics guiGraphics, Minecraft minecraft, List<OverlayLine> lines,
            int screenWidth, int screenHeight) {
        Font font = minecraft.font;
        List<Component> segments = new ArrayList<>();
        int totalWidth = 0;
        int paddingX = 5;

        for (int i = 0; i < lines.size(); i++) {
            OverlayLine line = lines.get(i);
            String label = getLabelForType(line.type);
            String value = line.parts.isEmpty() ? "0" : line.parts.get(0).text.getString();
            String unit = getUnitForType(line.type);

            segments.add(Component.literal(label + " ").withColor(COLOR_CLEAN_LABEL));
            segments.add(Component.literal(value).withColor(getAdaptiveColor(line.type, value)));
            segments.add(Component.literal(unit).withColor(COLOR_CLEAN_UNIT));

            if (i < lines.size() - 1) {
                segments.add(Component.literal(" | ").withColor(COLOR_CLEAN_DIVIDER));
            }
        }

        for (Component t : segments) totalWidth += font.width(t);

        int lineHeight = font.lineHeight + 3;
        int x = calculateX(screenWidth, totalWidth + (paddingX * 2));
        int y = calculateY(screenHeight, lineHeight);

        if (config.appearance.showBackground) {
            int opacity = config.appearance.backgroundOpacity;
            int color = (opacity << 24) | (COLOR_CLEAN_BG & 0xFFFFFF);
            drawRoundedRect(guiGraphics, x, y, totalWidth + (paddingX * 2), lineHeight, 4, color);
        }

        int currentX = x + paddingX;
        int textY = y + 2;
        for (Component t : segments) {
            guiGraphics.drawString(font, t, currentX, textY, 0xFFFFFFFF, false);
            currentX += font.width(t);
        }
    }

    private static void renderDefault(GuiGraphics guiGraphics, Minecraft minecraft, List<OverlayLine> lines,
            int screenWidth, int screenHeight) {
        Font font = minecraft.font;
        int verticalPadding = 1;
        int lineHeight = font.lineHeight + verticalPadding;
        int maxWidth = 80;

        int dividerExtra = (lines.size() > 2) ? 3 : 0;
        int totalHeight = (lines.size() * lineHeight) + dividerExtra;

        int x = calculateX(screenWidth, maxWidth);
        int y = calculateY(screenHeight, totalHeight);

        if (config.appearance.showBackground) {
            int opacity = config.appearance.backgroundOpacity;
            int color = (opacity << 24) | (COLOR_CLEAN_BG & 0xFFFFFF);
            int pad = 3;
            drawRoundedRect(guiGraphics, x - pad, y - pad, maxWidth + (pad * 2), totalHeight + (pad * 2), 4, color);
        }

        for (int i = 0; i < lines.size(); i++) {
            int dividerOffset = (i > 1) ? 3 : 0;
            int lineY = y + (i * lineHeight) + dividerOffset;
            renderDefaultRow(guiGraphics, font, lines.get(i), x, lineY);

            if (i == 1 && lines.size() > 2) {
                int dividerY = lineY + font.lineHeight + 1;
                guiGraphics.fill(x, dividerY, x + maxWidth, dividerY + 1, COLOR_CLEAN_DIVIDER);
            }
        }
    }

    private static void renderDefaultRow(GuiGraphics guiGraphics, Font font, OverlayLine line, int x, int y) {
        String label = getLabelForType(line.type);
        guiGraphics.drawString(font, label, x + 3, y, COLOR_CLEAN_LABEL, false);

        String val = line.parts.isEmpty() ? "0" : line.parts.get(0).text.getString();
        int color = getAdaptiveColor(line.type, val);
        guiGraphics.drawString(font, val, x + 58 - font.width(val), y, color, false);

        String unit = getUnitForType(line.type).trim();
        renderScaledText(guiGraphics, font, unit, x + 61, y, COLOR_CLEAN_UNIT, 0.65f, false);
    }

    private static int getAdaptiveColor(String type, String value) {
        if (!config.appearance.adaptiveColors) return COLOR_CLEAN_VALUE;
        try {
            double val = Double.parseDouble(value.replace(",", "."));
            return switch (type) {
                case "fps", "avg", "low" -> (val >= 60) ? COLOR_GOOD : (val >= 30 ? COLOR_MID : COLOR_BAD);
                case "ping" -> (val < 60) ? COLOR_GOOD : (val < 150 ? COLOR_MID : COLOR_BAD);
                case "mspt" -> (val < 30) ? COLOR_GOOD : (val < 45 ? COLOR_MID : COLOR_BAD);
                case "tps" -> (val >= 19.5) ? COLOR_GOOD : (val >= 15 ? COLOR_MID : COLOR_BAD);
                case "memory" -> {
                    PerformanceTracker tracker = PerformanceTracker.getInstance();
                    double pct = (double) tracker.getUsedMemory() / tracker.getMaxMemory();
                    yield (pct < 0.75) ? COLOR_GOOD : (pct < 0.9 ? COLOR_MID : COLOR_BAD);
                }
                default -> COLOR_CLEAN_VALUE;
            };
        } catch (Exception e) { return COLOR_CLEAN_VALUE; }
    }

    private static void renderScaledText(GuiGraphics guiGraphics, Font font, String text, int x, int y, int color,
            float scale, boolean shadow) {
        Matrix3x2fStack poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        poseStack.translate(x, y + 2.5f);
        poseStack.scale(scale, scale);
        guiGraphics.drawString(font, text, 0, 0, color, shadow);
        poseStack.popMatrix();
    }

    private static void drawRoundedRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int color) {
        guiGraphics.fill(x + radius, y, x + width - radius, y + height, color);
        guiGraphics.fill(x, y + radius, x + radius, y + height - radius, color);
        guiGraphics.fill(x + width - radius, y + radius, x + width, y + height - radius, color);
        drawCorner(guiGraphics, x, y, radius, radius, true, true, color);
        drawCorner(guiGraphics, x + width - radius, y, radius, radius, false, true, color);
        drawCorner(guiGraphics, x, y + height - radius, radius, radius, true, false, color);
        drawCorner(guiGraphics, x + width - radius, y + height - radius, radius, radius, false, false, color);
    }

    private static void drawCorner(GuiGraphics guiGraphics, int x, int y, int w, int h, boolean left, boolean top, int color) {
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int dx = left ? (w - 1 - i) : i;
                int dy = top ? (h - 1 - j) : j;
                if (dx * dx + dy * dy <= w * w) {
                    guiGraphics.fill(x + i, y + j, x + i + 1, y + j + 1, color);
                }
            }
        }
    }

    private static String getLabelForType(String type) {
        return switch (type) {
            case "fps" -> "Fps";
            case "avg" -> "Avg";
            case "low" -> "1%Low";
            case "memory" -> "Ram";
            case "ping" -> "Ping";
            case "mspt" -> "Mspt";
            case "tps" -> "Tps";
            default -> "";
        };
    }

    private static String getUnitForType(String type) {
        return switch (type) {
            case "fps", "avg", "low" -> " fps";
            case "memory" -> " gb";
            case "ping", "mspt" -> " ms";
            case "tps" -> " tps";
            default -> "";
        };
    }

    private static int calculateX(int screenWidth, int totalWidth) {
        int margin = 4;
        return switch (config.appearance.position) {
            case TOP_LEFT, BOTTOM_LEFT -> margin;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - totalWidth - margin;
            case TOP_CENTER -> (screenWidth - totalWidth) / 2;
        };
    }

    private static int calculateY(int screenHeight, int totalHeight) {
        int margin = 4;
        return switch (config.appearance.position) {
            case TOP_LEFT, TOP_RIGHT, TOP_CENTER -> margin;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - totalHeight - margin;
        };
    }

    private static class OverlayLine {
        final List<TextPart> parts = new ArrayList<>(3);
        String type = "";
        void reset() { parts.clear(); type = ""; }
        void addPart(Component text) { parts.add(new TextPart(text)); }
    }

    private static class TextPart {
        final Component text;
        TextPart(Component text) { this.text = text; }
    }
}