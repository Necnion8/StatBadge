package com.gmail.necnionch.myplugin.statbadge.bukkit.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.util.*;

public class SQLiteDatabase implements StatBadgeDatabase {

    private final Config config;
    private final File dbDirectory;
    private @Nullable HikariDataSource hikari;

    public static class Config {

        private final String filename;
        private final Map<String, Object> options;

        public Config(String filename, Map<String, Object> options) {
            this.filename = filename;
            this.options = options;
        }

        public String getFilename() {
            return filename;
        }

        public Map<String, Object> options() {
            return options;
        }
    }

    public SQLiteDatabase(File dbDirectory, Config config) {
        this.config = config;
        this.dbDirectory = dbDirectory;
    }

    public Config getConfig() {
        return config;
    }

    public File getParentDir() {
        return dbDirectory;
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

    private Connection getConnection(boolean reconnect) throws SQLException {
        if ((hikari == null || isClosed()) && (!reconnect || !openConnection())) {
            throw new IllegalStateException("Connection is closed");
        }
        return hikari.getConnection();
    }

    private Connection getConnectionTry() throws SQLException {
        return getConnection(true);
    }

    @Override
    public void initDatabase() throws SQLException {
        try (Connection connection = getConnection(false)) {
            String sql = "CREATE TABLE IF NOT EXISTS `player_actions` (" +
                    "`player` VARCHAR(36) NOT NULL," +
                    "`plugin` TEXT NOT NULL," +
                    "`id` TEXT NOT NULL," +
                    "`time` BIGINT NOT NULL," +
                    "`value` BIGINT NOT NULL," +
                    "`extra` TEXT," +
                    ");";
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(sql);
            }

            sql = "CREATE TABLE IF NOT EXISTS `player_titles` (" +
                    "`player` VARCHAR(36) NOT NULL," +
                    "`plugin` TEXT NOT NULL," +
                    "`id` TEXT NOT NULL," +
                    "`start_time` BIGINT," +
                    "`complete_time` BIGINT," +
                    "UNIQUE KEY (`player`, `plugin`, `id`)," +
                    ");";
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(sql);
            }
        }
    }

}
