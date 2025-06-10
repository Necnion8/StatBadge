package com.gmail.necnionch.myplugin.statbadge.bukkit.plugin;

import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import com.gmail.necnionch.myplugin.statbadge.bukkit.command.BadgesCommand;
import com.gmail.necnionch.myplugin.statbadge.bukkit.command.BukkitCommand;
import com.gmail.necnionch.myplugin.statbadge.bukkit.command.StatBadgeCommand;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.BadgesConfig;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.Lang;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.StatBadgeConfig;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.StatBadgeLang;
import com.gmail.necnionch.myplugin.statbadge.bukkit.database.MySQLDatabase;
import com.gmail.necnionch.myplugin.statbadge.bukkit.database.SQLiteDatabase;
import com.gmail.necnionch.myplugin.statbadge.bukkit.database.StatBadgeDatabase;
import com.gmail.necnionch.myplugin.statbadge.bukkit.event.PlayerActionEvent;
import com.gmail.necnionch.myplugin.statbadge.bukkit.event.PlayerBadgeCompleteEvent;
import com.gmail.necnionch.myplugin.statbadge.bukkit.event.PlayerBadgeValueChangeEvent;
import com.gmail.necnionch.myplugin.statbadge.bukkit.event.PlayerStatsEvent;
import com.gmail.necnionch.myplugin.statbadge.bukkit.hook.*;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.ActionType;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerActionStats;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerActionStatsProvider;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerStats;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.impl.PlayerMobActionStats;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.impl.PlayerMobEventListener;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.impl.PlayerOnlineActionStats;
import com.gmail.necnionch.myplugin.statbadge.bukkit.util.ItemCustomModelData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StatBadgePlugin extends JavaPlugin implements StatBadgePluginInterface, Listener {

    public static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.legacy('&');
    private final BukkitCommand.Compat commands = BukkitCommand.compat(this);
    private final StatManager statManager = new StatManager(this, null);
    private final ActionType actionEntityKilled = new ActionType(this, "entity_killed");
    private final ActionType actionEntityDeath = new ActionType(this, "entity_death");
    private final ActionType actionOnlineTimeSource = new ActionType(this, "online_time");
    private final ActionType actionOnlineTime = new ActionType(this, "online_time");
    private final ActionType actionPlayTime = new ActionType(this, "play_time");
    private final StatBadgeConfig config = new StatBadgeConfig(this);
    private final BadgesConfig badgesConfig = new BadgesConfig(this);
    private final StatBadgeLang langConfig = new StatBadgeLang(this);
    private final PlayerOnlineTimeManager onlineTimeManager = new PlayerOnlineTimeManager(this, actionOnlineTimeSource);
    //
    private final Map<String, Supplier<PluginHook>> pluginHooks = new HashMap<>();
    private final List<PluginHook> hookedPlugins = new ArrayList<>();

    @Override
    public void onLoad() {
        ItemCustomModelData.init();
        commands.init();
    }

    @Override
    public void onEnable() {
        config.load();
        badgesConfig.load();
        langConfig.load();

        boolean result = false;
        try {
            result = initStatManager();
        } catch (Throwable e) {
            getLogger().log(Level.SEVERE, "Failed to initialize plugin", e);
        }

        getServer().getPluginManager().registerEvents(this, this);

        if (result) {
            setupDefaultStats();
        }
        setupHookPlugins();
        hookPlugins();
        onlineTimeManager.start();
        onlineTimeManager.loadOnlinePlayers((AFKProvider) hookedPlugins.stream().filter(hook -> hook instanceof AFKProvider).findFirst().orElse(null));
        if (result) {
            getServer().getOnlinePlayers().forEach(this::loadPlayer);
        }

        commands.register(new StatBadgeCommand(this));
        commands.register(new BadgesCommand(this));
    }

    @Override
    public void onDisable() {
        unhookPlugins();
        try {
            onlineTimeManager.shutdown();
        } catch (Throwable e) {
            getLogger().log(Level.WARNING, "Exception in shutdown online time manager", e);
        }

        try {
            closeStatManager();
        } catch (Throwable e) {
            getLogger().log(Level.SEVERE, "Failed to cleanup plugin", e);
        }

        commands.close();
        ItemCustomModelData.clear();
    }

    @Override
    public boolean reloadStatBadge() {
        getLogger().info("Reloading StatBadge config & database");
        config.load();
        badgesConfig.load();
        langConfig.load();
        boolean result = initStatManager();
        if (result) {
            getServer().getOnlinePlayers().forEach(this::loadPlayer);
            getLogger().info("Reload OK!");
        } else {
            getLogger().warning("Reload Failed!");
        }
        return result;
    }

    private boolean initStatManager() {
        String dbType = config.getDatabaseType();

        Supplier<StatBadgeDatabase> initializer = null;
        StatBadgeDatabase database = statManager.getDatabase();

        if ("mysql".equalsIgnoreCase(dbType)) {
            if (!(database instanceof MySQLDatabase) || database.isClosed()) {
                initializer = () -> new MySQLDatabase(config.getMySQLConfig());
            }
        } else if ("sqlite".equalsIgnoreCase(dbType)) {
            if (!(database instanceof SQLiteDatabase) || database.isClosed()) {
                initializer = () -> new SQLiteDatabase(getDataFolder(), config.getSQLiteConfig());
            }
        } else {
            getLogger().severe("Unknown database type: " + dbType);
            return false;
        }

        logDebug("Using " + dbType + " database");
        try {
            logDebug("Unloading stat manager");
            try {
                statManager.commitAndUnloadAll().get();
            } catch (Throwable e) {
                getLogger().log(Level.SEVERE, "Exception in commit stats (ignored)", e);
            }
            statManager.setDatabase(null);

            if (initializer != null) {
                if (database != null && !database.isClosed()) {
                    logDebug("Closing database");
                    try {
                        database.closeConnection();
                    } catch (Throwable e) {
                        getLogger().log(Level.SEVERE, "Exception close database (ignored)", e);
                    }
                }

                database = initializer.get();
            }

            if (database.isClosed()) {
                logDebug("Starting database");
                database.openConnection();
                database.initDatabase();
            }
            statManager.setDatabase(database);

        } catch (Throwable e) {
            getLogger().log(Level.SEVERE, "Exception in reload stats manager", e);
            return false;
        }
        return true;
    }

    private void closeStatManager() {
        logDebug("Unloading stat manager");
        try {
            statManager.commitAndUnloadAll().get();
        } catch (Throwable e) {
            getLogger().log(Level.SEVERE, "Exception in commit stats", e);
        }
        StatBadgeDatabase database = statManager.getDatabase();
        statManager.setDatabase(null);

        if (database != null) {
            logDebug("Closing database");
            try {
                database.closeConnection();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error in close database: " + e);
            }
        }
    }

    private void setupDefaultStats() {
        StatManager statManager = getStatManager();
        statManager.addPlayerActionStatsProvider(actionEntityKilled, new PlayerActionStatsProvider(this) {
            @Override
            public PlayerActionStats create(UUID playerId, ConfigurationSection config, long targetValue) throws ConfigurationError {
                String entityTypeName = Optional.ofNullable(config.getString("entity")).orElse("").toUpperCase(Locale.ROOT);
                EntityType entityType;
                try {
                    entityType = EntityType.valueOf(entityTypeName);
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationError("Unknown entity type: " + entityTypeName, e);
                }
                return new PlayerMobActionStats(playerId, actionEntityKilled, 0, targetValue, entityType);
            }
        });
        statManager.addPlayerActionStatsProvider(actionEntityDeath, new PlayerActionStatsProvider(this) {
            @Override
            public PlayerActionStats create(UUID playerId, ConfigurationSection config, long targetValue) throws ConfigurationError {
                String entityTypeName = Optional.ofNullable(config.getString("entity")).orElse("").toUpperCase(Locale.ROOT);
                EntityType entityType;
                try {
                    entityType = EntityType.valueOf(entityTypeName);
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationError("Unknown entity type: " + entityTypeName, e);
                }
                return new PlayerMobActionStats(playerId, actionEntityDeath, 0, targetValue, entityType);
            }
        });
        statManager.addPlayerActionStatsProvider(actionOnlineTime, new PlayerActionStatsProvider(this) {
            @Override
            public PlayerActionStats create(UUID playerId, ConfigurationSection config, long targetValue) throws ConfigurationError {
                long duration = TimeUnit.HOURS.toMillis(targetValue);
                return new PlayerOnlineActionStats(playerId, actionOnlineTimeSource, 0, duration, false);
            }
        });
        statManager.addPlayerActionStatsProvider(actionPlayTime, new PlayerActionStatsProvider(this) {
            @Override
            public PlayerActionStats create(UUID playerId, ConfigurationSection config, long targetValue) throws ConfigurationError {
                long duration = TimeUnit.HOURS.toMillis(targetValue);
                return new PlayerOnlineActionStats(playerId, actionOnlineTimeSource, 0, duration, true);
            }
        });
        getServer().getPluginManager().registerEvents(new PlayerMobEventListener(statManager, actionEntityKilled, actionEntityDeath), this);
    }

    private void setupHookPlugins() {
        pluginHooks.clear();
        pluginHooks.put("MythicMobs", () -> new MythicMobsHook(this, "MythicMobs", getLogger()));
        pluginHooks.put("Lands", () -> new LandsHook(this, "Lands", getLogger()));
        pluginHooks.put("AFKPlus", () -> new AFKPlusHook(this, "AFKPlus", getLogger()));
    }

    private void hookPlugins() {
        PluginManager mgr = getServer().getPluginManager();
        pluginHooks.forEach((name, initializer) -> {
            Plugin plugin = mgr.getPlugin(name);
            if (plugin == null || !plugin.isEnabled())
                return;
            PluginHook hook = initializer.get();
            if (hook.hook(plugin)) {
                hookedPlugins.add(0, hook);
                if (hook instanceof Listener) {
                    mgr.registerEvents((Listener) hook, this);
                }
                getLogger().info("Hooked plugin: " + name);
            }
        });
    }

    private void unhookPlugins() {
        for (Iterator<PluginHook> it = hookedPlugins.iterator(); it.hasNext(); ) {
            PluginHook hook = it.next();
            if (hook instanceof Listener) {
                HandlerList.unregisterAll((Listener) hook);
            }
            it.remove();
            hook.unhook();
        }
    }

    private void loadPlayer(Player player) {
        getStats().loadPlayer(player);
    }

    private void unloadPlayer(Player player) {
        getStats().unloadPlayer(player);
    }

    // interface

    @Override
    public @NotNull Logger getLogger() {
        return super.getLogger();
    }

    @Override
    public Plugin getPlugin() {
        return this;
    }

    @Override
    public StatBadgeConfig getPluginConfig() {
        return config;
    }

    @Override
    public BadgesConfig getBadgesConfig() {
        return badgesConfig;
    }

    @Override
    public StatBadgeLang getLangConfig() {
        return langConfig;
    }

    @Override
    public @Nullable Player getPlayer(UUID playerId) {
        return getServer().getPlayer(playerId);
    }

    @Override
    public Collection<? extends Player> getOnlinePlayers() {
        return getServer().getOnlinePlayers();
    }

    @Override
    public StatManager getStats() {
        if (statManager.isInitialized())
            return statManager;
        throw new RuntimeException("StatManager Database not initialized");
    }

    @Override
    public StatManager getStatManager() {
        return statManager;
    }

    @Override
    public PlayerOnlineTimeManager getPlayerOnlineTimeManager() {
        return onlineTimeManager;
    }

    @Override
    public BukkitTask runTaskLaterAsynchronously(Runnable task, long delay) {
        return getServer().getScheduler().runTaskLaterAsynchronously(this, task, delay);
    }

    @Override
    public BukkitTask runTask(Runnable task) {
        return getServer().getScheduler().runTask(this, task);
    }

    @Override
    public <E extends Event> E callEvent(E event) {
        if (!isEnabled()) {
            return null;
        }
        getServer().getPluginManager().callEvent(event);
        return event;
    }

    @Override
    public void logDebug(String message) {
        if (config.isDebugEnable()) {
            getLogger().warning("[DEBUG]: " + message);
        }
    }

    @Override
    public void logDebug(Supplier<String> message) {
        if (config.isDebugEnable()) {
            getLogger().warning(() -> "[DEBUG]: " + message.get());
        }
    }

    // events

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoinPlayer(PlayerJoinEvent event) {
        loadPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuitPlayer(PlayerQuitEvent event) {
        unloadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onDisablePlugin(PluginDisableEvent event) {
        for (Iterator<PluginHook> it = hookedPlugins.iterator(); it.hasNext(); ) {
            PluginHook hook = it.next();
            if (event.getPlugin().getName().equals(hook.getPluginName())) {
                it.remove();
                hook.unhook();
            }
        }
    }


    // test

    @EventHandler
    public void onBadgeValue(PlayerBadgeValueChangeEvent event) {
        Player player = event.getPlayer();
        logDebug(() -> player.getUniqueId() + " onBadgeValueChange [" + event.getBadge().getId() + "] " + event.getOldValue() + " -> " + event.getNewValue());
    }

    @EventHandler
    public void onBadgeComplete(PlayerBadgeCompleteEvent event) {
        Player player = event.getPlayer();
        Badge<?> badge = event.getBadge();
        logDebug(() -> player.getUniqueId() + " onBadgeComplete [" + badge.getId() + "] " + badge.getStats().getValue() + " (" + badge.getCompleteTime() + ")");

        if (config.isShowBadgeCompleteMessage()) {
            PlayerStats stats = badge.getStats();
            Object[] args = new Object[] {
                    badge.getId(),
                    ChatColor.translateAlternateColorCodes('&', badge.getTitle()),
                    ChatColor.translateAlternateColorCodes('&', badge.getName()),
                    ChatColor.translateAlternateColorCodes('&', badge.getDescription()),
                    stats.formatValue(langConfig, stats.getTargetValue()),
                    stats.formatValue(langConfig, stats.getValue()),
                    Optional.ofNullable(badge.getStartTime()).map(t -> langConfig.formatDateTime(t, true, false)).orElse("?"),
                    Optional.ofNullable(badge.getCompleteTime()).map(t -> langConfig.formatDateTime(t, true, false)).orElse("?"),
                    stats.getTargetValue() != 0 ? Math.max(0, (double) stats.getValue() / stats.getTargetValue() * 100) : 0
            };
            commands.getAudience(player).sendMessage(langConfig.format(Lang.NOTIFY_BADGE_COMPLETED, args));
        }
    }

    @EventHandler
    public void onAction(PlayerActionEvent event) {
        Player player = event.getPlayer();
        logDebug(() -> player.getUniqueId() + " onAction [" + event.getAction().getType() + "] " + event.getAction().getKey1() + ", " + event.getAction().getKey2() + ", " + event.getAction().getKey3());
    }

    @EventHandler
    public void onStats(PlayerStatsEvent event) {
        Player player = event.getPlayer();
        logDebug(() -> player.getUniqueId() + " onStats [" + event.getType() + "] " + event.getValue() + " (" + event.getTime() + ")");
    }

}
