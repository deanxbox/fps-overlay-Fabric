package net.hicham.fps_overlay;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConfigScreenFactory {
    private static final String NO_PROFILE_VALUE = "(No Profile)";

    public static Screen createConfigScreen(Screen parent) {
        return new ConfigHubScreen(parent);
    }

    public static Screen createSettingsScreen(Screen parent) {
        ModConfig config = FpsOverlayMod.getConfigForEditing();
        ModConfig defaults = new ModConfig();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("title.fps_overlay.config"))
                .save(() -> FpsOverlayMod.saveConfigForCurrentContext(config))
                .category(buildGeneralCategory(config, defaults))
                .category(buildAppearanceCategory(config, defaults))
                .category(buildProfileCategory(parent, config))
                .category(buildColorCategory(config, defaults))
                .category(buildThresholdCategory(config, defaults))
                .category(buildAutoHideCategory(config, defaults))
                .build()
                .generateScreen(parent);
    }

    private static ConfigCategory buildGeneralCategory(ModConfig config, ModConfig defaults) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("category.fps_overlay.hud"))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("category.fps_overlay.hud"))
                        .option(booleanOption("option.fps_overlay.enabled", defaults.general.enabled,
                                () -> config.general.enabled, value -> config.general.enabled = value))
                        .option(booleanOption("option.fps_overlay.enableKeybindings", defaults.general.enableKeybindings,
                                () -> config.general.enableKeybindings, value -> config.general.enableKeybindings = value))
                        .option(booleanOption("option.fps_overlay.showGraph", defaults.hud.showGraph,
                                () -> config.hud.showGraph, value -> config.hud.showGraph = value))
                        .option(booleanOption("option.fps_overlay.showPingGraph", defaults.hud.showPingGraph,
                                () -> config.hud.showPingGraph, value -> config.hud.showPingGraph = value))
                        .option(Option.<ModConfig.MemoryDisplayMode>createBuilder()
                                .name(Component.translatable("option.fps_overlay.memoryDisplayMode"))
                                .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.memoryDisplayMode")))
                                .binding(defaults.hud.memoryDisplayMode, () -> config.hud.memoryDisplayMode,
                                        value -> config.hud.memoryDisplayMode = value)
                                .controller(builder -> EnumControllerBuilder.create(builder)
                                        .enumClass(ModConfig.MemoryDisplayMode.class)
                                        .formatValue(value -> Component.translatable("enum.fps_overlay.memorydisplay." + value.name().toLowerCase(Locale.ROOT))))
                                .build())
                        .option(Option.<ModConfig.MemoryUnit>createBuilder()
                                .name(Component.translatable("option.fps_overlay.memoryUnit"))
                                .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.memoryUnit")))
                                .binding(defaults.hud.memoryUnit, () -> config.hud.memoryUnit,
                                        value -> config.hud.memoryUnit = value)
                                .controller(builder -> EnumControllerBuilder.create(builder)
                                        .enumClass(ModConfig.MemoryUnit.class)
                                        .formatValue(value -> Component.translatable("enum.fps_overlay.memoryunit." + value.name().toLowerCase(Locale.ROOT))))
                                .build())
                        .option(Option.<ModConfig.ClockFormat>createBuilder()
                                .name(Component.translatable("option.fps_overlay.clockFormat"))
                                .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.clockFormat")))
                                .binding(defaults.hud.clockFormat, () -> config.hud.clockFormat,
                                        value -> config.hud.clockFormat = value)
                                .controller(builder -> EnumControllerBuilder.create(builder)
                                        .enumClass(ModConfig.ClockFormat.class)
                                        .formatValue(value -> Component.translatable("enum.fps_overlay.clockformat." + value.name().toLowerCase(Locale.ROOT))))
                                .build())
                        .option(booleanOption("option.fps_overlay.showMinMaxStats", defaults.hud.showMinMaxStats,
                                () -> config.hud.showMinMaxStats, value -> config.hud.showMinMaxStats = value))
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("option.fps_overlay.updateInterval"))
                                .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.updateInterval")))
                                .binding(defaults.general.updateIntervalMs, () -> config.general.updateIntervalMs,
                                        value -> config.general.updateIntervalMs = value)
                                .controller(builder -> IntegerSliderControllerBuilder.create(builder)
                                        .range(0, 1000)
                                        .step(1)
                                        .formatValue(value -> Component.literal(value <= 0 ? "Live" : value + "ms")))
                                .build())
                        .build())
                .build();
    }

    private static ConfigCategory buildAppearanceCategory(ModConfig config, ModConfig defaults) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("category.fps_overlay.appearance"))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("category.fps_overlay.appearance"))
                        .option(Option.<ModConfig.OverlayStyle>createBuilder()
                                .name(Component.translatable("option.fps_overlay.overlay_style"))
                                .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.overlay_style")))
                                .binding(defaults.appearance.overlayStyle, () -> config.appearance.overlayStyle,
                                        value -> config.appearance.overlayStyle = value)
                                .controller(builder -> EnumControllerBuilder.create(builder)
                                        .enumClass(ModConfig.OverlayStyle.class)
                                        .formatValue(value -> Component.translatable("enum.fps_overlay.overlaystyle." + value.name().toLowerCase(Locale.ROOT))))
                                .build())
                        .option(Option.<ModConfig.OverlayPosition>createBuilder()
                                .name(Component.translatable("option.fps_overlay.position"))
                                .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.position")))
                                .binding(defaults.appearance.position, () -> config.appearance.position,
                                        value -> config.appearance.position = value)
                                .controller(builder -> EnumControllerBuilder.create(builder)
                                        .enumClass(ModConfig.OverlayPosition.class)
                                        .formatValue(ModConfig.OverlayPosition::getDisplayText))
                                .build())
                        .option(floatSlider("option.fps_overlay.hudScale", "tooltip.fps_overlay.hud_scale",
                                defaults.appearance.hudScale, () -> config.appearance.hudScale,
                                value -> config.appearance.hudScale = value, 0.2f, 1.5f, 0.05f,
                                value -> Component.literal((int) (value * 100) + "%")))
                        .option(intSlider("option.fps_overlay.xOffset", "tooltip.fps_overlay.xOffset",
                                defaults.appearance.xOffset, () -> config.appearance.xOffset,
                                value -> config.appearance.xOffset = value, -1000, 1000,
                                value -> Component.literal(value + "px")))
                        .option(intSlider("option.fps_overlay.yOffset", "tooltip.fps_overlay.yOffset",
                                defaults.appearance.yOffset, () -> config.appearance.yOffset,
                                value -> config.appearance.yOffset = value, -1000, 1000,
                                value -> Component.literal(value + "px")))
                        .option(booleanOption("option.fps_overlay.showBackground", defaults.appearance.showBackground,
                                () -> config.appearance.showBackground, value -> config.appearance.showBackground = value))
                        .option(intSlider("option.fps_overlay.backgroundOpacity", "tooltip.fps_overlay.backgroundOpacity",
                                defaults.appearance.backgroundOpacity, () -> config.appearance.backgroundOpacity,
                                value -> config.appearance.backgroundOpacity = value, 0, 255,
                                value -> Component.literal(value + " (" + (int) (value * 100 / 255f) + "%)")))
                        .option(booleanOption("option.fps_overlay.adaptiveColors", defaults.appearance.adaptiveColors,
                                () -> config.appearance.adaptiveColors, value -> config.appearance.adaptiveColors = value))
                        .option(Option.<ModConfig.TextEffect>createBuilder()
                                .name(Component.translatable("option.fps_overlay.textEffect"))
                                .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.textEffect")))
                                .binding(defaults.appearance.textEffect, () -> config.appearance.textEffect,
                                        value -> config.appearance.textEffect = value)
                                .controller(builder -> EnumControllerBuilder.create(builder)
                                        .enumClass(ModConfig.TextEffect.class)
                                        .formatValue(value -> Component.translatable("enum.fps_overlay.textEffect." + value.name().toLowerCase(Locale.ROOT))))
                                .build())
                        .build())
                .build();
    }

    private static ConfigCategory buildColorCategory(ModConfig config, ModConfig defaults) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("category.fps_overlay.colors"))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("category.fps_overlay.colors"))
                        .option(colorOption("option.fps_overlay.backgroundColor",
                                withAlpha(defaults.appearance.backgroundColor, defaults.appearance.backgroundOpacity),
                                () -> withAlpha(config.appearance.backgroundColor, config.appearance.backgroundOpacity),
                                value -> {
                                    config.appearance.backgroundColor = value & 0x00FFFFFF;
                                    config.appearance.backgroundOpacity = (value >>> 24) & 0xFF;
                                }))
                        .option(colorOption("option.fps_overlay.labelColor", defaults.appearance.labelColor,
                                () -> config.appearance.labelColor, value -> config.appearance.labelColor = value))
                        .option(colorOption("option.fps_overlay.valueColor", defaults.appearance.valueColor,
                                () -> config.appearance.valueColor, value -> config.appearance.valueColor = value))
                        .option(colorOption("option.fps_overlay.unitColor", defaults.appearance.unitColor,
                                () -> config.appearance.unitColor, value -> config.appearance.unitColor = value))
                        .option(colorOption("option.fps_overlay.dividerColor", defaults.appearance.dividerColor,
                                () -> config.appearance.dividerColor, value -> config.appearance.dividerColor = value))
                        .option(colorOption("option.fps_overlay.goodColor", defaults.appearance.goodColor,
                                () -> config.appearance.goodColor, value -> config.appearance.goodColor = value))
                        .option(colorOption("option.fps_overlay.warningColor", defaults.appearance.warningColor,
                                () -> config.appearance.warningColor, value -> config.appearance.warningColor = value))
                        .option(colorOption("option.fps_overlay.badColor", defaults.appearance.badColor,
                                () -> config.appearance.badColor, value -> config.appearance.badColor = value))
                        .build())
                .build();
    }

    private static ConfigCategory buildProfileCategory(Screen parent, ModConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("category.fps_overlay.profiles"))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("group.fps_overlay.profiles.actions"))
                        .option(ButtonOption.createBuilder()
                                .name(Component.translatable("button.fps_overlay.save_profile"))
                                .text(Component.translatable("button.fps_overlay.save_profile"))
                                .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.save_profile")))
                                .action((screen, option) -> Minecraft.getInstance().setScreen(
                                        new ProfileNameScreen((Screen) screen,
                                                Component.translatable("screen.fps_overlay.save_profile"),
                                                config.selectedProfile,
                                                name -> {
                                                    saveProfile(config, name);
                                                    Minecraft.getInstance().setScreen(createSettingsScreen(parent));
                                                })))
                                .build())
                        .option(ButtonOption.createBuilder()
                                .name(Component.translatable("button.fps_overlay.update_profile"))
                                .text(Component.translatable("button.fps_overlay.update_profile"))
                                .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.update_profile")))
                                .available(hasSelectedProfile(config))
                                .action((screen, option) -> {
                                    updateSelectedProfile(config);
                                    Minecraft.getInstance().setScreen(createSettingsScreen(parent));
                                })
                                .build())
                        .option(ButtonOption.createBuilder()
                                .name(Component.translatable("button.fps_overlay.rename_profile"))
                                .text(Component.translatable("button.fps_overlay.rename_profile"))
                                .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.rename_profile")))
                                .available(hasSelectedProfile(config))
                                .action((screen, option) -> Minecraft.getInstance().setScreen(
                                        new ProfileNameScreen((Screen) screen,
                                                Component.translatable("screen.fps_overlay.rename_profile"),
                                                config.selectedProfile,
                                                name -> {
                                                    renameProfile(config, name);
                                                    Minecraft.getInstance().setScreen(createSettingsScreen(parent));
                                                })))
                                .build())
                        .option(ButtonOption.createBuilder()
                                .name(Component.translatable("button.fps_overlay.delete_profile"))
                                .text(Component.translatable("button.fps_overlay.delete_profile"))
                                .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.delete_profile")))
                                .available(hasSelectedProfile(config))
                                .action((screen, option) -> {
                                    deleteProfile(config);
                                    Minecraft.getInstance().setScreen(createSettingsScreen(parent));
                                })
                                .build())
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("group.fps_overlay.profiles.available"))
                        .option(ButtonOption.createBuilder()
                                .name(Component.translatable("option.fps_overlay.configProfile"))
                                .text(Component.literal(getProfileSelectionValue(config)))
                                .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.configProfile")))
                                .action((screen, option) -> Minecraft.getInstance().setScreen(
                                        new ProfileSelectScreen((Screen) screen, config,
                                                profileName -> {
                                                    selectProfile(config, profileName);
                                                    Minecraft.getInstance().setScreen(createSettingsScreen(parent));
                                                })))
                                .build())
                        .build())
                .build();
    }

    private static ConfigCategory buildThresholdCategory(ModConfig config, ModConfig defaults) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("category.fps_overlay.thresholds"))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("group.fps_overlay.thresholds.fps"))
                        .option(intSlider("option.fps_overlay.thresholds.fpsGood", "tooltip.fps_overlay.thresholds.fpsGood",
                                defaults.thresholds.fpsGood, () -> config.thresholds.fpsGood,
                                value -> config.thresholds.fpsGood = value, 1, 240, value -> Component.literal(value + " FPS")))
                        .option(intSlider("option.fps_overlay.thresholds.fpsWarning", "tooltip.fps_overlay.thresholds.fpsWarning",
                                defaults.thresholds.fpsWarning, () -> config.thresholds.fpsWarning,
                                value -> config.thresholds.fpsWarning = value, 1, 240, value -> Component.literal(value + " FPS")))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("group.fps_overlay.thresholds.timing"))
                        .option(doubleSlider("option.fps_overlay.thresholds.frameTimeGood", "tooltip.fps_overlay.thresholds.frameTimeGood",
                                defaults.thresholds.frameTimeGood, () -> config.thresholds.frameTimeGood,
                                value -> config.thresholds.frameTimeGood = value, 1.0, 100.0,
                                value -> Component.literal(String.format(Locale.ROOT, "%.1f ms", value))))
                        .option(doubleSlider("option.fps_overlay.thresholds.frameTimeWarning", "tooltip.fps_overlay.thresholds.frameTimeWarning",
                                defaults.thresholds.frameTimeWarning, () -> config.thresholds.frameTimeWarning,
                                value -> config.thresholds.frameTimeWarning = value, 1.0, 100.0,
                                value -> Component.literal(String.format(Locale.ROOT, "%.1f ms", value))))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("group.fps_overlay.thresholds.network"))
                        .option(intSlider("option.fps_overlay.thresholds.pingGood", "tooltip.fps_overlay.thresholds.pingGood",
                                defaults.thresholds.pingGood, () -> config.thresholds.pingGood,
                                value -> config.thresholds.pingGood = value, 0, 500, value -> Component.literal(value + " ms")))
                        .option(intSlider("option.fps_overlay.thresholds.pingWarning", "tooltip.fps_overlay.thresholds.pingWarning",
                                defaults.thresholds.pingWarning, () -> config.thresholds.pingWarning,
                                value -> config.thresholds.pingWarning = value, 0, 1000, value -> Component.literal(value + " ms")))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("group.fps_overlay.thresholds.server"))
                        .option(doubleSlider("option.fps_overlay.thresholds.tpsGood", "tooltip.fps_overlay.thresholds.tpsGood",
                                defaults.thresholds.tpsGood, () -> config.thresholds.tpsGood,
                                value -> config.thresholds.tpsGood = value, 0.1, 20.0,
                                value -> Component.literal(String.format(Locale.ROOT, "%.1f TPS", value))))
                        .option(doubleSlider("option.fps_overlay.thresholds.tpsWarning", "tooltip.fps_overlay.thresholds.tpsWarning",
                                defaults.thresholds.tpsWarning, () -> config.thresholds.tpsWarning,
                                value -> config.thresholds.tpsWarning = value, 0.1, 20.0,
                                value -> Component.literal(String.format(Locale.ROOT, "%.1f TPS", value))))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("group.fps_overlay.thresholds.resources"))
                        .option(intSlider("option.fps_overlay.thresholds.memoryGood", "tooltip.fps_overlay.thresholds.memoryGood",
                                defaults.thresholds.memoryGood, () -> config.thresholds.memoryGood,
                                value -> config.thresholds.memoryGood = value, 1, 100, value -> Component.literal(value + "%")))
                        .option(intSlider("option.fps_overlay.thresholds.memoryWarning", "tooltip.fps_overlay.thresholds.memoryWarning",
                                defaults.thresholds.memoryWarning, () -> config.thresholds.memoryWarning,
                                value -> config.thresholds.memoryWarning = value, 1, 100, value -> Component.literal(value + "%")))
                        .build())
                .build();
    }

    private static ConfigCategory buildAutoHideCategory(ModConfig config, ModConfig defaults) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("category.fps_overlay.autohide"))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("category.fps_overlay.autohide"))
                        .option(booleanOption("option.fps_overlay.autohide.chat", defaults.autoHide.hideInChat,
                                () -> config.autoHide.hideInChat, value -> config.autoHide.hideInChat = value))
                        .option(booleanOption("option.fps_overlay.autohide.inventory", defaults.autoHide.hideInInventory,
                                () -> config.autoHide.hideInInventory, value -> config.autoHide.hideInInventory = value))
                        .option(booleanOption("option.fps_overlay.autohide.screenshot", defaults.autoHide.hideInScreenshots,
                                () -> config.autoHide.hideInScreenshots, value -> config.autoHide.hideInScreenshots = value))
                        .option(booleanOption("option.fps_overlay.autoHideF3", defaults.autoHide.hideWithF3,
                                () -> config.autoHide.hideWithF3, value -> {
                                    config.autoHide.hideWithF3 = value;
                                    config.appearance.autoHideF3 = value;
                                }))
                        .option(booleanOption("option.fps_overlay.autohide.fade", defaults.autoHide.fadeOnIdle,
                                () -> config.autoHide.fadeOnIdle, value -> config.autoHide.fadeOnIdle = value))
                        .option(intSlider("option.fps_overlay.autohide.delay", "tooltip.fps_overlay.autohide.delay",
                                defaults.autoHide.fadeDelaySeconds, () -> config.autoHide.fadeDelaySeconds,
                                value -> config.autoHide.fadeDelaySeconds = value, 1, 600,
                                value -> Component.literal(value + "s")))
                        .option(floatSlider("option.fps_overlay.autohide.opacity", "tooltip.fps_overlay.autohide.opacity",
                                defaults.autoHide.fadeOpacity, () -> config.autoHide.fadeOpacity,
                                value -> config.autoHide.fadeOpacity = value, 0.0f, 1.0f, 0.05f,
                                value -> Component.literal((int) (value * 100) + "%")))
                        .build())
                .build();
    }

    private static Option<Boolean> booleanOption(String key, boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable(key))
                .binding(defaultValue, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    private static Option<Integer> intSlider(String key, String tooltipKey, int defaultValue, Supplier<Integer> getter,
            Consumer<Integer> setter, int min, int max, Function<Integer, Component> formatter) {
        return Option.<Integer>createBuilder()
                .name(Component.translatable(key))
                .description(OptionDescription.of(Component.translatable(tooltipKey)))
                .binding(defaultValue, getter, setter)
                .controller(builder -> IntegerSliderControllerBuilder.create(builder)
                        .range(min, max)
                        .step(1)
                        .formatValue(formatter::apply))
                .build();
    }

    private static Option<Float> floatSlider(String key, String tooltipKey, float defaultValue, Supplier<Float> getter,
            Consumer<Float> setter, float min, float max, float step, Function<Float, Component> formatter) {
        return Option.<Float>createBuilder()
                .name(Component.translatable(key))
                .description(OptionDescription.of(Component.translatable(tooltipKey)))
                .binding(defaultValue, getter, setter)
                .controller(builder -> FloatSliderControllerBuilder.create(builder)
                        .range(min, max)
                        .step(step)
                        .formatValue(formatter::apply))
                .build();
    }

    private static Option<Double> doubleSlider(String key, String tooltipKey, double defaultValue, Supplier<Double> getter,
            Consumer<Double> setter, double min, double max, Function<Double, Component> formatter) {
        return Option.<Double>createBuilder()
                .name(Component.translatable(key))
                .description(OptionDescription.of(Component.translatable(tooltipKey)))
                .binding(defaultValue, getter, setter)
                .controller(builder -> DoubleSliderControllerBuilder.create(builder)
                        .range(min, max)
                        .step(0.1)
                        .formatValue(formatter::apply))
                .build();
    }

    private static Option<Color> colorOption(String key, int defaultValue, Supplier<Integer> getter, Consumer<Integer> setter) {
        return Option.<Color>createBuilder()
                .name(Component.translatable(key))
                .binding(new Color(defaultValue, true), () -> new Color(getter.get(), true), value -> setter.accept(value.getRGB()))
                .controller(builder -> ColorControllerBuilder.create(builder).allowAlpha(true))
                .build();
    }

    private static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    private static boolean hasSelectedProfile(ModConfig config) {
        return config.selectedProfile != null
                && !config.selectedProfile.isBlank()
                && config.configProfiles.containsKey(config.selectedProfile);
    }

    private static void saveProfile(ModConfig config, String name) {
        config.saveProfile(name);
        FpsOverlayMod.saveConfigForCurrentContext(config);
    }

    private static void updateSelectedProfile(ModConfig config) {
        if (!hasSelectedProfile(config)) {
            return;
        }
        config.saveProfile(config.selectedProfile);
        FpsOverlayMod.saveConfigForCurrentContext(config);
    }

    private static void renameProfile(ModConfig config, String newName) {
        config.renameProfile(config.selectedProfile, newName);
        FpsOverlayMod.saveConfigForCurrentContext(config);
    }

    private static void deleteProfile(ModConfig config) {
        config.deleteProfile(config.selectedProfile);
        FpsOverlayMod.saveConfigForCurrentContext(config);
    }

    private static void selectProfile(ModConfig config, String profileName) {
        if (NO_PROFILE_VALUE.equals(profileName)) {
            config.selectedProfile = "";
            FpsOverlayMod.saveConfigForCurrentContext(config);
            return;
        }

        config.applyProfile(profileName);
        FpsOverlayMod.saveConfigForCurrentContext(config);
    }

    private static String getProfileSelectionValue(ModConfig config) {
        return hasSelectedProfile(config) ? config.selectedProfile : NO_PROFILE_VALUE;
    }
}
