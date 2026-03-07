package be.RedSwick.skyblock.gui;

import be.RedSwick.skyblock.player.*;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GradeGUI {

    public static final String TITLE = "§5✦ Grades Arcanium";

    public static Inventory create(PlayerData data) {

        Inventory inv = Bukkit.createInventory(null, 45, TITLE);

        fillBorder(inv);

        // ── Séparateurs déco ──
        setGlassColumn(inv, 17, Material.CYAN_STAINED_GLASS_PANE);

        // ── Titre / Info joueur (slot 4) ──
        inv.setItem(4, makeInfoItem(data));

        // ── Les 3 grades (slots 20, 22, 24) ──
        inv.setItem(20, makeGradeItem(PlayerGrade.NEXUS,      data));
        inv.setItem(22, makeGradeItem(PlayerGrade.ASCENDANT,  data));
        inv.setItem(24, makeGradeItem(PlayerGrade.ARCANIUM,   data));

        // ── Bouton fermer (slot 40) ──
        inv.setItem(40, makeCloseButton());

        return inv;
    }

    // ─────────────────────────────────────────────
    //  Items
    // ─────────────────────────────────────────────

    private static ItemStack makeInfoItem(PlayerData data) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta  = item.getItemMeta();

        meta.setDisplayName("§5§lTon Grade Actuel");

        PlayerGrade current = data.getGrade();
        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("  " + current.getDisplay());
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        if (current == PlayerGrade.AUCUN) {
            lore.add("§7Tu n'as pas encore de grade.");
            lore.add("§7Les grades offrent du §econfort§7,");
            lore.add("§7du §estyle§7 et du §egain de temps§7.");
        } else {
            lore.add("§aMerci de ton soutien à Arcanium !");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeGradeItem(PlayerGrade grade, PlayerData data) {

        boolean owned = data.getGrade() == grade;

        Material mat = switch (grade) {
            case NEXUS     -> Material.CYAN_DYE;
            case ASCENDANT -> Material.BLUE_DYE;
            case ARCANIUM  -> Material.PURPLE_DYE;
            default        -> Material.GRAY_DYE;
        };

        // Bloc plus impressionnant si owned
        if (owned) {
            mat = switch (grade) {
                case NEXUS     -> Material.CYAN_TERRACOTTA;
                case ASCENDANT -> Material.BLUE_TERRACOTTA;
                case ARCANIUM  -> Material.PURPLE_TERRACOTTA;
                default        -> Material.GRAY_TERRACOTTA;
            };
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();

        String price = switch (grade) {
            case NEXUS     -> "4.99€";
            case ASCENDANT -> "9.99€";
            case ARCANIUM  -> "19.99€";
            default        -> "?";
        };

        if (owned) {
            meta.setDisplayName(grade.getPrefix() + "§a§l✔ Possédé");
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.setDisplayName(grade.getDisplay() + " §8- §e" + price);
        }

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        // Avantages par grade
        switch (grade) {
            case NEXUS -> {
                lore.add("§b§lAvantages Nexus :");
                lore.add("§7• §fFly sur ton île");
                lore.add("§7• §fCoins bonus x§e1.25");
                lore.add("§7• §fAccès au §eNick§f cosmétique");
                lore.add("§7• §fCouleur de chat §bcyan");
                lore.add("§7• §f+1 warp île");
            }
            case ASCENDANT -> {
                lore.add("§9§lAvantages Ascendant :");
                lore.add("§7• §fTout le grade §bNexus");
                lore.add("§7• §fCoins bonus x§e1.5");
                lore.add("§7• §fEssence bonus x§e1.25");
                lore.add("§7• §f+2 slots membres île");
                lore.add("§7• §fAccès §9trail §fcosmétique");
                lore.add("§7• §fCouleur de chat §9bleu");
            }
            case ARCANIUM -> {
                lore.add("§5§lAvantages Arcanium :");
                lore.add("§7• §fTout le grade §9Ascendant");
                lore.add("§7• §fCoins bonus x§e2.0");
                lore.add("§7• §fEssence bonus x§e1.5");
                lore.add("§7• §fDrop rate cartes x§e1.25");
                lore.add("§7• §fAccès §5aura§f cosmétique exclusive");
                lore.add("§7• §fPréfixe §5§l[ARCANIUM]§f en chat");
                lore.add("§7• §f+4 slots membres île");
            }
            default -> {}
        }

        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        if (owned) {
            lore.add("§a✔ Tu possèdes ce grade !");
        } else {
            lore.add("§e§lCLICKER pour accéder à la boutique");
            lore.add("§8(Boutique en ligne — bientôt disponible)");
        }

        meta.setLore(lore);
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
    //  Helpers déco
    // ─────────────────────────────────────────────

    private static void fillBorder(Inventory inv) {
        ItemStack pane = makePane(Material.BLACK_STAINED_GLASS_PANE);
        int size = inv.getSize();
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = size - 9; i < size; i++) inv.setItem(i, pane);
        for (int i = 0; i < size; i += 9) inv.setItem(i, pane);
        for (int i = 8; i < size; i += 9) inv.setItem(i, pane);
    }

    private static void setGlassColumn(Inventory inv, int slot, Material mat) {
        ItemStack pane = makePane(mat);
        inv.setItem(slot, pane);
    }

    private static ItemStack makePane(Material mat) {
        ItemStack p = new ItemStack(mat);
        ItemMeta m  = p.getItemMeta();
        m.setDisplayName("§r");
        p.setItemMeta(m);
        return p;
    }
}