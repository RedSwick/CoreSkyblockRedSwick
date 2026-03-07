package be.RedSwick.skyblock.customitem;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Config pour les épées custom (Lame Arcanium uniquement pour l'instant).
 */
public class SwordConfig {

    private static SwordConfig instance;
    public static SwordConfig get() {
        if (instance == null) instance = new SwordConfig();
        return instance;
    }

    public record ArcaniumConfig(boolean autoSell) {}

    private final Map<UUID, ArcaniumConfig> configs = new HashMap<>();
    private YamlConfiguration config;
    private File file;

    public void init() {
        JavaPlugin plugin = be.RedSwick.skyblock.SkyBlockPlugin.getInstance();
        file = new File(plugin.getDataFolder(), "sword_config.yml");
        config = YamlConfiguration.loadConfiguration(file);

        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                configs.put(uuid, new ArcaniumConfig(config.getBoolean(key + ".autoSell")));
            } catch (Exception ignored) {}
        }
    }

    public ArcaniumConfig getArcanium(UUID uuid) {
        return configs.getOrDefault(uuid, new ArcaniumConfig(false));
    }

    public void setArcanium(UUID uuid, ArcaniumConfig cfg) {
        configs.put(uuid, cfg);
        config.set(uuid + ".autoSell", cfg.autoSell());
        save();
    }

    private void save() {
        try { config.save(file); } catch (IOException ignored) {}
    }
}