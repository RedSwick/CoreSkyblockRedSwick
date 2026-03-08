package be.RedSwick.skyblock.manager;

import be.RedSwick.skyblock.SkyBlockPlugin;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Location;

import java.io.File;
import java.io.FileInputStream;

/**
 * Colle le schéma d'île de spawn via WorldEdit.
 *
 * CONFIGURATION :
 *   Placer le fichier "spawn_island.schem" dans :
 *   plugins/CoreSkyblock/schematics/spawn_island.schem
 *
 * CRÉATION DU SCHÉMA :
 *   1. Construis ton île de spawn dans le monde
 *   2. Sélectionne-la avec WorldEdit (//wand → //expand vert → //contract)
 *   3. Positionne-toi exactement où le joueur doit spawner (y+1 du sol)
 *   4. //copy
 *   5. //schem save spawn_island
 *   6. Copie le .schem dans plugins/CoreSkyblock/schematics/
 *
 * Le point d'origin du clipboard (ta position lors du //copy) sera
 * collé au centre de l'île (x, y, z de createIsland).
 */
public final class SchematicManager {

    private static final String SCHEMATIC_NAME = "spawn_island.schem";
    private final SkyBlockPlugin plugin;

    public SchematicManager(SkyBlockPlugin plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "schematics");
        if (!dir.exists()) dir.mkdirs();
    }

    /**
     * Colle le schéma centré sur {@code center}.
     * Doit être appelé sur le main thread.
     *
     * @return true si le collage a réussi, false (fallback bedrock+grass)
     */
    public boolean pasteAt(Location center) {
        File schematic = resolveSchematic();
        if (schematic == null) return false;

        ClipboardFormat format = ClipboardFormats.findByFile(schematic);
        if (format == null) {
            plugin.getLogger().warning("[SchematicManager] Format schéma inconnu : " + schematic.getName());
            plugin.getLogger().warning("[SchematicManager] Utilise un fichier .schem (WorldEdit).");
            return false;
        }

        try {
            Clipboard clipboard;
            try (ClipboardReader reader = format.getReader(new FileInputStream(schematic))) {
                clipboard = reader.read();
            }

            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(center.getWorld());
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                Operation op = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(
                                center.getBlockX(),
                                center.getBlockY(),
                                center.getBlockZ()))
                        .ignoreAirBlocks(true)
                        .build();
                Operations.complete(op);
                editSession.flushSession();
            }

            plugin.getLogger().info("[SchematicManager] Schéma collé @ "
                    + center.getBlockX() + "," + center.getBlockY() + "," + center.getBlockZ());
            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("[SchematicManager] Erreur lors du paste : " + e.getMessage());
            return false;
        }
    }

    /** true si le fichier schéma existe et est prêt. */
    public boolean schematicExists() {
        return resolveSchematic() != null;
    }

    private File resolveSchematic() {
        // Priorité 1 : plugins/CoreSkyblock/schematics/spawn_island.schem
        File f1 = new File(new File(plugin.getDataFolder(), "schematics"), SCHEMATIC_NAME);
        if (f1.exists()) return f1;
        // Priorité 2 : plugins/CoreSkyblock/spawn_island.schem
        File f2 = new File(plugin.getDataFolder(), SCHEMATIC_NAME);
        if (f2.exists()) return f2;
        plugin.getLogger().warning("[SchematicManager] Schéma introuvable : " + f1.getAbsolutePath());
        plugin.getLogger().warning("[SchematicManager] Fallback bedrock+grass utilisé.");
        return null;
    }
}
