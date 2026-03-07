package be.RedSwick.skyblock.customitem;

import be.RedSwick.skyblock.customitem.CustomItemConfig.HammerConfig;
import be.RedSwick.skyblock.customitem.CustomItemConfig.HoeConfig;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * GUI de configuration pour Hammer et Farmer's Hoe 5x5.
 * Ouvert par Shift+Clic droit dans le vide.
 */
public class CustomToolGUI {

    public static final String TITLE_HAMMER        = "§8⚙ Config §6Hammer";
    public static final String TITLE_AXE           = "§8⚙ Config §6Hache 3x3";
    public static final String TITLE_HOE           = "§8⚙ Config §aFarmer's Hoe";
    public static final String TITLE_HOE_5X5       = "§8⚙ Config §aFarmer's Hoe 5x5";
    public static final String TITLE_MULTITOOL     = "§8⚙ Config §bMultiTool";
    public static final String TITLE_FISHING_NET   = "§8⚙ Config §3Filet de Pêche";
    public static final String TITLE_SWORD_PREFIX  = "§8⚙ Config §5Épée";
    public static final String TITLE_REPAIR_PREFIX = "§8⚙ Réparation";
    // ════════════════════════════════════════════════
    //  MULTITOOL GUI (Elite uniquement)
    // ════════════════════════════════════════════════

