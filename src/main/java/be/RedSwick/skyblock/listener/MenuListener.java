package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.gui.*;
import be.RedSwick.skyblock.manager.PlayerDataManager;
import be.RedSwick.skyblock.player.*;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MenuListener implements Listener {

    private final PlayerDataManager pdm =
            SkyBlockPlugin.getInstance().getPlayerDataManager();

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();

        // ════════════════════════════════════════
        //  GUI MISSIONS — Menu catégories
        // ════════════════════════════════════════
        if (title.equals(be.RedSwick.skyblock.gui.MissionGUI.TITLE_CATEGORIES)) {
            if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;
            int slot = event.getRawSlot();
            be.RedSwick.skyblock.island.Island mIsland =
                    SkyBlockPlugin.getInstance().getIslandManager()
                            .getIslandByMember(player.getUniqueId());
            if (mIsland == null) { player.closeInventory(); return; }

            be.RedSwick.skyblock.island.IslandMission.Category cat = switch (slot) {
                case 10 -> be.RedSwick.skyblock.island.IslandMission.Category.FARMER;
                case 12 -> be.RedSwick.skyblock.island.IslandMission.Category.CHASSEUR;
                case 14 -> be.RedSwick.skyblock.island.IslandMission.Category.BUCHERON;
                case 16 -> be.RedSwick.skyblock.island.IslandMission.Category.MINEUR;
                default -> null;
            };
            if (slot == 22) { player.closeInventory(); return; }
            if (cat != null) {
                player.openInventory(be.RedSwick.skyblock.gui.MissionGUI.createPage(cat, mIsland, 1));
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
        }

        // ════════════════════════════════════════
        //  GUI MISSIONS — Pages paginées
        // ════════════════════════════════════════
        if (be.RedSwick.skyblock.gui.MissionGUI.isPageTitle(title)) {
            if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;
            int slot = event.getRawSlot();
            be.RedSwick.skyblock.island.Island mIsland =
                    SkyBlockPlugin.getInstance().getIslandManager()
                            .getIslandByMember(player.getUniqueId());
            if (mIsland == null) { player.closeInventory(); return; }

            // Extraire catégorie et page depuis le titre : "§6✦ Missions — FARMER — p2"
            try {
                String[] parts = title.replace("§6✦ Missions — ", "").split(" — p");
                be.RedSwick.skyblock.island.IslandMission.Category cat =
                        be.RedSwick.skyblock.island.IslandMission.Category.valueOf(parts[0]);
                int currentPage = Integer.parseInt(parts[1]);

                if (slot == 49) { // Retour catégories
                    player.openInventory(be.RedSwick.skyblock.gui.MissionGUI.createCategories(mIsland));
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 0.9f);
                } else if (slot == 45) { // Page précédente
                    player.openInventory(be.RedSwick.skyblock.gui.MissionGUI.createPage(cat, mIsland, currentPage - 1));
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 0.9f);
                } else if (slot == 53) { // Page suivante
                    player.openInventory(be.RedSwick.skyblock.gui.MissionGUI.createPage(cat, mIsland, currentPage + 1));
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1.1f);
                }
            } catch (Exception ignored) {}
        }

        // ════════════════════════════════════════
        //  GUI RANG
        // ════════════════════════════════════════
        if (title.equals(RankGUI.TITLE)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            PlayerData data = pdm.get(player.getUniqueId());
            if (data == null) return;

            int slot = event.getRawSlot();
            if (slot == 45) { player.closeInventory(); return; }
            if (slot == 49) { handleRankUpgrade(player, data); }
        }

        // ════════════════════════════════════════
        //  GUI GRADE
        // ════════════════════════════════════════
        if (title.equals(GradeGUI.TITLE)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            int slot = event.getRawSlot();
            if (slot == 40) { player.closeInventory(); return; }

            if (slot == 20 || slot == 22 || slot == 24) {
                player.closeInventory();
                player.sendMessage("§5§lArcanium §r§7» §eLa boutique sera disponible prochainement !");
                player.sendMessage("§7Rejoins notre Discord : §bdiscord.gg/arcanium");
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.8f);
            }
        }

        // ════════════════════════════════════════
        //  GUI JOB — Menu principal
        // ════════════════════════════════════════
        if (title.equals(JobGUI.TITLE_MAIN)) {
            if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;
            int slot = event.getRawSlot();
            PlayerData data = pdm.get(player.getUniqueId());
            if (data == null) return;
            if (slot == 49) { player.closeInventory(); return; }
            PlayerJob job = getJobFromMainSlot(slot);
            if (job != null) {
                player.openInventory(JobGUI.createLevelPage(job, data, 1));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
        }

        // ════════════════════════════════════════
        //  GUI ISLAND BLOCK
        // ════════════════════════════════════════
        if (title.equals(IslandBlockGUI.TITLE)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            if (event.getCurrentItem().getType() == Material.BARRIER) {
                player.closeInventory();
                return;
            }
        }

        // ════════════════════════════════════════
        //  GUI JOB — Pages de niveaux (paginées)
        // ════════════════════════════════════════
        if (JobGUI.isLevelPageTitle(title)) {
            if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;
            int slot = event.getRawSlot();
            PlayerData data = pdm.get(player.getUniqueId());
            if (data == null) return;

            try {
                // Extraire job et page depuis "§6✦ Chasseur — Niveaux p3"
                String stripped = title.replace("§6✦ ", "");
                String[] parts = stripped.split(" — Niveaux p");
                String jobName = parts[0];
                int currentPage = Integer.parseInt(parts[1]);

                PlayerJob job = null;
                for (PlayerJob j : PlayerJob.values()) {
                    if (j.getDisplay().equals(jobName)) { job = j; break; }
                }
                if (job == null) return;

                if (slot == 49) { // Retour menu principal
                    player.openInventory(JobGUI.createMain(data));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.9f);
                } else if (slot == 45 && currentPage > 1) { // Page précédente
                    player.openInventory(JobGUI.createLevelPage(job, data, currentPage - 1));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.9f);
                } else if (slot == 53) { // Page suivante
                    player.openInventory(JobGUI.createLevelPage(job, data, currentPage + 1));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.1f);
                }
            } catch (Exception ignored) {}
        }
    }

    // ─────────────────────────────────────────────
    //  Upgrade de rang
    // ─────────────────────────────────────────────

    private void handleRankUpgrade(Player player, PlayerData data) {

        PlayerRank current = data.getRank();
        PlayerRank next    = current.next();

        if (next == null) {
            player.sendMessage("§6§lTu es déjà Éternel — rang maximum !");
            return;
        }

        long required = RankGUI.getEssenceRequired().getOrDefault(next, Long.MAX_VALUE);

        if (data.getEssence() < required) {
            long missing = required - data.getEssence();
            player.sendMessage("§cIl te manque §f" + RankGUI.formatNumber(missing) + " §bEssence.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        data.removeEssence(required);
        data.setRank(next);
        pdm.savePlayer(player.getUniqueId());

        player.closeInventory();
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        player.sendTitle(next.getDisplay(), "§7Rang atteint !", 10, 60, 20);
        player.sendMessage("§5§l✦ §r§aFélicitations ! Nouveau rang : " + next.getDisplay());
        player.sendMessage("§7Essence consommée : §b" + RankGUI.formatNumber(required));

        if (next.getLevel() >= 8) {
            Bukkit.broadcastMessage("§5§l[Arcanium] §r§e" + player.getName()
                    + " §7a atteint le rang " + next.getDisplay() + "§7!");
        }

        player.openInventory(RankGUI.create(data));
    }

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    private PlayerJob getJobFromMainSlot(int slot) {
        return switch (slot) {
            case 20 -> PlayerJob.CHASSEUR;
            case 22 -> PlayerJob.FARMER;
            case 24 -> PlayerJob.MINER;
            case 29 -> PlayerJob.BUCHERON;
            case 31 -> PlayerJob.ALCHIMISTE;
            case 33 -> PlayerJob.PECHEUR;
            default -> null;
        };
    }
}