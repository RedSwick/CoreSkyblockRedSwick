package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.island.Island;
import be.RedSwick.skyblock.manager.IslandManager;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.Location;

/**
 * Limite les cadres (ItemFrame + GlowItemFrame) à 75 par île.
 *
 * Pourquoi une limite ?
 *  - Chaque cadre est une entité persistante avec rendu côté client
 *  - 300 joueurs × îles pleines de cadres = charge réseau + CPU render importantes
 *  - 75 cadres/île = largement suffisant pour décorer des coffres, afficher des sets, etc.
 */
public class ItemFrameLimitListener implements Listener {

    private static final int MAX_FRAMES_PER_ISLAND = 75;

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        Entity entity = event.getEntity();

        // Seulement pour les cadres
        if (!(entity instanceof ItemFrame) && !(entity instanceof GlowItemFrame)) return;

        Player player = event.getPlayer();
        if (player == null) return;

        // Admin bypass
        if (player.hasPermission("arcanium.admin")) return;

        IslandManager im = SkyBlockPlugin.getInstance().getIslandManager();
        Location loc = entity.getLocation();
        Island island = im.getIslandAtLocation(loc);
        if (island == null) return;

        // Compter les cadres existants sur l'île
        int count = countFrames(island, loc.getWorld());
        if (count >= MAX_FRAMES_PER_ISLAND) {
            event.setCancelled(true);
            player.sendMessage("§c✗ §fCette île a atteint la limite de §e"
                    + MAX_FRAMES_PER_ISLAND + " cadres §fmaximum !");
        }
    }

    /**
     * Compte les cadres dans la zone de l'île.
     * Utilise getNearbyEntitiesByType() avec le rayon de l'île — Paper API ciblée.
     */
    private int countFrames(Island island, org.bukkit.World world) {
        Location center = island.getCenter();
        int radius = island.getRadius();

        int count = 0;
        // Chercher dans un cube couvrant l'île entière
        for (Entity e : world.getNearbyEntities(center, radius, 256, radius)) {
            if (e instanceof ItemFrame || e instanceof GlowItemFrame) count++;
        }
        return count;
    }
}