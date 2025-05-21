package com.gmail.necnionch.myplugin.statbadge.bukkit.database;

import com.gmail.necnionch.myplugin.statbadge.bukkit.action.ActionType;
import com.gmail.necnionch.myplugin.statbadge.bukkit.action.PlayerAction;
import com.gmail.necnionch.myplugin.statbadge.bukkit.action.PlayerStats;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
                    "`plugin` TEXT NOT NULL," +
                    "`id` TEXT NOT NULL," +
                    "`start_time` BIGINT," +
                    "`complete_time` BIGINT," +
                    "UNIQUE (`player`, `plugin`, `id`)" +
                    ");";
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(sql);
            }
        }
    }


    @Override
    public void addActions(Iterable<PlayerAction> actions) throws SQLException {
        String sql = "INSERT INTO `player_actions` VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnectionTry();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (PlayerAction action : actions) {
                stmt.setString(1, action.player().toString());
                stmt.setString(2, action.type().plugin());
                stmt.setString(3, action.type().type());
                stmt.setLong(4, action.time().toEpochMilli());
                stmt.setString(5, action.key1());
                stmt.setString(6, action.key2());
                stmt.setString(7, action.key3());
                stmt.setLong(8, action.value());
                stmt.executeUpdate();
            }
        }
    }

    @Override
    public List<PlayerStats> getActionStats(PlayerAction.@Nullable Filter filter) throws SQLException {
        String sql = "SELECT `player`, `plugin`, `id`, `time`, `key1`, `key2`, `key3`, SUM(`value`) FROM `player_actions`";
        List<String> conditions = new ArrayList<>();
        List<StatementArgumentSetter> conditionArgs = new ArrayList<>();

        if (filter != null) {
            if (!filter.isAllPlayers()) {
                if (filter.players().isEmpty())
                    return Collections.emptyList();

                String uuids = filter.players().stream().map(uuid -> "\"" + uuid + "\"").collect(Collectors.joining(","));
                conditions.add("`player` in (" + uuids + ")");
            }

            Optional.ofNullable(filter.timeFrom()).ifPresent(time -> {
                conditions.add("? <= `time`");
                conditionArgs.add((stmt, idx) -> stmt.setLong(idx, time.toEpochMilli()));
            });

            Optional.ofNullable(filter.timeTo()).ifPresent(time -> {
                conditions.add("`time` <= ?");
                conditionArgs.add((stmt, idx) -> stmt.setLong(idx, time.toEpochMilli()));
            });

            Optional.ofNullable(filter.keys1()).ifPresent(keys -> {
                String joinedKeys = keys.stream().map(s -> "\"" + s.replace("\"", "\\\"") + "\"").collect(Collectors.joining(","));
                conditions.add("`key1` in (" + joinedKeys + ")");
            });

            Optional.ofNullable(filter.keys2()).ifPresent(keys -> {
                String joinedKeys = keys.stream().map(s -> "\"" + s.replace("\"", "\\\"") + "\"").collect(Collectors.joining(","));
                conditions.add("`key2` in (" + joinedKeys + ")");
            });

            Optional.ofNullable(filter.keys3()).ifPresent(keys -> {
                String joinedKeys = keys.stream().map(s -> "\"" + s.replace("\"", "\\\"") + "\"").collect(Collectors.joining(","));
                conditions.add("`key3` in (" + joinedKeys + ")");
            });
        }

        if (!conditions.isEmpty())
            sql += " WHEN " + String.join(" AND ", conditions);

        try (Connection conn = getConnectionTry();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int i = 1;
            for (StatementArgumentSetter setter : conditionArgs) {
                setter.set(stmt, i++);
            }

            try (ResultSet resultSet = stmt.executeQuery()) {
                List<PlayerStats> stats = new ArrayList<>();
                while (resultSet.next()) {
                    UUID player = UUID.fromString(resultSet.getString(1));
                    String plugin = resultSet.getString(2);
                    String id = resultSet.getString(3);
                    long time = resultSet.getLong(4);
                    String key1 = resultSet.getString(5);
                    String key2 = resultSet.getString(6);
                    String key3 = resultSet.getString(7);
                    long value = resultSet.getLong(8);
                    stats.add(new PlayerStats(player, new ActionType(plugin, id), Instant.ofEpochMilli(time), value, key1, key2, key3));
                }
                return stats;
            }
        }
    }


    interface StatementArgumentSetter {
        void set(PreparedStatement statement, int index) throws SQLException;
    }

}
