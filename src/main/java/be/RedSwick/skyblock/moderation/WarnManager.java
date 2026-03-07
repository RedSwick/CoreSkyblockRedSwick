package be.RedSwick.skyblock.moderation;

import be.RedSwick.skyblock.SkyBlockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * OPTIMISATIONS :
 *  - save() async → plus de I/O synchrone sur le main thread
 *  - clearWarns() supprime aussi la clé YAML (évitait une entrée vide persistante)
 */
public class WarnManager {

    private static final WarnManager INSTANCE = new WarnManager();
    public static WarnManager get() { return INSTANCE; }

    public record WarnEntry(String reason, String by, long timestamp) {}

    private final Map<UUID, List<WarnEntry>> warns = new HashMap<>();
    private File file;

    public void init() {
        file = new File(SkyBlockPlugin.getInstance().getDataFolder(), "warns.yml");
        if (!file.exists()) {
            try { file.getParentFile().mkdirs(); file.createNewFile(); }
            catch (Exception e) { e.printStackTrace(); }
        }
        load();
    }

    public void warn(UUID uuid, String reason, String by) {
        warns.computeIfAbsent(uuid, k -> new ArrayList<>())
                .add(new WarnEntry(reason, by, System.currentTimeMillis()));
        saveAsync();
    }

    public List<WarnEntry> getWarns(UUID uuid) {
        return warns.getOrDefault(uuid, List.of());
    }

    public int getWarnCount(UUID uuid) {
        return getWarns(uuid).size();
    }

    public void clearWarns(UUID uuid) {
        warns.remove(uuid);
        saveAsync(); // supprimera la clé UUID du YAML (config reconstruite entièrement)
    }

    private void load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                List<WarnEntry> list = new ArrayList<>();
                for (String i : config.getStringList(key + ".warns")) {
                    String[] parts = i.split("\\|\\|");
                    if (parts.length >= 3)
                        list.add(new WarnEntry(parts[0], parts[1], Long.parseLong(parts[2])));
                }
                if (!list.isEmpty()) warns.put(uuid, list);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Rebuild entier du YAML → supprime automatiquement les entrées dont clearWarns() a été appelé.
     * Save async.
     */
    private void saveAsync() {
        // Snapshot immutable pour le thread async
        Map<UUID, List<String>> snapshot = new HashMap<>();
        warns.forEach((uuid, list) -> {
            List<String> serialized = new ArrayList<>();
            list.forEach(w -> serialized.add(w.reason() + "||" + w.by() + "||" + w.timestamp()));
            snapshot.put(uuid, serialized);
        });

        Bukkit.getScheduler().runTaskAsynchronously(SkyBlockPlugin.getInstance(), () -> {
            YamlConfiguration cfg = new YamlConfiguration();
            snapshot.forEach((uuid, serialized) ->
                    cfg.set(uuid.toString() + ".warns", serialized));
            try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
        });
    }
}