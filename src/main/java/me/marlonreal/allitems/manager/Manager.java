package me.marlonreal.allitems.manager;

import me.marlonreal.allitems.Allitems;
import me.marlonreal.allitems.FilterMode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Manager {

    private final Allitems plugin;

    private final Map<UUID, Set<Material>> playerCollectedItems = new HashMap<>();
    private final Set<Material> sharedCollectedItems = new HashSet<>();
    private final Set<Material> requiredItems = new HashSet<>();
    private List<Material> sortedItems = new ArrayList<>();
    private final Map<UUID, String> playerSearchQuery = new HashMap<>();
    private final Map<UUID, FilterMode> playerFilterMode = new HashMap<>();
    private final Map<UUID, Integer> playerCurrentPage = new HashMap<>();
    private final Set<UUID> playersInSearchMode = new HashSet<>();

    public Manager(Allitems plugin) {
        this.plugin = plugin;
    }

    public void loadRequiredItems() {
        for (Material mat : Material.values()) {
            if (isSurvivalObtainable(mat)) requiredItems.add(mat);
        }
        sortedItems = new ArrayList<>(requiredItems);
        sortedItems.sort(Comparator.comparing(Material::name));
    }

    private boolean isSurvivalObtainable(Material mat) {
        if (!mat.isItem()) return false;
        String name = mat.name();
        if (name.equals("AIR") || name.endsWith("_AIR")) return false;
        if (name.contains("COMMAND") || name.contains("BARRIER") || name.contains("STRUCTURE") ||
                name.contains("TEST_BLOCK") || name.contains("TEST_INSTANCE_BLOCK") ||
                name.startsWith("INFESTED_") || name.startsWith("POTTED_") ||
                name.endsWith("_SPAWN_EGG")) return false;
        if (name.equals("BEDROCK") || name.equals("END_PORTAL_FRAME") || name.equals("END_PORTAL") ||
                name.equals("NETHER_PORTAL") || name.equals("END_GATEWAY") || name.equals("SPAWNER") ||
                name.equals("REINFORCED_DEEPSLATE") || name.equals("VAULT") ||
                name.equals("TRIAL_SPAWNER") ||
                name.equals("CHORUS_PLANT") || name.equals("FROGSPAWN") || name.equals("BUDDING_AMETHYST")) return false;
        if (name.equals("FARMLAND") || name.equals("DIRT_PATH") || name.equals("FROSTED_ICE")) return false;
        if (name.endsWith("_FIRE") || name.equals("WATER") || name.equals("LAVA")) return false;
        if (name.equals("LIGHT") || name.equals("JIGSAW") || name.equals("DEBUG_STICK") ||
                name.equals("KNOWLEDGE_BOOK") || name.equals("PLAYER_HEAD") ||
                name.equals("PLAYER_WALL_HEAD") || name.equals("PETRIFIED_OAK_SLAB")) return false;
        return true;
    }

    public Set<Material> getEffectiveCollectedItems(UUID uuid) {
        if (plugin.isMultiplayer()) return sharedCollectedItems;
        playerCollectedItems.putIfAbsent(uuid, new HashSet<>());
        return playerCollectedItems.get(uuid);
    }

    public void checkAndAddItem(Player p, Material mat) {
        if (!requiredItems.contains(mat)) return;

        UUID uuid = p.getUniqueId();
        Set<Material> collected = getEffectiveCollectedItems(uuid);

        if (!collected.contains(mat)) {
            collected.add(mat);
            int total = requiredItems.size();
            int collectedSize = collected.size();
            String percent = String.format("%.1f", collectedSize * 100.0 / total);
            MessageManager mm = plugin.getMessageManager();

            if (plugin.isMultiplayer()) {
                Bukkit.broadcastMessage(mm.get("chat.item-collected-multiplayer",
                        MessageManager.ph("player", p.getName(), "item", formatItemName(mat))));
                Bukkit.broadcastMessage(mm.get("chat.item-collected-multiplayer-progress",
                        MessageManager.ph("collected", String.valueOf(collectedSize),
                                "total", String.valueOf(total), "percent", percent)));
            } else {
                p.sendMessage(mm.get("chat.item-collected",
                        MessageManager.ph("item", formatItemName(mat))));
                p.sendMessage(mm.get("chat.item-collected-progress",
                        MessageManager.ph("collected", String.valueOf(collectedSize),
                                "total", String.valueOf(total), "percent", percent)));
            }

            if (collectedSize == total) {
                Bukkit.broadcastMessage(mm.get("chat.all-collected-border"));
                Bukkit.broadcastMessage(plugin.isMultiplayer()
                        ? mm.get("chat.all-collected-broadcast-multiplayer")
                        : mm.get("chat.all-collected-broadcast",
                        MessageManager.ph("player", p.getName())));
                Bukkit.broadcastMessage(mm.get("chat.all-collected-border"));
            }
        }
    }

    public String formatItemName(Material mat) {
        String name = mat.name().replace("_", " ");
        StringBuilder formatted = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase()).append(" ");
            }
        }
        return formatted.toString().trim();
    }

    public List<Material> getFilteredItems(UUID uuid) {
        Set<Material> collected = getEffectiveCollectedItems(uuid);
        FilterMode mode = playerFilterMode.getOrDefault(uuid, FilterMode.ALL);
        String search = playerSearchQuery.getOrDefault(uuid, "").toLowerCase();

        List<Material> filtered = new ArrayList<>();
        for (Material mat : sortedItems) {
            boolean passes = switch (mode) {
                case ALL -> true;
                case COLLECTED -> collected.contains(mat);
                case MISSING -> !collected.contains(mat);
            };
            if (!passes) continue;
            if (!search.isEmpty() && !mat.name().toLowerCase().contains(search)) continue;
            filtered.add(mat);
        }
        return filtered;
    }

    public Map<UUID, Set<Material>> getPlayerCollectedItems() { return playerCollectedItems; }
    public Set<Material> getSharedCollectedItems() { return sharedCollectedItems; }
    public Set<Material> getRequiredItems() { return requiredItems; }
    public Map<UUID, FilterMode> getPlayerFilterMode() { return playerFilterMode; }
    public Map<UUID, String> getPlayerSearchQuery() { return playerSearchQuery; }
    public Map<UUID, Integer> getPlayerCurrentPage() { return playerCurrentPage; }
    public Set<UUID> getPlayersInSearchMode() { return playersInSearchMode; }
}