package be.RedSwick.skyblock.manager;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.island.Island;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.io.File;
import java.io.IOException;

/**
 * Gère les warps d'île et les slots sponsorisés.
 *
 * OPTIMISATION :
 *  - cleanExpired() est appelé UNIQUEMENT par le ticker (toutes les minutes via SkyBlockPlugin)
 *  - Plus appelé sur chaque getSponsoredWarps() / isSponsoredFull() / isAlreadySponsored()
 *  - Zéro I/O dans le hot path du /warp
 */
public class WarpManager {

    private static final int MAX_SPONSORED = 5;

    // Prix des slots sponsorisés — référencés par WarpGUI
    public static final Map<Integer, Long> SPONSOR_PRICES = Map.of(
            1,  10_000L,
            6,  40_000L,
            12, 70_000L,
            24, 120_000L
    );

    public static String formatTimeLeft(long expiresAt) {
        long ms  = expiresAt - System.currentTimeMillis();
        if (ms <= 0) return "Expiré";
        long sec = ms / 1000;
        if (sec < 60)   return sec + "s";
        if (sec < 3600) return (sec / 60) + "m";
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        return m > 0 ? h + "h" + m + "m" : h + "h";
    }

    private final File warpFile;
    private final YamlConfiguration warpConfig;

    // Liste des slots sponsorisés (max 5)
    private final List<SponsoredWarp> sponsoredWarps = new ArrayList<>();

    public record SponsoredWarp(UUID ownerUuid, String ownerName, long expiresAt) {
        /** Alias pour la compatibilité avec WarpGUI qui appelle sw.islandOwner() */
        public UUID islandOwner() { return ownerUuid; }
    }

    public WarpManager() {
        warpFile = new File(SkyBlockPlugin.getInstance().getDataFolder(), "warps.yml");
        if (!warpFile.exists()) {
            try { warpFile.getParentFile().mkdirs(); warpFile.createNewFile(); }
            catch (Exception e) { e.printStackTrace(); }
        }
        warpConfig = YamlConfiguration.loadConfiguration(warpFile);
        loadSponsored();
    }

    // ════════════════════════════════════════════════
    //  NETTOYAGE EXPIRÉS — appelé par ticker uniquement
    // ════════════════════════════════════════════════

    /** Appelé par SkyBlockPlugin toutes les minutes — plus dans le hot path. */
    public void cleanExpired() {
        boolean changed = sponsoredWarps.removeIf(w -> System.currentTimeMillis() > w.expiresAt());
        if (changed) saveSponsored();
    }

    // ════════════════════════════════════════════════
    //  GETTERS — lecture directe sans I/O
    // ════════════════════════════════════════════════

    /** Retourne la liste courante (nettoyage uniquement via ticker). */
    public List<SponsoredWarp> getSponsoredWarps() {
        return Collections.unmodifiableList(sponsoredWarps);
    }

    public boolean isSponsoredFull() {
        return sponsoredWarps.size() >= MAX_SPONSORED;
    }

    public boolean isAlreadySponsored(UUID uuid) {
        for (SponsoredWarp w : sponsoredWarps) {
            if (w.ownerUuid().equals(uuid)) return true;
        }
        return false;
    }

    // ════════════════════════════════════════════════
    //  SPONSOR — ajouter un warp sponsorisé
    // ════════════════════════════════════════════════

    public boolean addSponsored(UUID uuid, String name, long durationMs, long cost) {
        PlayerDataManager pdm = SkyBlockPlugin.getInstance().getPlayerDataManager();
        var data = pdm.get(uuid);
        if (data == null) return false;
        if (data.getCoins() < cost) return false;
        if (isSponsoredFull() || isAlreadySponsored(uuid)) return false;

        data.addCoins(-cost);
        pdm.savePlayer(uuid);

        long expiresAt = System.currentTimeMillis() + durationMs;
        sponsoredWarps.add(new SponsoredWarp(uuid, name, expiresAt));
        saveSponsored();
        return true;
    }

    public void removeSponsored(UUID uuid) {
        sponsoredWarps.removeIf(w -> w.ownerUuid().equals(uuid));
        saveSponsored();
    }

    // ════════════════════════════════════════════════
    //  PERSISTANCE SPONSORISÉS
    // ════════════════════════════════════════════════

    private void loadSponsored() {
        sponsoredWarps.clear();
        List<Map<?, ?>> list = warpConfig.getMapList("sponsored");
        long now = System.currentTimeMillis();
        for (Map<?, ?> entry : list) {
            try {
                UUID uuid      = UUID.fromString((String) entry.get("uuid"));
                String name    = (String) entry.get("name");
                long expiresAt = ((Number) entry.get("expiresAt")).longValue();
                if (expiresAt > now) {
                    sponsoredWarps.add(new SponsoredWarp(uuid, name, expiresAt));
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveSponsored() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (SponsoredWarp w : sponsoredWarps) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("uuid",      w.ownerUuid().toString());
            m.put("name",      w.ownerName());
            m.put("expiresAt", w.expiresAt());
            list.add(m);
        }
        warpConfig.set("sponsored", list);
        // Save async
        Bukkit.getScheduler().runTaskAsynchronously(SkyBlockPlugin.getInstance(), () -> {
            try { warpConfig.save(warpFile); } catch (IOException ignored) {}
        });
    }

    // ════════════════════════════════════════════════
    //  WARPS ÎLE — lookup via IslandManager
    // ════════════════════════════════════════════════

    /** Retourne toutes les îles ayant un warp public, triées par IS Level. */
    public List<Island> getPublicWarps() {
        IslandManager im = SkyBlockPlugin.getInstance().getIslandManager();
        List<Island> result = new ArrayList<>();
        for (Island island : im.getAllIslands()) {
            if (island.hasWarp() && island.isOpen()) result.add(island);
        }
        result.sort((a, b) -> Double.compare(b.getIsLevel(), a.getIsLevel()));
        return result;
    }

    public Location getSpawnLocation() {
        org.bukkit.World world = Bukkit.getWorld("skyblock");
        if (world == null) return null;
        double x = warpConfig.getDouble("spawn.x", 0);
        double y = warpConfig.getDouble("spawn.y", 100);
        double z = warpConfig.getDouble("spawn.z", 0);
        return new Location(world, x, y, z);
    }

    public void setSpawnLocation(Location loc) {
        warpConfig.set("spawn.x", loc.getX());
        warpConfig.set("spawn.y", loc.getY());
        warpConfig.set("spawn.z", loc.getZ());
        Bukkit.getScheduler().runTaskAsynchronously(SkyBlockPlugin.getInstance(), () -> {
            try { warpConfig.save(warpFile); } catch (IOException ignored) {}
        });
    }
}