package com.gmail.necnionch.myplugin.statbadge.bukkit.stats;

import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

public abstract class PlayerActionStatsProvider {

    public abstract PlayerActionStats create(UUID playerId, String statsType, String actionType, ConfigurationSection config);

}
