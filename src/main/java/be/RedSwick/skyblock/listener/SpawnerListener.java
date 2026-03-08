package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.island.IslandUpgrade;
import be.RedSwick.skyblock.island.Island;
import be.RedSwick.skyblock.manager.IslandManager;
import be.RedSwick.skyblock.manager.SpawnerManager;
import be.RedSwick.skyblock.spawner.GlobalSpawnerEngine;
import be.RedSwick.skyblock.util.ChatUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class SpawnerListener implements Listener {

    // ── Stack count sur le MOB (NBT, survit aux restarts) ──────────────────
    public static final NamespacedKey KEY_STACK =
            new NamespacedKey(SkyBlockPlugin.getInstance(), "stack_count");

    private final SpawnerManager manager;
    private final IslandManager  islandManager = SkyBlockPlugin.getInstance().getIslandManager();

    public SpawnerListener() {
        this.manager = SkyBlockPlugin.getInstance().getSpawnerManager();
    }

    public SpawnerListener(SpawnerManager manager) {
        this.manager = manager;
    }

    public void startAllCycles() {
        new be.RedSwick.skyblock.spawner.GlobalSpawnerEngine(manager);
    }

    // ════════════════════════════════════════════════
    //  STACK COUNT API — NBT sur le mob
    // ════════════════════════════════════════════════

    public static void setStackCount(Entity entity, int amount) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (amount <= 1) pdc.remove(KEY_STACK);
        else             pdc.set(KEY_STACK, PersistentDataType.INTEGER, amount);
    }

    public static int getStackCount(Entity entity) {
        Integer val = entity.getPersistentDataContainer().get(KEY_STACK, PersistentDataType.INTEGER);
        return val != null ? val : 1;
    }

    public static void removeStackEntry(UUID uuid) {
        Entity e = Bukkit.getEntity(uuid);
        if (e != null) e.getPersistentDataContainer().remove(KEY_STACK);
    }

    // ════════════════════════════════════════════════
    //  HOLOGRAMME MOB
    // ════════════════════════════════════════════════

    public static void updateMobHologram(LivingEntity mob, EntityType type, int amount) {
        mob.setCustomName("§7" + formatType(type) + " §fx" + amount);
        mob.setCustomNameVisible(amount > 1);
    }

    // ════════════════════════════════════════════════
    //  ITEM SPAWNER
    // ════════════════════════════════════════════════

    public static ItemStack makeSpawnerItem(EntityType type, int amount) {
        ItemStack item = new ItemStack(Material.SPAWNER, amount);
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (meta == null) return item;
        CreatureSpawner cs = (CreatureSpawner) meta.getBlockState();
        cs.setSpawnedType(type);
        meta.setBlockState(cs);
        meta.setDisplayName("§e" + formatType(type) + " Spawner");
        item.setItemMeta(meta);
        return item;
    }

    public static String formatType(EntityType type) {
        String name = type.name().toLowerCase().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String w : name.split(" "))
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        return sb.toString().trim();
    }

    // ════════════════════════════════════════════════
    //  HOLOGRAMME SPAWNER (sur le bloc)
    // ════════════════════════════════════════════════

    private void updateSpawnerHologram(Location loc, EntityType type, int count) {
        String text = "§e" + formatType(type) + " Spawner §7x§6" + count;
        SkyBlockPlugin.getInstance().getHologramManager().setHologram(loc, text);
    }

    private void removeSpawnerHologram(Location loc) {
        SkyBlockPlugin.getInstance().getHologramManager().removeHologram(loc);
    }

    // ════════════════════════════════════════════════
    //  SPAWN EGG — mise à jour du type dans le manager
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawnEggUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.SPAWNER) return;

        ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
        if (hand == null || !hand.getType().name().endsWith("_SPAWN_EGG")) return;

        String eggName = hand.getType().name().replace("_SPAWN_EGG", "");
        EntityType newType;
        try { newType = EntityType.valueOf(eggName); }
        catch (IllegalArgumentException e) { return; }

        Location loc = clicked.getLocation();
        String key = SpawnerManager.toKey(loc);
        SpawnerManager.SpawnerData existing = manager.getAllSpawners().get(key);
        int count = existing != null ? existing.count() : 1;

        final EntityType finalType = newType;
        Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(), () -> {
            manager.addSpawner(loc, finalType, count);
            if (count > 1) updateSpawnerHologram(loc, finalType, count);
        });
    }

    // ════════════════════════════════════════════════
    //  CLIC DROIT — empiler des spawners sur le bloc
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.SPAWNER) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != Material.SPAWNER) return;

        EntityType handType   = getSpawnerType(hand);
        EntityType placedType = getPlacedType(clicked.getLocation());
        if (handType == null || placedType == null) return;
        if (handType != placedType) return;

        event.setCancelled(true);

        String key = SpawnerManager.toKey(clicked.getLocation());
        SpawnerManager.SpawnerData data = manager.getAllSpawners().get(key);
        if (data == null) return;

        int toAdd = player.isSneaking() ? hand.getAmount() : 1;

        Island island = islandManager.getIslandAtLocation(clicked.getLocation());
        if (island != null) {
            int limit     = island.getUpgradeValue(IslandUpgrade.SPAWNER_LIMIT);
            int current   = countSpawnersOnIsland(island);
            int available = limit - current;
            if (available <= 0) {
                player.sendMessage("§c✦ Limite de spawners atteinte ! §7(" + current + "§c/§7" + limit + "§7)");
                player.sendMessage("§7Améliore §e/is upgrade §7pour augmenter la limite.");
                return;
            }
            if (toAdd > available) {
                toAdd = available;
                player.sendMessage("§e⚠ Seulement §6" + toAdd + " spawner(s) §eajouté(s) — limite atteinte !");
            }
        }

        int newCount = data.count() + toAdd;
        manager.addSpawner(clicked.getLocation(), placedType, newCount);
        updateSpawnerHologram(clicked.getLocation(), placedType, newCount);

        if (toAdd >= hand.getAmount()) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            hand.setAmount(hand.getAmount() - toAdd);
        }

        ChatUtil.actionBar(player, "§e" + formatType(placedType) + " Spawner §7x§6" + newCount
                + "  §7(§b+" + toAdd + " ajouté" + (toAdd > 1 ? "s" : "") + "§7)");
        player.updateInventory();
    }

    // ════════════════════════════════════════════════
    //  POSE — premier spawner → count = 1
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.SPAWNER) return;

        Player   player = event.getPlayer();
        Location loc    = event.getBlockPlaced().getLocation();

        Island island = islandManager.getIslandAtLocation(loc);
        if (island != null) {
            int limit   = island.getUpgradeValue(IslandUpgrade.SPAWNER_LIMIT);
            int current = countSpawnersOnIsland(island);
            if (current >= limit) {
                event.setCancelled(true);
                player.sendMessage("§c✦ Limite de spawners atteinte ! §7("
                        + current + "§c/§7" + limit + "§7)");
                player.sendMessage("§7Améliore §e/is upgrade §7pour augmenter la limite.");
                return;
            }
        }

        ItemStack item = event.getItemInHand();
        EntityType type = getSpawnerType(item);
        if (type == null) type = EntityType.PIG;

        final EntityType finalType = type;
        Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(), () -> {
            if (loc.getBlock().getState() instanceof CreatureSpawner cs) {
                cs.setSpawnedType(finalType);
                cs.update();
            }
        });

        manager.addSpawner(loc, type, 1);
    }

    private int countSpawnersOnIsland(Island island) {
        Location center = island.getCenter();
        int radius      = island.getRadius();
        int total       = 0;
        for (Map.Entry<String, SpawnerManager.SpawnerData> entry : manager.getAllSpawners().entrySet()) {
            Location l = SpawnerManager.fromKey(entry.getKey());
            if (l == null || !l.getWorld().equals(center.getWorld())) continue;
            if (Math.abs(l.getBlockX() - center.getBlockX()) <= radius
                    && Math.abs(l.getBlockZ() - center.getBlockZ()) <= radius) {
                total += entry.getValue().count();
            }
        }
        return total;
    }

    // ════════════════════════════════════════════════
    //  CASSE — récupère 1 spawner (shift = tout)
    // ════════════════════════════════════════════════

    private final Map<String, EntityType> breakTypeCache = new HashMap<>();

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreakPre(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER) return;
        Location loc = event.getBlock().getLocation();
        String cacheKey = SpawnerManager.toKey(loc);
        SpawnerManager.SpawnerData d = manager.getAllSpawners().get(cacheKey);
        if (d != null) { breakTypeCache.put(cacheKey, d.type()); return; }
        if (loc.getBlock().getState() instanceof CreatureSpawner cs && cs.getSpawnedType() != null)
            breakTypeCache.put(cacheKey, cs.getSpawnedType());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER) return;

        Location loc      = event.getBlock().getLocation();
        Player   player   = event.getPlayer();
        String   cacheKey = SpawnerManager.toKey(loc);

        event.setDropItems(false);
        event.setExpToDrop(0);

        SpawnerManager.SpawnerData data = manager.getAllSpawners().get(cacheKey);

        EntityType type = breakTypeCache.remove(cacheKey);
        if (type == null && data != null) type = data.type();
        if (type == null) {
            player.sendMessage("§cErreur : type de spawner non détecté.");
            return;
        }

        int stored   = data != null ? data.count() : 1;
        int toRemove = player.isSneaking() ? stored : 1;
        int newCount = stored - toRemove;

        ItemStack give = makeSpawnerItem(type, toRemove);
        var leftover = player.getInventory().addItem(give);
        leftover.values().forEach(rest ->
                player.getWorld().dropItemNaturally(player.getLocation(), rest));

        if (newCount <= 0) {
            manager.removeSpawner(loc);
            removeSpawnerHologram(loc);
            ChatUtil.actionBar(player, "§e" + formatType(type) + " Spawner §arécupéré !");
        } else {
            event.setCancelled(true);
            manager.addSpawner(loc, type, newCount);
            updateSpawnerHologram(loc, type, newCount);
            ChatUtil.actionBar(player, "§e" + formatType(type) + " Spawner §7x§6" + newCount
                    + "  §7(§c-" + toRemove + " retiré" + (toRemove > 1 ? "s" : "") + "§7)");
        }

        player.updateInventory();
    }

    // ════════════════════════════════════════════════
    //  SPAWN EVENT
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        String key = SpawnerManager.toKey(event.getSpawner().getLocation());
        SpawnerManager.SpawnerData data = manager.getAllSpawners().get(key);
        if (data == null) return;
        if (!(event.getEntity() instanceof Mob firstMob)) return;

        // Marquer persistent IMMÉDIATEMENT (même tick que le spawn)
        firstMob.setPersistent(true);
        firstMob.setRemoveWhenFarAway(false);

        int count = data.count();

        if (count <= 1) {
            Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(), () -> {
                if (firstMob.isDead() || !firstMob.isValid()) return;
                // FIX : findMergeTarget ne cherche QUE des mobs persistent (issus d'un spawner)
                LivingEntity target = findMergeTarget(firstMob.getLocation(), firstMob.getType(), firstMob);
                if (target != null) {
                    setStackCount(target, getStackCount(target) + 1);
                    updateMobHologram(target, target.getType(), getStackCount(target));
                    if (target instanceof Mob m) GlobalSpawnerEngine.applyStackAttributes(m);
                    firstMob.remove();
                } else {
                    GlobalSpawnerEngine.applyStackAttributes(firstMob);
                    MobStackListener.addStacked(firstMob.getUniqueId());
                }
            });
            return;
        }

        Location spawnLoc = event.getSpawner().getLocation().clone().add(0.5, 0.5, 0.5);
        World world       = spawnLoc.getWorld();

        Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(), () -> {
            if (!firstMob.isDead() && firstMob.isValid()) {
                GlobalSpawnerEngine.applyStackAttributes(firstMob);
                MobStackListener.addStacked(firstMob.getUniqueId());
            }

            LivingEntity master = firstMob.isValid() ? firstMob : null;

            for (int i = 1; i < count; i++) {
                if (master != null) {
                    setStackCount(master, getStackCount(master) + 1);
                } else {
                    Mob extra = (Mob) world.spawnEntity(spawnLoc, data.type());
                    GlobalSpawnerEngine.applyStackAttributes(extra);
                    MobStackListener.addStacked(extra.getUniqueId());
                    master = extra;
                }
            }

            if (master != null && master.isValid()) {
                int total = getStackCount(master);
                updateMobHologram(master, master.getType(), total);
                if (master instanceof Mob m) GlobalSpawnerEngine.applyStackAttributes(m);
            }
        });
    }

    // ════════════════════════════════════════════════
    //  UTILITAIRES
    // ════════════════════════════════════════════════

    /**
     * Cherche un mob du même type à merger dans un rayon de 5 blocs.
     *
     * FIX CRITIQUE : vérification isPersistent() ajoutée.
     * Sans ça, un mob naturel (zombie/skeleton) qui traîne encore peut être
     * absorbé par un stack de spawner → le stack hérite d'un mob non-persistent
     * qui disparaît au déchargement du chunk, corrompant le stack.
     *
     * On ne merge QUE des mobs issus d'un spawner (persistent = true).
     */
    private LivingEntity findMergeTarget(Location center, EntityType type, Entity exclude) {
        for (Entity e : center.getWorld().getNearbyEntities(center, 5, 5, 5)) {
            if (e.getUniqueId().equals(exclude.getUniqueId())) continue;
            if (e.getType() != type) continue;
            if (!(e instanceof LivingEntity le)) continue;
            if (le.isDead()) continue;
            // FIX : ne merger que des mobs persistent (= issus d'un spawner)
            if (!le.isPersistent()) continue;
            return le;
        }
        return null;
    }

    private EntityType getSpawnerType(ItemStack item) {
        if (item == null || item.getType() != Material.SPAWNER) return null;
        if (!(item.getItemMeta() instanceof BlockStateMeta meta)) return null;
        if (!(meta.getBlockState() instanceof CreatureSpawner cs)) return null;
        return cs.getSpawnedType();
    }

    private EntityType getPlacedType(Location loc) {
        SpawnerManager.SpawnerData data = manager.getAllSpawners().get(SpawnerManager.toKey(loc));
        if (data != null) return data.type();
        if (loc.getBlock().getState() instanceof CreatureSpawner cs && cs.getSpawnedType() != null)
            return cs.getSpawnedType();
        return null;
    }
}