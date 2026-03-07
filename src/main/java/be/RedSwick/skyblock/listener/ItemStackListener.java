package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Stack automatique des items au sol (style RoseStacker).
 *
 * Comportement :
 *  - Quand un item spawn ou qu'un item merge se produit, on cherche
 *    un item du même type dans un rayon de 3 blocs.
 *  - Si trouvé, on incrémente le compteur PDC du master et on supprime le nouveau.
 *  - Le nom de l'item affiche "§e<NOM> §7x§6<COUNT>" quand > 1.
 *  - Au pickup, le joueur reçoit la quantité réelle (stack × amount de l'item).
 *
 * Note : on utilise PDC pour stocker le count, pas le nbt "count" natif,
 * car l'item peut avoir un amount > 64 (stack).
 */
public class ItemStackListener implements Listener {

    private static final NamespacedKey KEY_STACK =
            new NamespacedKey(SkyBlockPlugin.getInstance(), "item_stack_count");
    private static final int MERGE_RADIUS = 3; // blocs
    private static final int MAX_STACK    = 100_000; // limite anti-grieff

    // ════════════════════════════════════════════════
    //  ITEM SPAWN — tenter merge immédiat
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item spawned = event.getEntity();
        // Delay d'un tick pour que l'entité soit bien placée dans le monde
        Bukkit.getScheduler().runTaskLater(SkyBlockPlugin.getInstance(),
                () -> tryMerge(spawned), 1L);
    }

    // ════════════════════════════════════════════════
    //  ITEM MERGE natif — on annule et on gère nous-mêmes
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMerge(ItemMergeEvent event) {
        // Laisser notre système gérer — annuler le merge vanilla pour éviter doublons
        event.setCancelled(true);
        // Notre tryMerge sera appelé depuis onItemSpawn
    }

    // ════════════════════════════════════════════════
    //  PICKUP — donner la vraie quantité au joueur
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Player player)) return;
        Item   item      = event.getItem();
        int    stackCount = getStackCount(item);
        if (stackCount <= 1) return; // merge normal suffisant

        event.setCancelled(true); // on gère manuellement
        item.remove();

        ItemStack stack = item.getItemStack().clone();
        // Enlever le nom custom si présent
        if (stack.hasItemMeta()) {
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(null);
            meta.setLore(null);
            meta.getPersistentDataContainer().remove(KEY_STACK);
            stack.setItemMeta(meta);
        }
        stack.setAmount(1);

        // Distribuer stackCount items au joueur
        int remaining = stackCount;
        while (remaining > 0) {
            int give = Math.min(remaining, stack.getMaxStackSize());
            ItemStack toGive = stack.clone();
            toGive.setAmount(give);
            player.getInventory().addItem(toGive).forEach((k, v) ->
                    player.getWorld().dropItemNaturally(player.getLocation(), v));
            remaining -= give;
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.2f, 1f);
    }

    // ════════════════════════════════════════════════
    //  LOGIQUE MERGE
    // ════════════════════════════════════════════════

    private void tryMerge(Item spawned) {
        if (!spawned.isValid()) return;

        ItemStack type = spawned.getItemStack();

        for (Entity nearby : spawned.getNearbyEntities(MERGE_RADIUS, MERGE_RADIUS, MERGE_RADIUS)) {
            if (!(nearby instanceof Item master)) continue;
            if (master.equals(spawned)) continue;
            if (!master.isValid()) continue;

            // Même type ?
            if (!isSameType(master.getItemStack(), type)) continue;

            // Vérifier limite stack
            int masterCount  = getStackCount(master);
            int spawnedCount = getStackCount(spawned);
            if (masterCount >= MAX_STACK) continue;

            int newCount = Math.min(masterCount + spawnedCount, MAX_STACK);
            setStackCount(master, newCount);
            updateDisplayName(master);
            spawned.remove();
            return;
        }

        // Pas de merge → initialiser le count si pas déjà fait
        if (getStackCount(spawned) <= 0) {
            setStackCount(spawned, spawned.getItemStack().getAmount());
        }
    }

    // ════════════════════════════════════════════════
    //  HELPERS PDC
    // ════════════════════════════════════════════════

    public static int getStackCount(Item item) {
        ItemMeta meta = item.getItemStack().getItemMeta();
        if (meta == null) return item.getItemStack().getAmount();
        Integer val = meta.getPersistentDataContainer().get(KEY_STACK, PersistentDataType.INTEGER);
        return val != null ? val : item.getItemStack().getAmount();
    }

    private void setStackCount(Item item, int count) {
        ItemStack stack = item.getItemStack();
        ItemMeta  meta  = stack.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(KEY_STACK, PersistentDataType.INTEGER, count);
        stack.setItemMeta(meta);
        item.setItemStack(stack);
    }

    private void updateDisplayName(Item item) {
        int count = getStackCount(item);
        ItemStack stack = item.getItemStack();
        ItemMeta  meta  = stack.getItemMeta();
        if (meta == null) return;

        if (count > 1) {
            String baseName = stack.getType().name().charAt(0)
                    + stack.getType().name().substring(1).toLowerCase().replace("_", " ");
            meta.setDisplayName("§e" + baseName + " §7x§6" + formatCount(count));
        } else {
            meta.setDisplayName(null);
        }
        stack.setItemMeta(meta);
        item.setItemStack(stack);
    }

    private boolean isSameType(ItemStack a, ItemStack b) {
        if (a.getType() != b.getType()) return false;
        // Ne pas merger les items enchantés ou avec meta custom
        if (a.hasItemMeta() && b.hasItemMeta()) {
            ItemMeta ma = a.getItemMeta(), mb = b.getItemMeta();
            // Comparer enchants uniquement (ignorer displayName/lore qui peut différer à cause du stack count)
            return ma.getEnchants().equals(mb.getEnchants());
        }
        return !a.hasItemMeta() && !b.hasItemMeta();
    }

    private String formatCount(int count) {
        if (count >= 1_000_000) return String.format("%.1fM", count / 1_000_000.0);
        if (count >= 1_000)     return String.format("%.1fk", count / 1_000.0);
        return String.valueOf(count);
    }
}