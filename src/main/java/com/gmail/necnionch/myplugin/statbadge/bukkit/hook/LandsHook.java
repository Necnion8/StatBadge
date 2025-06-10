package com.gmail.necnionch.myplugin.statbadge.bukkit.hook;

import com.gmail.necnionch.myplugin.statbadge.bukkit.config.Lang;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.StatBadgeLang;
import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatBadgePluginInterface;
import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatManager;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.*;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.events.ChunkPostClaimEvent;
import me.angeschossen.lands.api.events.LandCreateEvent;
import me.angeschossen.lands.api.events.player.database.PlayerDataLoadedEvent;
import me.angeschossen.lands.api.events.war.WarEndEvent;
import me.angeschossen.lands.api.land.Land;
import me.angeschossen.lands.api.memberholder.MemberHolder;
import me.angeschossen.lands.api.player.LandPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class LandsHook extends PluginHook implements Listener {

    private final StatBadgePluginInterface plugin;
    private LandsIntegration lands;
    private final StatsType statsLandChunks;
    private final ActionType actionWarWins;
    private final ActionType actionWarLoses;

    public LandsHook(StatBadgePluginInterface plugin, String pluginName, Logger logger) {
        super(pluginName, logger);
        this.plugin = plugin;
        this.statsLandChunks = new StatsType(plugin.getPlugin(), "lands_chunks");
        this.actionWarWins = new ActionType(plugin.getPlugin(), "lands_war_wins");
        this.actionWarLoses = new ActionType(plugin.getPlugin(), "lands_war_loses");
    }

    @Override
    protected boolean onHook(Plugin plugin) {
        StatManager stats = this.plugin.getStatManager();
        lands = LandsIntegration.of(this.plugin.getPlugin());
        stats.addPlayerStatsProvider(statsLandChunks, new PlayerStatsProvider(this.plugin.getPlugin()) {
            @Override
            public PlayerStats create(UUID playerId, ConfigurationSection config, long targetValue) {
                return new PlayerStats(playerId, statsLandChunks, getOwnLandChunkCountOrZero(playerId), targetValue);
            }
        });
        stats.addPlayerActionStatsProvider(actionWarWins, new PlayerActionStatsProvider(this.plugin.getPlugin()) {
            @Override
            public PlayerActionStats create(UUID playerId, ConfigurationSection config, long targetValue) throws ConfigurationError {
                return new PlayerLandWarCount(playerId, actionWarWins, 0, targetValue);
            }
        });
        stats.addPlayerActionStatsProvider(actionWarLoses, new PlayerActionStatsProvider(this.plugin.getPlugin()) {
            @Override
            public PlayerActionStats create(UUID playerId, ConfigurationSection config, long targetValue) throws ConfigurationError {
                return new PlayerLandWarCount(playerId, actionWarLoses, 0, targetValue);
            }
        });
        return true;
    }

    @Override
    protected boolean onUnhook() {
        StatManager stats = this.plugin.getStatManager();
        stats.removePlayerStatsProvider(statsLandChunks);
        stats.removePlayerActionStatsProvider(actionWarWins);
        stats.removePlayerActionStatsProvider(actionWarLoses);
        lands = null;
        return true;
    }

    private int getOwnLandChunkCount(UUID player) {
        if (lands == null)
            throw new RuntimeException("No loaded LandsIntegration");
        LandPlayer landPlayer = lands.getLandPlayer(player);
        if (landPlayer == null)
            throw new IllegalArgumentException("LandPlayer not loaded");
        return getOwnLandChunkCount(landPlayer);
    }

    private int getOwnLandChunkCountOrZero(UUID player) {
        if (lands == null)
            return 0;
        LandPlayer landPlayer = lands.getLandPlayer(player);
        if (landPlayer == null)
            return 0;
        return getOwnLandChunkCount(landPlayer);
    }

    private int getOwnLandChunkCount(LandPlayer player) {
        return player.getLands().stream()
                .filter(land -> player.getUID().equals(land.getOwnerUID()))
                .mapToInt(MemberHolder::getChunksAmount)
                .sum();
    }

    private void putPlayerStats(UUID playerId, StatsType type, Supplier<Integer> action) {
        Optional.ofNullable(plugin.getPlayer(playerId))
                .ifPresent(p -> plugin.getStats().changeStats(p, type, null, action.get()));
    }

    private void addPlayerAction(Player player, ActionType actionType) {
        plugin.getStats().addAction(player, new PlayerAction(player.getUniqueId(), actionType, Instant.now(), null, null, null, 1));
    }



    @EventHandler
    public void onLoadPlayer(PlayerDataLoadedEvent event) {
        LandPlayer landPlayer = event.getLandPlayer();
        Runnable task = () -> plugin.getStats().changeStats(landPlayer.getPlayer(), statsLandChunks, null, getOwnLandChunkCount(landPlayer));

        if (event.isAsynchronous()) {
            plugin.runTask(task);
        } else {
            task.run();
        }
    }

    @EventHandler
    public void onCreate(LandCreateEvent event) {
        Land land = event.getLand();
        UUID ownerId = land.getOwnerUID();
        if (event.isAsynchronous()) {
            plugin.runTask(() -> updatePlayerLandChunks(ownerId));
        } else {
            updatePlayerLandChunks(ownerId);
        }
    }

    @EventHandler
    public void onClaim(ChunkPostClaimEvent event) {
        Land land = event.getLand();
        UUID ownerId = land.getOwnerUID();
        if (event.isAsynchronous()) {
            plugin.runTask(() -> updatePlayerLandChunks(ownerId));
        } else {
            updatePlayerLandChunks(ownerId);
        }
    }

    @EventHandler
    public void onWarEnd(WarEndEvent event) {
        if (event.isAsynchronous()) {
            plugin.runTask(() -> processWarEndEvent(event));
        } else {
            processWarEndEvent(event);
        }
    }

    private void updatePlayerLandChunks(UUID ownerId) {
        putPlayerStats(ownerId, statsLandChunks, () -> getOwnLandChunkCount(ownerId));
    }

    private void processWarEndEvent(WarEndEvent event) {
        MemberHolder winner = event.getWinner();
        MemberHolder loser = event.getLoser();

        if (winner != null) {
            for (Player player : winner.getOnlinePlayers()) {
                addPlayerAction(player, actionWarWins);
            }
        }
        if (loser != null) {
            for (Player player : loser.getOnlinePlayers()) {
                addPlayerAction(player, actionWarLoses);
            }
        }
    }


    public static class PlayerLandWarCount extends PlayerActionStats {
        public PlayerLandWarCount(UUID playerId, ActionType sourceActionType, long value, long targetValue) {
            super(playerId, sourceActionType, value, targetValue);
        }

        @Override
        public String formatValue(StatBadgeLang lang, long value) {
            return lang.format(Lang.UI_UNIT_CHUNKS, value).content();
        }

        @Override
        public KeyCondition getKeyCondition1() {
            return KeyCondition.ANY;
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
