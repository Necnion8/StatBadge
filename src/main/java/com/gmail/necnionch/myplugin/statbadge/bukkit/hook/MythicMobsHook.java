package com.gmail.necnionch.myplugin.statbadge.bukkit.hook;

import com.gmail.necnionch.myplugin.statbadge.bukkit.config.Lang;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.StatBadgeLang;
import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatBadgePluginInterface;
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

import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;

public class MythicMobsHook extends PluginHook implements Listener {

    private final StatBadgePluginInterface plugin;
    private final ActionType actionKilled;
    private final ActionType actionDeath;

    public MythicMobsHook(StatBadgePluginInterface plugin, String pluginName, Logger logger) {
        super(pluginName, logger);
        this.plugin = plugin;
        this.actionKilled = new ActionType(plugin.getPlugin(), "mythicmobs_killed");
        this.actionDeath = new ActionType(plugin.getPlugin(), "mythicmobs_death");
    }

    @Override
    protected boolean onHook(Plugin plugin) {
        StatManager stats = this.plugin.getStatManager();
        stats.addPlayerActionStatsProvider(actionKilled, new PlayerActionStatsProvider(this.plugin.getPlugin()) {
            @Override
            public PlayerActionStats create(UUID playerId, ConfigurationSection config) {
                return new PlayerMobActionStats(playerId, actionKilled, 0, config.getString("mob"));
            }
        });

        stats.addPlayerActionStatsProvider(actionDeath, new PlayerActionStatsProvider(this.plugin.getPlugin()) {
            @Override
            public PlayerActionStats create(UUID playerId, ConfigurationSection config) {
                return new PlayerMobActionStats(playerId, actionDeath, 0, config.getString("mob"));
            }
        });

        return true;
    }

    @Override
    protected boolean onUnhook() {
        StatManager stats = plugin.getStatManager();
        stats.removePlayerActionStatsProvider(actionKilled);
        stats.removePlayerActionStatsProvider(actionDeath);
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
                addAction(damagerPlayer, mob, actionKilled);
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
                addAction(deathPlayer, mob, actionDeath);
        }
    }

    private void addAction(Player player, ActiveMob mob, ActionType actionType) {
        plugin.getStats().addAction(player, new PlayerAction(
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
        public String formatValue(StatBadgeLang lang, long value) {
            return lang.format(Lang.UI_UNIT_COUNT, value).content();
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
