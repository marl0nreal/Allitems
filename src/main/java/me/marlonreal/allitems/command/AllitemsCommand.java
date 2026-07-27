package me.marlonreal.allitems.command;

import me.marlonreal.allitems.Allitems;
import me.marlonreal.allitems.gui.ItemsGui;
import me.marlonreal.allitems.manager.Manager;
import me.marlonreal.allitems.manager.MessageManager;
import me.marlonreal.allitems.manager.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AllitemsCommand implements TabExecutor {

    private static final List<String> PLAYER_SUBCOMMANDS = List.of(
            "gui", "search", "progress", "missing", "reset", "top"
    );

    private final Allitems plugin;
    private final Manager manager;
    private final PlayerDataManager playerDataManager;
    private final MessageManager mm;

    public AllitemsCommand(Allitems plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
        this.playerDataManager = plugin.getPlayerDataManager();
        this.mm = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            return handleReload(sender);
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.get("commands.only-players"));
            return true;
        }

        UUID uuid = player.getUniqueId();

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "gui" -> player.openInventory(new ItemsGui(plugin, uuid, 1).getInventory());
            case "search" -> handleSearch(player, uuid, args);
            case "progress" -> showProgress(player, uuid);
            case "missing" -> showMissing(player, uuid);
            case "reset" -> handleReset(player, uuid);
            case "top" -> showTop(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleSearch(Player player, UUID uuid, String[] args) {
        if (args.length < 2) {
            player.sendMessage(mm.get("commands.search-no-term"));
            return;
        }

        String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                .toLowerCase(Locale.ROOT);
        manager.getPlayerSearchQuery().put(uuid, query);
        player.sendMessage(mm.get("chat.search-result", MessageManager.ph("search", query)));
        player.openInventory(new ItemsGui(plugin, uuid, 1).getInventory());
    }

    private void handleReset(Player player, UUID uuid) {
        manager.resetProgress(uuid);
        playerDataManager.savePlayerData(uuid);

        if (plugin.isMultiplayer()) {
            plugin.getBossBarManager().updateAll();
        } else {
            plugin.getBossBarManager().update(player);
        }

        player.sendMessage(mm.get("commands.reset-done"));
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("allitems.admin")) {
            sender.sendMessage(mm.get("commands.no-permission"));
            return true;
        }

        playerDataManager.saveAllPlayerData();
        plugin.reloadConfig();
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
        plugin.getMessageManager().reload();
        manager.loadRequiredItems();
        playerDataManager.loadPlayerData();
        plugin.restartAutosaveTask();
        plugin.getBossBarManager().reload();

        sender.sendMessage(mm.get("commands.reload-done", MessageManager.ph(
                "total", String.valueOf(manager.getRequiredItems().size()),
                "ordered", manager.isOrderedCollectionEnabled() ? "enabled" : "disabled",
                "bossbar", plugin.getConfig().getBoolean("bossbar.enabled", true) ? "enabled" : "disabled"
        )));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(mm.get("commands.help-header"));
        sender.sendMessage(mm.get("commands.help-gui"));
        sender.sendMessage(mm.get("commands.help-search"));
        sender.sendMessage(mm.get("commands.help-progress"));
        sender.sendMessage(mm.get("commands.help-missing"));
        sender.sendMessage(mm.get("commands.help-reset"));
        sender.sendMessage(mm.get("commands.help-top"));
        if (sender.hasPermission("allitems.admin")) {
            sender.sendMessage(mm.get("commands.help-reload"));
        }
    }

    private void showProgress(Player player, UUID uuid) {
        int collected = manager.getEffectiveCollectedItems(uuid).size();
        int total = manager.getRequiredItems().size();
        player.sendMessage(mm.get("commands.progress-header"));
        player.sendMessage(mm.get("commands.progress-collected",
                MessageManager.ph("collected", String.valueOf(collected),
                        "total", String.valueOf(total))));
        player.sendMessage(mm.get("commands.progress-percent",
                MessageManager.ph("percent", manager.formatPercent(collected, total, 2))));

        if (manager.isOrderedCollectionEnabled()) {
            Material next = manager.getNextRequiredItem(uuid);
            if (next != null) {
                player.sendMessage(mm.get("commands.progress-next", MessageManager.ph(
                        "item", manager.formatItemName(next),
                        "position", String.valueOf(manager.getOrderPosition(next)),
                        "total", String.valueOf(total)
                )));
            }
        }
    }

    private void showMissing(Player player, UUID uuid) {
        Set<Material> collected = manager.getEffectiveCollectedItems(uuid);
        List<Material> missing = new ArrayList<>();
        for (Material material : manager.getDisplayItems()) {
            if (!collected.contains(material)) missing.add(material);
        }

        player.sendMessage(mm.get("commands.missing-header",
                MessageManager.ph("count", String.valueOf(missing.size()))));
        int shown = Math.min(20, missing.size());
        for (int i = 0; i < shown; i++) {
            Material material = missing.get(i);
            player.sendMessage(mm.get("commands.missing-item",
                    MessageManager.ph("item", manager.formatItemName(material))));
        }
        if (missing.size() > shown) {
            player.sendMessage(mm.get("commands.missing-more",
                    MessageManager.ph("count", String.valueOf(missing.size() - shown))));
        }
    }

    private void showTop(Player player) {
        if (plugin.isMultiplayer()) {
            player.sendMessage(mm.get("commands.top-unavailable-multiplayer"));
            return;
        }

        List<Map.Entry<UUID, Set<Material>>> sorted =
                new ArrayList<>(manager.getPlayerCollectedItems().entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()));

        player.sendMessage(mm.get("commands.top-header"));
        if (sorted.isEmpty()) {
            player.sendMessage(mm.get("commands.top-empty"));
            return;
        }

        int rank = 1;
        for (Map.Entry<UUID, Set<Material>> entry : sorted) {
            if (rank > 10) break;
            Player onlinePlayer = Bukkit.getPlayer(entry.getKey());
            String name = onlinePlayer != null
                    ? onlinePlayer.getName()
                    : Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = mm.get("commands.top-unknown-player");

            int collected = entry.getValue().size();
            player.sendMessage(mm.get("commands.top-entry", MessageManager.ph(
                    "rank", String.valueOf(rank),
                    "player", name,
                    "collected", String.valueOf(collected),
                    "percent", manager.formatPercent(collected, manager.getRequiredItems().size(), 1)
            )));
            rank++;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) return List.of();

        List<String> available = new ArrayList<>(PLAYER_SUBCOMMANDS);
        if (sender.hasPermission("allitems.admin")) {
            available.add("reload");
        }

        String input = args[0].toLowerCase(Locale.ROOT);
        return available.stream()
                .filter(option -> option.startsWith(input))
                .sorted()
                .toList();
    }
}