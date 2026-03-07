package be.RedSwick.skyblock.customitem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SwordGUI {

    public static final String TITLE = "§8⚙ Config §5Lame Arcanium";

    public static Inventory create(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        SwordConfig.ArcaniumConfig cfg = SwordConfig.get().getArcanium(p.getUniqueId());

        // Bordure
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta(); bm.setDisplayName("§0"); border.setItemMeta(bm);
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, border);
        }

        // Bouton auto-sell
        Material mat = cfg.autoSell() ? Material.LIME_DYE : Material.GRAY_DYE;
        String status = cfg.autoSell() ? "§aActivée" : "§cDésactivée";
        ItemStack btn = new ItemStack(mat);
        ItemMeta m = btn.getItemMeta();
        m.setDisplayName("§6Auto-Vente des drops");
        m.setLore(List.of(
                "§7Vend automatiquement les drops",
                "§7des mobs tués avec cette épée",
                "",
                "§7État : " + status,
                "§eCliquer pour changer",
                "§0ACT:sword_autosell"
        ));
        btn.setItemMeta(m);
        inv.setItem(13, btn);

        // Fermer
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta(); cm.setDisplayName("§cFermer"); close.setItemMeta(cm);
        inv.setItem(22, close);

        return inv;
    }
}