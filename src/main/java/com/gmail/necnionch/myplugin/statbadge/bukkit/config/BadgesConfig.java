package com.gmail.necnionch.myplugin.statbadge.bukkit.config;

import com.gmail.necnionch.myplugin.statbadge.bukkit.util.ItemCustomModelData;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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
            BadgeEntry entry = parseBadgeEntry(id, config.getConfigurationSection(id));
            if (entry != null) {
                badges.put(id, entry);
            }
        }
        return true;
    }

    public Map<String, BadgeEntry> badges() {
        return badges;
    }


    public BadgeEntry parseBadgeEntry(String id, ConfigurationSection config) {
        BadgeEntry.Stats stats = parseBadgeEntryStats(config);

        if (stats == null)
            return null;

        BadgeEntry.Icon icon = parseBadgeEntryIcon(config);
        return new BadgeEntry(
                id,
                config.getString("name", id),
                config.getString("description"),
                config.getString("title"),
                icon, stats
        );
    }

    private BadgeEntry.Icon parseBadgeEntryIcon(ConfigurationSection parent) {
        if (!parent.contains("icon"))
            return new BadgeEntry.Icon(Material.STONE, null);
        if (parent.get("icon") instanceof String) {
            return new BadgeEntry.Icon(parseMaterial(parent.getString("icon")), null);
        }

        return new BadgeEntry.Icon(
                parseMaterial(parent.getString("icon" + ".type")),
                ItemCustomModelData.serializeCustomModelData(parent, "icon" + ".custom-model-data")
        );
    }

    private BadgeEntry.Stats parseBadgeEntryStats(ConfigurationSection parent) {
        ConfigurationSection config = parent.getConfigurationSection("stats");
        if (config == null)
            return null;

        return new BadgeEntry.Stats(
                config.getString("type", "action"),
                config.getLong("value", 0),
                config
        );
    }


    private Material parseMaterial(String type) {
        if (type == null)
            return Material.STONE;
        Material material = Material.matchMaterial(type);
        if (material == null) {
            log.warning("Unknown material: " + type);
            return Material.STONE;
        }
        return material;
    }

}
