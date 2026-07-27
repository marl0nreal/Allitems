package me.marlonreal.allitems.manager;

import me.marlonreal.allitems.Allitems;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class BossBarManager {

    private static final String DEFAULT_TITLE = "&6Next item: &e{item} &7({position}/{total})";

    private final Allitems plugin;
    private final Manager manager;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    private boolean enabled;
    private boolean showProgress;
    private String titleTemplate;
    private BarColor color;
    private BarStyle style;

    public BossBarManager(Allitems plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
        loadSettings();
    }

    public void update(Player player) {
        if (!enabled || !manager.isOrderedCollectionEnabled()) {
            hide(player.getUniqueId());
            return;
        }

        Material next = manager.getNextRequiredItem(player.getUniqueId());
        if (next == null) {
            hide(player.getUniqueId());
            return;
        }

        BossBar bossBar = bossBars.computeIfAbsent(player.getUniqueId(), ignored -> createBossBar(player));
        if (!bossBar.getPlayers().contains(player)) {
            bossBar.addPlayer(player);
        }

        int collected = manager.getEffectiveCollectedItems(player.getUniqueId()).size();
        int total = manager.getRequiredItems().size();
        int position = manager.getOrderPosition(next);
        String percent = manager.formatPercent(collected, total, 1);

        String title = titleTemplate
                .replace("{item}", manager.formatItemName(next))
                .replace("{material}", next.name())
                .replace("{position}", String.valueOf(position))
                .replace("{collected}", String.valueOf(collected))
                .replace("{total}", String.valueOf(total))
                .replace("{percent}", percent);

        bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', title));
        bossBar.setColor(color);
        bossBar.setStyle(style);
        bossBar.setProgress(calculateProgress(collected, total));
        bossBar.setVisible(true);
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player);
        }
    }

    public void reload() {
        clear();
        loadSettings();
        updateAll();
    }

    public void hide(UUID uuid) {
        BossBar bossBar = bossBars.remove(uuid);
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
    }

    public void clear() {
        for (BossBar bossBar : bossBars.values()) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
        bossBars.clear();
    }

    private BossBar createBossBar(Player player) {
        BossBar bossBar = Bukkit.createBossBar("", color, style);
        bossBar.addPlayer(player);
        return bossBar;
    }

    private void loadSettings() {
        enabled = plugin.getConfig().getBoolean("bossbar.enabled", true);
        showProgress = plugin.getConfig().getBoolean("bossbar.show-progress", true);
        titleTemplate = plugin.getConfig().getString("bossbar.title", DEFAULT_TITLE);
        color = readColor(plugin.getConfig().getString("bossbar.color", "YELLOW"));
        style = readStyle(plugin.getConfig().getString("bossbar.style", "SOLID"));
    }

    private double calculateProgress(int collected, int total) {
        if (!showProgress) return 1.0D;
        if (total <= 0) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D, collected / (double) total));
    }

    private BarColor readColor(String configured) {
        String value = configured == null ? "YELLOW" : configured.trim().toUpperCase(Locale.ROOT);
        try {
            return BarColor.valueOf(value);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid bossbar.color '" + configured + "'. Using YELLOW.");
            return BarColor.YELLOW;
        }
    }

    private BarStyle readStyle(String configured) {
        String value = configured == null ? "SOLID" : configured.trim().toUpperCase(Locale.ROOT);
        try {
            return BarStyle.valueOf(value);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid bossbar.style '" + configured + "'. Using SOLID.");
            return BarStyle.SOLID;
        }
    }
}