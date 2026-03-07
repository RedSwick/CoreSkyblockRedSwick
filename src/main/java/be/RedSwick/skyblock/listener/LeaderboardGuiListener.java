package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.gui.LeaderboardGUI;
import be.RedSwick.skyblock.leaderboard.LeaderboardType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class LeaderboardGuiListener implements Listener {

    // FIX : ignoreCancelled=false (défaut) — GuiProtectionListener annule à LOWEST,
    // mais on doit quand même traiter le clic pour la navigation.
    // On re-cancel nous-mêmes dans les deux cas, donc pas de double action.
    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        String title = event.getView().getTitle();

        // ── Menu principal ──
        if (title.equals(LeaderboardGUI.TITLE_MAIN)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            List<String> lore = clicked.getItemMeta().getLore();
            if (lore == null) return;
            String lbId = lore.stream().filter(l -> l.startsWith("§0LB:")).findFirst().orElse(null);
            if (lbId == null) return;
            LeaderboardType type = LeaderboardType.fromId(lbId.replace("§0LB:", ""));
            if (type == null) return;
            Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                    () -> p.openInventory(LeaderboardGUI.createDetail(type, p)));
            return;
        }

        // ── Détail d'un classement ──
        if (title.startsWith(LeaderboardGUI.TITLE_PREFIX)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            List<String> lore = clicked.getItemMeta().getLore();
            if (lore != null && lore.contains("§0NAV:back_leaderboard")) {
                Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                        () -> p.openInventory(LeaderboardGUI.createMain()));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.equals(LeaderboardGUI.TITLE_MAIN) || title.startsWith(LeaderboardGUI.TITLE_PREFIX))
            event.setCancelled(true);
    }
}