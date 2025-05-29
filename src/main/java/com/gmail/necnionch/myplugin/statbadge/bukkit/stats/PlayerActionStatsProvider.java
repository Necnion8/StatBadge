package com.gmail.necnionch.myplugin.statbadge.bukkit.stats;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public abstract class PlayerActionStatsProvider {

    private final Plugin plugin;

    public PlayerActionStatsProvider(Plugin plugin) {
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public abstract PlayerActionStats create(UUID playerId, String statsId, ConfigurationSection config);

}
