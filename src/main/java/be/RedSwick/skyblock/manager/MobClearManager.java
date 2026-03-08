package be.RedSwick.skyblock.manager;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.listener.MobStackListener;
import be.RedSwick.skyblock.listener.SpawnerListener;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

public class MobClearManager {

    private static final int CLEAR_INTERVAL_TICKS = 12_000; // 10 minutes
    private static final int WARNING_TICKS         = 600;   // 30 secondes avant

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                scheduleClear();
            }
        }.runTaskTimer(SkyBlockPlugin.getInstance(), CLEAR_INTERVAL_TICKS, CLEAR_INTERVAL_TICKS);
    }

    private void scheduleClear() {
        Bukkit.broadcastMessage("§c[ClearLag] §fLes mobs et items au sol seront supprimés dans §e30 secondes§f.");

        Bukkit.getScheduler().runTaskLater(SkyBlockPlugin.getInstance(), () -> {
            int mobs  = 0;
            int items = 0;

            for (World world : Bukkit.getWorlds()) {
                if (!world.getName().equals("skyblock")) continue;
                for (Entity entity : world.getEntities()) {
                    if (isProtected(entity)) continue;

                    if (entity instanceof Mob mob) {
                        // Nettoyer les maps de stack proprement avant de remove
                        MobStackListener.removeStacked(mob.getUniqueId());
                        SpawnerListener.removeStackEntry(mob.getUniqueId());
                        mob.remove();
                        mobs++;
                    } else if (entity instanceof Item) {
                        entity.remove();
                        items++;
                    }
                }
            }

            Bukkit.broadcastMessage("§a[ClearLag] §f" + mobs + " mobs et " + items + " items supprimés.");

        }, WARNING_TICKS);
    }

    private boolean isProtected(Entity entity) {
        return entity instanceof ArmorStand
                || entity instanceof ItemFrame
                || entity instanceof GlowItemFrame
                || entity instanceof Villager
                || entity instanceof WanderingTrader
                || entity instanceof TextDisplay
                || entity instanceof ItemDisplay
                || entity instanceof BlockDisplay
                || entity instanceof Player
                || !entity.getMetadata("protected").isEmpty()
                || !entity.getMetadata("minion").isEmpty()
                || !entity.getMetadata("hologram").isEmpty();
    }
}