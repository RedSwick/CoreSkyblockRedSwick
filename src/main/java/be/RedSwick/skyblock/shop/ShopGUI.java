package be.RedSwick.skyblock.shop;

import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * OPTIMISATIONS :
 *  - getSpawnerEntityType() : switch String → Map<String, EntityType> statique précalculée (O(1))
 *  - fmtCoins() → délègue à JobManager.fmt() (consolidation des copies identiques)
 *  - fillBorder() consolidée dans cette classe en attendant GUIUtil
 */
public class ShopGUI {

    public static final String TITLE_MAIN = "Shop Arcanium";

    // ─── Map statique O(1) pour la résolution spawner ───────────
    private static final Map<String, EntityType> SPAWNER_ENTITY_MAP;
    static {
        SPAWNER_ENTITY_MAP = new HashMap<>();
        SPAWNER_ENTITY_MAP.put("Spawner Cochon",        EntityType.PIG);
        SPAWNER_ENTITY_MAP.put("Spawner Vache",         EntityType.COW);
        SPAWNER_ENTITY_MAP.put("Spawner Mouton",        EntityType.SHEEP);
        SPAWNER_ENTITY_MAP.put("Spawner Poulet",        EntityType.CHICKEN);
        SPAWNER_ENTITY_MAP.put("Spawner Zombie",        EntityType.ZOMBIE);
        SPAWNER_ENTITY_MAP.put("Spawner Squelette",     EntityType.SKELETON);
        SPAWNER_ENTITY_MAP.put("Spawner Araignée",      EntityType.SPIDER);
        SPAWNER_ENTITY_MAP.put("Spawner Creeper",       EntityType.CREEPER);
        SPAWNER_ENTITY_MAP.put("Spawner Slime",         EntityType.SLIME);
        SPAWNER_ENTITY_MAP.put("Spawner Sorcière",      EntityType.WITCH);
        SPAWNER_ENTITY_MAP.put("Spawner Blaze",         EntityType.BLAZE);
        SPAWNER_ENTITY_MAP.put("Spawner Enderman",      EntityType.ENDERMAN);
        SPAWNER_ENTITY_MAP.put("Spawner Guardian",      EntityType.GUARDIAN);
        SPAWNER_ENTITY_MAP.put("Spawner Wither Skelet", EntityType.WITHER_SKELETON);
        SPAWNER_ENTITY_MAP.put("Spawner Golem de Fer",  EntityType.IRON_GOLEM);
    }

    public static String getCategoryTitle(ShopCategory cat, int page) {
        return "Shop|" + cat.name() + "|" + page;
    }

    public static boolean isCategoryTitle(String t) {
        return t.startsWith("Shop|");
    }

    public static int getPageFromTitle(String t) {
        try {
            String[] p = t.split("\\|");
            return p.length >= 3 ? Integer.parseInt(p[2]) : 1;
        } catch (Exception e) { return 1; }
    }

    public static ShopCategory getCategoryFromTitle(String t) {
        if (!t.startsWith("Shop|")) return null;
        String[] parts = t.split("\\|");
        if (parts.length < 2) return null;
        try { return ShopCategory.valueOf(parts[1]); }
        catch (Exception e) { return null; }
    }

