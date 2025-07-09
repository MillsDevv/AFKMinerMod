package com.mills.afkminermod.client.gui;

import com.mills.afkminermod.client.config.Config;
import com.mills.afkminermod.client.config.ConfigManager;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ConfigScreenBuilder {
    public static Screen create(Screen parent, ConfigManager configManager) {
        Config config = configManager.getConfig();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("AfkMiner Config"))
                .setSavingRunnable(configManager::save);

        ConfigCategory category = builder.getOrCreateCategory(Text.literal("Settings"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // duration of ability
        category.addEntry(entryBuilder
                .startIntField(Text.literal("Duration of super breaker ability (ticks)"), config.duration)
                .setDefaultValue(config.duration)
                .setSaveConsumer(newValue -> config.duration = newValue)
                .build());

        // check for ability
        category.addEntry(entryBuilder
                .startIntField(Text.literal("How often it checks for super breaker ability (ticks)"), config.checkForAbility)
                .setDefaultValue(config.checkForAbility)
                .setSaveConsumer(newValue -> config.checkForAbility = newValue)
                .build());

        // Min Z coordinate
        category.addEntry(entryBuilder
                .startIntField(Text.literal("Minimum Z coordinate of mining build"), config.minZCoordinate)
                .setDefaultValue(config.minZCoordinate)
                .setSaveConsumer(newValue -> config.minZCoordinate = newValue)
                .build());

        // Max Z coordinate
        category.addEntry(entryBuilder
                .startIntField(Text.literal("Manimum Z coordinate of mining build"), config.maxZCoordinate)
                .setDefaultValue(config.maxZCoordinate)
                .setSaveConsumer(newValue -> config.maxZCoordinate = newValue)
                .build());

        // rotation reset
        category.addEntry(entryBuilder
                .startBooleanToggle(Text.literal("Toggles if you will fix your yaw and pitch gets reset after rotation"),
                        config.rotationReset)
                .setDefaultValue(config.rotationReset)
                .setSaveConsumer(newValue -> config.rotationReset = newValue)
                .build());

        // instant rotation reset
        category.addEntry(entryBuilder
                .startBooleanToggle(Text.literal("Toggles if your rotation reset is instant or gets reset on a delay"),
                        config.instantRotationReset)
                .setDefaultValue(config.instantRotationReset)
                .setSaveConsumer(newValue -> config.instantRotationReset = newValue)
                .build());

        // rotation notification
        category.addEntry(entryBuilder
                .startBooleanToggle(Text.literal("Toggles if you get a notification on rotation (windows only)"),
                        config.windowsNotificationOnRotation)
                .setDefaultValue(config.windowsNotificationOnRotation)
                .setSaveConsumer(newValue -> config.windowsNotificationOnRotation = newValue)
                .build());

        // rotation threshold
        category.addEntry(entryBuilder
                .startFloatField(Text.literal("The threshold on how much of rotation to detect"), config.rotationThreshold)
                .setDefaultValue(config.rotationThreshold)
                .setSaveConsumer(newValue -> config.rotationThreshold = newValue)
                .build());

        // hotbar shuffle reset
        category.addEntry(entryBuilder
                .startBooleanToggle(Text.literal("Toggles if you will fix your selected item in hotbar after shuffle"),
                        config.resetHotbarOnInvShuffle)
                .setDefaultValue(config.resetHotbarOnInvShuffle)
                .setSaveConsumer(newValue -> config.resetHotbarOnInvShuffle = newValue)
                .build());

        // instant hotbar shuffle reset
        category.addEntry(entryBuilder
                .startBooleanToggle(Text.literal("Toggles if your shuffle reset is instant or gets reset on a delay"),
                        config.instantHotbarReset)
                .setDefaultValue(config.instantHotbarReset)
                .setSaveConsumer(newValue -> config.instantHotbarReset = newValue)
                .build());

        // hotbar shuffle notification
        category.addEntry(entryBuilder
                .startBooleanToggle(Text.literal("Toggles if you get a notification on shuffle (windows only)"),
                        config.windowsNotificationOnHotbarShuffle)
                .setDefaultValue(config.windowsNotificationOnHotbarShuffle)
                .setSaveConsumer(newValue -> config.windowsNotificationOnHotbarShuffle = newValue)
                .build());
        return builder.build();
    }
}
