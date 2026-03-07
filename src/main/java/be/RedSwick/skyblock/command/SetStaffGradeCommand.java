package be.RedSwick.skyblock.command;

import be.RedSwick.skyblock.moderation.StaffPermissionManager;
import be.RedSwick.skyblock.moderation.StaffRank;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /setstaffgrade <joueur> <grade>
 * Grades : GUIDE, MODERATEUR, ADMIN, FONDATEUR, NONE (retire)
 * Accessible : ADMIN et FONDATEUR uniquement
 */
public class SetStaffGradeCommand implements CommandExecutor, org.bukkit.command.TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Console a accès total
        boolean isConsole = !(sender instanceof Player);
        StaffRank senderRank = isConsole ? StaffRank.FONDATEUR : StaffRank.of((Player) sender);

        if (!isConsole && !senderRank.isAtLeast(StaffRank.ADMIN)) {
            sender.sendMessage("§cRéservé aux Admins et Fondateurs."); return true;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage : /setstaffgrade <joueur> <GUIDE|MODERATEUR|ADMIN|FONDATEUR|NONE>");
            return true;
        }

        String targetName = args[0];
        StaffRank newRank;
        try {
            newRank = StaffRank.valueOf(args[1].toUpperCase());
        } catch (Exception e) {
            sender.sendMessage("Grade invalide. Utilise : GUIDE, MODERATEUR, ADMIN, FONDATEUR, NONE");
            return true;
        }

        // Un admin ne peut pas assigner FONDATEUR
        if (newRank == StaffRank.FONDATEUR && senderRank != StaffRank.FONDATEUR) {
            sender.sendMessage("§cSeul un Fondateur peut assigner le grade Fondateur."); return true;
        }

        // Un admin ne peut pas modifier un fondateur
        StaffRank targetCurrentRank = StaffPermissionManager.get().getRank(targetName);
        if (targetCurrentRank == StaffRank.FONDATEUR && senderRank != StaffRank.FONDATEUR) {
            sender.sendMessage("§cTu ne peux pas modifier le grade d'un Fondateur."); return true;
        }

        StaffPermissionManager.get().setStaffRank(targetName, newRank);

        String msg = newRank == StaffRank.NONE
                ? "Grade staff de " + targetName + " retiré."
                : "Grade de " + targetName + " set → " + newRank.name();
        sender.sendMessage(msg);

        // Notifier le joueur si connecté
        var online = org.bukkit.Bukkit.getPlayer(targetName);
        if (online != null) {
            online.sendMessage(newRank == StaffRank.NONE
                    ? "§7Ton grade staff a été retiré."
                    : "§aTon grade staff est maintenant " + newRank.getDisplay());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        if (args.length == 1)
            return org.bukkit.Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        if (args.length == 2)
            return List.of("GUIDE", "MODERATEUR", "ADMIN", "FONDATEUR", "NONE").stream()
                    .filter(g -> g.startsWith(args[1].toUpperCase()))
                    .toList();
        return List.of();
    }
}