package com.gmail.necnionch.myplugin.statbadge.bukkit.command;

import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatBadgePluginInterface;
import net.kyori.adventure.text.Component;

public class BadgesCommand extends Command {

    private final StatBadgePluginInterface plugin;

    public BadgesCommand(StatBadgePluginInterface plugin) {
        super("badges", null);
        this.plugin = plugin;
    }

    @Override
    protected void execute(Context context) {
        context.send(Component.text("Hello World!"));
    }

}
