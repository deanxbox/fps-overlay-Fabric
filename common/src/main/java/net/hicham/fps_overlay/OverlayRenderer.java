package net.hicham.fps_overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OverlayRenderer {
    private static final DecimalFormat ONE_DECIMAL = new DecimalFormat("0.0");
    private static final DecimalFormat WHOLE_NUMBER = new DecimalFormat("0");
    private static final int PANEL_PADDING = 6;
    private static final int GRAPH_HEIGHT = 24;
    private static final int GRAPH_GAP = 6;
    private static final int MEMORY_BAR_WIDTH = 54;
    private static final int MEMORY_BAR_HEIGHT = 7;
    private static final int TRANSLATION_CACHE_LIMIT = 128;

    private static ModConfig config;
    private static final Map<String, String> TRANSLATION_CACHE = new LinkedHashMap<>(32, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > TRANSLATION_CACHE_LIMIT;
        }
    };
    private static ModConfig cachedLiveConfig;
    private static long cachedLiveVersion = Long.MIN_VALUE;
    private static List<OverlayLine> cachedLiveLines = List.of();
    private static ModConfig cachedLiveLayoutConfig;
    private static int cachedLiveLayoutSignature = 0;
    private static int cachedLiveScreenWidth = -1;
    private static int cachedLiveScreenHeight = -1;
    private static OverlayLayout cachedLiveLayout;

    public static void setConfig(ModConfig configData) {
        config = configData;
        invalidateLiveCaches();
    }

    public static void render(GuiGraphics context, Minecraft client) {
        if (config == null || !config.general.enabled) {
            return;
        }
        PerformanceTracker tracker = PerformanceTracker.getInstance();
        tracker.recordFrame();
        long dataVersion = getRenderDataVersion(config, tracker);

        List<OverlayLine> lines = getPreparedLines(config, tracker, false, dataVersion);
        if (lines.isEmpty() && !config.hud.showGraph && !config.hud.showPingGraph) {
            return;
        }

        float scale = config.appearance.hudScale;
        applyScale(context, scale, () -> renderScaled(context, client, config, lines, false, dataVersion));
    }

    public static void renderPreview(GuiGraphics context, Minecraft client, ModConfig previewConfig, int screenWidth,
            int screenHeight) {
        if (previewConfig == null) {
            return;
        }

        List<OverlayLine> lines = getPreparedLines(previewConfig, PerformanceTracker.getInstance(), true, -1);
        applyScale(context, previewConfig.appearance.hudScale,
                () -> renderScaled(context, client, previewConfig, lines, true, -1));
    }

    public static LayoutBounds getPreviewBounds(int screenWidth, int screenHeight, ModConfig previewConfig) {
        Minecraft client = Minecraft.getInstance();
        Font font = client != null ? client.font : null;
        List<OverlayLine> lines = getPreparedLines(previewConfig, PerformanceTracker.getInstance(), true, -1);
        LayoutBounds logical = getPreviewLogicalBounds(screenWidth, screenHeight, previewConfig, font, lines);
        float scale = previewConfig.appearance.hudScale;
        return new LayoutBounds(Math.round(logical.x() * scale), Math.round(logical.y() * scale),
                Math.round(logical.width() * scale), Math.round(logical.height() * scale));
    }

    public static LayoutBounds getPreviewLogicalBounds(int screenWidth, int screenHeight, ModConfig previewConfig) {
        Minecraft client = Minecraft.getInstance();
        Font font = client != null ? client.font : null;
        List<OverlayLine> lines = getPreparedLines(previewConfig, PerformanceTracker.getInstance(), true, -1);
        return getPreviewLogicalBounds(screenWidth, screenHeight, previewConfig, font, lines);
    }

    public static AnchorPoint getAnchorPoint(int screenWidth, int screenHeight, int contentWidth, int contentHeight,
            ModConfig.OverlayPosition position) {
        int x = switch (position) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> 4;
            case TOP_CENTER, BOTTOM_CENTER -> (screenWidth - contentWidth) / 2;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> screenWidth - contentWidth - 4;
        };

        int y = switch (position) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 4;
            case CENTER_LEFT, CENTER_RIGHT -> (screenHeight - contentHeight) / 2;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenHeight - contentHeight - 4;
        };

        return new AnchorPoint(x, y);
    }

    private static LayoutBounds getPreviewLogicalBounds(int screenWidth, int screenHeight, ModConfig previewConfig,
            Font font, List<OverlayLine> lines) {
        float scale = previewConfig.appearance.hudScale;
        int logicalWidth = Math.max(1, Math.round(screenWidth / scale));
        int logicalHeight = Math.max(1, Math.round(screenHeight / scale));
        return measureLayout(font, previewConfig, lines, logicalWidth, logicalHeight, true).bounds();
    }

    private static void renderScaled(GuiGraphics context, Minecraft client, ModConfig activeConfig,
            List<OverlayLine> lines, boolean preview, long dataVersion) {
        Minecraft resolvedClient = client != null ? client : Minecraft.getInstance();
        Font font = resolvedClient != null ? resolvedClient.font : null;
        if (font == null) {
            return;
        }

        int screenWidth = (int) (context.guiWidth() / activeConfig.appearance.hudScale);
        int screenHeight = (int) (context.guiHeight() / activeConfig.appearance.hudScale);
        OverlayLayout layout = preview
                ? measureLayout(font, activeConfig, lines, screenWidth, screenHeight, true)
                : getLiveLayout(font, activeConfig, lines, screenWidth, screenHeight);
        LayoutBounds bounds = layout.bounds();
        float fadeAlpha = preview ? 1.0f : FpsOverlayMod.getOverlayFadeAlpha();

        if (activeConfig.appearance.showBackground) {
            drawRoundedRect(context, bounds.x(), bounds.y(), bounds.width(), bounds.height(), 4,
                    applyAlpha(getBackgroundColor(activeConfig), fadeAlpha));
        }

        if (activeConfig.appearance.overlayStyle == ModConfig.OverlayStyle.NAVBAR) {
            int maxContentWidth = Math.max(40, bounds.width() - (PANEL_PADDING * 2));
            renderNavbar(context, font, activeConfig, layoutNavbarRows(font, lines, maxContentWidth), bounds, fadeAlpha);
        } else {
            renderVertical(context, font, activeConfig, lines, bounds, fadeAlpha);
        }

        int nextY = bounds.y() + bounds.height() - PANEL_PADDING;
        if (layout.showPingGraph()) {
            nextY -= GRAPH_HEIGHT;
            renderGraph(context, font, activeConfig, bounds, nextY,
                    preview ? getPreviewPingGraphValues() : PerformanceTracker.getInstance().copyPingGraphValues(),
                    false, fadeAlpha);
            nextY -= GRAPH_GAP;
        }
        if (layout.showFpsGraph()) {
            nextY -= GRAPH_HEIGHT;
            renderGraph(context, font, activeConfig, bounds, nextY,
                    preview ? getPreviewGraphValues() : PerformanceTracker.getInstance().copyGraphValues(),
                    true, fadeAlpha);
        }
    }

    private static void renderNavbar(GuiGraphics context, Font font, ModConfig activeConfig, List<NavbarRow> rows,
            LayoutBounds bounds, float fadeAlpha) {
        int lineHeight = font.lineHeight + 2;
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            NavbarRow row = rows.get(rowIndex);
            int x = bounds.x() + PANEL_PADDING;
            int y = bounds.y() + PANEL_PADDING + (rowIndex * lineHeight);
            for (int i = 0; i < row.lines().size(); i++) {
                OverlayLine line = row.lines().get(i);
                drawStyledText(context, font, activeConfig, line.label(), x, y, applyAlpha(getLabelColor(activeConfig), fadeAlpha));
                x += font.width(line.label()) + 4;
                if (line.percentageBar()) {
                    drawMemoryBar(context, activeConfig, line, x, centeredBarY(font, y), fadeAlpha);
                    x += MEMORY_BAR_WIDTH + 4;
                    drawStyledText(context, font, activeConfig, line.value(), x, y,
                            applyAlpha(getAdaptiveColor(activeConfig, line), fadeAlpha));
                    x += font.width(line.value());
                } else {
                    drawStyledText(context, font, activeConfig, line.value(), x, y,
                            applyAlpha(getAdaptiveColor(activeConfig, line), fadeAlpha));
                    x += font.width(line.value());
                }
                if (!line.unit().isEmpty()) {
                    x += 3;
                    drawStyledText(context, font, activeConfig, line.unit(), x, y, applyAlpha(getUnitColor(activeConfig), fadeAlpha));
                    x += font.width(line.unit());
                }
                if (i < row.lines().size() - 1) {
                    x += 6;
                    drawStyledText(context, font, activeConfig, "|", x, y, applyAlpha(getDividerColor(activeConfig), fadeAlpha));
                    x += font.width("|") + 6;
                }
            }
        }
    }

    private static void renderVertical(GuiGraphics context, Font font, ModConfig activeConfig, List<OverlayLine> lines,
            LayoutBounds bounds, float fadeAlpha) {
        int lineHeight = font.lineHeight + 2;
        int y = bounds.y() + PANEL_PADDING;
        for (OverlayLine line : lines) {
            int x = bounds.x() + PANEL_PADDING;
            drawStyledText(context, font, activeConfig, line.label(), x, y, applyAlpha(getLabelColor(activeConfig), fadeAlpha));
            x += font.width(line.label()) + 4;
            if (line.percentageBar()) {
                drawMemoryBar(context, activeConfig, line, x, centeredBarY(font, y), fadeAlpha);
                x += MEMORY_BAR_WIDTH + 4;
                drawStyledText(context, font, activeConfig, line.value(), x, y,
                        applyAlpha(getAdaptiveColor(activeConfig, line), fadeAlpha));
                x += font.width(line.value());
            } else {
                drawStyledText(context, font, activeConfig, line.value(), x, y,
                        applyAlpha(getAdaptiveColor(activeConfig, line), fadeAlpha));
                x += font.width(line.value());
            }
            if (!line.unit().isEmpty()) {
                x += 3;
                drawStyledText(context, font, activeConfig, line.unit(), x, y, applyAlpha(getUnitColor(activeConfig), fadeAlpha));
            }
            y += lineHeight;
        }
    }

    private static void renderGraph(GuiGraphics context, Font font, ModConfig activeConfig, LayoutBounds bounds,
            int graphTop, int[] values, boolean fpsGraph, float fadeAlpha) {
        if (values.length == 0) {
            return;
        }

        int left = bounds.x() + PANEL_PADDING;
        int width = Math.max(2, bounds.width() - (PANEL_PADDING * 2));
        int bottom = graphTop + GRAPH_HEIGHT;
        int background = applyAlpha(0x18000000, fadeAlpha);
        int divider = applyAlpha(getDividerColor(activeConfig), fadeAlpha);
        int accent = fpsGraph ? getGoodColor(activeConfig) : getLabelColor(activeConfig);
        int lineColor = applyAlpha(accent, fadeAlpha);
        int fillColor = applyAlpha(withAlpha(accent, 0x40), fadeAlpha);
        String title = translated(fpsGraph ? "text.fps_overlay.graph_fps" : "text.fps_overlay.graph_ping");

        context.fill(left, graphTop, left + width, bottom, background);
        context.fill(left, graphTop, left + width, graphTop + 1, divider);
        context.fill(left, bottom - 1, left + width, bottom, divider);
        drawStyledText(context, font, activeConfig, title, left + 3, graphTop + 2, lineColor);

        int chartTop = graphTop + 9;
        int chartBottom = bottom - 2;
        int chartHeight = Math.max(4, chartBottom - chartTop);
        int maxValue = fpsGraph ? 60 : 100;
        for (int value : values) {
            maxValue = Math.max(maxValue, value);
        }

        for (int i = 0; i < values.length; i++) {
            int xStart = left + (int) Math.floor((i * width) / (double) values.length);
            int xEnd = left + (int) Math.floor(((i + 1) * width) / (double) values.length);
            xEnd = Math.max(xStart + 1, xEnd);
            xEnd = Math.min(left + width, xEnd);
            int y = chartBottom - 1 - (int) Math.round((values[i] / (double) maxValue) * (chartHeight - 1));
            y = Math.max(chartTop, Math.min(chartBottom - 1, y));

            if (y + 1 < chartBottom) {
                context.fill(xStart, y + 1, xEnd, chartBottom, fillColor);
            }
            context.fill(xStart, y, xEnd, y + 1, lineColor);
        }
    }

    private static void drawMemoryBar(GuiGraphics context, ModConfig activeConfig, OverlayLine line,
            int x, int y, float fadeAlpha) {
        double percentage = line.adaptiveValue() == null ? 0.0 : Math.max(0.0, Math.min(100.0, line.adaptiveValue()));
        int border = applyAlpha(getDividerColor(activeConfig), fadeAlpha);
        int track = applyAlpha(0x44000000, fadeAlpha);
        int fill = applyAlpha(getAdaptiveColor(activeConfig, line), fadeAlpha);
        int fillWidth = (int) Math.round((MEMORY_BAR_WIDTH - 2) * (percentage / 100.0));

        drawRoundedRect(context, x, y, MEMORY_BAR_WIDTH, MEMORY_BAR_HEIGHT, 3, track);
        if (fillWidth > 0) {
            drawRoundedRect(context, x + 1, y + 1, fillWidth, MEMORY_BAR_HEIGHT - 2, 2, fill);
        }
        context.fill(x, y, x + MEMORY_BAR_WIDTH, y + 1, border);
        context.fill(x, y + MEMORY_BAR_HEIGHT - 1, x + MEMORY_BAR_WIDTH, y + MEMORY_BAR_HEIGHT, border);
        context.fill(x, y, x + 1, y + MEMORY_BAR_HEIGHT, border);
        context.fill(x + MEMORY_BAR_WIDTH - 1, y, x + MEMORY_BAR_WIDTH, y + MEMORY_BAR_HEIGHT, border);
    }

    private static int centeredBarY(Font font, int textY) {
        return textY + Math.max(0, (font.lineHeight - MEMORY_BAR_HEIGHT) / 2);
    }

    private static OverlayLayout measureLayout(Font font, ModConfig activeConfig, List<OverlayLine> lines,
            int screenWidth, int screenHeight, boolean preview) {
        if (font == null) {
            AnchorPoint fallback = getAnchorPoint(screenWidth, screenHeight, 180, 64, activeConfig.appearance.position);
            return new OverlayLayout(new LayoutBounds(fallback.x() + activeConfig.appearance.xOffset,
                    fallback.y() + activeConfig.appearance.yOffset, 180, 64), List.of(), false, false);
        }

        int lineHeight = font.lineHeight + 2;
        int textWidth = 0;
        int lineCount = lines.size();
        List<NavbarRow> navbarRows = List.of();

        if (activeConfig.appearance.overlayStyle == ModConfig.OverlayStyle.NAVBAR) {
            int maxContentWidth = Math.max(40, screenWidth - 8 - (PANEL_PADDING * 2));
            navbarRows = layoutNavbarRows(font, lines, maxContentWidth);
            lineCount = Math.max(1, navbarRows.size());
            for (NavbarRow row : navbarRows) {
                textWidth = Math.max(textWidth, row.width());
            }
        } else {
            for (OverlayLine line : lines) {
                textWidth = Math.max(textWidth, measureLineWidth(font, line));
            }
        }

        boolean showFpsGraph = activeConfig.hud.showGraph;
        boolean showPingGraph = activeConfig.hud.showPingGraph;
        int graphHeight = graphStackHeight(showFpsGraph, showPingGraph);
        int contentWidth = Math.max(textWidth, showFpsGraph || showPingGraph ? 140 : 0);
        int width = contentWidth + (PANEL_PADDING * 2);
        int contentHeight = lineCount > 0 ? (lineHeight * lineCount) : lineHeight;
        int height = contentHeight + (PANEL_PADDING * 2) + graphHeight;

        AnchorPoint anchor = getAnchorPoint(screenWidth, screenHeight, width, height, activeConfig.appearance.position);
        return new OverlayLayout(new LayoutBounds(anchor.x() + activeConfig.appearance.xOffset,
                anchor.y() + activeConfig.appearance.yOffset, width, height), navbarRows, showFpsGraph, showPingGraph);
    }

    private static int graphStackHeight(boolean showFpsGraph, boolean showPingGraph) {
        int height = 0;
        if (showFpsGraph) {
            height += GRAPH_HEIGHT;
        }
        if (showPingGraph) {
            if (height > 0) {
                height += GRAPH_GAP;
            }
            height += GRAPH_HEIGHT;
        }
        return height > 0 ? height + GRAPH_GAP : 0;
    }

    private static List<NavbarRow> layoutNavbarRows(Font font, List<OverlayLine> lines, int maxContentWidth) {
        List<NavbarRow> rows = new ArrayList<>();
        if (lines.isEmpty()) {
            return rows;
        }

        int separatorWidth = font.width("|") + 12;
        List<OverlayLine> currentRow = new ArrayList<>();
        int currentWidth = 0;
        for (OverlayLine line : lines) {
            int lineWidth = measureLineWidth(font, line);
            int additional = currentRow.isEmpty() ? lineWidth : separatorWidth + lineWidth;
            if (!currentRow.isEmpty() && currentWidth + additional > maxContentWidth) {
                rows.add(new NavbarRow(List.copyOf(currentRow), currentWidth));
                currentRow.clear();
                currentWidth = 0;
                additional = lineWidth;
            }
            currentRow.add(line);
            currentWidth += additional;
        }
        if (!currentRow.isEmpty()) {
            rows.add(new NavbarRow(List.copyOf(currentRow), currentWidth));
        }
        return rows;
    }

    private static int measureLineWidth(Font font, OverlayLine line) {
        int width = font.width(line.label()) + 4 + font.width(line.value());
        if (line.percentageBar()) {
            width += MEMORY_BAR_WIDTH + 4;
        }
        if (!line.unit().isEmpty()) {
            width += 3 + font.width(line.unit());
        }
        return width;
    }

    private static List<OverlayLine> getPreparedLines(ModConfig activeConfig, PerformanceTracker tracker,
            boolean preview, long dataVersion) {
        if (!preview && activeConfig == cachedLiveConfig && dataVersion == cachedLiveVersion) {
            return cachedLiveLines;
        }

        List<OverlayLine> lines = new ArrayList<>();
        for (OverlayMetric metric : activeConfig.hud.getSanitizedMetricOrder()) {
            if (!activeConfig.hud.isMetricEnabled(metric)) {
                continue;
            }
            OverlayLine line = preview ? createPreviewLine(activeConfig, metric) : createLiveLine(activeConfig, tracker, metric);
            if (line != null) {
                lines.add(line);
            }
        }

        List<OverlayLine> immutableLines = List.copyOf(lines);
        if (!preview) {
            cachedLiveConfig = activeConfig;
            cachedLiveVersion = dataVersion;
            cachedLiveLines = immutableLines;
        }
        return immutableLines;
    }

    private static OverlayLayout getLiveLayout(Font font, ModConfig activeConfig, List<OverlayLine> lines,
            int screenWidth, int screenHeight) {
        int layoutSignature = getLayoutSignature(font, activeConfig, lines);
        if (activeConfig == cachedLiveLayoutConfig
                && layoutSignature == cachedLiveLayoutSignature
                && screenWidth == cachedLiveScreenWidth
                && screenHeight == cachedLiveScreenHeight
                && cachedLiveLayout != null) {
            return cachedLiveLayout;
        }

        OverlayLayout layout = measureLayout(font, activeConfig, lines, screenWidth, screenHeight, false);
        cachedLiveLayoutConfig = activeConfig;
        cachedLiveLayoutSignature = layoutSignature;
        cachedLiveScreenWidth = screenWidth;
        cachedLiveScreenHeight = screenHeight;
        cachedLiveLayout = layout;
        return layout;
    }

    private static int getLayoutSignature(Font font, ModConfig activeConfig, List<OverlayLine> lines) {
        int signature = activeConfig.appearance.overlayStyle.ordinal();
        signature = (signature * 31) + (activeConfig.hud.showGraph ? 1 : 0);
        signature = (signature * 31) + (activeConfig.hud.showPingGraph ? 1 : 0);
        for (OverlayLine line : lines) {
            signature = (signature * 31) + line.metric().ordinal();
            signature = (signature * 31) + measureLineWidth(font, line);
        }
        return signature;
    }

    private static OverlayLine createLiveLine(ModConfig activeConfig, PerformanceTracker tracker, OverlayMetric metric) {
        String metricLabel = activeConfig.hud.getMetricDisplayName(metric);
        boolean liveMetric = isLiveMetric(activeConfig, metric);
        return switch (metric) {
            case FPS -> {
                int fpsValue = liveMetric ? Math.max(0, Minecraft.getInstance().getFps()) : tracker.getCurrentFps();
                yield new OverlayLine(metric, metricLabel,
                        withMinMax(activeConfig, fpsValue, tracker.getMinFps(), tracker.getMaxFps()),
                        translated(metric.getUnitKey()), (double) fpsValue);
            }
            case AVG_FPS -> {
                double averageFps = liveMetric ? tracker.getLiveAverageFps() : tracker.getAverageFps();
                yield new OverlayLine(metric, metricLabel, WHOLE_NUMBER.format(averageFps),
                        translated(metric.getUnitKey()), averageFps);
            }
            case FRAME_TIME -> new OverlayLine(metric, metricLabel,
                    ONE_DECIMAL.format(tracker.getCurrentFrameTimeMs()), translated(metric.getUnitKey()),
                    tracker.getCurrentFrameTimeMs());
            case LOW_1 -> {
                int onePercentLow = liveMetric ? tracker.getLiveOnePercentLow() : tracker.getOnePercentLow();
                yield new OverlayLine(metric, metricLabel,
                        onePercentLow > 0 ? String.valueOf(onePercentLow) : "N/A",
                        translated(metric.getUnitKey()), onePercentLow > 0 ? (double) onePercentLow : null);
            }
            case LOW_01 -> {
                int pointOnePercentLow = liveMetric ? tracker.getLivePointOnePercentLow() : tracker.getPointOnePercentLow();
                yield new OverlayLine(metric, metricLabel,
                        pointOnePercentLow > 0 ? String.valueOf(pointOnePercentLow) : "N/A",
                        translated(metric.getUnitKey()), pointOnePercentLow > 0 ? (double) pointOnePercentLow : null);
            }
            case MEMORY -> createMemoryLine(activeConfig, tracker, metric, metricLabel);
            case PING -> new OverlayLine(metric, metricLabel,
                    withMinMax(activeConfig, tracker.getCurrentPing(), tracker.getMinPing(), tracker.getMaxPing()),
                    translated(metric.getUnitKey()), (double) tracker.getCurrentPing());
            case MSPT -> new OverlayLine(metric, metricLabel,
                    tracker.getMspt() >= 0 ? ONE_DECIMAL.format(tracker.getMspt()) : "N/A",
                    translated(metric.getUnitKey()), tracker.getMspt() >= 0 ? tracker.getMspt() : null);
            case TPS -> new OverlayLine(metric, metricLabel,
                    tracker.getTps() >= 0 ? ONE_DECIMAL.format(tracker.getTps()) : "N/A",
                    translated(metric.getUnitKey()), tracker.getTps() >= 0 ? tracker.getTps() : null);
            case CHUNKS -> new OverlayLine(metric, metricLabel,
                    tracker.getVisibleChunks() > 0 ? tracker.getCompletedChunks() + "/" + tracker.getVisibleChunks()
                            : String.valueOf(tracker.getLoadedChunks()),
                    "", tracker.getVisibleChunks() > 0
                            ? (tracker.getCompletedChunks() * 100.0 / Math.max(1, tracker.getVisibleChunks()))
                            : null);
            case COORDS -> new OverlayLine(metric, metricLabel, tracker.getCoordinatesText(), "", null);
            case BIOME -> new OverlayLine(metric, metricLabel, tracker.getBiomeText(), "", null);
            case DIMENSION -> new OverlayLine(metric, metricLabel, tracker.getDimensionText(), "", null);
            case FACING -> new OverlayLine(metric, metricLabel, tracker.getFacingText(), "", null);
            case CHUNK_COORDS -> new OverlayLine(metric, metricLabel, tracker.getChunkCoordsText(), "", null);
            case LIGHT -> new OverlayLine(metric, metricLabel, tracker.getLightText(), "", null);
            case REAL_TIME -> new OverlayLine(metric, metricLabel, tracker.getRealTimeText(), "", null);
            case DAY_COUNT -> new OverlayLine(metric, metricLabel, tracker.getDayCountText(), "", null);
            case DAY_TIME -> new OverlayLine(metric, metricLabel, tracker.getDayTimeText(), "", null);
            case JITTER -> new OverlayLine(metric, metricLabel, ONE_DECIMAL.format(tracker.getJitterMs()),
                    translated(metric.getUnitKey()), tracker.getJitterMs());
            case PACKET_LOSS -> new OverlayLine(metric, metricLabel,
                    ONE_DECIMAL.format(tracker.getPacketLossPercent()), translated(metric.getUnitKey()),
                    tracker.getPacketLossPercent());
        };
    }

    private static OverlayLine createPreviewLine(ModConfig activeConfig, OverlayMetric metric) {
        String metricLabel = activeConfig.hud.getMetricDisplayName(metric);
        return switch (metric) {
            case FPS -> new OverlayLine(metric, metricLabel, "144", translated(metric.getUnitKey()), 144.0);
            case AVG_FPS -> new OverlayLine(metric, metricLabel, "138", translated(metric.getUnitKey()), 138.0);
            case FRAME_TIME -> new OverlayLine(metric, metricLabel, "6.9", translated(metric.getUnitKey()), 6.9);
            case LOW_1 -> new OverlayLine(metric, metricLabel, "92", translated(metric.getUnitKey()), 92.0);
            case LOW_01 -> new OverlayLine(metric, metricLabel, "71", translated(metric.getUnitKey()), 71.0);
            case MEMORY -> switch (activeConfig.hud.memoryDisplayMode) {
                case USED_GB -> new OverlayLine(metric, metricLabel,
                        formatMemoryValue(3.4, activeConfig.hud.memoryUnit),
                        translated(getMemoryUnitKey(activeConfig.hud.memoryUnit)), 58.0);
                case PERCENT -> new OverlayLine(metric, metricLabel, "58", translated("text.fps_overlay.percent"), 58.0);
                case BOTH -> new OverlayLine(metric, metricLabel,
                        formatMemoryValue(3.4, activeConfig.hud.memoryUnit) + " / "
                                + formatMemoryValue(8.0, activeConfig.hud.memoryUnit)
                                + " " + translated(getMemoryUnitKey(activeConfig.hud.memoryUnit)) + " (58%)",
                        "", 58.0);
                case BAR -> new OverlayLine(metric, metricLabel, "58%", "", 58.0, true);
                case BAR_WITH_USED -> new OverlayLine(metric, metricLabel,
                        formatMemoryValue(3.4, activeConfig.hud.memoryUnit) + " "
                                + translated(getMemoryUnitKey(activeConfig.hud.memoryUnit)) + " (58%)",
                        "", 58.0, true);
            };
            case PING -> new OverlayLine(metric, metricLabel, "42", translated(metric.getUnitKey()), 42.0);
            case MSPT -> new OverlayLine(metric, metricLabel, "18.7", translated(metric.getUnitKey()), 18.7);
            case TPS -> new OverlayLine(metric, metricLabel, "20.0", translated(metric.getUnitKey()), 20.0);
            case CHUNKS -> new OverlayLine(metric, metricLabel, "324/361", "", 90.0);
            case COORDS -> new OverlayLine(metric, metricLabel, "128 64 -52", "", null);
            case BIOME -> new OverlayLine(metric, metricLabel, "Plains", "", null);
            case DIMENSION -> new OverlayLine(metric, metricLabel, "Overworld", "", null);
            case FACING -> new OverlayLine(metric, metricLabel, "north (180°)", "", null);
            case CHUNK_COORDS -> new OverlayLine(metric, metricLabel, "8, -4", "", null);
            case LIGHT -> new OverlayLine(metric, metricLabel, "10 / 15", "", null);
            case REAL_TIME -> new OverlayLine(metric, metricLabel,
                    activeConfig.hud.clockFormat == ModConfig.ClockFormat.HOUR_24 ? "21:42" : "9:42 PM", "", null);
            case DAY_COUNT -> new OverlayLine(metric, metricLabel, "128", "", null);
            case DAY_TIME -> new OverlayLine(metric, metricLabel,
                    activeConfig.hud.clockFormat == ModConfig.ClockFormat.HOUR_24 ? "14:36" : "2:36 PM", "", null);
            case JITTER -> new OverlayLine(metric, metricLabel, "3.2", translated(metric.getUnitKey()), 3.2);
            case PACKET_LOSS -> new OverlayLine(metric, metricLabel, "1.4", translated(metric.getUnitKey()), 1.4);
        };
    }

    private static String withMinMax(ModConfig activeConfig, int current, int min, int max) {
        return activeConfig.hud.showMinMaxStats ? current + " (" + min + "/" + max + ")" : String.valueOf(current);
    }

    private static boolean isLiveMetric(ModConfig activeConfig, OverlayMetric metric) {
        return activeConfig.hud.getMetricUpdateInterval(metric, activeConfig.general.updateIntervalMs) <= 0;
    }

    private static boolean hasFrameLiveMetrics(ModConfig activeConfig) {
        return isMetricFrameLive(activeConfig, OverlayMetric.FPS)
                || isMetricFrameLive(activeConfig, OverlayMetric.AVG_FPS)
                || isMetricFrameLive(activeConfig, OverlayMetric.LOW_1)
                || isMetricFrameLive(activeConfig, OverlayMetric.LOW_01);
    }

    private static boolean isMetricFrameLive(ModConfig activeConfig, OverlayMetric metric) {
        return activeConfig.hud.isMetricEnabled(metric) && isLiveMetric(activeConfig, metric);
    }

    private static long getRenderDataVersion(ModConfig activeConfig, PerformanceTracker tracker) {
        long dataVersion = tracker.getDataVersion();
        if (!hasFrameLiveMetrics(activeConfig)) {
            return dataVersion;
        }
        return (dataVersion * 31L) ^ tracker.getFrameDataVersion();
    }

    private static OverlayLine createMemoryLine(ModConfig activeConfig, PerformanceTracker tracker, OverlayMetric metric,
            String metricLabel) {
        if (tracker.getMaxMemory() <= 0) {
            return new OverlayLine(metric, metricLabel, "N/A", translated(metric.getUnitKey()), null);
        }

        double usedGb = tracker.getUsedMemory() / (1024.0 * 1024.0 * 1024.0);
        double totalGb = tracker.getMaxMemory() / (1024.0 * 1024.0 * 1024.0);
        double percentUsed = tracker.getUsedMemory() * 100.0 / tracker.getMaxMemory();
        ModConfig.MemoryUnit memoryUnit = activeConfig.hud.memoryUnit != null
                ? activeConfig.hud.memoryUnit
                : ModConfig.MemoryUnit.GB;
        String unitKey = getMemoryUnitKey(memoryUnit);

        return switch (activeConfig.hud.memoryDisplayMode) {
            case USED_GB -> new OverlayLine(metric, metricLabel, formatMemoryValue(usedGb, memoryUnit),
                    translated(unitKey), percentUsed);
            case PERCENT -> new OverlayLine(metric, metricLabel, WHOLE_NUMBER.format(percentUsed),
                    translated("text.fps_overlay.percent"), percentUsed);
            case BOTH -> new OverlayLine(metric, metricLabel,
                    formatMemoryValue(usedGb, memoryUnit) + " / " + formatMemoryValue(totalGb, memoryUnit)
                            + " " + translated(unitKey) + " (" + WHOLE_NUMBER.format(percentUsed) + "%)",
                    "", percentUsed);
            case BAR -> new OverlayLine(metric, metricLabel, WHOLE_NUMBER.format(percentUsed) + "%", "", percentUsed, true);
            case BAR_WITH_USED -> new OverlayLine(metric, metricLabel,
                    formatMemoryValue(usedGb, memoryUnit) + " " + translated(unitKey)
                            + " (" + WHOLE_NUMBER.format(percentUsed) + "%)",
                    "", percentUsed, true);
        };
    }

    private static String getMemoryUnitKey(ModConfig.MemoryUnit memoryUnit) {
        return memoryUnit == ModConfig.MemoryUnit.MB ? "text.fps_overlay.mb" : "text.fps_overlay.gb";
    }

    private static String formatMemoryValue(double valueInGb, ModConfig.MemoryUnit memoryUnit) {
        return switch (memoryUnit) {
            case MB -> WHOLE_NUMBER.format(valueInGb * 1024.0);
            case GB -> ONE_DECIMAL.format(valueInGb);
        };
    }

    private static String translated(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        synchronized (TRANSLATION_CACHE) {
            return TRANSLATION_CACHE.computeIfAbsent(key, cachedKey -> Component.translatable(cachedKey).getString());
        }
    }

    private static String formatTranslated(String key, Object... args) {
        return String.format(Locale.ROOT, translated(key), args);
    }

    private static void drawStyledText(GuiGraphics context, Font font, ModConfig activeConfig, String text,
            int x, int y, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        ModConfig.TextEffect effect = activeConfig != null ? activeConfig.appearance.textEffect : ModConfig.TextEffect.NONE;
        switch (effect) {
            case SHADOW -> context.drawString(font, text, x, y, color, true);
            case OUTLINE -> {
                int outlineColor = applyAlpha(0xD0000000, ((color >>> 24) & 0xFF) / 255.0f);
                context.drawString(font, text, x - 1, y, outlineColor, false);
                context.drawString(font, text, x + 1, y, outlineColor, false);
                context.drawString(font, text, x, y - 1, outlineColor, false);
                context.drawString(font, text, x, y + 1, outlineColor, false);
                context.drawString(font, text, x, y, color, false);
            }
            case NONE -> context.drawString(font, text, x, y, color, false);
        }
    }

    private static int getAdaptiveColor(ModConfig activeConfig, OverlayLine line) {
        if (!activeConfig.appearance.adaptiveColors || line.adaptiveValue() == null) {
            return getValueColor(activeConfig);
        }

        double value = line.adaptiveValue();
        ModConfig.Thresholds thresholds = activeConfig.thresholds != null ? activeConfig.thresholds : new ModConfig.Thresholds();
        return switch (line.metric()) {
            case FPS, AVG_FPS, LOW_1, LOW_01 -> value >= thresholds.fpsGood ? getGoodColor(activeConfig)
                    : value >= thresholds.fpsWarning ? getWarningColor(activeConfig) : getBadColor(activeConfig);
            case FRAME_TIME, MSPT -> value <= thresholds.frameTimeGood ? getGoodColor(activeConfig)
                    : value <= thresholds.frameTimeWarning ? getWarningColor(activeConfig) : getBadColor(activeConfig);
            case MEMORY, CHUNKS -> value <= thresholds.memoryGood ? getGoodColor(activeConfig)
                    : value <= thresholds.memoryWarning ? getWarningColor(activeConfig) : getBadColor(activeConfig);
            case PING, JITTER -> value <= thresholds.pingGood ? getGoodColor(activeConfig)
                    : value <= thresholds.pingWarning ? getWarningColor(activeConfig) : getBadColor(activeConfig);
            case TPS -> value >= thresholds.tpsGood ? getGoodColor(activeConfig)
                    : value >= thresholds.tpsWarning ? getWarningColor(activeConfig) : getBadColor(activeConfig);
            case PACKET_LOSS -> value <= 1.0 ? getGoodColor(activeConfig)
                    : value <= 5.0 ? getWarningColor(activeConfig) : getBadColor(activeConfig);
            case COORDS, BIOME, DIMENSION, FACING, CHUNK_COORDS, LIGHT, REAL_TIME, DAY_COUNT, DAY_TIME ->
                    getValueColor(activeConfig);
        };
    }

    private static void invalidateLiveCaches() {
        cachedLiveConfig = null;
        cachedLiveVersion = Long.MIN_VALUE;
        cachedLiveLines = List.of();
        cachedLiveLayoutConfig = null;
        cachedLiveLayoutSignature = 0;
        cachedLiveScreenWidth = -1;
        cachedLiveScreenHeight = -1;
        cachedLiveLayout = null;
    }

    private static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    private static int applyAlpha(int color, float multiplier) {
        int alpha = (color >>> 24) & 0xFF;
        if (alpha == 0 && (color & 0x00FFFFFF) != 0) {
            alpha = 0xFF;
        }
        alpha = Math.max(0, Math.min(255, Math.round(alpha * multiplier)));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int getBackgroundColor(ModConfig activeConfig) {
        return ((activeConfig.appearance.backgroundOpacity & 0xFF) << 24) | (activeConfig.appearance.backgroundColor & 0xFFFFFF);
    }

    private static int getLabelColor(ModConfig activeConfig) { return activeConfig.appearance.labelColor; }
    private static int getValueColor(ModConfig activeConfig) { return activeConfig.appearance.valueColor; }
    private static int getUnitColor(ModConfig activeConfig) { return activeConfig.appearance.unitColor; }
    private static int getDividerColor(ModConfig activeConfig) { return activeConfig.appearance.dividerColor; }
    private static int getGoodColor(ModConfig activeConfig) { return activeConfig.appearance.goodColor; }
    private static int getWarningColor(ModConfig activeConfig) { return activeConfig.appearance.warningColor; }
    private static int getBadColor(ModConfig activeConfig) { return activeConfig.appearance.badColor; }

    private static int[] getPreviewGraphValues() {
        return new int[] {120, 144, 140, 138, 147, 145, 141, 130, 136, 142, 144, 139, 148, 150, 143, 140, 137, 145, 149, 146, 142, 141, 147, 144};
    }

    private static int[] getPreviewPingGraphValues() {
        return new int[] {44, 42, 40, 43, 45, 41, 46, 49, 43, 42, 44, 47, 39, 40, 42, 45};
    }

    private static void applyScale(GuiGraphics context, float scale, Runnable renderer) {
        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.scale(scale, scale);
        renderer.run();
        matrices.popMatrix();
    }

    private static void drawRoundedRect(GuiGraphics context, int x, int y, int width, int height, int radius, int color) {
        int clampedRadius = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (clampedRadius <= 1) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        context.fill(x, y + clampedRadius, x + width, y + height - clampedRadius, color);
        for (int row = 0; row < clampedRadius; row++) {
            int dy = clampedRadius - 1 - row;
            int inset = Math.max(0, clampedRadius - (int) Math.ceil(Math.sqrt(Math.max(0, (clampedRadius * clampedRadius) - (dy * dy)))));
            int left = x + inset;
            int right = x + width - inset;
            context.fill(left, y + row, right, y + row + 1, color);
            context.fill(left, y + height - row - 1, right, y + height - row, color);
        }
    }

    private record OverlayLine(OverlayMetric metric, String label, String value, String unit, Double adaptiveValue,
                               boolean percentageBar) {
        private OverlayLine(OverlayMetric metric, String label, String value, String unit, Double adaptiveValue) {
            this(metric, label, value, unit, adaptiveValue, false);
        }
    }
    private record NavbarRow(List<OverlayLine> lines, int width) {}
    private record OverlayLayout(LayoutBounds bounds, List<NavbarRow> navbarRows, boolean showFpsGraph,
                                 boolean showPingGraph) {}
    public record LayoutBounds(int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
    public record AnchorPoint(int x, int y) {}
}
