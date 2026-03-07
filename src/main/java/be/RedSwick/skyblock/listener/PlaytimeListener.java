package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

/**
 * Compte le temps de jeu des joueurs en ticks.
 * Ticker toutes les 20 ticks (1 seconde) = 1200 ticks/minute.
 */
public class PlaytimeListener implements Listener {

    public static void startTicker() {
        Bukkit.getScheduler().runTaskTimer(SkyBlockPlugin.getInstance(), () -> {
            for (var p : Bukkit.getOnlinePlayers()) {
                PlayerData data = SkyBlockPlugin.getInstance()
                        .getPlayerDataManager().get(p.getUniqueId());
                if (data != null) data.addPlaytimeTicks(20);
            }
        }, 20L, 20L);
    }
}