package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.island.Island;
import be.RedSwick.skyblock.island.IslandMission;
import be.RedSwick.skyblock.island.IslandMission.Category;
import be.RedSwick.skyblock.manager.IslandManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;

public class MissionListener implements Listener {

    private final IslandManager islandManager =
            SkyBlockPlugin.getInstance().getIslandManager();

    // ══════════════════════════════════════════════════════
    //  AGRICULTURE & BÛCHERON & MINEUR — BlockBreakEvent
    // ══════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        Block  block  = event.getBlock();
        Material mat  = block.getType();

        Island island = islandManager.getIslandByMember(player.getUniqueId());
        if (island == null) return;

        int amount = getBlockAmount(block);
        if (amount <= 0) return;

        for (IslandMission mission : IslandMission.values()) {
            if (island.isMissionCompleted(mission)) continue;
            if (mission.getTarget().isMob()) continue;

            Material target = mission.getTarget().material();
            if (target == null) continue;

            if (matches(mat, target)) {
                boolean wasDone = island.isMissionCompleted(mission);
                island.addMissionProgress(mission, amount);
                if (!wasDone && island.isMissionCompleted(mission)) {
                    notifyCompletion(player, island, mission);
                }
            }
        }

        // ← CORRECTION : saveIsland à la place de markDirty
        islandManager.saveIsland(island);
    }

    // ══════════════════════════════════════════════════════
    //  CHASSEUR — EntityDeathEvent
    // ══════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {

        if (!(event.getEntity().getKiller() instanceof Player player)) return;

        EntityType type = event.getEntityType();

        // Récupère le compteur de stack si présent (MobStackListener)
        int count = event.getEntity().getMetadata("stack_count")
                .stream().findFirst()
                .map(v -> v.asInt())
                .orElse(1);

        Island island = islandManager.getIslandByMember(player.getUniqueId());
        if (island == null) return;

        for (IslandMission mission : IslandMission.values()) {
            if (mission.getCategory() != Category.CHASSEUR) continue;
            if (island.isMissionCompleted(mission)) continue;

            EntityType target = mission.getTarget().entityType();
            if (target == null || target != type) continue;

            boolean wasDone = island.isMissionCompleted(mission);
            island.addMissionProgress(mission, count);
            if (!wasDone && island.isMissionCompleted(mission)) {
                notifyCompletion(player, island, mission);
            }
        }

        // ← CORRECTION : saveIsland à la place de markDirty
        islandManager.saveIsland(island);
    }

    // ─────────────────────────────────────────────
    //  Combien d'unités rapporte un bloc cassé
    // ─────────────────────────────────────────────

    private int getBlockAmount(Block block) {
        return switch (block.getType()) {
            case SUGAR_CANE              -> countSugarcaneHeight(block);
            case BAMBOO, BAMBOO_SAPLING  -> countBambooHeight(block);

            case WHEAT, CARROT, POTATO, BEETROOT,
                 NETHER_WART, COCOA -> {
                if (block.getBlockData() instanceof Ageable a && a.getAge() == a.getMaximumAge())
                    yield 1;
                yield 0;
            }

            case PUMPKIN, MELON, MELON_STEM,
                 SWEET_BERRY_BUSH, CAVE_VINES, CAVE_VINES_PLANT,
                 CHORUS_FLOWER, CHORUS_PLANT -> 1;

            case OAK_LOG, BIRCH_LOG, SPRUCE_LOG, JUNGLE_LOG,
                 ACACIA_LOG, DARK_OAK_LOG, MANGROVE_LOG,
                 CHERRY_LOG, CRIMSON_STEM, WARPED_STEM -> 1;

            case STONE, COBBLESTONE, DEEPSLATE,
                 COAL_ORE, DEEPSLATE_COAL_ORE,
                 IRON_ORE, DEEPSLATE_IRON_ORE,
                 GOLD_ORE, DEEPSLATE_GOLD_ORE,
                 LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                 REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
                 DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                 EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                 ANCIENT_DEBRIS -> 1;

            default -> 0;
        };
    }

    private int countSugarcaneHeight(Block base) {
        int count = 0;
        Block current = base;
        while (current.getType() == Material.SUGAR_CANE) {
            count++;
            current = current.getRelative(org.bukkit.block.BlockFace.UP);
        }
        return Math.max(1, count);
    }

    private int countBambooHeight(Block base) {
        int count = 0;
        Block current = base;
        while (current.getType() == Material.BAMBOO
                || current.getType() == Material.BAMBOO_SAPLING) {
            count++;
            current = current.getRelative(org.bukkit.block.BlockFace.UP);
        }
        return Math.max(1, count);
    }

    private boolean matches(Material broke, Material target) {
        if (broke == target) return true;
        return switch (target) {
            case MELON_SLICE   -> broke == Material.MELON;
            case SWEET_BERRIES -> broke == Material.SWEET_BERRY_BUSH || broke == Material.CAVE_VINES;
            case GLOW_BERRIES  -> broke == Material.CAVE_VINES || broke == Material.CAVE_VINES_PLANT;
            case CHORUS_FRUIT  -> broke == Material.CHORUS_FLOWER || broke == Material.CHORUS_PLANT;
            default            -> false;
        };
    }

    // ─────────────────────────────────────────────
    //  Notification completion
    // ─────────────────────────────────────────────

    private void notifyCompletion(Player player, Island island, IslandMission mission) {
        for (UUID mid : island.getAllMembers()) {
            Player member = Bukkit.getPlayer(mid);
            if (member == null) continue;
            member.sendTitle(
                    "§6§l✦ Mission complétée !",
                    "§e" + mission.getName() + " §7(§b+" + fmt(mission.getIsLevelReward()) + " IS§7)",
                    10, 60, 20
            );
            member.sendMessage("§6§l[Mission] §r§a" + mission.getName()
                    + " §7complétée ! §b+" + fmt(mission.getIsLevelReward())
                    + " IS §7→ Total : §e" + fmt(island.getIsLevel()));
            member.playSound(member.getLocation(),
                    Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
    }

    private String fmt(double v) {
        if (v >= 1_000_000) return String.format("%.0fM", v / 1_000_000);
        if (v >= 1_000)     return String.format("%.0fk", v / 1_000);
        return String.format("%.0f", v);
    }
}