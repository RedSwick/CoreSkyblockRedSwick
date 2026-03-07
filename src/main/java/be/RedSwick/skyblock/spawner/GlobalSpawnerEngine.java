package be.RedSwick.skyblock.spawner;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.listener.MobStackListener;
import be.RedSwick.skyblock.listener.SpawnerListener;
import be.RedSwick.skyblock.manager.SpawnerManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Freeze via réflexion — vide goalSelector/targetSelector NMS
 * sans jamais poser NoAI → gravité conservée.
 *
 * La réflexion est initialisée LAZY au premier mob rencontré
 * (pas au static{} — à ce moment le classloader Paper n'est pas prêt).
 */
public class GlobalSpawnerEngine {

    private static final int INTERVAL     = 100; // 5s
    private static final int MERGE_RADIUS = 5;

    // ── Réflexion (init lazy au 1er mob) ────────────────────────────────
    private static volatile boolean reflectionInit   = false;
    private static volatile boolean reflectionReady  = false;

    private static Method getHandle           = null;
    private static Field  goalSelectorField   = null;
    private static Field  targetSelectorField = null;
    private static Method removeAllGoals      = null;

    /** Appelé sur le premier mob vivant — classloader Paper pleinement chargé */
    private static synchronized void initReflection(Mob mob) {
        if (reflectionInit) return;
        reflectionInit = true;
        try {
            // Partir de l'instance réelle (ex: CraftZombie) → remonter jusqu'à CraftMob
            Class<?> craftMobClass = mob.getClass();
            while (craftMobClass != null) {
                try {
                    getHandle = craftMobClass.getMethod("getHandle");
                    break;
                } catch (NoSuchMethodException ignored) {
                    craftMobClass = craftMobClass.getSuperclass();
                }
            }

            if (getHandle == null) {
                Bukkit.getLogger().warning("[GlobalSpawnerEngine] getHandle() introuvable — fallback setAware.");
                return;
            }

            // Récupérer le NMS mob pour avoir la vraie classe runtime
            Object nmsMob = getHandle.invoke(mob);
            Class<?> nmsMobClass = nmsMob.getClass();

            goalSelectorField   = findField(nmsMobClass, "goalSelector");
            targetSelectorField = findField(nmsMobClass, "targetSelector");

            if (goalSelectorField == null || targetSelectorField == null) {
                Bukkit.getLogger().warning("[GlobalSpawnerEngine] goalSelector introuvable — fallback setAware.");
                return;
            }

            // GoalSelector#removeAllGoals(Predicate)
            Object goalSel = goalSelectorField.get(nmsMob);
            try {
                removeAllGoals = goalSel.getClass()
                        .getMethod("removeAllGoals", java.util.function.Predicate.class);
            } catch (NoSuchMethodException ignored) {
                // pas de removeAllGoals → on videra le Set interne directement
            }

            reflectionReady = true;
            Bukkit.getLogger().info("[GlobalSpawnerEngine] Réflexion NMS OK — mob freeze sans NoAI.");

        } catch (Exception e) {
            Bukkit.getLogger().warning("[GlobalSpawnerEngine] Réflexion échouée : "
                    + e.getClass().getSimpleName() + ": " + e.getMessage() + " — fallback setAware.");
        }
    }

    /** Cherche un Field par nom dans la hiérarchie de classes */
    private static Field findField(Class<?> clazz, String name) {
        Class<?> c = clazz;
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    // ────────────────────────────────────────────────────────────────────

    public GlobalSpawnerEngine(SpawnerManager manager) {
        Bukkit.getScheduler().runTaskTimer(
                SkyBlockPlugin.getInstance(), this::tick, INTERVAL, INTERVAL);
    }

    private void tick() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                tickChunk(chunk);
            }
        }
    }

    private void tickChunk(Chunk chunk) {
        Map<EntityType, List<Mob>> byType = new HashMap<>();
        for (Entity e : chunk.getEntities()) {
            if (!(e instanceof Mob mob)) continue;
            if (mob.isDead() || !mob.isValid()) continue;
            if (!mob.isPersistent()) continue;
            byType.computeIfAbsent(mob.getType(), t -> new ArrayList<>()).add(mob);
        }

        for (List<Mob> mobs : byType.values()) {
            Set<UUID> merged = new HashSet<>();
            for (Mob master : mobs) {
                if (merged.contains(master.getUniqueId())) continue;
                int total = SpawnerListener.getStackCount(master);

                for (Mob other : mobs) {
                    if (other == master) continue;
                    if (merged.contains(other.getUniqueId())) continue;
                    if (master.getLocation().distance(other.getLocation()) > MERGE_RADIUS) continue;

                    total += SpawnerListener.getStackCount(other);
                    merged.add(other.getUniqueId());
                    MobStackListener.removeStacked(other.getUniqueId());
                    SpawnerListener.removeStackEntry(other.getUniqueId());
                    other.remove();
                }

                SpawnerListener.setStackCount(master, total);
                SpawnerListener.updateMobHologram(master, master.getType(), total);
                applyStackAttributes(master);
                MobStackListener.addStacked(master.getUniqueId());
                merged.add(master.getUniqueId());
            }
        }
    }

    // ══════════════════════════════════════════════════════
    //  applyStackAttributes
    // ══════════════════════════════════════════════════════

    public static void applyStackAttributes(Mob mob) {
        // Init lazy — au premier vrai mob, pas au static{}
        if (!reflectionInit) initReflection(mob);

        if (reflectionReady) {
            clearGoalsViaReflection(mob);
        } else {
            mob.setAware(false);
        }

        mob.setGravity(true);
        mob.setSilent(true);
        mob.setCanPickupItems(false);
        mob.setRemoveWhenFarAway(false);
        mob.setPersistent(true);
        mob.setTarget(null);

        var kb = mob.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (kb != null) kb.setBaseValue(1.0);

        var follow = mob.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);
        if (follow != null) follow.setBaseValue(0.0);
    }

    private static void clearGoalsViaReflection(Mob mob) {
        try {
            Object nmsMob = getHandle.invoke(mob);
            clearSelector(goalSelectorField.get(nmsMob));
            clearSelector(targetSelectorField.get(nmsMob));
        } catch (Exception ignored) {}
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void clearSelector(Object selector) throws Exception {
        if (removeAllGoals != null) {
            removeAllGoals.invoke(selector, (java.util.function.Predicate) g -> true);
            return;
        }
        // Fallback : vider le premier Set trouvé dans GoalSelector
        for (Field f : selector.getClass().getDeclaredFields()) {
            if (Set.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                ((Set<?>) f.get(selector)).clear();
                return;
            }
        }
    }

    public static void freezeMob(Mob mob) { applyStackAttributes(mob); }
}