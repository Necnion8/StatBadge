package com.gmail.necnionch.myplugin.statbadge.bukkit.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public class SQLiteDatabase extends SQLDatabase {

    private final Config config;
    private final File dbDirectory;
    private @Nullable HikariDataSource hikari;

    public record Config(String filename, Map<String, Object> options) {
    }

    public SQLiteDatabase(File dbDirectory, Config config) {
        this.config = config;
        this.dbDirectory = dbDirectory;
    }

    public Config getConfig() {
        return config;
    }

    @Override
    public boolean openConnection() {
        if (!isClosed())
            throw new IllegalStateException("Already connection available");

        File dbFile = new File(dbDirectory, config.filename);
        File dbParent = dbFile.getParentFile();
        if (!dbParent.exists() && !dbParent.mkdirs()) {
            throw new RuntimeException("Failed to create database parent directory: " + dbParent);
        }

        String url = "jdbc:sqlite:" + dbFile.toURI();
        HikariConfig dbConf = new HikariConfig();
        dbConf.setPoolName(StatBadgeDatabase.class.getSimpleName() + "-HikariPool");
        dbConf.setDriverClassName("org.sqlite.JDBC");
        dbConf.setJdbcUrl(url);
        dbConf.setAutoCommit(true);
        config.options.forEach(dbConf::addDataSourceProperty);
        dbConf.setConnectionInitSql("SELECT 1");

        hikari = new HikariDataSource(dbConf);
        return true;
    }

    @Override
    public boolean isClosed() {
        return hikari == null || hikari.isClosed();
    }

    @Override
    public void closeConnection() {
        if (hikari != null && !hikari.isClosed())
            hikari.close();
        hikari = null;
    }

    @Override
    protected Connection getConnection(boolean reconnect) throws SQLException {
        if ((hikari == null || isClosed()) && (!reconnect || !openConnection())) {
            throw new IllegalStateException("Connection is closed");
        }
        return hikari.getConnection();
    }

}
