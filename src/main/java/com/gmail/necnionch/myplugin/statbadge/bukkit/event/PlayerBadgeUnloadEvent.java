package com.gmail.necnionch.myplugin.statbadge.bukkit.event;

import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PlayerBadgeUnloadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final List<Badge<?>> badges;

    public PlayerBadgeUnloadEvent(Player player, List<Badge<?>> badges) {
        this.player = player;
        this.badges = Collections.unmodifiableList(badges);
    }

    public Player getPlayer() {
        return player;
    }

    public List<Badge<?>> getBadges() {
        return badges;
    }


    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
