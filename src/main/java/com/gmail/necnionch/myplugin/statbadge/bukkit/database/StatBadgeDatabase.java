package com.gmail.necnionch.myplugin.statbadge.bukkit.database;

import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerAction;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerActionStats;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;

public interface StatBadgeDatabase {

    boolean openConnection() throws SQLException;

    boolean isClosed();

    void closeConnection() throws Exception;

    void initDatabase() throws SQLException;


    void addActions(Iterable<PlayerAction> actions) throws SQLException;

    List<PlayerActionStats> getActionStats(@Nullable PlayerAction.Filter filter) throws SQLException;

    <AS extends PlayerActionStats> void loadActionStatsTo(Badge<AS> badge) throws SQLException;


}
