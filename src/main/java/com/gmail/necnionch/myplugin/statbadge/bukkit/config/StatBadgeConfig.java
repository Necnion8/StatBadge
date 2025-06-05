package com.gmail.necnionch.myplugin.statbadge.bukkit.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class StatBadgeConfig extends BukkitConfiguration {

    private final Map<String, StatsEntry> stats = new HashMap<>();
    private final Map<String, BadgeEntry> badges = new HashMap<>();

    public StatBadgeConfig(Plugin plugin) {
        super(plugin);
    }

    @Override
    protected boolean onLoaded() {
        stats.clear();
        ConfigurationSection section = config.getConfigurationSection("stats");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                stats.put(id, StatsEntry.parse(id, section.getConfigurationSection(id)));
            }
        }
        badges.clear();
        section = config.getConfigurationSection("badges");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                //noinspection DataFlowIssue
                badges.put(id, BadgeEntry.parse(id, section.getConfigurationSection(id)));
            }
        }
        return true;
    }

    public Map<String, StatsEntry> stats() {
        return stats;
    }

    public Map<String, BadgeEntry> badges() {
        return badges;
    }

    public boolean isDebugEnable() {
        return true;  // TODO:
    }

}
