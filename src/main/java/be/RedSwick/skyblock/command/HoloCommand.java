package be.RedSwick.skyblock.command;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.leaderboard.LeaderboardManager;
import be.RedSwick.skyblock.leaderboard.LeaderboardType;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /holo <spawn|remove|list|refresh> [type]
 *
 * /holo spawn blockminer   → spawn l'hologramme à ta position
 * /holo remove blockminer  → supprime l'hologramme
 * /holo refresh            → rafraîchit tous les holos
 * /holo list               → liste les holos actifs
 */
public class HoloCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "arcanium.admin";

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cJoueurs uniquement."); return true; }
        if (!p.hasPermission(PERM)) { p.sendMessage("§cPas la permission."); return true; }

        if (args.length == 0) { sendHelp(p); return true; }

        LeaderboardManager lm = SkyBlockPlugin.getInstance().getLeaderboardManager();

        switch (args[0].toLowerCase()) {

            case "spawn" -> {
                if (args.length < 2) { p.sendMessage("§cUsage : /holo spawn <type>"); return true; }
                LeaderboardType type = LeaderboardType.fromId(args[1]);
                if (type == null) { p.sendMessage("§cType inconnu : " + args[1]); return true; }
                lm.spawnHolo(type, p.getLocation());
                p.sendMessage("§a✔ Hologramme §f" + type.getDisplay() + " §aspawné ici !");
            }

            case "remove" -> {
                if (args.length < 2) { p.sendMessage("§cUsage : /holo remove <type>"); return true; }
                LeaderboardType type = LeaderboardType.fromId(args[1]);
                if (type == null) { p.sendMessage("§cType inconnu : " + args[1]); return true; }
                if (!lm.hasHolo(type)) { p.sendMessage("§cAucun hologramme de ce type."); return true; }
                lm.deleteHolo(type);
                p.sendMessage("§a✔ Hologramme §f" + type.getDisplay() + " §asupprimé.");
            }

            case "refresh" -> {
                lm.refreshAll();
                p.sendMessage("§a✔ Tous les hologrammes rafraîchis (" + LeaderboardType.values().length + " classements).");
            }

            case "list" -> {
                p.sendMessage("§6§lHologrammes actifs :");
                boolean any = false;
                for (LeaderboardType type : LeaderboardType.values()) {
                    if (lm.hasHolo(type)) {
                        var loc = lm.getHoloLocation(type);
                        p.sendMessage("§7- §f" + type.getDisplay() + " §8@ §7"
                                + loc.getWorld().getName() + " "
                                + (int)loc.getX() + "/" + (int)loc.getY() + "/" + (int)loc.getZ());
                        any = true;
                    }
                }
                if (!any) p.sendMessage("§7Aucun hologramme spawné.");
            }

            default -> sendHelp(p);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return List.of("spawn", "remove", "refresh", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("remove"))) {
            LeaderboardManager lm = SkyBlockPlugin.getInstance().getLeaderboardManager();
            List<String> ids = new ArrayList<>();
            for (LeaderboardType t : LeaderboardType.values()) {
                // Pour spawn : tous les types. Pour remove : seulement les actifs
                if (args[0].equalsIgnoreCase("remove") && !lm.hasHolo(t)) continue;
                if (t.getId().startsWith(args[1].toLowerCase())) ids.add(t.getId());
            }
            return ids;
        }
        return List.of(); // jamais null — sinon Bukkit propose les noms de joueurs
    }

    private void sendHelp(Player p) {
        p.sendMessage("§6§lHolo §8—§7 Commandes :");
        p.sendMessage("§e/holo spawn <type>   §7→ spawner à ta position");
        p.sendMessage("§e/holo remove <type>  §7→ supprimer");
        p.sendMessage("§e/holo refresh        §7→ rafraîchir tous");
        p.sendMessage("§e/holo list           §7→ lister les actifs");
        p.sendMessage("§7Types : §f" + String.join(", ", LeaderboardType.allIds()));
    }
}