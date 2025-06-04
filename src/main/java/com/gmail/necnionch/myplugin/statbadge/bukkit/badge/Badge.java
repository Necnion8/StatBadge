package com.gmail.necnionch.myplugin.statbadge.bukkit.badge;

import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerActionStats;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerStats;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class Badge<S extends PlayerStats> {

    private final String id;
    private final UUID player;
    private final S stats;
    private final @Nullable Instant startTime;
    private @Nullable Instant completeTime;
    private final long targetValue;

    public Badge(String id, UUID player, S stats, @Nullable Instant startTime, @Nullable Instant completeTime, long targetValue) {
        this.id = id;
        this.player = player;
        this.stats = stats;
        this.startTime = startTime;
        this.completeTime = completeTime;
        this.targetValue = targetValue;
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

    public @Nullable Instant getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(@Nullable Instant time) {
        this.completeTime = time;
    }

    public long getTargetValue() {
        return targetValue;
    }


    public boolean isCompleted() {
        return completeTime != null;  // 未来の値だったら未達成と見なす？
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=\"" + id + "\""
                + ", stats=\"" + stats.getType().toString() + "\""
                + (stats instanceof PlayerActionStats ? ", action=\"" + ((PlayerActionStats) stats).getSourceActionType() + "\"" : "")
                + ", value=" + stats.getValue()
                + ", targetValue=" + targetValue
                + ", start=" + Optional.ofNullable(startTime).map(Instant::toString).orElse("null")
                + ", complete=" + Optional.ofNullable(completeTime).map(Instant::toString).orElse("null")
                + "}";
    }

    public record Partial(UUID player, String id, Optional<Long> startTime, Optional<Long> completeTime) {
    }

}
