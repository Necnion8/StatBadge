package com.gmail.necnionch.myplugin.statbadge.bukkit.hook;

import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.Lang;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.StatBadgeLang;
import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatBadgePluginInterface;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlaceholderAPIHook extends PluginHook {

    private final StatBadgePluginInterface owner;
    private final Map<String, ValuePlacer> valuePlacers;
    private @Nullable PlaceholderExpansion expansion;

    public PlaceholderAPIHook(StatBadgePluginInterface owner, String pluginName) {
        super(pluginName, owner.getLogger());
        this.owner = owner;
        StatBadgeLang lang = owner.getLangConfig();
        this.valuePlacers = Stream.of(
                new ValuePlacer("title", true, badge -> ChatColor.translateAlternateColorCodes('&', badge.getTitleOrName())),
                new ValuePlacer("name", true, badge -> ChatColor.translateAlternateColorCodes('&', badge.getName())),
                new ValuePlacer("id", true, Badge::getId),
                new ValuePlacer("current_value", false, badge -> badge.getStats().formatValue(lang, badge.getStats().getValue())),
                new ValuePlacer("current_value_raw", false, badge -> "" + badge.getStats().getValue()),
                new ValuePlacer("current_progress", false, badge -> lang.format(Lang.PLACEHOLDER_VALUE_PROGRESS, badge.getStats().getTargetValue() != 0 ? Math.max(0, (double) badge.getStats().getValue() / badge.getStats().getTargetValue() * 100) : 0).content()),
                new ValuePlacer("target_value", false, badge -> badge.getStats().formatValue(lang, badge.getStats().getTargetValue())),
                new ValuePlacer("target_value_raw", false, badge -> "" + badge.getStats().getTargetValue()),
                new ValuePlacer("start_time", false, badge -> Optional.ofNullable(badge.getStartTime()).map(t -> lang.formatDateTime(t, true, false)).orElse("?")),
                new ValuePlacer("complete_time", false, badge -> Optional.ofNullable(badge.getCompleteTime()).map(t -> lang.formatDateTime(t, true, false)).orElse("?")),
                new ValuePlacer("completed", false, badge -> ChatColor.translateAlternateColorCodes('&', lang.get(badge.isCompleted() ? Lang.PLACEHOLDER_VALUE_COMPLETE : Lang.PLACEHOLDER_VALUE_NOT_COMPLETE)))
        ).collect(Collectors.toMap(p -> p.key, p -> p));
    }

    @Override
    protected boolean onHook(Plugin plugin) {
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
        } catch (ClassNotFoundException e) {
            return false;
        }

        expansion = new PlaceholderExpansion() {

            @Override
            public @NotNull String getIdentifier() {
                return owner.getPlugin().getName().toLowerCase(Locale.ROOT);
            }

            @Override
            public @NotNull String getAuthor() {
                return Optional.ofNullable(owner.getPlugin().getDescription().getAuthors().get(0)).orElse("Necnion8");
            }

            @Override
            public @NotNull String getVersion() {
                return owner.getPlugin().getDescription().getVersion();
            }

            @Override
            public boolean persist() {
                return true;
            }

            @Override
            public @NotNull List<String> getPlaceholders() {
                List<String> values = new ArrayList<>();

                values.add("%" + getIdentifier() + "_badge_count%");
                values.add("%" + getIdentifier() + "_badge_count_completed%");
                values.add("%" + getIdentifier() + "_badge_progress%");

                valuePlacers.keySet().stream()
                        .map(k -> "%" + getIdentifier() + "_" + k + "%")
                        .forEachOrdered(values::add);

                valuePlacers.keySet().stream()
                        .map(k -> "%" + getIdentifier() + "_" + k + "_<badge_id>%")
                        .forEachOrdered(values::add);

                return values;
            }

            @Override
            public String onRequest(OfflinePlayer player, @NotNull String params) {

                switch (params) {
                    case "badge_count":
                        return "" + owner.getStatManager().streamPlayerBadges(player.getUniqueId()).count();
                    case "badge_count_completed":
                        return "" + owner.getStatManager().streamCompletedPlayerBadges(player.getUniqueId()).count();
                    case "badge_progress": {
                        long total = owner.getStatManager().streamPlayerBadges(player.getUniqueId()).count();
                        long completed = owner.getStatManager().streamCompletedPlayerBadges(player.getUniqueId()).count();
                        return owner.getLangConfig().format(Lang.PLACEHOLDER_VALUE_PROGRESS, total == 0 ? 0f : (float) completed / total * 100).content();
                    }
                }

                Badge<?> badge;
                ValuePlacer placer = valuePlacers.get(params);
                if (placer != null) {
                    badge = owner.getStatManager().getSelectBadgeTitle(player.getUniqueId());
                    if (badge != null)
                        return placer.processor.apply(badge);
                    if (placer.notSetLabel)
                        return ChatColor.translateAlternateColorCodes('&', owner.getLangConfig().get(Lang.PLACEHOLDER_NOT_SET_BADGE));
                    return "";
                }

                for (Map.Entry<String, ValuePlacer> e : valuePlacers.entrySet()) {
                    if (params.startsWith(e.getKey() + "_")) {
                        String badgeId = params.substring(e.getKey().length() + 1);
                        badge = owner.getStatManager().getPlayerBadge(player.getUniqueId(), badgeId).orElse(null);
                        if (badge != null)
                            return e.getValue().processor.apply(badge);
                        if (e.getValue().notSetLabel)
                            return ChatColor.translateAlternateColorCodes('&', owner.getLangConfig().get(Lang.PLACEHOLDER_NOT_SET_BADGE));
                        return "";
                    }
                }
                return "";
            }
        };

        return expansion.register();
    }

    @Override
    protected boolean onUnhook() {
        if (expansion != null)
            expansion.unregister();
        return true;
    }


    private record ValuePlacer(String key, boolean notSetLabel, Function<Badge<?>, String> processor) {
    }

}
