package com.gmail.necnionch.myplugin.statbadge.bukkit.plugin;

import com.gmail.necnionch.myplugin.statbadge.bukkit.hook.AFKProvider;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.ActionType;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerAction;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.impl.PlayerOnlineActionStats;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class PlayerOnlineTimeManager implements Listener {

    private final StatBadgePluginInterface plugin;
    private final Logger log;
    private final Map<Player, PlayerTimer> players = new HashMap<>();
    private final ActionType actionType;

    public PlayerOnlineTimeManager(StatBadgePluginInterface plugin, ActionType actionType) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.actionType = actionType;
    }

    public void start() {
        plugin.getPlugin().getServer().getPluginManager().registerEvents(this, plugin.getPlugin());
    }

    public void stop() {
        HandlerList.unregisterAll(this);
    }

    public void stopAndCommitAll() {
        stop();
        new ArrayList<>(players.keySet()).forEach(this::commitPlayerTimer);
        players.clear();
    }

    public void loadOnlinePlayers(@Nullable AFKProvider afkProvider) {
        plugin.getPlugin().getServer().getOnlinePlayers().forEach(p -> {
            startPlayerTimer(p, afkProvider != null && afkProvider.isAFK(p));
        });
    }

    private StatManager getStats() {
        return ((StatBadgePlugin) plugin.getPlugin()).getStats();
    }

    private void startPlayerTimer(Player player, boolean isAFK) {
        commitPlayerTimer(player);
        if (players.containsKey(player)) {
            players.get(player).afk = isAFK;
            players.get(player).startTime = Instant.now();
        } else {
            players.put(player, new PlayerTimer(isAFK, Instant.now()));
        }
    }

    private void commitPlayerTimer(Player player) {
        PlayerTimer timer;
        if ((timer = players.remove(player)) == null)
            return;

        Instant now = Instant.now();
        long duration = now.toEpochMilli() - timer.startTime.toEpochMilli();
        String afk = timer.afk ? PlayerOnlineActionStats.KEY_AFK_ON : PlayerOnlineActionStats.KEY_AFK_OFF;
        getStats().addAction(player, new PlayerAction(player.getUniqueId(), actionType, now, afk, null, null, duration));
    }

    private void clearPlayerTimer(Player player) {
        players.remove(player);
    }

    //

    public void setAFK(Player player) {
        PlayerTimer timer;
        if ((timer = players.get(player)) == null || !timer.afk) {
            startPlayerTimer(player, true);
        }
    }

    public void unsetAFK(Player player) {
        PlayerTimer timer;
        if ((timer = players.get(player)) == null || timer.afk) {
            startPlayerTimer(player, false);
        }
    }

    //

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        startPlayerTimer(event.getPlayer(), false);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        commitPlayerTimer(event.getPlayer());
        clearPlayerTimer(event.getPlayer());
    }


    private static class PlayerTimer {
        private boolean afk;
        private Instant startTime;

        public PlayerTimer(boolean afk, Instant startTime) {
            this.afk = afk;
            this.startTime = startTime;
        }
    }

}
