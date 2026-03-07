package be.RedSwick.skyblock.gui;

import be.RedSwick.skyblock.customitem.CustomItemManager;
import be.RedSwick.skyblock.customitem.CustomItemType;
import be.RedSwick.skyblock.customitem.SimpleLootBag;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * GUI /arc item — tous les items custom, paginé par catégorie.
 * Page 1 : Outils
 * Page 2 : Armes & Spéciaux
 * Page 3 : Sacs & Utilitaires  ← Sac de Butin Simple ajouté slot 13
 * Page 4 : Anneaux
 */
public class AdminItemGUI {

    public static final String TITLE_PAGE1 = "§8⚙ Items Custom §7— §fOutils §8(1/4)";
    public static final String TITLE_PAGE2 = "§8⚙ Items Custom §7— §fArmes §8(2/4)";
    public static final String TITLE_PAGE3 = "§8⚙ Items Custom §7— §fSacs §8(3/4)";
    public static final String TITLE_PAGE4 = "§8⚙ Items Custom §7— §fAnneaux §8(4/4)";
    public static final String TITLE = TITLE_PAGE1;

    // Tag utilisé dans le lore pour identifier le SimpleLootBag dans ce GUI
    public static final String SIMPLE_LOOT_BAG_ID = "simple_loot_bag";

    public static Inventory create()          { return createPage(1); }
    public static Inventory createPage(int p) {
        return switch (p) {
            case 2  -> page2();
            case 3  -> page3();
            case 4  -> page4();
            default -> page1();
        };
    }

    // ════════════════════════════════════════════════
    //  PAGE 1 — OUTILS
    // ════════════════════════════════════════════════
    private static Inventory page1() {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PAGE1);
        fillBorder(inv);

        inv.setItem(10, ci(CustomItemType.HAMMER));
        inv.setItem(11, ci(CustomItemType.AXE_3X3));
        inv.setItem(13, ci(CustomItemType.MULTITOOL_BASIC));
        inv.setItem(14, ci(CustomItemType.MULTITOOL_ELITE));
        inv.setItem(19, ci(CustomItemType.FARMERS_HOE_1X1));
        inv.setItem(20, ci(CustomItemType.FARMERS_HOE_3X3));
        inv.setItem(21, ci(CustomItemType.FARMERS_HOE_5X5));
        inv.setItem(22, ci(CustomItemType.PLANTER_HOE));
        inv.setItem(28, ci(CustomItemType.SELL_WAND_1X));
        inv.setItem(29, ci(CustomItemType.SELL_WAND_1_5X));
        inv.setItem(30, ci(CustomItemType.SELL_WAND_2X));
        inv.setItem(31, ci(CustomItemType.SELL_WAND_2_5X));
        inv.setItem(33, ci(CustomItemType.FISHING_NET));
        inv.setItem(34, ci(CustomItemType.INFINITE_WATER_BUCKET));

