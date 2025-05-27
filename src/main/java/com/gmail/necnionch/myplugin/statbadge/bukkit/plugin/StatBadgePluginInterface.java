package com.gmail.necnionch.myplugin.statbadge.bukkit.plugin;

import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Logger;

public interface StatBadgePluginInterface {

    Logger getLogger();

    BukkitTask runTaskLaterAsynchronously(Runnable task, long delay);

    <E extends Event> E callEvent(E event);

}
