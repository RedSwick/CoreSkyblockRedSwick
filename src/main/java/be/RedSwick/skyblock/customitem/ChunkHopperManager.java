package be.RedSwick.skyblock.customitem;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Gère les Chunk Hoppers placés dans le monde.
 * Stocke leur position, collecte les drops du chunk entier au-dessus.
 */
public class ChunkHopperManager {

    private static ChunkHopperManager instance;
    public static ChunkHopperManager get() {
        if (instance == null) instance = new ChunkHopperManager();
        return instance;
    }

    // Clé "world,cx,cz,y" → location du hopper (x,y,z)
    private final Map<String, Location> hoppers = new HashMap<>();
    // Clé → UUID de l'ArmorStand hologramme
    private final Map<String, java.util.UUID> holoIds = new HashMap<>();
    private YamlConfiguration config;
    private File file;

    public void init() {
        JavaPlugin plugin = be.RedSwick.skyblock.SkyBlockPlugin.getInstance();
        file = new File(plugin.getDataFolder(), "chunk_hoppers.yml");
        config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                String worldName = config.getString(key + ".world");
                int x = config.getInt(key + ".x");
                int y = config.getInt(key + ".y");
                int z = config.getInt(key + ".z");
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    Location l = new Location(world, x, y, z);
                    hoppers.put(key, l);
                    // Restaurer hologramme après chargement (délai pour que le monde soit prêt)
                    String holoIdStr = config.getString(key + ".holoId");
                    if (holoIdStr != null) {
                        try {
                            java.util.UUID holoId = java.util.UUID.fromString(holoIdStr);
                            holoIds.put(key, holoId);
                            // Vérifier si l'ArmorStand existe encore, sinon le recréer
                            final String fKey = key;
                            final Location fLoc = l;
                            Bukkit.getScheduler().runTaskLater(
                                    be.RedSwick.skyblock.SkyBlockPlugin.getInstance(), () -> {
                                        boolean found = world.getEntitiesByClass(ArmorStand.class)
                                                .stream().anyMatch(e -> e.getUniqueId().equals(holoId));
                                        if (!found) spawnHologram(fKey, fLoc);
                                    }, 40L);
                        } catch (Exception ignored) {}
                    } else {
                        // Pas de holo sauvegardé → en créer un
                        final String fKey = key;
                        final Location fLoc = l;
                        Bukkit.getScheduler().runTaskLater(
                                be.RedSwick.skyblock.SkyBlockPlugin.getInstance(),
                                () -> spawnHologram(fKey, fLoc), 40L);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    public void placeHopper(Location loc) {
        String key = chunkKey(loc);
        hoppers.put(key, loc.clone());
        config.set(key + ".world", loc.getWorld().getName());
        config.set(key + ".x", loc.getBlockX());
        config.set(key + ".y", loc.getBlockY());
        config.set(key + ".z", loc.getBlockZ());
        save();
        spawnHologram(key, loc);
    }

    private void spawnHologram(String key, Location loc) {
        // Position hologramme : centre du bloc + 1.5 en Y
        Location holoLoc = loc.clone().add(0.5, 1.5, 0.5);
        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
        stand.setCustomName("§8§lChunk Hopper");
        stand.setCustomNameVisible(true);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setMarker(true);
        stand.setSmall(true);
        stand.setInvulnerable(true);
        stand.addScoreboardTag("arcanium_chunk_hopper_holo"); // protège contre cleanup
        holoIds.put(key, stand.getUniqueId());
        config.set(key + ".holoId", stand.getUniqueId().toString());
        save();
    }

    public boolean removeHopper(Location loc) {
        String key = chunkKey(loc);
        if (!hoppers.containsKey(key)) return false;
        hoppers.remove(key);
        // Détruire le hologramme
        java.util.UUID holoId = holoIds.remove(key);
        if (holoId != null && loc.getWorld() != null) {
            for (ArmorStand e : loc.getWorld().getEntitiesByClass(ArmorStand.class)) {
                if (e.getUniqueId().equals(holoId)) { e.remove(); break; }
            }
        }
        config.set(key, null);
        save();
        return true;
    }

    public boolean isChunkHopper(Location loc) {
        return hoppers.containsKey(chunkKey(loc));
    }

    /** Retourne la location du chunk hopper pour ce chunk, ou null. */
    public Location getHopperInChunk(Chunk chunk) {
        for (Map.Entry<String, Location> e : hoppers.entrySet()) {
            Location loc = e.getValue();
            if (loc.getWorld().equals(chunk.getWorld())
                    && loc.getChunk().getX() == chunk.getX()
                    && loc.getChunk().getZ() == chunk.getZ()) {
                return loc;
            }
        }
        return null;
    }

    /** Collecte un item drop vers le hopper (et le coffre en dessous si présent). */
    public boolean collectItem(Location hopperLoc, ItemStack item) {
        Block block = hopperLoc.getBlock();
        if (block.getType() != Material.HOPPER) return false;

        Hopper hopperState = (Hopper) block.getState();
        Inventory inv = hopperState.getInventory();

        // Essayer d'ajouter dans le hopper
        Map<Integer, ItemStack> leftover = inv.addItem(item.clone());
        return leftover.isEmpty(); // true si tout a été stocké
    }

    private String chunkKey(Location loc) {
        Chunk chunk = loc.getChunk();
        return loc.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ()
                + "," + loc.getBlockY();
    }

    private void save() {
        try { config.save(file); } catch (IOException ignored) {}
    }

    public Collection<Location> getAllHoppers() {
        return hoppers.values();
    }
}