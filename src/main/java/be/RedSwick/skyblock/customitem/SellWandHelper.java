package be.RedSwick.skyblock.customitem;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.manager.PlayerDataManager;
import be.RedSwick.skyblock.player.PlayerData;
import be.RedSwick.skyblock.shop.ShopCategory;
import be.RedSwick.skyblock.shop.ShopItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class SellWandHelper {

    /**
     * OPTIMISATION CRITIQUE : sellMap précalculée UNE SEULE FOIS au chargement.
     * Avant : buildMap() était appelé à chaque sellStack() → reconstruction
     * d'une HashMap complète pour chaque bloc/mob/crop.
     * Avec la hoe 5x5 sur un champ 100×100 → des milliers d'appels en quelques secondes.
     */
    private static final Map<Material, ShopItem> SELL_MAP = buildMap();

    private static Map<Material, ShopItem> buildMap() {
        Map<Material, ShopItem> map = new HashMap<>();
        for (ShopCategory cat : ShopCategory.values()) {
            for (ShopItem si : cat.getItems()) {
                if (si.sellPrice() > 0) {
                    map.put(si.material(), si);
                }
            }
        }
        return map;
    }

    /** Vend tout le contenu d'un inventaire, retourne les coins gagnés. */
    public static long sellInventory(Player player, Inventory inv, double multiplier) {
        PlayerDataManager pdm = SkyBlockPlugin.getInstance().getPlayerDataManager();
        PlayerData data = pdm.get(player.getUniqueId());
        if (data == null) return 0;

        long total = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;
            if (CustomItemManager.isCustomItem(item)) continue;

            ShopItem si = SELL_MAP.get(item.getType());
            if (si == null) continue;

            long price = si.sellPrice();
            if (price <= 0) continue;

            total += (long) (price * item.getAmount() * multiplier);
            inv.setItem(i, null);
        }

        if (total > 0) {
            data.addCoins(total);
            // Dirty flag uniquement — pas de I/O ici
            pdm.savePlayer(player.getUniqueId());
        }
        return total;
    }

    /**
     * Vend un seul ItemStack, retourne les coins gagnés.
     * NE SAUVEGARDE PAS — le appelant accumule et sauvegarde en fin de traitement.
     * Cela évite 25 dirty-marks pour une hoe 5x5 (1 seul en fin de handleHoe).
     */
    public static long sellStack(Player player, ItemStack item, double multiplier) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) return 0;
        if (CustomItemManager.isCustomItem(item)) return 0;

        ShopItem si = SELL_MAP.get(item.getType());
        if (si == null) return 0;

        long price = si.sellPrice();
        if (price <= 0) return 0;

        PlayerData data = SkyBlockPlugin.getInstance()
                .getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return 0;

        long earned = (long) (price * item.getAmount() * multiplier);
        data.addCoins(earned);
        // PAS de savePlayer ici — appelé des milliers de fois par session hoe/hammer.
        // Le dirty flag est flush automatiquement toutes les 5 minutes par PlayerDataManager.
        return earned;
    }

    /**
     * Variante qui sauvegarde — pour les cas ponctuels (bâton de vente sur coffre).
     */
    public static long sellStackAndSave(Player player, ItemStack item, double multiplier) {
        long earned = sellStack(player, item, multiplier);
        if (earned > 0) {
            SkyBlockPlugin.getInstance().getPlayerDataManager().savePlayer(player.getUniqueId());
        }
        return earned;
    }
}