package me.marlonreal.allitems.event;

import me.marlonreal.allitems.Allitems;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerConnectionListener implements Listener {

    private final Allitems plugin;

    public PlayerConnectionListener(Allitems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plugin.getBossBarManager().update(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayerDataManager().savePlayerData(event.getPlayer().getUniqueId());
        plugin.getBossBarManager().hide(event.getPlayer().getUniqueId());
    }
}