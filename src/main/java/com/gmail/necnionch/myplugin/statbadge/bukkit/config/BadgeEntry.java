package com.gmail.necnionch.myplugin.statbadge.bukkit.config;

import org.bukkit.configuration.ConfigurationSection;

public record BadgeEntry(
        String id,
        String statsType,
        long statsValue,
        String name,
        String description,
        String title
) {

    public static BadgeEntry parse(String id, ConfigurationSection config) {
        return new BadgeEntry(
                id,
                config.getString("stats"),
                config.getLong("stats-value"),
                config.getString("name"),
                config.getString("description"),
                config.getString("title")
        );
    }

}
