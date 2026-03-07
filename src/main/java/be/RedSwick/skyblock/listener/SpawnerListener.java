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

    // \u2500\u2500 Stack count sur le MOB (NBT, survit aux restarts) \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500
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

    // \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550
    //  STACK COUNT API \u2014 NBT sur le mob
    // \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550

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

    // \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550
    //  HOLOGRAMME MOB
    // \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550

    public static void updateMobHologram(LivingEntity mob, EntityType type, int amount) {
        mob.setCustomName("\u00a77" + formatType(type) + " \u00a7fx" + amount);
        mob.setCustomNameVisible(amount > 1);
    }

    // \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550
    //  ITEM SPAWNER
    // \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550

    public static ItemStack makeSpawnerItem(EntityType type, int amount) {
        ItemStack item = new ItemStack(Material.SPAWNER, amount);
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (meta == null) return item;
        CreatureSpawner cs = (CreatureSpawner) meta.getBlockState();
        cs.setSpawnedType(type);
        meta.setBlockState(cs);
        meta.setDisplayName("\u00a7e" + formatType(type) + " Spawner");
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

    // \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550
    //  HOLOGRAMME SPAWNER (sur le bloc)
    // \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550

    private void updateSpawnerHologram(Location loc, EntityType type, int count) {
        String text = "\u00a7e" + formatType(type) + " Spawner \u00a77x\u00a76" + count;
        SkyBlockPlugin.getInstance().getHologramManager().setHologram(loc, text);
    }

    private void removeSpawnerHologram(Location loc) {
        SkyBlockPlugin.getInstance().getHologramManager().removeHologram(loc);
    }

    // \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550
    //  CLIC DROIT \u2014 EMPILER des spawners sur le bloc
    //  (m\u00eame type en main + clic sur le spawner pos\u00e9)
    //  Clic simple  \u2192 +1
    //  Shift+clic   \u2192 +tout le stack en main
    // \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550

    @EventHandler(priority = EventPriority.HIGH)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.SPAWNER) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != Material.SPAWNER) return;

        // V\u00e9rifier que les types correspondent
        EntityType handType    = getSpawnerType(hand);
        EntityType placedType  = getPlacedType(clicked.getLocation());
        if (handType == null || placedType == null) return;
        if (handType != placedType) return;

        event.setCancelled(true);

        String key = SpawnerManager.toKey(clicked.getLocation());
        SpawnerManager.SpawnerData data = manager.getAllSpawners().get(key);
        if (data == null) return;

        int toAdd = player.isSneaking() ? hand.getAmount() : 1;

        // ── Vérifier la limite de spawners de l'île ──
        Island island = islandManager.getIslandAtLocation(clicked.getLocation());
        if (island != null) {
            int limit   = island.getUpgradeValue(IslandUpgrade.SPAWNER_LIMIT);
            int current = countSpawnersOnIsland(island);
            int available = limit - current;
            if (available <= 0) {
                player.sendMessage("§c✦ Limite de spawners atteinte ! §7(" + current + "§c/§7" + limit + "§7)");
                player.sendMessage("§7Améliore §e/is upgrade §7pour augmenter la limite.");
                return;
            }
            // Limiter toAdd à ce qu'il reste de disponible
            if (toAdd > available) {
                toAdd = available;
                player.sendMessage("§e⚠ Seulement §6" + toAdd + " spawner(s) §eajouté(s) — limite atteinte !");
            }
        }

        int newCount = data.count() + toAdd;

        manager.addSpawner(clicked.getLocation(), placedType, newCount);
        updateSpawnerHologram(clicked.getLocation(), placedType, newCount);

        // Retirer de l'inventaire
        if (toAdd >= hand.getAmount()) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            hand.setAmount(hand.getAmount() - toAdd);
        }

        ChatUtil.actionBar(player, "\u00a7e" + formatType(placedType) + " Spawner \u00a77x\u00a76" + newCount
                + "  \u00a77(\u00a7b+" + toAdd + " ajout\u00e9" + (toAdd > 1 ? "s" : "") + "\u00a77)");
        player.updateInventory();
    }

    // \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550
    //  POSE \u2014 premier spawner \u2192 count = 1
    // \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.SPAWNER) return;

        Player   player = event.getPlayer();
        Location loc    = event.getBlockPlaced().getLocation();

        // ── Vérifier la limite de spawners de l'île ──
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
        // Pas d'hologramme pour count=1 (vanilla, pas besoin)
    }

    // ── Compte le TOTAL de spawners (stackés compris) dans le rayon de l'île ──
    private int countSpawnersOnIsland(Island island) {
        Location center = island.getCenter();
        int radius      = island.getRadius();
        int total       = 0;
        for (Map.Entry<String, SpawnerManager.SpawnerData> entry : manager.getAllSpawners().entrySet()) {
            Location l = SpawnerManager.fromKey(entry.getKey());
            if (l == null || !l.getWorld().equals(center.getWorld())) continue;
            if (Math.abs(l.getBlockX() - center.getBlockX()) <= radius
                    && Math.abs(l.getBlockZ() - center.getBlockZ()) <= radius) {
                total += entry.getValue().count(); // ← count() pas juste +1
            }
        }
        return total;
    }

    // \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550
    //  CASSE \u2014 r\u00e9cup\u00e8re 1 spawner (shift = tout)
    //  Bloc physique reste tant que count > 0
    //  Dispara\u00eet \u00e0 count = 0
    // \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER) return;

        Location loc    = event.getBlock().getLocation();
        Player   player = event.getPlayer();

        event.setDropItems(false);
        event.setExpToDrop(0);

        EntityType type = EntityType.PIG;
        SpawnerManager.SpawnerData data = manager.getAllSpawners().get(SpawnerManager.toKey(loc));
        if (data != null) {
            type = data.type();
        } else if (loc.getBlock().getState() instanceof CreatureSpawner cs && cs.getSpawnedType() != null) {
            type = cs.getSpawnedType();
        }

        int stored   = data != null ? data.count() : 1;
        int toRemove = player.isSneaking() ? stored : 1;
        int newCount = stored - toRemove;

        // Donner les spawners r\u00e9cup\u00e9r\u00e9s
        ItemStack give = makeSpawnerItem(type, toRemove);
        var leftover = player.getInventory().addItem(give);
        leftover.values().forEach(rest ->
                player.getWorld().dropItemNaturally(player.getLocation(), rest));

        if (newCount <= 0) {
            // Tout r\u00e9cup\u00e9r\u00e9 \u2192 bloc dispara\u00eet normalement
            manager.removeSpawner(loc);
            removeSpawnerHologram(loc);
            ChatUtil.actionBar(player, "\u00a7e" + formatType(type) + " Spawner \u00a7arecup\u00e9r\u00e9 !");
        } else {
            // Il reste des spawners \u2192 bloc reste en place
            event.setCancelled(true);
            manager.addSpawner(loc, type, newCount);
            updateSpawnerHologram(loc, type, newCount);
            ChatUtil.actionBar(player, "\u00a7e" + formatType(type) + " Spawner \u00a77x\u00a76" + newCount
                    + "  \u00a77(\u00a7c-" + toRemove + " retir\u00e9" + (toRemove > 1 ? "s" : "") + "\u00a77)");
        }

        player.updateInventory();
    }

    // \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550
    //  SPAWN \u2014 x spawners vanilla
    //  Un spawner x6 spawn 6 mobs \u00e0 chaque cycle
    // \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        String key = SpawnerManager.toKey(event.getSpawner().getLocation());
        SpawnerManager.SpawnerData data = manager.getAllSpawners().get(key);
        if (data == null) return;
        if (!(event.getEntity() instanceof Mob firstMob)) return;

        int count = data.count(); // nombre de spawners stack\u00e9s

        // Le premier mob est d\u00e9j\u00e0 spawn\u00e9 par l'event vanilla
        // On spawn (count - 1) mobs suppl\u00e9mentaires au tick suivant
        if (count <= 1) {
            // Spawner simple \u2192 juste freeze + stack
            Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(), () -> {
                if (firstMob.isDead() || !firstMob.isValid()) return;
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

        // Spawner stack\u00e9 \u2192 spawn count-1 mobs suppl\u00e9mentaires
        Location spawnLoc = event.getSpawner().getLocation().clone().add(0.5, 0.5, 0.5);
        World world       = spawnLoc.getWorld();

        Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(), () -> {
            // Traiter le premier mob
            if (!firstMob.isDead() && firstMob.isValid()) {
                GlobalSpawnerEngine.applyStackAttributes(firstMob);
                MobStackListener.addStacked(firstMob.getUniqueId());
            }

            // Spawner les (count - 1) mobs suppl\u00e9mentaires et les merger sur le premier
            LivingEntity master = firstMob.isValid() ? firstMob : null;

            for (int i = 1; i < count; i++) {
                if (master != null) {
                    // Merger directement dans le master \u2192 pas de nouveau mob physique
                    setStackCount(master, getStackCount(master) + 1);
                } else {
                    // Si le 1er est mort, spawner physiquement un nouveau
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

    // \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550
    //  UTILITAIRES
    // \u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550

    private LivingEntity findMergeTarget(Location center, EntityType type, Entity exclude) {
        for (Entity e : center.getWorld().getNearbyEntities(center, 5, 5, 5)) {
            if (e.getUniqueId().equals(exclude.getUniqueId())) continue;
            if (e.getType() == type && e instanceof LivingEntity le && !le.isDead())
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