package com.gmail.necnionch.myplugin.statbadge.bukkit.stats;

import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.StatBadgeLang;

import java.util.UUID;

/**
 * 統計値を返すクラス<br>
 * 統計を持つ外侮プラグインはこれを返して称号を与える
 */
public class PlayerStats {

    private final StatsType type;
    private final UUID player;
    private final long targetValue;
    private long value;

    public PlayerStats(UUID playerId, StatsType statsType, long value, long targetValue) {
        this.player = playerId;
        this.type = statsType;
        this.value = value;
        this.targetValue = targetValue;
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

    public long getTargetValue() {
        return targetValue;
    }

    public String formatValue(StatBadgeLang lang, long value) {
        return String.format("%,d", value);
    }

    public boolean compareTargetValue(Badge<?> badge) {
        return getTargetValue() <= value;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{type=\"" + type.toString() + "\", value=" + value + "}";
    }
}
