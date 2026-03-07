package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.moderation.ServerState;
import be.RedSwick.skyblock.moderation.StaffRank;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerLoginEvent;

public class ServerCloseListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        if (!ServerState.INSTANCE.isClosed()) return;
        // Seuls ADMIN et FONDATEUR peuvent se connecter quand serveur fermé
        // On vérifie via les permissions déjà attribuées au joueur
        if (event.getPlayer().hasPermission("arcanium.admin")
                || event.getPlayer().hasPermission("arcanium.fondateur")) return;
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                "§4§lSERVEUR FERMÉ\n§7Le serveur est temporairement fermé. Revenez plus tard.");
    }
}