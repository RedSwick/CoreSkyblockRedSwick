package be.RedSwick.skyblock.customitem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI du Sac de Butin.
 *
 * Layout (54 slots) :
 *  Ligne 0 : bordure verre noir
 *  Lignes 1-4 (slots 9-44) : contenu du sac (items filtrés avec quantité dans le nom)
 *  Ligne 5 : bouton fermer (slot 49) + info (slot 45)
 *
 * Clic gauche sur un item = retirer 1 stack (64)
 * Shift+clic = retirer le maximum possible dans l'inventaire
 * Clic droit sur un slot vide = déposer l'item en main comme filtre
 */
public class LootBagGUI {

    public static final String TITLE_PREFIX = "§6⬡ Sac de Butin";

    public static Inventory create(Player p, UUID bagId) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PREFIX);

        // Bordure
        ItemStack border = glass(Material.BLACK_STAINED_GLASS_PANE, "§r");
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        inv.setItem(9, border); inv.setItem(17, border);
        inv.setItem(18, border); inv.setItem(26, border);
        inv.setItem(27, border); inv.setItem(35, border);
        inv.setItem(36, border); inv.setItem(44, border);

        // Contenu
        Map<Material, Long> contents = LootBagData.get().getLootBagContents(bagId);

        int slot = 10;
        for (Map.Entry<Material, Long> entry : contents.entrySet()) {
            if (slot > 43 || isEdgeslot(slot)) { slot++; continue; }
            while (isEdgeslot(slot) && slot <= 43) slot++;
            if (slot > 43) break;

            Material mat = entry.getKey();
            long qty = entry.getValue();

            ItemStack display = new ItemStack(mat);
            ItemMeta m = display.getItemMeta();
            m.setDisplayName("§f" + formatMat(mat));
            m.setLore(List.of(
                    "§7Quantité : §e" + String.format("%,d", qty),
                    "",
                    "§aClick §7→ retirer §e64",
                    "§aShift+Click §7→ retirer le maximum",
                    "§0BAG_ITEM:" + mat.name() + ":" + bagId
            ));
            display.setItemMeta(m);
            inv.setItem(slot, display);
            slot++;
        }

        // Info
        ItemStack info = new ItemStack(Material.CHEST);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§6§lSac de Butin");
        im.setLore(List.of(
                "§7Items stockés : §e" + contents.size() + " §7types",
                "§7Total : §e" + contents.values().stream().mapToLong(Long::longValue).sum() + " §7items",
                "",
                "§7Pour ajouter un filtre :",
                "§7Mets l'item en main et fais",
                "§7Shift+Click droit avec le sac"
        ));
        info.setItemMeta(im);
        inv.setItem(46, info);

        // Fermer
        inv.setItem(49, closeBtn());

        return inv;
    }

    private static boolean isEdgeslot(int slot) {
        return slot % 9 == 0 || slot % 9 == 8;
    }

    private static ItemStack glass(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(name);
        item.setItemMeta(m);
        return item;
    }

    private static ItemStack closeBtn() {
        ItemStack btn = new ItemStack(Material.BARRIER);
        ItemMeta m = btn.getItemMeta();
        m.setDisplayName("§cFermer");
        btn.setItemMeta(m);
        return btn;
    }

    private static String formatMat(Material mat) {
        String name = mat.name().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) sb.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }
}