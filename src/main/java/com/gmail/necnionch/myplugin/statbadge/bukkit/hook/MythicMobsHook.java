package com.gmail.necnionch.myplugin.statbadge.bukkit.hook;

import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatBadgePlugin;
import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatManager;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.ActionType;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerAction;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerActionStats;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerActionStatsProvider;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;

public class MythicMobsHook extends PluginHook implements Listener {

    private static final JavaPlugin PLUGIN = JavaPlugin.getProvidingPlugin(StatBadgePlugin.class);
    public static final ActionType ACTION_MM_KILLED = new ActionType(PLUGIN, "mythicmobs_killed");
    public static final ActionType ACTION_MM_DEATH = new ActionType(PLUGIN, "mythicmobs_death");
    private final StatManager stats;

    public MythicMobsHook(String pluginName, Logger logger, StatManager stats) {
        super(pluginName, logger);
        this.stats = stats;
    }

    @Override
    protected boolean onHook(Plugin plugin) {
        stats.addPlayerActionStatsProvider(ACTION_MM_KILLED, new PlayerActionStatsProvider(PLUGIN) {
            @Override
            public PlayerActionStats create(UUID playerId, String statsId, ConfigurationSection config) {
                return new PlayerMobActionStats(playerId, ACTION_MM_KILLED, 0, config.getString("mob"));
            }
        });

        stats.addPlayerActionStatsProvider(ACTION_MM_DEATH, new PlayerActionStatsProvider(PLUGIN) {
            @Override
            public PlayerActionStats create(UUID playerId, String statsId, ConfigurationSection config) {
                return new PlayerMobActionStats(playerId, ACTION_MM_DEATH, 0, config.getString("mob"));
            }
        });

        return true;
    }

    @Override
    protected boolean onUnhook() {
        stats.removePlayerActionStatsProvider(ACTION_MM_KILLED);
        stats.removePlayerActionStatsProvider(ACTION_MM_DEATH);
        return true;
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

        try (MythicBukkit api = MythicBukkit.inst()) {
            ActiveMob mob = api.getMobManager().getMythicMobInstance(deathEntity);
            if (mob != null)
                addAction(damagerPlayer, mob, ACTION_MM_KILLED);
        }
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

        try (MythicBukkit api = MythicBukkit.inst()) {
            ActiveMob mob = api.getMobManager().getMythicMobInstance(damager);
            if (mob != null)
                addAction(deathPlayer, mob, ACTION_MM_DEATH);
        }
    }

    private void addAction(Player player, ActiveMob mob, ActionType actionType) {
        stats.addAction(player, new PlayerAction(
                player.getUniqueId(), actionType, Instant.now(), mob.getMobType(), null, null, 1
        ));
    }


    public static class PlayerMobActionStats extends PlayerActionStats {

        private final String mobType;

        public PlayerMobActionStats(UUID playerId, ActionType actionType, long value, String mobType) {
            super(playerId, actionType, value);
            this.mobType = mobType;
        }

        public String getMobType() {
            return mobType;
        }

        @Override
        public KeyCondition getKeyCondition1() {
            return KeyCondition.match(mobType);
        }

        @Override
        public KeyCondition getKeyCondition2() {
            return KeyCondition.ANY;
        }

        @Override
        public KeyCondition getKeyCondition3() {
            return KeyCondition.ANY;
        }
    }


}
