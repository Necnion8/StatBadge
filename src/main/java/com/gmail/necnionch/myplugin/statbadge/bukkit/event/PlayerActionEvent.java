package com.gmail.necnionch.myplugin.statbadge.bukkit.event;

import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerAction;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerActionEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final PlayerAction action;
    private final Player player;

    public PlayerActionEvent(Player player, PlayerAction action) {
        this.player = player;
        this.action = action;
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerAction getAction() {
        return action;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
