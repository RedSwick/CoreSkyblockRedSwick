package be.RedSwick.skyblock.gui;

import be.RedSwick.skyblock.job.JobXpTable;
import be.RedSwick.skyblock.player.PlayerData;
import be.RedSwick.skyblock.player.PlayerJob;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class JobGUI {

    public static final String TITLE_MAIN = "§6✦ Tes Métiers";

    // Titres dynamiques
    public static String getLevelPageTitle(PlayerJob job, int page) {
        return "§6✦ " + job.getDisplay() + " — Niveaux p" + page;
    }
    public static boolean isLevelPageTitle(String title) {
        return title.startsWith("§6✦ ") && title.contains(" — Niveaux p");
    }

    public static String getDetailTitle(PlayerJob job) {
        return "§6✦ " + job.getDisplay() + " — Détail";
    }
    public static String getActionsTitle(PlayerJob job) {
        return "§6✦ " + job.getDisplay() + " — Actions";
    }

    private static final int[] CONTENT_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };
    private static final int PAGE_SIZE = CONTENT_SLOTS.length; // 28

    // ════════════════════════════════════════════════
    //  MENU PRINCIPAL — les 6 métiers
    // ════════════════════════════════════════════════

    public static Inventory createMain(PlayerData data) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_MAIN);
        fillBorder(inv);

        int[] slots = {20, 22, 24, 29, 31, 33};
        PlayerJob[] jobs = PlayerJob.values();

        for (int i = 0; i < jobs.length && i < slots.length; i++) {
            inv.setItem(slots[i], makeJobItem(jobs[i], data));
        }

        inv.setItem(49, makeCloseButton());
        return inv;
    }

    // ════════════════════════════════════════════════
    //  PAGE DE NIVEAUX (1-150) pour un métier
    // ════════════════════════════════════════════════

    public static Inventory createLevelPage(PlayerJob job, PlayerData data, int page) {
        String title = getLevelPageTitle(job, page);
        Inventory inv = Bukkit.createInventory(null, 54, title);
        fillBorder(inv);

        int currentLevel = data.getJobLevel(job);
        int from = (page - 1) * PAGE_SIZE + 1; // niveau de départ de cette page
        int to   = Math.min(from + PAGE_SIZE - 1, 150);

        int slotIdx = 0;
        for (int lvl = from; lvl <= to; lvl++) {
            inv.setItem(CONTENT_SLOTS[slotIdx], makeLevelItem(job, lvl, currentLevel));
            slotIdx++;
        }

        int totalPages = (int) Math.ceil(150.0 / PAGE_SIZE);

        if (page > 1)          inv.setItem(45, makePrevButton(page));
        if (page < totalPages) inv.setItem(53, makeNextButton(page));
        inv.setItem(49, makeBackButton());

        return inv;
    }

    // ─────────────────────────────────────────────
    //  Item d'un niveau dans la liste
    // ─────────────────────────────────────────────

    private static ItemStack makeLevelItem(PlayerJob job, int lvl, int currentLevel) {
        boolean done    = lvl < currentLevel;
        boolean current = lvl == currentLevel;
        boolean next    = lvl > currentLevel;

        // Matériau selon état
        Material mat = current ? Material.LIME_STAINED_GLASS_PANE
                : done   ? Material.GREEN_STAINED_GLASS_PANE
                : Material.RED_STAINED_GLASS_PANE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();

        // Couleur du titre
        String prefix = current ? "§a§l" : done ? "§a" : "§c";
        meta.setDisplayName(prefix + "Niveau " + lvl);

        if (current) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        // XP requise pour ce niveau
        long xpRequired = JobXpTable.getXpRequired(lvl); // déjà en XP lisibles
        lore.add("§7XP requise : §e" + fmt(xpRequired));

        // Récompenses
        JobLevelReward reward = getReward(job, lvl);
        if (reward.coins() > 0 || reward.gems() > 0 || reward.tag() != null) {
            lore.add("");
            lore.add("§7§lRécompenses :");
            if (reward.coins() > 0)
                lore.add("  §6⬡ §e" + fmt(reward.coins()) + " Coins");
            if (reward.gems() > 0)
                lore.add("  §b💎 §3" + reward.gems() + " Gemmes");
            if (reward.tag() != null)
                lore.add("  §6✦ §eTag §6[" + reward.tag() + "]");
        }

        if (current) lore.add("");
        if (current) lore.add("§a◀ Niveau actuel");
        if (done)    lore.add("§a✔ Complété");
        if (next)    lore.add("§c✗ Non atteint");

        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ─────────────────────────────────────────────
    //  Item job dans le menu principal
    // ─────────────────────────────────────────────

    private static ItemStack makeJobItem(PlayerJob job, PlayerData data) {
        int level = data.getJobLevel(job);
        long xpBuf = data.getJobXp(job);       // valeur brute ×100
        long xpReqBuf = JobXpTable.getXpRequired(level) * 100L; // ×100
        // Valeurs lisibles pour affichage
        long xpDisplay    = xpBuf / 100;
        long xpReqDisplay = JobXpTable.getXpRequired(level);

        ItemStack item = new ItemStack(getJobMaterial(job));
        ItemMeta meta  = item.getItemMeta();

        meta.setDisplayName("§6" + job.getDisplay());
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        int pct = (int) Math.min(100, (double) xpBuf / xpReqBuf * 100);

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Niveau : §e" + level + " §8/ §7150");
        lore.add("§7XP : §e" + fmt(xpDisplay) + " §7/ §e" + fmt(xpReqDisplay));
        lore.add(makeBar(xpBuf, xpReqBuf) + " §7" + pct + "%");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§eClic pour voir les niveaux");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ════════════════════════════════════════════════
    //  RÉCOMPENSES PAR NIVEAU
    // ════════════════════════════════════════════════

    public record JobLevelReward(long coins, int gems, String tag) {}

    public static JobLevelReward getReward(PlayerJob job, int level) {
        long coins = 0;
        int  gems  = 0;
        String tag = null;

        // Coins à chaque niveau (progressif)
        coins = switch (level) {
            case 1,2,3,4 -> 50;
            case 5       -> 200;
            case 10      -> 500;
            case 15      -> 750;
            case 20      -> 1_000;
            case 25      -> 2_000;
            case 30      -> 2_500;
            case 35      -> 3_000;
            case 40      -> 3_500;
            case 45      -> 4_000;
            case 50      -> 5_000;
            case 55      -> 5_500;
            case 60      -> 6_000;
            case 65      -> 6_500;
            case 70      -> 7_000;
            case 75      -> 8_000;
            case 80      -> 9_000;
            case 85      -> 10_000;
            case 90      -> 12_000;
            case 95      -> 14_000;
            case 100     -> 20_000;
            case 110     -> 25_000;
            case 120     -> 35_000;
            case 125     -> 40_000;
            case 130     -> 45_000;
            case 140     -> 60_000;
            case 150     -> 100_000;
            default      -> {
                // Petits coins à chaque niveau non-palier
                yield level % 5 == 0 ? 0 : 100 + (level * 10L);
            }
        };

        // Gemmes aux paliers 25 / 50 / 75 / 100 / 125 / 150
        gems = switch (level) {
            case 25  -> 5;
            case 50  -> 10;
            case 75  -> 15;
            case 100 -> 20;
            case 125 -> 25;
            case 150 -> 250;
            default  -> 0;
        };

        // Tag au niveau 150 uniquement
        if (level == 150) {
            tag = switch (job) {
                case CHASSEUR  -> "Chasseur";
                case FARMER    -> "Fermier";
                case MINER    -> "Mineur";
                case BUCHERON  -> "Bûcheron";
                case PECHEUR   -> "Pêcheur";
                case ALCHIMISTE-> "Alchimiste";
            };
        }

        return new JobLevelReward(coins, gems, tag);
    }

    // ─────────────────────────────────────────────
    //  Boutons navigation
    // ─────────────────────────────────────────────

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
        meta.setDisplayName("§7◀ Mes Métiers");
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
    //  Matériaux par métier
    // ─────────────────────────────────────────────

    private static Material getJobMaterial(PlayerJob job) {
        return switch (job) {
            case CHASSEUR   -> Material.IRON_SWORD;
            case FARMER     -> Material.WHEAT;
            case MINER     -> Material.IRON_PICKAXE;
            case BUCHERON   -> Material.IRON_AXE;
            case PECHEUR    -> Material.FISHING_ROD;
            case ALCHIMISTE -> Material.BREWING_STAND;
        };
    }

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    private static String makeBar(long current, long max) {
        int bars   = 20;
        int filled = max == 0 ? 0 : (int) Math.min(bars, (double) current / max * bars);
        StringBuilder sb = new StringBuilder("§7[");
        for (int i = 0; i < bars; i++) sb.append(i < filled ? "§a|" : "§8|");
        sb.append("§7]");
        return sb.toString();
    }

    private static String fmt(long n) {
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
        for (int i = 0; i < 9; i++)           inv.setItem(i, pane);
        for (int i = size - 9; i < size; i++) inv.setItem(i, pane);
        for (int i = 0; i < size; i += 9)     inv.setItem(i, pane);
        for (int i = 8; i < size; i += 9)     inv.setItem(i, pane);
    }
}