        nav(inv, false, true);
        return inv;
    }

    // ════════════════════════════════════════════════
    //  PAGE 2 — ARMES & SPÉCIAUX
    // ════════════════════════════════════════════════
    private static Inventory page2() {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PAGE2);
        fillBorder(inv);

        inv.setItem(10, ci(CustomItemType.SWORD_BRUTE));
        inv.setItem(11, ci(CustomItemType.SWORD_CHASSEUR));
        inv.setItem(12, ci(CustomItemType.SWORD_ARCANIUM));
        inv.setItem(19, ci(CustomItemType.XP_CRYSTAL_SMALL));
        inv.setItem(20, ci(CustomItemType.XP_CRYSTAL_MEDIUM));
        inv.setItem(21, ci(CustomItemType.XP_CRYSTAL_LARGE));
        inv.setItem(24, ci(CustomItemType.CHUNK_HOPPER));
        inv.setItem(28, makeItem(Material.TRIPWIRE_HOOK, "§6Clé de Vote",  "§7Ouvre une boîte de vote",  "key_vote"));
        inv.setItem(29, makeItem(Material.TRIPWIRE_HOOK, "§aClé de Farm",  "§7Ouvre une boîte de farm",  "key_farm"));
        inv.setItem(30, makeItem(Material.TRIPWIRE_HOOK, "§5Clé Arcanium", "§7Ouvre une boîte Arcanium", "key_arcanium"));

        nav(inv, true, true);
        return inv;
    }

    // ════════════════════════════════════════════════
    //  PAGE 3 — SACS & UTILITAIRES
    // ════════════════════════════════════════════════
    private static Inventory page3() {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PAGE3);
        fillBorder(inv);

        inv.setItem(10, ci(CustomItemType.SEED_BAG));
        inv.setItem(11, ci(CustomItemType.LOOT_BAG));       // ancien sac de butin (filtres pré-définis)
        inv.setItem(12, ci(CustomItemType.MERCHANT_POUCH));

        // ── NOUVEAU : Sac de Butin Simple (papier + GUI libre) ──
        inv.setItem(14, makeSimpleLootBagDisplay());

        nav(inv, true, true);
        return inv;
    }

    // ════════════════════════════════════════════════
    //  PAGE 4 — ANNEAUX
    // ════════════════════════════════════════════════
    private static Inventory page4() {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PAGE4);
        fillBorder(inv);

        inv.setItem(10, ci(CustomItemType.RING_XP_FARMER_5));
        inv.setItem(11, ci(CustomItemType.RING_XP_FARMER_10));
        inv.setItem(13, ci(CustomItemType.RING_XP_MINER_5));
        inv.setItem(14, ci(CustomItemType.RING_XP_MINER_10));
        inv.setItem(16, ci(CustomItemType.RING_XP_BUCHERON_5));
        inv.setItem(17, ci(CustomItemType.RING_XP_BUCHERON_10));
        inv.setItem(19, ci(CustomItemType.RING_XP_CHASSEUR_5));
        inv.setItem(20, ci(CustomItemType.RING_XP_CHASSEUR_10));
        inv.setItem(22, ci(CustomItemType.RING_XP_PECHEUR_5));
        inv.setItem(23, ci(CustomItemType.RING_XP_PECHEUR_10));
        inv.setItem(25, ci(CustomItemType.RING_XP_ALCHIMISTE_5));
        inv.setItem(26, ci(CustomItemType.RING_XP_ALCHIMISTE_10));

        nav(inv, true, false);
        return inv;
    }

    // ════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════

    /**
     * Item d'affichage du Sac de Butin Simple dans le GUI admin.
     * Quand l'admin clique dessus, AdminItemListener voit l'ID "simple_loot_bag"
     * et appelle SimpleLootBag.createItem() pour donner un sac tout neuf.
     */
    private static ItemStack makeSimpleLootBagDisplay() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName("§6⬡ §lSac de Butin §8[Simple]");
        meta.setLore(List.of(
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7GUI §flibres §7— pose toi-même",
                "§7les §e10 items §7que tu veux stocker.",
                "§7Ramassage §6automatique §7une fois configuré.",
                "§7Stockage §billimité §7en YAML.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§aClic gauche §7pour recevoir",
                "§8ID: §7" + SIMPLE_LOOT_BAG_ID
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack ci(CustomItemType type) {
        return CustomItemManager.create(type);
    }

    private static void nav(Inventory inv, boolean prev, boolean next) {
        if (prev) {
            ItemStack p = new ItemStack(Material.ARROW);
            ItemMeta m  = p.getItemMeta();
            m.setDisplayName("§e← Page précédente");
            m.setLore(List.of("§0NAV:prev"));
            p.setItemMeta(m);
            inv.setItem(45, p);
        }
        if (next) {
            ItemStack n = new ItemStack(Material.ARROW);
            ItemMeta m  = n.getItemMeta();
            m.setDisplayName("§e→ Page suivante");
            m.setLore(List.of("§0NAV:next"));
            n.setItemMeta(m);
            inv.setItem(53, n);
        }
        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta  im   = info.getItemMeta();
        im.setDisplayName("§6✦ Items Custom");
        im.setLore(List.of("§7Clic gauche sur un item pour le recevoir."));
        info.setItemMeta(im);
        inv.setItem(49, info);
    }

    public static ItemStack makeItem(Material mat, String name, String desc, String customId) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7" + desc,
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§aClic gauche §7pour recevoir",
                "§8ID: §7" + customId
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static String getCustomId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        var lore = item.getItemMeta().getLore();
        if (lore == null) return null;
        for (String line : lore)
            if (line.startsWith("§8ID: §7")) return line.replace("§8ID: §7", "");
        return null;
    }

    private static void fillBorder(Inventory inv) {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta  meta = pane.getItemMeta();
        meta.setDisplayName("§r");
        pane.setItemMeta(meta);
        int size = inv.getSize();
        for (int i = 0; i < 9; i++)           inv.setItem(i, pane);
        for (int i = size - 9; i < size; i++)  inv.setItem(i, pane);
        for (int i = 0; i < size; i += 9)     inv.setItem(i, pane);
        for (int i = 8; i < size; i += 9)     inv.setItem(i, pane);
    }
}