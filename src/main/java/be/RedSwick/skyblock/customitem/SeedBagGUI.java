package be.RedSwick.skyblock.customitem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SeedBagGUI {

    public static final String TITLE_PREFIX = "§2§lSac de Graines";

    public static Inventory create(Player p, UUID bagId) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PREFIX);
        Map<Material, Long> contents = LootBagData.get().getSeedBagContents(bagId);

        // Afficher chaque type de graine
        int slot = 0;
        for (Material seed : LootBagData.SEED_MATERIALS) {
            long qty = contents.getOrDefault(seed, 0L);
            ItemStack item = new ItemStack(seed);
            ItemMeta m = item.getItemMeta();
            m.setDisplayName("§f" + formatName(seed));
            m.setLore(List.of(
                    "§7Quantité : §a" + String.format("%,d", qty),
                    "",
                    "§eShift+Clic §7pour retirer 64",
                    "§eCliquer §7pour retirer 1"
            ));
            item.setItemMeta(m);
            inv.setItem(slot++, item);
        }

        // Info
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§2Sac de Graines");
        im.setLore(List.of(
                "§7Stockage illimité de graines",
                "§7Utilisé auto par le §aPlanteur",
                "§8ID: " + bagId.toString().substring(0, 8)
        ));
        info.setItemMeta(im);
        inv.setItem(49, info);

        return inv;
    }

    private static String formatName(Material mat) {
        return mat.name().replace("_", " ").toLowerCase()
                .substring(0, 1).toUpperCase()
                + mat.name().replace("_", " ").toLowerCase().substring(1);
    }
}