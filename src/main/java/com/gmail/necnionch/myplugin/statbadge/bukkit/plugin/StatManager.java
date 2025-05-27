package com.gmail.necnionch.myplugin.statbadge.bukkit.plugin;

import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import com.gmail.necnionch.myplugin.statbadge.bukkit.database.StatBadgeDatabase;
import com.gmail.necnionch.myplugin.statbadge.bukkit.event.PlayerBadgeCompleteEvent;
import com.gmail.necnionch.myplugin.statbadge.bukkit.event.PlayerBadgeValueChangeEvent;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerAction;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerActionStats;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerStats;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class StatManager {

    private final Object lock = new Object();
    private final StatBadgePluginInterface plugin;
    private final StatBadgeDatabase database;
    private @Nullable BukkitTask commitTimerTask;
    private final List<PlayerAction> actionCached = new ArrayList<>();
    //
    private final List<Badge<?>> playerBadges = new ArrayList<>();


    public StatManager(StatBadgePluginInterface plugin, StatBadgeDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    public void commitAll() {
        commitCachedActions();
    }

    private void commitCachedActions() {
        List<PlayerAction> actions;
        synchronized (lock) {
            // clear timer
            if (commitTimerTask != null)
                commitTimerTask.cancel();
            commitTimerTask = null;

            if (actionCached.isEmpty())
                return;
            actions = new ArrayList<>(actionCached);
            actionCached.clear();
        }

        try {
            database.addActions(actions);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error in commit actions", e);
        }
    }


    public List<Badge<?>> getPlayerBadges(UUID player) {
        return playerBadges.stream()
                .filter(b -> b.getPlayer().equals(player))
                .toList();
    }

    public CompletableFuture<List<Badge<?>>> loadPlayerBadges(UUID player) {
        CompletableFuture<List<Badge<?>>> future = new CompletableFuture<>();
        // TODO: configからbadgeを定義して、dbからプレイヤーのbadge情報を読んでロードして
        return future;
    }


    public void addAction(Player player, PlayerAction action) {
        synchronized (lock) {
            actionCached.add(action);
            // queue timer
            if (commitTimerTask == null || commitTimerTask.isCancelled()) {
                commitTimerTask = plugin.runTaskLaterAsynchronously(this::commitCachedActions, 60 * 20);
            }
        }

        for (Badge<?> badge : playerBadges) {
            if (validBadgeAction(badge, action)) {
                changeBadgeValue(player, badge, badge.getStats().getValue() + action.getValue(), action.getTime());
            }
        }
    }

    public void changeStats(Player player, PlayerStats stats, @Nullable Instant statsTime) {
        if (statsTime == null)
            statsTime = Instant.now();
        for (Badge<?> badge : playerBadges) {
            if (validBadgeStats(badge, stats, statsTime)) {
                changeBadgeValue(player, badge, stats.getValue(), statsTime);
            }
        }
    }

    public void changeStats(Player player, PlayerStats stats) {
        changeStats(player, stats, null);
    }

    public boolean validBadgeAction(Badge<?> badge, PlayerAction action) {
        if (!badge.getPlayer().equals(action.getPlayer()))
            return false;

        if (!badge.isCompleted())
            return false;

        Instant startTime = badge.getStartTime();
        if (startTime != null && startTime.isAfter(action.getTime()))
            return false;

        return badge.getStats() instanceof PlayerActionStats stats && stats.matchAction(action);
    }

    public boolean validBadgeStats(Badge<?> badge, PlayerStats stats, Instant statsTime) {
        if (!badge.getStats().equals(stats))
            return false;

        if (!badge.getPlayer().equals(stats.getPlayer()))
            return false;

        if (!badge.isCompleted())
            return false;

        Instant startTime = badge.getStartTime();
        return startTime == null || !startTime.isAfter(statsTime);
    }

    /**
     * Badgeの値を変更します。目標値に達したら達成イベントを呼びます
     */
    private void changeBadgeValue(Player player, Badge<?> badge, long value, Instant time) {
        long oldValue = badge.getStats().getValue();
        badge.getStats().setValue(value);
        plugin.callEvent(new PlayerBadgeValueChangeEvent(player, badge, value, oldValue));
        if (badge.isCompleted() || value < badge.getActionTargetValue())
            return;

        badge.setCompleteTime(time);
        plugin.callEvent(new PlayerBadgeCompleteEvent(player, badge));
    }


}
