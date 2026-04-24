package me.marlonreal.allitems.gui;

import me.marlonreal.allitems.Allitems;
import me.marlonreal.allitems.FilterMode;
import me.marlonreal.allitems.manager.Manager;
import me.marlonreal.allitems.manager.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ItemsGui implements InventoryHolder {

    private final Allitems plugin;
    private final Manager manager;
    private final MessageManager mm;
    private final UUID uuid;
    private int page;

    public ItemsGui(Allitems plugin, UUID uuid, int page) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
        this.mm = plugin.getMessageManager();
        this.uuid = uuid;
        this.page = page;
    }

    @Override
    public @NotNull Inventory getInventory() {
        Set<Material> collected = manager.getEffectiveCollectedItems(uuid);
        List<Material> filtered = manager.getFilteredItems(uuid);

        int maxPages = Math.max(1, (int) Math.ceil(filtered.size() / 45.0));
        if (page < 1) page = 1;
        if (page > maxPages) page = maxPages;

        // Aktuelle Seite speichern – ClickListener liest sie von hier
        manager.getPlayerCurrentPage().put(uuid, page);

        Inventory inv = Bukkit.createInventory(this, 54,
                mm.get("gui.title", MessageManager.ph("page", String.valueOf(page))));

        int startIndex = (page - 1) * 45;
        int endIndex = Math.min(startIndex + 45, filtered.size());

        for (int i = startIndex; i < endIndex; i++) {
            Material mat = filtered.get(i);
            ItemStack item;

            if (collected.contains(mat)) {
                item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(mm.get("gui.item-collected-name",
                            MessageManager.ph("item", manager.formatItemName(mat))));
                    meta.setLore(List.of(mm.get("gui.item-collected-lore-status")));
                    item.setItemMeta(meta);
                }
            } else {
                item = new ItemStack(Material.BARRIER);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(mm.get("gui.item-missing-name",
                            MessageManager.ph("item", manager.formatItemName(mat))));
                    meta.setLore(List.of(
                            mm.get("gui.item-missing-lore-status"),
                            mm.get("gui.item-missing-lore-material",
                                    MessageManager.ph("material", mat.name()))
                    ));
                    item.setItemMeta(meta);
                }
            }

            inv.setItem(i - startIndex, item);
        }

        // Navigation
        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            if (meta != null) { meta.setDisplayName(mm.get("gui.prev-page")); prev.setItemMeta(meta); }
            inv.setItem(45, prev);
        }
        if (page < maxPages) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            if (meta != null) { meta.setDisplayName(mm.get("gui.next-page")); next.setItemMeta(meta); }
            inv.setItem(53, next);
        }

        // Filter (Slot 46)
        FilterMode mode = manager.getPlayerFilterMode().getOrDefault(uuid, FilterMode.ALL);
        inv.setItem(46, buildFilterItem(mode));

        // Suche (Slot 47)
        String search = manager.getPlayerSearchQuery().getOrDefault(uuid, "");
        ItemStack searchItem = new ItemStack(Material.COMPASS);
        ItemMeta searchMeta = searchItem.getItemMeta();
        if (searchMeta != null) {
            searchMeta.setDisplayName(mm.get("gui.search-name"));
            List<String> lore = new ArrayList<>();
            lore.add(mm.get("gui.search-lore-click"));
            if (!search.isEmpty()) {
                lore.add("");
                lore.add(mm.get("gui.search-lore-current", MessageManager.ph("search", search)));
                lore.add(mm.get("gui.search-lore-found",
                        MessageManager.ph("found", String.valueOf(filtered.size()))));
            }
            searchMeta.setLore(lore);
            searchItem.setItemMeta(searchMeta);
        }
        inv.setItem(47, searchItem);

        // Info (Slot 49)
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(mm.get("gui.info-name"));
            String percent = String.format("%.2f",
                    collected.size() * 100.0 / manager.getRequiredItems().size());
            infoMeta.setLore(List.of(
                    mm.get("gui.info-lore-collected", MessageManager.ph(
                            "collected", String.valueOf(collected.size()),
                            "total", String.valueOf(manager.getRequiredItems().size()))),
                    mm.get("gui.info-lore-percent", MessageManager.ph("percent", percent)),
                    mm.get("gui.info-lore-displayed",
                            MessageManager.ph("found", String.valueOf(filtered.size()))),
                    mm.get("gui.info-lore-page", MessageManager.ph(
                            "page", String.valueOf(page),
                            "max_pages", String.valueOf(maxPages)))
            ));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(49, info);

        return inv;
    }

    private ItemStack buildFilterItem(FilterMode mode) {
        String modeDisplay = switch (mode) {
            case ALL -> mm.get("gui.filter-mode-all");
            case COLLECTED -> mm.get("gui.filter-mode-collected");
            case MISSING -> mm.get("gui.filter-mode-missing");
        };

        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(mm.get("gui.filter-name", MessageManager.ph("mode", modeDisplay)));
            meta.setLore(List.of(
                    mm.get("gui.filter-lore-click"),
                    "",
                    mm.get(mode == FilterMode.ALL       ? "gui.filter-lore-all-active"       : "gui.filter-lore-all"),
                    mm.get(mode == FilterMode.COLLECTED  ? "gui.filter-lore-collected-active" : "gui.filter-lore-collected"),
                    mm.get(mode == FilterMode.MISSING    ? "gui.filter-lore-missing-active"   : "gui.filter-lore-missing")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
}