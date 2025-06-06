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

    private StatManager getStats() {
        try {
            return plugin.getStats();
        } catch (NullPointerException e) {
            throw new PluginNoLoadedError();
        }
    }

    @Override
    protected void execute(Context context) {
        Player player = (Player) context.getSenderObject();

        List<Badge<?>> badges = getStats().streamPlayerBadges(player.getUniqueId())
                .sorted(BadgeListGUI.DEFAULT_SORT)
                .toList();

        if (badges.isEmpty()) {
            plugin.getLangConfig().send(context, Lang.COMMAND_BADGES_EMPTY);
            return;
        }

        BadgeListGUI.show(plugin, player, badges);
    }

    @Override
    protected boolean processError(Context context, Throwable error) {
        if (error instanceof PluginNoLoadedError || error.getCause() instanceof PluginNoLoadedError) {
            if (context.isExecuted()) {
                plugin.getLangConfig().send(context, Lang.COMMAND_NO_LOADED_DATA);
            }
            return true;
        }
        return super.processError(context, error);
    }


    public static class PluginNoLoadedError extends InvalidArgumentError.InExecuting {
    }

}
