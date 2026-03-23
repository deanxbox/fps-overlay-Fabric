package net.hicham.fps_overlay;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Platform-agnostic ConfigManager. Uses a simple listener list instead of
 * Fabric's Event API so it can live in the common module.
 */
public class ConfigManager {
    private static final Logger LOGGER = LogManager.getLogger("fps_overlay/ConfigManager");
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static File configFile;
    private static ModConfig config;

    @FunctionalInterface
    public interface ConfigChangedCallback {
        void onConfigChanged();
    }

    private static final List<ConfigChangedCallback> listeners = new ArrayList<>();

    public static void initialize() {
        if (initialized.get()) {
            return;
        }

        try {
            Path configDir = Paths.get("config", "fps_overlay");
            File configFile = configDir.resolve("config.json").toFile();

            if (!configDir.toFile().exists()) {
                configDir.toFile().mkdirs();
            }

            ConfigManager.configFile = configFile;
            config = ModConfig.load(configFile);
            initialized.set(true);
            LOGGER.info("Configuration manager initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize configuration", e);
            initialized.set(true);
        }
    }

    public static ModConfig getConfig() {
        ensureInitialized();
        if (config == null) {
            config = new ModConfig();
        }
        return config;
    }

    public static void saveConfig() {
        ensureInitialized();
        ModConfig.save();
        notifyListeners();
    }

    public static void resetToDefaults() {
        ensureInitialized();
        getConfig().resetToDefaults();
        saveConfig();
        LOGGER.info("Configuration reset to defaults");
    }

    public static void registerConfigListener(ConfigChangedCallback listener) {
        listeners.add(listener);
    }

    private static void notifyListeners() {
        for (ConfigChangedCallback listener : listeners) {
            try {
                listener.onConfigChanged();
            } catch (Exception e) {
                LOGGER.error("Error in config change listener", e);
            }
        }
    }

    private static void ensureInitialized() {
        if (!initialized.get()) {
            LOGGER.warn("ConfigManager accessed before initialization, initializing now...");
            initialize();
        }
    }

    public static boolean isInitialized() {
        return initialized.get();
    }

    public static void cleanup() {
        initialized.set(false);
        config = null;
        configFile = null;
    }
}
