package com.gmail.necnionch.myplugin.statbadge.bukkit;

import com.gmail.necnionch.myplugin.statbadge.bukkit.action.ActionType;
import com.gmail.necnionch.myplugin.statbadge.bukkit.action.PlayerAction;
import com.gmail.necnionch.myplugin.statbadge.bukkit.database.SQLiteDatabase;
import com.gmail.necnionch.myplugin.statbadge.bukkit.database.StatBadgeDatabase;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;

public final class StatBadgePlugin extends JavaPlugin {

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

            private final ActionType actionType = new ActionType(getName(), "mythicmobs_killed");

            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onKill(EntityDamageByEntityEvent event) {
                if (!(event.getDamager() instanceof Player))
                    return;

                if (event.getEntity() instanceof LivingEntity && ((LivingEntity) event.getEntity()).getHealth() - event.getFinalDamage() <= 0) {
                    try (MythicBukkit api = MythicBukkit.inst()) {
                        ActiveMob mob = api.getMobManager().getMythicMobInstance(event.getEntity());
                        if (mob != null) {
                            statManager.addAction(new PlayerAction(
                                    event.getDamager().getUniqueId(),
                                    actionType,
                                    Instant.now(),
                                    1,
                                    mob.getMobType(),
                                    null,
                                    null
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

}
