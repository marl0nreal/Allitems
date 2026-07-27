package me.marlonreal.allitems.manager;

import me.marlonreal.allitems.Allitems;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CollectionOrderManager {

    private static final String FILE_NAME = "collection-order.yml";
    private static final String ORDER_PATH = "order";

    private final Allitems plugin;

    public CollectionOrderManager(Allitems plugin) {
        this.plugin = plugin;
    }

    public List<Material> loadOrCreate(Set<Material> requiredItems) {
        File orderFile = new File(plugin.getDataFolder(), FILE_NAME);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(orderFile);

        List<Material> order = new ArrayList<>();
        Set<Material> seen = new HashSet<>();

        for (String materialName : configuration.getStringList(ORDER_PATH)) {
            try {
                Material material = Material.valueOf(materialName);
                if (!requiredItems.contains(material) || !seen.add(material)) {
                    continue;
                }
                order.add(material);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("[All Items] Ignoring unknown material in "
                        + FILE_NAME + ": " + materialName);
            }
        }

        List<Material> missing = new ArrayList<>();
        for (Material material : requiredItems) {
            if (!seen.contains(material)) {
                missing.add(material);
            }
        }
        Collections.shuffle(missing);

        boolean newlyGenerated = order.isEmpty();
        order.addAll(missing);

        saveOrder(orderFile, configuration, order);

        if (newlyGenerated) {
            plugin.getLogger().info("[All Items] Generated a random collection order with "
                    + order.size() + " items.");
        } else if (!missing.isEmpty()) {
            plugin.getLogger().info("[All Items] Added " + missing.size()
                    + " newly available items to the saved random collection order.");
        } else {
            plugin.getLogger().info("[All Items] Loaded the saved random collection order.");
        }

        return List.copyOf(order);
    }

    private void saveOrder(File orderFile, YamlConfiguration configuration, List<Material> order) {
        List<String> names = order.stream().map(Material::name).toList();
        configuration.set(ORDER_PATH, names);

        try {
            configuration.save(orderFile);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save " + FILE_NAME, exception);
        }
    }
}