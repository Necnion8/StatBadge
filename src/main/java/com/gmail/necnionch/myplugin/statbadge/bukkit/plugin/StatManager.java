package com.gmail.necnionch.myplugin.statbadge.bukkit.plugin;

import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.BadgeEntry;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.StatBadgeConfig;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.StatsEntry;
import com.gmail.necnionch.myplugin.statbadge.bukkit.database.StatBadgeDatabase;
import com.gmail.necnionch.myplugin.statbadge.bukkit.event.PlayerBadgeCompleteEvent;
import com.gmail.necnionch.myplugin.statbadge.bukkit.event.PlayerBadgeValueChangeEvent;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class StatManager {

    private final Object lock = new Object();
    private final StatBadgePluginInterface plugin;
    private final StatBadgeConfig config;
    private final StatBadgeDatabase database;
    private @Nullable BukkitTask commitTimerTask;
    private final List<PlayerAction> actionCached = new ArrayList<>();
    //
    private final List<Badge<?>> playerBadges = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, PlayerStatsProvider> playerStatsProviders = new HashMap<>();
    private final Map<String, PlayerActionStatsProvider> playerActionStatsProviders = new HashMap<>();


    public StatManager(StatBadgePluginInterface plugin, StatBadgeConfig config, StatBadgeDatabase database) {
        this.plugin = plugin;
        this.config = config;
        this.database = database;
    }

    private Logger getLogger() {
        return plugin.getLogger();
    }


    public CompletableFuture<Void> commitAll() {
        return CompletableFuture.supplyAsync(() -> {
            commitCachedActions();
            commitPlayerBadges();
            return null;
        });
    }

    public CompletableFuture<Void> commitAndUnloadAll() {
        return commitAll().handle((u, e) -> {
            actionCached.clear();
            playerBadges.clear();
            return null;
        });
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

    private void commitPlayerBadges() {
        try {
            database.addBadges(playerBadges);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error in commit actions", e);
        }
    }

    public Set<UUID> getPlayerBadgesPlayerIds() {
        return playerBadges.stream().map(Badge::getPlayer).collect(Collectors.toUnmodifiableSet());
    }

    public List<Badge<?>> getPlayerBadges(UUID player) {
        return playerBadges.stream()
                .filter(b -> b.getPlayer().equals(player))
                .toList();
    }

    public CompletableFuture<Boolean> loadPlayer(UUID player) {
        return loadPlayerBadges(player).handle((badges, throwable) -> {
            if (throwable != null) {
                getLogger().log(Level.SEVERE, "Exception in load player: " + player, throwable);
                return false;
            } else {
                getLogger().info("Loaded " + player + "'s badge " + badges.size());
                return true;
            }
        });
    }

    public CompletableFuture<Boolean> commitAndUnloadPlayer(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Badge<?>> badges;
                synchronized (lock) {
                    badges = playerBadges.stream().filter(b -> b.getPlayer().equals(player)).toList();
                    if (badges.isEmpty())
                        return true;
                    playerBadges.removeAll(badges);
                }
                database.addBadges(badges);
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Exception in unload player: " + player, e);
                throw new RuntimeException(e);
            }
            return true;
        });
    }

    // providers

    public Optional<PlayerStatsProvider> getPlayerStatsProvider(StatsType type) {
        return getPlayerStatsProvider(type.toString());
    }

    public Optional<PlayerActionStatsProvider> getPlayerActionStatsProvider(ActionType type) {
        return getPlayerActionStatsProvider(type.toString());
    }

    private Optional<PlayerStatsProvider> getPlayerStatsProvider(String statsType) {
        return Optional.ofNullable(playerStatsProviders.get(statsType));
    }

    private Optional<PlayerActionStatsProvider> getPlayerActionStatsProvider(String actionType) {
        return Optional.ofNullable(playerActionStatsProviders.get(actionType));
    }

    public void addPlayerStatsProvider(StatsType type, PlayerStatsProvider provider) {
        if (playerStatsProviders.containsKey(type.toString()))
            throw new IllegalArgumentException("Already registered: " + type);
        playerStatsProviders.put(type.toString(), provider);
    }

    public @Nullable PlayerStatsProvider removePlayerStatsProvider(StatsType type) {
        return playerStatsProviders.remove(type.toString());
    }

    public void addPlayerActionStatsProvider(ActionType type, PlayerActionStatsProvider provider) {
        if (playerActionStatsProviders.containsKey(type.toString()))
            throw new IllegalArgumentException("Already registered: " + type);
        playerActionStatsProviders.put(type.toString(), provider);
    }

    public @Nullable PlayerActionStatsProvider removePlayerActionStatsProvider(ActionType type) {
        return playerActionStatsProviders.remove(type.toString());
    }

    public void removeProviders(Plugin plugin) {
        playerStatsProviders.values().removeIf(p -> p.getPlugin().equals(plugin));
        playerActionStatsProviders.values().removeIf(p -> p.getPlugin().equals(plugin));
    }

    //

    private Optional<PlayerStats> createPlayerStats(UUID player, StatsEntry entry) {
        String statsType = completeAliasedType(entry.type());
        if (statsType.equals(PlayerActionStats.STATS_TYPE.toString()))
            return createPlayerActionStats(player, entry).map(s -> s);
        return getPlayerStatsProvider(statsType)
                .map(p -> p.create(player, entry.id(), entry.config()));
    }

    private Optional<PlayerActionStats> createPlayerActionStats(UUID player, StatsEntry entry) {
        String actionType = completeAliasedType(Objects.requireNonNull(entry.config().getString("action"), "Required 'action' type"));
        return getPlayerActionStatsProvider(actionType)
                .map(p -> p.create(player, entry.id(), entry.config()));
    }

    private CompletableFuture<List<Badge<?>>> loadPlayerBadges(UUID player) {
        if (config.badges().isEmpty()) {
            playerBadges.clear();
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        Map<String, BadgeEntry> configBadges = new HashMap<>(config.badges());

        return CompletableFuture.supplyAsync(() -> {
            try {
                commitCachedActions();
                return database.loadPlayerBadges(player, configBadges.keySet());
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Exception in load player badges", e);
                throw new RuntimeException(e);
            }
        }).thenApply(partials -> {
            List<Badge<?>> badges = new ArrayList<>();

            configBadges.forEach((id, badgeEntry) -> {
                StatsEntry statsEntry = config.stats().get(badgeEntry.statsType());
                if (statsEntry == null) {
                    getLogger().warning("Unable to init badge: " + badgeEntry.id() + ": Unknown stats: " + badgeEntry.statsType());
                    return;
                }

                PlayerStats playerStats;
                try {
                    playerStats = createPlayerStats(player, statsEntry).orElse(null);
                } catch (Throwable e) {
                    e.printStackTrace();
                    return;
                }

                if (playerStats == null) {
                    if (completeAliasedType(statsEntry.type()).equals(PlayerActionStats.STATS_TYPE.toString())) {
                        getLogger().warning("Unable to init action stats: " + statsEntry.config().getString("action") + ": No provider");
                    } else {
                        getLogger().warning("Unable to init stats: " + statsEntry.type() + ": No provider");
                    }
                    return;
                }

                Badge.Partial partial = partials.get(id);
                Badge<?> badge;
                if (partial != null) {
                    Instant startTime = partial.startTime().map(Instant::ofEpochMilli).orElse(null);
                    Instant completeTime = partial.completeTime().map(Instant::ofEpochMilli).orElse(null);
                    badge = new Badge<>(badgeEntry.id(), player, playerStats, startTime, completeTime, badgeEntry.statsValue());

                    if (playerStats instanceof PlayerActionStats) {
                        try {
                            //noinspection unchecked
                            database.loadActionStatsTo((Badge<PlayerActionStats>) badge);
                        } catch (SQLException e) {
                            getLogger().log(Level.SEVERE, "Exception in load player actions", e);
                            throw new RuntimeException(e);
                        }
                    }

                } else {
                    badge = new Badge<>(badgeEntry.id(), player, playerStats, Instant.now(), null, badgeEntry.statsValue());
                }

                badges.add(badge);
            });

            this.playerBadges.removeIf(b -> b.getPlayer().equals(player));
            this.playerBadges.addAll(badges);
            return badges;
        });
    }

    // main use

    public void addAction(Player player, PlayerAction action) {
        synchronized (lock) {
            actionCached.add(action);
            // queue timer
            if (commitTimerTask == null || commitTimerTask.isCancelled()) {
                commitTimerTask = plugin.runTaskLaterAsynchronously(this::commitCachedActions, 60 * 20);
            }
        }

        for (Badge<?> badge : playerBadges) {
            if (!badge.isCompleted() && validBadgeAction(badge, action)) {
                changeBadgeValue(player, badge, badge.getStats().getValue() + action.getValue(), action.getTime());
            }
        }
    }

    public void changeStats(Player player, PlayerStats stats, @Nullable Instant statsTime) {
        if (statsTime == null)
            statsTime = Instant.now();
        for (Badge<?> badge : playerBadges) {
            if (!badge.isCompleted() && validBadgeStats(badge, stats, statsTime)) {
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

        Instant startTime = badge.getStartTime();
        if (startTime != null && startTime.isAfter(action.getTime()))
            return false;

        if (!(badge.getStats() instanceof PlayerActionStats stats))
            return false;

        if (!stats.getSourceActionType().equals(action.getType()))
            return false;

        return stats.getKeyCondition1().test(action.getKey1()) && stats.getKeyCondition2().test(action.getKey2()) && stats.getKeyCondition3().test(action.getKey3());
    }

    public boolean validBadgeStats(Badge<?> badge, PlayerStats stats, Instant statsTime) {
        if (!badge.getStats().equals(stats))
            return false;

        if (!badge.getPlayer().equals(stats.getPlayer()))
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

    // utility

    public String completeAliasedType(String type) {
        return type.contains(":") ? type : plugin.getPlugin().getName().toLowerCase(Locale.ROOT) + ":" + type;
    }

}
