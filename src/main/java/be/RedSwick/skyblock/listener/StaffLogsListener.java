package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.gui.StaffLogsGUI;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class StaffLogsListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        boolean isMain   = title.equals(StaffLogsGUI.TITLE_MAIN);
        boolean isDetail = title.startsWith(StaffLogsGUI.TITLE_DETAIL);
        if (!isMain && !isDetail) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        if (isMain) {
            // Clic sur une tête → ouvre le détail
            if (clicked.getType() == Material.PLAYER_HEAD) {
                if (!(clicked.getItemMeta() instanceof SkullMeta skull)) return;
                OfflinePlayer owner = skull.getOwningPlayer();
                if (owner == null) return;
                String staffName = owner.getName();
                if (staffName == null) return;
                Bukkit.getScheduler().runTask(
                        be.RedSwick.skyblock.SkyBlockPlugin.getInstance(),
                        () -> player.openInventory(StaffLogsGUI.createDetail(player, staffName))
                );
            }
        } else {
            // Bouton retour
            if (slot == 49) {
                Bukkit.getScheduler().runTask(
                        be.RedSwick.skyblock.SkyBlockPlugin.getInstance(),
                        () -> player.openInventory(StaffLogsGUI.createMain(player))
                );
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.equals(StaffLogsGUI.TITLE_MAIN) || title.startsWith(StaffLogsGUI.TITLE_DETAIL))
            event.setCancelled(true);
    }
}