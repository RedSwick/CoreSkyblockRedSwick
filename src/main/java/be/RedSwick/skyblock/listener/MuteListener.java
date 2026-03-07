package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.moderation.MuteManager;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class MuteListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();
        MuteManager m = MuteManager.get();
        if (!m.isMuted(p.getUniqueId())) return;
        event.setCancelled(true);
        p.sendMessage("§cTu es mute encore §e" + m.formatRemaining(p.getUniqueId())
                + " §c— " + m.getReason(p.getUniqueId()));
    }
}