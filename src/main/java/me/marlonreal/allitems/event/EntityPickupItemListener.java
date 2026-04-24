package me.marlonreal.allitems.event;

import me.marlonreal.allitems.manager.Manager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

public class EntityPickupItemListener implements Listener {

    private final Manager manager;

    public EntityPickupItemListener(Manager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p) {
            manager.checkAndAddItem(p, e.getItem().getItemStack().getType());
        }
    }
}