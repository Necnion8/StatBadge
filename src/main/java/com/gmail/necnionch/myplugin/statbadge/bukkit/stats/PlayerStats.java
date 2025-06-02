package com.gmail.necnionch.myplugin.statbadge.bukkit.stats;

import java.util.UUID;

/**
 * 統計値を返すクラス<br>
 * 統計を持つ外侮プラグインはこれを返して称号を与える
 */
public class PlayerStats {

    private final StatsType type;
    private final UUID player;
    private long value;

    public PlayerStats(UUID playerId, StatsType statsType, long value) {
        this.player = playerId;
        this.type = statsType;
        this.value = value;
    }

    public UUID getPlayer() {
        return player;
    }

    public StatsType getType() {
        return type;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{type=\"" + type.toString() + "\", value=" + value + "}";
    }
}
