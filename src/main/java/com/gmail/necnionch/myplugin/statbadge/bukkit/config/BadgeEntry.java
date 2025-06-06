package com.gmail.necnionch.myplugin.statbadge.bukkit.config;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public record BadgeEntry(
        String id,
        String statsType,
        long statsValue,
        String name,
        String description,
        String title,
        Material icon,
        @Nullable Object iconCustomModelData
) {}
