package be.RedSwick.skyblock.command;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.gui.LeaderboardGUI;
import be.RedSwick.skyblock.leaderboard.LeaderboardType;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /classement [type] — ouvre le GUI de classement.
 * Si type fourni, ouvre directement le détail.
 */
public class LeaderboardCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cJoueurs uniquement.");
            return true;
        }

        if (args.length == 0) {
            Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                    () -> p.openInventory(LeaderboardGUI.createMain()));
            return true;
        }

        LeaderboardType type = LeaderboardType.fromId(args[0]);
        if (type == null) {
            p.sendMessage("§cClassement inconnu. Types : " + LeaderboardType.allIds());
            return true;
        }

        Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                () -> p.openInventory(LeaderboardGUI.createDetail(type, p)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return LeaderboardType.allIds().stream()
                    .filter(id -> id.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}