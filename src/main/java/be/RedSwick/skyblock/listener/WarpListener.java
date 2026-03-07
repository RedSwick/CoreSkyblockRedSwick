package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.gui.WarpGUI;
import be.RedSwick.skyblock.island.Island;
import be.RedSwick.skyblock.manager.IslandManager;
import be.RedSwick.skyblock.manager.PlayerDataManager;
import be.RedSwick.skyblock.manager.WarpManager;
import be.RedSwick.skyblock.player.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * Gère tous les clics dans les GUIs WarpGUI et WarpGUI Sponsor.
 *
 * FONCTIONNALITÉS :
 *  - Bouton Fermer (slot 49) → closeInventory()
 *  - Warp normal (EnderPearl → tête du proprio) → TP avec délai 3s + vérif sol
 *  - Bouton Sponsoriser (slot 7) → ouvre createSponsorMenu()
 *  - Durées sponsor (slots 10/12/14/16) → achète le slot sponsorisé
 *  - Retour (slot 18 dans sponsor menu) → retour WarpGUI principal
 *  - Warps sponsorisés (slots 1-5) → TP avec délai 3s
 *
 *  TP SÉCURISÉ :
 *  - Vérifie qu'il y a un bloc sous le joueur (pas de void)
 *  - Si la destination n'est pas safe → cherche Y+1 à Y-10 autour
 *  - Délai 3s — annulé si le joueur bouge
 */
public class WarpListener implements Listener {

    private final IslandManager   islandManager   = SkyBlockPlugin.getInstance().getIslandManager();
    private final WarpManager     warpManager     = SkyBlockPlugin.getInstance().getWarpManager();
    private final PlayerDataManager pdm           = SkyBlockPlugin.getInstance().getPlayerDataManager();

    // Joueurs en attente de TP (UUID → destination)
    private final Map<UUID, Location> pending = new HashMap<>();

    // Slots des warps normaux dans le GUI 54
    private static final int[] WARP_SLOTS = {
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };
    private static final int[] SPONSORED_SLOTS = {1,2,3,4,5};
    private static final int[] SPONSOR_DURATION_SLOTS = {10,12,14,16};
    private static final int[] SPONSOR_DURATIONS       = {1, 6, 12, 24};

    // ════════════════════════════════════════════════
    //  CLICS GUI PRINCIPAL
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onWarpGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        // ── GUI PRINCIPAL ──────────────────────────────────────
        if (title.equals(WarpGUI.TITLE)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;

            int slot = event.getRawSlot();

            // Fermer
            if (slot == 49) {
                player.closeInventory();
                return;
            }

            // Bouton sponsoriser (slot 7)
            if (slot == 7) {
                Island island = islandManager.getIslandByMember(player.getUniqueId());
                if (island == null) {
                    player.sendMessage("§cTu n'as pas d'île !");
                    return;
                }
                if (!island.hasWarp()) {
                    player.sendMessage("§cDéfinis d'abord un warp avec §e/is warp create§c !");
                    return;
                }
                if (warpManager.isAlreadySponsored(island.getOwner())) {
                    player.sendMessage("§cTon île est déjà dans les slots sponsorisés !");
                    return;
                }
                if (warpManager.isSponsoredFull()) {
                    player.sendMessage("§cTous les slots sponsorisés sont occupés. Réessaie plus tard !");
                    return;
                }
                PlayerData data = pdm.get(player.getUniqueId());
                player.openInventory(WarpGUI.createSponsorMenu(data));
                return;
            }

            // Warps sponsorisés (slots 1-5)
            for (int i = 0; i < SPONSORED_SLOTS.length; i++) {
                if (slot == SPONSORED_SLOTS[i]) {
                    List<WarpManager.SponsoredWarp> sponsored = warpManager.getSponsoredWarps();
                    if (i < sponsored.size()) {
                        Island island = islandManager.getIsland(sponsored.get(i).islandOwner());
                        if (island != null && island.hasWarp()) {
                            scheduleWarpTp(player, island);
                        }
                    }
                    return;
                }
            }

            // Warps normaux
            for (int i = 0; i < WARP_SLOTS.length; i++) {
                if (slot == WARP_SLOTS[i]) {
                    // Récupérer l'île depuis le slot — on recrée la liste dans le même ordre
                    List<Island> openWarps = islandManager.getAllIslands().stream()
                            .filter(isl -> isl.hasWarp() && isl.isOpen())
                            .sorted((a, b) -> Double.compare(b.getIsLevel(), a.getIsLevel()))
                            .toList();
                    if (i < openWarps.size()) {
                        scheduleWarpTp(player, openWarps.get(i));
                    }
                    return;
                }
            }
        }

