package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.job.JobXpTable;
import be.RedSwick.skyblock.job.JobXpTable.JobAction;
import be.RedSwick.skyblock.spawner.GlobalSpawnerEngine;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class MobStackListener implements Listener {

    // stack count au moment de la mort — lu par JobListener avant que le PDC soit nettoyé
    public static final Map<UUID, Integer> handledKills = new HashMap<>();
    private static final Set<UUID> stacked = Collections.synchronizedSet(new HashSet<>());

    private static final Set<Material> BLACKLIST = Set.of(
            Material.BOW, Material.CROSSBOW, Material.POTION,
            Material.SPLASH_POTION, Material.LINGERING_POTION, Material.GLASS_BOTTLE
    );

    public MobStackListener() {
        // Ticker toutes les 20 ticks (1s) — setTarget null pour sécurité
        // setGravity n'est plus nécessaire car on ne pose plus NoAI (méthode NMS)
        new BukkitRunnable() {
            @Override public void run() {
                stacked.removeIf(id -> {
                    Entity e = Bukkit.getEntity(id);
                    if (!(e instanceof Mob mob) || !mob.isValid() || mob.isDead()) return true;
                    mob.setTarget(null);
                    return false;
                });
            }
        }.runTaskTimer(SkyBlockPlugin.getInstance(), 1L, 20L);
    }

    public static void addStacked(UUID uuid)    { stacked.add(uuid); }
    public static void removeStacked(UUID uuid) { stacked.remove(uuid); }

    // ════════════════════════════════════════════════
    //  DEATH — drops × stack, XP × stack
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        int stack = SpawnerListener.getStackCount(mob);

        stacked.remove(mob.getUniqueId());
        SpawnerListener.removeStackEntry(mob.getUniqueId());
        SkyBlockPlugin.getInstance().getHologramManager().removeMobHologram(mob.getUniqueId());

        event.getDrops().removeIf(item ->
                item == null || item.getType().isAir() || BLACKLIST.contains(item.getType()));

        if (stack <= 1) {
            handledKills.remove(mob.getUniqueId());
            return;
        }

        List<ItemStack> baseDrops = new ArrayList<>(event.getDrops());
        event.getDrops().clear();

        for (ItemStack drop : baseDrops) {
            int total = drop.getAmount() * stack;
            while (total > 0) {
                ItemStack clone = drop.clone();
                clone.setAmount(Math.min(64, total));
                event.getDrops().add(clone);
                total -= clone.getAmount();
            }
        }

        event.setDroppedExp(event.getDroppedExp() * stack);
        handledKills.put(mob.getUniqueId(), stack);
    }

    // ════════════════════════════════════════════════
    //  DÉGÂTS — déstack par overflow
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mob)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        int stack = SpawnerListener.getStackCount(mob);
        if (stack <= 1) return;

        double maxHp   = mob.getMaxHealth();
        if (maxHp <= 0) return;
        double damage  = event.getDamage();
        double current = mob.getHealth();
        if (damage < current) return;

        double overflow = damage - current;
        int extraKills  = (int) (overflow / maxHp);
        int kills       = Math.min(stack, 1 + extraKills);
        int newStack    = stack - kills;

        // ── Récompenser immédiatement les kills du déstack ──
        // On récompense (kills) mobs — sauf le dernier qui sera géré par EntityDeathEvent
        int jobKills = newStack <= 0 ? kills - 1 : kills;
        if (jobKills > 0) {
            rewardChasseur(player, mob.getType(), jobKills);
        }

        if (newStack <= 0) {
            // Laisser le dernier mob mourir normalement → EntityDeathEvent gère le reste
            SpawnerListener.setStackCount(mob, 1);
            return;
        }

        event.setCancelled(true);
        double remaining = overflow - (extraKills * maxHp);
        mob.setHealth(Math.max(1.0, maxHp - remaining));
        SpawnerListener.setStackCount(mob, newStack);
        SpawnerListener.updateMobHologram(mob, mob.getType(), newStack);

        if (mob instanceof Mob m) {
            Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                    () -> { if (!m.isDead()) GlobalSpawnerEngine.applyStackAttributes(m); });
        }
    }

    /** Récompense immédiate des kills de déstack (sans attendre la mort) */
    private void rewardChasseur(Player player, EntityType type, int kills) {
        JobAction action = JobXpTable.CHASSEUR_MOBS.get(type);
        if (action == null) return;
        double essence = JobXpTable.CHASSEUR_ESSENCE.getOrDefault(type, 0.0);
        if (essence > 0) {
            var data = SkyBlockPlugin.getInstance()
                    .getPlayerDataManager().get(player.getUniqueId());
            if (data != null) data.addEssence(Math.round(essence * kills));
        }
        SkyBlockPlugin.getInstance().getJobManager()
                .rewardActionChasseur(player, action.xp() * kills, action.coins() * kills, essence * kills);
    }
}