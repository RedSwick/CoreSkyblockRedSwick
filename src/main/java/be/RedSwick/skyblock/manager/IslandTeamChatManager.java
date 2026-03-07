package be.RedSwick.skyblock.manager;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.island.Island;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Gère le TeamChat par île.
 * Quand activé pour un joueur, tous ses messages sont redirigés vers l'équipe.
 */
public class IslandTeamChatManager {

    private final IslandManager manager = SkyBlockPlugin.getInstance().getIslandManager();

    // ════════════════════════════════════════════════
    //  Toggle
    // ════════════════════════════════════════════════

    public void toggle(Player player) {
        Island island = manager.getIslandByMember(player.getUniqueId());
        if (island == null) {
            player.sendMessage("§cTu n'as pas d'île !");
            return;
        }
        // Le flag teamchat est stocké sur l'île mais par joueur via PlayerData ou en RAM
        // On utilise le flag par joueur stocké dans PlayerData
        be.RedSwick.skyblock.player.PlayerData data =
                SkyBlockPlugin.getInstance().getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        boolean now = !data.isTeamChatEnabled();
        data.setTeamChatEnabled(now);

        if (now) {
            player.sendMessage("§a§l[TeamChat] §aActivé — tes messages vont à l'équipe.");
        } else {
            player.sendMessage("§7§l[TeamChat] §7Désactivé — retour au chat global.");
        }
    }

    // ════════════════════════════════════════════════
    //  Envoyer un message d'équipe
    // ════════════════════════════════════════════════

    public void sendTeamMessage(Player sender, String message) {
        Island island = manager.getIslandByMember(sender.getUniqueId());
        if (island == null) return;

        String formatted = "§b§l[Équipe] §r§7" + sender.getName() + " §f» §e" + message;

        for (UUID uid : island.getAllMembers()) {
            Player member = Bukkit.getPlayer(uid);
            if (member != null && member.isOnline()) {
                member.sendMessage(formatted);
            }
        }
        // Log console
        Bukkit.getLogger().info("[TeamChat] [Île de " +
                Bukkit.getOfflinePlayer(island.getOwner()).getName() + "] " +
                sender.getName() + ": " + message);
    }

    // ════════════════════════════════════════════════
    //  Check si le joueur est en TeamChat
    // ════════════════════════════════════════════════

    public boolean isInTeamChat(UUID uuid) {
        be.RedSwick.skyblock.player.PlayerData data =
                SkyBlockPlugin.getInstance().getPlayerDataManager().get(uuid);
        return data != null && data.isTeamChatEnabled();
    }
}