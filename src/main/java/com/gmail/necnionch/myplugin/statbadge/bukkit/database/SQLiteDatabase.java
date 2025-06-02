package com.gmail.necnionch.myplugin.statbadge.bukkit.database;

import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerAction;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerActionStats;
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
        String sql = "INSERT OR REPLACE INTO `player_actions` VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnectionTry();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (PlayerAction action : actions) {
                stmt.setString(1, action.getPlayer().toString());
                stmt.setString(2, action.getType().getNamespace());
                stmt.setString(3, action.getType().getKey());
                stmt.setLong(4, action.getTime().toEpochMilli());
                stmt.setString(5, action.getKey1());
                stmt.setString(6, action.getKey2());
                stmt.setString(7, action.getKey3());
                stmt.setLong(8, action.getValue());
                stmt.executeUpdate();
            }
        }
    }

    @Override
    public void addBadges(List<Badge<?>> badges) throws SQLException {
        String sql = "INSERT OR REPLACE INTO `player_badges` VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnectionTry();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (Badge<?> badge : badges) {
                stmt.setString(1, badge.getPlayer().toString());
                stmt.setString(2, badge.getId());
                stmt.setLong(3, Optional.ofNullable(badge.getStartTime()).map(Instant::toEpochMilli).orElse(0L));
                stmt.setLong(4, Optional.ofNullable(badge.getCompleteTime()).map(Instant::toEpochMilli).orElse(0L));
                stmt.executeUpdate();
            }
        }
    }

    @Override
    public List<PlayerActionStats> getActionStats(PlayerAction.@Nullable Filter filter) throws SQLException {
//        String sql = "SELECT `player`, `plugin`, `id`, `time`, `key1`, `key2`, `key3`, SUM(`value`) FROM `player_actions`";
//        List<String> conditions = new ArrayList<>();
//        List<StatementArgumentSetter> conditionArgs = new ArrayList<>();
//
//        if (filter != null) {
//            if (!filter.isAllPlayers()) {
//                if (filter.players().isEmpty())
//                    return Collections.emptyList();
//
//                String uuids = filter.players().stream().map(uuid -> "\"" + uuid + "\"").collect(Collectors.joining(","));
//                conditions.add("`player` in (" + uuids + ")");
//            }
//
//            Optional.ofNullable(filter.timeFrom()).ifPresent(time -> {
//                conditions.add("? <= `time`");
//                conditionArgs.add((stmt, idx) -> stmt.setLong(idx, time.toEpochMilli()));
//            });
//
//            Optional.ofNullable(filter.timeTo()).ifPresent(time -> {
//                conditions.add("`time` <= ?");
//                conditionArgs.add((stmt, idx) -> stmt.setLong(idx, time.toEpochMilli()));
//            });
//
//            Optional.ofNullable(filter.keys1()).ifPresent(keys -> {
//                String joinedKeys = keys.stream().map(s -> "\"" + s.replace("\"", "\\\"") + "\"").collect(Collectors.joining(","));
//                conditions.add("`key1` in (" + joinedKeys + ")");
//            });
//
//            Optional.ofNullable(filter.keys2()).ifPresent(keys -> {
//                String joinedKeys = keys.stream().map(s -> "\"" + s.replace("\"", "\\\"") + "\"").collect(Collectors.joining(","));
//                conditions.add("`key2` in (" + joinedKeys + ")");
//            });
//
//            Optional.ofNullable(filter.keys3()).ifPresent(keys -> {
//                String joinedKeys = keys.stream().map(s -> "\"" + s.replace("\"", "\\\"") + "\"").collect(Collectors.joining(","));
//                conditions.add("`key3` in (" + joinedKeys + ")");
//            });
//        }
//
//        if (!conditions.isEmpty())
//            sql += " WHEN " + String.join(" AND ", conditions);
//
//        try (Connection conn = getConnectionTry();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//
//            int i = 1;
//            for (StatementArgumentSetter setter : conditionArgs) {
//                setter.set(stmt, i++);
//            }
//
//            try (ResultSet resultSet = stmt.executeQuery()) {
//                List<PlayerActionStats> stats = new ArrayList<>();
//                while (resultSet.next()) {
//                    UUID player = UUID.fromString(resultSet.getString(1));
//                    String plugin = resultSet.getString(2);
//                    String id = resultSet.getString(3);
//                    long time = resultSet.getLong(4);
//                    String key1 = resultSet.getString(5);
//                    String key2 = resultSet.getString(6);
//                    String key3 = resultSet.getString(7);
//                    long value = resultSet.getLong(8);
//                    stats.add(new PlayerStats(player, new ActionType(plugin, id), Instant.ofEpochMilli(time), value, key1, key2, key3));
//                }
//                return stats;
//            }
//        }
        return null;
    }

    @Override
    public void loadActionStatsTo(Badge<PlayerActionStats> badge) throws SQLException {
        String sql = "SELECT `player`, `plugin`, `type`, `time`, `key1`, `key2`, `key3`, SUM(`value`) FROM `player_actions`";
        List<String> conditions = new ArrayList<>();
        List<StatementArgumentSetter> conditionArgs = new ArrayList<>();

        conditions.add("`player` = ? AND `plugin` = ? AND `type` = ?");
        conditionArgs.add((stmt, idx) -> stmt.setString(idx, badge.getPlayer().toString()));
        conditionArgs.add((stmt, idx) -> stmt.setString(idx, badge.getStats().getSourceActionType().getNamespace()));
        conditionArgs.add((stmt, idx) -> stmt.setString(idx, badge.getStats().getSourceActionType().getKey()));

        Optional.ofNullable(badge.getStartTime()).ifPresent(time -> {
            conditions.add("? <= `time`");
            conditionArgs.add((stmt, idx) -> stmt.setLong(idx, time.toEpochMilli()));
        });

        Optional.ofNullable(badge.getCompleteTime()).ifPresent(time -> {
            conditions.add("`time` <= ?");
            conditionArgs.add((stmt, idx) -> stmt.setLong(idx, time.toEpochMilli()));
        });

        createKeyCondition(badge.getStats().getKeyCondition1(), "key1", conditions, conditionArgs);
        createKeyCondition(badge.getStats().getKeyCondition2(), "key2", conditions, conditionArgs);
        createKeyCondition(badge.getStats().getKeyCondition3(), "key3", conditions, conditionArgs);

        sql += " WHERE " + String.join(" AND ", conditions);

        try (Connection conn = getConnectionTry();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int i = 1;
            for (StatementArgumentSetter setter : conditionArgs) {
                setter.set(stmt, i++);
            }

            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    long value = resultSet.getLong(8);
                    badge.getStats().setValue(badge.getStats().getValue() + value);
                }
            }
        }
    }

    @Override
    public Map<String, Badge.Partial> loadPlayerBadges(UUID player, Set<String> ids) throws SQLException {
        String sql = "SELECT `player`, `id`, `start_time`, `complete_time` FROM `player_badges` WHERE `player` = ?";
        sql += "AND `id` in (" + ids.stream().map(s -> "?").collect(Collectors.joining(",")) + ")";

        try (Connection conn = getConnectionTry();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, player.toString());
            int i = 1;
            for (String id : ids) {
                stmt.setString(++i, id);
            }

            try (ResultSet resultSet = stmt.executeQuery()) {
                Map<String, Badge.Partial> badges = new HashMap<>();
                while (resultSet.next()) {
                    String id = resultSet.getString(2);
                    long v = resultSet.getLong(3);
                    Optional<Long> startTime = v != 0 ? Optional.of(v) : Optional.empty();
                    v = resultSet.getLong(4);
                    Optional<Long> completeTime = v != 0 ? Optional.of(v) : Optional.empty();
                    badges.put(id, new Badge.Partial(player, id, startTime, completeTime));
                }
                return badges;
            }
        }
    }


    private void createKeyCondition(PlayerActionStats.KeyCondition keyCondition, String columnName, List<String> conditions, List<StatementArgumentSetter> conditionArgs) {
        if ("contains".equalsIgnoreCase(keyCondition.getType())) {
            String valList = keyCondition.args().stream().map(s -> "?").collect(Collectors.joining(","));
            conditions.add("`" + columnName + "` in (" + valList + ")");
            for (String arg : keyCondition.args()) {
                conditionArgs.add((stmt, idx) -> stmt.setString(idx, arg));
            }

        } else if ("match".equalsIgnoreCase(keyCondition.getType())) {
            conditions.add("`" + columnName + "` = ?");
            conditionArgs.add((stmt, idx) -> stmt.setString(idx, keyCondition.args().iterator().next()));

        } else if ("null".equalsIgnoreCase(keyCondition.getType())) {
            conditions.add("`" + columnName + "` IS NULL");

        } else if ("not_null".equalsIgnoreCase(keyCondition.getType())) {
            conditions.add("`" + columnName + "` IS NOT NULL");

        } else if (!"any".equalsIgnoreCase(keyCondition.getType())) {
            throw new IllegalArgumentException("Unknown key condition type: " + keyCondition.getType());
        }
    }

    interface StatementArgumentSetter {
        void set(PreparedStatement statement, int index) throws SQLException;
    }

}
