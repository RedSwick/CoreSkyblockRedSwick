package be.RedSwick.skyblock.customitem;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Crée et identifie les items custom.
 * L'ID est stocké dans le lore (ligne cachée §0ID:xxx).
 * La durabilité custom est stockée dans une ligne §0DUR:xxx.
 */
public class CustomItemManager {

    private static final String ID_PREFIX  = "§0ID:";
    private static final String DUR_PREFIX = "§0DUR:";

    // ════════════════════════════════════════════════
    //  CRÉER UN ITEM
    // ════════════════════════════════════════════════

    public static ItemStack create(CustomItemType type) {
        ItemStack item = new ItemStack(type.getMaterial());
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(type.getDisplayName());
        meta.setUnbreakable(true); // Pas de casse Minecraft — on gère la dura nous-mêmes

        // Enchantements
        type.getEnchants().forEach((ench, lvl) -> meta.addEnchant(ench, lvl, true));

        // Lore
        meta.setLore(buildLore(type, type.getMaxDurability()));
        item.setItemMeta(meta);
        return item;
    }

    // ════════════════════════════════════════════════
    //  IDENTIFIER UN ITEM
    // ════════════════════════════════════════════════

    public static CustomItemType getType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        var lore = item.getItemMeta().getLore();
        if (lore == null) return null;
        for (String line : lore) {
            if (line.startsWith(ID_PREFIX))
                return CustomItemType.fromId(line.substring(ID_PREFIX.length()));
        }
        return null;
    }

    public static boolean isCustomItem(ItemStack item) {
        return getType(item) != null;
    }

    // ════════════════════════════════════════════════
    //  DURABILITÉ CUSTOM
    // ════════════════════════════════════════════════

    public static int getDurability(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        var lore = item.getItemMeta().getLore();
        if (lore == null) return 0;
        for (String line : lore) {
            if (line.startsWith(DUR_PREFIX)) {
                try { return Integer.parseInt(line.substring(DUR_PREFIX.length())); }
                catch (Exception ignored) {}
            }
        }
        CustomItemType type = getType(item);
        return type != null ? type.getMaxDurability() : 0;
    }

    /**
     * Réduit la durabilité de amount.
     * Si dura <= 0 :
     *  - item réparable → reste à 0 (grisé)
     *  - item non réparable → détruit (retourne null)
     */
    public static ItemStack useDurability(ItemStack item, int amount) {
        CustomItemType type = getType(item);
        if (type == null) return item;

        int cur = getDurability(item);
        if (cur <= 0) {
            // Déjà à 0 — bloque l'utilisation
            return type.isRepairable() ? item : null;
        }

        int newDur = Math.max(0, cur - amount);
        return setDurability(item, type, newDur);
    }

    public static ItemStack repair(ItemStack item) {
        CustomItemType type = getType(item);
        if (type == null || !type.isRepairable()) return item;
        return setDurability(item, type, type.getMaxDurability());
    }

    // ════════════════════════════════════════════════
    //  HELPERS INTERNES
    // ════════════════════════════════════════════════

    private static ItemStack setDurability(ItemStack item, CustomItemType type, int dur) {
        ItemMeta meta = item.getItemMeta();
        meta.setLore(buildLore(type, dur));
        item.setItemMeta(meta);
        return item;
    }

    private static List<String> buildLore(CustomItemType type, int dur) {
        List<String> lore = new ArrayList<>();

        // Ligne de durabilité visible
        if (type.getMaxDurability() > 0) {
            lore.add("§7Durabilité : §f" + dur + " §7/ §f" + type.getMaxDurability());
        }

        // Infos réparation
        if (type.isRepairable()) {
            lore.add("§7Réparation : §e" + type.getRepairLevels() + " niveaux §7+ §6" +
                    String.format("%,d", type.getRepairCoins()) + " coins");
            lore.add("§7Clic droit dans le vide pour réparer");
        }

        // Infos spécifiques
        if (type.isHoe() && type.getRadius() > 0) {
            int size = type.getRadius() * 2 + 1;
            lore.add("§7Rayon : §a" + size + "x" + size);
        }
        if (type.isSellWand()) {
            lore.add("§7Multiplicateur : §6x" + type.getSellMultiplier());
            lore.add("§7Utilisations : §f" + dur + "§7/§f" + type.getMaxDurability());
        }

        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        // Config / Réparation hint
        if (type == CustomItemType.HAMMER || type == CustomItemType.FARMERS_HOE_5X5
                || type == CustomItemType.AXE_3X3 || type.isMultitool()
                || type == CustomItemType.FISHING_NET || type.isSword()) {
            lore.add("§7Shift+Clic droit §8→ §eConfig & Réparation");
        } else if (type.isRepairable()) {
            lore.add("§7Shift+Clic droit §8→ §eRéparer");
        }
        if (type == CustomItemType.SEED_BAG || type == CustomItemType.LOOT_BAG) {
            lore.add("§7Clic droit §8→ §eOuvrir");
        }

        // Épées
        if (type.isSword()) {
            if (type.isAutoSellSword()) lore.add("§7Auto-vente : §aactivable");
            lore.add("§7Clic droit coffre §8→ §evente mobs drops");
        }

        // Anneaux
        if (type.isRing()) {
            lore.add("§7Bonus XP §f" + type.getRingJob().getDisplay()
                    + " §a+" + (int)(type.getRingBonus()*100) + "%");
            lore.add("§7Équiper en §eoffhand §7pour activer");
        }

        // Sac de graines
        if (type == CustomItemType.SEED_BAG) {
            lore.add("§7Stocke toutes vos graines");
            lore.add("§7Utilisé automatiquement par le §aPlanteur");
            lore.add("§7Clic droit §8→ §eouvrir");
        }

        // Sac de butin
        if (type == CustomItemType.LOOT_BAG) {
            lore.add("§7Stocke automatiquement 10 types d'items");
            lore.add("§7Clic droit §8→ §econfigurer / ouvrir");
        }

        // Sacoche de marchand
        if (type == CustomItemType.MERCHANT_POUCH) {
            lore.add("§7Vend automatiquement tout ce qui");
            lore.add("§7entre dans votre inventaire");
            lore.add("§7(uniquement si dans l'inventaire principal)");
            lore.add("§c⚠ Durée restante : §f60:00");
        }

        // Cristaux XP
        if (type.getCrystalXp() > 0) {
            lore.add("§7Donne §b" + (int)type.getCrystalXp() + " XP §7à un métier §6aléatoire");
            lore.add("§7Clic droit pour utiliser");
        }

        // Chunk Hopper
        if (type == CustomItemType.CHUNK_HOPPER) {
            lore.add("§7Collecte tous les drops du chunk");
            lore.add("§7au-dessus de lui");
            lore.add("§7Poser sur un coffre pour stocker");
            lore.add("§7Récupérer avec §eSilk Touch");
        }

        // Filet de pêche
        if (type == CustomItemType.FISHING_NET) {
            lore.add("§7Pêche §b3 poissons §7d'un coup");
            lore.add("§7XP Pêcheur §ax3");
        }

        // Seau infini
        if (type == CustomItemType.INFINITE_WATER_BUCKET) {
            lore.add("§7Utilisation §binfinie");
            lore.add("§7Ne se vide jamais");
        }

        // ID caché
        lore.add(ID_PREFIX + type.getId());
        lore.add(DUR_PREFIX + dur);

        return lore;
    }


}