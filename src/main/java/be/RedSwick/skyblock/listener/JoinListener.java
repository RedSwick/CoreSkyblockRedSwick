package be.RedSwick.skyblock.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        if (Bukkit.getWorld("world") == null) return;

        event.getPlayer().teleport(
                Bukkit.getWorld("world").getSpawnLocation()
        );

        event.getPlayer().setWorldBorder(null);
    }
}