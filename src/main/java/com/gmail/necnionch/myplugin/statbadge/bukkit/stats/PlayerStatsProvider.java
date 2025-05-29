package com.gmail.necnionch.myplugin.statbadge.bukkit.stats;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public abstract class PlayerStatsProvider {

    private final Plugin plugin;

    public PlayerStatsProvider(Plugin plugin) {
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public abstract PlayerStats create(UUID playerId, String statsId, ConfigurationSection config);

}
