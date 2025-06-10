package com.gmail.necnionch.myplugin.statbadge.bukkit.plugin;

import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.BadgeEntry;
import com.gmail.necnionch.myplugin.statbadge.bukkit.database.StatBadgeDatabase;
import com.gmail.necnionch.myplugin.statbadge.bukkit.event.*;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StatManager {

    private final Unsafe unsafe = new Unsafe();
    private final Object lock = new Object();
    private final StatBadgePluginInterface plugin;
    private final List<PlayerAction> actionCached = new ArrayList<>();
    private @Nullable StatBadgeDatabase database;
    private @Nullable BukkitTask commitTimerTask;
    //
    private final List<Badge<?>> playerBadges = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, PlayerStatsProvider> playerStatsProviders = new HashMap<>();
    private final Map<String, PlayerActionStatsProvider> playerActionStatsProviders = new HashMap<>();


    public StatManager(StatBadgePluginInterface plugin, @Nullable StatBadgeDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    private Logger getLogger() {
        return plugin.getLogger();
    }

    private StatBadgeDatabase getDatabaseOrThrow() {
        return Objects.requireNonNull(database, "StatBadge Database not initialized");
    }

    public @Nullable StatBadgeDatabase getDatabase() {
        return database;
    }

    public void setDatabase(@Nullable StatBadgeDatabase database) {
        this.database = database;
    }

    public boolean isInitialized() {
        return database != null && !database.isClosed();
    }

    private CompletableFuture<Void> commitAll() {
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
        StatBadgeDatabase db = getDatabaseOrThrow();
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
            db.addActions(actions);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error in commit actions", e);
        }
    }

    private void commitPlayerBadges() {
        try {
            getDatabaseOrThrow().addBadges(playerBadges);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error in commit actions", e);
        }
    }

    public Set<UUID> getPlayerBadgesPlayerIds() {
        return playerBadges.stream().map(Badge::getPlayer).collect(Collectors.toUnmodifiableSet());
    }

    public Stream<Badge<?>> streamPlayerBadges(UUID player) {
        return playerBadges.stream().filter(b -> b.getPlayer().equals(player));
    }

    public Stream<Badge<?>> streamCompletedPlayerBadges(UUID player) {
        return streamPlayerBadges(player).filter(Badge::isCompleted);
    }

    public List<Badge<?>> getPlayerBadges(UUID player) {
        return streamPlayerBadges(player).toList();
    }

    /**
     * プレイヤーの統計とバッジをロードします。<br>
     * 既に目標値に達している未処理バッジに対してバッジの達成処理が実行されます。
     */
    public CompletableFuture<Boolean> loadPlayer(Player player) {
        return loadPlayerBadges(player.getUniqueId()).handleAsync((badges, throwable) -> {
            if (throwable != null) {
                getLogger().log(Level.SEVERE, "Exception in load player: " + player.getUniqueId(), throwable);
                return false;
            } else {
                plugin.logDebug(() -> "Loaded " + badges.size() + " badges: " + player.getName());

                plugin.callEvent(new PlayerBadgeLoadEvent(player, badges));
                Instant now = Instant.now();
                badges.forEach(b -> processBadgeValueComplete(player, b, now));
                return true;
            }
        }, plugin::runTask);
    }

    /**
     * プレイヤーのバッジをコミットしてアンロードします
     */
    public CompletableFuture<Boolean> unloadPlayer(Player player) {
        StatBadgeDatabase db = getDatabaseOrThrow();
        List<Badge<?>> badges;
        synchronized (lock) {
            badges = playerBadges.stream().filter(b -> b.getPlayer().equals(player.getUniqueId())).toList();
            playerBadges.removeAll(badges);
        }

        plugin.callEvent(new PlayerBadgeUnloadEvent(player, badges));
        if (badges.isEmpty()) {
            return CompletableFuture.completedFuture(true);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                db.addBadges(badges);
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Exception in unload player: " + player.getUniqueId(), e);
                throw new RuntimeException(e);
            }
            return true;
        });
    }

    public Unsafe unsafe() {
        return unsafe;
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

    private Optional<PlayerStats> createPlayerStats(UUID player, BadgeEntry.Stats entry) {
        String statsType = completeAliasedType(entry.type());
        if (statsType.equals(PlayerActionStats.STATS_TYPE.toString()))
            return createPlayerActionStats(player, entry).map(s -> s);
        return getPlayerStatsProvider(statsType)
                .map(p -> p.create(player, entry.config(), entry.targetValue()));
    }

    private Optional<PlayerActionStats> createPlayerActionStats(UUID player, BadgeEntry.Stats entry) {
        String actionType = completeAliasedType(Objects.requireNonNull(entry.config().getString("action"), "Required 'action' type"));
        return getPlayerActionStatsProvider(actionType)
                .map(p -> p.create(player, entry.config(), entry.targetValue()));
    }

    private CompletableFuture<List<Badge<?>>> loadPlayerBadges(UUID player) {
        StatBadgeDatabase db = getDatabaseOrThrow();
        Map<String, BadgeEntry> configBadges = new HashMap<>(plugin.getBadgesConfig().badges());

        if (configBadges.isEmpty()) {
            playerBadges.clear();
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                commitCachedActions();
                return db.loadPlayerBadges(player, configBadges.keySet());
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Exception in load player badges", e);
                throw new RuntimeException(e);
            }
        }).thenApply(partials -> {
            List<Badge<?>> badges = new ArrayList<>();

            configBadges.forEach((id, badgeEntry) -> {
                BadgeEntry.Stats statsEntry = badgeEntry.stats();
                PlayerStats playerStats;
                try {
                    playerStats = createPlayerStats(player, statsEntry).orElse(null);
                } catch (Throwable e) {
                    getLogger().log(Level.SEVERE, "Exception in create player stats: " + statsEntry.type() + " (badge: " + badgeEntry.id() + ")", e);
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
                    badge = new Badge<>(badgeEntry.id(), badgeEntry, player, playerStats, startTime, completeTime);

                    if (playerStats instanceof PlayerActionStats) {
                        try {
                            //noinspection unchecked
                            db.loadActionStatsTo((Badge<PlayerActionStats>) badge);
                        } catch (SQLException e) {
                            getLogger().log(Level.SEVERE, "Exception in load player actions", e);
                            throw new RuntimeException(e);
                        }
                    }

                } else {
                    badge = new Badge<>(badgeEntry.id(), badgeEntry, player, playerStats, Instant.now(), null);
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
        plugin.callEvent(new PlayerActionEvent(player, action));

        synchronized (lock) {
            actionCached.add(action);
            // queue timer
            if (commitTimerTask == null || commitTimerTask.isCancelled()) {
                commitTimerTask = plugin.runTaskLaterAsynchronously(this::commitCachedActions, 60 * 20);
            }
        }

        for (Badge<?> badge : playerBadges) {
            if (isCompletableBadge(badge, action.getTime()) && matchBadgeAction(badge, action)) {
                changeBadgeValue(player, badge, badge.getStats().getValue() + action.getValue(), action.getTime());
            }
        }
    }

    public void changeStats(Player player, StatsType type, @Nullable Instant statsTime, long value) {
        if (statsTime == null)
            statsTime = Instant.now();

        plugin.callEvent(new PlayerStatsEvent(player, type, statsTime, value));

        for (Badge<?> badge : playerBadges) {
            if (isCompletableBadge(badge, statsTime) && matchBadgeStats(badge, player.getUniqueId(), type)) {
                changeBadgeValue(player, badge, value, statsTime);
            }
        }
    }

    public boolean matchBadgeAction(Badge<?> badge, PlayerAction action) {
        if (!badge.getPlayer().equals(action.getPlayer()))
            return false;

        if (!(badge.getStats() instanceof PlayerActionStats stats))
            return false;

        if (!stats.getSourceActionType().equals(action.getType()))
            return false;

        return stats.getKeyCondition1().test(action.getKey1()) && stats.getKeyCondition2().test(action.getKey2()) && stats.getKeyCondition3().test(action.getKey3());
    }

    public boolean matchBadgeStats(Badge<?> badge, UUID player, StatsType statsType) {
        return badge.getStats().getType().equals(statsType) && badge.getPlayer().equals(player);
    }

    public boolean isCompletableBadge(Badge<?> badge, Instant time) {
        return !badge.isCompleted() && (badge.getStartTime() == null || !badge.getStartTime().isAfter(time));
    }

    /**
     * Badgeの値を変更します。目標値に達したら達成イベントを呼びます
     */
    private void changeBadgeValue(Player player, Badge<?> badge, long value, Instant time) {
        long oldValue = badge.getStats().getValue();
        badge.getStats().setValue(value);
        plugin.callEvent(new PlayerBadgeValueChangeEvent(player, badge, value, oldValue));
        processBadgeValueComplete(player, badge, time);
    }

    private void processBadgeValueComplete(Player player, Badge<?> badge, Instant time) {
        if (badge.isCompleted() || !badge.getStats().compareTargetValue(badge))
            return;

        badge.setCompleteTime(time);
        plugin.callEvent(new PlayerBadgeCompleteEvent(player, badge));
    }

    public Optional<Badge<?>> getPlayerBadge(UUID player, String badgeId) {
        return streamPlayerBadges(player)
                .filter(b -> b.getId().equals(badgeId))
                .findFirst();
    }

    public @Nullable Badge<?> applyPlayerBadge(UUID player, String badgeId, Consumer<Badge<?>> action) {
        Badge<?> badge = getPlayerBadge(player, badgeId).orElse(null);
        if (badge != null) {
            action.accept(badge);
        }
        return badge;
    }

    public @Nullable Badge<?> grantBadge(Player player, String badgeId, @Nullable Instant completeTime) {
        return applyPlayerBadge(player.getUniqueId(), badgeId, badge -> {
            if (!badge.isCompleted()) {
                badge.setCompleteTime(completeTime);
                plugin.callEvent(new PlayerBadgeCompleteEvent(player, badge));
            }
        });
    }

    public @Nullable Badge<?> grantBadge(Player player, String badgeId) {
        return grantBadge(player, badgeId, Instant.now());
    }

    public @Nullable Badge<?> revokeBadge(Player player, String badgeId, @Nullable Instant startTime) {
        return applyPlayerBadge(player.getUniqueId(), badgeId,badge -> {
            if (badge.isCompleted()) {
                badge.setStartTime(startTime);
                badge.setCompleteTime(null);
                badge.getStats().resetValue();
                plugin.callEvent(new PlayerBadgeRemoveEvent(player, badge));
            }
        });
    }

    public @Nullable Badge<?> revokeBadge(Player player, String badgeId) {
        return revokeBadge(player, badgeId, Instant.now());
    }

    // utility

    public String completeAliasedType(String type) {
        return type.contains(":") ? type : plugin.getPlugin().getName().toLowerCase(Locale.ROOT) + ":" + type;
    }


    public class Unsafe {
        private Unsafe() {}

        /**
         * アクションをデータベースにコミットします<br>
         * イベントや値を処理せず、同期的にデータベースへコミットします。
         */
        public void addActionsToDatabase(Collection<PlayerAction> actions) {
            synchronized (lock) {
                if (commitTimerTask != null) {
                    commitTimerTask.cancel();
                    commitTimerTask = null;
                }
            }
            actionCached.addAll(actions);
            commitCachedActions();
        }
    }

}
