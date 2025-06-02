package com.gmail.necnionch.myplugin.statbadge.bukkit.event;

import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.StatsType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class PlayerStatsEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final StatsType type;
    private final Instant time;
    private final long value;

    public PlayerStatsEvent(Player player, StatsType type, Instant time, long value) {
        this.player = player;
        this.type = type;
        this.time = time;
        this.value = value;
    }

    public Player getPlayer() {
        return player;
    }

    public StatsType getType() {
        return type;
    }

    public Instant getTime() {
        return time;
    }

    public long getValue() {
        return value;
    }


    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
