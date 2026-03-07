package be.RedSwick.skyblock.customitem;

import be.RedSwick.skyblock.SkyBlockPlugin;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * SAC PERSONNEL (SimpleLootBag)
 *
 * Titre GUI  : "§6✦ Sac Personnel"  ← DIFFERENT de LootBagGUI "§6⬡ Sac de Butin"
 * Item       : PAPER avec UUID dans le lore (tag §0SLB_UUID:<uuid>)
 *
 * Comportement :
 *  - Clic droit → ouvre le GUI (54 slots, slots 0-44 libres)
 *  - Le joueur pose lui-même jusqu'à 10 types d'items (= filtres actifs)
 *  - Dépôt/retrait libres dans les slots 0-44 (comportement coffre normal)
 *  - Slots 45-53 = bordure protégée + bouton fermer
 *  - Quand un item filtré est ramassé au sol → stocké en YAML (illimité)
 *    → SEULEMENT si le sac contient déjà cet item (filtre actif)
 *    → Si plus de place pour stocker → item ramassé normalement dans l'inventaire
 *
 * Enregistrement : SkyBlockPlugin.onEnable() → registerEvents(new SimpleLootBag.Listener(), this)
 */
public class SimpleLootBag {

    // Titre DIFFERENT de LootBagGUI.TITLE_PREFIX pour éviter le conflit GuiProtectionListener
    public static final String GUI_TITLE  = "§6✦ Sac Personnel";
    private static final String UUID_TAG  = "§0SLB_UUID:";
    private static final int    MAX_TYPES = 10;
    private static final int    CONTENT   = 45; // slots 0-44 libres

    // ══════════════════════════════════════════════
    //  CRÉATION DE L'ITEM
    // ══════════════════════════════════════════════

