package be.RedSwick.skyblock.spawner;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.island.Island;
import be.RedSwick.skyblock.island.IslandUpgrade;
import be.RedSwick.skyblock.listener.MobStackListener;
import be.RedSwick.skyblock.listener.SpawnerListener;
import be.RedSwick.skyblock.manager.IslandManager;
import be.RedSwick.skyblock.manager.SpawnerManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GlobalSpawnerEngine — Version ultra-optimisée
 *
 * 1. Ne tick QUE les chunks avec des spawners enregistrés
 * 2. distanceSquared() — pas de sqrt()
 * 3. frozenState — applyStackAttributes UNE SEULE FOIS par mob
 * 4. Cleanup toutes les 5 min :
 *    - Supprime les mobs non-persistent orphelins
 *    - Supprime les mobs hors île
 *    - Respecte la limite SPAWNER_LIMIT de l'upgrade d'île
 */
public class GlobalSpawnerEngine {

    private static final int INTERVAL        = 60;     // 3s
    private static final int MERGE_RADIUS_SQ = 8 * 8;
    private static final int CLEANUP_INTERVAL = 6000;  // 5 min

    private final Map<UUID, Integer> frozenState   = new ConcurrentHashMap<>();
    private final SpawnerManager     spawnerManager;
    private final IslandManager      islandManager;

    public GlobalSpawnerEngine(SpawnerManager spawnerManager) {
        this.spawnerManager = spawnerManager;
        this.islandManager  = SkyBlockPlugin.getInstance().getIslandManager();

        Bukkit.getScheduler().runTaskTimer(
                SkyBlockPlugin.getInstance(), this::tick, INTERVAL, INTERVAL);

        Bukkit.getScheduler().runTaskTimer(
                SkyBlockPlugin.getInstance(), this::cleanup, CLEANUP_INTERVAL, CLEANUP_INTERVAL);
    }

    // ════════════════════════════════════════════════
    //  TICK PRINCIPAL
    // ════════════════════════════════════════════════

    private void tick() {
        World world = Bukkit.getWorld("skyblock");
        if (world == null) return;

        Set<String> spawnerKeys = spawnerManager.getAllSpawners().keySet();
        Set<Long> chunksToTick  = new HashSet<>(spawnerKeys.size());

        for (String key : spawnerKeys) {
            Location loc = SpawnerManager.fromKey(key);
            if (loc == null || !loc.getWorld().equals(world)) continue;
            int cx = loc.getBlockX() >> 4;
            int cz = loc.getBlockZ() >> 4;
            if (world.isChunkLoaded(cx, cz))
                chunksToTick.add(chunkKey(cx, cz));
        }

        for (long ck : chunksToTick) {
            int cx = (int)(ck >> 32);
            int cz = (int)(ck & 0xFFFFFFFFL);
            if (world.isChunkLoaded(cx, cz))
                tickChunk(world.getChunkAt(cx, cz));
        }

        // Nettoyage léger du frozenState — mobs morts
        frozenState.entrySet().removeIf(e -> {
            Entity ent = Bukkit.getEntity(e.getKey());
            return ent == null || ent.isDead() || !ent.isValid();
        });
    }

    // ════════════════════════════════════════════════
    //  TICK PAR CHUNK
    // ════════════════════════════════════════════════

    private void tickChunk(Chunk chunk) {
        Map<EntityType, List<Mob>> byType = null;

        for (Entity e : chunk.getEntities()) {
            if (!(e instanceof Mob mob)) continue;
            if (mob.isDead() || !mob.isValid()) continue;
            if (!mob.isPersistent()) continue;
            if (byType == null) byType = new HashMap<>();
            byType.computeIfAbsent(mob.getType(), t -> new ArrayList<>()).add(mob);
        }

        if (byType == null) return;

        for (List<Mob> mobs : byType.values()) {
            if (mobs.size() == 1) { ensureFrozen(mobs.get(0)); continue; }
            mergeAndFreeze(mobs);
        }
    }

    // ════════════════════════════════════════════════
    //  MERGE EN O(n)
    // ════════════════════════════════════════════════

