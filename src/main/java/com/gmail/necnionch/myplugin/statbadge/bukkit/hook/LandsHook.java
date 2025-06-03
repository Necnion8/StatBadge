package com.gmail.necnionch.myplugin.statbadge.bukkit.hook;

import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatBadgePlugin;
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
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class LandsHook extends PluginHook implements Listener {

    private static final StatBadgePlugin PLUGIN = JavaPlugin.getPlugin(StatBadgePlugin.class);
    public static final StatsType STATS_LAND_CHUNKS = new StatsType(PLUGIN, "lands_chunks");
    public static final ActionType ACTION_LAND_WAR_WINS = new ActionType(PLUGIN, "lands_war_wins");
    public static final ActionType ACTION_LAND_WAR_LOSES = new ActionType(PLUGIN, "lands_war_loses");
    private LandsIntegration lands;

    private final StatManager stats;

    public LandsHook(String pluginName, Logger logger, StatManager stats) {
        super(pluginName, logger);
        this.stats = stats;
    }

    @Override
    protected boolean onHook(Plugin plugin) {
        lands = LandsIntegration.of(PLUGIN);
        stats.addPlayerStatsProvider(STATS_LAND_CHUNKS, new PlayerStatsProvider(PLUGIN) {
            @Override
            public PlayerStats create(UUID playerId, String statsId, ConfigurationSection config) {
                return new PlayerStats(playerId, STATS_LAND_CHUNKS, getOwnLandChunkCountOrZero(playerId));
            }
        });
        stats.addPlayerActionStatsProvider(ACTION_LAND_WAR_WINS, new PlayerActionStatsProvider(PLUGIN) {
            @Override
            public PlayerActionStats create(UUID playerId, String statsId, ConfigurationSection config) throws ConfigurationError {
                return new PlayerLandWarCount(playerId, ACTION_LAND_WAR_WINS, 0);
            }
        });
        stats.addPlayerActionStatsProvider(ACTION_LAND_WAR_LOSES, new PlayerActionStatsProvider(PLUGIN) {
            @Override
            public PlayerActionStats create(UUID playerId, String statsId, ConfigurationSection config) throws ConfigurationError {
                return new PlayerLandWarCount(playerId, ACTION_LAND_WAR_LOSES, 0);
            }
        });
        return true;
    }

    @Override
    protected boolean onUnhook() {
        stats.removePlayerStatsProvider(STATS_LAND_CHUNKS);
        stats.removePlayerActionStatsProvider(ACTION_LAND_WAR_WINS);
        stats.removePlayerActionStatsProvider(ACTION_LAND_WAR_LOSES);
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
        Optional.ofNullable(PLUGIN.getServer().getPlayer(playerId))
                .ifPresent(p -> stats.changeStats(p, type, null, action.get()));
    }

    private void addPlayerAction(Player player, ActionType actionType) {
        stats.addAction(player, new PlayerAction(player.getUniqueId(), actionType, Instant.now(), null, null, null, 1));
    }



    @EventHandler
    public void onLoadPlayer(PlayerDataLoadedEvent event) {
        LandPlayer landPlayer = event.getLandPlayer();
        Runnable task = () -> stats.changeStats(landPlayer.getPlayer(), STATS_LAND_CHUNKS, null, getOwnLandChunkCount(landPlayer));

        if (event.isAsynchronous()) {
            PLUGIN.runTask(task);
        } else {
            task.run();
        }
    }

    @EventHandler
    public void onCreate(LandCreateEvent event) {
        Land land = event.getLand();
        UUID ownerId = land.getOwnerUID();
        if (event.isAsynchronous()) {
            PLUGIN.runTask(() -> updatePlayerLandChunks(ownerId));
        } else {
            updatePlayerLandChunks(ownerId);
        }
    }

    @EventHandler
    public void onClaim(ChunkPostClaimEvent event) {
        Land land = event.getLand();
        UUID ownerId = land.getOwnerUID();
        if (event.isAsynchronous()) {
            PLUGIN.runTask(() -> updatePlayerLandChunks(ownerId));
        } else {
            updatePlayerLandChunks(ownerId);
        }
    }

    @EventHandler
    public void onWarEnd(WarEndEvent event) {
        if (event.isAsynchronous()) {
            PLUGIN.runTask(() -> processWarEndEvent(event));
        } else {
            processWarEndEvent(event);
        }
    }

    private void updatePlayerLandChunks(UUID ownerId) {
        putPlayerStats(ownerId, STATS_LAND_CHUNKS, () -> getOwnLandChunkCount(ownerId));
    }

    private void processWarEndEvent(WarEndEvent event) {
        MemberHolder winner = event.getWinner();
        MemberHolder loser = event.getLoser();

        if (winner != null) {
            for (Player player : winner.getOnlinePlayers()) {
                addPlayerAction(player, ACTION_LAND_WAR_WINS);
            }
        }
        if (loser != null) {
            for (Player player : loser.getOnlinePlayers()) {
                addPlayerAction(player, ACTION_LAND_WAR_LOSES);
            }
        }
    }


    public static class PlayerLandWarCount extends PlayerActionStats {
        public PlayerLandWarCount(UUID playerId, ActionType sourceActionType, long value) {
            super(playerId, sourceActionType, value);
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
