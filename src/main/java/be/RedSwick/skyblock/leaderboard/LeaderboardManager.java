package be.RedSwick.skyblock.leaderboard;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.island.Island;
import be.RedSwick.skyblock.manager.PlayerDataManager;
import be.RedSwick.skyblock.player.PlayerData;
import be.RedSwick.skyblock.player.PlayerJob;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Gère les classements et leurs hologrammes de classement au spawn.
 * OPTIMISATIONS :
 *  - TextDisplay à la place des ArmorStands (zéro AI, zéro entité physique)
 *  - Calcul du top10 en ASYNC → zéro I/O sur le main thread
 *  - Cache des résultats : le refresh crée le cache async, l'affichage lit le cache
 */
public class LeaderboardManager {

    private static final int TOP_SIZE = 10;

    // TextDisplay (un par ligne de holo)
    private final Map<LeaderboardType, List<TextDisplay>> holoDisplays  = new EnumMap<>(LeaderboardType.class);
    private final Map<LeaderboardType, Location>          holoLocations = new EnumMap<>(LeaderboardType.class);

    // Cache du top10 calculé en async
    private final Map<LeaderboardType, List<LeaderboardEntry>> cachedTop = new EnumMap<>(LeaderboardType.class);

    private YamlConfiguration config;
    private File configFile;
    private final SkyBlockPlugin plugin;

    public LeaderboardManager(SkyBlockPlugin plugin) {
        this.plugin = plugin;
        configFile = new File(plugin.getDataFolder(), "leaderboard_holos.yml");
        config     = YamlConfiguration.loadConfiguration(configFile);
        loadHoloLocations();
    }

    // ════════════════════════════════════════════════
    //  CALCUL DU TOP 10 — en ASYNC via cache
    // ════════════════════════════════════════════════

    /** Retourne le top depuis le cache (jamais de calcul synchrone). */
    public List<LeaderboardEntry> getTop(LeaderboardType type) {
        return cachedTop.getOrDefault(type, List.of());
    }

