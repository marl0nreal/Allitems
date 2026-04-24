package me.marlonreal.allitems.command;

import me.marlonreal.allitems.Allitems;
import me.marlonreal.allitems.gui.ItemsGui;
import me.marlonreal.allitems.manager.Manager;
import me.marlonreal.allitems.manager.MessageManager;
import me.marlonreal.allitems.manager.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class AllitemsCommand implements CommandExecutor {

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
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("allitems")) return false;

        if (args.length == 0) {
            sender.sendMessage(mm.get("commands.help-header"));
            sender.sendMessage(mm.get("commands.help-gui"));
            sender.sendMessage(mm.get("commands.help-search"));
            sender.sendMessage(mm.get("commands.help-progress"));
            sender.sendMessage(mm.get("commands.help-missing"));
            sender.sendMessage(mm.get("commands.help-reset"));
            sender.sendMessage(mm.get("commands.help-top"));
            return true;
        }

        if (!(sender instanceof Player p)) {
            sender.sendMessage(mm.get("commands.only-players"));
            return true;
        }

        UUID uuid = p.getUniqueId();

        switch (args[0].toLowerCase()) {
            case "gui" -> p.openInventory(new ItemsGui(plugin, uuid, 1).getInventory());

            case "search" -> {
                if (args.length < 2) {
                    p.sendMessage(mm.get("commands.search-no-term"));
                    return true;
                }
                String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).toLowerCase();
                manager.getPlayerSearchQuery().put(uuid, query);
                p.sendMessage(mm.get("chat.search-result", MessageManager.ph("search", query)));
                p.openInventory(new ItemsGui(plugin, uuid, 1).getInventory());
            }

            case "progress" -> {
                int collected = manager.getEffectiveCollectedItems(uuid).size();
                int total = manager.getRequiredItems().size();
                p.sendMessage(mm.get("commands.progress-header"));
                p.sendMessage(mm.get("commands.progress-collected",
                        MessageManager.ph("collected", String.valueOf(collected),
                                "total", String.valueOf(total))));
                p.sendMessage(mm.get("commands.progress-percent",
                        MessageManager.ph("percent", String.format("%.2f", collected * 100.0 / total))));
            }

            case "missing" -> {
                Set<Material> missing = new HashSet<>(manager.getRequiredItems());
                missing.removeAll(manager.getEffectiveCollectedItems(uuid));
                p.sendMessage(mm.get("commands.missing-header",
                        MessageManager.ph("count", String.valueOf(missing.size()))));
                int count = 0;
                for (Material mat : missing) {
                    if (count >= 20) {
                        p.sendMessage(mm.get("commands.missing-more",
                                MessageManager.ph("count", String.valueOf(missing.size() - 20))));
                        break;
                    }
                    p.sendMessage(mm.get("commands.missing-item",
                            MessageManager.ph("item", manager.formatItemName(mat))));
                    count++;
                }
            }

            case "reset" -> {
                manager.getPlayerCollectedItems().put(uuid, new HashSet<>());
                playerDataManager.savePlayerData(uuid);
                p.sendMessage(mm.get("commands.reset-done"));
            }

            case "top" -> {
                List<Map.Entry<UUID, Set<Material>>> sorted =
                        new ArrayList<>(manager.getPlayerCollectedItems().entrySet());
                sorted.sort((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()));

                p.sendMessage(mm.get("commands.top-header"));
                int rank = 1;
                for (Map.Entry<UUID, Set<Material>> entry : sorted) {
                    if (rank > 10) break;
                    Player pl = Bukkit.getPlayer(entry.getKey());
                    String name = pl != null ? pl.getName() : mm.get("commands.top-unknown-player");
                    int collected = entry.getValue().size();
                    p.sendMessage(mm.get("commands.top-entry", MessageManager.ph(
                            "rank", String.valueOf(rank),
                            "player", name,
                            "collected", String.valueOf(collected),
                            "percent", String.format("%.1f", collected * 100.0 / manager.getRequiredItems().size())
                    )));
                    rank++;
                }
            }
        }

        return true;
    }
}