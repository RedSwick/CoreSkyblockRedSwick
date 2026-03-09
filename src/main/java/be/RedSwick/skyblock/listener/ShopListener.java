package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.listener.SpawnerListener;
import be.RedSwick.skyblock.manager.PlayerDataManager;
import be.RedSwick.skyblock.player.PlayerData;
import be.RedSwick.skyblock.shop.*;
import be.RedSwick.skyblock.config.ShopConfig;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShopListener implements Listener {

    private final PlayerDataManager pdm = SkyBlockPlugin.getInstance().getPlayerDataManager();

    // Quantité sélectionnée par joueur dans le GUI quantité
    private final Map<UUID, Integer> qtyMap = new ConcurrentHashMap<>();

    // ════════════════════════════════════════════════
    //  CLICK
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        boolean isMain = title.equals(ShopGUI.TITLE_MAIN);
        boolean isCat  = ShopGUI.isCategoryTitle(title);
        boolean isQty  = ShopGUI.isQtyTitle(title);

        if (!isMain && !isCat && !isQty) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        if (isMain)      handleMain(player, slot);
        else if (isCat)  handleCategory(player, title, slot, event.getClick(), clicked);
        else             handleQty(player, title, slot, event.getClick());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = event.getView().getTitle();
        if (title.equals(ShopGUI.TITLE_MAIN)
                || ShopGUI.isCategoryTitle(title)
                || ShopGUI.isQtyTitle(title))
            event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        // Nettoyer la qty si on ferme un GUI autre que le shop
        String title = event.getView().getTitle();
        if (!ShopGUI.isQtyTitle(title))
            qtyMap.remove(player.getUniqueId());
    }

    // ════════════════════════════════════════════════
    //  MENU PRINCIPAL
    // ════════════════════════════════════════════════

    private void handleMain(Player player, int slot) {
        // Fermer (slot 40)
        if (slot == 40) { player.closeInventory(); return; }

        int[] catSlots = ShopGUI.getCatSlots();
        ShopCategory[] cats = ShopCategory.values();
        for (int i = 0; i < catSlots.length && i < cats.length; i++) {
            if (slot == catSlots[i]) {
                openCat(player, cats[i], 1);
                return;
            }
        }
    }

    // ════════════════════════════════════════════════
    //  PAGE CATÉGORIE
    // ════════════════════════════════════════════════

    private void handleCategory(Player player, String title, int slot,
                                ClickType click, ItemStack clicked) {
        ShopCategory cat = ShopGUI.getCategoryFromTitle(title);
        if (cat == null) return;
        int page = ShopGUI.getPageFromTitle(title);

        if (slot == 49) { openMain(player); return; }
        if (slot == 45 && clicked.getType() == Material.ARROW) { openCat(player, cat, page - 1); return; }
        if (slot == 53 && clicked.getType() == Material.ARROW) { openCat(player, cat, page + 1); return; }

        int[] SLOTS = ShopGUI.getContentSlots();
        int idx = -1;
        for (int i = 0; i < SLOTS.length; i++)
            if (SLOTS[i] == slot) { idx = i; break; }
        if (idx < 0) return;

        int itemIdx = (page - 1) * SLOTS.length + idx;
        List<ShopItem> items = ShopConfig.getItems(cat) != null ? ShopConfig.getItems(cat) : cat.getItems();
        if (itemIdx >= items.size()) return;
        ShopItem si = items.get(itemIdx);

        PlayerData data = pdm.get(player.getUniqueId());
        if (data == null) return;

        if (click == ClickType.LEFT || click == ClickType.SHIFT_LEFT) {
            if (si.isGemBuy()) {
                // Spawners → achat direct 1 par 1 (gemmes, pas de GUI qté)
                buy(player, data, si, 1);
                refreshCategory(player, cat, page);
            } else if (si.isBuyable()) {
                // GUI quantité
                openQty(player, si, cat, page, itemIdx);
            } else {
                player.sendMessage("§cCet item ne s'achète pas ici.");
            }
        } else if (click == ClickType.RIGHT) {
            sell(player, data, si, 1);
            refreshCategory(player, cat, page);
        } else if (click == ClickType.SHIFT_RIGHT) {
            sell(player, data, si, count(player, si.material()));
            refreshCategory(player, cat, page);
        }
    }

    // ════════════════════════════════════════════════
    //  GUI QUANTITÉ
    // ════════════════════════════════════════════════

    private void handleQty(Player player, String title, int slot, ClickType click) {
        int[] parsed = ShopGUI.parseQtyTitle(title);
        if (parsed == null) return;

        ShopCategory cat     = ShopCategory.values()[parsed[0]];
        int page             = parsed[1];
        int itemIdx          = parsed[2];
        List<ShopItem> items = ShopConfig.getItems(cat) != null ? ShopConfig.getItems(cat) : cat.getItems();
        if (itemIdx >= items.size()) return;
        ShopItem si = items.get(itemIdx);

        UUID uid = player.getUniqueId();
        int qty  = qtyMap.getOrDefault(uid, 1);

        // Annuler → retour catégorie
        if (slot == ShopGUI.QTY_SLOT_CANCEL) {
            qtyMap.remove(uid);
            openCat(player, cat, page);
            return;
        }

        // Confirmer → achat
        if (slot == ShopGUI.QTY_SLOT_CONFIRM) {
            PlayerData data = pdm.get(uid);
            if (data == null) return;
            buy(player, data, si, qty);
            qtyMap.remove(uid);
            openCat(player, cat, page);
            return;
        }

        // Reset → qty 1
        if (slot == ShopGUI.QTY_SLOT_RESET) {
            qtyMap.put(uid, 1);
            refreshQty(player, si, cat, page, itemIdx, 1);
            return;
        }

        // Boutons - (slots 0,1,2,3 → valeurs 64,32,16,1)
        int[] minusSlots = {0, 1, 2, 3};
        int[] minusVals  = {64, 32, 16, 1};
        for (int i = 0; i < minusSlots.length; i++) {
            if (slot == minusSlots[i]) {
                int newQty = Math.max(1, qty - minusVals[i]);
                qtyMap.put(uid, newQty);
                refreshQty(player, si, cat, page, itemIdx, newQty);
                return;
            }
        }

        // Boutons + (slots 5,6,7,8 → valeurs 1,16,32,64)
        int[] plusSlots = {5, 6, 7, 8};
        int[] plusVals  = {1, 16, 32, 64};
        for (int i = 0; i < plusSlots.length; i++) {
            if (slot == plusSlots[i]) {
                int newQty = Math.min(9999, qty + plusVals[i]);
                qtyMap.put(uid, newQty);
                refreshQty(player, si, cat, page, itemIdx, newQty);
                return;
            }
        }
    }

    // ════════════════════════════════════════════════
    //  ACHAT
    // ════════════════════════════════════════════════

    private void buy(Player player, PlayerData data, ShopItem si, int qty) {
        if (qty <= 0) return;

        if (si.isGemBuy()) {
            long total = (long) si.gemPrice() * qty;
            if (data.getGems() < total) {
                player.sendMessage("§cPas assez de gemmes §8(§3"
                        + data.getGems() + "§8/§3" + total + " 💎§8)");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            var type = ShopGUI.getSpawnerEntityType(si.displayName());
            if (type == null) return;
            data.removeGems((int) total);
            pdm.savePlayer(data.getUuid());
            for (int i = 0; i < qty; i++)
                give(player, SpawnerListener.makeSpawnerItem(type, 1));
            player.sendMessage("§a§lACHAT §r§e" + qty + "x " + si.displayName()
                    + " §apour §3" + total + " 💎");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);

        } else {
            if (!si.isBuyable()) {
                player.sendMessage("§cCet item ne s'achète pas ici."); return;
            }
            long total = si.buyPrice() * qty;
            if (!data.removeCoins(total)) {
                player.sendMessage("§cPas assez de coins §8(§e"
                        + ShopGUI.fmtCoins(data.getCoins()) + "§8/§e"
                        + ShopGUI.fmtCoins(total) + " §6⬡§8)");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            pdm.savePlayer(data.getUuid());
            // Donner en stacks (max 64 par ItemStack)
            int remaining = qty;
            int maxStack  = si.material().getMaxStackSize();
            while (remaining > 0) {
                int amount = Math.min(remaining, maxStack);
                give(player, new ItemStack(si.material(), amount));
                remaining -= amount;
            }
            player.sendMessage("§a§lACHAT §r§f" + qty + "x " + si.displayName()
                    + " §apour §e" + ShopGUI.fmtCoins(total) + " §6⬡");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        }
    }

    // ════════════════════════════════════════════════
    //  VENTE
    // ════════════════════════════════════════════════

    private void sell(Player player, PlayerData data, ShopItem si, int amount) {
        if (!si.isSellable()) {
            player.sendMessage("§cCet item ne se vend pas ici."); return;
        }
        if (amount <= 0) {
            player.sendMessage("§cTu n'as pas de §f" + si.displayName());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        int removed = remove(player, si.material(), amount);
        if (removed == 0) {
            player.sendMessage("§cTu n'as pas de §f" + si.displayName());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        long earned = si.sellPrice() * removed;
        data.addCoins(earned);
        pdm.savePlayer(data.getUuid());
        player.sendMessage("§2§lVENTE §r§f" + removed + "x " + si.displayName()
                + " §apour §e" + ShopGUI.fmtCoins(earned) + " §6⬡");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.8f);
    }

    // ════════════════════════════════════════════════
    //  HELPERS OUVERTURE
    // ════════════════════════════════════════════════

    private void openMain(Player player) {
        Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                () -> player.openInventory(ShopGUI.createMain()));
    }

    private void openCat(Player player, ShopCategory cat, int page) {
        PlayerData data = pdm.get(player.getUniqueId());
        if (data == null) return;
        int max = (int) Math.ceil((double) (ShopConfig.getItems(cat) != null ? ShopConfig.getItems(cat) : cat.getItems()).size() / (double) ShopGUI.PAGE_SIZE);
        final int p = Math.max(1, Math.min(page, Math.max(1, max)));
        Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                () -> player.openInventory(ShopGUI.createCategory(cat, data.getCoins(), data.getGems(), p)));
    }

    private void openQty(Player player, ShopItem si, ShopCategory cat, int page, int itemIdx) {
        PlayerData data = pdm.get(player.getUniqueId());
        if (data == null) return;
        int qty = qtyMap.getOrDefault(player.getUniqueId(), 1);
        qtyMap.put(player.getUniqueId(), qty);
        Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(), () ->
                player.openInventory(ShopGUI.createQtyGUI(si, cat, page, itemIdx, qty,
                        data.getCoins(), data.getGems())));
    }

    private void refreshCategory(Player player, ShopCategory cat, int page) {
        Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(), () -> {
            PlayerData fresh = pdm.get(player.getUniqueId());
            if (fresh == null) return;
            player.openInventory(ShopGUI.createCategory(cat, fresh.getCoins(), fresh.getGems(), page));
        });
    }

    private void refreshQty(Player player, ShopItem si, ShopCategory cat,
                            int page, int itemIdx, int qty) {
        Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(), () -> {
            PlayerData fresh = pdm.get(player.getUniqueId());
            if (fresh == null) return;
            player.openInventory(ShopGUI.createQtyGUI(si, cat, page, itemIdx, qty,
                    fresh.getCoins(), fresh.getGems()));
        });
    }

    // ════════════════════════════════════════════════
    //  HELPERS INVENTAIRE
    // ════════════════════════════════════════════════

    private void give(Player player, ItemStack item) {
        player.getInventory().addItem(item).forEach((k, v) ->
                player.getWorld().dropItemNaturally(player.getLocation(), v));
    }

    private int count(Player player, Material mat) {
        int n = 0;
        for (ItemStack i : player.getInventory().getContents())
            if (i != null && i.getType() == mat) n += i.getAmount();
        return n;
    }

    private int remove(Player player, Material mat, int amount) {
        int removed = 0;
        for (ItemStack i : player.getInventory().getContents()) {
            if (i == null || i.getType() != mat) continue;
            int take = Math.min(i.getAmount(), amount - removed);
            i.setAmount(i.getAmount() - take);
            removed += take;
            if (removed >= amount) break;
        }
        player.updateInventory();
        return removed;
    }
}