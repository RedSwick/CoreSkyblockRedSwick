package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.island.Island;
import be.RedSwick.skyblock.island.IslandFlag;
import be.RedSwick.skyblock.island.IslandPermission;
import be.RedSwick.skyblock.island.IslandUpgrade;
import be.RedSwick.skyblock.manager.IslandManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;

public class ProtectionListener implements Listener {

    private final IslandManager manager = SkyBlockPlugin.getInstance().getIslandManager();

    // ════════════════════════════════════════════════
    //  POSE / CASSE de blocs
    // ════════════════════════════════════════════════

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player   player = event.getPlayer();
        Location loc    = event.getBlock().getLocation();
        if (!isSkyblock(loc)) return;

        Island island = manager.getIslandAtLocation(loc);
        if (island == null) { event.setCancelled(true); return; }

        if (!island.hasPermission(player.getUniqueId(), IslandPermission.BREAK_BLOCK)) {
            player.sendMessage("§cTu ne peux pas casser ici !");
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player   player = event.getPlayer();
        Location loc    = event.getBlock().getLocation();
        if (!isSkyblock(loc)) return;

        Island island = manager.getIslandAtLocation(loc);
        if (island == null) { event.setCancelled(true); return; }

        if (!island.hasPermission(player.getUniqueId(), IslandPermission.PLACE_BLOCK)) {
            player.sendMessage("§cTu ne peux pas poser ici !");
            event.setCancelled(true);
        }
    }

