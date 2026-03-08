package be.RedSwick.skyblock.manager;

import be.RedSwick.skyblock.SkyBlockPlugin;
import org.bukkit.Location;

import java.io.File;

/**
 * SchematicManager — aucune dépendance directe WorldEdit ici.
 *
 * Le code WorldEdit est isolé dans WorldEditPaster.java.
 * Cette classe n'importe aucune classe WorldEdit → elle se charge
 * toujours sans erreur, même si WorldEdit n'est PAS installé.
 *
 * FORMATS SUPPORTÉS : .schem / .schematic (WorldEdit)
 * NON SUPPORTÉ     : .litematic (Litematica) → convertir avec //schem save
 *
 * EMPLACEMENTS RECHERCHÉS (par ordre de priorité) :
 *   1. plugins/CoreSkyblock/schematics/spawn_island.schem
 *   2. plugins/CoreSkyblock/spawn_island.schem
 *   3. plugins/WorldEdit/schematics/<noms connus>.schem
 *   4. plugins/WorldEdit/schematics/<premier .schem trouvé>
 */
public final class SchematicManager {

    private static final String SCHEMATIC_NAME = "spawn_island.schem";
    private final SkyBlockPlugin plugin;
    private final boolean worldEditAvailable;

    public SchematicManager(SkyBlockPlugin plugin) {
        this.plugin = plugin;
        // Crée le dossier schematics si absent
        new File(plugin.getDataFolder(), "schematics").mkdirs();
        // Vérifie si WorldEdit est chargé sur le serveur
        this.worldEditAvailable = plugin.getServer().getPluginManager()
                .isPluginEnabled("WorldEdit");
        if (worldEditAvailable) {
            plugin.getLogger().info("[SchematicManager] WorldEdit détecté — schéma activé.");
        } else {
            plugin.getLogger().info("[SchematicManager] WorldEdit absent — fallback bedrock+grass.");
        }
    }

    /**
     * Colle le schéma centré sur {@code center}.
     * Doit être appelé sur le main thread.
     *
     * @return true si le collage a réussi, false = fallback bedrock+grass
     */
    public boolean pasteAt(Location center) {
        if (!worldEditAvailable) return false;

        File schematic = resolveSchematic();
        if (schematic == null) return false;

        // WorldEditPaster est chargé ICI, seulement si WorldEdit est actif
        try {
            return WorldEditPaster.paste(plugin, schematic, center);
        } catch (Throwable t) {
            plugin.getLogger().warning("[SchematicManager] Erreur paste : " + t.getMessage());
            return false;
        }
    }

    /** true si le fichier schéma existe. */
    public boolean schematicExists() {
        return resolveSchematic() != null;
    }

    private File resolveSchematic() {
        // 1) plugins/CoreSkyblock/schematics/<nom>
        File ownDir = new File(plugin.getDataFolder(), "schematics");
        File f1 = new File(ownDir, SCHEMATIC_NAME);
        if (f1.exists()) return f1;

        // 2) plugins/CoreSkyblock/<nom>
        File f2 = new File(plugin.getDataFolder(), SCHEMATIC_NAME);
        if (f2.exists()) return f2;

        // 3) plugins/WorldEdit/schematics/ — noms connus
        org.bukkit.plugin.Plugin wePlugin =
                plugin.getServer().getPluginManager().getPlugin("WorldEdit");
        if (wePlugin != null) {
            File weDir = new File(wePlugin.getDataFolder(), "schematics");
            for (String name : new String[]{ SCHEMATIC_NAME, "ile_skyblock.schem",
                    "island.schem", "spawn_island.schem", "spawn.schem" }) {
                File f = new File(weDir, name);
                if (f.exists()) return f;
            }
            // 4) N'importe quel .schem dans plugins/WorldEdit/schematics/
            if (weDir.exists()) {
                File[] all = weDir.listFiles((d, n) -> n.endsWith(".schem") || n.endsWith(".schematic"));
                if (all != null && all.length > 0) {
                    plugin.getLogger().info("[SchematicManager] Schéma auto-détecté : " + all[0].getName());
                    return all[0];
                }
            }
        }

        plugin.getLogger().warning("[SchematicManager] Aucun schéma .schem trouvé."
                + " Placez votre fichier dans plugins/WorldEdit/schematics/ (format .schem)"
                + " — le format .litematic n'est PAS supporté, utilisez //schem save en jeu.");
        return null;
    }
}
