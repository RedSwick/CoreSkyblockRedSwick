package be.RedSwick.skyblock.gui;

import be.RedSwick.skyblock.island.Island;
import be.RedSwick.skyblock.island.IslandUpgrade;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class IslandUpgradeGUI {

    private static final String TITLE = "§5⬆ Upgrades de l'île";

    private static final Material[] ICONS = {
            Material.GRASS_BLOCK,    // SIZE
            Material.PLAYER_HEAD,    // MEMBER_LIMIT
            Material.SPAWNER,        // SPAWNER_LIMIT
            Material.WHEAT,          // CROP_SPEED
            Material.ZOMBIE_HEAD     // MOB_RATE
    };

    public static Inventory create(Island island) {
        IslandUpgrade[] upgrades = IslandUpgrade.values();
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        for (int i = 0; i < upgrades.length; i++) {
            IslandUpgrade upg   = upgrades[i];
            int           level = island.getUpgradeLevel(upg);
            int           max   = upg.getMaxLevel();
            boolean       maxed = level >= max;

            Material mat   = maxed ? Material.GOLD_BLOCK : ICONS[i];
            ItemStack item = new ItemStack(mat);
            ItemMeta  meta = item.getItemMeta();

            meta.setDisplayName(upg.getDisplayName() + " §7[Niv. §e" + level + "§7/§e" + max + "§7]");

            List<String> lore = new ArrayList<>();
            lore.add(upg.getDescription());
            lore.add("");
            lore.add("§7Valeur actuelle : §e" + upg.getValue(level) + upg.getUnit());

            if (!maxed) {
                lore.add("§7Prochain niveau : §a" + upg.getValue(level + 1) + upg.getUnit());
                lore.add("");
                lore.add("§7Coût : §6" + String.format("%,d", upg.getCost(level + 1)) + " coins");
                lore.add("§eCliquer pour améliorer");
            } else {
                lore.add("");
                lore.add("§6§l★ NIVEAU MAX ATTEINT ★");
            }

            // Barre de progression visuelle
            lore.add("");
            lore.add(buildProgressBar(level, max));

            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(i + 10, item); // centré sur ligne 2
        }

        return inv;
    }

    private static String buildProgressBar(int level, int max) {
        StringBuilder sb = new StringBuilder("§7[");
        for (int i = 0; i < max; i++) {
            sb.append(i < level ? "§a■" : "§8■");
        }
        sb.append("§7]");
        return sb.toString();
    }

    public static String getTitle() { return TITLE; }

    public static IslandUpgrade getUpgradeAt(int slot) {
        IslandUpgrade[] upgrades = IslandUpgrade.values();
        int idx = slot - 10;
        if (idx < 0 || idx >= upgrades.length) return null;
        return upgrades[idx];
    }
}