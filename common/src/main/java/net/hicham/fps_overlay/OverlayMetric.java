package net.hicham.fps_overlay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public enum OverlayMetric {
    FPS("fps", "text.fps_overlay.fps_short", "text.fps_overlay.fps", "text.fps_overlay.fps"),
    AVG_FPS("avg_fps", "text.fps_overlay.avg_fps_short", "text.fps_overlay.avg_fps", "text.fps_overlay.fps"),
    FRAME_TIME("frame_time", "text.fps_overlay.frame_time_short", "text.fps_overlay.frame_time", "text.fps_overlay.ms"),
    LOW_1("one_percent_low", "text.fps_overlay.1percent_low_short", "text.fps_overlay.1percent_low", "text.fps_overlay.fps"),
    LOW_01("point_one_percent_low", "text.fps_overlay.0_1percent_low_short", "text.fps_overlay.0_1percent_low", "text.fps_overlay.fps"),
    MEMORY("memory", "text.fps_overlay.memory_short", "text.fps_overlay.memory", "text.fps_overlay.gb"),
    PING("ping", "text.fps_overlay.ping_short", "text.fps_overlay.ping", "text.fps_overlay.ms"),
    MSPT("mspt", "text.fps_overlay.mspt_short", "text.fps_overlay.mspt", "text.fps_overlay.ms"),
    TPS("tps", "text.fps_overlay.tps_short", "text.fps_overlay.tps", "text.fps_overlay.tps_unit"),
    CHUNKS("chunks", "text.fps_overlay.chunks_short", "text.fps_overlay.chunks", ""),
    COORDS("coords", "text.fps_overlay.coords_short", "text.fps_overlay.coords", ""),
    BIOME("biome", "text.fps_overlay.biome_short", "text.fps_overlay.biome", ""),
    DIMENSION("dimension", "text.fps_overlay.dimension_short", "text.fps_overlay.dimension", ""),
    FACING("facing", "text.fps_overlay.facing_short", "text.fps_overlay.facing", ""),
    CHUNK_COORDS("chunk_coords", "text.fps_overlay.chunk_coords_short", "text.fps_overlay.chunk_coords", ""),
    LIGHT("light", "text.fps_overlay.light_short", "text.fps_overlay.light", ""),
    REAL_TIME("real_time", "text.fps_overlay.real_time_short", "text.fps_overlay.real_time", ""),
    DAY_COUNT("day_count", "text.fps_overlay.day_count_short", "text.fps_overlay.day_count", ""),
    DAY_TIME("day_time", "text.fps_overlay.day_time_short", "text.fps_overlay.day_time", ""),
    JITTER("jitter", "text.fps_overlay.jitter_short", "text.fps_overlay.jitter", "text.fps_overlay.ms"),
    PACKET_LOSS("packet_loss", "text.fps_overlay.packet_loss_short", "text.fps_overlay.packet_loss", "text.fps_overlay.percent");

    private final String id;
    private final String labelKey;
    private final String displayNameKey;
    private final String unitKey;

    OverlayMetric(String id, String labelKey, String displayNameKey, String unitKey) {
        this.id = id;
        this.labelKey = labelKey;
        this.displayNameKey = displayNameKey;
        this.unitKey = unitKey;
    }

    public String getId() {
        return id;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public String getDisplayNameKey() {
        return displayNameKey;
    }

    public String getUnitKey() {
        return unitKey;
    }

    public static OverlayMetric fromId(String id) {
        for (OverlayMetric metric : values()) {
            if (metric.id.equalsIgnoreCase(id)) {
                return metric;
            }
        }
        return null;
    }

    public static List<String> defaultOrderIds() {
        List<String> order = new ArrayList<>(values().length);
        for (OverlayMetric metric : values()) {
            order.add(metric.id);
        }
        return order;
    }

    public static List<OverlayMetric> sanitizeOrder(List<String> configuredOrder) {
        Set<OverlayMetric> ordered = new LinkedHashSet<>();
        if (configuredOrder != null) {
            for (String id : configuredOrder) {
                OverlayMetric metric = fromId(id);
                if (metric != null) {
                    ordered.add(metric);
                }
            }
        }

        ordered.addAll(Arrays.asList(values()));
        return new ArrayList<>(ordered);
    }
}
