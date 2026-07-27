package me.marlonreal.allitems.event;

import me.marlonreal.allitems.Allitems;
import me.marlonreal.allitems.gui.ItemsGui;
import me.marlonreal.allitems.manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class InventoryObtainListener implements Listener {

    private final Allitems plugin;
    private final Manager manager;

    public InventoryObtainListener(Allitems plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getTopInventory().getHolder() instanceof ItemsGui) return;

        Inventory clicked = event.getClickedInventory();
        if (clicked == null || clicked.equals(player.getInventory())) return;
        if (!movesItemToPlayer(event.getAction())) return;

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;

        Material material = current.getType();
        int amountBefore = countMaterial(player, material);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (countMaterial(player, material) > amountBefore) {
                manager.checkAndAddItem(player, material);
            }
        });
    }

    private int countMaterial(Player player, Material material) {
        int amount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                amount += item.getAmount();
            }
        }

        ItemStack cursor = player.getItemOnCursor();
        if (cursor.getType() == material) {
            amount += cursor.getAmount();
        }
        return amount;
    }

    private boolean movesItemToPlayer(InventoryAction action) {
        return switch (action) {
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME,
                    MOVE_TO_OTHER_INVENTORY, HOTBAR_SWAP, HOTBAR_MOVE_AND_READD,
                    SWAP_WITH_CURSOR, COLLECT_TO_CURSOR -> true;
            default -> false;
        };
    }
}