package be.RedSwick.skyblock.manager;

import org.bukkit.*;
import org.bukkit.entity.EntityType;
import be.RedSwick.skyblock.SkyBlockPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnerManager {

    public record SpawnerData(EntityType type, int count) {}

    private final Map<String, SpawnerData> spawners = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> spawnersByChunk = new ConcurrentHashMap<>();

    public SpawnerManager() {
        startCleanupTimer();
    }

    private void startCleanupTimer() {
        // Nettoyage des chunks déchargés toutes les 10 min
        Bukkit.getScheduler().runTaskTimer(SkyBlockPlugin.getInstance(), this::cleanupUnloadedChunks, 12000L, 12000L);
    }

    private void cleanupUnloadedChunks() {
        spawnersByChunk.entrySet().removeIf(entry -> {
            long chunkKey = entry.getKey();
            int cx = (int) (chunkKey >> 32);
            int cz = (int) chunkKey;
            World world = Bukkit.getWorld("skyblock");
            if (world == null) return true;
            return !world.isChunkLoaded(cx, cz);
        });
    }

    public void addSpawner(Location loc, EntityType type, int count) {
        String key = toKey(loc);
        spawners.put(key, new SpawnerData(type, count));
        long chunkKey = chunkKey(loc);
        spawnersByChunk.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet()).add(key);
    }

    public void removeSpawner(Location loc) {
        String key = toKey(loc);
        spawners.remove(key);
        long chunkKey = chunkKey(loc);
        Set<String> set = spawnersByChunk.get(chunkKey);
        if (set != null) {
            set.remove(key);
            if (set.isEmpty()) spawnersByChunk.remove(chunkKey);
        }
    }

    public Map<String, SpawnerData> getAllSpawners() {
        return spawners;
    }

    public Set<String> getSpawnersInChunk(Chunk chunk) {
        return spawnersByChunk.getOrDefault(chunkKey(chunk), Collections.emptySet());
    }

    public static String toKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public static Location fromKey(String key) {
        String[] parts = key.split(":");
        if (parts.length != 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            return new Location(world,
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private long chunkKey(Location loc) {
        int x = loc.getChunk().getX();
        int z = loc.getChunk().getZ();
        return (((long) x) << 32) | (z & 0xffffffffL);
    }

    private long chunkKey(Chunk chunk) {
        int x = chunk.getX();
        int z = chunk.getZ();
        return (((long) x) << 32) | (z & 0xffffffffL);
    }
}