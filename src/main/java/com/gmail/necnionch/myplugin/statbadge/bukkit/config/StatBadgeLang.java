package com.gmail.necnionch.myplugin.statbadge.bukkit.config;

import com.gmail.necnionch.myplugin.statbadge.bukkit.command.Command;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.Set;

public class StatBadgeLang extends BukkitConfiguration {

    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacy('&');

    public StatBadgeLang(Plugin plugin) {
        super(plugin, "lang.yml", "dummy.yml");
    }

    public String get(Lang lang) {
        return config.getString(lang.getKey(), lang.getDefaultText());
    }

    public TextComponent format(Lang lang, Object... args) {
        return serializer.deserialize(String.format(get(lang), args));
    }

    public void send(Command.Context context, Lang lang, Object... args) {
        context.send(format(lang, args));
    }


    @Override
    protected boolean onLoaded() {
        if (fillLangValues())
            save();
        return true;
    }

    @Override
    protected void generateNewFile(Path path) {
        // no defaults
    }

    public boolean fillLangValues() {
        Set<String> existsKeys = config.getKeys(false);

        boolean updated = false;
        for (Lang lang : Lang.values()) {
            if (existsKeys.contains(lang.getKey()))
                continue;

            config.set(lang.getKey(), lang.getDefaultText());
            updated = true;
        }
        return updated;
    }


    public String formatDateTime(Instant instant, boolean skipYears, boolean replaceToday) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate now = LocalDate.now(zone);
        LocalDate date = LocalDate.ofInstant(instant, zone);

        if (replaceToday && now.isEqual(date)) {
            return format(Lang.UI_TODAY).content();
        } else if (skipYears && now.getYear() == date.getYear()) {
            return date.format(DATE_WITHOUT_YEAR_FORMATTER);
        }
        return date.format(DATE_FORMATTER);
    }

    public String formatDuration(Duration duration) {
        long totalSeconds = Math.abs(duration.getSeconds());

        long seconds = totalSeconds % 60;
        long totalMinutes = totalSeconds / 60;
        long minutes = totalMinutes % 60;
        long totalHours = totalMinutes / 60;
        long hours = totalHours % 24;
        long days = totalHours / 24;

        String formatted;
        if (totalSeconds < 60) {
            formatted = format(Lang.UI_VALUE_SECONDS, seconds, minutes, hours, days).content();
        } else if (totalSeconds < 3600) {
            formatted = format(Lang.UI_VALUE_MINUTES, seconds, minutes, hours, days).content();
        } else if (totalSeconds < 86400) {
            formatted = format(Lang.UI_VALUE_HOURS, seconds, minutes, hours, days).content();
        } else {
            formatted = format(Lang.UI_VALUE_DAYS, seconds, minutes, hours, days).content();
        }

        return duration.isNegative() ? "-" + formatted : formatted;
    }


    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4, 10,SignStyle.EXCEEDS_PAD)
            .appendLiteral('/')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('/')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .toFormatter();

    private static final DateTimeFormatter DATE_WITHOUT_YEAR_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('/')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .toFormatter();
}