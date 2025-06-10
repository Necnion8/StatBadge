package com.gmail.necnionch.myplugin.statbadge.bukkit.badge;

import com.gmail.necnionch.myplugin.statbadge.bukkit.config.Lang;
import com.gmail.necnionch.myplugin.statbadge.bukkit.config.StatBadgeLang;
import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatBadgePlugin;
import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatBadgePluginInterface;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerStats;
import com.gmail.necnionch.myplugin.statbadge.bukkit.util.ItemCustomModelData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.util.*;
import java.util.logging.Level;

public class BadgeListGUI implements Listener {

    public static final Comparator<Badge<?>> DEFAULT_SORT = createDefaultComparator();
    private final LegacyComponentSerializer serializer = StatBadgePlugin.LEGACY_COMPONENT_SERIALIZER;
    private final StatBadgePluginInterface plugin;
    private final Player player;
    private final List<Badge<?>> badges;
    private final boolean pageable;
    private final ItemStack backPageItem;
    private final ItemStack nextPageItem;
    private final int maxPage;
    private final float badgeCompleteProgress;
    private Inventory inventory;
    private int currentPage;

    private static Comparator<Badge<?>> createDefaultComparator() {
        Comparator<Badge<?>> comparator = Comparator.comparing(Badge::isCompleted);
        comparator = comparator.reversed();
        comparator = comparator.thenComparing(b -> Optional.ofNullable(b.getCompleteTime()).map(Instant::toEpochMilli).orElse(-1L)).reversed();
        return comparator.thenComparing(b -> Optional.ofNullable(b.getStartTime()).map(Instant::toEpochMilli).orElse(-1L)).reversed();
    }

    public static void show(StatBadgePluginInterface plugin, Player player, List<Badge<?>> badges) {
        int size = (int) Math.ceil(badges.size() / 9f) * 9;
        boolean pageable = 54 < size;
        size = Math.min(size, 54);
        int maxPage = (int) Math.ceil((float) badges.size() / size) - 1;

        BadgeListGUI ui = new BadgeListGUI(plugin, player, badges, pageable, maxPage, size);
        plugin.getPlugin().getServer().getPluginManager().registerEvents(ui, plugin.getPlugin());
        ui.show();
    }

    private Inventory createInventory(StatBadgePluginInterface plugin, InventoryHolder owner, int page, int maxPage, int size) {
        TextComponent titleComponent = plugin.getLangConfig().format(Lang.UI_BADGE_LIST_TITLE, page + 1, maxPage + 1, badgeCompleteProgress * 100);
        String title = ChatColor.translateAlternateColorCodes('&', serializer.serialize(titleComponent));
        return plugin.getPlugin().getServer().createInventory(owner, Math.max(size, 9), title);
    }

