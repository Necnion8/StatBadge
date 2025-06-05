package com.gmail.necnionch.myplugin.statbadge.bukkit.config;

import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class BadgesConfig extends BukkitConfiguration {

    private final Map<String, BadgeEntry> badges = new HashMap<>();

    public BadgesConfig(Plugin plugin) {
        super(plugin, "badges.yml", "badges.yml");
    }

    @Override
    protected boolean onLoaded() {
        badges.clear();
        for (String id : config.getKeys(false)) {
            //noinspection DataFlowIssue
            badges.put(id, BadgeEntry.parse(id, config.getConfigurationSection(id)));
        }
        return true;
    }

    public Map<String, BadgeEntry> badges() {
        return badges;
    }

}
