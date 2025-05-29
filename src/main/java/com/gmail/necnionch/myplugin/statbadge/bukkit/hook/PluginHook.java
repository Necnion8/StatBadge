package com.gmail.necnionch.myplugin.statbadge.bukkit.hook;

import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class PluginHook {

    protected final String pluginName;
    protected final Logger log;
    private boolean hooked;

    public PluginHook(String pluginName, Logger logger) {
        this.pluginName = pluginName;
        this.log = logger;
    }

    public String getPluginName() {
        return pluginName;
    }

    protected abstract boolean onHook(Plugin plugin);

    protected abstract boolean onUnhook();

    public final boolean hook(Plugin plugin) {
        hooked = false;
        try {
            hooked = onHook(plugin);
            return hooked;
        } catch (Throwable e) {
            log.log(Level.SEVERE, "Exception in hook plugin to " + getPluginName(), e);
            return false;
        }
    }

    public final void unhook() {
        hooked = false;
        try {
            onUnhook();
        } catch (Throwable e) {
            log.log(Level.WARNING, "Exception in unhook plugin to " + getPluginName(), e);
        }
    }

}
