package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.customitem.*;
import be.RedSwick.skyblock.player.PlayerJob;
import be.RedSwick.skyblock.util.ChatUtil;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Listener pour tous les nouveaux items custom :
 * - Seau d'eau infini
 * - Filet de pêche 3x3
 * - Épées (Brute, Chasseur, Lame Arcanium)
 * - Anneau d'XP (offhand)
 * - Sac de graines
 * - Sac de butin
 * - Sacoche de marchand  ← pickup géré par MerchantPouchTickListener (pas ici)
 * - Cristal d'XP
 * - Chunk Hopper
 *
 * FIX : onPickupWithPouch supprimé — doublon avec MerchantPouchTickListener.onPickup.
 *       MerchantPouchTickListener a le cache UUID actif → plus efficace et sans double-vente.
 */
public class NewItemsListener implements Listener {

    private final SkyBlockPlugin plugin = SkyBlockPlugin.getInstance();

    // Cooldown cristal d'XP anti double-clic
    private static final Map<UUID, Long> crystalCooldown = new HashMap<>();

    // ════════════════════════════════════════════════
    //  SEAU D'EAU INFINI
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInfiniteWaterUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                && event.getAction() != Action.RIGHT_CLICK_AIR) return;

        Player p = event.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (CustomItemManager.getType(hand) != CustomItemType.INFINITE_WATER_BUCKET) return;

        event.setCancelled(true);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block target = event.getClickedBlock().getRelative(event.getBlockFace());
            if (target.getType() == Material.AIR || target.getType() == Material.CAVE_AIR) {
                target.setType(Material.WATER);
                p.playSound(p.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1f, 1f);
            }
        }
        // L'item reste dans la main — pas de consommation
    }

    // ════════════════════════════════════════════════
    //  FILET DE PÊCHE 3x3
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFishingNet(PlayerFishEvent event) {
        Player p = event.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (CustomItemManager.getType(hand) != CustomItemType.FISHING_NET) return;

        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        // Incrémenter stat poissons pêchés
        var fishData = plugin.getPlayerDataManager().get(p.getUniqueId());
        if (fishData != null) fishData.incrementFishCaught();

        Entity caught = event.getCaught();
        if (!(caught instanceof Item caughtItem)) return;

        // Dupliquer le drop x3 (filet = 3 poissons d'un coup)
        ItemStack drop = caughtItem.getItemStack().clone();
        drop.setAmount(drop.getAmount() * 3);
        caughtItem.setItemStack(drop);

        // XP pêcheur x3 (l'event vanilla donne déjà x1, on ajoute x2 de plus)
        var action = be.RedSwick.skyblock.job.JobXpTable.PECHEUR_FISH.get(drop.getType());
        if (action != null) {
            plugin.getJobManager().rewardAction(p, PlayerJob.PECHEUR,
                    action.xp() * 2.0, action.coins() * 2.0);
        }

        // Durabilité filet
        ItemStack updated = CustomItemManager.useDurability(hand, 1);
        if (updated == null) {
            p.getInventory().setItemInMainHand(null);
            p.sendMessage("§cTon §fFilet de Pêche §cs'est cassé !");
        } else {
            p.getInventory().setItemInMainHand(updated);
        }
    }

    // ════════════════════════════════════════════════
    //  ÉPÉES — GUI config + auto-vente Lame Arcanium
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwordShiftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!event.getPlayer().isSneaking()) return;

        Player p = event.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        CustomItemType type = CustomItemManager.getType(hand);
        if (type == null || !type.isSword()) return;

        event.setCancelled(true);
        // Seule la Lame Arcanium a un GUI (auto-sell)
        if (type == CustomItemType.SWORD_ARCANIUM) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    p.openInventory(SwordGUI.create(p)));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityKilledBySword(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        ItemStack hand = killer.getInventory().getItemInMainHand();
        CustomItemType type = CustomItemManager.getType(hand);
        if (type == null || !type.isSword()) return;

        // Auto-vente des drops uniquement pour la Lame Arcanium
        if (type == CustomItemType.SWORD_ARCANIUM) {
            SwordConfig.ArcaniumConfig cfg = SwordConfig.get().getArcanium(killer.getUniqueId());
            if (cfg.autoSell()) {
                long total = 0;
                List<ItemStack> toRemove = new ArrayList<>();
                for (ItemStack drop : event.getDrops()) {
                    if (drop == null) continue;
                    long earned = SellWandHelper.sellStack(killer, drop, 1.0);
                    if (earned > 0) {
                        total += earned;
                        toRemove.add(drop);
                    }
                }
                event.getDrops().removeAll(toRemove);
                if (total > 0) {
                    final long finalTotal = total;
                    plugin.getJobManager().displaySellCoins(killer, finalTotal);
                }
            }
        }
    }

    // ════════════════════════════════════════════════
    //  ANNEAU D'XP — offhand bonus
    //  Appliqué dans JobManager.rewardAction via getRingBonus()
    // ════════════════════════════════════════════════

    public static double getRingBonus(Player p, PlayerJob job) {
        ItemStack offhand = p.getInventory().getItemInOffHand();
        CustomItemType ring = CustomItemManager.getType(offhand);
        if (ring == null || !ring.isRing()) return 0.0;
        if (ring.getRingJob() == job) return ring.getRingBonus();
        return 0.0;
    }

    // ════════════════════════════════════════════════
    //  CRISTAL D'XP
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCrystalUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        CustomItemType type = CustomItemManager.getType(hand);
        if (type == null || type.getCrystalXp() <= 0) return;

        // Bloquer toute interaction vanilla
        event.setCancelled(true);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);

        // Cooldown simple anti double-clic
        long now = System.currentTimeMillis();
        Long last = crystalCooldown.get(p.getUniqueId());
        if (last != null && now - last < 500) return;
        crystalCooldown.put(p.getUniqueId(), now);

        // Métier aléatoire
        PlayerJob[] jobs = PlayerJob.values();
        PlayerJob randomJob = jobs[new Random().nextInt(jobs.length)];

        double xp = type.getCrystalXp();
        plugin.getJobManager().rewardAction(p, randomJob, xp, 0);
        p.sendMessage("§b✦ §fCristal d'XP §b→ §a+" + (int) xp
                + " XP §7en §f" + randomJob.getDisplay() + " §b✦");
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);

        // Consommer 1 au tick suivant (évite les problèmes d'inventaire)
        final ItemStack finalHand = hand;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (finalHand.getAmount() > 1) finalHand.setAmount(finalHand.getAmount() - 1);
            else p.getInventory().setItemInMainHand(null);
        });
    }

    // ════════════════════════════════════════════════
    //  SAC DE GRAINES
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSeedBagClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (CustomItemManager.getType(hand) != CustomItemType.SEED_BAG) return;

        event.setCancelled(true);
        hand = BagIdUtil.ensureId(hand);
        p.getInventory().setItemInMainHand(hand);
        UUID bagId = BagIdUtil.getBagId(hand);

        Bukkit.getScheduler().runTask(plugin, () ->
                p.openInventory(SeedBagGUI.create(p, bagId)));
    }

    /** Quand un joueur reçoit une graine dans l'inventaire → redirige dans le sac si présent. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickupSeed(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;

        ItemStack item = event.getItem().getItemStack();
        if (!LootBagData.SEED_MATERIALS.contains(item.getType())) return;

        // Chercher un sac de graines dans l'inventaire
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack inv = contents[i];
            if (CustomItemManager.getType(inv) != CustomItemType.SEED_BAG) continue;
            ItemStack updated = BagIdUtil.ensureId(inv.clone());
            if (!updated.equals(inv)) p.getInventory().setItem(i, updated);
            UUID bagId = BagIdUtil.getBagId(updated);
            if (bagId == null) continue;
            LootBagData.get().addToSeedBag(bagId, item.getType(), item.getAmount());
            event.setCancelled(true);
            ChatUtil.actionBar(p, "§2+§f" + item.getAmount() + " §7→ §2Sac de Graines");
            return;
        }
    }

    // ════════════════════════════════════════════════
    //  SAC DE BUTIN
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLootBagClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (CustomItemManager.getType(hand) != CustomItemType.LOOT_BAG) return;

        event.setCancelled(true);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);

        // Assurer UUID
        ItemStack bag = BagIdUtil.ensureId(hand.clone());
        if (!bag.equals(hand)) {
            p.getInventory().setItemInMainHand(bag);
            hand = bag;
        }
        UUID bagId = BagIdUtil.getBagId(hand);
        if (bagId == null) return;

        if (p.isSneaking()) {
            // Shift+clic droit = ajouter l'item de l'offhand comme filtre
            ItemStack offhand = p.getInventory().getItemInOffHand();
            if (offhand != null && !offhand.getType().isAir()) {
                Material mat = offhand.getType();
                LootBagData data = LootBagData.get();
                Map<Material, Long> contents = data.getLootBagContents(bagId);
                if (!contents.containsKey(mat)) {
                    contents.put(mat, 0L);
                    data.save();
                    p.sendMessage("§a✔ §f" + mat.name() + " §7ajouté comme filtre au sac !");
                } else {
                    p.sendMessage("§7Ce matériau est déjà filtré dans ce sac.");
                }
                return;
            }
        }

        final ItemStack finalHand = hand;
        Bukkit.getScheduler().runTask(plugin, () ->
                p.openInventory(LootBagGUI.create(p, BagIdUtil.getBagId(finalHand))));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickupLoot(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        ItemStack item = event.getItem().getItemStack();
        if (item == null || item.getType().isAir()) return;
        Material mat = item.getType();

        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack inv = contents[i];
            if (CustomItemManager.getType(inv) != CustomItemType.LOOT_BAG) continue;

            UUID bagId = BagIdUtil.getBagId(inv);
            if (bagId == null) {
                ItemStack withId = BagIdUtil.ensureId(inv.clone());
                p.getInventory().setItem(i, withId);
                bagId = BagIdUtil.getBagId(withId);
            }
            if (bagId == null) continue;

            Map<Material, Long> bagContents = LootBagData.get().getLootBagContents(bagId);
            if (!bagContents.containsKey(mat)) continue;

            LootBagData.get().addToLootBag(bagId, mat, item.getAmount());
            event.setCancelled(true);
            long total = LootBagData.get().getLootBagContents(bagId).getOrDefault(mat, 0L);
            ChatUtil.actionBar(p, "§6+" + item.getAmount() + " " + mat.name().replace("_", " ")
                    + " §8(§6" + String.format("%,d", total) + "§8) §7→ §6Sac de Butin");
            return;
        }
    }

    // ════════════════════════════════════════════════
    //  SACOCHE DE MARCHAND
    //  FIX : onPickupWithPouch SUPPRIMÉ — doublon avec MerchantPouchTickListener.onPickup
    //  MerchantPouchTickListener maintient un cache UUID actif et gère cet event
    //  avec priorité HIGH. Garder les deux causait une double vente possible.
    // ════════════════════════════════════════════════

    // ════════════════════════════════════════════════
    //  CHUNK HOPPER — placement et collecte des drops
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChunkHopperPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (CustomItemManager.getType(hand) != CustomItemType.CHUNK_HOPPER) return;

        Location loc = event.getBlock().getLocation();
        ChunkHopperManager.get().placeHopper(loc);
        p.sendMessage("§8§lChunk Hopper §7placé — collecte tout le chunk au-dessus !");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChunkHopperBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.HOPPER) return;
        if (!ChunkHopperManager.get().isChunkHopper(block.getLocation())) return;

        Player p = event.getPlayer();
        ItemStack held = p.getInventory().getItemInMainHand();

        // Récupérer uniquement avec Silk Touch
        boolean hasSilk = held != null && held.containsEnchantment(
                org.bukkit.enchantments.Enchantment.SILK_TOUCH);
        if (!hasSilk) {
            event.setCancelled(true);
            p.sendMessage("§cUtilise un outil §fSilk Touch §cpour récupérer le Chunk Hopper !");
            return;
        }

        event.setDropItems(false);
        ChunkHopperManager.get().removeHopper(block.getLocation());
        block.getWorld().dropItemNaturally(block.getLocation(),
                CustomItemManager.create(CustomItemType.CHUNK_HOPPER));
        p.sendMessage("§7Chunk Hopper récupéré !");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemSpawnInChunk(ItemSpawnEvent event) {
        Location loc = event.getLocation();
        Location hopperLoc = ChunkHopperManager.get().getHopperInChunk(loc.getChunk());
        if (hopperLoc == null) return;

        // Seulement les drops AU-DESSUS du hopper
        if (loc.getBlockY() <= hopperLoc.getBlockY()) return;

        ItemStack item = event.getEntity().getItemStack();
        boolean collected = ChunkHopperManager.get().collectItem(hopperLoc, item);
        if (collected) event.setCancelled(true);
    }
}