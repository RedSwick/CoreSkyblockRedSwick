package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.island.Island;
import be.RedSwick.skyblock.manager.IslandManager;
import be.RedSwick.skyblock.manager.PlayerDataManager;
import be.RedSwick.skyblock.player.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

public class PlayerDataListener implements Listener {

    private final PlayerDataManager pdm = SkyBlockPlugin.getInstance().getPlayerDataManager();

    // ════════════════════════════════════════════════
    //  JOIN
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        PlayerData data = pdm.loadPlayer(event.getPlayer().getUniqueId());

        event.setJoinMessage(
                data.getGrade().getPrefix()
                        + "§e" + event.getPlayer().getName()
                        + " §7[" + data.getRank().getDisplay() + "§7] a rejoint le serveur."
        );

        // Charger les chunks de l'île du joueur qui rejoint
        IslandManager im = SkyBlockPlugin.getInstance().getIslandManager();
        Island island = im.getIslandByMember(event.getPlayer().getUniqueId());
        if (island != null) {
            Bukkit.getScheduler().runTaskLater(SkyBlockPlugin.getInstance(), () ->
                    loadIslandChunks(island), 5L);
        }

        Bukkit.getScheduler().runTaskLater(SkyBlockPlugin.getInstance(), () ->
                SkyBlockPlugin.getInstance().getBossBarManager()
                        .createBar(event.getPlayer()), 5L);
    }

    // ════════════════════════════════════════════════
    //  QUIT
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        IslandManager im = SkyBlockPlugin.getInstance().getIslandManager();
        Island island = im.getIslandByMember(player.getUniqueId());

        if (island != null) {
            // Vérifier si un autre membre est encore connecté sur l'île
            boolean otherOnline = island.getAllMembers().stream()
                    .filter(uid -> !uid.equals(player.getUniqueId()))
                    .anyMatch(uid -> Bukkit.getPlayer(uid) != null);

            if (!otherOnline) {
                // Délai 1 tick pour laisser le PlayerQuitEvent se terminer proprement
                // avant de modifier le monde
                Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(), () ->
                        shutdownIsland(island));
            }
        }

        pdm.savePlayer(player.getUniqueId());
        SkyBlockPlugin.getInstance().getBossBarManager().removeBar(player);
        pdm.unloadPlayer(player.getUniqueId());
        be.RedSwick.skyblock.command.ModerationCommand.Vanish.vanished
                .remove(player.getUniqueId());
    }

    // ════════════════════════════════════════════════
    //  SHUTDOWN ÎLE — appelé quand plus aucun membre connecté
    //  1. Despawn mobs + hologrammes
    //  2. Désactiver la redstone (setForceLoaded false → chunks inactifs)
    //  3. Décharger les chunks de la RAM
    // ════════════════════════════════════════════════

    public static void shutdownIsland(Island island) {
        despawnIslandEntities(island);

        // Décharger les chunks en async pour ne pas lag le main thread
        Bukkit.getScheduler().runTaskAsynchronously(SkyBlockPlugin.getInstance(), () -> {
            World world = island.getCenter().getWorld();
            if (world == null) return;

            int cx = island.getCenter().getChunk().getX();
            int cz = island.getCenter().getChunk().getZ();
            int chunkRadius = (island.getRadius() / 16) + 2; // +2 de marge

            for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                    final int fcx = cx + dx;
                    final int fcz = cz + dz;

                    // Les opérations sur les chunks doivent être sur le main thread
                    Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(), () -> {
                        // Ne décharger que si le chunk est chargé
                        if (!world.isChunkLoaded(fcx, fcz)) return;

                        Chunk chunk = world.getChunkAt(fcx, fcz);

                        // Vérifier qu'aucun joueur n'est dans ce chunk
                        for (Entity e : chunk.getEntities()) {
                            if (e instanceof Player) return; // joueur présent, ne pas toucher
                        }

                        // Désactiver le force-load (stoppe la redstone + tick)
                        chunk.setForceLoaded(false);

                        // Décharger et sauvegarder le chunk (true = save to disk)
                        world.unloadChunk(fcx, fcz, true);
                    });
                }
            }
        });
    }

    // ════════════════════════════════════════════════
    //  CHARGER LES CHUNKS quand un joueur rejoint son île
    // ════════════════════════════════════════════════

    public static void loadIslandChunks(Island island) {
        World world = island.getCenter().getWorld();
        if (world == null) return;

        int cx = island.getCenter().getChunk().getX();
        int cz = island.getCenter().getChunk().getZ();
        int chunkRadius = (island.getRadius() / 16) + 2;

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                // loadChunk(x, z, generate=false) — ne génère pas de nouveaux chunks
                world.loadChunk(cx + dx, cz + dz, false);
            }
        }
    }

    // ════════════════════════════════════════════════
    //  DESPAWN ENTITÉS de l'île
    //  - Mobs stackés (avec nettoyage stackMap)
    //  - TextDisplay hologrammes
    //  - ArmorStands custom (legacy)
    //  - Items au sol
    // ════════════════════════════════════════════════

    public static void despawnIslandEntities(Island island) {
        var hologramManager = SkyBlockPlugin.getInstance().getHologramManager();
        Location center = island.getCenter();
        double radius = island.getRadius() + 16; // marge d'un chunk

        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, 320, radius)) {
            switch (entity) {
                case Mob mob -> {
                    // Nettoyer l'entrée stackMap pour éviter le memory leak
                    SpawnerListener.removeStackEntry(mob.getUniqueId());
                    hologramManager.removeMobHologram(mob.getUniqueId());
                    mob.remove();
                }
                case TextDisplay td -> {
                    // Hologrammes TextDisplay (nouveaux)
                    hologramManager.removeMobHologram(td.getUniqueId());
                    td.remove();
                }
                case ArmorStand stand -> {
                    // Hologrammes ArmorStand legacy
                    if (stand.isCustomNameVisible() || stand.isMarker()) {
                        hologramManager.removeMobHologram(stand.getUniqueId());
                        stand.remove();
                    }
                }
                case Item item -> {
                    // Items au sol — nettoyage optionnel (évite le lag visuel)
                    item.remove();
                }
                default -> {}
            }
        }
    }

    // Alias pour compatibilité avec l'ancien nom utilisé ailleurs
    public static void despawnIslandMobs(Island island) {
        despawnIslandEntities(island);
    }
}