package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.customitem.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Gère la sacoche de marchand : vente automatique des items ramassés.
 *
 * OPTIMISATION CRITIQUE :
 *  - Map<UUID player → UUID pouchId> maintenue à la connexion/déconnexion
 *  - Plus de scan de 36 slots par joueur toutes les secondes
 *  - Scan uniquement pour les joueurs qui ONT une sacoche active
 *  - Scan déclenché sur EntityPickupItemEvent (event-driven) plutôt que ticker
 *
 * NOTE : La logique de tick (vérification expiration) reste en ticker
 *        mais ne concerne que les joueurs avec sacoche.
 */
public class MerchantPouchTickListener implements Listener {

    // Index rapide : player UUID → pouchId actif
    // Mis à jour au join, à l'équipement/déséquipement
    private static final Map<UUID, UUID> activePouches = new HashMap<>();

    // ════════════════════════════════════════════════
    //  DÉMARRAGE — ticker de vérification expiration
    // ════════════════════════════════════════════════

    public static void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Vérifier uniquement les joueurs avec sacoche active
                for (Map.Entry<UUID, UUID> entry : new HashMap<>(activePouches).entrySet()) {
                    UUID pouchId = entry.getValue();
                    if (MerchantPouchData.get().isExpired(pouchId)) {
                        Player p = Bukkit.getPlayer(entry.getKey());
                        if (p != null) {
                            p.sendMessage("§6⚠ §fTa Sacoche de Marchand §ca expiré !");
                        }
                        activePouches.remove(entry.getKey());
                    }
                }
            }
        }.runTaskTimer(SkyBlockPlugin.getInstance(), 200L, 200L); // toutes les 10s
    }

    // ════════════════════════════════════════════════
    //  JOIN — scanner l'inventaire une seule fois
    // ════════════════════════════════════════════════

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        UUID pouchId = findPouchInInventory(p);
        if (pouchId != null) {
            activePouches.put(p.getUniqueId(), pouchId);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        activePouches.remove(event.getPlayer().getUniqueId());
    }

    // ════════════════════════════════════════════════
    //  PICKUP — vente event-driven (plus de ticker 1/s)
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;

        UUID pouchId = activePouches.get(p.getUniqueId());
        if (pouchId == null) return;

        if (MerchantPouchData.get().isExpired(pouchId)) {
            activePouches.remove(p.getUniqueId());
            return;
        }

        ItemStack item = event.getItem().getItemStack();
        long earned = SellWandHelper.sellStack(p, item, 1.0);
        if (earned > 0) {
            event.setCancelled(true);
            SkyBlockPlugin.getInstance().getJobManager().displaySellCoins(p, earned);
        }
    }

    // ════════════════════════════════════════════════
    //  INVENTORY CLICK — détecter ajout/retrait sacoche
    // ════════════════════════════════════════════════

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player p)) return;
        // Rescanner à la fermeture d'inventaire (cas placement/retrait sacoche)
        UUID pouchId = findPouchInInventory(p);
        if (pouchId != null) {
            activePouches.put(p.getUniqueId(), pouchId);
        } else {
            activePouches.remove(p.getUniqueId());
        }
    }

    // ════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════

    /** Cherche une sacoche active dans l'inventaire — appelé UNE SEULE FOIS au join/fermeture invent. */
    private static UUID findPouchInInventory(Player p) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (item == null) continue;
            if (CustomItemManager.getType(item) != CustomItemType.MERCHANT_POUCH) continue;
            item = BagIdUtil.ensureId(item);
            UUID id = BagIdUtil.getBagId(item);
            if (id != null && !MerchantPouchData.get().isExpired(id)) return id;
        }
        return null;
    }

    /** Permet à d'autres classes de notifier qu'une sacoche a été équipée. */
    public static void notifyPouchEquipped(UUID playerUUID, UUID pouchId) {
        activePouches.put(playerUUID, pouchId);
    }

    public static void notifyPouchRemoved(UUID playerUUID) {
        activePouches.remove(playerUUID);
    }
}