        // ── GUI SPONSOR ────────────────────────────────────────
        if (title.equals(WarpGUI.TITLE_SPONSOR)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;

            int slot = event.getRawSlot();

            // Retour
            if (slot == 18) {
                player.openInventory(WarpGUI.create(islandManager, warpManager));
                return;
            }

            // Durées
            for (int i = 0; i < SPONSOR_DURATION_SLOTS.length; i++) {
                if (slot == SPONSOR_DURATION_SLOTS[i]) {
                    handleSponsorBuy(player, SPONSOR_DURATIONS[i]);
                    return;
                }
            }
        }
    }

    // ════════════════════════════════════════════════
    //  LOGIQUE SPONSOR
    // ════════════════════════════════════════════════

    private void handleSponsorBuy(Player player, int hours) {
        Island island = islandManager.getIslandByMember(player.getUniqueId());
        if (island == null || !island.hasWarp()) {
            player.sendMessage("§cTu n'as pas d'île avec un warp !");
            player.closeInventory();
            return;
        }
        if (!island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§cSeul le chef de l'île peut sponsoriser !");
            return;
        }
        if (warpManager.isAlreadySponsored(island.getOwner())) {
            player.sendMessage("§cTon île est déjà sponsorisée !");
            return;
        }
        if (warpManager.isSponsoredFull()) {
            player.sendMessage("§cTous les slots sont pris !");
            return;
        }

        long price = WarpManager.SPONSOR_PRICES.getOrDefault(hours, Long.MAX_VALUE);
        PlayerData data = pdm.get(player.getUniqueId());
        if (data == null) return;

        if (data.getCoins() < price) {
            player.sendMessage("§cCoins insuffisants. Il te faut §6"
                    + String.format("%,d", price) + " §ccoins.");
            return;
        }

        data.addCoins(-price);
        pdm.savePlayer(player.getUniqueId());

        boolean ok = warpManager.addSponsored(island.getOwner(),
                Bukkit.getOfflinePlayer(island.getOwner()).getName(),
                hours * 3_600_000L,
                price);
        if (!ok) {
            // Remboursement si race condition
            data.addCoins(price);
            player.sendMessage("§cLes slots sont tous pris, tu as été remboursé.");
            return;
        }

        player.closeInventory();
        player.sendMessage("§a✔ Warp sponsorisé pendant §e" + hours + "h §a! (-§6"
                + String.format("%,d", price) + " coins§a)");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    // ════════════════════════════════════════════════
    //  TP AVEC DÉLAI 3S + VÉRIF SOL
    // ════════════════════════════════════════════════

    private void scheduleWarpTp(Player player, Island island) {
        Location dest = findSafeLocation(island.getWarpLocation());
        if (dest == null) {
            player.sendMessage("§cLe warp de cette île n'est pas accessible pour le moment.");
            return;
        }

        player.closeInventory();
        player.sendMessage("§7Téléportation dans §e3 secondes §7— ne bouge pas !");
        pending.put(player.getUniqueId(), dest);

        Bukkit.getScheduler().runTaskLater(SkyBlockPlugin.getInstance(), () -> {
            if (!pending.containsKey(player.getUniqueId())) return; // annulé car bougé
            pending.remove(player.getUniqueId());
            player.teleport(dest);
            player.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        }, 60L); // 3 secondes
    }

    /**
     * Trouve une position safe autour de la destination.
     * Vérifie qu'il y a un bloc solide sous le joueur et de l'air à sa position.
     */
    private Location findSafeLocation(Location dest) {
        if (dest == null || dest.getWorld() == null) return null;

        // Essayer la destination exacte et les Y adjacents
        for (int dy = 0; dy >= -10; dy--) {
            Location test = dest.clone().add(0, dy, 0);
            if (isSafe(test)) return test;
        }
        // Essayer au-dessus
        for (int dy = 1; dy <= 5; dy++) {
            Location test = dest.clone().add(0, dy, 0);
            if (isSafe(test)) return test;
        }
        return null;
    }

    private boolean isSafe(Location loc) {
        if (loc.getBlockY() < -64 || loc.getBlockY() > 320) return false;
        var world = loc.getWorld();
        var feet  = world.getBlockAt(loc);
        var head  = world.getBlockAt(loc.clone().add(0, 1, 0));
        var floor = world.getBlockAt(loc.clone().add(0, -1, 0));
        return feet.getType().isAir()
                && head.getType().isAir()
                && floor.getType().isSolid();
    }

    // ════════════════════════════════════════════════
    //  ANNULATION TP SI LE JOUEUR BOUGE
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!pending.containsKey(uuid)) return;
        Location from = event.getFrom();
        Location to   = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockZ() == to.getBlockZ()
                && from.getBlockY() == to.getBlockY()) return;
        pending.remove(uuid);
        event.getPlayer().sendMessage("§cTéléportation annulée — tu as bougé !");
    }

    // ════════════════════════════════════════════════
    //  TÊTE DU PROPRIO dans WarpGUI (makeWarpItem)
    // ════════════════════════════════════════════════

    /**
     * Crée un item tête de joueur pour afficher le proprio du warp.
     * Utilisé dans WarpGUI.makeWarpItem() — appelé statiquement depuis WarpGUI.
     */
    public static ItemStack makeOwnerHead(UUID ownerUuid, String ownerName,
                                          double isLevel, int memberCount) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerUuid));
        meta.setDisplayName("§e" + ownerName + "§7's Island");
        meta.setLore(List.of(
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7IS Level : §b" + fmtLevel(isLevel),
                "§7Membres  : §e" + memberCount,
                "§7Statut   : §aOuverte ✔",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§aClic pour téléporter !"
        ));
        head.setItemMeta(meta);
        return head;
    }

    private static String fmtLevel(double level) {
        if (level >= 1_000_000) return String.format("%.1fM", level / 1_000_000);
        if (level >= 1_000)     return String.format("%.1fk", level / 1_000);
        return String.format("%.1f", level);
    }
}