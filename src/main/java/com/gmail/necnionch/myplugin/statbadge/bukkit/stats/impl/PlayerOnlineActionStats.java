package com.gmail.necnionch.myplugin.statbadge.bukkit.stats.impl;

import com.gmail.necnionch.myplugin.statbadge.bukkit.config.StatBadgeLang;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.ActionType;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerActionStats;

import java.time.Duration;
import java.util.UUID;

public class PlayerOnlineActionStats extends PlayerActionStats {

    public static final String KEY_AFK_OFF = "0";
    public static final String KEY_AFK_ON = "1";
    private final boolean onlyPlayTime;

    public PlayerOnlineActionStats(UUID playerId, ActionType actionType, long value, boolean onlyPlayTime) {
        super(playerId, actionType, value);
        this.onlyPlayTime = onlyPlayTime;
    }

    @Override
    public String formatValue(StatBadgeLang lang, long value) {
        return lang.formatDuration(Duration.ofMillis(value));
    }

    public boolean isOnlyPlayTime() {
        return onlyPlayTime;
    }

    @Override
    public KeyCondition getKeyCondition1() {
        return onlyPlayTime ? KeyCondition.match(KEY_AFK_OFF) : KeyCondition.ANY;
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
