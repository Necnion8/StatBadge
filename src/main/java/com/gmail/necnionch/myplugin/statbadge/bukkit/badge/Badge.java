package com.gmail.necnionch.myplugin.statbadge.bukkit.badge;

import com.gmail.necnionch.myplugin.statbadge.bukkit.config.BadgeEntry;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerActionStats;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerStats;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class Badge<S extends PlayerStats> {

    private final String id;
    private final BadgeEntry config;
    private final UUID player;
    private final S stats;
    private @Nullable Instant startTime;
    private @Nullable Instant completeTime;
    private boolean titleSet;

    public Badge(String id, BadgeEntry config, UUID player, S stats, @Nullable Instant startTime, @Nullable Instant completeTime, boolean titleSet) {
        this.id = id;
        this.config = config;
        this.player = player;
        this.stats = stats;
        this.startTime = startTime;
        this.completeTime = completeTime;
        this.titleSet = titleSet;
    }

    public String getId() {
        return id;
    }

    public UUID getPlayer() {
        return player;
    }

    public S getStats() {
        return stats;
    }

    public @Nullable Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(@Nullable Instant startTime) {
        this.startTime = startTime;
    }

    public @Nullable Instant getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(@Nullable Instant time) {
        this.completeTime = time;
    }

    public boolean isCompleted() {
        return completeTime != null;  // 未来の値だったら未達成と見なす？
    }

    public boolean isTitleSet() {
        return titleSet;
    }

    public void setTitleSet(boolean titleSet) {
        this.titleSet = titleSet;
    }


    public String getName() {
        return config.name();
    }

    public String getDescription() {
        return config.description();
    }

    public @Nullable String getTitle() {
        return config.title();
    }

    public String getTitleOrName() {
        return Optional.ofNullable(config.title()).orElse(getName());
    }

    public Material getIcon() {
        return config.icon().type();
    }

    public @Nullable Object getIconCustomModelData() {
        return config.icon().customModelData();
    }

    public BadgeEntry getConfig() {
        return config;
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=\"" + id + "\""
                + ", stats=\"" + stats.getType().toString() + "\""
                + (stats instanceof PlayerActionStats ? ", action=\"" + ((PlayerActionStats) stats).getSourceActionType() + "\"" : "")
                + ", value=" + stats.getValue()
                + ", targetValue=" + stats.getTargetValue()
                + ", start=" + Optional.ofNullable(startTime).map(Instant::toString).orElse("null")
                + ", complete=" + Optional.ofNullable(completeTime).map(Instant::toString).orElse("null")
                + ", title=" + titleSet
                + "}";
    }

    public record Partial(UUID player, String id, Optional<Long> startTime, Optional<Long> completeTime, boolean titleSet) {
    }

}
