package be.RedSwick.skyblock.gui;

import be.RedSwick.skyblock.moderation.StaffLogManager;
import be.RedSwick.skyblock.moderation.StaffLogManager.LogEntry;
import be.RedSwick.skyblock.moderation.StaffRank;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;

import java.util.*;

public class StaffLogsGUI {

    public static final String TITLE_MAIN   = "StaffLogs|main";
    public static final String TITLE_DETAIL = "StaffLogs|detail|";

    private static final int[] CONTENT_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };

    // ════════════════════════════════════════════════
    //  GUI PRINCIPAL — têtes des staffs visibles
    // ════════════════════════════════════════════════

    public static Inventory createMain(Player viewer) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_MAIN);
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);

        StaffRank viewerRank = StaffRank.of(viewer);

        // Header
        ItemStack header = new ItemStack(Material.BOOK);
        ItemMeta hm = header.getItemMeta();
        hm.setDisplayName("§8⚔ §6§lStaff Logs");
        hm.setLore(List.of("§7" + viewerRank.getDisplay() + " §7— 3 derniers jours"));
        header.setItemMeta(hm);
        inv.setItem(4, header);

        // Collecte les staffs visibles (connectés + offline avec logs)
        List<String> visibleNames = getVisibleStaffNames(viewer, viewerRank);

        int i = 0;
        for (String name : visibleNames) {
            if (i >= CONTENT_SLOTS.length) break;
            inv.setItem(CONTENT_SLOTS[i++], makeHead(name));
        }

        return inv;
    }

    // ════════════════════════════════════════════════
    //  GUI DÉTAIL — logs d'un staff
    // ════════════════════════════════════════════════

    public static Inventory createDetail(Player viewer, String staffName) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_DETAIL + staffName);
        fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);

        List<LogEntry> entries = StaffLogManager.get().getLogs(staffName);

        // Tête du staff
        ItemStack head = makeHead(staffName);
        if (head.getItemMeta() instanceof SkullMeta sm) {
            sm.setDisplayName("§e§l" + staffName);
            sm.setLore(List.of(
                    getRankDisplay(staffName),
                    "§7" + entries.size() + " action(s) — 3 derniers jours"
            ));
            head.setItemMeta(sm);
        }
        inv.setItem(4, head);

        // Retour
        inv.setItem(49, makeBack());

        if (entries.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta em = empty.getItemMeta();
            em.setDisplayName("§cAucune action récente");
            em.setLore(List.of("§7Rien dans les 3 derniers jours."));
            empty.setItemMeta(em);
            inv.setItem(22, empty);
            return inv;
        }

        for (int i = 0; i < Math.min(entries.size(), CONTENT_SLOTS.length); i++) {
            inv.setItem(CONTENT_SLOTS[i], makeLogItem(entries.get(i), i + 1));
        }

        return inv;
    }

    // ════════════════════════════════════════════════
    //  VISIBILITÉ selon hiérarchie
    // ════════════════════════════════════════════════

    /**
     * Règles :
     * FONDATEUR → voit tout le monde (fondateur, admin, mod, guide)
     * ADMIN     → voit admins, modérateurs, guides (pas fondateurs)
     * MODERATEUR→ voit modérateurs + guides (pas admins ni fondateurs)
     * GUIDE     → voit uniquement les guides
     */
    private static List<String> getVisibleStaffNames(Player viewer, StaffRank viewerRank) {
        Set<String> names = new LinkedHashSet<>();

        // Ajoute les joueurs connectés
        for (Player p : Bukkit.getOnlinePlayers()) {
            StaffRank pRank = StaffRank.of(p);
            if (pRank == StaffRank.NONE) continue;
            if (canSee(viewerRank, pRank)) names.add(p.getName());
        }

        // Ajoute les staffs offline avec logs récents
        for (String name : StaffLogManager.get().getActiveStaff()) {
            if (names.contains(name)) continue;
            StaffRank offlineRank = StaffLogManager.get().getStoredRank(name);
            if (canSee(viewerRank, offlineRank)) names.add(name);
        }

        return new ArrayList<>(names);
    }

    private static boolean canSee(StaffRank viewer, StaffRank target) {
        if (target == StaffRank.NONE) return false;
        return switch (viewer) {
            case FONDATEUR  -> true; // voit tout le monde
            case ADMIN      -> target.getLevel() <= StaffRank.ADMIN.getLevel();    // admin, mod, guide
            case MODERATEUR -> target.getLevel() <= StaffRank.MODERATEUR.getLevel(); // mod, guide
            case GUIDE      -> target == StaffRank.GUIDE; // guides seulement
            default         -> false;
        };
    }

    // ════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════

    @SuppressWarnings("deprecation")
    private static ItemStack makeHead(String name) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        Player online = Bukkit.getPlayer(name);
        if (online != null) meta.setOwningPlayer(online);
        else meta.setOwningPlayer(Bukkit.getOfflinePlayer(name));

        List<LogEntry> logs = StaffLogManager.get().getLogs(name);
        String rankDisplay = getRankDisplay(name);

        List<String> lore = new ArrayList<>();
        lore.add(rankDisplay);
        lore.add("§7" + logs.size() + " action(s) sur 3 jours");
        if (!logs.isEmpty()) {
            lore.add("§8Dernière : §7" + logs.get(0).action() + " → §e" + logs.get(0).target());
        }
        lore.add("");
        lore.add("§aClic §7pour voir les logs");

        meta.setDisplayName((online != null ? "§a" : "§7") + name);
        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private static String getRankDisplay(String name) {
        Player p = Bukkit.getPlayer(name);
        if (p != null) return StaffRank.of(p).getDisplay();
        StaffRank stored = StaffLogManager.get().getStoredRank(name);
        return stored.getDisplay() + " §8(hors ligne)";
    }

    private static ItemStack makeLogItem(LogEntry entry, int index) {
        Material mat = switch (entry.action().split(" ")[0].toUpperCase()) {
            case "MUTE"          -> Material.NAME_TAG;
            case "UNMUTE"        -> Material.LIME_DYE;
            case "KICK"          -> Material.LEATHER_BOOTS;
            case "BAN"           -> Material.BARRIER;
            case "TEMPBAN"       -> Material.CLOCK;
            case "WARN"          -> Material.ORANGE_DYE;
            case "UNBAN"         -> Material.GREEN_DYE;
            case "TP", "TPHERE",
                 "TPISLAND"      -> Material.ENDER_PEARL;
            case "VANISH"        -> Material.PHANTOM_MEMBRANE;
            case "SERVER"        -> Material.REDSTONE;
            default              -> Material.PAPER;
        };

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§f#" + index + " §e" + entry.action() + " §7→ §f" + entry.target());
        meta.setLore(List.of(
                "§8" + formatTime(entry.timestamp()),
                entry.detail().isEmpty() ? "§7—" : "§7" + entry.detail()
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeBack() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§7← Retour");
        item.setItemMeta(meta);
        return item;
    }

    private static String formatTime(long ts) {
        long min = (System.currentTimeMillis() - ts) / 60_000;
        if (min < 1)    return "Il y a quelques secondes";
        if (min < 60)   return "Il y a " + min + "min";
        if (min < 1440) return "Il y a " + (min / 60) + "h";
        return "Il y a " + (min / 1440) + "j";
    }

    private static void fillBorder(Inventory inv, Material mat) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta m = pane.getItemMeta();
        m.setDisplayName("§r");
        pane.setItemMeta(m);
        int size = inv.getSize();
        for (int i = 0; i < 9; i++)          inv.setItem(i, pane);
        for (int i = size-9; i < size; i++)   inv.setItem(i, pane);
        for (int i = 9; i < size-9; i += 9)  inv.setItem(i, pane);
        for (int i = 17; i < size-9; i += 9) inv.setItem(i, pane);
    }
}