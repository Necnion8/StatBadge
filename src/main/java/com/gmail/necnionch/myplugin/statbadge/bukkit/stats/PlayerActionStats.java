package com.gmail.necnionch.myplugin.statbadge.bukkit.stats;

import org.bukkit.NamespacedKey;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * 統計値を返すクラス<br>
 * 統計を持つ外侮プラグインはこれを返して称号を与える
 */
public abstract class PlayerActionStats extends PlayerStats {

    private final NamespacedKey actionType;

    public PlayerActionStats(UUID playerId, NamespacedKey statsType, NamespacedKey actionType, long value) {
        super(playerId, statsType, value);
        this.actionType = actionType;
    }

    public NamespacedKey getActionType() {
        return actionType;
    }

    public abstract boolean matchAction(PlayerAction action);

    public abstract KeyCondition getKeyCondition1();

    public abstract KeyCondition getKeyCondition2();

    public abstract KeyCondition getKeyCondition3();


    public static final class KeyCondition {

        public static final KeyCondition NULL = new KeyCondition("null", Collections.emptySet());
        public static final KeyCondition NOT_NULL = new KeyCondition("not_null", Collections.emptySet());
        public static final KeyCondition ANY = new KeyCondition("any", Collections.emptySet());

        private final String type;
        private final Collection<String> args;

        private KeyCondition(String type, Collection<String> args) {
            this.type = type;
            this.args = args;
        }

        public String getType() {
            return type;
        }

        public Collection<String> args() {
            return args;
        }

        public static KeyCondition match(String key) {
            return new KeyCondition("match", Collections.singleton(key));
        }

        public static KeyCondition contains(Collection<String> keys) {
            return new KeyCondition("contains", keys);
        }
    }

}
