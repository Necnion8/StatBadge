package com.gmail.necnionch.myplugin.statbadge.bukkit.plugin;

import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import com.gmail.necnionch.myplugin.statbadge.bukkit.command.BukkitCommand;
import com.gmail.necnionch.myplugin.statbadge.bukkit.command.StatBadgeCommand;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.StatBadgeConfig;
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
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.impl.PlayerMobActionStats;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.impl.PlayerMobEventListener;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.impl.PlayerOnlineActionStats;
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

import java.sql.SQLException;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class StatBadgePlugin extends JavaPlugin implements StatBadgePluginInterface, Listener {

    private final BukkitCommand.Compat commands = BukkitCommand.compat(this);
    private final ActionType actionEntityKilled = new ActionType(this, "entity_killed");
    private final ActionType actionEntityDeath = new ActionType(this, "entity_death");
    private final ActionType actionOnlineTimeSource = new ActionType(this, "online_time");
    private final ActionType actionOnlineTime = new ActionType(this, "online_time");
    private final ActionType actionPlayTime = new ActionType(this, "play_time");
    private final StatBadgeConfig config = new StatBadgeConfig(this);
    private final PlayerOnlineTimeManager onlineTimeManager = new PlayerOnlineTimeManager(this, actionOnlineTimeSource);
    private @Nullable StatManager statManager;
    private @Nullable StatBadgeDatabase database;
    //
    private final Map<String, Supplier<PluginHook>> pluginHooks = new HashMap<>();
    private final List<PluginHook> hookedPlugins = new ArrayList<>();

    public static StatManager getStatManager() {
        return getPlugin(StatBadgePlugin.class).getStats();
    }

    @Override
    public void onEnable() {
        config.load();
        database = new SQLiteDatabase(getDataFolder(), new SQLiteDatabase.Config("test.db", Collections.emptyMap()));
        statManager = new StatManager(this, config, database);

        try {
            database.openConnection();
            database.initDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        getServer().getPluginManager().registerEvents(this, this);

        setupDefaultStats();
        setupHookPlugins();
        hookPlugins();
        onlineTimeManager.start();
        onlineTimeManager.loadOnlinePlayers((AFKProvider) hookedPlugins.stream().filter(hook -> hook instanceof AFKProvider).findFirst().orElse(null));
        getServer().getOnlinePlayers().forEach(this::loadPlayer);

        commands.init();
        commands.register(new StatBadgeCommand(statManager));  // TODO: load check
    }

    @Override
    public void onDisable() {
        unhookPlugins();
        onlineTimeManager.stopAndCommitAll();

        try {  // TODO: fix null
            statManager.commitAndUnloadAll().get();
            database.closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }

        commands.close();
    }

    public StatManager getStats() {
        return Objects.requireNonNull(statManager, "StatManager not initialized");
    }

    private void setupDefaultStats() {
        StatManager statManager = getStatManager();
        statManager.addPlayerActionStatsProvider(actionEntityKilled, new PlayerActionStatsProvider(this) {
            @Override
            public PlayerActionStats create(UUID playerId, String statsId, ConfigurationSection config) throws ConfigurationError {
                String entityTypeName = Optional.ofNullable(config.getString("entity")).orElse("").toUpperCase(Locale.ROOT);
                EntityType entityType;
                try {
                    entityType = EntityType.valueOf(entityTypeName);
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationError("Unknown entity type: " + entityTypeName, e);
                }
                return new PlayerMobActionStats(playerId, actionEntityKilled, 0, entityType);
            }
        });
        statManager.addPlayerActionStatsProvider(actionEntityDeath, new PlayerActionStatsProvider(this) {
            @Override
            public PlayerActionStats create(UUID playerId, String statsId, ConfigurationSection config) throws ConfigurationError {
                String entityTypeName = Optional.ofNullable(config.getString("entity")).orElse("").toUpperCase(Locale.ROOT);
                EntityType entityType;
                try {
                    entityType = EntityType.valueOf(entityTypeName);
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationError("Unknown entity type: " + entityTypeName, e);
                }
                return new PlayerMobActionStats(playerId, actionEntityDeath, 0, entityType);
            }
        });
        statManager.addPlayerActionStatsProvider(actionOnlineTime, new PlayerActionStatsProvider(this) {
            @Override
            public PlayerActionStats create(UUID playerId, String statsId, ConfigurationSection config) throws ConfigurationError {
                return new PlayerOnlineActionStats(playerId, actionOnlineTimeSource, 0, false);
            }
        });
        statManager.addPlayerActionStatsProvider(actionPlayTime, new PlayerActionStatsProvider(this) {
            @Override
            public PlayerActionStats create(UUID playerId, String statsId, ConfigurationSection config) throws ConfigurationError {
                return new PlayerOnlineActionStats(playerId, actionOnlineTimeSource, 0, true);
            }
        });
        getServer().getPluginManager().registerEvents(new PlayerMobEventListener(statManager, actionEntityKilled, actionEntityDeath), this);
    }

    private void setupHookPlugins() {
        StatManager statManager = getStatManager();
        pluginHooks.clear();
        pluginHooks.put("MythicMobs", () -> new MythicMobsHook("MythicMobs", getLogger(), statManager));
        pluginHooks.put("Lands", () -> new LandsHook("Lands", getLogger(), statManager));
        pluginHooks.put("AFKPlus", () -> new AFKPlusHook(this, "AFKPlus", getLogger(), onlineTimeManager));
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
        StatManager statManager = getStatManager();
        statManager.loadPlayer(player).thenAccept(result -> {
            if (result) {
                List<Badge<?>> badges = statManager.getPlayerBadges(player.getUniqueId());
                getLogger().warning("Loaded " + badges.size() + " player badges: " + player.getName());
                for (Badge<?> badge : badges) {
                    System.out.println("- " + badge);
                }
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    private void unloadPlayer(Player player) {
        getStatManager().unloadPlayer(player);
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
    public BukkitTask runTaskLaterAsynchronously(Runnable task, long delay) {
        return getServer().getScheduler().runTaskLaterAsynchronously(this, task, delay);
    }

    @Override
    public BukkitTask runTask(Runnable task) {
        return getServer().getScheduler().runTask(this, task);
    }

    @Override
    public <E extends Event> E callEvent(E event) {
        getServer().getPluginManager().callEvent(event);
        return event;
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
        player.sendMessage(ChatColor.DARK_AQUA + "onBadgeValueChange -> " + event.getBadge().getId() + " "
                + ChatColor.DARK_PURPLE + event.getOldValue() + ChatColor.WHITE + " -> " + ChatColor.LIGHT_PURPLE + event.getNewValue());
    }

    @EventHandler
    public void onBadgeComplete(PlayerBadgeCompleteEvent event) {
        Player player = event.getPlayer();
        player.sendMessage(ChatColor.DARK_AQUA + "onBadgeComplete -> " + event.getBadge().getId() + " "
                + ChatColor.LIGHT_PURPLE + event.getBadge().getStats().getValue() + " " + ChatColor.WHITE + event.getBadge().getCompleteTime());
    }

    @EventHandler
    public void onAction(PlayerActionEvent event) {
        Player player = event.getPlayer();
        player.sendMessage(ChatColor.DARK_RED + "onAction -> " + event.getAction().getType() + ChatColor.GRAY + " " + event.getAction().getKey1() + ", " + event.getAction().getKey2() + ", " + event.getAction().getKey3());
    }

    @EventHandler
    public void onStats(PlayerStatsEvent event) {
        Player player = event.getPlayer();
        player.sendMessage(ChatColor.DARK_RED + "onStats -> " + event.getType() + ChatColor.GRAY + " " + event.getValue() + " (" + event.getTime() + ")");
    }

}
