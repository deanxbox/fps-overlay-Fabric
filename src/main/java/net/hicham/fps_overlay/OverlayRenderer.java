package net.hicham.fps_overlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Main renderer for the FPS Overlay
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

    private static final Map<String, Text> TEXT_CACHE = new HashMap<>();
    private static boolean textCacheInitialized = false;

    private static final List<OverlayLine> REUSABLE_LINES = new ArrayList<>(6);

    static {
        for (int i = 0; i < 6; i++) {
            REUSABLE_LINES.add(new OverlayLine());
        }
    }

    public static void setConfig(ModConfig configData) {
        config = configData;
    }

    // Main render entry point called by HudRenderCallback
    public static void render(DrawContext ctx, MinecraftClient client) {
        if (config == null || !config.general.enabled)
            return;

        ensureTextCacheInitialized();

        PerformanceTracker tracker = PerformanceTracker.getInstance();
        tracker.recordFrame();

        if (config.appearance.autoHideF3 && client.getDebugHud().shouldShowDebugHud())
            return;

        List<OverlayLine> linesToRender = prepareLines();
        if (linesToRender.isEmpty())
            return;

        float scale = config.appearance.hudScale;
        Object matricesRaw = ctx.getMatrices();

        if (matricesRaw instanceof MatrixStack matrices) {
            matrices.push();
            matrices.scale(scale, scale, 1.0f);
            renderLines(ctx, client, linesToRender);
            matrices.pop();
        } else if (matricesRaw instanceof Matrix3x2fStack matrices) {
            matrices.pushMatrix();
            matrices.scale(scale, scale);
            renderLines(ctx, client, linesToRender);
            matrices.popMatrix();
        } else {
            renderLines(ctx, client, linesToRender);
        }
    }

    private static void ensureTextCacheInitialized() {
        if (!textCacheInitialized) {
            TEXT_CACHE.put("fps", Text.translatable("text.fps_overlay.fps"));
            TEXT_CACHE.put("avg_fps", Text.translatable("text.fps_overlay.avg_fps"));
            TEXT_CACHE.put("memory", Text.translatable("text.fps_overlay.memory"));
            TEXT_CACHE.put("ping", Text.translatable("text.fps_overlay.ping"));
            TEXT_CACHE.put("ms", Text.translatable("text.fps_overlay.ms"));
            TEXT_CACHE.put("gb", Text.translatable("text.fps_overlay.gb"));
            TEXT_CACHE.put("tps", Text.translatable("text.fps_overlay.tps"));
            TEXT_CACHE.put("mspt", Text.translatable("text.fps_overlay.mspt"));
            TEXT_CACHE.put("low", Text.translatable("text.fps_overlay.1percent_low"));

            textCacheInitialized = true;
        }
    }

    // Prepare the list of lines to be rendered based on user configuration
    private static List<OverlayLine> prepareLines() {
        List<OverlayLine> activeLines = new ArrayList<>(4);
        PerformanceTracker tracker = PerformanceTracker.getInstance();
        int lineIndex = 0;

        if (config.hud.showFps) {
            OverlayLine line = REUSABLE_LINES.get(lineIndex++);
            line.reset();
            line.type = "fps";
            updateFpsLine(line, tracker);
            activeLines.add(line);
        }

        if (config.hud.showAverageFps) {
            OverlayLine line = REUSABLE_LINES.get(lineIndex++);
            line.reset();
            line.type = "avg";
            updateAverageFpsLine(line, tracker);
            activeLines.add(line);
        }

        if (config.hud.showMemory) {
            OverlayLine line = REUSABLE_LINES.get(lineIndex++);
            line.reset();
            line.type = "memory";
            updateMemoryLine(line, tracker);
            activeLines.add(line);
        }

        if (config.hud.showPing) {
            OverlayLine line = REUSABLE_LINES.get(lineIndex++);
            line.reset();
            line.type = "ping";
            updatePingLine(line, tracker);
            activeLines.add(line);
        }

        if (config.hud.show1PercentLow) {
            OverlayLine line = REUSABLE_LINES.size() > lineIndex ? REUSABLE_LINES.get(lineIndex++) : new OverlayLine();
            line.reset();
            line.type = "low";
            line.addPart(Text.literal(String.valueOf(tracker.getOnePercentLow())));
            activeLines.add(line);
        }

        if (config.hud.showMspt) {
            OverlayLine line = REUSABLE_LINES.size() > lineIndex ? REUSABLE_LINES.get(lineIndex++) : new OverlayLine();
            line.reset();
            line.type = "mspt";
            double mspt = tracker.getMspt();
            line.addPart(Text.literal(MEMORY_FORMAT.get().format(mspt)));
            activeLines.add(line);
        }

        if (config.hud.showTps) {
            OverlayLine line = REUSABLE_LINES.size() > lineIndex ? REUSABLE_LINES.get(lineIndex++) : new OverlayLine();
            line.reset();
            line.type = "tps";
            double tps = tracker.getTps();
            line.addPart(Text.literal(MEMORY_FORMAT.get().format(tps)));
            activeLines.add(line);
        }

        return activeLines;
    }

    private static void updateFpsLine(OverlayLine line, PerformanceTracker tracker) {
        int fps = tracker.getCurrentFps();
        line.addPart(Text.literal(String.valueOf(fps)));
    }

    private static void updateAverageFpsLine(OverlayLine line, PerformanceTracker tracker) {
        double actualValue = tracker.getAverageFps();
        line.addPart(Text.literal(AVG_FPS_FORMAT.get().format(actualValue)));
    }

    private static void updateMemoryLine(OverlayLine line, PerformanceTracker tracker) {
        long used = tracker.getUsedMemory();
        long max = tracker.getMaxMemory();

        if (max == 0) {
            line.addPart(Text.literal("N/A"));
            return;
        }

        double usedGB = used / (1024.0 * 1024.0 * 1024.0);
        line.addPart(Text.literal(MEMORY_FORMAT.get().format(usedGB)));
    }

    private static void updatePingLine(OverlayLine line, PerformanceTracker tracker) {
        int ping = tracker.getCurrentPing();
        line.addPart(Text.literal(String.valueOf(ping)));
    }

    // Route to the appropriate style renderer
    private static void renderLines(DrawContext ctx, MinecraftClient client, List<OverlayLine> segments) {
        float scale = config.appearance.hudScale;
        int sw = (int) (ctx.getScaledWindowWidth() / scale);
        int sh = (int) (ctx.getScaledWindowHeight() / scale);

        if (config.appearance.overlayStyle == ModConfig.OverlayStyle.NAVBAR) {
            renderNavbar(ctx, client, segments, sw, sh);
        } else {
            renderDefault(ctx, client, segments, sw, sh);
        }
    }

    // Horizontal Navbar Style Renderer
    private static void renderNavbar(DrawContext ctx, MinecraftClient client, List<OverlayLine> lines, int screenWidth,
            int screenHeight) {
        TextRenderer renderer = client.textRenderer;

        List<Text> segments = new ArrayList<>();
        int totalWidth = 0;
        int paddingX = 5;

        for (int i = 0; i < lines.size(); i++) {
            OverlayLine line = lines.get(i);
            String label = getLabelForType(line.type);
            String value = line.parts.size() > 0 ? line.parts.get(0).text.getString() : "0";
            String unit = getUnitForType(line.type);

            // Construct part: "LABEL Value unit"
            segments.add(Text.literal(label + " ").withColor(COLOR_CLEAN_LABEL));

            int valueColor = getAdaptiveColor(line.type, value);
            segments.add(Text.literal(value).withColor(valueColor));

            segments.add(Text.literal(unit).withColor(COLOR_CLEAN_UNIT));

            if (i < lines.size() - 1) {
                segments.add(Text.literal(" | ").withColor(COLOR_CLEAN_DIVIDER));
            }
        }

        for (Text t : segments) {
            totalWidth += renderer.getWidth(t);
        }

        int lineHeight = renderer.fontHeight + 3;

        int x = calculateX(screenWidth, totalWidth + (paddingX * 2));
        int y = calculateY(screenHeight, lineHeight);

        if (config.appearance.showBackground) {
            int opacity = config.appearance.backgroundOpacity;
            int color = (opacity << 24) | (COLOR_CLEAN_BG & 0xFFFFFF);
            drawRoundedRect(ctx, x, y, totalWidth + (paddingX * 2), lineHeight, 4, color);
        }

        int currentX = x + paddingX;
        int textY = y + 2;
        for (Text t : segments) {
            ctx.drawText(renderer, t, currentX, textY, 0xFFFFFFFF, false);
            currentX += renderer.getWidth(t);
        }
    }

    // Vertical Box Style Renderer (Default)
    private static void renderDefault(DrawContext ctx, MinecraftClient client, List<OverlayLine> lines, int screenWidth,
            int screenHeight) {
        TextRenderer renderer = client.textRenderer;

        int verticalPadding = 1;
        int lineHeight = renderer.fontHeight + verticalPadding;
        int maxWidth = 80;

        int dividerExtra = (lines.size() > 2) ? 3 : 0;
        int totalHeight = (lines.size() * lineHeight) + dividerExtra;

        int x = calculateX(screenWidth, maxWidth);
        int y = calculateY(screenHeight, totalHeight);

        if (config.appearance.showBackground) {
            int opacity = config.appearance.backgroundOpacity;
            int color = (opacity << 24) | (COLOR_CLEAN_BG & 0xFFFFFF);
            int pad = 3;
            drawRoundedRect(ctx, x - pad, y - pad, maxWidth + (pad * 2), totalHeight + (pad * 2), 4, color);
        }

        for (int i = 0; i < lines.size(); i++) {
            OverlayLine line = lines.get(i);
            int dividerOffset = (i > 1) ? 4 : 0;
            int lineY = y + (i * lineHeight) + dividerOffset;

            renderDefaultRow(ctx, renderer, line, x, lineY);

            if (i == 1 && lines.size() > 2) {
                int dividerY = lineY + renderer.fontHeight + 1;
                ctx.fill(x, dividerY, x + maxWidth, dividerY + 1, COLOR_CLEAN_DIVIDER);
            }
        }
    }

    private static void renderDefaultRow(DrawContext ctx, TextRenderer renderer, OverlayLine line, int x, int y) {
        String label = switch (line.type) {
            case "fps" -> "Fps";
            case "avg" -> "Avg";
            case "low" -> "1%Low";
            case "memory" -> "Ram";
            case "ping" -> "Ping";
            case "mspt" -> "Mspt";
            case "tps" -> "Tps";
            default -> "";
        };

        boolean shadow = false;
        int labelX = x + 3;
        int valueRightX = x + 58;
        int unitLeftX = x + 61;

        ctx.drawText(renderer, label, labelX, y, COLOR_CLEAN_LABEL, shadow);

        String val = line.parts.size() > 0 ? line.parts.get(0).text.getString() : "0";
        int valWidth = renderer.getWidth(val);
        int unitColor = COLOR_CLEAN_UNIT;

        if ("ping".equals(line.type)) {
            int color = getAdaptiveColor(line.type, val);
            ctx.drawText(renderer, val, valueRightX - valWidth, y, color, shadow);
            renderScaledText(ctx, renderer, "ms", unitLeftX, y, unitColor, 0.65f, shadow);
        } else if ("memory".equals(line.type)) {
            int color = getAdaptiveColor(line.type, val);
            ctx.drawText(renderer, val, valueRightX - valWidth, y, color, shadow);
            renderScaledText(ctx, renderer, "gb", unitLeftX, y, unitColor, 0.65f, shadow);
        } else if ("mspt".equals(line.type)) {
            int color = getAdaptiveColor(line.type, val);
            ctx.drawText(renderer, val, valueRightX - valWidth, y, color, shadow);
            renderScaledText(ctx, renderer, "ms", unitLeftX, y, unitColor, 0.65f, shadow);
        } else if ("tps".equals(line.type)) {
            int color = getAdaptiveColor(line.type, val);
            ctx.drawText(renderer, val, valueRightX - valWidth, y, color, shadow);
            renderScaledText(ctx, renderer, "tps", unitLeftX, y, unitColor, 0.65f, shadow);
        } else {
            int color = getAdaptiveColor(line.type, val);
            ctx.drawText(renderer, val, valueRightX - valWidth, y, color, shadow);
            renderScaledText(ctx, renderer, "fps", unitLeftX, y, unitColor, 0.65f, shadow);
        }
    }

    // Determine text color based on performance thresholds
    private static int getAdaptiveColor(String type, String value) {
        if (!config.appearance.adaptiveColors)
            return COLOR_CLEAN_VALUE;

        try {
            double val = Double.parseDouble(value.replace(",", "."));
            return switch (type) {
                case "fps", "avg", "low" -> (val >= 60) ? COLOR_GOOD : (val >= 30 ? COLOR_MID : COLOR_BAD);
                case "ping" -> (val < 60) ? COLOR_GOOD : (val < 150 ? COLOR_MID : COLOR_BAD);
                case "mspt" -> (val < 30) ? COLOR_GOOD : (val < 45 ? COLOR_MID : COLOR_BAD);
                case "tps" -> (val >= 19.5) ? COLOR_GOOD : (val >= 15 ? COLOR_MID : COLOR_BAD);
                case "memory" -> {
                    // Memory is tougher because we need the percentage
                    PerformanceTracker tracker = PerformanceTracker.getInstance();
                    double pct = (double) tracker.getUsedMemory() / tracker.getMaxMemory();
                    yield (pct < 0.75) ? COLOR_GOOD : (pct < 0.9 ? COLOR_MID : COLOR_BAD);
                }
                default -> COLOR_CLEAN_VALUE;
            };
        } catch (Exception e) {
            return COLOR_CLEAN_VALUE;
        }
    }

    private static void renderScaledText(DrawContext ctx, TextRenderer renderer, String text, int x, int y, int color,
            float scale, boolean shadow) {
        Object matricesRaw = ctx.getMatrices();
        if (matricesRaw instanceof MatrixStack matrices) {
            matrices.push();
            matrices.translate(x, y + 2.5f, 0);
            matrices.scale(scale, scale, 1.0f);
            ctx.drawText(renderer, text, 0, 0, color, shadow);
            matrices.pop();
        } else if (matricesRaw instanceof Matrix3x2fStack matrices) {
            matrices.pushMatrix();
            matrices.translate(x, y + 2.5f);
            matrices.scale(scale, scale);
            ctx.drawText(renderer, text, 0, 0, color, shadow);
            matrices.popMatrix();
        } else {
            ctx.drawText(renderer, text, x, y, color, shadow);
        }
    }

    // Helper to draw background rectangles with rounded corners
    private static void drawRoundedRect(DrawContext ctx, int x, int y, int width, int height, int radius, int color) {
        ctx.fill(x + radius, y, x + width - radius, y + height, color);
        ctx.fill(x, y + radius, x + radius, y + height - radius, color);
        ctx.fill(x + width - radius, y + radius, x + width, y + height - radius, color);

        drawCorner(ctx, x, y, radius, radius, true, true, color);
        drawCorner(ctx, x + width - radius, y, radius, radius, false, true, color);
        drawCorner(ctx, x, y + height - radius, radius, radius, true, false, color);
        drawCorner(ctx, x + width - radius, y + height - radius, radius, radius, false, false, color);
    }

    private static void drawCorner(DrawContext ctx, int x, int y, int w, int h, boolean left, boolean top, int color) {
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int dx = left ? (w - 1 - i) : i;
                int dy = top ? (h - 1 - j) : j;
                if (dx * dx + dy * dy <= w * w) {
                    ctx.fill(x + i, y + j, x + i + 1, y + j + 1, color);
                }
            }
        }
    }

    private static String getLabelForType(String type) {
        return switch (type) {
            case "fps" -> "Fps";
            case "avg" -> "Avg";
            case "low" -> "1%Low";
            case "memory" -> "Mem";
            case "ping" -> "Ping";
            case "mspt" -> "Mspt";
            case "tps" -> "Tps";
            default -> "";
        };
    }

    private static String getUnitForType(String type) {
        return switch (type) {
            case "fps", "avg", "low", "1%low" -> " fps";
            case "memory" -> " gb";
            case "ping" -> " ms";
            case "mspt" -> " ms";
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

    // Inner classes
    // Helper structure for rendering lines
    private static class OverlayLine {
        final List<TextPart> parts = new ArrayList<>(5);
        String type = "";

        void reset() {
            parts.clear();
            type = "";
        }

        void addPart(Text text) {
            parts.add(new TextPart(text));
        }
    }

    private static class TextPart {
        final Text text;

        TextPart(Text text) {
            this.text = text;
        }
    }
}