    private static ItemStack createItem(Material icon) {
        ItemStack itemStack = new ItemStack(icon);
        ItemMeta itemMeta = Objects.requireNonNull(itemStack.getItemMeta());
        itemMeta.addItemFlags(ItemFlag.values());
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private BadgeListGUI(StatBadgePluginInterface plugin, Player player, List<Badge<?>> badges, boolean pageable, int maxPage, int size) {
        this.plugin = plugin;
        this.player = player;
        this.pageable = pageable;
        this.badges = badges;
        this.maxPage = maxPage;
        this.badgeCompleteProgress = badges.isEmpty() ? 0f : (float) badges.stream().filter(Badge::isCompleted).count() / badges.size();
        this.inventory = createInventory(plugin, player, currentPage, maxPage, size);

        this.backPageItem = createItem(Material.LIGHT_BLUE_DYE);
        this.nextPageItem = createItem(Material.ORANGE_DYE);
    }

    private void show() {
        fillSlots();
        player.openInventory(inventory);
    }

    public void fillSlots() {
        inventory.clear();
        int count = pageable ? inventory.getSize() - 9 : inventory.getSize();
        int badgeOffset = currentPage * count;
        for (int i = badgeOffset; i < Math.min((currentPage+1) * count, badges.size()); i++) {
            Badge<?> badge = badges.get(i);
            inventory.setItem(i - badgeOffset, createBadgeItem(badge));
        }

        if (pageable) {
            ItemMeta itemMeta = Objects.requireNonNull(backPageItem.getItemMeta());
            itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', String.format(plugin.getLangConfig().get(Lang.UI_BADGE_LIST_BACK_PAGE),currentPage + 1, maxPage + 1)));
            backPageItem.setItemMeta(itemMeta);
            inventory.setItem(inventory.getSize() - 9, backPageItem);
            itemMeta = Objects.requireNonNull(nextPageItem.getItemMeta());
            itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', String.format(plugin.getLangConfig().get(Lang.UI_BADGE_LIST_NEXT_PAGE), currentPage + 1, maxPage + 1)));
            nextPageItem.setItemMeta(itemMeta);
            inventory.setItem(inventory.getSize() - 1, nextPageItem);
        }

    }

    private ItemStack createBadgeItem(Badge<?> badge) {
        ItemStack itemStack = new ItemStack(badge.getIcon());
        ItemMeta itemMeta = Objects.requireNonNull(itemStack.getItemMeta());
        itemMeta.addItemFlags(ItemFlag.values());
        ItemCustomModelData.applyCustomModelData(itemMeta, badge.getIconCustomModelData());

        // badgeId, badgeTitle, badgeName, badgeDesc, targetValue, value, startTime, completeTime, completePercentage
        StatBadgeLang lang = plugin.getLangConfig();
        PlayerStats stats = badge.getStats();
        Object[] args = new Object[] {
                badge.getId(),
                ChatColor.translateAlternateColorCodes('&', badge.getTitle()),
                ChatColor.translateAlternateColorCodes('&', badge.getName()),
                ChatColor.translateAlternateColorCodes('&', badge.getDescription()),
                stats.formatValue(lang, stats.getTargetValue()),
                stats.formatValue(lang, stats.getValue()),
                Optional.ofNullable(badge.getStartTime()).map(t -> lang.formatDateTime(t, true, false)).orElse("?"),
                Optional.ofNullable(badge.getCompleteTime()).map(t -> lang.formatDateTime(t, true, false)).orElse("?"),
                stats.getTargetValue() != 0 ? Math.max(0, (double) stats.getValue() / stats.getTargetValue() * 100) : 0
        };
        // TODO: change selected badge
        Component title = lang.format(badge.isCompleted() ? Lang.UI_BADGE_LIST_ITEM_TITLE_COMPLETED : Lang.UI_BADGE_LIST_ITEM_TITLE, args);
        Component desc = lang.format(badge.isCompleted() ? Lang.UI_BADGE_LIST_ITEM_DESCRIPTION_COMPLETED : Lang.UI_BADGE_LIST_ITEM_DESCRIPTION, args);

        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', serializer.serialize(title)));
        itemMeta.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', serializer.serialize(desc)).split("\n")));

        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public void close(boolean closeInventory) {
        HandlerList.unregisterAll(this);
        if (closeInventory) {
            player.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (player.equals(event.getPlayer()) && !inventory.equals(event.getInventory())) {
            close(false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onClose(InventoryCloseEvent event) {
        if (player.equals(event.getPlayer()) && inventory.equals(event.getInventory())) {
            close(false);
        }
    }

    @EventHandler
    public void onDisablePlugin(PluginDisableEvent event) {
        if (plugin.getPlugin().equals(event.getPlugin()))
            close(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        if (!inventory.equals(event.getInventory()))
            return;

        event.setCancelled(true);
        event.setResult(Event.Result.DENY);

        ItemStack itemStack = event.getCurrentItem();

        if (backPageItem.isSimilar(itemStack)) {
            try {
                int page = Math.max(0, currentPage - 1);
                if (currentPage != page) {
                    currentPage = page;
                    inventory = createInventory(plugin, player, currentPage, maxPage, inventory.getSize());
                    show();
                }
            } catch (Throwable e) {
                plugin.getLogger().log(Level.SEVERE, "Exception in update to back page ui", e);
                close(true);
            }

        } else if (nextPageItem.isSimilar(itemStack)) {
            try {
                int page = Math.min(maxPage, currentPage + 1);
                if (currentPage != page) {
                    currentPage = page;
                    inventory = createInventory(plugin, player, currentPage, maxPage, inventory.getSize());
                    show();
                }
            } catch (Throwable e) {
                plugin.getLogger().log(Level.SEVERE, "Exception in update to next page ui", e);
                close(true);
            }
        }
    }

}
