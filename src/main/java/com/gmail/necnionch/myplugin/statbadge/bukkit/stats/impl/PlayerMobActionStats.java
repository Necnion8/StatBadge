package com.gmail.necnionch.myplugin.statbadge.bukkit.stats.impl;

import com.gmail.necnionch.myplugin.statbadge.bukkit.config.Lang;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.StatBadgeLang;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.ActionType;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerActionStats;
import org.bukkit.entity.EntityType;

import java.util.UUID;

public class PlayerMobActionStats extends PlayerActionStats {
    private final EntityType entityType;

    public PlayerMobActionStats(UUID playerId, ActionType actionType, long value, EntityType entityType) {
        super(playerId, actionType, value);
        this.entityType = entityType;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    @Override
    public String formatValue(StatBadgeLang lang, long value) {
        return lang.format(Lang.UI_UNIT_COUNT, value).content();
    }

    @Override
    public PlayerActionStats.KeyCondition getKeyCondition1() {
        return PlayerActionStats.KeyCondition.match(entityType.name());
    }

    @Override
    public PlayerActionStats.KeyCondition getKeyCondition2() {
        return PlayerActionStats.KeyCondition.ANY;
    }

    @Override
    public PlayerActionStats.KeyCondition getKeyCondition3() {
        return PlayerActionStats.KeyCondition.ANY;
    }
}
