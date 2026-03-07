package be.RedSwick.skyblock.gui;

import be.RedSwick.skyblock.island.Island;
import be.RedSwick.skyblock.island.IslandMission;
import be.RedSwick.skyblock.island.IslandMission.Category;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.*;

public class MissionGUI {

    public static final String TITLE_CATEGORIES = "§6✦ Missions de l'Île";
    // Titres des pages : "§6✦ Missions — FARMER — p1" etc
    public static String getPageTitle(Category cat, int page) {
        return "§6✦ Missions — " + cat.name() + " — p" + page;
    }
    public static boolean isPageTitle(String title) {
        return title.startsWith("§6✦ Missions — ") && title.contains(" — p");
    }

    // Slots de contenu (rangées 2-5, bordure exclue)
    private static final int[] CONTENT_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };
    private static final int PAGE_SIZE = CONTENT_SLOTS.length; // 28

    // ══════════════════════════════════════════════════════
    //  GUI CATÉGORIES — point d'entrée de /is missions
    // ══════════════════════════════════════════════════════

    public static Inventory createCategories(Island island) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_CATEGORIES);
        fillBorder(inv);

        // Résumé IS level slot 4
        inv.setItem(4, makeLevelItem(island));

        // 4 catégories aux slots 10, 12, 14, 16
        inv.setItem(10, makeCategoryItem(Category.FARMER,   island));
        inv.setItem(12, makeCategoryItem(Category.CHASSEUR, island));
        inv.setItem(14, makeCategoryItem(Category.BUCHERON, island));
        inv.setItem(16, makeCategoryItem(Category.MINEUR,   island));

        inv.setItem(22, makeCloseButton());
        return inv;
    }

    // ══════════════════════════════════════════════════════
    //  GUI PAGE — liste des missions d'une catégorie
    // ══════════════════════════════════════════════════════

    public static Inventory createPage(Category cat, Island island, int page) {
        String title = getPageTitle(cat, page);
        Inventory inv = Bukkit.createInventory(null, 54, title);
        fillBorder(inv);

        List<IslandMission> missions = Arrays.stream(IslandMission.values())
                .filter(m -> m.getCategory() == cat)
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) missions.size() / PAGE_SIZE);
        int from = (page - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, missions.size());

        List<IslandMission> pageMissions = missions.subList(from, to);

        for (int i = 0; i < pageMissions.size(); i++) {
            inv.setItem(CONTENT_SLOTS[i], makeMissionItem(pageMissions.get(i), island));
        }

        // Navigation
        if (page > 1)          inv.setItem(45, makePrevButton(page));
        if (page < totalPages) inv.setItem(53, makeNextButton(page));

        inv.setItem(49, makeBackButton());
        return inv;
    }

    // ─────────────────────────────────────────────
    //  Items
    // ─────────────────────────────────────────────

    private static ItemStack makeLevelItem(Island island) {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta  = item.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName("§b§lNiveau Île : §e" + fmt(island.getIsLevel()));

        long total     = IslandMission.values().length;
        long completed = Arrays.stream(IslandMission.values())
                .filter(island::isMissionCompleted).count();

        meta.setLore(Arrays.asList(
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Missions : §e" + completed + " §7/ §e" + total,
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeCategoryItem(Category cat, Island island) {
        Material mat = switch (cat) {
            case FARMER   -> Material.WHEAT;
            case CHASSEUR -> Material.IRON_SWORD;
            case BUCHERON -> Material.IRON_AXE;
            case MINEUR   -> Material.IRON_PICKAXE;
        };
        String label = switch (cat) {
            case FARMER   -> "§aFarmer";
            case CHASSEUR -> "§cChasseur";
            case BUCHERON -> "§6Bûcheron";
            case MINEUR   -> "§7Mineur";
        };

        long total = Arrays.stream(IslandMission.values())
                .filter(m -> m.getCategory() == cat).count();
        long done  = Arrays.stream(IslandMission.values())
                .filter(m -> m.getCategory() == cat && island.isMissionCompleted(m)).count();

        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName(label);
        meta.setLore(Arrays.asList(
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Complétées : §e" + done + " §7/ §e" + total,
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§eClic pour voir"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeMissionItem(IslandMission mission, Island island) {
        boolean completed = island.isMissionCompleted(mission);
        int progress  = island.getMissionProgress(mission);
        int required  = mission.getTarget().required();

        Material mat = getMissionMaterial(mission);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();

        if (completed) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.setDisplayName("§a§l✔ " + mission.getName());
        } else {
            meta.setDisplayName("§e" + mission.getName());
        }

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7" + mission.getDescription());
        lore.add("");
        if (completed) {
            lore.add("§a✔ Complétée ! §b+" + fmt(mission.getIsLevelReward()) + " IS");
        } else {
            lore.add(makeBar(progress, required));
            lore.add("§7" + fmtInt(progress) + " §7/ §e" + fmtInt(required));
            lore.add("§7Récompense : §b+" + fmt(mission.getIsLevelReward()) + " IS Level");
        }
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makePrevButton(int page) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName("§7◀ Page " + (page - 1));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeNextButton(int page) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName("§7Page " + (page + 1) + " ▶");
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeBackButton() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName("§7◀ Catégories");
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName("§cFermer");
        item.setItemMeta(meta);
        return item;
    }

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    private static Material getMissionMaterial(IslandMission mission) {
        if (mission.getTarget().isMob()) {
            return switch (mission.getTarget().entityType()) {
                case ZOMBIE          -> Material.ROTTEN_FLESH;
                case SKELETON        -> Material.BONE;
                case CREEPER         -> Material.GUNPOWDER;
                case WITCH           -> Material.GLASS_BOTTLE;
                case IRON_GOLEM      -> Material.IRON_INGOT;
                case CHICKEN         -> Material.FEATHER;
                case COW             -> Material.BEEF;
                case PIG             -> Material.PORKCHOP;
                case POLAR_BEAR      -> Material.SNOW_BLOCK;
                case RABBIT          -> Material.RABBIT;
                case SPIDER          -> Material.STRING;
                case GUARDIAN        -> Material.PRISMARINE_SHARD;
                case SLIME           -> Material.SLIME_BALL;
                case BLAZE           -> Material.BLAZE_ROD;
                case ENDERMAN        -> Material.ENDER_PEARL;
                case MAGMA_CUBE      -> Material.MAGMA_CREAM;
                case PIGLIN          -> Material.GOLD_INGOT;
                case WITHER_SKELETON -> Material.WITHER_SKELETON_SKULL;
                default              -> Material.PAPER;
            };
        }
        Material m = mission.getTarget().material();
        return m != null ? m : Material.PAPER;
    }

    private static String makeBar(int current, int max) {
        int bars   = 20;
        int filled = max == 0 ? 0 : (int) Math.min(bars, (double) current / max * bars);
        StringBuilder sb = new StringBuilder("§7[");
        for (int i = 0; i < bars; i++) sb.append(i < filled ? "§a|" : "§8|");
        sb.append("§7]");
        return sb.toString();
    }

    private static String fmt(double v) {
        if (v >= 1_000_000) return String.format("%.0fM", v / 1_000_000);
        if (v >= 1_000)     return String.format("%.0fk", v / 1_000);
        return String.format("%.0f", v);
    }

    private static String fmtInt(int n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fk", n / 1_000.0);
        return String.valueOf(n);
    }

    private static void fillBorder(Inventory inv) {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta  = pane.getItemMeta();
        meta.setDisplayName("§r");
        pane.setItemMeta(meta);
        int size = inv.getSize();
        for (int i = 0; i < 9; i++)          inv.setItem(i, pane);
        for (int i = size - 9; i < size; i++) inv.setItem(i, pane);
        for (int i = 0; i < size; i += 9)    inv.setItem(i, pane);
        for (int i = 8; i < size; i += 9)    inv.setItem(i, pane);
    }
}