package com.gmail.necnionch.myplugin.statbadge.bukkit.plugin;

import com.gmail.necnionch.myplugin.statbadge.bukkit.database.SQLiteDatabase;
import com.gmail.necnionch.myplugin.statbadge.bukkit.database.StatBadgeDatabase;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerAction;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;

public final class StatBadgePlugin extends JavaPlugin implements StatBadgePluginInterface {

    private @Nullable StatManager statManager;
    private @Nullable StatBadgeDatabase database;

    @Override
    public void onEnable() {
        database = new SQLiteDatabase(getDataFolder(), new SQLiteDatabase.Config("test.db", Collections.emptyMap()));
        statManager = new StatManager(this, database);

        try {
            database.openConnection();
            database.initDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        getServer().getPluginManager().registerEvents(new Listener() {

            private final NamespacedKey actionType = new NamespacedKey(StatBadgePlugin.this, "mythicmobs_killed");

            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onKill(EntityDamageByEntityEvent event) {
                if (!(event.getDamager() instanceof Player))
                    return;

                if (event.getEntity() instanceof LivingEntity && ((LivingEntity) event.getEntity()).getHealth() - event.getFinalDamage() <= 0) {
                    try (MythicBukkit api = MythicBukkit.inst()) {
                        ActiveMob mob = api.getMobManager().getMythicMobInstance(event.getEntity());
                        if (mob != null) {
                            statManager.addAction((Player) event.getDamager(), new PlayerAction(
                                    event.getDamager().getUniqueId(),
                                    actionType,
                                    Instant.now(),
                                    mob.getMobType(),
                                    null,
                                    null,
                                    1
                            ));
                        }
                    }
                }
            }
        }, this);
    }

    @Override
    public void onDisable() {
        statManager.commitAll();

        try {
            database.closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public BukkitTask runTaskLaterAsynchronously(Runnable task, long delay) {
        return getServer().getScheduler().runTaskLaterAsynchronously(this, task, delay);
    }

    @Override
    public <E extends Event> E callEvent(E event) {
        return event;
    }
}