    private static final int[] CONTENT_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };
    private static final int PAGE_SIZE = CONTENT_SLOTS.length; // 28

    // ════════════════════════════════════════════════
    //  MENU PRINCIPAL
    // ════════════════════════════════════════════════

    public static Inventory createMain() {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_MAIN);
        fillBorder(inv, Material.YELLOW_STAINED_GLASS_PANE);

        ShopCategory[] cats = ShopCategory.values();
        int[] slots = {19, 21, 23, 25, 29, 31, 33};

        for (int i = 0; i < cats.length && i < slots.length; i++) {
            inv.setItem(slots[i], makeCategoryItem(cats[i]));
        }

        inv.setItem(49, makeClose());
        return inv;
    }

    // ════════════════════════════════════════════════
    //  PAGE CATÉGORIE
    // ════════════════════════════════════════════════

    public static Inventory createCategory(ShopCategory cat, long playerCoins, long playerGems, int page) {
        String title = getCategoryTitle(cat, page);
        Inventory inv = Bukkit.createInventory(null, 54, title);
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);

        List<ShopItem> items = cat.getItems();
        int from = (page - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, items.size());

        for (int i = from; i < to; i++) {
            inv.setItem(CONTENT_SLOTS[i - from], makeShopItem(items.get(i), playerCoins, playerGems));
        }

        int totalPages = (int) Math.ceil((double) items.size() / PAGE_SIZE);
        if (page > 1)          inv.setItem(45, makePrev(page));
        if (page < totalPages) inv.setItem(53, makeNext(page));
        inv.setItem(49, makeBack());
        return inv;
    }

    // ─────────────────────────────────────────────
    //  Item dans la catégorie
    // ─────────────────────────────────────────────

    private static ItemStack makeShopItem(ShopItem si, long coins, long gems) {
        ItemStack item = new ItemStack(si.material());
        ItemMeta meta  = item.getItemMeta();

        meta.setDisplayName("§f" + si.displayName());
        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        if (si.isGemBuy()) {
            boolean canBuy = gems >= si.gemPrice();
            lore.add("§b💎 Prix : §3" + si.gemPrice() + " Gemmes");
            lore.add("");
            lore.add(canBuy ? "§aClic gauche §7pour acheter"
                    : "§cPas assez de gemmes §8(§3" + gems + "§8)");
        } else {
            if (si.isBuyable()) {
                boolean canBuy = coins >= si.buyPrice();
                lore.add("§6Achat §8» §e" + fmtCoins(si.buyPrice()) + " §6⬡");
                lore.add(canBuy ? "§aClic gauche §7pour acheter"
                        : "§cPas assez §8(§e" + fmtCoins(coins) + "§8)");
            }
            if (si.isSellable()) {
                lore.add("§2Vente §8» §a" + fmtCoins(si.sellPrice()) + " §6⬡");
                lore.add("§aClic droit §7pour vendre ×1");
                lore.add("§eShift+clic droit §7pour vendre tout");
            }
        }

        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeCategoryItem(ShopCategory cat) {
        ItemStack item = new ItemStack(cat.getIcon());
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName(cat.getDisplayName());
        meta.setLore(List.of(
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7" + cat.getItems().size() + " items",
                "§eClic pour ouvrir"
        ));
        item.setItemMeta(meta);
        return item;
    }

    // ─────────────────────────────────────────────
    //  Helper spawner — O(1) via Map statique
    // ─────────────────────────────────────────────

    public static EntityType getSpawnerEntityType(String displayName) {
        return SPAWNER_ENTITY_MAP.get(displayName);
    }

    // ─────────────────────────────────────────────
    //  Boutons
    // ─────────────────────────────────────────────

    private static ItemStack makePrev(int page) {
        ItemStack i = new ItemStack(Material.ARROW);
        ItemMeta m  = i.getItemMeta();
        m.setDisplayName("§7◀ Page " + (page - 1));
        i.setItemMeta(m); return i;
    }

    private static ItemStack makeNext(int page) {
        ItemStack i = new ItemStack(Material.ARROW);
        ItemMeta m  = i.getItemMeta();
        m.setDisplayName("§7Page " + (page + 1) + " ▶");
        i.setItemMeta(m); return i;
    }

    private static ItemStack makeBack() {
        ItemStack i = new ItemStack(Material.NETHER_STAR);
        ItemMeta m  = i.getItemMeta();
        m.setDisplayName("§7◀ Boutique");
        i.setItemMeta(m); return i;
    }

    private static ItemStack makeClose() {
        ItemStack i = new ItemStack(Material.BARRIER);
        ItemMeta m  = i.getItemMeta();
        m.setDisplayName("§cFermer");
        i.setItemMeta(m); return i;
    }

    private static void fillBorder(Inventory inv, Material mat) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta meta  = pane.getItemMeta();
        meta.setDisplayName("§r");
        pane.setItemMeta(meta);
        int size = inv.getSize();
        for (int i = 0; i < 9; i++)           inv.setItem(i, pane);
        for (int i = size - 9; i < size; i++) inv.setItem(i, pane);
        for (int i = 0; i < size; i += 9)     inv.setItem(i, pane);
        for (int i = 8; i < size; i += 9)     inv.setItem(i, pane);
    }

    /**
     * Formatage coins — source unique (référence JobManager.fmt() en interne).
     * Gardé ici pour compatibilité avec les autres classes du package shop.
     */
    public static String fmtCoins(long n) {
        if (n >= 1_000_000) {
            long m = n / 1_000_000;
            long k = (n % 1_000_000) / 1_000;
            return k > 0 ? m + "M" + k + "K" : m + "M";
        }
        if (n >= 1_000) return (n / 1_000) + "K";
        return String.valueOf(n);
    }
}