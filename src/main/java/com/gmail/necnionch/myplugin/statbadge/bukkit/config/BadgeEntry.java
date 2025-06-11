package com.gmail.necnionch.myplugin.statbadge.bukkit.config;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

public record BadgeEntry(
        String id,
        String name,
        String description,
        @Nullable String title,
        Icon icon,
        Stats stats,
        Completes completes
) {

    public record Icon(Material type, @Nullable Object customModelData) {
    }

    public record Stats(String type, long targetValue, ConfigurationSection config) {
    }

    public record Completes(@Nullable Sound sound, boolean useDefaultSound) {
    }

}
