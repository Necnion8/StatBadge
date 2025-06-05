package com.gmail.necnionch.myplugin.statbadge.bukkit.command;

import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class StatBadgeCommand extends Command {

    private final StatManager stats;

    public StatBadgeCommand(StatManager stats) {
        super("statbadge", null);
        this.stats = stats;

        addChild("reload", this::reload);
        addChild("grant", this::grantBadge)
                .argument("player", PLAYER_ARG)
                .argument("badge", grantBadgesArgument);
        addChild("revoke", this::revokeBadge)
                .argument("player", PLAYER_ARG)
                .argument("badge", revokeBadgesArgument);
        addChild("list", this::listBadge)
                .argument("player", PLAYER_ARG);
    }


    private void reload(Context context) {
        context.send(Component.text("reload command!"));
    }

    private void grantBadge(Context context) {
        Player player = context.get(PLAYER_ARG);
        Badge<?> badge = context.get(grantBadgesArgument);

        if (badge.isCompleted()) {
            context.send(Component.text("すでにその称号を持ってるよ！", NamedTextColor.RED));
            return;
        }

        badge.setCompleteTime(Instant.now());
        context.send(Component.text()
                .append(Component.text("称号 ", NamedTextColor.GOLD))
                .append(Component.text(badge.getId(), NamedTextColor.YELLOW))
                .append(Component.text(" を ", NamedTextColor.GOLD))
                .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" に与えました！", NamedTextColor.GOLD)));
    }

    private void revokeBadge(Context context) {
        Player player = context.get(PLAYER_ARG);
        Badge<?> badge = context.get(revokeBadgesArgument);

        if (!badge.isCompleted()) {
            context.send(Component.text("その称号はまだ持ってないよ！", NamedTextColor.RED));
            return;
        }

        badge.setStartTime(Instant.now());
        badge.setCompleteTime(null);
        context.send(Component.text()
                .append(Component.text("称号 ", NamedTextColor.GOLD))
                .append(Component.text(badge.getId(), NamedTextColor.YELLOW))
                .append(Component.text(" を ", NamedTextColor.GOLD))
                .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" に剥奪しました！", NamedTextColor.GOLD)));
    }

    private void listBadge(Context context) {
        Player player = context.getOptional(PLAYER_ARG).or(() -> getPlayerSender(context)).orElse(null);
        if (player == null) {
            context.send(Component.text("プレイヤー名を指定してください", NamedTextColor.RED));
            return;
        }

        List<Badge<?>> badges = new ArrayList<>(stats.getPlayerBadges(player.getUniqueId()));
        Comparator<Badge<?>> comparator = Comparator.comparing(Badge::isCompleted);
        comparator = comparator.reversed();
        comparator = comparator.thenComparing(b -> Optional.ofNullable(b.getCompleteTime()).map(Instant::toEpochMilli).orElse(-1L)).reversed();
        comparator = comparator.thenComparing(b -> Optional.ofNullable(b.getStartTime()).map(Instant::toEpochMilli).orElse(-1L)).reversed();
        badges.sort(comparator);

        for (Badge<?> badge : badges) {
            context.send(Component.text()
                    .append(Component.text("- ", NamedTextColor.GRAY))
                    .append(Component.text(badge.getId(), badge.isCompleted() ? NamedTextColor.GOLD : NamedTextColor.WHITE)));
        }
    }

    @Override
    protected ComponentLike getInvalidArgumentErrorMessage(Context context, InvalidArgumentError error) {
        if (error.getCause() instanceof PlayerNotFound) {
            return Component.text("指定されたプレイヤーが見つかりません", NamedTextColor.RED);
        }
        return super.getInvalidArgumentErrorMessage(context, error);
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
                    .orElseThrow(PlayerNotFound::new);
        }

        @Override
        public Stream<String> completeEntries(Context context, String input) {
            return Bukkit.getOnlinePlayers().stream().map(HumanEntity::getName);
        }
    };


    public static class PlayerNotFound extends InvalidArgumentError.InExecuting {
    }

    private class BadgeArgument extends Argument<Badge<?>> {

        private final Predicate<Badge<?>> filter;

        public BadgeArgument(Predicate<Badge<?>> filter) {
            this.filter = filter;
        }

        @Override
        public Badge<?> execute(Context context, String input) {
            return context.getOptional(PLAYER_ARG)
                    .or(() -> getPlayerSender(context))
                    .flatMap(p -> stats.getPlayerBadges(p.getUniqueId()).stream()
                            .filter(b -> b.getId().equalsIgnoreCase(input))
                            .findFirst())
                    .orElse(null);
        }

        @Override
        public Stream<String> completeEntries(Context context, String input) {
            return context.getOptional(PLAYER_ARG)
                    .or(() -> getPlayerSender(context))
                    .map(p -> stats.getPlayerBadges(p.getUniqueId()).stream()
                            .filter(filter)
                            .map(Badge::getId))
                    .orElse(Stream.empty());
        }
    }

    private final BadgeArgument grantBadgesArgument = new BadgeArgument(b -> !b.isCompleted());
    private final BadgeArgument revokeBadgesArgument = new BadgeArgument(Badge::isCompleted);

}
