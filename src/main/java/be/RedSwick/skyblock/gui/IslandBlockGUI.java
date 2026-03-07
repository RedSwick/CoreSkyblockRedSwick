package be.RedSwick.skyblock.gui;

import be.RedSwick.skyblock.island.Island;
import be.RedSwick.skyblock.island.IslandValueBlock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class IslandBlockGUI {

    public static final String TITLE = "§b✦ Blocs de Valeur — Île";

    public static Inventory create(Island island) {

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fillBorder(inv);

        // Slot 4 : résumé IS level
        inv.setItem(4, makeLevelItem(island));

        // Slots de contenu : rangées 2-4
        int[] slots = {
                10,11,12,13,14,15,16,
                19,20,21,22,23,24,25,
                28,29,30
        };

        IslandValueBlock[] blocks = IslandValueBlock.values();

        for (int i = 0; i < blocks.length && i < slots.length; i++) {
            inv.setItem(slots[i], makeBlockItem(blocks[i], island));
        }

        // Bouton fermer
        inv.setItem(49, makeCloseButton());

        return inv;
    }

    // ─────────────────────────────────────────────

    private static ItemStack makeLevelItem(Island island) {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta  = item.getItemMeta();

        meta.setDisplayName("§b§lNiveau Île : §e" + formatLevel(island.getIsLevel()));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Total IS : §e" + String.format("%.1f", island.getIsLevel()));
        lore.add("");
        lore.add("§7Le niveau est calculé selon");
        lore.add("§7les blocs de valeur posés sur");
        lore.add("§7ton île, dans la limite autorisée.");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeBlockItem(IslandValueBlock ivb, Island island) {

        int count     = island.getValueBlockCount(ivb);
        int limit     = ivb.getLimit();
        int effective = Math.min(count, limit);
        boolean maxed = count >= limit;

        ItemStack item = new ItemStack(ivb.getMaterial());
        ItemMeta meta  = item.getItemMeta();

        meta.setDisplayName(ivb.getDisplayName());
        if (maxed) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Posés    : §e" + formatNumber(count));
        lore.add("§7Comptés  : §e" + formatNumber(effective)
                + " §7/ §e" + formatNumber(limit));
        lore.add("§7Valeur   : §b+" + ivb.getPoints() + " IS §7par bloc");
        lore.add("§7Apport   : §b+" + String.format("%.1f", effective * ivb.getPoints()) + " IS §7total");
        lore.add("");
        lore.add(makeBar(count, limit));

        if (maxed) {
            lore.add("§6§l⚠ Limite atteinte ! Les blocs");
            lore.add("§6§l  supplémentaires ne comptent plus.");
        } else {
            int remaining = limit - count;
            lore.add("§7Encore §e" + formatNumber(remaining) + " §7blocs avant la limite.");
        }

        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    private static String makeBar(int count, int limit) {
        int bars   = 20;
        int filled = Math.min(bars, (int) ((double) count / limit * bars));
        StringBuilder sb = new StringBuilder("§7[");
        for (int i = 0; i < bars; i++) {
            sb.append(i < filled ? (count >= limit ? "§6|" : "§a|") : "§8|");
        }
        sb.append("§7]");
        return sb.toString();
    }

    private static String formatLevel(double level) {
        if (level >= 1_000_000) return String.format("%.1fM", level / 1_000_000);
        if (level >= 1_000)     return String.format("%.1fk", level / 1_000);
        return String.format("%.1f", level);
    }

    private static String formatNumber(int n) {
        if (n >= 1_000) return String.format("%.1fk", n / 1_000.0);
        return String.valueOf(n);
    }

    private static void fillBorder(Inventory inv) {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta  = pane.getItemMeta();
        meta.setDisplayName("§r");
        pane.setItemMeta(meta);
        int size = inv.getSize();
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = size - 9; i < size; i++) inv.setItem(i, pane);
        for (int i = 0; i < size; i += 9) inv.setItem(i, pane);
        for (int i = 8; i < size; i += 9) inv.setItem(i, pane);
    }

    private static ItemStack makeCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName("§cFermer");
        item.setItemMeta(meta);
        return item;
    }
}