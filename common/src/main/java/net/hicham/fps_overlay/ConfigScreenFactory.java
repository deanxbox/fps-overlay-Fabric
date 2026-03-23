package net.hicham.fps_overlay;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static net.hicham.fps_overlay.ModConfig.*;

/**
 * Builds the Cloth Config screen. Platform-agnostic (Mojang mappings).
 */
public class ConfigScreenFactory {
    private static final String TITLE_CONFIG = "title.fps_overlay.config";

    private static final String CATEGORY_HUD = "category.fps_overlay.hud";
    private static final String CATEGORY_APPEARANCE = "category.fps_overlay.appearance";

    private static final String TOOLTIP_ENABLED = "tooltip.fps_overlay.enabled";
    private static final String TOOLTIP_UPDATE_INTERVAL = "tooltip.fps_overlay.updateInterval";

    private static final String TOOLTIP_SHOW_FPS = "tooltip.fps_overlay.showFps";
    private static final String TOOLTIP_SHOW_AVERAGE_FPS = "tooltip.fps_overlay.showAverageFps";

    private static final String TOOLTIP_SHOW_MEMORY = "tooltip.fps_overlay.showMemory";
    private static final String TOOLTIP_SHOW_PING = "tooltip.fps_overlay.showPing";
    private static final String TOOLTIP_POSITION = "tooltip.fps_overlay.position";
    private static final String TOOLTIP_SHOW_BACKGROUND = "tooltip.fps_overlay.showBackground";
    private static final String TOOLTIP_BACKGROUND_OPACITY = "tooltip.fps_overlay.backgroundOpacity";

