package com.gmail.necnionch.myplugin.statbadge.bukkit.event;

import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerBadgeValueChangeEvent extends PlayerBadgeEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final long newValue;
    private final long oldValue;

    public PlayerBadgeValueChangeEvent(Player player, Badge<?> badge, long newValue, long oldValue) {
        super(player, badge);
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

    public long getNewValue() {
        return newValue;
    }

    public long getOldValue() {
        return oldValue;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
