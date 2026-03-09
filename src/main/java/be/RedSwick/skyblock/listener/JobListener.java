package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.job.JobManager;
import be.RedSwick.skyblock.job.JobXpTable;
import be.RedSwick.skyblock.job.JobXpTable.JobAction;
import be.RedSwick.skyblock.player.PlayerJob;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import java.util.Set;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

public class JobListener implements Listener {

    private final JobManager jobManager =
            SkyBlockPlugin.getInstance().getJobManager();

    // ══════════════════════════════════════════════════════
    //  FARMER + BÛCHERON + MINEUR — BlockBreakEvent
    // ══════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        org.bukkit.Location loc = event.getBlock().getLocation();
        String key = be.RedSwick.skyblock.listener.CustomItemListener.locKey(loc);
        be.RedSwick.skyblock.listener.CustomItemListener.playerPlaced.add(key);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        Block block   = event.getBlock();
        Material mat  = block.getType();

        boolean cancelled = event.isCancelled();

        // ── STAT blocs minés ──
        var statData = SkyBlockPlugin.getInstance().getPlayerDataManager().get(player.getUniqueId());
        if (!cancelled && statData != null && JobXpTable.MINER_BLOCKS.containsKey(mat)) {
            statData.incrementBlocksBroken();
        }

        // ── FARMER ──
        // NOTE: JobXpTable.FARMER_CROPS contient les Material de BLOCS (CARROTS, POTATOES, etc.)
        // XP donnée si :
        //   - event non cancelled = main nue, ou outil vanilla
        //   - event cancelled mais déjà géré = la houe custom a déjà donné l'XP via triggerFarmerJob()
        if (JobXpTable.FARMER_CROPS.containsKey(mat)) {
            if (statData != null) statData.incrementCropsBroken();
            // Seulement si event non cancelled (houe custom gère elle-même l'XP via triggerFarmerJob)
            if (!cancelled && isMatureCrop(block)) {
                // Anti-exploit : blocs non-ageables posés par le joueur (canne, cactus, bambou…)
                if (!(block.getBlockData() instanceof Ageable)) {
                    String locKey = be.RedSwick.skyblock.listener.CustomItemListener.locKey(block.getLocation());
                    if (be.RedSwick.skyblock.listener.CustomItemListener.playerPlaced.remove(locKey)) {
                        return; // Posé par le joueur → pas de récompense
                    }
                }
                JobAction action = JobXpTable.FARMER_CROPS.get(mat);
                jobManager.rewardAction(player, PlayerJob.FARMER,
                        action.xp(), action.coins());
            }
            return;
        }

        // ── BÛCHERON — main nue OU hache vanilla (hache custom gère ses propres XP) ──
        if (!cancelled && JobXpTable.BUCHERON_LOGS.containsKey(mat)) {
            if (!be.RedSwick.skyblock.listener.CustomItemListener.playerPlaced
                    .remove(be.RedSwick.skyblock.listener.CustomItemListener.locKey(block.getLocation()))) {
                if (statData != null) statData.incrementLogsCut();
                JobAction action = JobXpTable.BUCHERON_LOGS.get(mat);
                jobManager.rewardAction(player, PlayerJob.BUCHERON,
                        action.xp(), action.coins());
            }
            return;
        }

