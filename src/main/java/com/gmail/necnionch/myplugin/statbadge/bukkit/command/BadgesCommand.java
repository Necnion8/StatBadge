package com.gmail.necnionch.myplugin.statbadge.bukkit.command;

import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.Badge;
import com.gmail.necnionch.myplugin.statbadge.bukkit.badge.BadgeListGUI;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.Lang;
import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatBadgePluginInterface;
import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatManager;
import org.bukkit.entity.Player;

import java.util.List;

public class BadgesCommand extends Command {

    private final StatBadgePluginInterface plugin;

    public BadgesCommand(StatBadgePluginInterface plugin) {
        super("badges", null);
        playerOnly(true);
        this.plugin = plugin;
    }


    @Override
    protected void execute(Context context) {
        Player player = (Player) context.getSenderObject();

        StatManager stats;
        try {
            stats = plugin.getStats();
        } catch (RuntimeException e) {
            plugin.getLangConfig().send(context, Lang.COMMAND_NO_LOADED_DATA);
            return;
        }

        List<Badge<?>> badges = stats.streamPlayerBadges(player.getUniqueId())
                .sorted(BadgeListGUI.DEFAULT_SORT)
                .toList();

        if (badges.isEmpty()) {
            plugin.getLangConfig().send(context, Lang.COMMAND_BADGES_EMPTY);
            return;
        }

        BadgeListGUI.show(plugin, player, badges);
    }

}
