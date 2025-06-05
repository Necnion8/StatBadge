package com.gmail.necnionch.myplugin.statbadge.bukkit.config;

import org.bukkit.plugin.Plugin;

public class StatBadgeConfig extends BukkitConfiguration {

    private boolean debug;

    public StatBadgeConfig(Plugin plugin) {
        super(plugin);
    }

    @Override
    protected boolean onLoaded() {
        debug = config.getBoolean("debug", false);
        return true;
    }

    public boolean isDebugEnable() {
        return debug;
    }

}
