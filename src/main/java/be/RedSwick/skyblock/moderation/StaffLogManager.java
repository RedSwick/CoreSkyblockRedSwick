package be.RedSwick.skyblock.moderation;

import be.RedSwick.skyblock.SkyBlockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Enregistre les actions de modération des staffs.
 * Conserve uniquement les 3 derniers jours.
 *
 * OPTIMISATION :
 *  - save() est désormais async → plus de I/O synchrone sur le main thread
 *  - getActiveStaff() utilise un cache de vrai-noms pour éviter le double-stream nested
 */
public class StaffLogManager {

    private static final StaffLogManager INSTANCE = new StaffLogManager();
    public static StaffLogManager get() { return INSTANCE; }

    private static final long THREE_DAYS_MS = 3L * 24 * 60 * 60 * 1000;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault());

    public record LogEntry(String staffName, String action, String target, String detail, long timestamp) {
        public String format() {
            String time = FMT.format(Instant.ofEpochMilli(timestamp));
            return "§8[" + time + "] §f" + action + " §7→ §e" + target
                    + (detail.isEmpty() ? "" : " §8— §7" + detail);
        }
    }

    private final Map<String, List<LogEntry>> logs       = new HashMap<>();
    private final Map<String, String>         staffRanks = new HashMap<>();
    // Cache : lowercase → nom original (casse correcte) — évite le double stream
    private final Map<String, String>         realNames  = new HashMap<>();

    private File               file;
    private YamlConfiguration  config;

    public void init() {
        file = new File(SkyBlockPlugin.getInstance().getDataFolder(), "stafflogs.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (Exception e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    public void log(String staffName, String action, String target, String detail) {
        String key = staffName.toLowerCase();
        List<LogEntry> list = logs.computeIfAbsent(key, k -> new ArrayList<>());
        list.add(new LogEntry(staffName, action, target, detail, System.currentTimeMillis()));
        realNames.put(key, staffName); // mise à jour nom réel
        purgeOld(list);

        org.bukkit.entity.Player p = Bukkit.getPlayer(staffName);
        if (p != null) staffRanks.put(key, StaffRank.of(p).name());

        saveAsync();
    }

    public StaffRank getStoredRank(String staffName) {
        String stored = staffRanks.get(staffName.toLowerCase());
        if (stored == null) return StaffRank.NONE;
        try { return StaffRank.valueOf(stored); }
        catch (Exception e) { return StaffRank.NONE; }
    }

    public List<LogEntry> getLogs(String staffName) {
        List<LogEntry> list = logs.getOrDefault(staffName.toLowerCase(), List.of());
        long cutoff = System.currentTimeMillis() - THREE_DAYS_MS;
        return list.stream()
                .filter(e -> e.timestamp() > cutoff)
                .sorted(Comparator.comparingLong(LogEntry::timestamp).reversed())
                .toList();
    }

    /**
     * Retourne les noms des staffs ayant des logs récents.
     * OPTIMISATION : utilise realNames cache → plus de double-stream nested.
     */
    public Set<String> getActiveStaff() {
        long cutoff = System.currentTimeMillis() - THREE_DAYS_MS;
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, List<LogEntry>> entry : logs.entrySet()) {
            String key = entry.getKey();
            List<LogEntry> list = entry.getValue();
            for (LogEntry e : list) {
                if (e.timestamp() > cutoff) {
                    // Utiliser le vrai nom depuis le cache → O(1)
                    String real = realNames.getOrDefault(key, e.staffName());
                    result.add(real);
                    break; // un seul log suffit
                }
            }
        }
        return result;
    }

    private void purgeOld(List<LogEntry> list) {
        long cutoff = System.currentTimeMillis() - THREE_DAYS_MS;
        list.removeIf(e -> e.timestamp() <= cutoff);
    }

    private void load() {
        for (String staff : config.getKeys(false)) {
            List<LogEntry> list = new ArrayList<>();
            for (String raw : config.getStringList(staff + ".logs")) {
                try {
                    String[] p = raw.split("\\|\\|");
                    if (p.length >= 4) {
                        LogEntry entry = new LogEntry(staff, p[0], p[1], p[2], Long.parseLong(p[3]));
                        list.add(entry);
                    }
                } catch (Exception ignored) {}
            }
            purgeOld(list);
            if (!list.isEmpty()) {
                logs.put(staff, list);
                // Initialiser le cache nom réel depuis le premier log
                list.stream().findFirst().ifPresent(e -> realNames.put(staff, e.staffName()));
            }
            String rank = config.getString(staff + ".rank");
            if (rank != null) staffRanks.put(staff, rank);
        }
    }

    private void saveAsync() {
        // Snapshot des données pour éviter les ConcurrentModificationException en async
        Map<String, List<String>> snapshot = new HashMap<>();
        Map<String, String> ranksSnapshot  = new HashMap<>(staffRanks);

        logs.forEach((staff, list) -> {
            List<String> serialized = new ArrayList<>();
            list.forEach(e -> serialized.add(
                    e.action() + "||" + e.target() + "||" + e.detail() + "||" + e.timestamp()));
            snapshot.put(staff, serialized);
        });

        Bukkit.getScheduler().runTaskAsynchronously(SkyBlockPlugin.getInstance(), () -> {
            YamlConfiguration cfg = new YamlConfiguration();
            snapshot.forEach((staff, serialized) -> {
                cfg.set(staff + ".logs", serialized);
                if (ranksSnapshot.containsKey(staff))
                    cfg.set(staff + ".rank", ranksSnapshot.get(staff));
            });
            try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
        });
    }
}