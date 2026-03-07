package be.RedSwick.skyblock.customitem;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gère la durée restante des Sacoches de Marchand.
 * Durée = secondes restantes. Décompte uniquement quand le joueur est en ligne
 * ET que la sacoche est dans son inventaire (pas coffre/enderchest).
 */
public class MerchantPouchData {

    private static MerchantPouchData instance;
    public static MerchantPouchData get() {
        if (instance == null) instance = new MerchantPouchData();
        return instance;
    }

    // UUID sacoche → secondes restantes
    private final Map<UUID, Integer> timeLeft = new HashMap<>();
    private YamlConfiguration config;
    private File file;

    public static final int MAX_SECONDS = 3600; // 1 heure

    public void init() {
        JavaPlugin plugin = be.RedSwick.skyblock.SkyBlockPlugin.getInstance();
        file = new File(plugin.getDataFolder(), "merchant_pouch.yml");
        config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try { timeLeft.put(UUID.fromString(key), config.getInt(key)); }
            catch (Exception ignored) {}
        }
    }

    public int getTimeLeft(UUID pouchId) {
        return timeLeft.getOrDefault(pouchId, MAX_SECONDS);
    }

    public void setTimeLeft(UUID pouchId, int seconds) {
        timeLeft.put(pouchId, Math.max(0, seconds));
        config.set(pouchId.toString(), Math.max(0, seconds));
        // Pas de save() ici pour éviter trop d'I/O — save() appelé par le ticker
    }

    public void tick(UUID pouchId) {
        int cur = getTimeLeft(pouchId);
        if (cur > 0) setTimeLeft(pouchId, cur - 1);
    }

    public boolean isExpired(UUID pouchId) {
        return getTimeLeft(pouchId) <= 0;
    }

    public String getFormattedTime(UUID pouchId) {
        int secs = getTimeLeft(pouchId);
        int m = secs / 60;
        int s = secs % 60;
        return String.format("%02d:%02d", m, s);
    }

    public void save() {
        try { config.save(file); } catch (IOException ignored) {}
    }
}