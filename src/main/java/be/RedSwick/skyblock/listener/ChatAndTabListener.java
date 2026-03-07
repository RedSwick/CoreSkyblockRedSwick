package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.manager.IslandTeamChatManager;
import be.RedSwick.skyblock.manager.PlayerDataManager;
import be.RedSwick.skyblock.moderation.StaffPermissionManager;
import be.RedSwick.skyblock.moderation.StaffRank;
import be.RedSwick.skyblock.player.PlayerData;
import be.RedSwick.skyblock.player.PlayerGrade;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

public class ChatAndTabListener implements Listener {

    private final PlayerDataManager     pdm       = SkyBlockPlugin.getInstance().getPlayerDataManager();
    private final IslandTeamChatManager tcManager = SkyBlockPlugin.getInstance().getTeamChatManager();

    // ════════════════════════════════════════════════
    //  FORMAT CHAT + TEAMCHAT intercept
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();

        // Intercept TeamChat
        if (tcManager.isInTeamChat(p.getUniqueId())) {
            event.setCancelled(true);
            // TeamChat doit tourner sur le thread principal (accès à Bukkit API)
            String msg = event.getMessage();
            Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                    () -> tcManager.sendTeamMessage(p, msg));
            return;
        }

        // Chat normal
        String prefix = buildPrefix(p);
        event.setFormat(prefix + "§f" + p.getName() + " §8» §7" + event.getMessage());
    }

    // ════════════════════════════════════════════════
    //  TAB LIST
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(SkyBlockPlugin.getInstance(), () -> {
            updateTabName(event.getPlayer());
            for (Player online : Bukkit.getOnlinePlayers()) updateTabName(online);
        }, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Désactiver teamchat à la déconnexion
        PlayerData data = pdm.get(event.getPlayer().getUniqueId());
        if (data != null) data.setTeamChatEnabled(false);

        Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(), () -> {
            for (Player online : Bukkit.getOnlinePlayers()) updateTabName(online);
        });
    }

    // ════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════

    private void updateTabName(Player p) {
        String prefix = buildPrefix(p);
        // Indicateur teamchat actif
        PlayerData data = pdm.get(p.getUniqueId());
        String tc = (data != null && data.isTeamChatEnabled()) ? "§b[TC] " : "";
        p.setPlayerListName(tc + prefix + "§f" + p.getName());
    }

    private String buildPrefix(Player p) {
        StaffRank staffRank = StaffRank.of(p);
        if (staffRank != StaffRank.NONE) return staffRank.getPrefix();

        PlayerData data = pdm.get(p.getUniqueId());
        if (data == null) return "§7";
        if (data.getGrade() != PlayerGrade.AUCUN) return data.getGrade().getPrefix();
        return data.getRank().getDisplay() + " §r";
    }
}