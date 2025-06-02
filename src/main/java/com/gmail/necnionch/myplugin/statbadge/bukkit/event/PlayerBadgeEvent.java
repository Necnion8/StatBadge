package com.gmail.necnionch.myplugin.statbadge.bukkit.event;

import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public abstract class PlayerBadgeEvent extends Event {

    private final Player player;
    private final Badge<?> badge;

    public PlayerBadgeEvent(Player player, Badge<?> badge) {
        this.player = player;
        this.badge = badge;
    }

    public Player getPlayer() {
        return player;
    }

    public Badge<?> getBadge() {
        return badge;
    }

}
