package com.gmail.necnionch.myplugin.statbadge.bukkit.action;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;

public class ActionRecord {

    private final UUID player;
    private final String plugin;
    private final String id;
    private final Instant time;
    private final long value;

    public ActionRecord(UUID player, String plugin, String id, Instant time, long value) {
        this.player = player;
        this.plugin = plugin;
        this.id = id;
        this.time = time;
        this.value = value;
    }

    public UUID getPlayer() {
        return player;
    }

    public String getPlugin() {
        return plugin;
    }

    public String getId() {
        return id;
    }

    public Instant getTime() {
        return time;
    }

    public long getValue() {
        return value;
    }


    public static class Stats {

        private final UUID player;
        private final String plugin;
        private final String id;
        private final @Nullable Instant timeFrom;
        private final @Nullable Instant timeTo;
        private final long totalValue;

        public Stats(UUID player, String plugin, String id, @Nullable Instant timeFrom, @Nullable Instant timeTo, long totalValue) {
            this.player = player;
            this.plugin = plugin;
            this.id = id;
            this.timeFrom = timeFrom;
            this.timeTo = timeTo;
            this.totalValue = totalValue;
        }

        public UUID getPlayer() {
            return player;
        }

        public String getPlugin() {
            return plugin;
        }

        public String getId() {
            return id;
        }

        public @Nullable Instant getTimeFrom() {
            return timeFrom;
        }

        public @Nullable Instant getTimeTo() {
            return timeTo;
        }

        public long getTotalValue() {
            return totalValue;
        }

    }


    public static Filter.Builder filter() {
        return new Filter.Builder();
    }

    public static class Filter {

        private static final Set<UUID> ALL_PLAYERS = Collections.unmodifiableSet(new HashSet<>());
        private final Set<UUID> players;
        private final boolean allPlayers;
        private final @Nullable Instant timeFrom;
        private final @Nullable Instant timeTo;

        public Filter(Set<UUID> players, @Nullable Instant timeFrom, @Nullable Instant timeTo) {
            this.players = Collections.unmodifiableSet(players);
            this.allPlayers = ALL_PLAYERS.equals(players);
            this.timeFrom = timeFrom;
            this.timeTo = timeTo;
        }

        public Set<UUID> players() {
            return players;
        }

        public boolean isAllPlayers() {
            return allPlayers;
        }

        public @Nullable Instant timeFrom() {
            return timeFrom;
        }

        public @Nullable Instant timeTo() {
            return timeTo;
        }

        public static class Builder {

            private Set<UUID> players = ALL_PLAYERS;
            private @Nullable Instant timeFrom;
            private @Nullable Instant timeTo;

            public Builder() {
            }

            public Builder players(Collection<UUID> players) {
                this.players = new HashSet<>(players);
                return this;
            }

            public Builder allPlayers() {
                players = ALL_PLAYERS;
                return this;
            }

            public Builder timeFrom(@Nullable Instant time) {
                this.timeFrom = time;
                return this;
            }

            public Builder timeTo(@Nullable Instant time) {
                this.timeTo = time;
                return this;
            }

            public Filter build() {
                return new Filter(players, timeFrom, timeTo);
            }

        }

    }

}
