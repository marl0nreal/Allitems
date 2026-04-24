package me.marlonreal.allitems.manager;

import me.marlonreal.allitems.Allitems;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerDataManager {

    private static final String SHARED_FILE = "shared.yml";

    private final Allitems plugin;
    private final Manager manager;

    private File dataFolder;

    public PlayerDataManager(Allitems plugin) {
        this.plugin = plugin;
        manager = plugin.getManager();
    }

    public void setupDataFolder() {
        dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.getParentFile().mkdirs();
            dataFolder.mkdirs();
        }
    }

    public void loadPlayerData() {
        if (!dataFolder.exists()) return;

        if (plugin.isMultiplayer()) {
            loadSharedData();
        } else {
            loadPerPlayerData();
        }
    }

    private void loadSharedData() {
        File sharedFile = new File(dataFolder, SHARED_FILE);
        if (!sharedFile.exists()) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(sharedFile);
        List<String> items = cfg.getStringList("items");
        for (String itemName : items) {
            try {
                manager.getSharedCollectedItems().add(Material.valueOf(itemName));
            } catch (IllegalArgumentException ignored) {}
        }
        plugin.getLogger().info("[All Items] Loaded " + manager.getSharedCollectedItems().size()
                + " shared items from " + SHARED_FILE);
    }

    private void loadPerPlayerData() {
        File[] files = dataFolder.listFiles();
        if (files == null) return;

        for (File dataFile : files) {
            if (!dataFile.getName().endsWith(".yml")) continue;
            if (dataFile.getName().equals(SHARED_FILE)) continue;

            try {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
                UUID uuid = UUID.fromString(dataFile.getName().replace(".yml", ""));

                List<String> items = cfg.getStringList("items");
                Set<Material> collected = new HashSet<>();
                for (String itemName : items) {
                    try {
                        collected.add(Material.valueOf(itemName));
                    } catch (IllegalArgumentException ignored) {}
                }
                manager.getPlayerCollectedItems().put(uuid, collected);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void savePlayerData() {
        if (plugin.isMultiplayer()) {
            saveSharedData();
        } else {
            for (UUID uuid : manager.getPlayerCollectedItems().keySet()) {
                savePlayerData(uuid);
            }
        }
    }

    public void savePlayerData(UUID uuid) {
        if (plugin.isMultiplayer()) {
            saveSharedData();
            return;
        }

        Set<Material> collected = manager.getPlayerCollectedItems().get(uuid);
        File dataFile = new File(dataFolder, uuid + ".yml");

        if (collected == null || collected.isEmpty()) {
            if (dataFile.exists()) dataFile.delete();
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        List<String> items = new ArrayList<>();
        for (Material mat : collected) items.add(mat.name());
        cfg.set("items", items);

        try {
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data for " + uuid + ": " + e.getMessage());
        }
    }

    private void saveSharedData() {
        File sharedFile = new File(dataFolder, SHARED_FILE);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(sharedFile);

        List<String> items = new ArrayList<>();
        for (Material mat : manager.getSharedCollectedItems()) items.add(mat.name());
        cfg.set("items", items);

        try {
            cfg.save(sharedFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save shared data: " + e.getMessage());
        }
    }
}