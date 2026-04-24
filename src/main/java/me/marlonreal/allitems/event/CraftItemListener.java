package me.marlonreal.allitems.event;

import me.marlonreal.allitems.manager.Manager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;

public class CraftItemListener implements Listener {

    private final Manager manager;

    public CraftItemListener(Manager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (e.getWhoClicked() instanceof Player p) {
            manager.checkAndAddItem(p, e.getCurrentItem().getType());
        }
    }
}