    // ════════════════════════════════════════════════
    //  PISTONS — bloquer s'ils poussent hors de l'île
    // ════════════════════════════════════════════════

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!isSkyblock(event.getBlock().getLocation())) return;
        Island island = manager.getIslandAtLocation(event.getBlock().getLocation());
        if (island == null) { event.setCancelled(true); return; }

        for (Block pushed : event.getBlocks()) {
            Location dest = pushed.getRelative(event.getDirection()).getLocation();
            Island destIsland = manager.getIslandAtLocation(dest);
            if (destIsland == null || !destIsland.getOwner().equals(island.getOwner())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!isSkyblock(event.getBlock().getLocation())) return;
        Island island = manager.getIslandAtLocation(event.getBlock().getLocation());
        if (island == null) { event.setCancelled(true); return; }

        for (Block pulled : event.getBlocks()) {
            Location dest = pulled.getRelative(event.getDirection()).getLocation();
            Island destIsland = manager.getIslandAtLocation(dest);
            if (destIsland == null || !destIsland.getOwner().equals(island.getOwner())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // ════════════════════════════════════════════════
    //  LIQUIDES — empêcher coulée hors île
    // ════════════════════════════════════════════════

    @EventHandler(ignoreCancelled = true)
    public void onFlowFrom(BlockFromToEvent event) {
        Block block = event.getBlock();
        if (!isSkyblock(block.getLocation())) return;
        if (block.getType() != Material.WATER && block.getType() != Material.LAVA) return;

        Location from = block.getLocation();
        Location to   = event.getToBlock().getLocation();

        Island fromIsland = manager.getIslandAtLocation(from);
        Island toIsland   = manager.getIslandAtLocation(to);

        // Couler hors de toute île → annuler
        if (toIsland == null) { event.setCancelled(true); return; }
        // Couler d'une île à une autre (différentes) → annuler
        if (fromIsland != null && !fromIsland.getOwner().equals(toIsland.getOwner())) {
            event.setCancelled(true);
        }
    }

    // ════════════════════════════════════════════════
    //  EXPLOSION — bloquer TNT selon flag île
    // ════════════════════════════════════════════════

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (!isSkyblock(event.getLocation())) return;
        Island island = manager.getIslandAtLocation(event.getLocation());
        // Si pas sur île ou flag TNT désactivé → annuler les dégâts blocs
        if (island == null || !island.getFlag(IslandFlag.TNT)) {
            event.blockList().clear();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!isSkyblock(event.getBlock().getLocation())) return;
        Island island = manager.getIslandAtLocation(event.getBlock().getLocation());
        if (island == null || !island.getFlag(IslandFlag.TNT)) {
            event.blockList().clear();
        }
    }

    // ════════════════════════════════════════════════
    //  FEU — flag FIRE_SPREAD
    // ════════════════════════════════════════════════

    @EventHandler(ignoreCancelled = true)
    public void onFireSpread(BlockSpreadEvent event) {
        if (event.getNewState().getType() != Material.FIRE) return;
        if (!isSkyblock(event.getBlock().getLocation())) return;
        Island island = manager.getIslandAtLocation(event.getBlock().getLocation());
        if (island != null && !island.getFlag(IslandFlag.FIRE_SPREAD)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFireIgnite(BlockIgniteEvent event) {
        if (!isSkyblock(event.getBlock().getLocation())) return;
        if (event.getCause() == BlockIgniteEvent.IgniteCause.SPREAD ||
                event.getCause() == BlockIgniteEvent.IgniteCause.LIGHTNING) {
            Island island = manager.getIslandAtLocation(event.getBlock().getLocation());
            if (island != null && !island.getFlag(IslandFlag.FIRE_SPREAD)) {
                event.setCancelled(true);
            }
        }
    }

    // ════════════════════════════════════════════════
    //  FEUILLES — flag LEAF_DECAY
    // ════════════════════════════════════════════════

    @EventHandler(ignoreCancelled = true)
    public void onLeafDecay(LeavesDecayEvent event) {
        if (!isSkyblock(event.getBlock().getLocation())) return;
        Island island = manager.getIslandAtLocation(event.getBlock().getLocation());
        if (island != null && !island.getFlag(IslandFlag.LEAF_DECAY)) {
            event.setCancelled(true);
        }
    }

    // ════════════════════════════════════════════════
    //  PVP — flag PVP
    // ════════════════════════════════════════════════

    @EventHandler(ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player)) return;
        if (!isSkyblock(victim.getLocation())) return;

        Island island = manager.getIslandAtLocation(victim.getLocation());
        if (island != null && !island.getFlag(IslandFlag.PVP)) {
            event.setCancelled(true);
        }
    }

    // ════════════════════════════════════════════════
    //  DÉGÂTS MOBS — flag MOB_DAMAGE
    // ════════════════════════════════════════════════

    @EventHandler(ignoreCancelled = true)
    public void onMobDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (event.getDamager() instanceof Player) return; // géré par onPvp
        if (!isSkyblock(victim.getLocation())) return;

        Island island = manager.getIslandAtLocation(victim.getLocation());
        if (island != null && !island.getFlag(IslandFlag.MOB_DAMAGE)) {
            event.setCancelled(true);
        }
    }

    // ════════════════════════════════════════════════
    //  SPAWN NATUREL — flag MOB_SPAWNING
    // ════════════════════════════════════════════════

    @EventHandler(ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (!isSkyblock(event.getLocation())) return;
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) return; // spawners toujours ok
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM)   return; // spawns custom ok

        Island island = manager.getIslandAtLocation(event.getLocation());
        if (island != null && !island.getFlag(IslandFlag.MOB_SPAWNING)) {
            event.setCancelled(true);
        }
    }

    // ════════════════════════════════════════════════
    //  ÉLEVAGE — flag ANIMAL_BREEDING
    // ════════════════════════════════════════════════

    @EventHandler(ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!isSkyblock(event.getMother().getLocation())) return;
        Island island = manager.getIslandAtLocation(event.getMother().getLocation());
        if (island != null && !island.getFlag(IslandFlag.ANIMAL_BREEDING)) {
            event.setCancelled(true);
        }
    }

    // ════════════════════════════════════════════════
    //  DROPS VISITEURS
    // ════════════════════════════════════════════════

    @EventHandler(ignoreCancelled = true)
    public void onVisitorDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!isSkyblock(player.getLocation())) return;
        Island island = manager.getIslandAtLocation(player.getLocation());
        if (island == null) return;
        if (island.isVisitor(player.getUniqueId()) && !island.getFlag(IslandFlag.VISITOR_DROP)) {
            event.setCancelled(true);
            player.sendMessage("§cLes visiteurs ne peuvent pas dropper d'objets ici.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onVisitorPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isSkyblock(player.getLocation())) return;
        Island island = manager.getIslandAtLocation(player.getLocation());
        if (island == null) return;
        if (island.isVisitor(player.getUniqueId()) && !island.getFlag(IslandFlag.VISITOR_PICKUP)) {
            event.setCancelled(true);
        }
    }

    // ════════════════════════════════════════════════
    //  MOUVEMENT — empêcher sortie île pour membres
    // ════════════════════════════════════════════════

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isSkyblock(player.getLocation())) return;

        Island island = manager.getIslandByMember(player.getUniqueId());
        if (island == null) return;

        Location to     = event.getTo();
        Location center = island.getCenter();
        int radius      = island.getUpgradeValue(IslandUpgrade.SIZE); // ← upgrade SIZE, pas getRadius()

        if (Math.abs(to.getBlockX() - center.getBlockX()) > radius ||
                Math.abs(to.getBlockZ() - center.getBlockZ()) > radius) {
            event.setCancelled(true);
            player.sendMessage("§cTu ne peux pas quitter ton île !");
        }
    }

    // ════════════════════════════════════════════════
    //  HELPER
    // ════════════════════════════════════════════════

    private boolean isSkyblock(Location loc) {
        return loc.getWorld() != null && loc.getWorld().getName().equals("skyblock");
    }
}