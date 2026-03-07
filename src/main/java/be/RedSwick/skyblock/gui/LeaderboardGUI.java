package be.RedSwick.skyblock.gui;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.leaderboard.LeaderboardManager;
import be.RedSwick.skyblock.leaderboard.LeaderboardType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI /classement — affiche le top 10 de chaque catégorie.
 */
public class LeaderboardGUI {

    public static final String TITLE_MAIN    = "§6✦ Classements";
    public static final String TITLE_PREFIX  = "§6✦ Top 10 — ";

    // ── Page principale : liste des classements ──
    public static Inventory createMain() {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_MAIN);
        fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);

        // Ligne 1 — Stats générales
        inv.setItem(10, catItem(LeaderboardType.COINS,       Material.GOLD_NUGGET));
        inv.setItem(11, catItem(LeaderboardType.LEVEL,       Material.EXPERIENCE_BOTTLE));
        inv.setItem(12, catItem(LeaderboardType.BLOCS_MINES, Material.DIAMOND_PICKAXE));
        inv.setItem(13, catItem(LeaderboardType.MOBS_TUES,   Material.NETHERITE_SWORD));
        inv.setItem(14, catItem(LeaderboardType.TEMPS_JEU,   Material.CLOCK));
        inv.setItem(15, catItem(LeaderboardType.NIVEAU_ILE,  Material.GRASS_BLOCK));

        // Ligne 2 — Jobs
        inv.setItem(19, catItem(LeaderboardType.JOB_CHASSEUR,   Material.BOW));
        inv.setItem(20, catItem(LeaderboardType.JOB_FARMER,     Material.WHEAT));
        inv.setItem(21, catItem(LeaderboardType.JOB_MINER,      Material.IRON_PICKAXE));
        inv.setItem(22, catItem(LeaderboardType.JOB_BUCHERON,   Material.IRON_AXE));
        inv.setItem(23, catItem(LeaderboardType.JOB_ALCHIMISTE, Material.BREWING_STAND));
        inv.setItem(24, catItem(LeaderboardType.JOB_PECHEUR,    Material.FISHING_ROD));

        // Ligne 3 — Stats spécifiques
        inv.setItem(28, catItem(LeaderboardType.POISSONS_PECHES, Material.COD));
        inv.setItem(29, catItem(LeaderboardType.BUCHES_COUPEES,   Material.OAK_LOG));
        inv.setItem(30, catItem(LeaderboardType.CULTURES_RECOLTEES, Material.WHEAT));

        // Info
        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§6✦ Classements Arcanium");
        im.setLore(List.of("§7Clique sur une catégorie", "§7pour voir le Top 10"));
        info.setItemMeta(im);
        inv.setItem(49, info);

        return inv;
    }

    // ── Page détail : top 10 d'un classement ──
    public static Inventory createDetail(LeaderboardType type, Player viewer) {
        String title = TITLE_PREFIX + type.getDisplay();
        Inventory inv = Bukkit.createInventory(null, 54, title);
        fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);

        LeaderboardManager lm = SkyBlockPlugin.getInstance().getLeaderboardManager();
        List<LeaderboardManager.LeaderboardEntry> top = lm.getTop(type);

        String[] medals      = {"§6§l#1", "§7§l#2", "§8§l#3", "§f#4", "§f#5", "§f#6", "§f#7", "§f#8", "§f#9", "§f#10"};
        Material[] medalMats = {
                Material.GOLD_BLOCK, Material.IRON_BLOCK, Material.COPPER_BLOCK,
                Material.STONE, Material.STONE, Material.STONE, Material.STONE, Material.STONE, Material.STONE, Material.STONE
        };
        int[] slots = {10,11,12,13,14,19,20,21,22,23};

        // Trouver le rang du viewer
        String viewerName = viewer.getName();
        int viewerRank = -1;
        for (int i = 0; i < top.size(); i++) {
            if (top.get(i).name().equalsIgnoreCase(viewerName)) { viewerRank = i + 1; break; }
        }

        for (int i = 0; i < 10; i++) {
            ItemStack item = new ItemStack(medalMats[i]);
            ItemMeta m = item.getItemMeta();

            if (i < top.size()) {
                LeaderboardManager.LeaderboardEntry e = top.get(i);
                m.setDisplayName(medals[i] + " §f" + e.name());
                List<String> lore = new ArrayList<>();
                lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬");
                lore.add("§7" + type.getDisplay() + " : " + type.getColor() + lm.formatValue(type, e.value()));
                if (e.name().equalsIgnoreCase(viewerName)) lore.add("§a← Vous !");
                m.setLore(lore);
            } else {
                m.setDisplayName(medals[i] + " §8---");
                m.setLore(List.of("§7Aucun joueur"));
            }
            item.setItemMeta(m);
            inv.setItem(slots[i], item);
        }

        // Ton rang si pas dans le top
        ItemStack myRank = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta mr = myRank.getItemMeta();
        if (viewerRank > 0) {
            mr.setDisplayName("§aTon rang : §f#" + viewerRank);
        } else {
            mr.setDisplayName("§7Tu n'es pas dans le top 10");
        }
        myRank.setItemMeta(mr);
        inv.setItem(31, myRank);

        // Retour
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName("§e← Retour");
        bm.setLore(List.of("§0NAV:back_leaderboard"));
        back.setItemMeta(bm);
        inv.setItem(45, back);

        // Info titre
        ItemStack title2 = new ItemStack(Material.NETHER_STAR);
        ItemMeta tm = title2.getItemMeta();
        tm.setDisplayName(type.getDisplay());
        tm.setLore(List.of("§7Top 10 " + type.getDisplay()));
        title2.setItemMeta(tm);
        inv.setItem(49, title2);

        return inv;
    }

    private static ItemStack catItem(LeaderboardType type, Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(type.getDisplay());
        m.setLore(List.of(
                "§7Voir le Top 10 " + type.getDisplay(),
                "",
                "§eCliquer pour ouvrir",
                "§0LB:" + type.getId()
        ));
        item.setItemMeta(m);
        return item;
    }

    private static void fillBorder(Inventory inv, Material mat) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta m = pane.getItemMeta(); m.setDisplayName("§r"); pane.setItemMeta(m);
        int size = inv.getSize();
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = size - 9; i < size; i++) inv.setItem(i, pane);
        for (int i = 0; i < size; i += 9) inv.setItem(i, pane);
        for (int i = 8; i < size; i += 9) inv.setItem(i, pane);
    }
}