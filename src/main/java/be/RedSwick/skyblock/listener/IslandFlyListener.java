package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.island.Island;
import be.RedSwick.skyblock.island.IslandPermission;
import be.RedSwick.skyblock.manager.IslandManager;
import be.RedSwick.skyblock.player.PlayerData;
import be.RedSwick.skyblock.player.PlayerGrade;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Gère le fly sur l'île :
 *  - Activé si le joueur est sur sa propre île ET a la permission FLY
 *  - Activé pour les grades ARCANIUM, ASCENDANT, NEXUS et staff
 *  - Désactivé automatiquement en quittant l'île
 */
public class IslandFlyListener implements Listener {

    private final IslandManager manager = SkyBlockPlugin.getInstance().getIslandManager();

    // UUIDs des joueurs qui ont le fly d'île activé (pour le retirer en quittant)
    private static final Set<UUID> islandFlyers = new HashSet<>();

    // ════════════════════════════════════════════════
    //  Mouvement — check île à chaque déplacement
    //  (optimisé : seulement si changement de chunk)
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // Optimisation : ne vérifier que si le chunk change
        if (event.getFrom().getBlockX() >> 4 == event.getTo().getBlockX() >> 4
                && event.getFrom().getBlockZ() >> 4 == event.getTo().getBlockZ() >> 4) return;

        Player player = event.getPlayer();
        updateFly(player);
    }

    // ════════════════════════════════════════════════
    //  Téléport — recalcul immédiat
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        org.bukkit.Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                () -> updateFly(event.getPlayer()));
    }

    // ════════════════════════════════════════════════
    //  Join — restaurer fly si nécessaire
    // ════════════════════════════════════════════════

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        org.bukkit.Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                () -> updateFly(event.getPlayer()));
    }

    // ════════════════════════════════════════════════
    //  Quit — couper le fly
    // ════════════════════════════════════════════════

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeFly(event.getPlayer());
        islandFlyers.remove(event.getPlayer().getUniqueId());
    }

    // ════════════════════════════════════════════════
    //  Logique centrale
    // ════════════════════════════════════════════════

    public static void updateFly(Player player) {
        if (player.isOp()) return; // les ops gèrent leur fly eux-mêmes

        IslandManager mgr    = SkyBlockPlugin.getInstance().getIslandManager();
        Island        atLoc  = mgr.getIslandAtLocation(player.getLocation());
        Island        myIsle = mgr.getIslandByMember(player.getUniqueId());

        boolean canFly = false;

        if (atLoc != null && myIsle != null
                && atLoc.getOwner().equals(myIsle.getOwner())) {
            // Sur sa propre île — vérifier permission FLY
            if (myIsle.hasPermission(player.getUniqueId(), IslandPermission.FLY)) {
                // Vérifier le grade
                PlayerData data = SkyBlockPlugin.getInstance()
                        .getPlayerDataManager().get(player.getUniqueId());
                if (data != null) {
                    PlayerGrade grade = data.getGrade();
                    canFly = grade == PlayerGrade.ARCANIUM
                            || grade == PlayerGrade.ASCENDANT
                            || grade == PlayerGrade.NEXUS;
                }
            }
        }

        if (canFly) {
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
                islandFlyers.add(player.getUniqueId());
                // Ne pas notifier à chaque recalcul — seulement au toggle
            }
        } else {
            if (islandFlyers.contains(player.getUniqueId())) {
                removeFly(player);
                islandFlyers.remove(player.getUniqueId());
                player.sendMessage("§c✈ Fly désactivé §7(hors de ton île)");
            }
        }
    }

    private static void removeFly(Player player) {
        if (!player.isOp()) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
    }

    public static boolean isIslandFlying(UUID uuid) {
        return islandFlyers.contains(uuid);
    }
}
