package be.RedSwick.skyblock.customitem;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Gère les données persistantes des Sacs de Butin et Sacs de Graines.
 * Chaque sac est identifié par un UUID unique stocké dans son lore.
 */
public class LootBagData {

    private static LootBagData instance;
    public static LootBagData get() {
        if (instance == null) instance = new LootBagData();
        return instance;
    }

    // Sac de butin : UUID sac → Map<Material, Long quantité>
    private final Map<UUID, Map<Material, Long>> lootBags = new HashMap<>();
    // Sac de butin : quels matériaux le joueur a configurés (max 10)
    private final Map<UUID, List<Material>> lootBagFilters = new HashMap<>();
    // Sac de graines : UUID sac → Map<Material, Long>
    private final Map<UUID, Map<Material, Long>> seedBags = new HashMap<>();

    private YamlConfiguration config;
    private File file;

    public void init() {
        JavaPlugin plugin = be.RedSwick.skyblock.SkyBlockPlugin.getInstance();
        file = new File(plugin.getDataFolder(), "bags_data.yml");
        config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    // ── Sac de butin ──────────────────────────────────────────────────

    public Map<Material, Long> getLootBagContents(UUID bagId) {
        return lootBags.computeIfAbsent(bagId, k -> new LinkedHashMap<>());
    }

    public List<Material> getLootBagFilters(UUID bagId) {
        return lootBagFilters.computeIfAbsent(bagId, k -> new ArrayList<>());
    }

    public void addToLootBag(UUID bagId, Material mat, long amount) {
        Map<Material, Long> contents = getLootBagContents(bagId);
        contents.merge(mat, amount, Long::sum);
        save();
    }

    public void setLootBagFilter(UUID bagId, List<Material> filters) {
        lootBagFilters.put(bagId, new ArrayList<>(filters));
        save();
    }

    public boolean isFilteredByLootBag(UUID bagId, Material mat) {
        List<Material> filters = lootBagFilters.get(bagId);
        return filters != null && filters.contains(mat);
    }

    // ── Sac de graines ────────────────────────────────────────────────

    public static final Set<Material> SEED_MATERIALS = Set.of(
            Material.WHEAT_SEEDS, Material.CARROT, Material.POTATO,
            Material.BEETROOT_SEEDS, Material.NETHER_WART, Material.MELON_SEEDS,
            Material.PUMPKIN_SEEDS, Material.COCOA_BEANS, Material.TORCHFLOWER_SEEDS,
            Material.PITCHER_POD
    );

    public Map<Material, Long> getSeedBagContents(UUID bagId) {
        return seedBags.computeIfAbsent(bagId, k -> new LinkedHashMap<>());
    }

    public void addToSeedBag(UUID bagId, Material mat, long amount) {
        if (!SEED_MATERIALS.contains(mat)) return;
        getSeedBagContents(bagId).merge(mat, amount, Long::sum);
        save();
    }

    public boolean takeSeed(UUID bagId, Material mat, long amount) {
        Map<Material, Long> contents = getSeedBagContents(bagId);
        Long cur = contents.get(mat);
        if (cur == null || cur < amount) return false;
        if (cur == amount) contents.remove(mat);
        else contents.put(mat, cur - amount);
        save();
        return true;
    }

    public long getSeedCount(UUID bagId, Material mat) {
        return getSeedBagContents(bagId).getOrDefault(mat, 0L);
    }

    // ── Persistance ───────────────────────────────────────────────────

    private void load() {
        // Loot bags
        if (config.contains("lootbags")) {
            for (String bagStr : config.getConfigurationSection("lootbags").getKeys(false)) {
                try {
                    UUID bagId = UUID.fromString(bagStr);
                    Map<Material, Long> contents = new LinkedHashMap<>();
                    String path = "lootbags." + bagStr + ".contents";
                    if (config.contains(path)) {
                        for (String matStr : config.getConfigurationSection(path).getKeys(false)) {
                            try {
                                Material m = Material.valueOf(matStr);
                                contents.put(m, config.getLong(path + "." + matStr));
                            } catch (Exception ignored) {}
                        }
                    }
                    lootBags.put(bagId, contents);

                    // Filters
                    List<Material> filters = new ArrayList<>();
                    String fPath = "lootbags." + bagStr + ".filters";
                    if (config.contains(fPath)) {
                        for (String matStr : config.getStringList(fPath)) {
                            try { filters.add(Material.valueOf(matStr)); } catch (Exception ignored) {}
                        }
                    }
                    lootBagFilters.put(bagId, filters);
                } catch (Exception ignored) {}
            }
        }

        // Seed bags
        if (config.contains("seedbags")) {
            for (String bagStr : config.getConfigurationSection("seedbags").getKeys(false)) {
                try {
                    UUID bagId = UUID.fromString(bagStr);
                    Map<Material, Long> contents = new LinkedHashMap<>();
                    for (String matStr : config.getConfigurationSection("seedbags." + bagStr).getKeys(false)) {
                        try {
                            Material m = Material.valueOf(matStr);
                            contents.put(m, config.getLong("seedbags." + bagStr + "." + matStr));
                        } catch (Exception ignored) {}
                    }
                    seedBags.put(bagId, contents);
                } catch (Exception ignored) {}
            }
        }
    }

    public void save() {
        // Loot bags
        for (Map.Entry<UUID, Map<Material, Long>> e : lootBags.entrySet()) {
            String base = "lootbags." + e.getKey();
            e.getValue().forEach((m, qty) -> config.set(base + ".contents." + m.name(), qty));
        }
        for (Map.Entry<UUID, List<Material>> e : lootBagFilters.entrySet()) {
            config.set("lootbags." + e.getKey() + ".filters",
                    e.getValue().stream().map(Material::name).toList());
        }
        // Seed bags
        for (Map.Entry<UUID, Map<Material, Long>> e : seedBags.entrySet()) {
            e.getValue().forEach((m, qty) ->
                    config.set("seedbags." + e.getKey() + "." + m.name(), qty));
        }
        try { config.save(file); } catch (IOException ignored) {}
    }
}