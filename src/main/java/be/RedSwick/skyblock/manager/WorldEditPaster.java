package be.RedSwick.skyblock.manager;

import be.RedSwick.skyblock.SkyBlockPlugin;
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
 * WorldEditPaster — toutes les dépendances WorldEdit sont ICI.
 *
 * Cette classe n'est chargée par la JVM que lorsqu'elle est référencée
 * au moment de l'exécution. SchematicManager.pasteAt() ne l'appelle
 * qu'après avoir vérifié que WorldEdit est actif → pas de
 * NoClassDefFoundError si WorldEdit est absent.
 */
final class WorldEditPaster {

    private WorldEditPaster() {}

    static boolean paste(SkyBlockPlugin plugin, File schematic, Location center) {
        ClipboardFormat format = ClipboardFormats.findByFile(schematic);
        if (format == null) {
            plugin.getLogger().warning("[SchematicManager] Format de schéma inconnu : " + schematic.getName());
            return false;
        }

        Clipboard clipboard;
        try (ClipboardReader reader = format.getReader(new FileInputStream(schematic))) {
            clipboard = reader.read();
        } catch (Exception e) {
            plugin.getLogger().warning("[SchematicManager] Lecture schéma échouée : " + e.getMessage());
            return false;
        }

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(center.getWorld());
        try (com.sk89q.worldedit.EditSession editSession =
                     WorldEdit.getInstance().newEditSession(weWorld)) {
            Operation op = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(center.getBlockX(), center.getBlockY(), center.getBlockZ()))
                    .ignoreAirBlocks(false)
                    .build();
            Operations.complete(op);
            editSession.flushSession();
        } catch (Exception e) {
            plugin.getLogger().warning("[SchematicManager] Paste échoué : " + e.getMessage());
            return false;
        }

        plugin.getLogger().info("[SchematicManager] Schéma collé @ "
                + center.getBlockX() + "," + center.getBlockY() + "," + center.getBlockZ());
        return true;
    }
}
