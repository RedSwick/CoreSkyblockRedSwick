package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.island.Island;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Keep inventory + respawn sur l'île du joueur.
 */
public class DeathRespawnListener implements Listener {

    // ── Keep Inventory ───────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        // Garder tout le stuff
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    // ── Respawn sur l'île ─────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player p = event.getPlayer();

        Island island = SkyBlockPlugin.getInstance()
                .getIslandManager().getIslandByMember(p.getUniqueId());

        if (island == null) return; // pas d'île → spawn vanilla

        Location center = island.getCenter();
        if (center == null || center.getWorld() == null) return;

        // Chercher un bloc solide au-dessus du centre de l'île
        Location spawn = findSafeSpawn(center);
        if (spawn != null) event.setRespawnLocation(spawn);
    }

    private Location findSafeSpawn(Location center) {
        // Chercher de haut en bas un endroit safe (air dessus + sol solide en dessous)
        for (int y = center.getBlockY() + 10; y >= center.getBlockY() - 5; y--) {
            Location feet = center.clone();
            feet.setY(y);
            Location head = feet.clone().add(0, 1, 0);
            if (feet.getBlock().getType().isSolid()) continue; // feet doit être air
            if (head.getBlock().getType().isSolid()) continue; // head doit être air
            Location ground = feet.clone().subtract(0, 1, 0);
            if (ground.getBlock().getType().isSolid()) {
                // Position valide — centrer sur le bloc
                return feet.clone().add(0.5, 0, 0.5);
            }
        }
        // Fallback : y=center+5
        return center.clone().add(0.5, 5, 0.5);
    }
}