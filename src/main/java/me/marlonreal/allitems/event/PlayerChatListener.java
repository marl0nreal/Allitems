package me.marlonreal.allitems.event;

import me.marlonreal.allitems.Allitems;
import me.marlonreal.allitems.gui.ItemsGui;
import me.marlonreal.allitems.manager.Manager;
import me.marlonreal.allitems.manager.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class PlayerChatListener implements Listener {

    private final Allitems plugin;
    private final Manager manager;
    private final MessageManager mm;

    public PlayerChatListener(Allitems plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
        this.mm = plugin.getMessageManager();
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        if (!manager.getPlayersInSearchMode().contains(uuid)) return;

        e.setCancelled(true);
        manager.getPlayersInSearchMode().remove(uuid); // Bug-Fix: fehlte im Original

        String message = e.getMessage().toLowerCase();

        if (message.equals("reset")) {
            manager.getPlayerSearchQuery().remove(uuid);
            p.sendMessage(mm.get("chat.search-reset"));
        } else {
            manager.getPlayerSearchQuery().put(uuid, message);
            p.sendMessage(mm.get("chat.search-result", MessageManager.ph("search", message)));
        }

        Bukkit.getScheduler().runTask(plugin, () ->
                p.openInventory(new ItemsGui(plugin, uuid, 1).getInventory())
        );
    }
}