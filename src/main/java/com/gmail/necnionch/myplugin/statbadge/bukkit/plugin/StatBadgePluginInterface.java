package com.gmail.necnionch.myplugin.statbadge.bukkit.plugin;

import com.gmail.necnionch.myplugin.statbadge.bukkit.config.BadgesConfig;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.StatBadgeLang;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.StatsConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;

public interface StatBadgePluginInterface {

    Logger getLogger();

    Plugin getPlugin();

    BukkitTask runTaskLaterAsynchronously(Runnable task, long delay);

    BukkitTask runTask(Runnable task);

    <E extends Event> E callEvent(E event);

    @Nullable Player getPlayer(UUID playerId);

    Collection<? extends Player> getOnlinePlayers();

    StatManager getStats();

    PlayerOnlineTimeManager getPlayerOnlineTimeManager();

    StatsConfig getStatsConfig();

    BadgesConfig getBadgesConfig();

    StatBadgeLang getLangConfig();


    void logDebug(Supplier<String> message);

    void logDebug(String message);

}