    public static Inventory createMultitool(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_MULTITOOL);
        be.RedSwick.skyblock.customitem.CustomItemConfig.MultitoolConfig cfg =
                be.RedSwick.skyblock.customitem.CustomItemConfig.get().getMultitool(p.getUniqueId());
        fillBorder(inv);
        inv.setItem(10, toggle("§6Vente automatique", cfg.autoSell(),
                List.of("§7Vend automatiquement les blocs minés"), "mt_autosell"));
        inv.setItem(12, toggle("§bFonte automatique", cfg.smelt(),
                List.of("§7Minerais → lingots directement"), "mt_smelt"));
        inv.setItem(14, toggle("§fSilk Touch", cfg.silkTouch(),
                List.of("§7Mine les blocs tels quels (désactive la fonte)"), "mt_silk"));
        be.RedSwick.skyblock.customitem.CustomItemType multiType = p.getInventory().getItemInMainHand() != null
                ? be.RedSwick.skyblock.customitem.CustomItemManager.getType(p.getInventory().getItemInMainHand()) : null;
        if (multiType != null && multiType.isMultitool() && multiType.isRepairable()) {
            int multiDur = be.RedSwick.skyblock.customitem.CustomItemManager.getDurability(p.getInventory().getItemInMainHand());
            inv.setItem(22, repairButton(multiType, multiDur, p));
        } else {
            inv.setItem(22, close());
        }
        inv.setItem(16, close());
        return inv;
    }

    // ════════════════════════════════════════════════
    //  AXE GUI
    // ════════════════════════════════════════════════

    public static Inventory createAxe(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_AXE);
        be.RedSwick.skyblock.customitem.CustomItemConfig.AxeConfig cfg =
                be.RedSwick.skyblock.customitem.CustomItemConfig.get().getAxe(p.getUniqueId());
        fillBorder(inv);
        inv.setItem(13, toggle("§6Vente automatique", cfg.autoSell(),
                List.of("§7Vend automatiquement le bois coupé"), "axe_autosell"));
        // Bouton réparation
        be.RedSwick.skyblock.customitem.CustomItemType hoeType = be.RedSwick.skyblock.customitem.CustomItemType.FARMERS_HOE_5X5;
        ItemStack hoeHand = p.getInventory().getItemInMainHand();
        be.RedSwick.skyblock.customitem.CustomItemType hoeHandType = be.RedSwick.skyblock.customitem.CustomItemManager.getType(hoeHand);
        int hoeDur = (hoeHandType != null) ? be.RedSwick.skyblock.customitem.CustomItemManager.getDurability(hoeHand) : hoeType.getMaxDurability();
        inv.setItem(10, repairButton(hoeType, hoeDur, p));
        inv.setItem(22, close());
        return inv;
    }

    // ════════════════════════════════════════════════
    //  HAMMER GUI
    // ════════════════════════════════════════════════

    public static Inventory createHammer(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_HAMMER);
        HammerConfig cfg = CustomItemConfig.get().getHammer(p.getUniqueId());
        fillBorder(inv);

        inv.setItem(10, toggle("§6Vente automatique", cfg.autoSell(),
                List.of("§7Vend automatiquement les minerais cassés"), "hammer_autosell"));
        inv.setItem(12, toggle("§bFusion automatique", cfg.smelt(),
                List.of("§7Fond les minerais directement (ex: fer→lingot)"), "hammer_smelt"));
        inv.setItem(14, toggle("§eVente après fusion", cfg.autoSellSmelted(),
                List.of("§7Vend les lingots obtenus après fusion"), "hammer_autosell_smelted"));
        inv.setItem(16, close());
        return inv;
    }

    // ════════════════════════════════════════════════
    //  HOE GUI
    // ════════════════════════════════════════════════

    public static Inventory createHoe(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_HOE);
        HoeConfig cfg = CustomItemConfig.get().getHoe(p.getUniqueId());
        fillBorder(inv);

        inv.setItem(10, toggle("§aDrops → Inventaire", cfg.toInventory(),
                List.of("§7Les récoltes vont dans l'inventaire", "§7(désactivé = drop par terre)"), "hoe_inventory"));
        inv.setItem(12, toggle("§6Vente automatique", cfg.autoSell(),
                List.of("§7Vend les récoltes automatiquement au prix du /shop"), "hoe_autosell"));
        inv.setItem(14, radiusButton(cfg.radius()));
        inv.setItem(16, close());
        return inv;
    }

    // ════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════

    /** Bouton réparation commun à tous les GUI */
    public static ItemStack repairButton(be.RedSwick.skyblock.customitem.CustomItemType type, int currentDur, Player p) {
        boolean canRepair = type.isRepairable();
        Material mat = canRepair ? Material.ANVIL : Material.BARRIER;
        ItemStack btn = new ItemStack(mat);
        org.bukkit.inventory.meta.ItemMeta m = btn.getItemMeta();
        m.setDisplayName(canRepair ? "§a§lRéparer" : "§cNon réparable");
        int max = type.getMaxDurability();
        String durBar = "§7[§" + (currentDur > max * 0.5 ? "a" : currentDur > max * 0.2 ? "e" : "c")
                + currentDur + "§7/§f" + max + "§7]";
        if (canRepair) {
            be.RedSwick.skyblock.player.PlayerData data =
                    be.RedSwick.skyblock.SkyBlockPlugin.getInstance().getPlayerDataManager().get(p.getUniqueId());
            long coins = data != null ? data.getCoins() : 0;
            int levels = p.getLevel();
            boolean canAfford = levels >= type.getRepairLevels() && coins >= type.getRepairCoins();
            m.setLore(java.util.List.of(
                    "§7Durabilité : " + durBar,
                    "",
                    "§7Coût : §e" + type.getRepairLevels() + " niveaux §7+ §6"
                            + String.format("%,d", type.getRepairCoins()) + " coins",
                    "§7Tes niveaux : §e" + levels + "  §7Tes coins : §6" + String.format("%,d", coins),
                    "",
                    canAfford ? "§aCliquer pour réparer !" : "§cFonds insuffisants",
                    "§0ACT:repair"
            ));
        } else {
            m.setLore(java.util.List.of("§7Durabilité : " + durBar));
        }
        btn.setItemMeta(m);
        return btn;
    }

    public static ItemStack toggle(String name, boolean enabled, List<String> desc, String actionId) {
        Material mat = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name + (enabled ? " §a[ON]" : " §c[OFF]"));
        var lore = new java.util.ArrayList<>(desc);
        lore.add("");
        lore.add(enabled ? "§aClic pour désactiver" : "§cClic pour activer");
        lore.add("§0ACT:" + actionId);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack radiusButton(int radius) {
        String[] labels = {"§71x1", "§a3x3", "§65x5"};
        Material[] mats = {Material.WHEAT_SEEDS, Material.GRASS_BLOCK, Material.FARMLAND};
        ItemStack item = new ItemStack(mats[radius]);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eRayon de récolte : " + labels[radius]);
        meta.setLore(List.of(
                "§7Clic pour changer",
                "§71x1 §8→ §a3x3 §8→ §65x5",
                "§0ACT:hoe_radius"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack close() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§cFermer");
        item.setItemMeta(meta);
        return item;
    }

    private static void fillBorder(Inventory inv) {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta m = pane.getItemMeta(); m.setDisplayName("§r"); pane.setItemMeta(m);
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = 18; i < 27; i++) inv.setItem(i, pane);
        inv.setItem(9, pane); inv.setItem(17, pane);
    }

    public static String getActionId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        var lore = item.getItemMeta().getLore();
        if (lore == null) return null;
        for (String l : lore)
            if (l.startsWith("§0ACT:")) return l.substring(6);
        return null;
    }

    /** GUI simplifié pour la Hoe 5x5 — rayon fixe, juste drops + vente auto */
    public static Inventory createHoe5x5(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_HOE_5X5);
        be.RedSwick.skyblock.customitem.CustomItemConfig.HoeConfig cfg =
                be.RedSwick.skyblock.customitem.CustomItemConfig.get().getHoe(p.getUniqueId());
        fillBorder(inv);
        inv.setItem(12, toggle("§aDrops → Inventaire", cfg.toInventory(),
                List.of("§7Les récoltes vont dans l'inventaire",
                        "§7Désactivé = drop par terre"), "hoe_inventory"));
        inv.setItem(14, toggle("§6Vente automatique", cfg.autoSell(),
                List.of("§7Vend les récoltes auto au prix du /shop"), "hoe_autosell"));
        // Bouton réparation
        be.RedSwick.skyblock.customitem.CustomItemType hoeType = be.RedSwick.skyblock.customitem.CustomItemType.FARMERS_HOE_5X5;
        ItemStack hoeHand = p.getInventory().getItemInMainHand();
        be.RedSwick.skyblock.customitem.CustomItemType hoeHandType = be.RedSwick.skyblock.customitem.CustomItemManager.getType(hoeHand);
        int hoeDur = (hoeHandType != null) ? be.RedSwick.skyblock.customitem.CustomItemManager.getDurability(hoeHand) : hoeType.getMaxDurability();
        inv.setItem(10, repairButton(hoeType, hoeDur, p));
        inv.setItem(22, close());
        return inv;
    }

    // ════════════════════════════════════════════════
    //  FILET DE PÊCHE GUI
    // ════════════════════════════════════════════════
    public static Inventory createFishingNet(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_FISHING_NET);
        fillBorder(inv);
        be.RedSwick.skyblock.customitem.CustomItemType type = be.RedSwick.skyblock.customitem.CustomItemType.FISHING_NET;
        ItemStack hand = p.getInventory().getItemInMainHand();
        int dur = be.RedSwick.skyblock.customitem.CustomItemManager.getDurability(hand);
        inv.setItem(13, repairButton(type, dur, p));
        inv.setItem(22, close());
        return inv;
    }

    // ════════════════════════════════════════════════
    //  ÉPÉE GUI (avec auto-sell pour Arcanium)
    // ════════════════════════════════════════════════
    public static Inventory createSword(Player p, be.RedSwick.skyblock.customitem.CustomItemType type) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_SWORD_PREFIX + " §8— " + type.getDisplayName());
        fillBorder(inv);
        ItemStack hand = p.getInventory().getItemInMainHand();
        int dur = be.RedSwick.skyblock.customitem.CustomItemManager.getDurability(hand);
        // Réparation
        inv.setItem(11, repairButton(type, dur, p));
        // Auto-sell uniquement pour Arcanium
        if (type == be.RedSwick.skyblock.customitem.CustomItemType.SWORD_ARCANIUM) {
            be.RedSwick.skyblock.customitem.SwordConfig.ArcaniumConfig cfg =
                    be.RedSwick.skyblock.customitem.SwordConfig.get().getArcanium(p.getUniqueId());
            inv.setItem(15, toggle("§6Auto-Vente des drops", cfg.autoSell(),
                    List.of("§7Vend les drops des mobs tués"), "sword_autosell"));
        }
        inv.setItem(22, close());
        return inv;
    }

    // ════════════════════════════════════════════════
    //  GUI RÉPARATION SEULE (fallback)
    // ════════════════════════════════════════════════
    public static Inventory createRepairOnly(Player p, be.RedSwick.skyblock.customitem.CustomItemType type) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_REPAIR_PREFIX + " §8— " + type.getDisplayName());
        fillBorder(inv);
        ItemStack hand = p.getInventory().getItemInMainHand();
        int dur = be.RedSwick.skyblock.customitem.CustomItemManager.getDurability(hand);
        inv.setItem(13, repairButton(type, dur, p));
        inv.setItem(22, close());
        return inv;
    }


}