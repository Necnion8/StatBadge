package com.gmail.necnionch.myplugin.statbadge.bukkit.config;

import com.gmail.necnionch.myplugin.statbadge.bukkit.util.ItemCustomModelData;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
            badges.put(id, parseBadgeEntry(id, config.getConfigurationSection(id)));
        }
        return true;
    }

    public Map<String, BadgeEntry> badges() {
        return badges;
    }


    public BadgeEntry parseBadgeEntry(String id, ConfigurationSection config) {
        Material icon = Optional.ofNullable(config.getString("icon"))
                .flatMap(type -> Optional.ofNullable(Material.matchMaterial(type)))
                .orElse(Material.STONE);

        Object customModelData = ItemCustomModelData.serializeCustomModelData(config, "icon-custom-model-data");

        return new BadgeEntry(
                id,
                config.getString("stats"),
                config.getLong("stats-value"),
                config.getString("name"),
                config.getString("description"),
                config.getString("title"),
                icon,
                customModelData
        );
    }

}
