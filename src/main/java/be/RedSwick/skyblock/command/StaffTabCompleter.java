package be.RedSwick.skyblock.command;

import be.RedSwick.skyblock.moderation.StaffRank;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tab completion pour les commandes de modération.
 * Chaque commande suggère les noms de joueurs en ligne.
 */
public class StaffTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (!(sender instanceof Player player)) return List.of();
        StaffRank rank = StaffRank.of(player);

        String cmdName = command.getName().toLowerCase();

        // Arg 1 = nom du joueur pour la plupart des commandes
        if (args.length == 1) {
            return playerNames(args[0]);
        }

        // Arg 2 = durée pour mute / tempban
        if (args.length == 2) {
            return switch (cmdName) {
                case "mute"    -> List.of("5", "10", "30", "60", "120", "1440");
                case "tempban" -> List.of("1", "3", "7", "14", "30", "365");
                default        -> List.of();
            };
        }

        // Arg 3+ = raison (suggestions courantes)
        if (args.length >= 3) {
            return switch (cmdName) {
                case "mute", "kick", "ban", "tempban", "warn" -> List.of(
                        "Spam", "Insultes", "Publicité", "Triche", "Comportement_toxique",
                        "Non-respect_des_règles"
                );
                default -> List.of();
            };
        }

        return List.of();
    }

    public static List<String> playerNamesStatic(String input) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> playerNames(String input) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}