package com.gmail.necnionch.myplugin.statbadge.bukkit.stats;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public class ActionType {

    private final NamespacedKey type;

    public ActionType(NamespacedKey type) {
        this.type = type;
    }

    public ActionType(Plugin plugin, String key) {
        this(new NamespacedKey(plugin, key));
    }

    public String getNamespace() {
        return type.getNamespace();
    }

    public String getKey() {
        return type.getKey();
    }

    @Override
    public String toString() {
        return type.toString();
    }

    @Override
    public boolean equals(Object obj) {
        System.out.println("this: " + this + " vs obj: " + obj);
        return super.equals(obj) || (obj instanceof ActionType && type.equals(((ActionType) obj).type));
    }

}
