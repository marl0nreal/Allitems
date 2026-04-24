package me.marlonreal.allitems;

import me.marlonreal.allitems.command.AllitemsCommand;
import me.marlonreal.allitems.event.CraftItemListener;
import me.marlonreal.allitems.event.EntityPickupItemListener;
import me.marlonreal.allitems.event.InventoryClickListener;
import me.marlonreal.allitems.event.PlayerChatListener;
import me.marlonreal.allitems.event.PlayerQuitListener;
import me.marlonreal.allitems.manager.Manager;
import me.marlonreal.allitems.manager.MessageManager;
import me.marlonreal.allitems.manager.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Allitems extends JavaPlugin {

    private static Allitems plugin;

    private Manager manager;
    private PlayerDataManager playerDataManager;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        plugin = this;

        saveDefaultConfig();

        messageManager = new MessageManager(this);
        manager = new Manager(this);
        playerDataManager = new PlayerDataManager(this);

        manager.loadRequiredItems();
        playerDataManager.setupDataFolder();
        playerDataManager.loadPlayerData();

        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new CraftItemListener(manager), this);
        pluginManager.registerEvents(new EntityPickupItemListener(manager), this);
        pluginManager.registerEvents(new InventoryClickListener(this), this);
        pluginManager.registerEvents(new PlayerChatListener(this), this);
        pluginManager.registerEvents(new PlayerQuitListener(playerDataManager), this);

        getCommand("allitems").setExecutor(new AllitemsCommand(this));

        String mode = isMultiplayer() ? "MULTIPLAYER (shared)" : "per-player";
        getLogger().info("All Items Plugin enabled! " + manager.getRequiredItems().size() + " items loaded.");
    }

    @Override
    public void onDisable() {
        playerDataManager.savePlayerData();
        getLogger().info("All Items Plugin disabled!");
    }

    public boolean isMultiplayer() {
        return getConfig().getBoolean("multiplayer", false);
    }

    public static Allitems getPlugin() { return plugin; }
    public Manager getManager() { return manager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public MessageManager getMessageManager() { return messageManager; }
}