 package com.gmail.necnionch.myplugin.statbadge.bukkit.hook;

 import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.PlayerOnlineTimeManager;
 import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatBadgePlugin;
 import net.lapismc.afkplus.AFKPlus;
 import net.lapismc.afkplus.api.AFKStartEvent;
 import net.lapismc.afkplus.api.AFKStopEvent;
 import org.bukkit.Bukkit;
 import org.bukkit.entity.Player;
 import org.bukkit.event.EventHandler;
 import org.bukkit.event.EventPriority;
 import org.bukkit.event.Listener;
 import org.bukkit.plugin.Plugin;

 import java.util.UUID;
 import java.util.logging.Logger;

public class AFKPlusHook extends PluginHook implements Listener, AFKProvider {

    private final StatBadgePlugin owner;
    private final PlayerOnlineTimeManager onlineTimeManager;
    private AFKPlus afkPlus;

    public AFKPlusHook(StatBadgePlugin owner, String pluginName, Logger logger, PlayerOnlineTimeManager onlineTimeManager) {
        super(pluginName, logger);
        this.owner = owner;
        this.onlineTimeManager = onlineTimeManager;
    }

    @Override
    protected boolean onHook(Plugin plugin) {
        afkPlus = null;
        if (plugin instanceof AFKPlus) {
            afkPlus = (AFKPlus) plugin;
            return true;
        }
        return false;
    }

    @Override
    protected boolean onUnhook() {
        if (afkPlus != null) {
            owner.getServer().getOnlinePlayers().forEach(onlineTimeManager::unsetAFK);
        }
        afkPlus = null;
        return true;
    }

    @Override
    public boolean isAFK(Player player) {
        return afkPlus != null && afkPlus.getPlayer(player).isAFK();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStartAFK(AFKStartEvent event) {
        UUID playerId = event.getPlayer().getUUID();
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            onlineTimeManager.setAFK(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStopAFK(AFKStopEvent event) {
        UUID playerId = event.getPlayer().getUUID();
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            onlineTimeManager.unsetAFK(player);
        }
    }

}
