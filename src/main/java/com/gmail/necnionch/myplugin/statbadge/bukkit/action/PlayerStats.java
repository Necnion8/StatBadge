package com.gmail.necnionch.myplugin.statbadge.bukkit.action;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record PlayerStats(
        UUID player,
        ActionType type,
        Instant time,
        long value,
        @Nullable String key1,
        @Nullable String key2,
        @Nullable String key3
) {
}
