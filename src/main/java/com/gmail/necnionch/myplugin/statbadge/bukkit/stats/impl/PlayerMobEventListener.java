package com.gmail.necnionch.myplugin.statbadge.bukkit.stats.impl;

import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatManager;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.ActionType;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerAction;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.time.Instant;

public class PlayerMobEventListener implements Listener {

    private final StatManager stats;
    public final ActionType actionKilled;
    public final ActionType actionDeath;

    public PlayerMobEventListener(StatManager stats, ActionType actionKilledType, ActionType actionDeathType) {
        this.stats = stats;
        this.actionKilled = actionKilledType;
        this.actionDeath = actionDeathType;
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKilledByPlayer(EntityDeathEvent event) {
        LivingEntity deathEntity = event.getEntity();

        if (!(deathEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent causeEvent))
            return;

        Entity damager = causeEvent.getDamager();
        if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof LivingEntity shooter) {
            damager = shooter;
        }

        if (!(damager instanceof Player damagerPlayer))
            return;

        addAction(damagerPlayer, deathEntity.getType(), actionKilled);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKilledByMob(EntityDeathEvent event) {
        LivingEntity deathEntity = event.getEntity();

        if (!(deathEntity instanceof Player deathPlayer))
            return;

        if (!(deathEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent causeEvent))
            return;

        Entity damager = causeEvent.getDamager();
        if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof LivingEntity shooter) {
            damager = shooter;
        }

        addAction(deathPlayer, damager.getType(), actionDeath);
    }

    private void addAction(Player player, EntityType type, ActionType actionType) {
        stats.addAction(player, new PlayerAction(
                player.getUniqueId(), actionType, Instant.now(), type.name(), null, null, 1
        ));
    }

}
