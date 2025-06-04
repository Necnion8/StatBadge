package com.gmail.necnionch.myplugin.statbadge.bukkit.hook;

import org.bukkit.entity.Player;

public interface AFKProvider {
    boolean isAFK(Player player);
}
