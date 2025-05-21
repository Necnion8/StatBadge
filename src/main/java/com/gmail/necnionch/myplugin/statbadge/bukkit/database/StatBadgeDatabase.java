package com.gmail.necnionch.myplugin.statbadge.bukkit.database;

import com.gmail.necnionch.myplugin.statbadge.bukkit.action.PlayerAction;
import com.gmail.necnionch.myplugin.statbadge.bukkit.action.PlayerStats;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;

public interface StatBadgeDatabase {

    boolean openConnection() throws SQLException;

    boolean isClosed();

    void closeConnection() throws Exception;

    void initDatabase() throws SQLException;


    void addActions(Iterable<PlayerAction> actions) throws SQLException;

    List<PlayerStats> getActionStats(@Nullable PlayerAction.Filter filter) throws SQLException;


}