        // ── MINEUR ──
        if (!cancelled && JobXpTable.MINER_BLOCKS.containsKey(mat)) {
            if (!be.RedSwick.skyblock.listener.CustomItemListener.playerPlaced
                    .remove(be.RedSwick.skyblock.listener.CustomItemListener.locKey(block.getLocation()))) {
                JobAction action = JobXpTable.MINER_BLOCKS.get(mat);
                jobManager.rewardAction(player, PlayerJob.MINER,
                        action.xp(), action.coins());
            }
        }
    }

    // ══════════════════════════════════════════════════════
    //  CHASSEUR — EntityDeathEvent
    // ══════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {

        if (!(event.getEntity().getKiller() instanceof Player player)) return;

        EntityType type = event.getEntityType();
        if (!JobXpTable.CHASSEUR_MOBS.containsKey(type)) return;

        // Le stack est maintenant toujours 1 ici — les kills précédents sont récompensés
        // dans MobStackListener.onDamage au moment du déstack
        be.RedSwick.skyblock.listener.MobStackListener.handledKills
                .remove(event.getEntity().getUniqueId());
        int stack = 1;

        JobAction action = JobXpTable.CHASSEUR_MOBS.get(type);
        double essenceAmount = JobXpTable.CHASSEUR_ESSENCE.getOrDefault(type, 0.0);

        if (essenceAmount > 0) {
            var data = SkyBlockPlugin.getInstance()
                    .getPlayerDataManager().get(player.getUniqueId());
            if (data != null) data.addEssence(Math.round(essenceAmount * stack));
        }

        jobManager.rewardActionChasseur(player, action.xp() * stack, action.coins() * stack, essenceAmount * stack);
    }

    // ══════════════════════════════════════════════════════
    //  PÊCHEUR — PlayerFishEvent
    // ══════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {

        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!(event.getCaught() instanceof org.bukkit.entity.Item caughtItem)) return;

        Player player = event.getPlayer();
        Material mat  = caughtItem.getItemStack().getType();
        if (!JobXpTable.PECHEUR_FISH.containsKey(mat)) return;

        var fData = SkyBlockPlugin.getInstance().getPlayerDataManager().get(player.getUniqueId());
        if (fData != null) fData.incrementFishCaught();

        JobAction action = JobXpTable.PECHEUR_FISH.get(mat);
        jobManager.rewardAction(player, PlayerJob.PECHEUR, action.xp(), action.coins());
    }

    // ══════════════════════════════════════════════════════
    //  ALCHIMISTE — BrewEvent
    // ══════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {

        org.bukkit.Location loc = event.getBlock().getLocation();

        for (org.bukkit.entity.Entity entity :
                loc.getWorld().getNearbyEntities(loc, 5, 5, 5)) {

            if (!(entity instanceof Player player)) continue;

            var contents = event.getContents();
            for (int i = 0; i < 3; i++) {
                ItemStack result = contents.getItem(i);
                if (result == null) continue;

                Material mat = result.getType();
                if (!JobXpTable.ALCHIMISTE_POTIONS.containsKey(mat)) continue;

                JobAction action = JobXpTable.ALCHIMISTE_POTIONS.get(mat);
                double multiplier = getPotionMultiplier(result);
                jobManager.rewardAction(player, PlayerJob.ALCHIMISTE,
                        action.xp() * multiplier, action.coins() * multiplier);
            }
            break; // Un seul joueur récompensé
        }
    }

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    /**
     * Vérifie si une culture est à maturité.
     * CORRIGÉ : utilise les vrais Material de blocs (KELP_PLANT, CAVE_VINES_PLANT, CHORUS_PLANT).
     */
    private boolean isMatureCrop(Block block) {
        return switch (block.getType()) {
            // Non-Ageable : toujours récoltables
            case PUMPKIN, MELON, CACTUS, SUGAR_CANE, BAMBOO,
                 KELP, KELP_PLANT,
                 BROWN_MUSHROOM, RED_MUSHROOM,
                 CAVE_VINES, CAVE_VINES_PLANT,   // FIX: était CHORUS_FRUIT (item)
                 CHORUS_PLANT, CHORUS_FLOWER,     // FIX: était CHORUS_FRUIT (item)
                 TORCHFLOWER, PITCHER_PLANT -> true;
            default -> {
                if (block.getBlockData() instanceof Ageable ageable) {
                    yield ageable.getAge() == ageable.getMaximumAge();
                }
                yield true; // Bloc inconnu dans FARMER_CROPS → on récompense quand même
            }
        };
    }

    private double getPotionMultiplier(ItemStack item) {
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return 1.0;
        var potionData = meta.getBasePotionType();
        if (potionData == null) return 1.0;
        String name = potionData.name();
        if (name.contains("STRONG") || name.contains("LONG")) return 1.5;
        return 1.0;
    }

    // ══════════════════════════════════════════════════════
    //  FARMER — Plantation grandes plantes
    // ══════════════════════════════════════════════════════

    private static final Set<Material> TALL_PLANT_PLACE = Set.of(
            Material.SUNFLOWER, Material.LILAC, Material.ROSE_BUSH, Material.PEONY,
            Material.TALL_GRASS, Material.LARGE_FERN
    );

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlantTall(BlockPlaceEvent event) {
        Material mat = event.getBlock().getType();
        if (!TALL_PLANT_PLACE.contains(mat)) return;
        jobManager.rewardAction(event.getPlayer(), PlayerJob.FARMER, 0.5, 1);
    }

    // ══════════════════════════════════════════════════════
    //  FARMER — Plantation graines / cultures
    //  Déclenché quand le joueur plante sur la terre labourée
    // ══════════════════════════════════════════════════════

    private static final Set<Material> CROP_PLANT_BLOCKS = Set.of(
            Material.WHEAT,             // semé via WHEAT_SEEDS
            Material.CARROTS,           // planté via CARROT
            Material.POTATOES,          // planté via POTATO
            Material.BEETROOTS,         // semé via BEETROOT_SEEDS
            Material.MELON_STEM,        // semé via MELON_SEEDS
            Material.PUMPKIN_STEM,      // semé via PUMPKIN_SEEDS
            Material.NETHER_WART,       // planté dans du sable des âmes
            Material.TORCHFLOWER_CROP,  // semé via TORCHFLOWER_SEEDS
            Material.PITCHER_CROP       // semé via PITCHER_POD
    );

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCropPlant(BlockPlaceEvent event) {
        Material mat = event.getBlock().getType();
        if (!CROP_PLANT_BLOCKS.contains(mat)) return;
        jobManager.rewardAction(event.getPlayer(), PlayerJob.FARMER, 0.3, 0);
    }
}