    @SuppressWarnings("null")
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable(TITLE_CONFIG))
                .setTransparentBackground(true)
                .setSavingRunnable(ConfigManager::saveConfig);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ModConfig config = ConfigManager.getConfig();

        // --- HUD Modules Category ---
        ConfigCategory hud = builder.getOrCreateCategory(Component.translatable(CATEGORY_HUD));

        hud.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.fps_overlay.enabled"),
                        config.general.enabled)
                .setDefaultValue(true)
                .setTooltip(Component.translatable(TOOLTIP_ENABLED))
                .setYesNoTextSupplier(val -> val ? Component.literal("[ ON ]") : Component.literal("[ OFF ]"))
                .setSaveConsumer(value -> config.general.enabled = value)
                .build());

        hud.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.fps_overlay.showFps"), config.hud.showFps)
                .setDefaultValue(true)
                .setTooltip(Component.translatable(TOOLTIP_SHOW_FPS))
                .setYesNoTextSupplier(val -> val ? Component.literal("[ ON ]") : Component.literal("[ OFF ]"))
                .setSaveConsumer(value -> config.hud.showFps = value)
                .build());

        hud.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.fps_overlay.showAverageFps"),
                        config.hud.showAverageFps)
                .setDefaultValue(true)
                .setTooltip(Component.translatable(TOOLTIP_SHOW_AVERAGE_FPS))
                .setYesNoTextSupplier(val -> val ? Component.literal("[ ON ]") : Component.literal("[ OFF ]"))
                .setSaveConsumer(value -> config.hud.showAverageFps = value)
                .build());

        hud.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.fps_overlay.show1PercentLow"),
                        config.hud.show1PercentLow)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("tooltip.fps_overlay.show1PercentLow"))
                .setYesNoTextSupplier(val -> val ? Component.literal("[ ON ]") : Component.literal("[ OFF ]"))
                .setSaveConsumer(value -> config.hud.show1PercentLow = value)
                .build());

        hud.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.fps_overlay.showMemory"),
                        config.hud.showMemory)
                .setDefaultValue(true)
                .setTooltip(Component.translatable(TOOLTIP_SHOW_MEMORY))
                .setYesNoTextSupplier(val -> val ? Component.literal("[ ON ]") : Component.literal("[ OFF ]"))
                .setSaveConsumer(value -> config.hud.showMemory = value)
                .build());

        hud.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.fps_overlay.showPing"),
                        config.hud.showPing)
                .setDefaultValue(true)
                .setTooltip(Component.translatable(TOOLTIP_SHOW_PING))
                .setYesNoTextSupplier(val -> val ? Component.literal("[ ON ]") : Component.literal("[ OFF ]"))
                .setSaveConsumer(value -> config.hud.showPing = value)
                .build());

        hud.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.fps_overlay.showMspt"),
                        config.hud.showMspt)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("tooltip.fps_overlay.showMspt"))
                .setYesNoTextSupplier(val -> val ? Component.literal("[ ON ]") : Component.literal("[ OFF ]"))
                .setSaveConsumer(value -> config.hud.showMspt = value)
                .build());

        hud.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.fps_overlay.showTps"),
                        config.hud.showTps)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("tooltip.fps_overlay.showTps"))
                .setYesNoTextSupplier(val -> val ? Component.literal("[ ON ]") : Component.literal("[ OFF ]"))
                .setSaveConsumer(value -> config.hud.showTps = value)
                .build());

        // --- Appearance Category ---
        ConfigCategory appearance = builder.getOrCreateCategory(Component.translatable(CATEGORY_APPEARANCE));

        appearance.addEntry(entryBuilder.startEnumSelector(
                Component.translatable("option.fps_overlay.overlay_style"),
                OverlayStyle.class,
                config.appearance.overlayStyle)
                .setDefaultValue(OverlayStyle.DEFAULT)
                .setSaveConsumer(value -> config.appearance.overlayStyle = value)
                .build());

        appearance.addEntry(entryBuilder.startEnumSelector(
                Component.translatable("option.fps_overlay.position"),
                OverlayPosition.class,
                config.appearance.position)
                .setDefaultValue(OverlayPosition.TOP_LEFT)
                .setTooltip(Component.translatable(TOOLTIP_POSITION))
                .setSaveConsumer(value -> config.appearance.position = value)
                .build());

        appearance.addEntry(entryBuilder
                .startSelector(Component.translatable("option.fps_overlay.hudScale"),
                        new Float[] { 0.65f, 0.8f, 0.95f },
                        config.appearance.hudScale)
                .setDefaultValue(0.65f)
                .setNameProvider(val -> {
                    if (val == 0.65f)
                        return Component.translatable("enum.fps_overlay.scale.small");
                    if (val == 0.8f)
                        return Component.translatable("enum.fps_overlay.scale.normal");
                    if (val == 0.95f)
                        return Component.translatable("enum.fps_overlay.scale.big");
                    return Component.literal(String.format("%.2fx", val));
                })
                .setSaveConsumer(value -> config.appearance.hudScale = value)
                .build());

        appearance.addEntry(entryBuilder
                .startSelector(Component.translatable("option.fps_overlay.updateInterval"),
                        new Integer[] { 16, 33, 50, 100, 250, 500, 1000 },
                        config.general.updateIntervalMs)
                .setDefaultValue(250)
                .setTooltip(Component.translatable(TOOLTIP_UPDATE_INTERVAL))
                .setNameProvider(val -> {
                    return switch (val) {
                        case 16 -> Component.translatable("enum.fps_overlay.update.16");
                        case 33 -> Component.translatable("enum.fps_overlay.update.33");
                        case 50 -> Component.translatable("enum.fps_overlay.update.50");
                        case 100 -> Component.translatable("enum.fps_overlay.update.100");
                        case 250 -> Component.translatable("enum.fps_overlay.update.250");
                        case 500 -> Component.translatable("enum.fps_overlay.update.500");
                        case 1000 -> Component.translatable("enum.fps_overlay.update.1000");
                        default -> Component.literal(val + " ms");
                    };
                })
                .setSaveConsumer(value -> config.general.updateIntervalMs = value)
                .build());

        appearance.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.fps_overlay.showBackground"),
                        config.appearance.showBackground)
                .setDefaultValue(true)
                .setTooltip(Component.translatable(TOOLTIP_SHOW_BACKGROUND))
                .setYesNoTextSupplier(val -> val ? Component.literal("[ ON ]") : Component.literal("[ OFF ]"))
                .setSaveConsumer(value -> config.appearance.showBackground = value)
                .build());

        appearance.addEntry(entryBuilder
                .startSelector(Component.translatable("option.fps_overlay.backgroundOpacity"),
                        new Integer[] { 0, 25, 50, 100, 150, 180, 200, 225, 255 },
                        config.appearance.backgroundOpacity)
                .setDefaultValue(180)
                .setTooltip(Component.translatable(TOOLTIP_BACKGROUND_OPACITY))
                .setNameProvider(val -> {
                    return switch (val) {
                        case 0 -> Component.translatable("enum.fps_overlay.opacity.0");
                        case 25 -> Component.translatable("enum.fps_overlay.opacity.25");
                        case 50 -> Component.translatable("enum.fps_overlay.opacity.50");
                        case 100 -> Component.translatable("enum.fps_overlay.opacity.100");
                        case 150 -> Component.translatable("enum.fps_overlay.opacity.150");
                        case 180 -> Component.translatable("enum.fps_overlay.opacity.180");
                        case 200 -> Component.translatable("enum.fps_overlay.opacity.200");
                        case 225 -> Component.translatable("enum.fps_overlay.opacity.225");
                        case 255 -> Component.translatable("enum.fps_overlay.opacity.255");
                        default -> Component.literal(val.toString());
                    };
                })
                .setSaveConsumer(value -> config.appearance.backgroundOpacity = value)
                .build());

        appearance.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.fps_overlay.autoHideF3"),
                        config.appearance.autoHideF3)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("tooltip.fps_overlay.autoHideF3"))
                .setYesNoTextSupplier(val -> val ? Component.literal("[ ON ]") : Component.literal("[ OFF ]"))
                .setSaveConsumer(value -> config.appearance.autoHideF3 = value)
                .build());

        appearance.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("option.fps_overlay.adaptiveColors"),
                        config.appearance.adaptiveColors)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("tooltip.fps_overlay.adaptiveColors"))
                .setYesNoTextSupplier(val -> val ? Component.literal("[ ON ]") : Component.literal("[ OFF ]"))
                .setSaveConsumer(value -> config.appearance.adaptiveColors = value)
                .build());

        return builder.build();
    }
}
