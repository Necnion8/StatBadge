package com.gmail.necnionch.myplugin.statbadge.bukkit.plugin;

import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.StatBadgeConfig;
import com.gmail.necnionch.myplugin.statbadge.bukkit.database.SQLiteDatabase;
import com.gmail.necnionch.myplugin.statbadge.bukkit.database.StatBadgeDatabase;
import com.gmail.necnionch.myplugin.statbadge.bukkit.hook.MythicMobsHook;
import com.gmail.necnionch.myplugin.statbadge.bukkit.hook.PluginHook;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.StatsType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.*;
import java.util.function.Supplier;

public final class StatBadgePlugin extends JavaPlugin implements StatBadgePluginInterface {

    private final StatBadgeConfig config = new StatBadgeConfig(this);
    private @Nullable StatManager statManager;
    private @Nullable StatBadgeDatabase database;
    //
    private final Map<String, Supplier<PluginHook>> pluginHooks = new HashMap();
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

        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                loadPlayer(event.getPlayer());
            }
        }, this);

        setupHookPlugins();
        hookPlugins();
        loadOnlinePlayers();
    }

    @Override
    public void onDisable() {
        unhookPlugins();

        statManager.commitAll();

        try {
            database.closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void setupHookPlugins() {
        pluginHooks.clear();
        pluginHooks.put("MythicMobs", () -> new MythicMobsHook("MythicMobs", getLogger(), statManager));
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

    private void loadOnlinePlayers() {
        getServer().getOnlinePlayers().forEach(this::loadPlayer);
    }

    private void loadPlayer(Player player) {
        statManager.loadPlayerBadges(player.getUniqueId()).whenComplete((badges, throwable) -> {
            if (badges != null) {
                getLogger().warning("Loaded " + badges.size() + " player badges: " + player.getName());
                for (Badge<?> badge : badges) {
                    long target = badge.getActionTargetValue();
                    StatsType type = badge.getStats().getType();
                    System.out.println("- " + type.toString() + " target=" + target + ", value=" + badge.getStats().getValue());
                }
            }
        });
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
}
