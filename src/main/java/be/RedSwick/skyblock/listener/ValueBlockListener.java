package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.hologram.HologramManager;
import be.RedSwick.skyblock.island.Island;
import be.RedSwick.skyblock.island.IslandPermission;
import be.RedSwick.skyblock.util.ChatUtil;
import be.RedSwick.skyblock.island.IslandValueBlock;
import be.RedSwick.skyblock.manager.IslandManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ValueBlockListener implements Listener {

    private final IslandManager   islandManager   = SkyBlockPlugin.getInstance().getIslandManager();
    private final HologramManager hologramManager = SkyBlockPlugin.getInstance().getHologramManager();

    // ══════════════════════════════════════════════════════
    //  CLIC DROIT sur un bloc de valeur existant
    //  → Ajoute des blocs au compteur sans les poser
    // ══════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onRightClick(PlayerInteractEvent event) {

        // On traite seulement la main principale pour éviter le double appel
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        // Le bloc cliqué doit être un bloc de valeur
        IslandValueBlock ivb = IslandValueBlock.fromMaterial(clicked.getType());
        if (ivb == null) return;

        Player player = event.getPlayer();

        // L'item en main doit être du même type
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != ivb.getMaterial()) return;

        Island island = islandManager.getIslandAtLocation(clicked.getLocation());
        if (island == null) return;

        // Vérifie la permission de placer des blocs
        if (!island.hasPermission(player.getUniqueId(), IslandPermission.PLACE_BLOCK)) return;

        // Annule la pose physique du bloc
        event.setCancelled(true);

        // Shift + clic → tout déposer
        int toAdd = player.isSneaking() ? hand.getAmount() : 1;

        int currentCount = island.getValueBlockCount(ivb);
        int limit        = ivb.getLimit();

        // Ne dépasse pas la limite d'affichage (on continue à compter mais on prévient)
        for (int i = 0; i < toAdd; i++) {
            island.incrementValueBlock(ivb);
        }

        islandManager.saveIsland(island);

        // Retire les blocs de l'inventaire
        if (toAdd >= hand.getAmount()) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            hand.setAmount(hand.getAmount() - toAdd);
            player.getInventory().setItemInMainHand(hand);
        }

        // Met à jour l'hologramme
        int newCount  = island.getValueBlockCount(ivb);
        boolean over  = newCount > limit;
        String color  = over ? "§c" : "§e";
        String suffix = over ? " §8(MAX)" : "";
        hologramManager.setHologram(clicked.getLocation(),
                ivb.getDisplayName() + " §7x" + color + newCount + suffix);

        // ActionBar feedback
        if (over) {
            ChatUtil.actionBar(player, "§c⚠ Limite atteinte §7(" + formatNum(newCount)
                    + "/" + formatNum(limit) + ")§c — ne compte plus !");
        } else {
            ChatUtil.actionBar(player, ivb.getDisplayName() + " §7x§e" + formatNum(newCount)
                    + " §7/ §e" + formatNum(limit)
                    + "  §b(+" + toAdd + " ajouté" + (toAdd > 1 ? "s" : "") + ")");
        }

        player.updateInventory();
    }

    // ══════════════════════════════════════════════════════
    //  POSE du PREMIER bloc de valeur (bloc physique unique)
    //  → S'il n'y a pas encore de bloc physique sur l'île pour ce type
    // ══════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {

        Block    block  = event.getBlock();
        Material mat    = block.getType();

        IslandValueBlock ivb = IslandValueBlock.fromMaterial(mat);
        if (ivb == null) return;

        Player player = event.getPlayer();
        Island island = islandManager.getIslandAtLocation(block.getLocation());
        if (island == null) return;

        // Ce bloc est le "bloc de dépôt" — on incrémente le compteur normalement
        island.incrementValueBlock(ivb);
        islandManager.saveIsland(island);

        int count = island.getValueBlockCount(ivb);
        int limit = ivb.getLimit();

        // Hologramme seulement si count > 1 (le 1er bloc seul pas besoin)
        if (count > 1) {
            String color  = count > limit ? "§c" : "§e";
            String suffix = count > limit ? " §8(MAX)" : "";
            hologramManager.setHologram(block.getLocation(),
                    ivb.getDisplayName() + " §7x" + color + count + suffix);
        }

        // ActionBar
        ChatUtil.actionBar(player, ivb.getDisplayName() + " §7x§e" + formatNum(count)
                + " §7/ §e" + formatNum(limit)
                + "  §7(§b+" + ivb.getPoints() + " IS§7)");
    }

    // ══════════════════════════════════════════════════════
    //  CASSE du bloc physique de dépôt
    //  Casser       → retire 1 bloc du compteur
    //  Shift+casser → retire 64 blocs du compteur (1 stack)
    //  Le bloc physique disparaît seulement si compteur = 0
    // ══════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {

        Block    block = event.getBlock();
        Material mat   = block.getType();

        IslandValueBlock ivb = IslandValueBlock.fromMaterial(mat);
        if (ivb == null) return;

        Player player = event.getPlayer();
        Island island = islandManager.getIslandAtLocation(block.getLocation());
        if (island == null) return;

        int totalStored = island.getValueBlockCount(ivb);
        if (totalStored <= 0) return;

        // Annule le drop normal — on gère nous-mêmes
        event.setDropItems(false);

        // Combien on retire ce coup-ci
        int toRemove = player.isSneaking() ? Math.min(64, totalStored) : 1;
        int newCount = totalStored - toRemove;

        // Donne les blocs au joueur (1 seul item, max 64)
        ItemStack give = new ItemStack(ivb.getMaterial(), toRemove);
        var leftover = player.getInventory().addItem(give);
        leftover.values().forEach(drop ->
                player.getWorld().dropItemNaturally(player.getLocation(), drop));

        island.setValueBlockCount(ivb, newCount);
        island.recalculateLevel();
        islandManager.saveIsland(island);

        if (newCount <= 0) {
            // Plus rien en stock → le bloc disparaît physiquement (déjà cassé)
            hologramManager.removeHologram(block.getLocation());
            ChatUtil.actionBar(player, "§c" + ivb.getDisplayName() + " §7retiré totalement.");
        } else {
            // Il reste des blocs → on replace le bloc physique
            event.setCancelled(true);
            // Le bloc reste en place, on met juste à jour le compteur et l'hologramme
            String color  = newCount > ivb.getLimit() ? "§c" : "§e";
            String suffix = newCount > ivb.getLimit() ? " §8(MAX)" : "";
            hologramManager.setHologram(block.getLocation(),
                    ivb.getDisplayName() + " §7x" + color + newCount + suffix);

            ChatUtil.actionBar(player, ivb.getDisplayName() + " §7x§e" + formatNum(newCount)
                    + " §7/ §e" + formatNum(ivb.getLimit())
                    + "  §7(§c-" + toRemove + " retiré" + (toRemove > 1 ? "s" : "") + "§7)");
        }

        player.updateInventory();
    }

    // ─────────────────────────────────────────────
    private String formatNum(int n) {
        if (n >= 1_000) return String.format("%,.0fk", n / 1_000.0);
        return String.valueOf(n);
    }
}