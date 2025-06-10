package com.gmail.necnionch.myplugin.statbadge.bukkit.database;

import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MySQLDatabase extends SQLDatabase {

    private final Config config;
    private @Nullable HikariDataSource hikari;

    public record Config(String address, String database, String username, String password, Map<String, Object> options) {
    }

    public MySQLDatabase(Config config) {
        this.config = config;
    }

    public Config getConfig() {
        return config;
    }


    @Override
    public boolean openConnection() {
        if (!isClosed())
            throw new IllegalStateException("Already connection available");

        String url = "jdbc:mysql://" + config.address + "/" + config.database;
        HikariConfig dbConf = new HikariConfig();
        dbConf.setPoolName(StatBadgeDatabase.class.getSimpleName() + "-HikariPool");
        dbConf.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dbConf.setJdbcUrl(url);
        dbConf.addDataSourceProperty("user", config.username);
        dbConf.addDataSourceProperty("password", config.password);
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


    public void initDatabase() throws SQLException {
        try (Connection connection = getConnection(false)) {
            String sql = "CREATE TABLE IF NOT EXISTS `player_actions` (" +
                    "`player` VARCHAR(36) NOT NULL," +
                    "`plugin` TEXT NOT NULL," +
                    "`type` TEXT NOT NULL," +
                    "`time` BIGINT NOT NULL," +
                    "`key1` TEXT," +
                    "`key2` TEXT," +
                    "`key3` TEXT," +
                    "`value` BIGINT NOT NULL" +
                    ");";
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(sql);
            }

            sql = "CREATE TABLE IF NOT EXISTS `player_badges` (" +
                    "`player` VARCHAR(36) NOT NULL," +
                    "`id` TEXT NOT NULL," +
                    "`start_time` BIGINT," +
                    "`complete_time` BIGINT," +
                    "`title_set` BIT(1)," +
                    "UNIQUE (`player`, `id`)" +
                    ");";
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(sql);
            }
        }
    }

    public void addBadges(List<Badge<?>> badges) throws SQLException {
        String sql = "INSERT INTO `player_badges` VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `start_time` = ?, `complete_time` = ?";
        try (Connection conn = getConnectionTry();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (Badge<?> badge : badges) {
                stmt.setString(1, badge.getPlayer().toString());
                stmt.setString(2, badge.getId());
                stmt.setLong(3, Optional.ofNullable(badge.getStartTime()).map(Instant::toEpochMilli).orElse(0L));
                stmt.setLong(4, Optional.ofNullable(badge.getCompleteTime()).map(Instant::toEpochMilli).orElse(0L));
                stmt.setInt(5, badge.isTitleSet() ? 1 : 0);
                stmt.setLong(6, Optional.ofNullable(badge.getStartTime()).map(Instant::toEpochMilli).orElse(0L));
                stmt.setLong(7, Optional.ofNullable(badge.getCompleteTime()).map(Instant::toEpochMilli).orElse(0L));
                stmt.executeUpdate();
            }
        }
    }

}