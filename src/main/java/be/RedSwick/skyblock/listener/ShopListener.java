package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.manager.PlayerDataManager;
import be.RedSwick.skyblock.player.PlayerData;
import be.RedSwick.skyblock.shop.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;

import java.util.List;

public class ShopListener implements Listener {

    private final PlayerDataManager pdm = SkyBlockPlugin.getInstance().getPlayerDataManager();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        boolean isMain = title.equals(ShopGUI.TITLE_MAIN);
        boolean isCat  = ShopGUI.isCategoryTitle(title);
        if (!isMain && !isCat) return;

        // LOG DEBUG — à retirer une fois shop fonctionnel
        SkyBlockPlugin.getInstance().getLogger().info(
                "[ShopDebug] title='" + title + "' isMain=" + isMain + " isCat=" + isCat
                        + " slot=" + event.getRawSlot() + " clicked=" +
                        (event.getCurrentItem() != null ? event.getCurrentItem().getType() : "null")
        );

        event.setCancelled(true);

        // Seulement les clics dans le GUI (slots 0-53)
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        if (isMain) {
            handleMain(player, slot);
        } else {
            handleCategory(player, title, slot, event.getClick(), clicked);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = event.getView().getTitle();
        if (title.equals(ShopGUI.TITLE_MAIN) || ShopGUI.isCategoryTitle(title))
            event.setCancelled(true);
    }

    // ════════════════════════════════════════════════
    //  MENU PRINCIPAL
    // ════════════════════════════════════════════════

    private void handleMain(Player player, int slot) {
        // Fermer
        if (slot == 49) { player.closeInventory(); return; }

        // Catégories aux slots 19,21,23,25,29,31,33
        ShopCategory[] cats = ShopCategory.values();
        int[] catSlots = {19, 21, 23, 25, 29, 31, 33};
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

        // Retour
        if (slot == 49) { openMain(player); return; }

        // Page précédente
        if (slot == 45 && clicked.getType() == Material.ARROW) {
            openCat(player, cat, page - 1); return;
        }
        // Page suivante
        if (slot == 53 && clicked.getType() == Material.ARROW) {
            openCat(player, cat, page + 1); return;
        }

        // Item de shop
        int[] SLOTS = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,
                28,29,30,31,32,33,34,37,38,39,40,41,42,43};
        int idx = -1;
        for (int i = 0; i < SLOTS.length; i++) {
            if (SLOTS[i] == slot) { idx = i; break; }
        }
        if (idx < 0) return;

        int itemIdx = (page - 1) * SLOTS.length + idx;
        List<ShopItem> items = cat.getItems();
        if (itemIdx >= items.size()) return;
        ShopItem si = items.get(itemIdx);

        PlayerData data = pdm.get(player.getUniqueId());
        if (data == null) return;

        if (click == ClickType.LEFT || click == ClickType.SHIFT_LEFT) {
            buy(player, data, si);
        } else if (click == ClickType.RIGHT) {
            sell(player, data, si, 1);
        } else if (click == ClickType.SHIFT_RIGHT) {
            sell(player, data, si, count(player, si.material()));
        }

        // Refresh après achat/vente
        final int finalPage = page;
        Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(), () -> {
            PlayerData fresh = pdm.get(player.getUniqueId());
            if (fresh == null) return;
            player.openInventory(ShopGUI.createCategory(cat, fresh.getCoins(), fresh.getGems(), finalPage));
        });
    }

    // ════════════════════════════════════════════════
    //  ACHAT
    // ════════════════════════════════════════════════

    private void buy(Player player, PlayerData data, ShopItem si) {
        if (si.isGemBuy()) {
            if (data.getGems() < si.gemPrice()) {
                player.sendMessage("§cPas assez de gemmes §8(§3"
                        + data.getGems() + "§8/§3" + si.gemPrice() + "§8)");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            var type = ShopGUI.getSpawnerEntityType(si.displayName());
            if (type == null) return;
            data.removeGems(si.gemPrice());
            pdm.savePlayer(data.getUuid());
            give(player, SpawnerListener.makeSpawnerItem(type, 1));
            player.sendMessage("§a§lACHAT §r§e" + si.displayName()
                    + " §apour §3" + si.gemPrice() + " 💎");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        } else {
            if (!si.isBuyable()) {
                player.sendMessage("§cCet item ne s'achète pas ici."); return;
            }
            if (!data.removeCoins(si.buyPrice())) {
                player.sendMessage("§cPas assez de coins §8(§e"
                        + ShopGUI.fmtCoins(data.getCoins()) + "§8/§e"
                        + ShopGUI.fmtCoins(si.buyPrice()) + " §6⬡§8)");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            pdm.savePlayer(data.getUuid());
            give(player, new ItemStack(si.material()));
            player.sendMessage("§a§lACHAT §r§f" + si.displayName()
                    + " §apour §e" + ShopGUI.fmtCoins(si.buyPrice()) + " §6⬡");
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

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    private void openMain(Player player) {
        Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                () -> player.openInventory(ShopGUI.createMain()));
    }

    private void openCat(Player player, ShopCategory cat, int page) {
        PlayerData data = pdm.get(player.getUniqueId());
        if (data == null) return;
        int max = (int) Math.ceil((double) cat.getItems().size() / 28.0);
        final int p = Math.max(1, Math.min(page, Math.max(1, max)));
        Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                () -> player.openInventory(ShopGUI.createCategory(cat, data.getCoins(), data.getGems(), p)));
    }

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