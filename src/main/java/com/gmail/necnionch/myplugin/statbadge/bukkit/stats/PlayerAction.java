package com.gmail.necnionch.myplugin.statbadge.bukkit.stats;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;

/**
 * プレイヤーの行動を返すクラス<br>
 * 統計をStatBadgeで集計する時に必要
 */
public class PlayerAction {

    private final UUID player;
    private final NamespacedKey type;
    private final Instant time;
    private final @Nullable String key1;
    private final @Nullable String key2;
    private final @Nullable String key3;
    private final long value;

    public PlayerAction(UUID playerId, NamespacedKey actionType, Instant time, @Nullable String key1, @Nullable String key2, @Nullable String key3, long value) {
        this.player = playerId;
        this.type = actionType;
        this.time = time;
        this.key1 = key1;
        this.key2 = key2;
        this.key3 = key3;
        this.value = value;
    }

    public UUID getPlayer() {
        return player;
    }

    public NamespacedKey getType() {
        return type;
    }

    public Instant getTime() {
        return time;
    }

    public @Nullable String getKey1() {
        return key1;
    }

    public @Nullable String getKey2() {
        return key2;
    }

    public @Nullable String getKey3() {
        return key3;
    }

    public long getValue() {
        return value;
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
        private final @Nullable Set<String> keys1;
        private final @Nullable Set<String> keys2;
        private final @Nullable Set<String> keys3;

        public Filter(Set<UUID> players, @Nullable Instant timeFrom, @Nullable Instant timeTo, @Nullable Set<String> keys1, @Nullable Set<String> keys2, @Nullable Set<String> keys3) {
            this.players = players;
            this.allPlayers = ALL_PLAYERS.equals(players);
            this.timeFrom = timeFrom;
            this.timeTo = timeTo;
            this.keys1 = keys1;
            this.keys2 = keys2;
            this.keys3 = keys3;
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

        public @Nullable Set<String> keys1() {
            return keys1;
        }

        public @Nullable Set<String> keys2() {
            return keys2;
        }

        public @Nullable Set<String> keys3() {
            return keys3;
        }

        public static class Builder {

            private Set<UUID> players = ALL_PLAYERS;
            private @Nullable Instant timeFrom;
            private @Nullable Instant timeTo;
            private @Nullable Set<String> keys1;
            private @Nullable Set<String> keys2;
            private @Nullable Set<String> keys3;

            public Builder() {
            }

            public Builder players(Collection<UUID> players) {
                this.players = Set.copyOf(players);
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

            public Builder keys1(String... keys) {
                this.keys1 = Set.of(keys);
                return this;
            }

            public Builder keys1(Set<String> keys) {
                this.keys1 = Set.copyOf(keys);
                return this;
            }

            public Builder keys2(String... keys) {
                this.keys2 = Set.of(keys);
                return this;
            }

            public Builder keys2(Set<String> keys) {
                this.keys2 = Set.copyOf(keys);
                return this;
            }

            public Builder keys3(String... keys) {
                this.keys3 = Set.of(keys);
                return this;
            }

            public Builder keys3(Set<String> keys) {
                this.keys3 = Set.copyOf(keys);
                return this;
            }

            public Filter build() {
                return new Filter(players, timeFrom, timeTo, keys1, keys2, keys3);
            }

        }

    }


}
