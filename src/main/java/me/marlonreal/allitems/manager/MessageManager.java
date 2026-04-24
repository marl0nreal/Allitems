package me.marlonreal.allitems.manager;

import me.marlonreal.allitems.Allitems;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class MessageManager {

    private final Allitems plugin;

    private YamlConfiguration config;

    public MessageManager(Allitems plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);

        InputStream stream = plugin.getResource("messages.yml");
        if (stream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            config.setDefaults(defaults);
        }
    }

    public String get(String key, Map<String, String> placeholders) {
        String raw = config.getString(key, "&cMissing message: " + key);
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                raw = raw.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public String get(String key) {
        return get(key, null);
    }

    public static Map<String, String> ph(Object... pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("Must be key-value pairs");
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), String.valueOf(pairs[i + 1]));
        }
        return map;
    }
}