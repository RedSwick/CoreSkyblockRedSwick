package be.RedSwick.skyblock.listener;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * NaturalSpawnListener — Optimisé + Corrigé
 *
 * RÈGLE MÉTIER :
 * Dans le monde "skyblock", les SEULS mobs autorisés sont ceux issus d'un
 * spawner (SpawnReason.SPAWNER). Tout spawn naturel est bloqué.
 *
 * POURQUOI LE BUG EXISTAIT :
 * L'ancien code faisait setSpawnFlags(true, false) sur skyblock = monstres
 * autorisés au niveau monde → zombies/skeletons/creepers spawnaient la nuit.
 * La liste ALWAYS_BLOCKED ne contenait que des passifs/aquatiques.
 *
 * FIX : bloquer par SpawnReason dans skyblock.
 * SPAWNER est laissé passer → SpawnerListener et GlobalSpawnerEngine le gèrent.
 */
public class NaturalSpawnListener implements Listener {

    // Raisons de spawn naturelles à bloquer dans skyblock.
    // SPAWNER est absent intentionnellement — ces spawns doivent passer.
    private static final Set<CreatureSpawnEvent.SpawnReason> BLOCKED_REASONS = EnumSet.of(
            CreatureSpawnEvent.SpawnReason.NATURAL,
            CreatureSpawnEvent.SpawnReason.CHUNK_GEN,
            CreatureSpawnEvent.SpawnReason.LIGHTNING,
            CreatureSpawnEvent.SpawnReason.MOUNT,
            CreatureSpawnEvent.SpawnReason.PATROL,
            CreatureSpawnEvent.SpawnReason.RAID,
            CreatureSpawnEvent.SpawnReason.REINFORCEMENTS,
            CreatureSpawnEvent.SpawnReason.SLIME_SPLIT,
            CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION,
            CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE,
            CreatureSpawnEvent.SpawnReason.JOCKEY,
            CreatureSpawnEvent.SpawnReason.OCELOT_BABY,
            CreatureSpawnEvent.SpawnReason.DEFAULT
    );

    // Passifs/aquatiques/ambiants bloqués dans tous les mondes
    private static final Set<EntityType> ALWAYS_BLOCKED = EnumSet.of(
            EntityType.SQUID, EntityType.GLOW_SQUID, EntityType.COD,
            EntityType.SALMON, EntityType.TROPICAL_FISH, EntityType.PUFFERFISH,
            EntityType.DOLPHIN,
            EntityType.BAT, EntityType.PHANTOM,
            EntityType.RABBIT, EntityType.COW, EntityType.SHEEP, EntityType.PIG,
            EntityType.CHICKEN, EntityType.HORSE, EntityType.DONKEY, EntityType.MULE,
            EntityType.FOX, EntityType.WOLF, EntityType.BEE, EntityType.TURTLE,
            EntityType.FROG, EntityType.AXOLOTL, EntityType.GOAT, EntityType.CAMEL,
            EntityType.ARMADILLO, EntityType.POLAR_BEAR
    );

    public NaturalSpawnListener() {
        for (World w : Bukkit.getWorlds()) applyRules(w);

        // Reinforcer PEACEFUL sur "world" toutes les 5 min
        Bukkit.getScheduler().runTaskTimer(
                be.RedSwick.skyblock.SkyBlockPlugin.getInstance(),
                () -> {
                    World w = Bukkit.getWorld("world");
                    if (w != null && w.getDifficulty() != Difficulty.PEACEFUL)
                        w.setDifficulty(Difficulty.PEACEFUL);
                },
                6000L, 6000L
        );
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        applyRules(event.getWorld());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Entity entity    = event.getEntity();
        String worldName = entity.getWorld().getName();
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();

        // ── MONDE SKYBLOCK ──────────────────────────────────────────────────────
        if (worldName.equals("skyblock")) {
            // Bloquer tout spawn naturel
            if (BLOCKED_REASONS.contains(reason)) {
                event.setCancelled(true);
                return;
            }
            // Bloquer passifs/aquatiques même si raison = SPAWNER (cohérence)
            if (ALWAYS_BLOCKED.contains(event.getEntityType())) {
                event.setCancelled(true);
                return;
            }
            // SpawnReason.SPAWNER et CUSTOM passent → SpawnerListener les gère
            return;
        }

        // ── MONDE "world" ────────────────────────────────────────────────────────
        if (worldName.equals("world")) {
            // Aucun spawn naturel dans le monde spawn — CUSTOM seul autorisé (commandes admin)
            if (reason != CreatureSpawnEvent.SpawnReason.CUSTOM) {
                event.setCancelled(true);
            }
            return;
        }

        // ── AUTRES MONDES ────────────────────────────────────────────────────────
        if (ALWAYS_BLOCKED.contains(event.getEntityType())) {
            event.setCancelled(true);
        }
    }

    private static void applyRules(World world) {
        switch (world.getName()) {
            case "world" -> {
                world.setDifficulty(Difficulty.PEACEFUL);
                world.setSpawnFlags(false, false);
            }
            case "skyblock" -> {
                // NORMAL requis pour que les spawners fonctionnent
                // setSpawnFlags(false, false) = désactive spawn naturel au niveau gamerule
                world.setDifficulty(Difficulty.NORMAL);
                world.setSpawnFlags(false, false);
            }
            default -> world.setSpawnFlags(false, false);
        }
    }
}