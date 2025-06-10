package com.gmail.necnionch.myplugin.statbadge.bukkit.plugin;

import com.gmail.necnionch.myplugin.statbadge.bukkit.event.PlayerBadgeLoadEvent;
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
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerOnlineTimeManager implements Listener {

    private final StatBadgePluginInterface plugin;
    private final Map<Player, PlayerTimer> players = new HashMap<>();
    private final ActionType actionType;

    public PlayerOnlineTimeManager(StatBadgePluginInterface plugin, ActionType actionType) {
        this.plugin = plugin;
        this.actionType = actionType;
    }

    public void start() {
        plugin.getPlugin().getServer().getPluginManager().registerEvents(this, plugin.getPlugin());
    }

    /**
     * 管理している処理を停止させて、保持するデータをコミットして解放します。<br>
     * イベントを処理するために、プラグインが有効である必要があります。
     * @see PlayerOnlineTimeManager#shutdown()
     */
    public void stop() {
        HandlerList.unregisterAll(this);
        new ArrayList<>(players.keySet()).forEach(this::removeAndCommitPlayerTimerAction);
        players.clear();
    }

    /**
     * 管理している処理を停止させて、保持するデータをコミットして解放します。
     * @see PlayerOnlineTimeManager#stop()
     */
    public void shutdown() {
        HandlerList.unregisterAll(this);
        try {
            commitPlayerTimerAll();
        } finally {
            players.clear();
        }
    }

    public void loadOnlinePlayers(@Nullable AFKProvider afkProvider) {
        plugin.getPlugin().getServer().getOnlinePlayers().forEach(p -> {
            startPlayerTimer(p, afkProvider != null && afkProvider.isAFK(p));
        });
    }

    private StatManager getStats() {
        return plugin.getStats();
    }

    private void startPlayerTimer(Player player, boolean isAFK) {
        removeAndCommitPlayerTimerAction(player);
        PlayerTimer timer = new PlayerTimer(isAFK, Instant.now());
        players.put(player, timer);

        startPreTimer(player, timer);
    }

    private void startPreTimer(Player player, PlayerTimer timer) {
        plugin.getStatManager().streamPlayerBadges(player.getUniqueId())
                .filter(b -> b.getPlayer().equals(player.getUniqueId()))
                .filter(b -> !b.isCompleted() && b.getStats() instanceof PlayerOnlineActionStats)
                .mapToLong(b -> b.getStats().getTargetValue() - b.getStats().getValue())
                .min()
                .ifPresent(minTime -> {
                    if (timer.timer != null)
                        timer.timer.cancel();

                    timer.timer = plugin.runTaskLater(() -> {
                        timer.timer = null;
                        PlayerTimer cPlayer = players.get(player);
                        if (cPlayer != null) {
                            startPlayerTimer(player, cPlayer.afk);
                        }
                    }, minTime / 1000 * 20 + 20);
                });
    }

    private void removeAndCommitPlayerTimerAction(Player player) {
        PlayerTimer timer;
        if ((timer = players.remove(player)) == null)
            return;

        if (timer.timer != null)
            timer.timer.cancel();

        Instant now = Instant.now();
        long duration = now.toEpochMilli() - timer.startTime.toEpochMilli();
        String afk = timer.afk ? PlayerOnlineActionStats.KEY_AFK_ON : PlayerOnlineActionStats.KEY_AFK_OFF;
        getStats().addAction(player, new PlayerAction(player.getUniqueId(), actionType, now, afk, null, null, duration));
    }

    private void commitPlayerTimerAll() {
        Instant now = Instant.now();
        List<PlayerAction> actions = players.entrySet().stream()
                .map(e -> {
                    PlayerTimer timer = e.getValue();
                    long duration = now.toEpochMilli() - timer.startTime.toEpochMilli();
                    String afk = timer.afk ? PlayerOnlineActionStats.KEY_AFK_ON : PlayerOnlineActionStats.KEY_AFK_OFF;
                    return new PlayerAction(e.getKey().getUniqueId(), actionType, now, afk, null, null, duration);
                })
                .toList();

        if (!actions.isEmpty()) {
            getStats().unsafe().addActionsToDatabase(actions);
        }
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
        removeAndCommitPlayerTimerAction(event.getPlayer());
        clearPlayerTimer(event.getPlayer());
    }

    @EventHandler
    public void onLoadBadge(PlayerBadgeLoadEvent event) {
        PlayerTimer timer;
        if ((timer = players.get(event.getPlayer())) == null)
            return;
        startPreTimer(event.getPlayer(), timer);
    }


    private static class PlayerTimer {
        private final boolean afk;
        private final Instant startTime;
        private BukkitTask timer;

        public PlayerTimer(boolean afk, Instant startTime) {
            this.afk = afk;
            this.startTime = startTime;
        }
    }

}
