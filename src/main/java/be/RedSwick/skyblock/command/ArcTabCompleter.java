package be.RedSwick.skyblock.command;

import be.RedSwick.skyblock.player.PlayerGrade;
import be.RedSwick.skyblock.player.PlayerJob;
import be.RedSwick.skyblock.player.PlayerRank;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ArcTabCompleter implements TabCompleter {

    private static final List<String> SUB_COMMANDS = List.of(
            "give", "setrank", "setgrade", "island", "job", "item", "reload"
    );
    private static final List<String> GIVE_TYPES   = List.of("coins", "gems", "essence");
    private static final List<String> ISLAND_SUBS  = List.of("info", "delete", "tp");
    private static final List<String> RANKS  = Arrays.stream(PlayerRank.values())
            .map(r -> r.name().toLowerCase()).collect(Collectors.toList());
    private static final List<String> GRADES = Arrays.stream(PlayerGrade.values())
            .map(g -> g.name().toLowerCase()).collect(Collectors.toList());
    private static final List<String> JOBS   = Arrays.stream(PlayerJob.values())
            .map(j -> j.name().toLowerCase()).collect(Collectors.toList());

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd,
                                      String alias, String[] args) {
        if (!sender.hasPermission("arcanium.admin")
                && !(sender instanceof Player p && p.isOp())) return List.of();

        return switch (args.length) {
            // Arg 1 : sous-commande
            case 1 -> filter(SUB_COMMANDS, args[0]);

            // Arg 2 : dépend de la sous-commande
            case 2 -> switch (args[0].toLowerCase()) {
                case "give", "setrank", "setgrade", "job" -> playerNames(args[1]);
                case "island" -> filter(ISLAND_SUBS, args[1]);
                default -> List.of();
            };

            // Arg 3
            case 3 -> switch (args[0].toLowerCase()) {
                case "give"     -> filter(GIVE_TYPES, args[2]);
                case "setrank"  -> filter(RANKS, args[2]);
                case "setgrade" -> filter(GRADES, args[2]);
                case "job"      -> filter(JOBS, args[2]);
                case "island"   -> playerNames(args[2]);
                default -> List.of();
            };

            // Arg 4 : montant pour give / niveau pour job
            case 4 -> switch (args[0].toLowerCase()) {
                case "give" -> List.of("100", "1000", "10000", "100000");
                case "job"  -> List.of("1", "10", "25", "50", "100", "150");
                default -> List.of();
            };

            default -> List.of();
        };
    }

    private List<String> playerNames(String input) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> filter(List<String> list, String input) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}