package com.gmail.necnionch.myplugin.statbadge.bukkit.stats;

import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatBadgePlugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 統計値を返すクラス<br>
 * 統計を持つ外侮プラグインはこれを返して称号を与える
 */
public abstract class PlayerActionStats extends PlayerStats {

    public static final StatsType STATS_TYPE = new StatsType(JavaPlugin.getProvidingPlugin(StatBadgePlugin.class), "action");
    private final ActionType actionType;

    public PlayerActionStats(UUID playerId, ActionType actionType, long value) {
        super(playerId, STATS_TYPE, value);
        this.actionType = actionType;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public abstract KeyCondition getKeyCondition1();

    public abstract KeyCondition getKeyCondition2();

    public abstract KeyCondition getKeyCondition3();


    public static final class KeyCondition {

        public static final KeyCondition NULL = new KeyCondition("null", Collections.emptySet(), Objects::isNull);
        public static final KeyCondition NOT_NULL = new KeyCondition("not_null", Collections.emptySet(), Objects::nonNull);
        public static final KeyCondition ANY = new KeyCondition("any", Collections.emptySet(), s -> true);

        private final String type;
        private final Collection<String> args;
        private final Predicate<String> test;

        private KeyCondition(String type, Collection<String> args, Predicate<String> test) {
            this.type = type;
            this.args = args;
            this.test = test;
        }

        public String getType() {
            return type;
        }

        public Collection<String> args() {
            return args;
        }

        public boolean test(String value) {
            return test.test(value);
        }

        public static KeyCondition match(String key) {
            return new KeyCondition("match", Collections.singleton(key), s -> s.equals(key));
        }

        public static KeyCondition contains(Collection<String> keys) {
            return new KeyCondition("contains", keys, keys::contains);
        }
    }

}
