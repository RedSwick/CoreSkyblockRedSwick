package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.moderation.StaffPermissionManager;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

public class StaffPermissionListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        // Applique les permissions staff dès la connexion (priorité LOWEST = en premier)
        StaffPermissionManager.get().applyPermissions(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        StaffPermissionManager.get().removePermissions(event.getPlayer());
    }
}