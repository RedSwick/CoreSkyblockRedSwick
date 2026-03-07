package be.RedSwick.skyblock.customitem;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Utilitaire pour lire/écrire un UUID unique dans le lore d'un sac/sacoche.
 * Format lore caché : §0BAG:uuid
 */
public class BagIdUtil {

    private static final String BAG_PREFIX = "§0BAG:";

    public static UUID getBagId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) return null;
        for (String line : lore) {
            if (line.startsWith(BAG_PREFIX)) {
                try { return UUID.fromString(line.substring(BAG_PREFIX.length())); }
                catch (Exception ignored) {}
            }
        }
        return null;
    }

    /** Assigne un UUID au sac s'il n'en a pas encore. Retourne l'item mis à jour. */
    public static ItemStack ensureId(ItemStack item) {
        if (getBagId(item) != null) return item;
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add(BAG_PREFIX + UUID.randomUUID());
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** Met à jour une ligne dans le lore d'un sac (ex: affichage de la durée restante). */
    public static void updateLoreLine(ItemStack item, String prefix, String newLine) {
        if (!item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        boolean found = false;
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).startsWith(prefix)) {
                lore.set(i, newLine);
                found = true;
                break;
            }
        }
        if (!found) lore.add(newLine);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}