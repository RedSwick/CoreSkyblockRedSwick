package be.RedSwick.skyblock.manager;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.player.*;
import be.RedSwick.skyblock.player.PlayerJob;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Collection;

public class PlayerDataManager {

    private final Map<UUID, PlayerData> cache = new HashMap<>();
    private final File dataFolder;

    // Dirty flag : UUIDs à sauvegarder au prochain flush
    private final Set<UUID> dirtyPlayers = new HashSet<>();

    public PlayerDataManager() {
        dataFolder = new File(SkyBlockPlugin.getInstance().getDataFolder(), "playerdata");
        if (!dataFolder.exists()) dataFolder.mkdirs();
        startAutoSave();
    }

    // ════════════════════════════════════════════════
    //  AUTO-SAVE ASYNC — toutes les 5 minutes
    // ════════════════════════════════════════════════

    private void startAutoSave() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                SkyBlockPlugin.getInstance(),
                this::flushDirty,
                6000L, 6000L
        );
    }

    private void flushDirty() {
        if (dirtyPlayers.isEmpty()) return;
        Set<UUID> toSave = new HashSet<>(dirtyPlayers);
        dirtyPlayers.clear();
        for (UUID uuid : toSave) {
            savePlayerSync(uuid);
        }
    }

    // ════════════════════════════════════════════════
    //  LOAD / GET
    // ════════════════════════════════════════════════

    public PlayerData loadPlayer(UUID uuid) {
        if (cache.containsKey(uuid)) return cache.get(uuid);

        File file = getFile(uuid);
        PlayerData data = new PlayerData(uuid);

        if (file.exists()) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

            data.setCoins(cfg.getLong("coins", 0));
            data.setEssence(cfg.getLong("essence", 0));
            data.setGems(cfg.getLong("gems", 0));
            data.setTotalEssenceEarned(cfg.getLong("totalEssenceEarned", 0));

            String rankStr = cfg.getString("rank", PlayerRank.SERVITEUR.name());
            try { data.setRank(PlayerRank.valueOf(rankStr)); }
            catch (Exception e) { data.setRank(PlayerRank.SERVITEUR); }

            String gradeStr = cfg.getString("grade", PlayerGrade.AUCUN.name());
            try { data.setGrade(PlayerGrade.valueOf(gradeStr)); }
            catch (Exception e) { data.setGrade(PlayerGrade.AUCUN); }

            data.setLevel(cfg.getInt("level", 1));
            data.setXp(cfg.getLong("xp", 0));

            if (cfg.isConfigurationSection("jobs")) {
                for (PlayerJob job : PlayerJob.values()) {
                    String key = job.name();
                    data.setJobLevel(job, cfg.getInt("jobs." + key + ".level", 1));
                    data.setJobXp(job,   cfg.getLong("jobs." + key + ".xp",    0));
                }
            }

            data.setMobsKilled(cfg.getLong("stats.mobsKilled", 0));
            data.setBlocksPlaced(cfg.getLong("stats.blocksPlaced", 0));
            data.setBlocksBroken(cfg.getLong("stats.blocksBroken", 0));
            data.setItemsCrafted(cfg.getLong("stats.itemsCrafted", 0));
            data.setFishCaught(cfg.getLong("stats.fishCaught", 0));
            data.setLogsCut(cfg.getLong("stats.logsCut", 0));
            data.setPlaytimeTicks(cfg.getLong("stats.playtimeTicks", 0));
            data.setTotalCoinsEarned(cfg.getLong("stats.totalCoinsEarned", 0));
            data.setCropsBroken(cfg.getLong("stats.cropsBroken", 0));
        }

        cache.put(uuid, data);
        return data;
    }

    public PlayerData get(UUID uuid) {
        return cache.get(uuid);
    }

    // ════════════════════════════════════════════════
    //  SAVE — async uniquement, dirty flag
    // ════════════════════════════════════════════════

    /**
     * Marque le joueur comme dirty → sera sauvé lors du prochain flush async.
     * Ne bloque plus jamais le main thread.
     */
    public void savePlayer(UUID uuid) {
        if (cache.containsKey(uuid)) {
            dirtyPlayers.add(uuid);
        }
    }

    /**
     * Force une sauvegarde immédiate async (ex: quit joueur).
     */
    public void savePlayerNow(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return;
        dirtyPlayers.remove(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(
                SkyBlockPlugin.getInstance(),
                () -> savePlayerSync(uuid)
        );
    }

    private void savePlayerSync(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return;

        File file = getFile(uuid);
        FileConfiguration cfg = new YamlConfiguration();

        cfg.set("coins",              data.getCoins());
        cfg.set("essence",            data.getEssence());
        cfg.set("gems",               data.getGems());
        cfg.set("totalEssenceEarned", data.getTotalEssenceEarned());
        cfg.set("rank",               data.getRank().name());
        cfg.set("grade",              data.getGrade().name());
        cfg.set("level",              data.getLevel());
        cfg.set("xp",                 data.getXp());

        for (PlayerJob job : PlayerJob.values()) {
            String key = job.name();
            cfg.set("jobs." + key + ".level", data.getJobLevel(job));
            cfg.set("jobs." + key + ".xp",    data.getJobXp(job));
        }

        cfg.set("stats.mobsKilled",       data.getMobsKilled());
        cfg.set("stats.blocksPlaced",     data.getBlocksPlaced());
        cfg.set("stats.blocksBroken",     data.getBlocksBroken());
        cfg.set("stats.itemsCrafted",     data.getItemsCrafted());
        cfg.set("stats.fishCaught",       data.getFishCaught());
        cfg.set("stats.logsCut",          data.getLogsCut());
        cfg.set("stats.playtimeTicks",    data.getPlaytimeTicks());
        cfg.set("stats.totalCoinsEarned", data.getTotalCoinsEarned());
        cfg.set("stats.cropsBroken",      data.getCropsBroken());

        try { cfg.save(file); }
        catch (IOException e) { e.printStackTrace(); }
    }

    public Collection<PlayerData> getAll() {
        return cache.values();
    }

    /**
     * Retourne les données de TOUS les joueurs (connectés + déconnectés).
     * Exécuté en ASYNC uniquement (appelé depuis LeaderboardManager.refreshAll()).
     * Lit les fichiers sur disque pour les joueurs hors-ligne.
     */
    public Collection<PlayerData> getAllFromDisk() {
        Map<UUID, PlayerData> result = new HashMap<>(cache);
        File[] files = dataFolder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return result.values();
        for (File file : files) {
            try {
                String name = file.getName().replace(".yml", "");
                UUID uuid = UUID.fromString(name);
                if (result.containsKey(uuid)) continue;
                PlayerData data = new PlayerData(uuid);
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                data.setCoins(cfg.getLong("coins", 0));
                data.setLevel(cfg.getInt("level", 1));
                data.setMobsKilled(cfg.getLong("stats.mobsKilled", 0));
                data.setBlocksBroken(cfg.getLong("stats.blocksBroken", 0));
                data.setFishCaught(cfg.getLong("stats.fishCaught", 0));
                data.setLogsCut(cfg.getLong("stats.logsCut", 0));
                data.setPlaytimeTicks(cfg.getLong("stats.playtimeTicks", 0));
                data.setCropsBroken(cfg.getLong("stats.cropsBroken", 0));
                for (PlayerJob job : PlayerJob.values()) {
                    data.setJobLevel(job, cfg.getInt("jobs." + job.name() + ".level", 0));
                }
                result.put(uuid, data);
            } catch (Exception ignored) {}
        }
        return result.values();
    }

    public void saveAll() {
        // Flush dirty + sauvegarder tous les joueurs en cache (shutdown)
        for (UUID uuid : cache.keySet()) {
            savePlayerSync(uuid);
        }
        dirtyPlayers.clear();
    }

    public void unloadPlayer(UUID uuid) {
        // S'assurer que les données sont sauvées avant de décharger
        if (dirtyPlayers.remove(uuid)) {
            savePlayerSync(uuid);
        }
        cache.remove(uuid);
    }

    private File getFile(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }
}