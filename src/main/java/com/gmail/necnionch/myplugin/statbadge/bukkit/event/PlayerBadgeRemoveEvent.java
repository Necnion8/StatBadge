package com.gmail.necnionch.myplugin.statbadge.bukkit.event;

import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerBadgeRemoveEvent extends PlayerBadgeEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public PlayerBadgeRemoveEvent(Player player, Badge<?> badge) {
        super(player, badge);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
