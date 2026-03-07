package be.RedSwick.skyblock.manager;

import org.bukkit.*;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnerManager {

    public record SpawnerData(EntityType type, int count) {}

    private final Map<String, SpawnerData> spawners = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> spawnersByChunk = new ConcurrentHashMap<>();

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
        if (set != null) set.remove(key);
    }

    public Map<String, SpawnerData> getAllSpawners() {
        return spawners;
    }

    public Set<String> getSpawnersInChunk(Chunk chunk) {
        int x = chunk.getX();
        int z = chunk.getZ();
        long key = (((long) x) << 32) | (z & 0xffffffffL);
        return spawnersByChunk.getOrDefault(key, Collections.emptySet());
    }

    public static String toKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public static Location fromKey(String key) {

        String[] parts = key.split(":");
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;

        return new Location(
                world,
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3])
        );
    }

    private long chunkKey(Location loc) {
        int x = loc.getChunk().getX();
        int z = loc.getChunk().getZ();
        return (((long) x) << 32) | (z & 0xffffffffL);
    }
}