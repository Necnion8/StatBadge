package com.gmail.necnionch.myplugin.statbadge.bukkit.config;

import com.gmail.necnionch.myplugin.statbadge.bukkit.database.MySQLDatabase;
import com.gmail.necnionch.myplugin.statbadge.bukkit.database.SQLiteDatabase;
import org.bukkit.Sound;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class StatBadgeConfig extends BukkitConfiguration {

    private boolean debug;
    private @Nullable Sound badgeCompleteSound;

    public StatBadgeConfig(Plugin plugin) {
        super(plugin);
    }

    @Override
    protected boolean onLoaded() {
        debug = config.getBoolean("debug", false);
        badgeCompleteSound = null;

        String tmp = config.getString("badge-complete-sound");
        if (tmp != null) {
            try {
                badgeCompleteSound = Sound.valueOf(tmp.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                log.warning("Unknown sound: " + tmp);
            }
        }

        return true;
    }

    public boolean isDebugEnable() {
        return debug;
    }

    public boolean isShowBadgeCompleteMessage() {
        return config.getBoolean("show-badge-complete-message", true);
    }

    public @Nullable Sound getBadgeCompleteSound() {
        return badgeCompleteSound;
    }

    public boolean isBadgesGuiEnableIconGlowingComplete() {
        return config.getBoolean("badges-gui.enable-icon-glowing-complete", true);
    }

    public String getDatabaseType() {
        return config.getString("database.type", "");
    }

    public MySQLDatabase.Config getMySQLConfig() {
        String username = config.getString("database.mysql.username", "root");
        String password = config.getString("database.mysql.password", "password");
        String address = config.getString("database.mysql.address", "localhost:3306");
        String database = config.getString("database.mysql.database", "statbadge");

        @SuppressWarnings("DataFlowIssue")
        Map<String, Object> options = Optional.ofNullable(config.getConfigurationSection("database.mysql.options"))
                .map(c -> c.getKeys(false).stream().collect(Collectors.toMap(k -> k, c::get)))
                .orElseGet(Collections::emptyMap);

        return new MySQLDatabase.Config(address, database, username, password, options);
    }

    public SQLiteDatabase.Config getSQLiteConfig() {
        String filename = config.getString("database.sqlite.filename", "./plugin.db");

        @SuppressWarnings("DataFlowIssue")
        Map<String, Object> options = Optional.ofNullable(config.getConfigurationSection("database.sqlite.options"))
                .map(c -> c.getKeys(false).stream().collect(Collectors.toMap(k -> k, c::get)))
                .orElseGet(Collections::emptyMap);

        return new SQLiteDatabase.Config(filename, options);
    }

}
