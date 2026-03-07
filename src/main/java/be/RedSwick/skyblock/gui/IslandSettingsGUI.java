package be.RedSwick.skyblock.gui;

import be.RedSwick.skyblock.island.Island;
import be.RedSwick.skyblock.island.IslandFlag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class IslandSettingsGUI {

    private static final String TITLE = "§8⚙ Paramètres de l'île";

    public static Inventory create(Island island) {
        IslandFlag[] flags  = IslandFlag.values();
        int size = ((flags.length / 9) + 1) * 9;
        size = Math.max(size, 27);
        Inventory inv = Bukkit.createInventory(null, size, TITLE);

        for (int i = 0; i < flags.length; i++) {
            IslandFlag flag    = flags[i];
            boolean    enabled = island.getFlag(flag);

            Material mat  = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
            ItemStack item = new ItemStack(mat);
            ItemMeta  meta = item.getItemMeta();

            meta.setDisplayName(flag.getDisplayName());
            meta.setLore(List.of(
                    flag.getDescription(),
                    "",
                    enabled ? "§a● Activé" : "§c● Désactivé",
                    "§7Clic pour basculer"
            ));
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }

        return inv;
    }

    public static String getTitle() { return TITLE; }

    /** Retourne le flag correspondant au slot cliqué, ou null */
    public static IslandFlag getFlagAt(int slot) {
        IslandFlag[] flags = IslandFlag.values();
        if (slot < 0 || slot >= flags.length) return null;
        return flags[slot];
    }
}
