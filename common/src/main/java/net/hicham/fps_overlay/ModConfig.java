package net.hicham.fps_overlay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class ModConfig {
    private static final Logger LOGGER = LogManager.getLogger("fps_overlay/ModConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CURRENT_CONFIG_VERSION = 17;

    public enum OverlayPosition {
        TOP_LEFT("enum.fps_overlay.overlayposition.top_left"),
        TOP_CENTER("enum.fps_overlay.overlayposition.top_center"),
        TOP_RIGHT("enum.fps_overlay.overlayposition.top_right"),
        CENTER_LEFT("enum.fps_overlay.overlayposition.center_left"),
        CENTER_RIGHT("enum.fps_overlay.overlayposition.center_right"),
        BOTTOM_LEFT("enum.fps_overlay.overlayposition.bottom_left"),
        BOTTOM_CENTER("enum.fps_overlay.overlayposition.bottom_center"),
        BOTTOM_RIGHT("enum.fps_overlay.overlayposition.bottom_right");

        private final String translationKey;

        OverlayPosition(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component getDisplayText() {
            return Component.translatable(translationKey);
        }
    }

    public enum OverlayStyle {
        DEFAULT,
        NAVBAR
    }

    public enum TextEffect {
        NONE,
        SHADOW,
        OUTLINE
    }

    public enum MemoryDisplayMode {
        USED_GB,
        PERCENT,
        BOTH,
        BAR,
        BAR_WITH_USED
    }

    public enum MemoryUnit {
        GB,
        MB
    }

    public enum ClockFormat {
        AM_PM,
        HOUR_24
    }

    public General general = new General();
    public HUD hud = new HUD();
    public Appearance appearance = new Appearance();
    public Thresholds thresholds = new Thresholds();
    public AutoHideRules autoHide = new AutoHideRules();
    public String selectedProfile = "";
    public Map<String, ConfigProfile> configProfiles = new LinkedHashMap<>();

    public static class General {
        public boolean enabled = true;
        public boolean enableKeybindings = true;
        public int updateIntervalMs = 250;
        public int configVersion = CURRENT_CONFIG_VERSION;
    }

    public static class HUD {
        public boolean showFps = true;
        public boolean showAverageFps = true;
        public boolean showFrameTime = false;
        public boolean show1PercentLow = true;
        public boolean show0_1PercentLow = false;
        public boolean showMemory = true;
        public boolean showPing = true;
        public boolean showMspt = true;
        public boolean showTps = true;
        public boolean showChunks = false;
        public boolean showCoordinates = false;
        public boolean showBiome = false;
        public boolean showDimension = false;
        public boolean showFacing = false;
        public boolean showChunkCoords = false;
        public boolean showLight = false;
        public boolean showRealTime = false;
        public boolean showDayCount = false;
        public boolean showDayTime = false;
        public boolean showJitter = false;
        public boolean showPacketLoss = false;
        public boolean showPingGraph = false;
        public boolean showGraph = false;
        public boolean showMinMaxStats = false;
        public MemoryDisplayMode memoryDisplayMode = MemoryDisplayMode.USED_GB;
        public MemoryUnit memoryUnit = MemoryUnit.GB;
        public ClockFormat clockFormat = ClockFormat.AM_PM;

        public List<String> metricOrder = new CopyOnWriteArrayList<>(OverlayMetric.defaultOrderIds());
        public Map<String, String> metricDisplayNames = new LinkedHashMap<>();
        public Map<String, Integer> metricUpdateIntervals = new LinkedHashMap<>();
        private transient List<String> cachedMetricOrderIds;
        private transient List<OverlayMetric> cachedSanitizedMetricOrder;

        public boolean isMetricEnabled(OverlayMetric metric) {
            return switch (metric) {
                case FPS -> showFps;
                case AVG_FPS -> showAverageFps;
                case FRAME_TIME -> showFrameTime;
                case LOW_1 -> show1PercentLow;
                case LOW_01 -> show0_1PercentLow;
                case MEMORY -> showMemory;
                case PING -> showPing;
                case MSPT -> showMspt;
                case TPS -> showTps;
                case CHUNKS -> showChunks;
                case COORDS -> showCoordinates;
                case BIOME -> showBiome;
                case DIMENSION -> showDimension;
                case FACING -> showFacing;
                case CHUNK_COORDS -> showChunkCoords;
                case LIGHT -> showLight;
                case REAL_TIME -> showRealTime;
                case DAY_COUNT -> showDayCount;
                case DAY_TIME -> showDayTime;
                case JITTER -> showJitter;
                case PACKET_LOSS -> showPacketLoss;
            };
        }

        public void setMetricEnabled(OverlayMetric metric, boolean enabled) {
            switch (metric) {
                case FPS -> showFps = enabled;
                case AVG_FPS -> showAverageFps = enabled;
                case FRAME_TIME -> showFrameTime = enabled;
                case LOW_1 -> show1PercentLow = enabled;
                case LOW_01 -> show0_1PercentLow = enabled;
                case MEMORY -> showMemory = enabled;
                case PING -> showPing = enabled;
                case MSPT -> showMspt = enabled;
                case TPS -> showTps = enabled;
                case CHUNKS -> showChunks = enabled;
                case COORDS -> showCoordinates = enabled;
                case BIOME -> showBiome = enabled;
                case DIMENSION -> showDimension = enabled;
                case FACING -> showFacing = enabled;
                case CHUNK_COORDS -> showChunkCoords = enabled;
                case LIGHT -> showLight = enabled;
                case REAL_TIME -> showRealTime = enabled;
                case DAY_COUNT -> showDayCount = enabled;
                case DAY_TIME -> showDayTime = enabled;
                case JITTER -> showJitter = enabled;
                case PACKET_LOSS -> showPacketLoss = enabled;
            }
        }

        public String getMetricDisplayName(OverlayMetric metric) {
            String customName = getCustomMetricName(metric);
            if (!customName.isBlank()) {
                return customName;
            }

            return Component.translatable(metric.getLabelKey()).getString();
        }

        public String getCustomMetricName(OverlayMetric metric) {
            if (metricDisplayNames == null) {
                return "";
            }

            String value = metricDisplayNames.get(metric.getId());
            return value == null ? "" : value.trim();
        }

        public void setCustomMetricName(OverlayMetric metric, String value) {
            if (metricDisplayNames == null) {
                metricDisplayNames = new LinkedHashMap<>();
            }

            if (value == null || value.trim().isEmpty()) {
                metricDisplayNames.remove(metric.getId());
                return;
            }

            metricDisplayNames.put(metric.getId(), value.trim());
        }

        public List<OverlayMetric> getSanitizedMetricOrder() {
            if (cachedSanitizedMetricOrder == null || !Objects.equals(cachedMetricOrderIds, metricOrder)) {
                cachedMetricOrderIds = metricOrder == null ? List.of() : List.copyOf(metricOrder);
                cachedSanitizedMetricOrder = List.copyOf(OverlayMetric.sanitizeOrder(metricOrder));
            }
            return cachedSanitizedMetricOrder;
        }

        public int getMetricUpdateInterval(OverlayMetric metric, int globalDefaultMs) {
            if (metric == null) {
                return globalDefaultMs;
            }

            if (metricUpdateIntervals != null) {
                Integer override = metricUpdateIntervals.get(metric.getId());
                if (override != null) {
                    return Math.max(0, override);
                }
            }

            return switch (metric) {
                case COORDS, BIOME -> 0;
                default -> globalDefaultMs;
            };
        }

        public void setMetricUpdateInterval(OverlayMetric metric, int intervalMs) {
            if (metricUpdateIntervals == null) {
                metricUpdateIntervals = new LinkedHashMap<>();
            }
            metricUpdateIntervals.put(metric.getId(), Math.max(0, intervalMs));
        }
    }

    public static class Appearance {
        public OverlayPosition position = OverlayPosition.TOP_CENTER;
        public OverlayStyle overlayStyle = OverlayStyle.NAVBAR;
        public TextEffect textEffect = TextEffect.NONE;

        public boolean showBackground = true;
        public int backgroundOpacity = 180;
        public int backgroundColor = 0x212B36;
        public int labelColor = 0xFF839DB1;
        public int valueColor = 0xFFFFFFFF;
        public int unitColor = 0xFFC99566;
        public int dividerColor = 0xFF354451;
        public int goodColor = 0xFF2ED177;
        public int warningColor = 0xFFFFD100;
        public int badColor = 0xFFFF4545;

        public float hudScale = 0.65f;
        public boolean adaptiveColors = true;
        public boolean autoHideF3 = true;
        public int xOffset = 0;
        public int yOffset = 0;
    }

    public static class Thresholds {
        public int fpsGood = 60;
        public int fpsWarning = 30;
        public double frameTimeGood = 16.7;
        public double frameTimeWarning = 33.3;
        public int memoryGood = 75;
        public int memoryWarning = 90;
        public int pingGood = 60;
        public int pingWarning = 150;
        public double tpsGood = 19.5;
        public double tpsWarning = 15.0;
    }

    public static class AutoHideRules {
        public boolean hideInChat = false;
        public boolean hideInInventory = false;
        public boolean hideInScreenshots = false;
        public boolean hideWithF3 = true;
        public boolean fadeOnIdle = false;
        public int fadeDelaySeconds = 30;
        public float fadeOpacity = 0.35f;
    }

    public static class ConfigProfile {
        public General general = new General();
        public HUD hud = new HUD();
        public Appearance appearance = new Appearance();
        public Thresholds thresholds = new Thresholds();
        public AutoHideRules autoHide = new AutoHideRules();

        public void validate() {
            ModConfig wrapper = new ModConfig();
            wrapper.general = general;
            wrapper.hud = hud;
            wrapper.appearance = appearance;
            wrapper.thresholds = thresholds;
            wrapper.autoHide = autoHide;
            wrapper.configProfiles = new LinkedHashMap<>();
            wrapper.selectedProfile = "";
            wrapper.validate();

            general = wrapper.general;
            hud = wrapper.hud;
            appearance = wrapper.appearance;
            thresholds = wrapper.thresholds;
            autoHide = wrapper.autoHide;
        }
    }

    public void validate() {
        ensureSections();

        general.updateIntervalMs = clampInt(general.updateIntervalMs, 0, 1000);

        appearance.backgroundOpacity = clampInt(appearance.backgroundOpacity, 0, 255);
        appearance.hudScale = clampFloat(appearance.hudScale, 0.2f, 1.5f);
        appearance.xOffset = clampInt(appearance.xOffset, -2000, 2000);
        appearance.yOffset = clampInt(appearance.yOffset, -2000, 2000);

        if (appearance.position == null) {
            appearance.position = OverlayPosition.TOP_CENTER;
        }
        if (appearance.overlayStyle == null) {
            appearance.overlayStyle = OverlayStyle.NAVBAR;
        }
        if (appearance.textEffect == null) {
            appearance.textEffect = TextEffect.NONE;
        }

        if (hud.metricOrder == null || hud.metricOrder.isEmpty()) {
            hud.metricOrder = new CopyOnWriteArrayList<>(OverlayMetric.defaultOrderIds());
        } else {
            List<String> sanitized = new ArrayList<>();
            for (OverlayMetric metric : OverlayMetric.sanitizeOrder(hud.metricOrder)) {
                sanitized.add(metric.getId());
            }
            if (!(hud.metricOrder instanceof CopyOnWriteArrayList) || !hud.metricOrder.equals(sanitized)) {
                hud.metricOrder = new CopyOnWriteArrayList<>(sanitized);
            }
        }

        if (hud.metricDisplayNames == null) {
            hud.metricDisplayNames = new LinkedHashMap<>();
        } else {
            Map<String, String> sanitizedNames = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : hud.metricDisplayNames.entrySet()) {
                OverlayMetric metric = OverlayMetric.fromId(entry.getKey());
                if (metric == null || entry.getValue() == null) {
                    continue;
                }

                String trimmed = entry.getValue().trim();
                if (!trimmed.isEmpty()) {
                    sanitizedNames.put(metric.getId(), trimmed);
                }
            }
            hud.metricDisplayNames = sanitizedNames;
        }

        if (hud.metricUpdateIntervals == null) {
            hud.metricUpdateIntervals = new LinkedHashMap<>();
        } else {
            Map<String, Integer> sanitizedIntervals = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> entry : hud.metricUpdateIntervals.entrySet()) {
                OverlayMetric metric = OverlayMetric.fromId(entry.getKey());
                Integer value = entry.getValue();
                if (metric != null && value != null) {
                    sanitizedIntervals.put(metric.getId(), clampInt(value, 0, 5000));
                }
            }
            hud.metricUpdateIntervals = sanitizedIntervals;
        }

        if (hud.memoryDisplayMode == null) {
            hud.memoryDisplayMode = MemoryDisplayMode.USED_GB;
        }
        if (hud.memoryUnit == null) {
            hud.memoryUnit = MemoryUnit.GB;
        }
        if (hud.clockFormat == null) {
            hud.clockFormat = ClockFormat.AM_PM;
        }

        hud.cachedMetricOrderIds = null;
        hud.cachedSanitizedMetricOrder = null;

        validateThresholds();
        validateAutoHide();
        validateProfiles();
    }

    public void resetToDefaults() {
        this.general = new General();
        this.hud = new HUD();
        this.appearance = new Appearance();
        this.thresholds = new Thresholds();
        this.autoHide = new AutoHideRules();
        this.selectedProfile = "";
        this.configProfiles = new LinkedHashMap<>();
        validate();
    }

    public ModConfig copy() {
        ModConfig copy = GSON.fromJson(GSON.toJson(this), ModConfig.class);
        if (copy == null) {
            copy = new ModConfig();
        }
        copy.validate();
        return copy;
    }

    public List<String> getProfileNames() {
        return new ArrayList<>(configProfiles.keySet());
    }

    public void saveProfile(String name) {
        saveProfile(name, this);
    }

    public void saveProfile(String name, ModConfig sourceConfig) {
        String sanitizedName = sanitizeProfileName(name);
        if (sanitizedName.isEmpty()) {
            return;
        }

        ConfigProfile profile = createProfileFrom(sourceConfig == null ? this : sourceConfig);
        configProfiles.put(sanitizedName, profile);
        selectedProfile = sanitizedName;
    }

    public void renameProfile(String oldName, String newName) {
        String sanitizedOld = sanitizeProfileName(oldName);
        String sanitizedNew = sanitizeProfileName(newName);
        if (sanitizedOld.isEmpty() || sanitizedNew.isEmpty() || !configProfiles.containsKey(sanitizedOld)) {
            return;
        }

        ConfigProfile profile = configProfiles.remove(sanitizedOld);
        configProfiles.put(sanitizedNew, profile);
        if (sanitizedOld.equals(selectedProfile)) {
            selectedProfile = sanitizedNew;
        }
    }

    public void deleteProfile(String name) {
        String sanitizedName = sanitizeProfileName(name);
        if (sanitizedName.isEmpty()) {
            return;
        }

        configProfiles.remove(sanitizedName);
        if (sanitizedName.equals(selectedProfile)) {
            selectedProfile = "";
        }
    }

    public void applyProfile(String name) {
        String sanitizedName = sanitizeProfileName(name);
        ConfigProfile profile = configProfiles.get(sanitizedName);
        if (profile == null) {
            selectedProfile = "";
            return;
        }

        profile.validate();
        general = copySection(profile.general, General.class);
        hud = copySection(profile.hud, HUD.class);
        appearance = copySection(profile.appearance, Appearance.class);
        thresholds = copySection(profile.thresholds, Thresholds.class);
        autoHide = copySection(profile.autoHide, AutoHideRules.class);
        selectedProfile = sanitizedName;
        validate();
    }

    public static ModConfig load(File file) {
        ModConfig loadedConfig;
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                loadedConfig = GSON.fromJson(reader, ModConfig.class);
                if (loadedConfig == null) {
                    loadedConfig = new ModConfig();
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load FPS Overlay config, reverting to defaults.", e);
                loadedConfig = new ModConfig();
            }
        } else {
            loadedConfig = new ModConfig();
        }

        loadedConfig.migrate();
        loadedConfig.validate();
        save(file, loadedConfig);
        return loadedConfig;
    }

    private void migrate() {
        ensureSections();

        if (general.configVersion < 5) {
            LOGGER.info("Migrating config from version {} to 5", general.configVersion);
            if (hud.metricOrder != null) {
                for (OverlayMetric metric : OverlayMetric.values()) {
                    if (!hud.metricOrder.contains(metric.getId())) {
                        hud.metricOrder.add(metric.getId());
                    }
                }
            }
            general.configVersion = 5;
        }

        if (general.configVersion < 7) {
            LOGGER.info("Migrating config from version {} to 7", general.configVersion);
            if (thresholds == null) {
                thresholds = new Thresholds();
            }
            general.configVersion = 7;
        }

        if (general.configVersion < 13) {
            LOGGER.info("Migrating config from version {} to 13", general.configVersion);
            if (hud.memoryUnit == null) {
                hud.memoryUnit = MemoryUnit.GB;
            }
            general.configVersion = 13;
        }

        if (general.configVersion < 14) {
            LOGGER.info("Migrating config from version {} to 14", general.configVersion);
            if (hud.clockFormat == null) {
                hud.clockFormat = ClockFormat.AM_PM;
            }
            general.configVersion = 14;
        }

        if (general.configVersion < 16) {
            LOGGER.info("Migrating config from version {} to 16", general.configVersion);
            if (hud.metricOrder != null) {
                hud.metricOrder.remove("ping_graph");
            }
            general.configVersion = 16;
        }

        if (general.configVersion < 17) {
            LOGGER.info("Migrating config from version {} to 17", general.configVersion);
            if (selectedProfile == null) {
                selectedProfile = "";
            }
            if (configProfiles == null) {
                configProfiles = new LinkedHashMap<>();
            }
            general.configVersion = 17;
        }

        if (general.configVersion < CURRENT_CONFIG_VERSION) {
            LOGGER.info("Migrating config from version {} to {}", general.configVersion, CURRENT_CONFIG_VERSION);
            general.configVersion = CURRENT_CONFIG_VERSION;
        }
    }

    private void ensureSections() {
        if (general == null) {
            general = new General();
        }
        if (hud == null) {
            hud = new HUD();
        }
        if (appearance == null) {
            appearance = new Appearance();
        }
        if (thresholds == null) {
            thresholds = new Thresholds();
        }
        if (autoHide == null) {
            autoHide = new AutoHideRules();
        }
        if (selectedProfile == null) {
            selectedProfile = "";
        }
        if (configProfiles == null) {
            configProfiles = new LinkedHashMap<>();
        }
    }

    private void validateThresholds() {
        sanitizeThresholds(thresholds);
    }

    private static void sanitizeThresholds(Thresholds thresholds) {
        thresholds.fpsGood = clampInt(thresholds.fpsGood, 1, 500);
        thresholds.fpsWarning = clampInt(thresholds.fpsWarning, 1, thresholds.fpsGood);

        thresholds.frameTimeGood = clampDouble(thresholds.frameTimeGood, 1.0, 250.0);
        thresholds.frameTimeWarning = clampDouble(thresholds.frameTimeWarning, thresholds.frameTimeGood, 500.0);

        thresholds.memoryGood = clampInt(thresholds.memoryGood, 1, 100);
        thresholds.memoryWarning = clampInt(thresholds.memoryWarning, thresholds.memoryGood, 100);

        thresholds.pingGood = clampInt(thresholds.pingGood, 0, 1000);
        thresholds.pingWarning = clampInt(thresholds.pingWarning, thresholds.pingGood, 2000);

        thresholds.tpsGood = clampDouble(thresholds.tpsGood, 0.1, 20.0);
        thresholds.tpsWarning = clampDouble(thresholds.tpsWarning, 0.1, thresholds.tpsGood);
    }

    private void validateAutoHide() {
        autoHide.fadeDelaySeconds = clampInt(autoHide.fadeDelaySeconds, 1, 600);
        autoHide.fadeOpacity = clampFloat(autoHide.fadeOpacity, 0.0f, 1.0f);
        autoHide.hideWithF3 = autoHide.hideWithF3 || appearance.autoHideF3;
        appearance.autoHideF3 = autoHide.hideWithF3;
    }

    private void validateProfiles() {
        Map<String, ConfigProfile> sanitizedProfiles = new LinkedHashMap<>();
        for (Map.Entry<String, ConfigProfile> entry : configProfiles.entrySet()) {
            String name = sanitizeProfileName(entry.getKey());
            ConfigProfile profile = entry.getValue();
            if (name.isEmpty() || profile == null) {
                continue;
            }

            profile.validate();
            sanitizedProfiles.put(name, profile);
        }

        configProfiles = sanitizedProfiles;
        String sanitizedSelected = sanitizeProfileName(selectedProfile);
        selectedProfile = configProfiles.containsKey(sanitizedSelected) ? sanitizedSelected : "";
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String sanitizeProfileName(String name) {
        return name == null ? "" : name.trim();
    }

    private static ConfigProfile createProfileFrom(ModConfig sourceConfig) {
        ConfigProfile profile = new ConfigProfile();
        profile.general = copySection(sourceConfig.general, General.class);
        profile.hud = copySection(sourceConfig.hud, HUD.class);
        profile.appearance = copySection(sourceConfig.appearance, Appearance.class);
        profile.thresholds = copySection(sourceConfig.thresholds, Thresholds.class);
        profile.autoHide = copySection(sourceConfig.autoHide, AutoHideRules.class);
        profile.validate();
        return profile;
    }

    private static <T> T copySection(T source, Class<T> sectionClass) {
        T copy = GSON.fromJson(GSON.toJson(source), sectionClass);
        if (copy != null) {
            return copy;
        }

        try {
            return sectionClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to copy config section " + sectionClass.getSimpleName(), e);
        }
    }

    public static void save(File file, ModConfig config) {
        if (config != null && file != null) {
            config.validate();
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(config, writer);
            } catch (IOException e) {
                LOGGER.error("Failed to save FPS Overlay config!", e);
            }
        }
    }
}
