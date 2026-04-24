package me.marlonreal.allitems.event;

import me.marlonreal.allitems.Allitems;
import me.marlonreal.allitems.FilterMode;
import me.marlonreal.allitems.gui.ItemsGui;
import me.marlonreal.allitems.manager.Manager;
import me.marlonreal.allitems.manager.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;

public class InventoryClickListener implements Listener {

    private final Allitems plugin;
    private final Manager manager;
    private final MessageManager mm;

    public InventoryClickListener(Allitems plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
        this.mm = plugin.getMessageManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!(e.getInventory().getHolder() instanceof ItemsGui)) return;

        e.setCancelled(true);

        if (e.getCurrentItem() == null) return;

        UUID uuid = p.getUniqueId();
        int slot = e.getSlot();
        int currentPage = manager.getPlayerCurrentPage().getOrDefault(uuid, 1);

        switch (slot) {
            case 45 -> // Vorherige Seite
                    p.openInventory(new ItemsGui(plugin, uuid, currentPage - 1).getInventory());

            case 53 -> // Nächste Seite
                    p.openInventory(new ItemsGui(plugin, uuid, currentPage + 1).getInventory());

            case 46 -> { // Filter
                FilterMode current = manager.getPlayerFilterMode().getOrDefault(uuid, FilterMode.ALL);
                manager.getPlayerFilterMode().put(uuid, current.next());
                p.openInventory(new ItemsGui(plugin, uuid, 1).getInventory());
            }

            case 47 -> { // Suche
                p.closeInventory();
                manager.getPlayersInSearchMode().add(uuid);
                p.sendMessage(mm.get("chat.search-enter"));
                p.sendMessage(mm.get("chat.search-hint"));
            }
        }
    }
}