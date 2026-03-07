package be.RedSwick.skyblock.gui;

import be.RedSwick.skyblock.player.*;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RankGUI {

    public static final String TITLE = "§5✦ Progression des Rangs";

    /**
     * Exigences fictives pour monter au rang suivant.
     * Format : essence requise, description des conditions
     */
    private static final Map<PlayerRank, Long> ESSENCE_REQUIRED = new LinkedHashMap<>();
    private static final Map<PlayerRank, List<String>> RANK_REQUIREMENTS = new LinkedHashMap<>();

    static {
        // SERVITEUR → SENTINELLE
        ESSENCE_REQUIRED.put(PlayerRank.SENTINELLE, 500L);
        RANK_REQUIREMENTS.put(PlayerRank.SENTINELLE, Arrays.asList(
                "§7• §f500 §bEssence Arcanium",
                "§7• §fNiveau île §e5",
                "§7• §f10 mobs tués"
        ));
        // SENTINELLE → DISCIPLE
        ESSENCE_REQUIRED.put(PlayerRank.DISCIPLE, 2_000L);
        RANK_REQUIREMENTS.put(PlayerRank.DISCIPLE, Arrays.asList(
                "§7• §f2 000 §bEssence Arcanium",
                "§7• §fNiveau île §e10",
                "§7• §f1 métier niveau §e5"
        ));
        // DISCIPLE → ADEPTE
        ESSENCE_REQUIRED.put(PlayerRank.ADEPTE, 8_000L);
        RANK_REQUIREMENTS.put(PlayerRank.ADEPTE, Arrays.asList(
                "§7• §f8 000 §bEssence Arcanium",
                "§7• §fNiveau île §e20",
                "§7• §f50 mobs tués"
        ));
        // ADEPTE → INVOCATEUR
        ESSENCE_REQUIRED.put(PlayerRank.INVOCATEUR, 25_000L);
        RANK_REQUIREMENTS.put(PlayerRank.INVOCATEUR, Arrays.asList(
                "§7• §f25 000 §bEssence Arcanium",
                "§7• §fNiveau île §e30",
                "§7• §f1 carte obtenue"
        ));
        // INVOCATEUR → MYSTIQUE
        ESSENCE_REQUIRED.put(PlayerRank.MYSTIQUE, 75_000L);
        RANK_REQUIREMENTS.put(PlayerRank.MYSTIQUE, Arrays.asList(
                "§7• §f75 000 §bEssence Arcanium",
                "§7• §fNiveau île §e45",
                "§7• §fTous métiers niveau §e10"
        ));
        // MYSTIQUE → ARCANISTE
        ESSENCE_REQUIRED.put(PlayerRank.ARCANISTE, 200_000L);
        RANK_REQUIREMENTS.put(PlayerRank.ARCANISTE, Arrays.asList(
                "§7• §f200 000 §bEssence Arcanium",
                "§7• §fNiveau île §e60",
                "§7• §f5 cartes obtenues"
        ));
        // ARCANISTE → MAITRE
        ESSENCE_REQUIRED.put(PlayerRank.MAITRE, 500_000L);
        RANK_REQUIREMENTS.put(PlayerRank.MAITRE, Arrays.asList(
                "§7• §f500 000 §bEssence Arcanium",
                "§7• §fNiveau île §e75",
                "§7• §f1 Autel construit"
        ));
        // MAITRE → SEIGNEUR
        ESSENCE_REQUIRED.put(PlayerRank.SEIGNEUR, 1_200_000L);
        RANK_REQUIREMENTS.put(PlayerRank.SEIGNEUR, Arrays.asList(
                "§7• §f1 200 000 §bEssence Arcanium",
                "§7• §fNiveau île §e85",
                "§7• §f10 cartes obtenues"
        ));
        // SEIGNEUR → SOUVERAIN
        ESSENCE_REQUIRED.put(PlayerRank.SOUVERAIN, 3_000_000L);
        RANK_REQUIREMENTS.put(PlayerRank.SOUVERAIN, Arrays.asList(
                "§7• §f3 000 000 §bEssence Arcanium",
                "§7• §fNiveau île §e90",
                "§7• §fTous métiers niveau §e25"
        ));
        // SOUVERAIN → ARCHIMAGE
        ESSENCE_REQUIRED.put(PlayerRank.ARCHIMAGE, 8_000_000L);
        RANK_REQUIREMENTS.put(PlayerRank.ARCHIMAGE, Arrays.asList(
                "§7• §f8 000 000 §bEssence Arcanium",
                "§7• §fNiveau île §e95",
                "§7• §f25 cartes obtenues"
        ));
        // ARCHIMAGE → ETERNEL
        ESSENCE_REQUIRED.put(PlayerRank.ETERNEL, 25_000_000L);
        RANK_REQUIREMENTS.put(PlayerRank.ETERNEL, Arrays.asList(
                "§7• §f25 000 000 §bEssence Arcanium",
                "§7• §fNiveau île §e100 §c(MAX)",
                "§7• §fToutes les cartes obtenues",
                "§7• §fTous métiers niveau §e50 §c(MAX)"
        ));
    }

    public static Inventory create(PlayerData data) {

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        fillBorder(inv);

        PlayerRank currentRank = data.getRank();
        PlayerRank[] allRanks  = PlayerRank.values();

        // ── Rang actuel affiché au centre haut (slot 4) ──
        inv.setItem(4, makeCurrentRankItem(data));

        // ── Tous les rangs affichés en ligne du milieu (slots 10 à 21, max 12 rangs) ──
        int[] rankSlots = {10, 11, 12, 13, 14, 15, 16, 28, 29, 30, 31, 32};
        for (int i = 0; i < allRanks.length; i++) {
            if (i >= rankSlots.length) break;
            inv.setItem(rankSlots[i], makeRankItem(allRanks[i], currentRank, data));
        }

        // ── Bouton Upgrade (slot 49) ──
        inv.setItem(49, makeUpgradeButton(data));

        // ── Bouton Fermer (slot 45) ──
        inv.setItem(45, makeCloseButton());

        // ── Info essence (slot 53) ──
        inv.setItem(53, makeEssenceInfo(data));

        return inv;
    }

    // ─────────────────────────────────────────────
    //  Items
    // ─────────────────────────────────────────────

    private static ItemStack makeCurrentRankItem(PlayerData data) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta  = item.getItemMeta();

        meta.setDisplayName("§5§lTon Rang Actuel");
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("  " + data.getRank().getDisplay());
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Essence : §b" + formatNumber(data.getEssence()));
        lore.add("§7Niveau joueur : §e" + data.getLevel());

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeRankItem(PlayerRank rank, PlayerRank current, PlayerData data) {

        boolean isUnlocked  = rank.ordinal() < current.ordinal();
        boolean isCurrent   = rank == current;
        boolean isNext      = rank.ordinal() == current.ordinal() + 1;

        Material mat;
        if (isUnlocked)   mat = Material.LIME_STAINED_GLASS_PANE;
        else if (isCurrent) mat = Material.PURPLE_STAINED_GLASS_PANE;
        else              mat = Material.GRAY_STAINED_GLASS_PANE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();

        meta.setDisplayName(rank.getDisplay() + (isCurrent ? " §7(Actuel)" : ""));

        List<String> lore = new ArrayList<>();

        if (isUnlocked) {
            lore.add("§a✔ Débloqué");

        } else if (isCurrent) {
            lore.add("§d▶ Rang actuel");
            PlayerRank next = rank.next();
            if (next != null) {
                lore.add("");
                lore.add("§7Prochain : " + next.getDisplay());
            }

        } else {
            lore.add("§c✘ Verrouillé");
            lore.add("");
            lore.add("§7Conditions :");
            List<String> reqs = RANK_REQUIREMENTS.get(rank);
            if (reqs != null) lore.addAll(reqs);
        }

        // Indicateur "SUIVANT" en surbrillance
        if (isNext) {
            lore.add("");
            lore.add("§e§l← Prochain rang");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeUpgradeButton(PlayerData data) {

        PlayerRank current = data.getRank();
        PlayerRank next    = current.next();

        if (next == null) {
            // Joueur ETERNEL
            ItemStack item = new ItemStack(Material.BEACON);
            ItemMeta meta  = item.getItemMeta();
            meta.setDisplayName("§6§l✦ RANG MAXIMUM ATTEINT ✦");
            meta.setLore(Arrays.asList("§7Tu as atteint le rang ultime.", "§5Tu es un §6§lÉternel§5."));
            item.setItemMeta(meta);
            return item;
        }

        long required = ESSENCE_REQUIRED.getOrDefault(next, Long.MAX_VALUE);
        boolean canUpgrade = data.getEssence() >= required;

        Material mat = canUpgrade ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();

        if (canUpgrade) {
            meta.setDisplayName("§a§l▶ Monter au rang " + next.getDisplay());
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.setDisplayName("§c§l✘ Rang " + next.getDisplay() + " §c(insuffisant)");
        }

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Essence requise : §b" + formatNumber(required));
        lore.add("§7Ton essence     : §b" + formatNumber(data.getEssence()));
        lore.add("");

        List<String> reqs = RANK_REQUIREMENTS.get(next);
        if (reqs != null) {
            lore.add("§7Conditions complètes :");
            lore.addAll(reqs);
        }

        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        if (canUpgrade) {
            lore.add("§a§l  CLIQUER POUR MONTER !");
        } else {
            long missing = required - data.getEssence();
            lore.add("§cIl te manque §f" + formatNumber(missing) + " §bEssence.");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeEssenceInfo(PlayerData data) {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName("§b✦ Ton Essence");
        meta.setLore(Arrays.asList(
                "§7Actuelle : §b" + formatNumber(data.getEssence()),
                "§7Totale gagnée : §5" + formatNumber(data.getTotalEssenceEarned())
        ));
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

    public static String formatNumber(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fk", n / 1_000.0);
        return String.valueOf(n);
    }

    public static Map<PlayerRank, Long> getEssenceRequired() {
        return ESSENCE_REQUIRED;
    }

    public static Map<PlayerRank, List<String>> getRankRequirements() {
        return RANK_REQUIREMENTS;
    }
}