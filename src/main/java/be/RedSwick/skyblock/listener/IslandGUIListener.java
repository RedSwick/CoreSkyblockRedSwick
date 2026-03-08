package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.gui.IslandSettingsGUI;
import be.RedSwick.skyblock.gui.IslandUpgradeGUI;
import be.RedSwick.skyblock.island.*;
import be.RedSwick.skyblock.manager.IslandManager;
import be.RedSwick.skyblock.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;

/**
 * Listener pour les GUI Settings et Upgrade de l'île.
 * Gère les clics dans :
 *  - IslandSettingsGUI  → toggle flags
 *  - IslandUpgradeGUI   → acheter upgrade avec coins
 */
public class IslandGUIListener implements Listener {

    private final IslandManager manager = SkyBlockPlugin.getInstance().getIslandManager();

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        String title = event.getView().getTitle();

        // ── Settings GUI ──────────────────────────────
        if (title.equals(IslandSettingsGUI.getTitle())) {
            event.setCancelled(true);
            IslandFlag flag = IslandSettingsGUI.getFlagAt(event.getSlot());
            if (flag == null) return;

            Island island = manager.getIslandByMember(player.getUniqueId());
            if (island == null) return;
            if (!island.hasPermission(player.getUniqueId(), IslandPermission.SETTINGS)) {
                player.sendMessage("§cTu n'as pas la permission !"); return;
            }

            island.toggleFlag(flag);
            manager.saveIsland(island);

            boolean enabled = island.getFlag(flag);
            player.sendMessage("§b" + flag.getDisplayName() + " §7: " + (enabled ? "§aActivé" : "§cDésactivé"));

            // Rafraîchir le GUI
            player.openInventory(IslandSettingsGUI.create(island));
        }

        // ── Upgrade GUI ───────────────────────────────
        else if (title.equals(IslandUpgradeGUI.getTitle())) {
            event.setCancelled(true);
            IslandUpgrade upgrade = IslandUpgradeGUI.getUpgradeAt(event.getSlot());
            if (upgrade == null) return;

            Island island = manager.getIslandByMember(player.getUniqueId());
            if (island == null) return;
            if (!island.hasPermission(player.getUniqueId(), IslandPermission.UPGRADE)) {
                player.sendMessage("§cTu n'as pas la permission d'améliorer l'île !"); return;
            }

            if (!island.canUpgrade(upgrade)) {
                player.sendMessage("§c✦ Niveau maximum atteint pour §e" + upgrade.getDisplayName() + "§c !"); return;
            }

            int  currentLevel = island.getUpgradeLevel(upgrade);
            int  cost         = upgrade.getCost(currentLevel + 1);

            if (cost <= 0) {
                player.sendMessage("§cErreur de coût. Contactez un admin."); return;
            }

            PlayerData data = SkyBlockPlugin.getInstance().getPlayerDataManager().get(player.getUniqueId());
            if (data == null) return;

            if (!data.removeCoins(cost)) {
                player.sendMessage("§cPas assez de coins ! Il te faut §6" +
                        String.format("%,d", cost) + " coins §c(tu en as §6" +
                        String.format("%,d", data.getCoins()) + "§c).");
                return;
            }

            island.incrementUpgrade(upgrade);
            manager.saveIsland(island);
            SkyBlockPlugin.getInstance().getPlayerDataManager().savePlayer(player.getUniqueId());

            int newLevel = island.getUpgradeLevel(upgrade);
            int newValue = island.getUpgradeValue(upgrade);

            player.sendMessage("§a✦ " + upgrade.getDisplayName() + " §aupgradé au §eniveau " + newLevel +
                    " §a! (§e" + newValue + upgrade.getUnit() + "§a)");
            player.sendMessage("§7Coins restants : §6" + String.format("%,d", data.getCoins()));

            // Si upgrade SIZE → mettre à jour le radius ET recalculer la border
            if (upgrade == IslandUpgrade.SIZE) {
                // ← CRITIQUE : synchroniser island.radius avec l'upgrade
                island.setRadius(island.getUpgradeValue(IslandUpgrade.SIZE));
                IslandFlyListener.updateFly(player); // recalc fly aussi
                // Recalcul border
                org.bukkit.WorldBorder border = org.bukkit.Bukkit.createWorldBorder();
                border.setCenter(island.getCenter());
                border.setSize(island.getUpgradeValue(IslandUpgrade.SIZE) * 2);
                border.setWarningDistance(0);
                player.setWorldBorder(border);
                // Notifier l'équipe
                for (java.util.UUID uid : island.getAllMembers()) {
                    org.bukkit.entity.Player member = org.bukkit.Bukkit.getPlayer(uid);
                    if (member != null && !member.equals(player))
                        member.sendMessage("§a✦ L'île a été agrandie par §e" + player.getName() + "§a !");
                }
            }

            // Rafraîchir le GUI
            player.openInventory(IslandUpgradeGUI.create(island));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        // Rien à faire pour l'instant — GUIs sans état persistant
    }
}