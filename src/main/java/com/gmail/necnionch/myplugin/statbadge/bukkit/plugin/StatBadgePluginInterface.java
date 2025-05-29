package com.gmail.necnionch.myplugin.statbadge.bukkit.plugin;

import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Logger;

public interface StatBadgePluginInterface {

    Logger getLogger();

    Plugin getPlugin();

    BukkitTask runTaskLaterAsynchronously(Runnable task, long delay);

    BukkitTask runTaskAsynchronously(Runnable task);

    <E extends Event> E callEvent(E event);

}
