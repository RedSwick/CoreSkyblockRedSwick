package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.gui.*;
import be.RedSwick.skyblock.player.PlayerJob;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Annule TOUS les clics dans les GUIs Arcanium (priorité LOWEST).
 *
 * FIX : SimpleLootBag est EXCLU de cette liste.
 * Son titre "§6⬡ Sac de Butin" est identique à LootBagGUI.
 * Si on l'incluait ici, les joueurs ne pourraient pas déposer d'items.
 * La protection des slots 45-53 du SimpleLootBag est gérée directement
 * dans SimpleLootBag.Listener (priorité HIGH).
 */
public class GuiProtectionListener implements Listener {

    private static final Set<String> ARCANIUM_TITLES = new HashSet<>();

    static {
        ARCANIUM_TITLES.add(RankGUI.TITLE);
        ARCANIUM_TITLES.add(GradeGUI.TITLE);
        ARCANIUM_TITLES.add(JobGUI.TITLE_MAIN);
        ARCANIUM_TITLES.add(WarpGUI.TITLE);
        ARCANIUM_TITLES.add(WarpGUI.TITLE_SPONSOR);
        ARCANIUM_TITLES.add(IslandBlockGUI.TITLE);
        ARCANIUM_TITLES.add(PermissionsGUI.TITLE);
        ARCANIUM_TITLES.add(MissionGUI.TITLE_CATEGORIES);
        ARCANIUM_TITLES.add(be.RedSwick.skyblock.gui.IslandSettingsGUI.getTitle());
        ARCANIUM_TITLES.add(be.RedSwick.skyblock.gui.IslandUpgradeGUI.getTitle());

        for (PlayerJob job : PlayerJob.values()) {
            ARCANIUM_TITLES.add(JobGUI.getDetailTitle(job));
            ARCANIUM_TITLES.add(JobGUI.getActionsTitle(job));
        }
    }

    public static boolean isArcaniumGui(String title) {
        if (ARCANIUM_TITLES.contains(title)) return true;

        // Titres dynamiques missions
        if (title.startsWith("§6✦ Missions")
                || title.startsWith("§b✦ Blocs")
                || title.startsWith("§5✦")
                || title.startsWith("§6✦")) return true;

        if (title.equals(be.RedSwick.skyblock.shop.ShopGUI.TITLE_MAIN))               return true;
        if (be.RedSwick.skyblock.shop.ShopGUI.isCategoryTitle(title))                  return true;

        if (title.equals(be.RedSwick.skyblock.gui.AdminItemGUI.TITLE))                 return true;
        if (title.equals(be.RedSwick.skyblock.gui.AdminItemGUI.TITLE_PAGE1))           return true;
        if (title.equals(be.RedSwick.skyblock.gui.AdminItemGUI.TITLE_PAGE2))           return true;
        if (title.equals(be.RedSwick.skyblock.gui.AdminItemGUI.TITLE_PAGE3))           return true;
        if (title.equals(be.RedSwick.skyblock.gui.AdminItemGUI.TITLE_PAGE4))           return true;

        if (title.equals(be.RedSwick.skyblock.gui.StaffLogsGUI.TITLE_MAIN))           return true;
        if (title.startsWith(be.RedSwick.skyblock.gui.StaffLogsGUI.TITLE_DETAIL))     return true;

        if (title.equals(be.RedSwick.skyblock.gui.LeaderboardGUI.TITLE_MAIN))         return true;
        if (title.startsWith(be.RedSwick.skyblock.gui.LeaderboardGUI.TITLE_PREFIX))   return true;

        if (title.equals(be.RedSwick.skyblock.customitem.CustomToolGUI.TITLE_HAMMER))      return true;
        if (title.equals(be.RedSwick.skyblock.customitem.CustomToolGUI.TITLE_AXE))         return true;
        if (title.equals(be.RedSwick.skyblock.customitem.CustomToolGUI.TITLE_HOE))         return true;
        if (title.equals(be.RedSwick.skyblock.customitem.CustomToolGUI.TITLE_HOE_5X5))     return true;
        if (title.startsWith(be.RedSwick.skyblock.customitem.CustomToolGUI.TITLE_REPAIR_PREFIX)) return true;
        if (title.startsWith(be.RedSwick.skyblock.customitem.CustomToolGUI.TITLE_SWORD_PREFIX))  return true;
        if (title.equals(be.RedSwick.skyblock.customitem.CustomToolGUI.TITLE_FISHING_NET))  return true;
        if (title.equals(be.RedSwick.skyblock.customitem.CustomToolGUI.TITLE_MULTITOOL))    return true;
        if (title.equals(be.RedSwick.skyblock.customitem.SwordGUI.TITLE))                   return true;

        if (title.equals(be.RedSwick.skyblock.customitem.SeedBagGUI.TITLE_PREFIX))    return true;

        // NOTE : LootBagGUI (ancien sac de butin avec filtres fixes) = protégé
        if (title.startsWith(be.RedSwick.skyblock.customitem.LootBagGUI.TITLE_PREFIX)) return true;

        // SimpleLootBag titre = "§6✦ Sac Personnel" — EXCLU intentionnellement.
        // Les slots 0-44 sont libres (dépôt/retrait par le joueur).
        // Protection des slots 45-53 gérée par SimpleLootBag.Listener (HIGHEST).

        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (isArcaniumGui(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isArcaniumGui(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }
}