package me.marlonreal.allitems;

import me.marlonreal.allitems.command.AllitemsCommand;
import me.marlonreal.allitems.event.CraftItemListener;
import me.marlonreal.allitems.event.EntityPickupItemListener;
import me.marlonreal.allitems.event.InventoryClickListener;
import me.marlonreal.allitems.event.InventoryObtainListener;
import me.marlonreal.allitems.event.PlayerChatListener;
import me.marlonreal.allitems.event.PlayerConnectionListener;
import me.marlonreal.allitems.manager.BossBarManager;
import me.marlonreal.allitems.manager.Manager;
import me.marlonreal.allitems.manager.MessageManager;
import me.marlonreal.allitems.manager.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class Allitems extends JavaPlugin {

    private Manager manager;
    private PlayerDataManager playerDataManager;
    private MessageManager messageManager;
    private BossBarManager bossBarManager;
    private BukkitTask autosaveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        messageManager = new MessageManager(this);
        manager = new Manager(this);
        playerDataManager = new PlayerDataManager(this);
        bossBarManager = new BossBarManager(this);

        manager.loadRequiredItems();
        playerDataManager.setupDataFolder();
        playerDataManager.loadPlayerData();

        registerListeners();
        registerCommands();
        restartAutosaveTask();
        bossBarManager.updateAll();

        String mode = isMultiplayer() ? "shared multiplayer" : "per-player";
        getLogger().info("Enabled v" + getDescription().getVersion() + " in " + mode
                + " mode with " + manager.getRequiredItems().size() + " required items.");
    }

    @Override
    public void onDisable() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
        if (playerDataManager != null) {
            playerDataManager.saveAllPlayerData();
        }
        if (bossBarManager != null) {
            bossBarManager.clear();
        }
        getLogger().info("Disabled successfully.");
    }

    private void registerListeners() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new CraftItemListener(manager), this);
        pluginManager.registerEvents(new EntityPickupItemListener(manager), this);
        pluginManager.registerEvents(new InventoryObtainListener(this), this);
        pluginManager.registerEvents(new InventoryClickListener(this), this);
        pluginManager.registerEvents(new PlayerChatListener(this), this);
        pluginManager.registerEvents(new PlayerConnectionListener(this), this);
    }

    private void registerCommands() {
        PluginCommand command = getCommand("allitems");
        if (command == null) {
            throw new IllegalStateException("Command 'allitems' is missing from plugin.yml");
        }

        AllitemsCommand handler = new AllitemsCommand(this);
        command.setExecutor(handler);
        command.setTabCompleter(handler);
    }

    public void restartAutosaveTask() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }

        long intervalSeconds = getConfig().getLong("storage.autosave-interval-seconds", 300L);
        if (intervalSeconds <= 0L) {
            getLogger().info("Automatic saving is disabled.");
            return;
        }

        long intervalTicks = Math.max(20L, intervalSeconds * 20L);
        autosaveTask = Bukkit.getScheduler().runTaskTimer(
                this,
                playerDataManager::saveAllPlayerData,
                intervalTicks,
                intervalTicks
        );
    }

    public boolean isMultiplayer() {
        return getConfig().getBoolean("multiplayer", false);
    }

    public Manager getManager() {
        return manager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }
}