package com.gmail.necnionch.myplugin.statbadge.bukkit.plugin;

import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.StatBadgeConfig;
import com.gmail.necnionch.myplugin.statbadge.bukkit.database.SQLiteDatabase;
import com.gmail.necnionch.myplugin.statbadge.bukkit.database.StatBadgeDatabase;
import com.gmail.necnionch.myplugin.statbadge.bukkit.hook.LandsHook;
import com.gmail.necnionch.myplugin.statbadge.bukkit.hook.MythicMobsHook;
import com.gmail.necnionch.myplugin.statbadge.bukkit.hook.PluginHook;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.ActionType;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerActionStats;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerActionStatsProvider;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.impl.PlayerMobActionStats;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.impl.PlayerMobEventListener;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
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

    private final ActionType actionEntityKilled = new ActionType(this, "entity_killed");
    private final ActionType actionEntityDeath = new ActionType(this, "entity_death");
    private final StatBadgeConfig config = new StatBadgeConfig(this);
    private @Nullable StatManager statManager;
    private @Nullable StatBadgeDatabase database;
    //
    private final Map<String, Supplier<PluginHook>> pluginHooks = new HashMap<>();
    private final List<PluginHook> hookedPlugins = new ArrayList<>();

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
        getServer().getOnlinePlayers().forEach(this::loadPlayer);
    }

    @Override
    public void onDisable() {
        unhookPlugins();

        try {
            statManager.commitAndUnloadAll().get();
            database.closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void setupDefaultStats() {
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
        getServer().getPluginManager().registerEvents(new PlayerMobEventListener(statManager, actionEntityKilled, actionEntityDeath), this);
    }

    private void setupHookPlugins() {
        pluginHooks.clear();
        pluginHooks.put("MythicMobs", () -> new MythicMobsHook("MythicMobs", getLogger(), statManager));
        pluginHooks.put("Lands", () -> new LandsHook("Lands", getLogger(), statManager));
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
        statManager.loadPlayer(player.getUniqueId()).thenAccept(result -> {
            if (result) {
                List<Badge<?>> badges = statManager.getPlayerBadges(player.getUniqueId());
                getLogger().warning("Loaded " + badges.size() + " player badges: " + player.getName());
                for (Badge<?> badge : badges) {
                    System.out.println("- " + badge);
                }
            }
        });
    }

    private void unloadPlayer(Player player) {
        statManager.commitAndUnloadPlayer(player.getUniqueId());
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
    public BukkitTask runTaskAsynchronously(Runnable task) {
        return getServer().getScheduler().runTaskAsynchronously(this, task);
    }

    @Override
    public <E extends Event> E callEvent(E event) {
        return event;
    }

    // events

    @EventHandler
    public void onJoinPlayer(PlayerJoinEvent event) {
        loadPlayer(event.getPlayer());
    }

    @EventHandler
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

}
