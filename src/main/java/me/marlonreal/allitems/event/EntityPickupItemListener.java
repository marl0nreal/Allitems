package me.marlonreal.allitems.event;

import me.marlonreal.allitems.manager.Manager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

public final class EntityPickupItemListener implements Listener {

    private final Manager manager;

    public EntityPickupItemListener(Manager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            manager.checkAndAddItem(player, event.getItem().getItemStack().getType());
        }
    }
}