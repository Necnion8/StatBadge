package com.gmail.necnionch.myplugin.statbadge.bukkit.stats;

import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

public abstract class PlayerStatsProvider {

    public abstract PlayerStats create(UUID playerId, String statsType, ConfigurationSection config);

}
