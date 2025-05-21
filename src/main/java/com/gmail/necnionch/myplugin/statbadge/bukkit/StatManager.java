package com.gmail.necnionch.myplugin.statbadge.bukkit;

import com.gmail.necnionch.myplugin.statbadge.bukkit.action.ActionType;
import com.gmail.necnionch.myplugin.statbadge.bukkit.action.PlayerAction;
import com.gmail.necnionch.myplugin.statbadge.bukkit.action.PlayerStats;
import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import com.gmail.necnionch.myplugin.statbadge.bukkit.database.StatBadgeDatabase;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class StatManager {

    private final Object lock = new Object();
    private final Plugin plugin;
    private final StatBadgeDatabase database;
    private @Nullable BukkitTask commitTimerTask;
    private final List<PlayerAction> actionCached = new ArrayList<>();
    //
    private final List<Badge> playerBadges = new ArrayList<>();

    public StatManager(Plugin plugin, StatBadgeDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    private void clearCommitTimer() {
        if (commitTimerTask != null)
            commitTimerTask.cancel();
        commitTimerTask = null;
    }

    public void commitAll() {
        commitCachedActions();
    }

    private void commitCachedActions() {
        List<PlayerAction> actions;
        synchronized (lock) {
            clearCommitTimer();
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


    public ActionType createActionType(Plugin plugin, String type) {
        return new ActionType(plugin.getName(), type);
    }


    public void addAction(PlayerAction action) {
        synchronized (lock) {
            actionCached.add(action);
            // queue timer
            if (commitTimerTask == null || commitTimerTask.isCancelled()) {
                commitTimerTask = plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, this::commitCachedActions, 60 * 20);
            }
        }

        for (Badge badge : playerBadges) {
            if (!badge.isCompleted() && badge.isMatchAction(action)) {
                changeBadgeValue(badge, badge.getActionCurrentValue() + action.value(), action.time());
            }
        }
    }

    public void changedStats(PlayerStats stats) {
        for (Badge badge : playerBadges) {
            if (!badge.isCompleted() && badge.isMatchStats(stats)) {
                changeBadgeValue(badge, stats.value(), stats.time());
            }
        }
    }

    /**
     * Badgeの値を変更します。目標値に達したら達成イベントを呼びます
     */
    private void changeBadgeValue(Badge badge, long value, Instant time) {
        badge.setActionCurrentValue(value);
        // TODO: call updated badge value event
        if (value < badge.getActionTargetValue())
            return;

        badge.setCompleteTime(time);
        // TODO: call complete badge event
    }


}
