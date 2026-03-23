package net.hicham.fps_overlay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile;
    private static ModConfig instance;

    public static ModConfig get() {
        if (instance == null) {
            instance = new ModConfig();
        }
        return instance;
    }

    // Available overlay positions on the screen
    public enum OverlayPosition {
        TOP_LEFT, TOP_RIGHT, TOP_CENTER, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    // HUD Layout Style
    public enum OverlayStyle {
        DEFAULT, NAVBAR
    }

    public General general = new General();

    // General Mod Settings
    public static class General {
        public boolean enabled = true;
        public boolean enableKeybindings = true;
        public int updateIntervalMs = 250;
        public int configVersion = 4;
    }

    public HUD hud = new HUD();

    // HUD Modules Settings (Toggle individual counters)
    public static class HUD {
        public boolean showFps = true;
        public boolean showAverageFps = true;
        public boolean showMemory = true;
        public boolean showPing = true;
        public boolean show1PercentLow = true;
        public boolean showMspt = true;
        public boolean showTps = true;
    }

    public Appearance appearance = new Appearance();

    // Appearance & Visual Settings
    public static class Appearance {
        public OverlayPosition position = OverlayPosition.TOP_LEFT;
        public OverlayStyle overlayStyle = OverlayStyle.DEFAULT;
        public boolean showBackground = true;
        public int backgroundOpacity = 180;
        public float hudScale = 0.65f;
        public boolean adaptiveColors = true;
        public boolean autoHideF3 = true;
    }

    public void validate() {
        general.updateIntervalMs = Math.max(16, Math.min(1000, general.updateIntervalMs));
        appearance.backgroundOpacity = Math.max(0, Math.min(255, appearance.backgroundOpacity));
        if (appearance.hudScale != 0.65f && appearance.hudScale != 0.8f && appearance.hudScale != 0.95f) {
            appearance.hudScale = 0.65f; // Default to Small
        }
        if (appearance.position == null)
            appearance.position = OverlayPosition.TOP_LEFT;
    }

    public void resetToDefaults() {
        this.general = new General();
        this.hud = new HUD();
        this.appearance = new Appearance();
        validate();
    }

    public static void init(File file) {
        configFile = file;
        load();
    }

    @SuppressWarnings("unused")
    public static void load() {
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                instance = GSON.fromJson(reader, ModConfig.class);
                if (instance == null)
                    instance = new ModConfig();
            } catch (Exception e) {
                System.err.println("Failed to load FPS Overlay config, reverting to defaults.");
                instance = new ModConfig();
            }
        } else {
            instance = new ModConfig();
        }
        instance.validate();
        save();
    }

    public static void save() {
        if (instance != null && configFile != null) {
            instance.validate();
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(instance, writer);
            } catch (IOException e) {
                System.err.println("Failed to save FPS Overlay config!");
            }
        }
    }
}
