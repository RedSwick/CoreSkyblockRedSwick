package be.RedSwick.skyblock.customitem;

import be.RedSwick.skyblock.SkyBlockPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * OPTIMISATIONS :
 *
 *  getHopperInChunk() — LE PLUS CRITIQUE :
 *    Avant : scan linéaire O(n) de tous les hoppers à chaque ItemSpawnEvent
 *    500 joueurs qui farm = milliers d'ItemSpawnEvent/sec × N hoppers = serveur mort
 *    Après : Map<Long, Location> chunkIndex → lookup O(1), zéro boucle
 *
 *  save() synchrone → dirty flag + flush async toutes les 2min
 *
 *  ArmorStand → TextDisplay :
 *    Avant : getEntitiesByClass() scan tout le monde au démarrage + à la suppression
 *    Après : UUID stocké en mémoire → world.getEntity(uuid) O(1), zéro scan
 *            setPersistent(false) → disparaît au restart, recréé par init()
 */
public class ChunkHopperManager {

    private static ChunkHopperManager instance;
    public static ChunkHopperManager get() {
        if (instance == null) instance = new ChunkHopperManager();
        return instance;
    }

    // INDEX O(1) : chunkKey long → Location hopper
    private final Map<Long,   Location> chunkIndex = new HashMap<>();
    // Clé string (persist YAML) → Location
    private final Map<String, Location> hoppers    = new HashMap<>();
    // Clé string → UUID du TextDisplay (mémoire seulement)
    private final Map<String, UUID>     holoIds    = new HashMap<>();

    private YamlConfiguration config;
    private File               file;
    private boolean            dirty = false;

    // ════════════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════════════

    public void init() {
        file = new File(SkyBlockPlugin.getInstance().getDataFolder(), "chunk_hoppers.yml");
        if (!file.exists()) {
            try { file.getParentFile().mkdirs(); file.createNewFile(); }
            catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);

        for (String key : config.getKeys(false)) {
            try {
                World world = Bukkit.getWorld(config.getString(key + ".world"));
                if (world == null) continue;
                Location loc = new Location(world,
                        config.getInt(key + ".x"),
                        config.getInt(key + ".y"),
                        config.getInt(key + ".z"));
                hoppers.put(key, loc);
                chunkIndex.put(chunkKey(loc.getChunk()), loc.clone());
                // TextDisplay recréé directement — pas de scan monde
                final String fKey = key;
                final Location fLoc = loc;
                Bukkit.getScheduler().runTaskLater(SkyBlockPlugin.getInstance(),
                        () -> spawnHologram(fKey, fLoc), 40L);
            } catch (Exception ignored) {}
        }

        // Flush async si dirty toutes les 2 minutes
        Bukkit.getScheduler().runTaskTimerAsynchronously(SkyBlockPlugin.getInstance(), () -> {
            if (dirty) { dirty = false; asyncSave(); }
        }, 2400L, 2400L);
    }

    // ════════════════════════════════════════════════
    //  PLACEMENT / SUPPRESSION
    // ════════════════════════════════════════════════

    public void placeHopper(Location loc) {
        String key = stringKey(loc);
        hoppers.put(key, loc.clone());
        chunkIndex.put(chunkKey(loc.getChunk()), loc.clone());
        config.set(key + ".world", loc.getWorld().getName());
        config.set(key + ".x",    loc.getBlockX());
        config.set(key + ".y",    loc.getBlockY());
        config.set(key + ".z",    loc.getBlockZ());
        dirty = true;
        spawnHologram(key, loc);
    }

    public boolean removeHopper(Location loc) {
        String key = stringKey(loc);
        if (!hoppers.containsKey(key)) return false;
        hoppers.remove(key);
        chunkIndex.remove(chunkKey(loc.getChunk()));
        // Supprimer le TextDisplay par UUID — O(1), zéro scan monde
        UUID id = holoIds.remove(key);
        if (id != null && loc.getWorld() != null) {
            var entity = loc.getWorld().getEntity(id);
            if (entity != null) entity.remove();
        }
        config.set(key, null);
        dirty = true;
        return true;
    }

    public boolean isChunkHopper(Location loc) {
        return hoppers.containsKey(stringKey(loc));
    }

    // ════════════════════════════════════════════════
    //  LOOKUP O(1) — appelé à CHAQUE ItemSpawnEvent
    // ════════════════════════════════════════════════

    public Location getHopperInChunk(Chunk chunk) {
        return chunkIndex.get(chunkKey(chunk));
    }

    // ════════════════════════════════════════════════
    //  COLLECTE
    // ════════════════════════════════════════════════

    public boolean collectItem(Location hopperLoc, ItemStack item) {
        Block block = hopperLoc.getBlock();
        if (block.getType() != Material.HOPPER) return false;
        Inventory inv = ((Hopper) block.getState()).getInventory();
        return inv.addItem(item.clone()).isEmpty();
    }

    public Collection<Location> getAllHoppers() { return hoppers.values(); }

    // ════════════════════════════════════════════════
    //  HOLOGRAMME — TextDisplay, zéro entité physique
    // ════════════════════════════════════════════════

    private void spawnHologram(String key, Location loc) {
        // Supprimer l'ancien si existant — O(1) par UUID
        UUID old = holoIds.remove(key);
        if (old != null && loc.getWorld() != null) {
            var existing = loc.getWorld().getEntity(old);
            if (existing != null) existing.remove();
        }

        TextDisplay td = loc.getWorld().spawn(loc.clone().add(0.5, 1.6, 0.5), TextDisplay.class, d -> {
            d.setText("§8§lChunk Hopper");
            d.setBillboard(TextDisplay.Billboard.CENTER);
            d.setPersistent(false); // restart = disparu = recréé par init()
            d.setDefaultBackground(false);
            d.setShadowed(true);
            d.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1.2f, 1.2f, 1.2f),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
        });
        holoIds.put(key, td.getUniqueId());
    }

    // ════════════════════════════════════════════════
    //  PERSISTANCE
    // ════════════════════════════════════════════════

    private void asyncSave() {
        final YamlConfiguration snap = config;
        final File f = file;
        Bukkit.getScheduler().runTaskAsynchronously(SkyBlockPlugin.getInstance(), () -> {
            try { snap.save(f); } catch (IOException ignored) {}
        });
    }

    /** Sauvegarde forcée au shutdown depuis SkyBlockPlugin.onDisable(). */
    public void saveNow() {
        try { config.save(file); } catch (IOException ignored) {}
    }

    // ════════════════════════════════════════════════
    //  CLÉS
    // ════════════════════════════════════════════════

    /** Clé long O(1) : encode monde + chunkX + chunkZ. */
    private static long chunkKey(Chunk chunk) {
        long wh = chunk.getWorld().getName().hashCode() & 0xFFFFL;
        long cx = chunk.getX() & 0xFFFFFFL;
        long cz = chunk.getZ() & 0xFFFFFFL;
        return (wh << 48) | (cx << 24) | cz;
    }

    private static String stringKey(Location loc) {
        Chunk c = loc.getChunk();
        return loc.getWorld().getName() + "," + c.getX() + "," + c.getZ() + "," + loc.getBlockY();
    }
}