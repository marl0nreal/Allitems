package me.marlonreal.allitems.manager;

import me.marlonreal.allitems.Allitems;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException("Could not create player data directory: " + dataFolder);
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
        manager.getSharedCollectedItems().clear();

        File sharedFile = new File(dataFolder, SHARED_FILE);
        if (!sharedFile.exists()) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(sharedFile);
        List<String> items = cfg.getStringList("items");
        for (String itemName : items) {
            try {
                Material material = Material.valueOf(itemName);
                if (manager.getRequiredItems().contains(material)) {
                    manager.getSharedCollectedItems().add(material);
                }
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("[All Items] Ignoring unknown saved material: " + itemName);
            }
        }

        manager.normalizeCollectedItems(manager.getSharedCollectedItems());
        plugin.getLogger().info("[All Items] Loaded " + manager.getSharedCollectedItems().size()
                + " shared items from " + SHARED_FILE);
    }

    private void loadPerPlayerData() {
        manager.getPlayerCollectedItems().clear();

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
                        Material material = Material.valueOf(itemName);
                        if (manager.getRequiredItems().contains(material)) {
                            collected.add(material);
                        }
                    } catch (IllegalArgumentException ignored) {
                        plugin.getLogger().warning("[All Items] Ignoring unknown saved material: " + itemName);
                    }
                }

                manager.normalizeCollectedItems(collected);
                manager.getPlayerCollectedItems().put(uuid, collected);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("[All Items] Ignoring invalid player data file: " + dataFile.getName());
            }
        }
    }

    public void saveAllPlayerData() {
        if (plugin.isMultiplayer()) {
            saveSharedData();
        } else {
            for (UUID uuid : new HashSet<>(manager.getPlayerCollectedItems().keySet())) {
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
            if (dataFile.exists() && !dataFile.delete()) {
                plugin.getLogger().warning("Failed to delete empty player data file for " + uuid);
            }
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        cfg.set("items", toOrderedMaterialNames(collected));

        try {
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data for " + uuid + ": " + e.getMessage());
        }
    }

    private void saveSharedData() {
        File sharedFile = new File(dataFolder, SHARED_FILE);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(sharedFile);
        cfg.set("items", toOrderedMaterialNames(manager.getSharedCollectedItems()));

        try {
            cfg.save(sharedFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save shared data: " + e.getMessage());
        }
    }

    private List<String> toOrderedMaterialNames(Set<Material> collected) {
        List<String> items = new ArrayList<>();
        for (Material material : manager.getSortedItems()) {
            if (collected.contains(material)) items.add(material.name());
        }
        return items;
    }
}