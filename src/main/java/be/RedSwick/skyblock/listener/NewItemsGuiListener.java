package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.customitem.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Gère tous les clics dans les GUIs des nouveaux items custom.
 */
public class NewItemsGuiListener implements Listener {

    private final SkyBlockPlugin plugin = SkyBlockPlugin.getInstance();

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        String title = event.getView().getTitle();

        // ── Sword GUI ──
        if (title.equals(SwordGUI.TITLE)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            List<String> lore = clicked.getItemMeta().getLore();
            if (lore == null) return;
            String action = lore.stream().filter(l -> l.startsWith("§0ACT:")).findFirst().orElse(null);
            if ("§0ACT:sword_autosell".equals(action)) {
                SwordConfig.ArcaniumConfig cfg = SwordConfig.get().getArcanium(p.getUniqueId());
                SwordConfig.get().setArcanium(p.getUniqueId(), new SwordConfig.ArcaniumConfig(!cfg.autoSell()));
                Bukkit.getScheduler().runTask(plugin, () -> p.openInventory(SwordGUI.create(p)));
            } else if (clicked.getType() == Material.BARRIER) {
                p.closeInventory();
            }
            return;
        }

        // ── Seed Bag GUI ──
        if (title.startsWith(SeedBagGUI.TITLE_PREFIX)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            if (event.getCurrentItem().getType() == Material.BOOK) return;

            // Trouver le sac dans l'inventaire
            ItemStack bag = findInInventory(p, CustomItemType.SEED_BAG);
            if (bag == null) { p.closeInventory(); return; }
            UUID bagId = BagIdUtil.getBagId(bag);
            if (bagId == null) return;

            Material seed = event.getCurrentItem().getType();
            if (!LootBagData.SEED_MATERIALS.contains(seed)) return;

            long qty = LootBagData.get().getSeedCount(bagId, seed);
            long toTake = event.isShiftClick() ? Math.min(64, qty) : Math.min(1, qty);
            if (toTake <= 0) return;

            if (LootBagData.get().takeSeed(bagId, seed, toTake)) {
                ItemStack give = new ItemStack(seed, (int) toTake);
                p.getInventory().addItem(give).forEach((k, v) ->
                        p.getWorld().dropItemNaturally(p.getLocation(), v));
                Bukkit.getScheduler().runTask(plugin, () -> p.openInventory(SeedBagGUI.create(p, bagId)));
            }
            return;
        }

        // ── Loot Bag GUI ──
        if (title.startsWith(LootBagGUI.TITLE_PREFIX)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();

            // Fermer
            if (clicked != null && clicked.getType() == Material.BARRIER) {
                p.closeInventory(); return;
            }

            if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasLore()) return;

            ItemStack bag = findInInventory(p, CustomItemType.LOOT_BAG);
            if (bag == null) { p.closeInventory(); return; }
            UUID bagId = BagIdUtil.getBagId(bag);
            if (bagId == null) return;

            // Trouver le tag BAG_ITEM dans le lore
            List<String> lore = clicked.getItemMeta().getLore();
            String tag = lore.stream().filter(l -> l.startsWith("§0BAG_ITEM:")).findFirst().orElse(null);
            if (tag == null) return;

            // Format : §0BAG_ITEM:MATERIAL:bagUUID
            String[] parts = tag.replace("§0BAG_ITEM:", "").split(":");
            if (parts.length < 1) return;
            Material mat;
            try { mat = Material.valueOf(parts[0]); } catch (Exception e) { return; }

            Map<Material, Long> contents = LootBagData.get().getLootBagContents(bagId);
            long qty = contents.getOrDefault(mat, 0L);
            if (qty <= 0) return;

            long toTake;
            if (event.isShiftClick()) {
                // Retirer le maximum qui rentre dans l'inventaire
                int freeSlots = 0;
                int partialSpace = 0;
                for (ItemStack invItem : p.getInventory().getStorageContents()) {
                    if (invItem == null || invItem.getType().isAir()) {
                        freeSlots++;
                    } else if (invItem.getType() == mat && invItem.getAmount() < invItem.getMaxStackSize()) {
                        partialSpace += invItem.getMaxStackSize() - invItem.getAmount();
                    }
                }
                toTake = Math.min(qty, (long)(freeSlots * mat.getMaxStackSize()) + partialSpace);
                toTake = Math.min(toTake, Integer.MAX_VALUE);
            } else {
                // Retirer 1 stack (64)
                toTake = Math.min(64, qty);
            }

            if (toTake <= 0) { p.sendMessage("§cInventaire plein !"); return; }

            // Retirer du sac
            long remaining = qty - toTake;
            if (remaining <= 0) contents.remove(mat);
            else contents.put(mat, remaining);
            LootBagData.get().save();

            // Donner les items
            long left = toTake;
            while (left > 0) {
                int give = (int) Math.min(left, mat.getMaxStackSize());
                p.getInventory().addItem(new ItemStack(mat, give))
                        .forEach((k, v) -> p.getWorld().dropItemNaturally(p.getLocation(), v));
                left -= give;
            }

            Bukkit.getScheduler().runTask(plugin, () -> p.openInventory(LootBagGUI.create(p, bagId)));
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLootBagDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getView().getTitle().startsWith(LootBagGUI.TITLE_PREFIX))
            event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = event.getView().getTitle();
        if (title.equals(SwordGUI.TITLE) || title.equals(LootBagGUI.TITLE_PREFIX)
                || title.startsWith(SeedBagGUI.TITLE_PREFIX)) {
            event.setCancelled(true);
        }
    }

    private ItemStack findInInventory(Player p, CustomItemType type) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (CustomItemManager.getType(item) == type) return item;
        }
        return null;
    }
}