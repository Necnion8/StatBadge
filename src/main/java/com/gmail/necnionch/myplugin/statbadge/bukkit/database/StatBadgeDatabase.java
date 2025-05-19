package com.gmail.necnionch.myplugin.statbadge.bukkit.database;

import com.gmail.necnionch.myplugin.statbadge.bukkit.action.ActionRecord;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;

public interface StatBadgeDatabase {

    boolean openConnection() throws SQLException;

    boolean isClosed();

    void closeConnection() throws Exception;

    void initDatabase() throws SQLException;


    void addAction(ActionRecord action);

    void addActions(Iterable<ActionRecord> actions);

    List<ActionRecord.Stats> getActionStats(@Nullable ActionRecord.Filter filter);


}
