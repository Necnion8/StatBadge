package com.gmail.necnionch.myplugin.statbadge.bukkit.config;

import org.bukkit.configuration.ConfigurationSection;

public record StatsEntry(
        String id,
        String type,
        ConfigurationSection config
) {

    public static StatsEntry parse(String id, ConfigurationSection config) {
        return new StatsEntry(id, config.getString("type", "action"), config);
    }

}
