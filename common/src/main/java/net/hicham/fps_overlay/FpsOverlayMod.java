package net.hicham.fps_overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shared mod logic. Platform entrypoints call into this class.
 */
public class FpsOverlayMod {
    public static final String MOD_ID = "fps_overlay";
    public static final Logger LOGGER = LogManager.getLogger("FpsOverlay");

    private static ModConfig config;
    private static final AtomicBoolean modInitialized = new AtomicBoolean(false);
    private static final Object INIT_LOCK = new Object();

    public static void init() {
        LOGGER.info("Initializing Fps Overlay...");

        synchronized (INIT_LOCK) {
            if (modInitialized.get()) {
                LOGGER.warn("Mod already initialized");
                return;
            }

            try {
                ConfigManager.initialize();
                config = ConfigManager.getConfig();

                PerformanceTracker.getInstance().setConfig(config);
                OverlayRenderer.setConfig(config);

                ConfigManager.registerConfigListener(FpsOverlayMod::onConfigChanged);

                modInitialized.set(true);
                LOGGER.info("Fps Overlay initialized successfully");

            } catch (Exception e) {
                LOGGER.error("Failed to initialize Fps Overlay", e);
                modInitialized.set(false);
            }
        }
    }

    public static boolean isInitialized() {
        return modInitialized.get();
    }

    public static ModConfig getConfig() {
        return config;
    }

    public static boolean shouldRenderOverlay() {
        Minecraft client = Minecraft.getInstance();
        return modInitialized.get() &&
                config != null &&
                config.general.enabled &&
                client != null &&
                client.player != null &&
                client.level != null &&
                client.font != null &&
                !client.options.hideGui;
    }

    public static void onClientTick(Minecraft client) {
        if (!modInitialized.get() || client.player == null)
            return;

        PerformanceTracker.getInstance().update(client);
    }

    private static void onConfigChanged() {
        if (!modInitialized.get())
            return;

        LOGGER.debug("Configuration changed - reloading settings");
        try {
            config = ConfigManager.getConfig();

            PerformanceTracker.getInstance().setConfig(config);
            OverlayRenderer.setConfig(config);

            if (!config.hud.showAverageFps) {
                PerformanceTracker.getInstance().clearAverageFpsData();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to handle config change", e);
        }
    }

    public static void sendToggleMessage(String feature, boolean enabled) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            String status = enabled ? "§aENABLED" : "§cDISABLED";
            String message = String.format("§7[§6FpsOverlay§7] §f%s §7is now %s", feature, status);
            client.player.displayClientMessage(Component.literal(message), false);
        }
    }
}
