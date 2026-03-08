package be.RedSwick.skyblock.shop;

import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShopGUI {

    public static final String TITLE_MAIN = "§8✦ §6§lArcanium Shop §8✦";

    // Titre du GUI quantité — contient la catégorie + page + itemIdx encodés
    // Format : "BuyQty|CAT_NAME|page|itemIdx"
    public static final String PREFIX_QTY = "BuyQty|";

    // ── Cache menu principal ──
    private static Inventory cachedMain = null;
    public static void invalidateMainCache() { cachedMain = null; }

    // ── Cache templates items ──
    private static final Map<Integer, ItemStack> itemTemplateCache = new ConcurrentHashMap<>();

    // ════════════════════════════════════════════════
    //  PARSING TITRES
    // ════════════════════════════════════════════════

    public static String getCategoryTitle(ShopCategory cat, int page) {
        return "Shop|" + cat.name() + "|" + page;
    }
    public static boolean isCategoryTitle(String t) { return t.startsWith("Shop|"); }
    public static boolean isQtyTitle(String t)      { return t.startsWith(PREFIX_QTY); }

    public static int getPageFromTitle(String t) {
        try { String[] p = t.split("\\|"); return p.length >= 3 ? Integer.parseInt(p[2]) : 1; }
        catch (Exception e) { return 1; }
    }
    public static ShopCategory getCategoryFromTitle(String t) {
        if (!t.startsWith("Shop|")) return null;
        String[] parts = t.split("\\|");
        if (parts.length < 2) return null;
        try { return ShopCategory.valueOf(parts[1]); } catch (Exception e) { return null; }
    }

    /** Encode cat/page/itemIdx dans le titre du GUI quantité */
    public static String makeQtyTitle(ShopCategory cat, int page, int itemIdx) {
        return PREFIX_QTY + cat.name() + "|" + page + "|" + itemIdx;
    }
    /** Décode [cat, page, itemIdx] depuis le titre du GUI quantité */
    public static int[] parseQtyTitle(String t) {
        // PREFIX_QTY + CAT + "|" + page + "|" + itemIdx
        String body = t.substring(PREFIX_QTY.length());
        String[] p = body.split("\\|");
        if (p.length < 3) return null;
        try {
            ShopCategory cat = ShopCategory.valueOf(p[0]);
            int page    = Integer.parseInt(p[1]);
            int itemIdx = Integer.parseInt(p[2]);
            return new int[]{ cat.ordinal(), page, itemIdx };
        } catch (Exception e) { return null; }
    }
    public static ShopCategory getCatFromQtyTitle(String t) {
        String body = t.substring(PREFIX_QTY.length());
        String[] p = body.split("\\|");
        try { return ShopCategory.valueOf(p[0]); } catch (Exception e) { return null; }
    }

    private static final int[] CONTENT_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };
    public static final int PAGE_SIZE = CONTENT_SLOTS.length; // 28

    // ════════════════════════════════════════════════
    //  MENU PRINCIPAL — 9 catégories sur 2 rangées
    //  Rangée 1 (slots 19-25 impairs) : 4 catégories
    //  Rangée 2 (slots 28-34 impairs + 31) : 5 catégories
    //  Layout :
    //   . . . . . . . . .
    //   . A . B . C . D .   ← slots 19,21,23,25
    //   . E . F . G . H . I  ← slots 28,30,32,34 + 22 (centre bas)
    //  On utilise plutôt une grille propre à 9 :
    //   slots : 10,12,14,16 (rangée 1) + 19,21,23,25,27 (rangée 2) = non
    //  Layout choisi (inventaire 54) :
    //   Ligne 2 (index 9-17)  : 9 10 11 12 13 14 15 16 17
    //   Ligne 3 (index 18-26) : 18 19 20 21 22 23 24 25 26
    //   9 catégories centrées sur 2 lignes : 4 sur ligne 2, 5 sur ligne 3
    //   Slots : 11,13,15,20,22,24   non... simplifions :
    //
    //  SOLUTION SIMPLE : 9 slots sans chevauchement
    //   19, 20, 21, 22, 23, 24, 25 → 7 slots ligne 3
    //   + 28, 30                   → 2 slots ligne 4
    // ════════════════════════════════════════════════

    // Slots des 9 catégories dans le menu principal
    private static final int[] CAT_SLOTS = {10, 12, 14, 16, 28, 30, 32, 34, 49};
    //  ┌─ 9 slots bien espacés, pas de chevauchement avec le bouton fermer (slot 49 = 9e cat)
    //  Ligne 2 : 10 . 12 . 14 . 16
    //  Ligne 4 : 28 . 30 . 32 . 34
    //  Centre  : slot 49 = 9e catégorie (Spawners)
    //  Bouton fermer déplacé à slot 40

    public static Inventory createMain() {
        if (cachedMain != null) {
            Inventory clone = Bukkit.createInventory(null, 54, TITLE_MAIN);
            for (int i = 0; i < 54; i++) {
                ItemStack it = cachedMain.getItem(i);
                if (it != null) clone.setItem(i, it.clone());
            }
            return clone;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_MAIN);
        fillBorder(inv, Material.YELLOW_STAINED_GLASS_PANE);

        ShopCategory[] cats = ShopCategory.values();
        for (int i = 0; i < cats.length && i < CAT_SLOTS.length; i++)
            inv.setItem(CAT_SLOTS[i], makeCategoryItem(cats[i]));

        // Bouton fermer au slot 40 (ligne 5 centre)
        inv.setItem(40, makeClose());

        cachedMain = inv;
        Inventory clone = Bukkit.createInventory(null, 54, TITLE_MAIN);
        for (int i = 0; i < 54; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null) clone.setItem(i, it.clone());
        }
        return clone;
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

        for (int i = from; i < to; i++)
            inv.setItem(CONTENT_SLOTS[i - from], makeShopItem(items.get(i), playerCoins, playerGems));

        int totalPages = (int) Math.ceil((double) items.size() / PAGE_SIZE);
        if (page > 1)          inv.setItem(45, makePrev(page));
        if (page < totalPages) inv.setItem(53, makeNext(page));
        inv.setItem(49, makeBack());
        return inv;
    }

    // ════════════════════════════════════════════════
    //  GUI QUANTITÉ — coffre 27 slots
    //
    //  Layout (27 slots, 3 lignes × 9) :
    //  [ -64 ][ -32 ][ -16 ][  -1 ][ITEM ][  +1 ][ +16 ][ +32 ][ +64 ]  ← ligne 1
    //  [     ][     ][     ][     ][ RST ][     ][     ][     ][     ]  ← ligne 2
    //  [  ✗  ][     ][     ][     ][     ][     ][     ][     ][  ✔  ]  ← ligne 3
    // ════════════════════════════════════════════════

    public static final int QTY_SLOT_PREVIEW  = 4;
    public static final int QTY_SLOT_CANCEL   = 18;
    public static final int QTY_SLOT_CONFIRM  = 26;
    public static final int QTY_SLOT_RESET    = 13;

    // Slots et valeurs des boutons (négatifs = indices 0-3, positifs = 5-8)
    private static final int[] QTY_MINUS_SLOTS  = {0,  1,  2,  3};
    private static final int[] QTY_MINUS_VALS   = {64, 32, 16,  1};
    private static final int[] QTY_PLUS_SLOTS   = {5,  6,  7,  8};
    private static final int[] QTY_PLUS_VALS    = {1, 16, 32, 64};

    public static Inventory createQtyGUI(ShopItem si, ShopCategory cat, int page,
                                         int itemIdx, int qty,
                                         long coins, long gems) {
        String title = makeQtyTitle(cat, page, itemIdx);
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Fond
        ItemStack filler = makeFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        // Boutons -
        for (int i = 0; i < QTY_MINUS_SLOTS.length; i++)
            inv.setItem(QTY_MINUS_SLOTS[i], makeQtyBtn(-QTY_MINUS_VALS[i], si, qty, coins, gems));

        // Aperçu item au centre (slot 4)
        inv.setItem(QTY_SLOT_PREVIEW, makeQtyPreview(si, qty, coins, gems));

        // Boutons +
        for (int i = 0; i < QTY_PLUS_SLOTS.length; i++)
            inv.setItem(QTY_PLUS_SLOTS[i], makeQtyBtn(QTY_PLUS_VALS[i], si, qty, coins, gems));

        // Reset slot 13
        inv.setItem(QTY_SLOT_RESET, makeQtyReset());

        // Cancel slot 18, Confirm slot 26
        inv.setItem(QTY_SLOT_CANCEL,  makeQtyCancel());
        inv.setItem(QTY_SLOT_CONFIRM, makeQtyConfirm(si, qty, coins, gems));

        return inv;
    }

    // ── Preview item au centre — affiche la quantité sélectionnée ──
    private static ItemStack makeQtyPreview(ShopItem si, int qty, long coins, long gems) {
        ItemStack item = new ItemStack(si.material());
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName("§f§l" + si.displayName());
        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Quantité : §e§l" + qty);
        long total = si.isGemBuy() ? (long) si.gemPrice() * qty : si.buyPrice() * qty;
        if (si.isGemBuy())
            lore.add("§bCoût total : §3§l" + total + " 💎");
        else
            lore.add("§7Coût total : §e§l" + fmtCoins(total) + " §6⬡");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ── Bouton +N ou -N ──
    private static ItemStack makeQtyBtn(int delta, ShopItem si, int currentQty, long coins, long gems) {
        boolean isPlus = delta > 0;
        int absVal = Math.abs(delta);
        Material mat = isPlus ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName((isPlus ? "§a§l+" : "§c§l-") + absVal);
        long unitPrice = si.isGemBuy() ? si.gemPrice() : si.buyPrice();
        String currency = si.isGemBuy() ? "§3💎" : "§6⬡";
        meta.setLore(List.of("§7Prix unitaire : §e" + unitPrice + " " + currency));
        item.setItemMeta(meta);
        return item;
    }

    // ── Reset ──
    private static ItemStack makeQtyReset() {
        ItemStack item = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName("§eRéinitialiser");
        meta.setLore(List.of("§7Remet la quantité à §e1"));
        item.setItemMeta(meta);
        return item;
    }

    // ── Confirmer ──
    private static ItemStack makeQtyConfirm(ShopItem si, int qty, long coins, long gems) {
        long total    = si.isGemBuy() ? (long) si.gemPrice() * qty : si.buyPrice() * qty;
        boolean canBuy = si.isGemBuy() ? gems >= total : coins >= total;
        Material mat   = canBuy ? Material.LIME_WOOL : Material.RED_WOOL;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName(canBuy ? "§a§lAcheter §e" + qty + "x" : "§c§lPas assez de fonds");
        List<String> lore = new ArrayList<>();
        if (si.isGemBuy()) {
            lore.add("§bCoût total : §3" + total + " 💎");
            lore.add(canBuy ? "§7Tu as §3" + gems + " 💎" : "§cIl te manque §3" + (total - gems) + " 💎");
        } else {
            lore.add("§7Coût total : §e" + fmtCoins(total) + " §6⬡");
            lore.add(canBuy ? "§7Tu as §e" + fmtCoins(coins) + " §6⬡"
                    : "§cIl te manque §e" + fmtCoins(total - coins) + " §6⬡");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ── Annuler ──
    private static ItemStack makeQtyCancel() {
        ItemStack item = new ItemStack(Material.RED_WOOL);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName("§c§lAnnuler");
        meta.setLore(List.of("§7Retour au shop"));
        item.setItemMeta(meta);
        return item;
    }

    // ════════════════════════════════════════════════
    //  ITEM SHOP (lore coins/gems)
    // ════════════════════════════════════════════════

    private static ItemStack makeShopItem(ShopItem si, long coins, long gems) {
        ItemStack base = itemTemplateCache.computeIfAbsent(System.identityHashCode(si), k -> {
            ItemStack item = new ItemStack(si.material());
            ItemMeta meta  = item.getItemMeta();
            meta.setDisplayName("§f" + si.displayName());
            item.setItemMeta(meta);
            return item;
        });

        ItemStack item = base.clone();
        ItemMeta meta  = item.getItemMeta();
        List<String> lore = new ArrayList<>(8);
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        if (si.isGemBuy()) {
            boolean canBuy = gems >= si.gemPrice();
            lore.add("§b💎 Achat §8» §3" + si.gemPrice() + " Gemmes");
            if (si.isGemWithSell())
                lore.add("§2Vente §8» §a" + fmtCoins(si.sellPrice()) + " §6⬡");
            lore.add("");
            lore.add(canBuy ? "§aClic gauche §7pour acheter"
                    : "§cPas assez de gemmes §8(§3" + gems + "§8)");
            if (si.isGemWithSell())
                lore.add("§aClic droit §7pour vendre");
        } else {
            if (si.isBuyable()) {
                boolean canBuy = coins >= si.buyPrice();
                lore.add("§6Achat §8» §e" + fmtCoins(si.buyPrice()) + " §6⬡");
                lore.add(canBuy ? "§aClic gauche §7pour acheter (GUI qté)"
                        : "§cPas assez §8(§e" + fmtCoins(coins) + "§8)");
            }
            if (si.isSellable()) {
                lore.add("§2Vente §8» §a" + fmtCoins(si.sellPrice()) + " §6⬡");
                lore.add("§aClic droit §7pour vendre ×1");
                lore.add("§eShift+clic droit §7pour vendre tout");
            }
            if (!si.isBuyable() && !si.isSellable())
                lore.add("§7Vente uniquement");
        }

        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ════════════════════════════════════════════════
    //  ITEM CATÉGORIE (menu principal)
    // ════════════════════════════════════════════════

    private static ItemStack makeCategoryItem(ShopCategory cat) {
        ItemStack item = new ItemStack(cat.getIcon());
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName(cat.getDisplayName());
        meta.setLore(List.of(
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7" + cat.getItems().size() + " articles",
                "§eClic pour ouvrir"
        ));
        item.setItemMeta(meta);
        return item;
    }

    // ════════════════════════════════════════════════
    //  HELPER SPAWNER
    // ════════════════════════════════════════════════

    public static EntityType getSpawnerEntityType(String displayName) {
        return switch (displayName) {
            case "Spawner Cochon"        -> EntityType.PIG;
            case "Spawner Vache"         -> EntityType.COW;
            case "Spawner Mouton"        -> EntityType.SHEEP;
            case "Spawner Poulet"        -> EntityType.CHICKEN;
            case "Spawner Lapin"         -> EntityType.RABBIT;
            case "Spawner Tortue"        -> EntityType.TURTLE;
            case "Spawner Zombie"        -> EntityType.ZOMBIE;
            case "Spawner Squelette"     -> EntityType.SKELETON;
            case "Spawner Araignée"      -> EntityType.SPIDER;
            case "Spawner Creeper"       -> EntityType.CREEPER;
            case "Spawner Slime"         -> EntityType.SLIME;
            case "Spawner Sorcière"      -> EntityType.WITCH;
            case "Spawner Blaze"         -> EntityType.BLAZE;
            case "Spawner Enderman"      -> EntityType.ENDERMAN;
            case "Spawner Guardian"      -> EntityType.GUARDIAN;
            case "Spawner Wither Skelet" -> EntityType.WITHER_SKELETON;
            case "Spawner Golem de Fer"  -> EntityType.IRON_GOLEM;
            case "Spawner Ghast"         -> EntityType.GHAST;
            case "Spawner Magma Cube"    -> EntityType.MAGMA_CUBE;
            case "Spawner Strider"       -> EntityType.STRIDER;
            case "Spawner Hoglin"        -> EntityType.HOGLIN;
            default -> null;
        };
    }

    // ════════════════════════════════════════════════
    //  BOUTONS NAVIGATION
    // ════════════════════════════════════════════════

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
    private static ItemStack makeFiller(Material mat) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m  = i.getItemMeta();
        m.setDisplayName("§r");
        i.setItemMeta(m); return i;
    }

    // ════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════

    private static void fillBorder(Inventory inv, Material mat) {
        ItemStack pane = makeFiller(mat);
        int size = inv.getSize();
        for (int i = 0; i < 9; i++)           inv.setItem(i, pane);
        for (int i = size - 9; i < size; i++) inv.setItem(i, pane);
        for (int i = 0; i < size; i += 9)     inv.setItem(i, pane);
        for (int i = 8; i < size; i += 9)     inv.setItem(i, pane);
    }

    public static String fmtCoins(long n) {
        if (n >= 1_000_000_000) return (n / 1_000_000_000) + "G";
        if (n >= 1_000_000) {
            long m = n / 1_000_000, k = (n % 1_000_000) / 1_000;
            return k > 0 ? m + "M" + k + "K" : m + "M";
        }
        if (n >= 1_000) return (n / 1_000) + "K";
        return String.valueOf(n);
    }

    // Exposer les slots catégories pour ShopListener
    public static int[] getCatSlots() { return CAT_SLOTS; }
    public static int[] getContentSlots() { return CONTENT_SLOTS; }
}