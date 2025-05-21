package com.gmail.necnionch.myplugin.statbadge.bukkit.badge;

import com.gmail.necnionch.myplugin.statbadge.bukkit.action.ActionType;
import com.gmail.necnionch.myplugin.statbadge.bukkit.action.PlayerAction;
import com.gmail.necnionch.myplugin.statbadge.bukkit.action.PlayerStats;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Badge {

    private final UUID player;
    private final ActionType actionType;
    private final @Nullable Instant startTime;
    private @Nullable Instant completeTime;
    private final @Nullable String actionKey1;
    private final @Nullable String actionKey2;
    private final @Nullable String actionKey3;
    private final long actionTargetValue;
    private long actionCurrentValue;

    public Badge(UUID player, ActionType actionType, @Nullable Instant startTime, @Nullable Instant completeTime, @Nullable String actionKey1, @Nullable String actionKey2, @Nullable String actionKey3, long actionTargetValue, long actionCurrentValue) {
        this.player = player;
        this.actionType = actionType;
        this.startTime = startTime;
        this.completeTime = completeTime;
        this.actionKey1 = actionKey1;
        this.actionKey2 = actionKey2;
        this.actionKey3 = actionKey3;
        this.actionTargetValue = actionTargetValue;
        this.actionCurrentValue = actionCurrentValue;
    }

    public UUID getPlayer() {
        return player;
    }

    public ActionType getActionType() {
        return actionType;
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

    public @Nullable String getActionKey1() {
        return actionKey1;
    }

    public @Nullable String getActionKey2() {
        return actionKey2;
    }

    public @Nullable String getActionKey3() {
        return actionKey3;
    }

    public long getActionTargetValue() {
        return actionTargetValue;
    }

    public long getActionCurrentValue() {
        return actionCurrentValue;
    }

    public void setActionCurrentValue(long value) {
        this.actionCurrentValue = value;
    }


    public boolean isCompleted() {
        return completeTime != null;  // 未来の値だったら未達成と見なす？
    }

    public boolean isMatchAction(PlayerAction action) {
        if (player.equals(action.player()) && actionType.equals(action.type()))
            return (Objects.requireNonNull(actionKey1).equals(action.key1()) && Objects.requireNonNull(actionKey2).equals(action.key2()) && Objects.requireNonNull(actionKey3).equals(action.key3()));
        return false;
    }

    public boolean isMatchStats(PlayerStats stats) {
        if (player.equals(stats.player()) && actionType.equals(stats.type()))
            return (Objects.requireNonNull(actionKey1).equals(stats.key1()) && Objects.requireNonNull(actionKey2).equals(stats.key2()) && Objects.requireNonNull(actionKey3).equals(stats.key3()));
        return false;
    }

}
