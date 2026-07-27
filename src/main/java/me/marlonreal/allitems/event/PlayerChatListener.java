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

import java.util.Locale;
import java.util.UUID;

public final class PlayerChatListener implements Listener {

    private final Allitems plugin;
    private final Manager manager;
    private final MessageManager mm;

    public PlayerChatListener(Allitems plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
        this.mm = plugin.getMessageManager();
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!manager.getPlayersInSearchMode().remove(uuid)) return;
        event.setCancelled(true);

        String message = event.getMessage().trim().toLowerCase(Locale.ROOT);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;

            if (message.equals("reset")) {
                manager.getPlayerSearchQuery().remove(uuid);
                player.sendMessage(mm.get("chat.search-reset"));
            } else {
                manager.getPlayerSearchQuery().put(uuid, message);
                player.sendMessage(mm.get("chat.search-result", MessageManager.ph("search", message)));
            }

            player.openInventory(new ItemsGui(plugin, uuid, 1).getInventory());
        });
    }
}