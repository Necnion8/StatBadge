package com.gmail.necnionch.myplugin.statbadge.bukkit.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Objects;

public final class ItemCustomModelData {

    private static @Nullable CustomModelDataAccessor accessor;

    public static void init() {
        try {
            accessor = new Version_v1_14();
        } catch (ReflectiveOperationException e) {
            accessor = new Version_None();
        }
    }

    public static void clear() {
        accessor = null;
    }


    private static CustomModelDataAccessor getAccessor() {
        return Objects.requireNonNull(accessor, "CustomModelDataAccessor not initialized");
    }


    public static Object getCustomModelData(ItemMeta itemMeta) {
        try {
            return getAccessor().getCustomModelData(itemMeta);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public static void applyCustomModelData(ItemMeta itemMeta, Object object) {
        try {
            getAccessor().applyCustomModelData(itemMeta, object);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static Object serializeCustomModelData(ConfigurationSection config, String key) {
        try {
            return getAccessor().serializeCustomModelData(config, key);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }


    public interface CustomModelDataAccessor {
        Object serializeCustomModelData(ConfigurationSection config, String key) throws ReflectiveOperationException;
        Object getCustomModelData(ItemMeta itemMeta) throws ReflectiveOperationException;
        void applyCustomModelData(ItemMeta itemMeta, Object object) throws ReflectiveOperationException;
    }

    public static class Version_None implements CustomModelDataAccessor {
        @Override
        public Object getCustomModelData(ItemMeta itemMeta) {
            return null;
        }

        @Override
        public Object serializeCustomModelData(ConfigurationSection config, String key) {
            return null;
        }

        @Override
        public void applyCustomModelData(ItemMeta itemMeta, Object object) {
        }
    }

    public static class Version_v1_14 implements CustomModelDataAccessor {

        private final Method fieldGetCustomModelData;
        private final Method fieldHasCustomModelData;
        private final Method fieldSetCustomModelData;

        @SuppressWarnings("JavaReflectionMemberAccess")
        public Version_v1_14() throws ReflectiveOperationException {
            this.fieldGetCustomModelData = ItemMeta.class.getMethod("getCustomModelData");
            this.fieldHasCustomModelData = ItemMeta.class.getMethod("hasCustomModelData");
            this.fieldSetCustomModelData = ItemMeta.class.getMethod("setCustomModelData", Integer.class);
        }

        @Override
        public Object getCustomModelData(ItemMeta itemMeta) throws ReflectiveOperationException {
            if (Boolean.TRUE.equals(fieldHasCustomModelData.invoke(itemMeta))) {
                return fieldGetCustomModelData.invoke(itemMeta);
            }
            return null;
        }

        @Override
        public Object serializeCustomModelData(ConfigurationSection config, String key) {
            return config.get(key) != null ? config.getInt(key) : null;
        }

        @Override
        public void applyCustomModelData(ItemMeta itemMeta, Object object) throws ReflectiveOperationException {
            if (object == null || object instanceof Integer) {
                //noinspection RedundantCast
                fieldSetCustomModelData.invoke(itemMeta, (Integer) object);
            }
        }
    }

}