    public static ItemStack createItem() {
        UUID id   = UUID.randomUUID();
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName("§6⬡ §lSac de Butin");
        meta.setLore(List.of(
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Clic droit §8→ §7ouvre le sac",
                "§7Max §e10 types §7d'items filtrés",
                "§7Stockage §billimité",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                UUID_TAG + id
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static UUID getBagId(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            if (line != null && line.startsWith(UUID_TAG)) {
                try { return UUID.fromString(line.substring(UUID_TAG.length())); }
                catch (Exception ignored) {}
            }
        }
        return null;
    }

    public static boolean isLootBag(ItemStack item) {
        return getBagId(item) != null;
    }

    // ══════════════════════════════════════════════
    //  GUI
    // ══════════════════════════════════════════════

    public static Inventory buildGui(UUID bagId) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Charger les items déjà posés
        List<ItemStack> saved = Storage.loadSlots(bagId);
        for (int i = 0; i < Math.min(saved.size(), CONTENT); i++) {
            if (saved.get(i) != null) inv.setItem(i, saved.get(i));
        }

        // Bordure slots 45-53
        ItemStack pane = pane();
        for (int i = 45; i < 54; i++) inv.setItem(i, pane);

        // Info slot 46
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta  im   = info.getItemMeta();
        im.setDisplayName("§6§l Sac Personnel");
        List<String> lore = new ArrayList<>();
        lore.add("§7Place un item dans les cases");
        lore.add("§7ci-dessus pour l'ajouter comme filtre.");
        lore.add("§7Max §e" + MAX_TYPES + " §7types différents.");
        lore.add("");
        // Afficher le stock actuel
        Map<Material, Long> stock = Storage.getStock(bagId);
        if (!stock.isEmpty()) {
            lore.add("§7Stock :");
            int shown = 0;
            for (Map.Entry<Material, Long> e : stock.entrySet()) {
                if (shown++ >= 6) { lore.add("§8..."); break; }
                lore.add("§8• §f" + fmtMat(e.getKey()) + " §7→ §e" + fmtNum(e.getValue()));
            }
        } else {
            lore.add("§8Stock vide.");
        }
        im.setLore(lore);
        info.setItemMeta(im);
        inv.setItem(46, info);

        // Fermer slot 49
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta  cm    = close.getItemMeta();
        cm.setDisplayName("§cFermer");
        close.setItemMeta(cm);
        inv.setItem(49, close);

        return inv;
    }

    private static ItemStack pane() {
        ItemStack p = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta  m = p.getItemMeta();
        m.setDisplayName("§r");
        p.setItemMeta(m);
        return p;
    }

    // ══════════════════════════════════════════════
    //  LISTENER
    // ══════════════════════════════════════════════

    public static class Listener implements org.bukkit.event.Listener {

        private final SkyBlockPlugin plugin = SkyBlockPlugin.getInstance();

        // ─── Clic droit avec le sac → ouvre le GUI ───
        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onUse(PlayerInteractEvent event) {
            if (event.getAction() != Action.RIGHT_CLICK_AIR
                    && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            if (event.getHand() != EquipmentSlot.HAND) return;

            Player    p    = event.getPlayer();
            ItemStack hand = p.getInventory().getItemInMainHand();
            UUID bagId = getBagId(hand);
            if (bagId == null) return;

            event.setCancelled(true);
            event.setUseItemInHand(Event.Result.DENY);
            Bukkit.getScheduler().runTask(plugin, () -> p.openInventory(buildGui(bagId)));
        }

        // ─── Fermeture → sauvegarder les slots 0-44 ───
        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            if (!(event.getPlayer() instanceof Player p)) return;
            if (!event.getView().getTitle().equals(GUI_TITLE)) return;

            UUID bagId = findBagId(p);
            if (bagId == null) return;

            List<ItemStack> slots = new ArrayList<>();
            for (int i = 0; i < CONTENT; i++) {
                slots.add(event.getInventory().getItem(i));
            }
            Storage.saveSlots(bagId, slots);
        }

        // ─── Clics dans le GUI ───
        // Priorité HIGHEST pour passer après GuiProtectionListener (LOWEST)
        // ignoreCancelled=false pour recevoir l'event même si quelqu'un d'autre l'a annulé
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onClick(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player p)) return;
            if (!event.getView().getTitle().equals(GUI_TITLE)) return;

            int slot = event.getRawSlot();

            // Fermer
            if (slot == 49) {
                event.setCancelled(true);
                p.closeInventory();
                return;
            }

            // Bordure protégée
            if (slot >= 45 && slot < 54) {
                event.setCancelled(true);
                return;
            }

            // Slots 0-44 : AUTORISER le dépôt et le retrait librement
            // On s'assure que l'event N'EST PAS annulé pour ces slots
            if (slot >= 0 && slot < CONTENT) {
                event.setCancelled(false); // annuler le cancel de GuiProtectionListener
                // La vérification du nombre de types max se fait à la fermeture
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onDrag(InventoryDragEvent event) {
            if (!event.getView().getTitle().equals(GUI_TITLE)) return;
            // Autoriser le drag uniquement dans les slots 0-44
            boolean touchesBordure = event.getRawSlots().stream().anyMatch(s -> s >= 45 && s < 54);
            if (touchesBordure) {
                event.setCancelled(true);
            } else {
                event.setCancelled(false); // autoriser le drag dans les slots libres
            }
        }

        // ─── Auto-pickup ───
        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onPickup(EntityPickupItemEvent event) {
            if (!(event.getEntity() instanceof Player p)) return;
            ItemStack dropping = event.getItem().getItemStack();
            if (dropping == null || dropping.getType().isAir()) return;

            // Ne pas absorber le sac lui-même
            if (isLootBag(dropping)) return;

            Material mat = dropping.getType();

            for (ItemStack inv : p.getInventory().getContents()) {
                UUID bagId = getBagId(inv);
                if (bagId == null) continue;

                // Vérifier si ce matériau est dans les filtres (slots 0-44 sauvegardés)
                if (!isFiltered(bagId, mat)) continue;

                // Stocker dans le sac
                Storage.addToStock(bagId, mat, dropping.getAmount());
                event.setCancelled(true);

                long total = Storage.getStockCount(bagId, mat);
                p.sendActionBar("§6+" + dropping.getAmount() + " §f" + fmtMat(mat)
                        + " §8(§6" + fmtNum(total) + "§8) §7→ §6Sac de Butin");
                return;
            }
            // Pas de sac ou item non filtré → ramassage normal, rien à faire
        }

        // ─── Helpers ───

        private UUID findBagId(Player p) {
            for (ItemStack item : p.getInventory().getContents()) {
                UUID id = getBagId(item);
                if (id != null) return id;
            }
            return null;
        }

        private boolean isFiltered(UUID bagId, Material mat) {
            List<ItemStack> slots = Storage.loadSlots(bagId);
            for (ItemStack item : slots) {
                if (item != null && !item.getType().isAir() && item.getType() == mat) return true;
            }
            return false;
        }
    }

    // ══════════════════════════════════════════════
    //  STOCKAGE YAML
    //  plugins/CoreSkyblock/lootbags/<uuid>.yml
    //  "slots.*"  : les items posés dans le GUI (filtres)
    //  "stock.*"  : les quantités cumulées (illimité)
    // ══════════════════════════════════════════════

    public static class Storage {

        private static File getDir() {
            File dir = new File(SkyBlockPlugin.getInstance().getDataFolder(), "lootbags");
            if (!dir.exists()) dir.mkdirs();
            return dir;
        }

        private static YamlConfiguration load(UUID bagId) {
            File f = new File(getDir(), bagId + ".yml");
            return f.exists() ? YamlConfiguration.loadConfiguration(f) : new YamlConfiguration();
        }

        private static void save(UUID bagId, YamlConfiguration cfg) {
            try { cfg.save(new File(getDir(), bagId + ".yml")); }
            catch (IOException e) { e.printStackTrace(); }
        }

        public static void saveSlots(UUID bagId, List<ItemStack> slots) {
            YamlConfiguration cfg = load(bagId);
            cfg.set("slots", null);
            for (int i = 0; i < slots.size(); i++) {
                ItemStack item = slots.get(i);
                if (item != null && !item.getType().isAir()) {
                    cfg.set("slots." + i, item);
                }
            }
            save(bagId, cfg);
        }

        public static List<ItemStack> loadSlots(UUID bagId) {
            List<ItemStack> result = new ArrayList<>(Collections.nCopies(CONTENT, null));
            YamlConfiguration cfg = load(bagId);
            if (!cfg.isConfigurationSection("slots")) return result;
            for (String key : cfg.getConfigurationSection("slots").getKeys(false)) {
                try {
                    int idx = Integer.parseInt(key);
                    if (idx >= 0 && idx < CONTENT)
                        result.set(idx, cfg.getItemStack("slots." + key));
                } catch (Exception ignored) {}
            }
            return result;
        }

        public static void addToStock(UUID bagId, Material mat, long amount) {
            YamlConfiguration cfg = load(bagId);
            long cur = cfg.getLong("stock." + mat.name(), 0);
            cfg.set("stock." + mat.name(), cur + amount);
            save(bagId, cfg);
        }

        public static long getStockCount(UUID bagId, Material mat) {
            return load(bagId).getLong("stock." + mat.name(), 0);
        }

        public static Map<Material, Long> getStock(UUID bagId) {
            Map<Material, Long> result = new LinkedHashMap<>();
            YamlConfiguration cfg = load(bagId);
            if (!cfg.isConfigurationSection("stock")) return result;
            for (String key : cfg.getConfigurationSection("stock").getKeys(false)) {
                try { result.put(Material.valueOf(key), cfg.getLong("stock." + key)); }
                catch (Exception ignored) {}
            }
            return result;
        }

        public static void removeFromStock(UUID bagId, Material mat, long amount) {
            YamlConfiguration cfg = load(bagId);
            long cur = cfg.getLong("stock." + mat.name(), 0);
            long nv  = Math.max(0, cur - amount);
            if (nv == 0) cfg.set("stock." + mat.name(), null);
            else cfg.set("stock." + mat.name(), nv);
            save(bagId, cfg);
        }
    }

    // ── Helpers affichage ──
    private static String fmtMat(Material m) {
        return m.name().replace("_", " ").toLowerCase();
    }
    private static String fmtNum(long n) {
        if (n >= 1_000_000) return (n / 1_000_000) + "M";
        if (n >= 1_000)     return (n / 1_000) + "K";
        return String.valueOf(n);
    }
}