    /**
     * Recalcule le top10 en async, puis actualise le hologramme sur le main thread.
     * Appelé par refreshAll() toutes les 5 minutes.
     */
    private void refreshTopAsync(LeaderboardType type) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<LeaderboardEntry> entries = computeTop(type);
            // Retour sur le main thread pour mettre à jour le hologramme
            Bukkit.getScheduler().runTask(plugin, () -> {
                cachedTop.put(type, entries);
                Location loc = holoLocations.get(type);
                if (loc != null) spawnHolo(type, loc);
            });
        });
    }

    private List<LeaderboardEntry> computeTop(LeaderboardType type) {
        PlayerDataManager pdm = plugin.getPlayerDataManager();
        List<LeaderboardEntry> entries = new ArrayList<>();

        if (type == LeaderboardType.NIVEAU_ILE) {
            for (Island island : plugin.getIslandManager().getAllIslands()) {
                UUID owner = island.getOwner();
                String name = Bukkit.getOfflinePlayer(owner).getName();
                if (name == null) name = owner.toString().substring(0, 8);
                entries.add(new LeaderboardEntry(name, (long) island.getIsLevel()));
            }
        } else {
            for (PlayerData data : pdm.getAllFromDisk()) {
                String name = Bukkit.getOfflinePlayer(data.getUuid()).getName();
                if (name == null) name = data.getUuid().toString().substring(0, 8);
                long value = getValue(data, type);
                entries.add(new LeaderboardEntry(name, value));
            }
        }

        entries.sort((a, b) -> Long.compare(b.value(), a.value()));
        return entries.subList(0, Math.min(TOP_SIZE, entries.size()));
    }

    private long getValue(PlayerData data, LeaderboardType type) {
        return switch (type) {
            case COINS              -> data.getCoins();
            case LEVEL              -> data.getLevel();
            case BLOCS_MINES        -> data.getBlocksBroken();
            case MOBS_TUES          -> data.getMobsKilled();
            case TEMPS_JEU          -> data.getPlaytimeMinutes();
            case POISSONS_PECHES    -> data.getFishCaught();
            case BUCHES_COUPEES     -> data.getLogsCut();
            case CULTURES_RECOLTEES -> data.getCropsBroken();
            case JOB_CHASSEUR       -> data.getJobLevel(PlayerJob.CHASSEUR);
            case JOB_FARMER         -> data.getJobLevel(PlayerJob.FARMER);
            case JOB_MINER          -> data.getJobLevel(PlayerJob.MINER);
            case JOB_BUCHERON       -> data.getJobLevel(PlayerJob.BUCHERON);
            case JOB_ALCHIMISTE     -> data.getJobLevel(PlayerJob.ALCHIMISTE);
            case JOB_PECHEUR        -> data.getJobLevel(PlayerJob.PECHEUR);
            default                 -> 0;
        };
    }

    public String formatValue(LeaderboardType type, long value) {
        return switch (type) {
            case TEMPS_JEU -> {
                long h = value / 60; long m = value % 60;
                yield h > 0 ? h + "h" + m + "m" : m + "min";
            }
            case COINS -> String.format("%,d", value) + " ⬡";
            default    -> String.format("%,d", value);
        };
    }

    // ════════════════════════════════════════════════
    //  HOLOGRAMMES — TextDisplay (Paper 1.19.4+)
    // ════════════════════════════════════════════════

    public void spawnHolo(LeaderboardType type, Location baseLoc) {
        removeHolo(type);
        holoLocations.put(type, baseLoc.clone());
        saveHoloLocation(type, baseLoc);

        List<TextDisplay> displays = new ArrayList<>();
        List<LeaderboardEntry> top = getTop(type);

        double spacing    = 0.28;
        int    totalLines = 2 + TOP_SIZE;
        double startY     = baseLoc.getY() + (totalLines - 1) * spacing;

        // Titre
        displays.add(spawnTextDisplay(baseLoc.getWorld(),
                baseLoc.getX(), startY, baseLoc.getZ(),
                "§8▬▬▬ " + type.getDisplay() + " §8▬▬▬"));
        // Espace
        displays.add(spawnTextDisplay(baseLoc.getWorld(),
                baseLoc.getX(), startY - spacing, baseLoc.getZ(), " "));

        String[] medals = {"§6§l①","§7§l②","§8§l③","§f④","§f⑤","§f⑥","§f⑦","§f⑧","§f⑨","§f⑩"};
        for (int i = 0; i < TOP_SIZE; i++) {
            String line;
            if (i < top.size()) {
                LeaderboardEntry e = top.get(i);
                line = medals[i] + " §f" + e.name() + " §8— " + type.getColor() + formatValue(type, e.value());
            } else {
                line = medals[i] + " §8---";
            }
            displays.add(spawnTextDisplay(baseLoc.getWorld(),
                    baseLoc.getX(), startY - (i + 2) * spacing, baseLoc.getZ(), line));
        }

        holoDisplays.put(type, displays);
    }

    public void refreshHolo(LeaderboardType type) {
        Location loc = holoLocations.get(type);
        if (loc == null) return;
        // Recalcul async + respawn du holo sur le main thread
        refreshTopAsync(type);
    }

    public void removeHolo(LeaderboardType type) {
        List<TextDisplay> old = holoDisplays.remove(type);
        if (old != null) old.forEach(d -> { if (d != null && !d.isDead()) d.remove(); });
    }

    public void refreshAll() {
        // Sauvegarde des joueurs déjà gérée par PlayerDataManager auto-save
        for (LeaderboardType type : new ArrayList<>(holoLocations.keySet())) {
            refreshTopAsync(type);
        }
    }

    public void removeAll() {
        for (LeaderboardType type : new ArrayList<>(holoDisplays.keySet())) {
            removeHolo(type);
        }
    }

    public boolean   hasHolo(LeaderboardType type)      { return holoLocations.containsKey(type); }
    public Location  getHoloLocation(LeaderboardType t)  { return holoLocations.get(t); }

    // ════════════════════════════════════════════════
    //  PERSISTANCE
    // ════════════════════════════════════════════════

    private void loadHoloLocations() {
        for (String key : config.getKeys(false)) {
            try {
                LeaderboardType type = LeaderboardType.fromId(key);
                if (type == null) continue;
                String worldName = config.getString(key + ".world");
                double x = config.getDouble(key + ".x");
                double y = config.getDouble(key + ".y");
                double z = config.getDouble(key + ".z");
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;
                holoLocations.put(type, new Location(world, x, y, z));
            } catch (Exception ignored) {}
        }
        // Spawn après chargement du monde (différé + calcul async)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Map.Entry<LeaderboardType, Location> e : holoLocations.entrySet()) {
                refreshTopAsync(e.getKey());
            }
        }, 80L);
    }

    private void saveHoloLocation(LeaderboardType type, Location loc) {
        String key = type.getId();
        config.set(key + ".world", loc.getWorld().getName());
        config.set(key + ".x", loc.getX());
        config.set(key + ".y", loc.getY());
        config.set(key + ".z", loc.getZ());
        // Save async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { config.save(configFile); } catch (IOException ignored) {}
        });
    }

    public void deleteHolo(LeaderboardType type) {
        removeHolo(type);
        config.set(type.getId(), null);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { config.save(configFile); } catch (IOException ignored) {}
        });
        holoLocations.remove(type);
    }

    // ════════════════════════════════════════════════
    //  HELPERS — TextDisplay
    // ════════════════════════════════════════════════

    private TextDisplay spawnTextDisplay(World world, double x, double y, double z, String text) {
        Location loc = new Location(world, x, y, z);
        return world.spawn(loc, TextDisplay.class, display -> {
            display.setText(text);
            display.setGravity(false);
            display.setPersistent(false);
            display.setBillboard(Display.Billboard.CENTER);
            display.setViewRange(32f);
        });
    }

    public record LeaderboardEntry(String name, long value) {}
}