package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.customitem.CustomItemManager;
import be.RedSwick.skyblock.customitem.CustomItemType;
import be.RedSwick.skyblock.customitem.SimpleLootBag;
import be.RedSwick.skyblock.gui.AdminItemGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AdminItemListener implements Listener {

    private static boolean isAdminGUI(String title) {
        return title.equals(AdminItemGUI.TITLE_PAGE1)
                || title.equals(AdminItemGUI.TITLE_PAGE2)
                || title.equals(AdminItemGUI.TITLE_PAGE3)
                || title.equals(AdminItemGUI.TITLE_PAGE4);
    }

    private static int getPage(String title) {
        if (title.equals(AdminItemGUI.TITLE_PAGE2)) return 2;
        if (title.equals(AdminItemGUI.TITLE_PAGE3)) return 3;
        if (title.equals(AdminItemGUI.TITLE_PAGE4)) return 4;
        return 1;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!isAdminGUI(title)) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        // ── Navigation ──
        List<String> lore = clicked.hasItemMeta() && clicked.getItemMeta().getLore() != null
                ? clicked.getItemMeta().getLore() : List.of();

        if (lore.contains("§0NAV:next")) {
            int next = Math.min(4, getPage(title) + 1);
            Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                    () -> player.openInventory(AdminItemGUI.createPage(next)));
            return;
        }
        if (lore.contains("§0NAV:prev")) {
            int prev = Math.max(1, getPage(title) - 1);
            Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                    () -> player.openInventory(AdminItemGUI.createPage(prev)));
            return;
        }

        // ── Identifier l'item à donner ──
        ItemStack give = null;

        // Cas 1 : item custom connu (houe, épée, anneau, etc.)
        CustomItemType type = CustomItemManager.getType(clicked);
        if (type != null) {
            give = CustomItemManager.create(type);
        }

        // Cas 2 : item avec ID custom dans le lore
        if (give == null) {
            String id = AdminItemGUI.getCustomId(clicked);
            if (id == null) return;

            // ── Sac de Butin Simple ──
            if (id.equals(AdminItemGUI.SIMPLE_LOOT_BAG_ID)) {
                give = SimpleLootBag.createItem();
            } else {
                // Autres items "spéciaux" (clés, etc.) — cloner l'affichage sans l'ID
                give = clicked.clone();
                give.setAmount(1);
                ItemMeta meta = give.getItemMeta();
                if (meta != null && meta.getLore() != null) {
                    var newLore = new ArrayList<>(meta.getLore());
                    newLore.removeIf(l -> l.startsWith("§8ID: §7"));
                    newLore.removeIf(l -> l.equals("§aClic gauche §7pour recevoir"));
                    meta.setLore(newLore);
                    give.setItemMeta(meta);
                }
            }
        }

        if (give == null) return;

        // ── Donner l'item (overflow = drop au sol) ──
        player.getInventory().addItem(give).forEach((k, v) ->
                player.getWorld().dropItemNaturally(player.getLocation(), v));

        String name = give.hasItemMeta() ? give.getItemMeta().getDisplayName() : give.getType().name();
        player.sendMessage("§aItem §f" + name + " §aajouté à ton inventaire.");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (isAdminGUI(event.getView().getTitle())) event.setCancelled(true);
    }
}