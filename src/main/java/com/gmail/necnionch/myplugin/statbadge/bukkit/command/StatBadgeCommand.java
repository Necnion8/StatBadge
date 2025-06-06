package com.gmail.necnionch.myplugin.statbadge.bukkit.command;

import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.Lang;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.StatBadgeLang;
import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatBadgePlugin;
import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatBadgePluginInterface;
import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StatBadgeCommand extends Command {

    private final StatBadgePluginInterface plugin;
    private final LegacyComponentSerializer serializer = StatBadgePlugin.LEGACY_COMPONENT_SERIALIZER;

    public StatBadgeCommand(StatBadgePluginInterface plugin) {
        super("statbadge", null);
        this.plugin = plugin;

        addChild("list", this::listBadge)
                .argument("player", PLAYER_ARG);
        addChild("grant", this::grantBadge)
                .argument("player", PLAYER_ARG)
                .argument("badge", grantBadgesArgument);
        addChild("revoke", this::revokeBadge)
                .argument("player", PLAYER_ARG)
                .argument("badge", revokeBadgesArgument);
        addChild("reload", this::reload);
    }

    private StatManager getStats() {
        try {
            return plugin.getStats();
        } catch (NullPointerException e) {
            throw new PluginNoLoadedError();
        }
    }

    private void sendLang(Command.Context context, Lang lang, Object... args) {
        plugin.getLangConfig().send(context, lang, args);
    }

    private ComponentLike formatBadgeName(Badge<?> badge) {
        StatBadgeLang lang = plugin.getLangConfig();

        String startTime = Optional.ofNullable(badge.getStartTime())
                .map(t -> lang.formatDateTime(t, true, false))
                .orElse("?");
        String completeTime = Optional.ofNullable(badge.getCompleteTime())
                .map(t -> lang.formatDateTime(t, true, false))
                .orElse("?");

        String value = badge.getStats().formatValue(lang, badge.getStats().getValue());
        String targetValue = badge.getStats().formatValue(lang, badge.getTargetValue());
        double progress = 0;
        if (badge.getTargetValue() != 0) {
            progress = (double) badge.getStats().getValue() / badge.getTargetValue() * 100;
            progress = Math.max(0, progress);
        }

        TextComponent.Builder hoverText = Component.text()
                .append(lang.format(Lang.COMMAND_BADGE_LIST_ITEM_TITLE))
                .append(serializer.deserialize(badge.getName()).colorIfAbsent(NamedTextColor.GOLD))
                .append(lang.format(badge.isCompleted() ? Lang.COMMAND_BADGE_LIST_ITEM_TITLE_COMPLETE : Lang.COMMAND_BADGE_LIST_ITEM_TITLE_NONE))
                .appendNewline()
                .append(Component.text(badge.getId(), NamedTextColor.GRAY))
                .appendNewline()
                .appendNewline()
                .append(lang.format(Lang.COMMAND_BADGE_LIST_ITEM_STATS))
                .appendNewline()
                .append(lang.format(Lang.COMMAND_BADGE_LIST_ITEM_STATS_DATE, startTime, completeTime))
                .appendNewline()
                .append(lang.format(badge.isCompleted() ? Lang.COMMAND_BADGE_LIST_ITEM_STATS_VALUE_COMPLETED : Lang.COMMAND_BADGE_LIST_ITEM_STATS_VALUE, value, targetValue, progress))
                .appendNewline()
                .appendNewline()
                .append(lang.format(Lang.COMMAND_BADGE_LIST_ITEM_NAME))
                .append(serializer.deserialize(badge.getTitle()))
                .appendNewline()
                .appendNewline()
                .append(lang.format(Lang.COMMAND_BADGE_LIST_ITEM_DESCRIPTION))
                .appendNewline()
                .append(serializer.deserialize("  " + badge.getDescription().replace("\n", "\n  ")));

        TextComponent name;
        if (badge.getName().isEmpty()) {
            name = Component.text(badge.getId());
        } else {
            name = serializer.deserialize(badge.getName());
        }
        return name.color(badge.isCompleted() ? NamedTextColor.GOLD : NamedTextColor.GRAY).hoverEvent(HoverEvent.showText(hoverText));
    }


    private void reload(Context context) {
        context.send(Component.text("reload command!"));
    }

    private void grantBadge(Context context) {
        Player player = context.get(PLAYER_ARG);
        Badge<?> badge = context.get(grantBadgesArgument);

        if (badge.isCompleted()) {
            sendLang(context, Lang.COMMAND_BADGE_GRANT_ALREADY, badge.getId(), player.getName());

        } else if (getStats().grantBadge(player, badge.getId()) == null) {
            sendLang(context, Lang.COMMAND_BADGE_GRANT_ERROR, badge.getId(), player.getName());

        } else {
            sendLang(context, Lang.COMMAND_BADGE_GRANT, badge.getId(), player.getName());
        }
    }

    private void revokeBadge(Context context) {
        Player player = context.get(PLAYER_ARG);
        Badge<?> badge = context.get(revokeBadgesArgument);

        if (!badge.isCompleted()) {
            sendLang(context, Lang.COMMAND_BADGE_REVOKE_ALREADY, badge.getId(), player.getName());

        } else if (getStats().revokeBadge(player, badge.getId()) == null) {
            sendLang(context, Lang.COMMAND_BADGE_REVOKE_ERROR, badge.getId(), player.getName());

        } else {
            sendLang(context, Lang.COMMAND_BADGE_REVOKE, badge.getId(), player.getName());
        }
    }

    private void listBadge(Context context) {
        Player player = context.getOptional(PLAYER_ARG).or(() -> getPlayerSender(context)).orElse(null);
        if (player == null) {
            sendLang(context, Lang.COMMAND_PLAYER_ARGUMENT);
            return;
        }

        List<Badge<?>> badges = getStats().streamPlayerBadges(player.getUniqueId()).collect(Collectors.toCollection(ArrayList::new));
        Comparator<Badge<?>> comparator = Comparator.comparing(Badge::isCompleted);
        comparator = comparator.reversed();
        comparator = comparator.thenComparing(b -> Optional.ofNullable(b.getCompleteTime()).map(Instant::toEpochMilli).orElse(-1L)).reversed();
        comparator = comparator.thenComparing(b -> Optional.ofNullable(b.getStartTime()).map(Instant::toEpochMilli).orElse(-1L)).reversed();
        badges.sort(comparator);

        long completed = badges.stream().filter(Badge::isCompleted).count();
        sendLang(context, Lang.COMMAND_BADGE_LIST, completed, badges.size(), player.getName());
        context.send(Component.join(
                JoinConfiguration.separator(Component.text(", ", NamedTextColor.GRAY)),
                badges.stream().map(this::formatBadgeName).toList()
        ));
    }


    @Override
    protected ComponentLike getInvalidArgumentErrorMessage(Context context, InvalidArgumentError error) {
        if (error.getCause() instanceof PlayerNotFoundError) {
            return plugin.getLangConfig().format(Lang.COMMAND_UNKNOWN_PLAYER);
        }
        return super.getInvalidArgumentErrorMessage(context, error);
    }

    @Override
    protected boolean processError(Context context, Throwable error) {
        if (error.getCause() instanceof UnknownBadgeError) {
            if (context.isExecuted()) {
                sendLang(context, Lang.COMMAND_UNKNOWN_BADGE, ((UnknownBadgeError) error.getCause()).input);
            }
            return true;
        } else if (error instanceof  PluginNoLoadedError || error.getCause() instanceof PluginNoLoadedError) {
            if (context.isExecuted()) {
                sendLang(context, Lang.COMMAND_NO_LOADED_DATA);
            }
            return true;
        }
        return super.processError(context, error);
    }


    private static Optional<Player> getPlayerSender(Context context) {
        if (context.getSenderObject() instanceof Player player)
            return Optional.of(player);
        return Optional.empty();
    }

    private final static Argument<Player> PLAYER_ARG = new Argument<>() {
        @Override
        public Player execute(Context context, String input) {
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(input))
                    .findFirst()
                    .orElseThrow(PlayerNotFoundError::new);
        }

        @Override
        public Stream<String> completeEntries(Context context, String input) {
            return Bukkit.getOnlinePlayers().stream().map(HumanEntity::getName);
        }
    };


    private static class PlayerNotFoundError extends InvalidArgumentError.InExecuting {
    }

    private class BadgeArgument extends Argument<Badge<?>> {

        private final Predicate<Badge<?>> completeFilter;

        public BadgeArgument(Predicate<Badge<?>> completeFilter) {
            this.completeFilter = completeFilter;
        }

        @Override
        public Badge<?> execute(Context context, String input) {
            StatManager stats = getStats();
            return context.getOptional(PLAYER_ARG)
                    .or(() -> getPlayerSender(context))
                    .flatMap(p -> stats.streamPlayerBadges(p.getUniqueId())
                            .filter(b -> b.getId().equalsIgnoreCase(input))
                            .findFirst())
                    .orElseThrow(() -> new UnknownBadgeError(input));
        }

        @Override
        public Stream<String> completeEntries(Context context, String input) {
            StatManager stats = getStats();
            return context.getOptional(PLAYER_ARG)
                    .or(() -> getPlayerSender(context))
                    .map(p -> stats.streamPlayerBadges(p.getUniqueId())
                            .filter(completeFilter)
                            .map(Badge::getId))
                    .orElse(Stream.empty());
        }
    }

    private static class UnknownBadgeError extends InvalidArgumentError.InExecuting {

        private final String input;

        public UnknownBadgeError(String input) {
            this.input = input;
        }

        public String getInput() {
            return input;
        }
    }

    private final BadgeArgument grantBadgesArgument = new BadgeArgument(b -> !b.isCompleted());
    private final BadgeArgument revokeBadgesArgument = new BadgeArgument(Badge::isCompleted);

    private static class PluginNoLoadedError extends InvalidArgumentError.InExecuting {
    }

}
