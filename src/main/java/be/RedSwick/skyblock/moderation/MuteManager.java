package be.RedSwick.skyblock.moderation;

import be.RedSwick.skyblock.SkyBlockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gère les mutes en mémoire + persistance YAML.
 *
 * BUG FIX : les mutes étaient perdus au restart (in-memory uniquement).
 * Maintenant sauvegardés dans mutes.yml, chargés au démarrage.
 */
public class MuteManager {

    private static final MuteManager INSTANCE = new MuteManager();
    public static MuteManager get() { return INSTANCE; }

    private final Map<UUID, Long>   muteEnd    = new HashMap<>();
    private final Map<UUID, String> muteReason = new HashMap<>();

    private File                file;
    private YamlConfiguration   config;

    public void init() {
        file = new File(SkyBlockPlugin.getInstance().getDataFolder(), "mutes.yml");
        if (!file.exists()) {
            try { file.getParentFile().mkdirs(); file.createNewFile(); }
            catch (Exception e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    // ════════════════════════════════════════════════
    //  API
    // ════════════════════════════════════════════════

    public void mute(UUID uuid, long durationMs, String reason) {
        long end = System.currentTimeMillis() + durationMs;
        muteEnd.put(uuid, end);
        muteReason.put(uuid, reason);
        saveAsync();
    }

    public void unmute(UUID uuid) {
        muteEnd.remove(uuid);
        muteReason.remove(uuid);
        saveAsync();
    }

    public boolean isMuted(UUID uuid) {
        Long end = muteEnd.get(uuid);
        if (end == null) return false;
        if (System.currentTimeMillis() >= end) {
            // Expiration silencieuse — ne pas sauvegarder à chaque check
            muteEnd.remove(uuid);
            muteReason.remove(uuid);
            return false;
        }
        return true;
    }

    public long getRemainingMs(UUID uuid) {
        Long end = muteEnd.get(uuid);
        if (end == null) return 0;
        return Math.max(0, end - System.currentTimeMillis());
    }

    public String getReason(UUID uuid) {
        return muteReason.getOrDefault(uuid, "Aucune raison");
    }

    public String formatRemaining(UUID uuid) {
        long ms = getRemainingMs(uuid);
        long sec = ms / 1000;
        if (sec < 60)   return sec + "s";
        if (sec < 3600) return (sec / 60) + "m";
        return (sec / 3600) + "h" + ((sec % 3600) / 60) + "m";
    }

    // ════════════════════════════════════════════════
    //  PERSISTANCE
    // ════════════════════════════════════════════════

    private void load() {
        long now = System.currentTimeMillis();
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid    = UUID.fromString(key);
                long end     = config.getLong(key + ".end", 0);
                String reason = config.getString(key + ".reason", "Aucune raison");
                if (end > now) {
                    muteEnd.put(uuid, end);
                    muteReason.put(uuid, reason);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveAsync() {
        // Reconstruire la config depuis les maps en mémoire
        config = new YamlConfiguration();
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : muteEnd.entrySet()) {
            if (entry.getValue() > now) {
                String key = entry.getKey().toString();
                config.set(key + ".end",    entry.getValue());
                config.set(key + ".reason", muteReason.getOrDefault(entry.getKey(), "Aucune raison"));
            }
        }
        final YamlConfiguration snapshot = config;
        Bukkit.getScheduler().runTaskAsynchronously(SkyBlockPlugin.getInstance(), () -> {
            try { snapshot.save(file); } catch (IOException ignored) {}
        });
    }
}