    private void mergeAndFreeze(List<Mob> mobs) {
        mobs.sort((a, b) -> Integer.compare(
                SpawnerListener.getStackCount(b),
                SpawnerListener.getStackCount(a)));

        Set<UUID> absorbed = new HashSet<>();

        for (int i = 0; i < mobs.size(); i++) {
            Mob master = mobs.get(i);
            if (absorbed.contains(master.getUniqueId())) continue;

            int total = SpawnerListener.getStackCount(master);

            for (int j = i + 1; j < mobs.size(); j++) {
                Mob other = mobs.get(j);
                if (absorbed.contains(other.getUniqueId())) continue;
                if (master.getLocation().distanceSquared(other.getLocation()) > MERGE_RADIUS_SQ) continue;

                total += SpawnerListener.getStackCount(other);
                absorbed.add(other.getUniqueId());
                MobStackListener.removeStacked(other.getUniqueId());
                SpawnerListener.removeStackEntry(other.getUniqueId());
                frozenState.remove(other.getUniqueId());
                other.remove();
            }

            int prevStack = SpawnerListener.getStackCount(master);
            if (total != prevStack) {
                SpawnerListener.setStackCount(master, total);
                SpawnerListener.updateMobHologram(master, master.getType(), total);
            }

            ensureFrozen(master);
            MobStackListener.addStacked(master.getUniqueId());
        }
    }

    // ════════════════════════════════════════════════
    //  FREEZE — appliqué seulement si état changé
    // ════════════════════════════════════════════════

    private void ensureFrozen(Mob mob) {
        int currentStack = SpawnerListener.getStackCount(mob);
        Integer lastKnown = frozenState.get(mob.getUniqueId());
        if (lastKnown != null && lastKnown == currentStack) return;
        applyStackAttributes(mob);
        frozenState.put(mob.getUniqueId(), currentStack);
        MobStackListener.addStacked(mob.getUniqueId());
    }

    // ════════════════════════════════════════════════
    //  CLEANUP — toutes les 5 min
    // ════════════════════════════════════════════════

    private void cleanup() {
        World world = Bukkit.getWorld("skyblock");
        if (world == null) return;

        // Grouper les mobs persistent par île
        Map<Island, List<Mob>> islandMobs = new HashMap<>();

        for (Entity e : world.getEntities()) {
            if (!(e instanceof Mob mob)) continue;
            if (mob.isDead() || !mob.isValid()) continue;

            // Mobs non-persistent = pas issus d'un spawner → retirer
            if (!mob.isPersistent()) {
                mob.remove();
                continue;
            }

            Island island = islandManager.getIslandAtLocation(mob.getLocation());
            if (island == null) {
                // Mob orphelin hors île → retirer
                cleanupMob(mob);
                continue;
            }

            islandMobs.computeIfAbsent(island, k -> new ArrayList<>()).add(mob);
        }

        // Appliquer la limite SPAWNER_LIMIT par île
        for (Map.Entry<Island, List<Mob>> entry : islandMobs.entrySet()) {
            Island island   = entry.getKey();
            List<Mob> mobs  = entry.getValue();

            // Limite = valeur de l'upgrade SPAWNER_LIMIT (50/75/100/150/200)
            int limit = island.getUpgradeValue(IslandUpgrade.SPAWNER_LIMIT);

            if (mobs.size() <= limit) continue;

            // Supprimer les stacks les plus petits en trop
            mobs.sort(Comparator.comparingInt(SpawnerListener::getStackCount));
            int toRemove = mobs.size() - limit;
            for (int i = 0; i < toRemove; i++) cleanupMob(mobs.get(i));

            SkyBlockPlugin.getInstance().getLogger().info(
                    "[Cleanup] Île " + island.getOwner() +
                            " : " + toRemove + " stack(s) retiré(s) (limite " + limit + ")");
        }
    }

    private void cleanupMob(Mob mob) {
        MobStackListener.removeStacked(mob.getUniqueId());
        SpawnerListener.removeStackEntry(mob.getUniqueId());
        frozenState.remove(mob.getUniqueId());
        mob.remove();
    }

    // ════════════════════════════════════════════════
    //  ATTRIBUTS DE STACK
    // ════════════════════════════════════════════════

    public static void applyStackAttributes(Mob mob) {
        mob.setGravity(true);
        mob.setSilent(true);
        mob.setCanPickupItems(false);
        mob.setRemoveWhenFarAway(false);
        mob.setPersistent(true);
        mob.setTarget(null);
        mob.setAware(false);

        AttributeInstance kb = mob.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (kb != null) kb.setBaseValue(1.0);

        AttributeInstance follow = mob.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);
        if (follow != null) follow.setBaseValue(0.0);
    }

    public static void freezeMob(Mob mob) { applyStackAttributes(mob); }

    public void onMobDeath(UUID uuid) { frozenState.remove(uuid); }

    private long chunkKey(int cx, int cz) {
        return (((long) cx) << 32) | (cz & 0xFFFFFFFFL);
    }
}