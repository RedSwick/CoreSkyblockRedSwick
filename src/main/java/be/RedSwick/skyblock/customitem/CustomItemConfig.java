package be.RedSwick.skyblock.customitem;

import be.RedSwick.skyblock.SkyBlockPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stocke les préférences de configuration par joueur pour
 * le Hammer et la Farmer's Hoe 5x5.
 */
public class CustomItemConfig {

    private static final CustomItemConfig INSTANCE = new CustomItemConfig();
    public static CustomItemConfig get() { return INSTANCE; }

    // ── Hammer config ─────────────────────────────
    public record HammerConfig(boolean autoSell, boolean smelt, boolean autoSellSmelted) {}

    // ── Axe config ────────────────────────────────
    public record AxeConfig(boolean autoSell) {}

    // ── Multitool config ──────────────────────────
    public record MultitoolConfig(boolean autoSell, boolean smelt, boolean silkTouch) {}

    // ── Hoe config ────────────────────────────────
    public record HoeConfig(boolean toInventory, boolean autoSell, int radius) {} // radius: 0=1x1, 1=3x3, 2=5x5

    private final Map<UUID, HammerConfig> hammerConfigs = new HashMap<>();
    private final Map<UUID, AxeConfig>       axeConfigs       = new HashMap<>();
    private final Map<UUID, MultitoolConfig> multitoolConfigs = new HashMap<>();
    private final Map<UUID, HoeConfig>    hoeConfigs    = new HashMap<>();

    private File              file;
    private YamlConfiguration config;

    public void init() {
        file = new File(SkyBlockPlugin.getInstance().getDataFolder(), "customitem_config.yml");
        if (!file.exists()) try { file.createNewFile(); } catch (Exception e) { e.printStackTrace(); }
        config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    // ── Getters avec défauts ──────────────────────

    public HammerConfig getHammer(UUID uuid) {
        return hammerConfigs.getOrDefault(uuid, new HammerConfig(false, false, false));
    }

    public AxeConfig getAxe(UUID uuid) {
        return axeConfigs.getOrDefault(uuid, new AxeConfig(false));
    }

    public MultitoolConfig getMultitool(UUID uuid) {
        return multitoolConfigs.getOrDefault(uuid, new MultitoolConfig(false, false, false));
    }

    public HoeConfig getHoe(UUID uuid) {
        return hoeConfigs.getOrDefault(uuid, new HoeConfig(true, false, 2));
    }

    // ── Setters + save ────────────────────────────

    public void setMultitool(UUID uuid, MultitoolConfig cfg) {
        multitoolConfigs.put(uuid, cfg);
        String k = uuid.toString() + ".multitool";
        config.set(k + ".autoSell",  cfg.autoSell());
        config.set(k + ".smelt",     cfg.smelt());
        config.set(k + ".silkTouch", cfg.silkTouch());
        save();
    }

    public void setAxe(UUID uuid, AxeConfig cfg) {
        axeConfigs.put(uuid, cfg);
        String k = uuid.toString() + ".axe";
        config.set(k + ".autoSell", cfg.autoSell());
        save();
    }

    public void setHammer(UUID uuid, HammerConfig cfg) {
        hammerConfigs.put(uuid, cfg);
        String k = uuid.toString() + ".hammer";
        config.set(k + ".autoSell",         cfg.autoSell());
        config.set(k + ".smelt",            cfg.smelt());
        config.set(k + ".autoSellSmelted",  cfg.autoSellSmelted());
        save();
    }

    public void setHoe(UUID uuid, HoeConfig cfg) {
        hoeConfigs.put(uuid, cfg);
        String k = uuid.toString() + ".hoe";
        config.set(k + ".toInventory", cfg.toInventory());
        config.set(k + ".autoSell",    cfg.autoSell());
        config.set(k + ".radius",      cfg.radius());
        save();
    }

    private void load() {
        for (String uuidStr : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String hk = uuidStr + ".hammer";
                if (config.contains(hk))
                    hammerConfigs.put(uuid, new HammerConfig(
                            config.getBoolean(hk + ".autoSell"),
                            config.getBoolean(hk + ".smelt"),
                            config.getBoolean(hk + ".autoSellSmelted")
                    ));
                String mt = uuidStr + ".multitool";
                if (config.contains(mt))
                    multitoolConfigs.put(uuid, new MultitoolConfig(
                            config.getBoolean(mt + ".autoSell"),
                            config.getBoolean(mt + ".smelt"),
                            config.getBoolean(mt + ".silkTouch")
                    ));
                String ax = uuidStr + ".axe";
                if (config.contains(ax))
                    axeConfigs.put(uuid, new AxeConfig(config.getBoolean(ax + ".autoSell")));
                String hoe = uuidStr + ".hoe";
                if (config.contains(hoe))
                    hoeConfigs.put(uuid, new HoeConfig(
                            config.getBoolean(hoe + ".toInventory", true),
                            config.getBoolean(hoe + ".autoSell"),
                            config.getInt(hoe + ".radius", 2)
                    ));
            } catch (Exception ignored) {}
        }
    }

    private void save() {
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }
}