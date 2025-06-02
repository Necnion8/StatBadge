package com.gmail.necnionch.myplugin.statbadge.bukkit.stats;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public class StatsType {

    private final NamespacedKey type;

    public StatsType(NamespacedKey type) {
        this.type = type;
    }

    public StatsType(Plugin plugin, String key) {
        this(new NamespacedKey(plugin, key));
    }

    public String getNamespace() {
        return type.getNamespace();
    }

    public String getKey() {
        return type.getKey();
    }

    @Override
    public String toString() {
        return type.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) || (obj instanceof StatsType && type.equals(((StatsType) obj).type));
    }

}
