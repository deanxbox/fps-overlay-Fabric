package net.hicham.fps_overlay;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.Locale;

public class ConfigScreenFactory {
    public static Screen createConfigScreen(Screen parent) {
        return new ConfigHubScreen(parent);
    }

    public static Screen createSettingsScreen(Screen parent) {
        ModConfig config = ConfigManager.getConfig();
        ModConfig defaults = new ModConfig();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("title.fps_overlay.config"))
                .save(ConfigManager::saveConfig)
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("category.fps_overlay.hud"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("category.fps_overlay.hud"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.translatable("option.fps_overlay.enabled"))
                                        .binding(defaults.general.enabled, () -> config.general.enabled, value -> config.general.enabled = value)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.translatable("option.fps_overlay.enableKeybindings"))
                                        .binding(defaults.general.enableKeybindings, () -> config.general.enableKeybindings, value -> config.general.enableKeybindings = value)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.translatable("option.fps_overlay.showGraph"))
                                        .binding(defaults.hud.showGraph, () -> config.hud.showGraph, value -> config.hud.showGraph = value)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.translatable("option.fps_overlay.showMinMaxStats"))
                                        .binding(defaults.hud.showMinMaxStats, () -> config.hud.showMinMaxStats, value -> config.hud.showMinMaxStats = value)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("category.fps_overlay.appearance"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("category.fps_overlay.appearance"))
                                .option(Option.<ModConfig.OverlayStyle>createBuilder()
                                        .name(Component.translatable("option.fps_overlay.overlay_style"))
                                        .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.overlay_style")))
                                        .binding(defaults.appearance.overlayStyle, () -> config.appearance.overlayStyle, value -> config.appearance.overlayStyle = value)
                                        .controller(builder -> EnumControllerBuilder.create(builder)
                                                .enumClass(ModConfig.OverlayStyle.class)
                                                .formatValue(value -> Component.translatable("enum.fps_overlay.overlaystyle." + value.name().toLowerCase(Locale.ROOT))))
                                        .build())
                                .option(Option.<ModConfig.OverlayPosition>createBuilder()
                                        .name(Component.translatable("option.fps_overlay.position"))
                                        .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.position")))
                                        .binding(defaults.appearance.position, () -> config.appearance.position, value -> config.appearance.position = value)
                                        .controller(builder -> EnumControllerBuilder.create(builder)
                                                .enumClass(ModConfig.OverlayPosition.class)
                                                .formatValue(ModConfig.OverlayPosition::getDisplayText))
                                        .build())
                                .option(Option.<Float>createBuilder()
                                        .name(Component.translatable("option.fps_overlay.hudScale"))
                                        .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.hud_scale")))
                                        .binding(defaults.appearance.hudScale, () -> config.appearance.hudScale, value -> config.appearance.hudScale = value)
                                        .controller(builder -> FloatSliderControllerBuilder.create(builder)
                                                .range(0.2f, 1.5f)
                                                .step(0.05f)
                                                .formatValue(value -> Component.literal((int)(value * 100) + "%")))
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("option.fps_overlay.updateInterval"))
                                        .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.updateInterval")))
                                        .binding(defaults.general.updateIntervalMs, () -> config.general.updateIntervalMs, value -> config.general.updateIntervalMs = value)
                                        .controller(builder -> IntegerSliderControllerBuilder.create(builder)
                                                .range(16, 1000)
                                                .step(1)
                                                .formatValue(value -> {
                                                    if (value == 16) return Component.translatable("enum.fps_overlay.update.16");
                                                    if (value == 33) return Component.translatable("enum.fps_overlay.update.33");
                                                    if (value == 50) return Component.translatable("enum.fps_overlay.update.50");
                                                    if (value == 100) return Component.translatable("enum.fps_overlay.update.100");
                                                    if (value == 250) return Component.translatable("enum.fps_overlay.update.250");
                                                    if (value == 500) return Component.translatable("enum.fps_overlay.update.500");
                                                    if (value == 1000) return Component.translatable("enum.fps_overlay.update.1000");
                                                    return Component.literal(value + "ms");
                                                }))
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("option.fps_overlay.xOffset"))
                                        .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.xOffset")))
                                        .binding(defaults.appearance.xOffset, () -> config.appearance.xOffset, value -> config.appearance.xOffset = value)
                                        .controller(builder -> IntegerSliderControllerBuilder.create(builder)
                                                .range(-1000, 1000)
                                                .step(1)
                                                .formatValue(value -> Component.literal(value + "px")))
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("option.fps_overlay.yOffset"))
                                        .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.yOffset")))
                                        .binding(defaults.appearance.yOffset, () -> config.appearance.yOffset, value -> config.appearance.yOffset = value)
                                        .controller(builder -> IntegerSliderControllerBuilder.create(builder)
                                                .range(-1000, 1000)
                                                .step(1)
                                                .formatValue(value -> Component.literal(value + "px")))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.translatable("option.fps_overlay.showBackground"))
                                        .binding(defaults.appearance.showBackground, () -> config.appearance.showBackground, value -> config.appearance.showBackground = value)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("option.fps_overlay.backgroundOpacity"))
                                        .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.backgroundOpacity")))
                                        .binding(defaults.appearance.backgroundOpacity, () -> config.appearance.backgroundOpacity, value -> config.appearance.backgroundOpacity = value)
                                        .controller(builder -> IntegerSliderControllerBuilder.create(builder)
                                                .range(0, 255)
                                                .step(1)
                                                .formatValue(value -> Component.literal(value + " (" + (int)(value * 100 / 255f) + "%)")))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.translatable("option.fps_overlay.autoHideF3"))
                                        .binding(defaults.appearance.autoHideF3, () -> config.appearance.autoHideF3, value -> config.appearance.autoHideF3 = value)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.translatable("option.fps_overlay.adaptiveColors"))
                                        .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.adaptiveColors")))
                                        .binding(defaults.appearance.adaptiveColors, () -> config.appearance.adaptiveColors, value -> config.appearance.adaptiveColors = value)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<ModConfig.TextEffect>createBuilder()
                                        .name(Component.translatable("option.fps_overlay.textEffect"))
                                        .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.textEffect")))
                                        .binding(defaults.appearance.textEffect, () -> config.appearance.textEffect, value -> config.appearance.textEffect = value)
                                        .controller(builder -> EnumControllerBuilder.create(builder)
                                                .enumClass(ModConfig.TextEffect.class)
                                                .formatValue(value -> Component.translatable("enum.fps_overlay.textEffect." + value.name().toLowerCase(Locale.ROOT))))
                                        .build())
                                .option(Option.<ModConfig.ThemePreset>createBuilder()
                                        .name(Component.translatable("option.fps_overlay.themePreset"))
                                        .description(OptionDescription.of(Component.translatable("tooltip.fps_overlay.themePreset")))
                                        .binding(defaults.appearance.themePreset, () -> config.appearance.themePreset, value -> {
                                            config.appearance.applyThemePreset(value);
                                        })
                                        .controller(builder -> EnumControllerBuilder.create(builder)
                                                .enumClass(ModConfig.ThemePreset.class)
                                                .formatValue(value -> Component.translatable("enum.fps_overlay.themePreset." + value.name().toLowerCase(Locale.ROOT))))
                                        .build())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("category.fps_overlay.colors"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("category.fps_overlay.colors"))
                                .option(colorOption("option.fps_overlay.backgroundColor", defaults.appearance.backgroundColor, () -> config.appearance.backgroundColor, value -> config.appearance.backgroundColor = value, config))
                                .option(colorOption("option.fps_overlay.labelColor", defaults.appearance.labelColor, () -> config.appearance.labelColor, value -> config.appearance.labelColor = value, config))
                                .option(colorOption("option.fps_overlay.valueColor", defaults.appearance.valueColor, () -> config.appearance.valueColor, value -> config.appearance.valueColor = value, config))
                                .option(colorOption("option.fps_overlay.unitColor", defaults.appearance.unitColor, () -> config.appearance.unitColor, value -> config.appearance.unitColor = value, config))
                                .option(colorOption("option.fps_overlay.dividerColor", defaults.appearance.dividerColor, () -> config.appearance.dividerColor, value -> config.appearance.dividerColor = value, config))
                                .option(colorOption("option.fps_overlay.goodColor", defaults.appearance.goodColor, () -> config.appearance.goodColor, value -> config.appearance.goodColor = value, config))
                                .option(colorOption("option.fps_overlay.warningColor", defaults.appearance.warningColor, () -> config.appearance.warningColor, value -> config.appearance.warningColor = value, config))
                                .option(colorOption("option.fps_overlay.badColor", defaults.appearance.badColor, () -> config.appearance.badColor, value -> config.appearance.badColor = value, config))
                                .build())
                        .build())
                .build()
                .generateScreen(parent);
    }

    private static Option<Color> colorOption(String key, int defaultValue, java.util.function.Supplier<Integer> getter, java.util.function.Consumer<Integer> setter, ModConfig config) {
        return Option.<Color>createBuilder()
                .name(Component.translatable(key))
                .binding(new Color(defaultValue, true), () -> new Color(getter.get(), true), value -> {
                    setter.accept(value.getRGB());
                    config.appearance.themePreset = ModConfig.ThemePreset.CUSTOM;
                })
                .controller(builder -> ColorControllerBuilder.create(builder)
                        .allowAlpha(true))
                .build();
    }
}
