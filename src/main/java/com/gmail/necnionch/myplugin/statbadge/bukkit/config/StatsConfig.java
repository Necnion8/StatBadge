package com.gmail.necnionch.myplugin.statbadge.bukkit.config;

import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class StatsConfig extends BukkitConfiguration {

    private final Map<String, StatsEntry> stats = new HashMap<>();

    public StatsConfig(Plugin plugin) {
        super(plugin, "stats.yml", "stats.yml");
    }

    @Override
    protected boolean onLoaded() {
        stats.clear();
        for (String id : config.getKeys(false)) {
            stats.put(id, StatsEntry.parse(id, config.getConfigurationSection(id)));
        }
        return true;
    }

    public Map<String, StatsEntry> stats() {
        return stats;
    }

}
