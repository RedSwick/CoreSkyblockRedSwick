package be.RedSwick.skyblock.gui;

import be.RedSwick.skyblock.island.Island;
import be.RedSwick.skyblock.manager.IslandManager;
import be.RedSwick.skyblock.manager.WarpManager;
import be.RedSwick.skyblock.manager.WarpManager.SponsoredWarp;
import be.RedSwick.skyblock.player.PlayerData;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class WarpGUI {

    public static final String TITLE        = "§5✦ Warps des Îles";
    public static final String TITLE_SPONSOR = "§5✦ Sponsoriser mon Warp";

    // ════════════════════════════════════════════════
    //  GUI PRINCIPAL — liste des warps
    // ════════════════════════════════════════════════

    public static Inventory create(IslandManager islandManager,
                                   WarpManager warpManager) {

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fillBorder(inv);

        // ── Séparateur entre sponsored et normaux ──
        ItemStack sep = makePane(Material.PURPLE_STAINED_GLASS_PANE);
        for (int i = 9; i < 18; i++) inv.setItem(i, sep);

        // ── En-tête sponsored (rangée 1, slots 0-8 déjà en bordure noire) ──
        // On les place aux slots 1-5
        List<SponsoredWarp> sponsored = warpManager.getSponsoredWarps();
        int[] sponsoredSlots = {1, 2, 3, 4, 5};

        for (int i = 0; i < sponsoredSlots.length; i++) {
            if (i < sponsored.size()) {
                SponsoredWarp sw = sponsored.get(i);
                Island island = islandManager.getIsland(sw.islandOwner());
                if (island != null && island.hasWarp()) {
                    inv.setItem(sponsoredSlots[i],
                            makeSponsoredWarpItem(island, sw, islandManager));
                }
            } else {
                inv.setItem(sponsoredSlots[i], makeEmptySponsoredSlot(i + 1));
            }
        }

        // ── Warps normaux — triés par IS level décroissant ──
        List<Island> openWarps = islandManager.getAllIslands().stream()
                .filter(island -> island.hasWarp() && island.isOpen())
                .sorted((a, b) -> Double.compare(b.getIsLevel(), a.getIsLevel()))
                .toList();

        // Slots disponibles : rangées 3-5 = slots 18-26, 27-35, 36-44
        int[] warpSlots = {
                19,20,21,22,23,24,25,
                28,29,30,31,32,33,34,
                37,38,39,40,41,42,43
        };

        for (int i = 0; i < warpSlots.length && i < openWarps.size(); i++) {
            inv.setItem(warpSlots[i],
                    makeWarpItem(openWarps.get(i), islandManager));
        }

        // ── Bouton sponsoriser (slot 7) ──
        inv.setItem(7, makeSponsorButton());

        // ── Bouton fermer (slot 49) ──
        inv.setItem(49, makeCloseButton());

        return inv;
    }

    // ════════════════════════════════════════════════
    //  GUI SPONSOR — choisir la durée
    // ════════════════════════════════════════════════

    public static Inventory createSponsorMenu(PlayerData data) {

        Inventory inv = Bukkit.createInventory(null, 27, TITLE_SPONSOR);
        fillBorder(inv);

        long coins = data.getCoins();

        // Durées : 1h, 6h, 12h, 24h aux slots 10, 12, 14, 16
        int[] durations = {1, 6, 12, 24};
        int[] slots     = {10, 12, 14, 16};

        for (int i = 0; i < durations.length; i++) {
            int   duration = durations[i];
            long  price    = WarpManager.SPONSOR_PRICES.get(duration);
            boolean canAfford = coins >= price;
            inv.setItem(slots[i], makeDurationItem(duration, price, canAfford));
        }

        inv.setItem(22, makeCoinsItem(coins));
        inv.setItem(18, makeBackButton());

        return inv;
    }

    // ─────────────────────────────────────────────
    //  Items
    // ─────────────────────────────────────────────

    private static ItemStack makeSponsoredWarpItem(Island island,
                                                   SponsoredWarp sw,
                                                   IslandManager islandManager) {
        ItemStack item = new ItemStack(Material.AMETHYST_BLOCK);
        ItemMeta meta  = item.getItemMeta();

        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        String ownerName = getPlayerName(island.getOwner());
        meta.setDisplayName("§5§l★ §r§e" + ownerName + "§5 §l[SPONSORISÉ]");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7IS Level : §b" + formatLevel(island.getIsLevel()));
        lore.add("§7Membres  : §e" + island.getAllMembers().size());
        lore.add("§7Expire   : §c" + WarpManager.formatTimeLeft(sw.expiresAt()));
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§aClic pour téléporter !");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeEmptySponsoredSlot(int number) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName("§8Slot Sponsorisé #" + number + " §7— Libre");
        meta.setLore(Arrays.asList(
                "§7Aucun warp sponsorisé ici.",
                "§7Clique sur §e§lSPONSORISER §7pour",
                "§7mettre ton île en avant !"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeWarpItem(Island island, IslandManager islandManager) {
        // Tête du propriétaire au lieu de l'EnderPearl
        return be.RedSwick.skyblock.listener.WarpListener.makeOwnerHead(
                island.getOwner(),
                getPlayerName(island.getOwner()),
                island.getIsLevel(),
                island.getAllMembers().size()
        );
    }

    private static ItemStack makeSponsorButton() {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta  = item.getItemMeta();

        meta.setDisplayName("§5§l★ Sponsoriser mon Warp");
        meta.setLore(Arrays.asList(
                "§7Mets ton île en avant dans",
                "§7les §5slots sponsorisés §7en haut !",
                "",
                "§71h  §e→ §610 000 Coins",
                "§76h  §e→ §640 000 Coins",
                "§712h §e→ §670 000 Coins",
                "§724h §e→ §6120 000 Coins",
                "",
                "§eClic pour ouvrir le menu !"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeDurationItem(int hours, long price, boolean canAfford) {
        Material mat = canAfford ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();

        meta.setDisplayName((canAfford ? "§a" : "§c") + hours + "h de sponsoring");

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Durée  : §e" + hours + "h");
        lore.add("§7Prix   : §6" + formatCoins(price) + " Coins");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(canAfford
                ? "§aClic pour acheter !"
                : "§cCoins insuffisants.");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeCoinsItem(long coins) {
        ItemStack item = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName("§6Tes Coins : §e" + formatCoins(coins));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName("§7◀ Retour");
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

    private static String getPlayerName(UUID uuid) {
        var player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : uuid.toString().substring(0, 8);
    }

    private static String formatLevel(double level) {
        if (level >= 1_000_000) return String.format("%.1fM", level / 1_000_000);
        if (level >= 1_000)     return String.format("%.1fk", level / 1_000);
        return String.format("%.1f", level);
    }

    private static String formatCoins(long coins) {
        if (coins >= 1_000_000) return String.format("%.1fM", coins / 1_000_000.0);
        if (coins >= 1_000)     return String.format("%.1fk", coins / 1_000.0);
        return String.valueOf(coins);
    }

    private static void fillBorder(Inventory inv) {
        ItemStack pane = makePane(Material.BLACK_STAINED_GLASS_PANE);
        int size = inv.getSize();
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = size - 9; i < size; i++) inv.setItem(i, pane);
        for (int i = 0; i < size; i += 9) inv.setItem(i, pane);
        for (int i = 8; i < size; i += 9) inv.setItem(i, pane);
    }

    private static ItemStack makePane(Material mat) {
        ItemStack p = new ItemStack(mat);
        ItemMeta m  = p.getItemMeta();
        m.setDisplayName("§r");
        p.setItemMeta(m);
        return p;
    }
}