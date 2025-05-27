package com.gmail.necnionch.myplugin.statbadge.bukkit.stats;

import org.bukkit.NamespacedKey;

import java.util.UUID;

/**
 * 統計値を返すクラス<br>
 * 統計を持つ外侮プラグインはこれを返して称号を与える
 */
public class PlayerStats {

    private final NamespacedKey type;
    private final UUID player;
    private long value;

    public PlayerStats(UUID playerId, NamespacedKey statsType, long value) {
        this.player = playerId;
        this.type = statsType;
        this.value = value;
    }

    public UUID getPlayer() {
        return player;
    }

    public NamespacedKey getType() {
        return type;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

}
