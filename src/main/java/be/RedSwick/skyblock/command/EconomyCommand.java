package be.RedSwick.skyblock.command;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.manager.PlayerDataManager;
import be.RedSwick.skyblock.player.PlayerData;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class EconomyCommand implements CommandExecutor, TabCompleter {

    private final PlayerDataManager pdm = SkyBlockPlugin.getInstance().getPlayerDataManager();

    // ════════════════════════════════════════════════
    //  /bal [joueur]
    // ════════════════════════════════════════════════

    public static class BalCommand implements CommandExecutor {
        private final PlayerDataManager pdm = SkyBlockPlugin.getInstance().getPlayerDataManager();

        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (args.length == 0) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cUtilisation : /bal <joueur>");
                    return true;
                }
                PlayerData data = pdm.get(player.getUniqueId());
                sendBalance(player, player.getName(), data);
            } else {
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) { sender.sendMessage("§cJoueur introuvable."); return true; }
                PlayerData data = pdm.get(target.getUniqueId());
                if (data == null) { sender.sendMessage("§cDonnées introuvables."); return true; }
                sendBalance(sender, target.getName(), data);
            }
            return true;
        }

        private void sendBalance(CommandSender sender, String name, PlayerData data) {
            sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            sender.sendMessage("§6§l 💰 Solde de §e" + name);
            sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            sender.sendMessage(" §6Coins   §8» §e" + fmt(data.getCoins()) + " §6⬡");
            sender.sendMessage(" §5Essence §8» §d" + fmt(data.getEssence()) + " §5✦");
            sender.sendMessage(" §bGemmes  §8» §3" + fmt(data.getGems()) + " §b💎");
            sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        }
    }

    // ════════════════════════════════════════════════
    //  /pay <joueur> <montant>
    // ════════════════════════════════════════════════

    public static class PayCommand implements CommandExecutor {
        private final PlayerDataManager pdm = SkyBlockPlugin.getInstance().getPlayerDataManager();

        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!(sender instanceof Player player)) return true;
            if (args.length < 2) {
                player.sendMessage("§cUsage : /pay <joueur> <montant>"); return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || target == player) {
                player.sendMessage("§cJoueur introuvable ou invalide."); return true;
            }

            long amount;
            try { amount = Long.parseLong(args[1]); }
            catch (NumberFormatException e) {
                player.sendMessage("§cMontant invalide."); return true;
            }

            if (amount <= 0) { player.sendMessage("§cMontant doit être positif."); return true; }

            PlayerData senderData = pdm.get(player.getUniqueId());
            PlayerData recipientData = pdm.get(target.getUniqueId());

            if (senderData == null || recipientData == null) return true;

            if (!senderData.removeCoins(amount)) {
                player.sendMessage("§cCoins insuffisants ! Tu as §e" + fmt(senderData.getCoins()) + " §c⬡"); return true;
            }

            recipientData.addCoins(amount);
            pdm.savePlayer(player.getUniqueId());
            pdm.savePlayer(target.getUniqueId());

            player.sendMessage("§aTu as envoyé §e" + fmt(amount) + " §6⬡ §aà §e" + target.getName() + "§a !");
            target.sendMessage("§e" + player.getName() + " §at'a envoyé §e" + fmt(amount) + " §6⬡ §aCoins !");
            return true;
        }
    }

    // ════════════════════════════════════════════════
    //  /essence
    // ════════════════════════════════════════════════

    public static class EssenceCommand implements CommandExecutor {
        private final PlayerDataManager pdm = SkyBlockPlugin.getInstance().getPlayerDataManager();

        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!(sender instanceof Player player)) return true;
            PlayerData data = pdm.get(player.getUniqueId());
            if (data == null) return true;

            player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            player.sendMessage("§5§l ✦ Ton Essence");
            player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            player.sendMessage(" §5Actuelle §8» §d" + fmt(data.getEssence()) + " §5✦");
            player.sendMessage(" §5Total gagné §8» §d" + fmt(data.getTotalEssenceEarned()) + " §5✦");
            player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            player.sendMessage("§7L'Essence se gagne dans le §5monde Aventure§7.");
            return true;
        }
    }

    // ════════════════════════════════════════════════
    //  /baltop
    // ════════════════════════════════════════════════

    public static class BaltopCommand implements CommandExecutor {
        private final PlayerDataManager pdm = SkyBlockPlugin.getInstance().getPlayerDataManager();

        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            List<PlayerData> sorted = pdm.getAll().stream()
                    .sorted((a, b) -> Long.compare(b.getCoins(), a.getCoins()))
                    .limit(10)
                    .collect(Collectors.toList());

            sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            sender.sendMessage("§6§l 💰 Top 10 — Richesse");
            sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

            for (int i = 0; i < sorted.size(); i++) {
                PlayerData data = sorted.get(i);
                String name  = Bukkit.getOfflinePlayer(data.getUuid()).getName();
                String medal = switch (i) {
                    case 0 -> "§6#1";
                    case 1 -> "§7#2";
                    case 2 -> "§c#3";
                    default -> "§f#" + (i + 1);
                };
                sender.sendMessage(" " + medal + " §e" + name
                        + " §8— §e" + fmt(data.getCoins()) + " §6⬡");
            }

            sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            return true;
        }
    }

    // ════════════════════════════════════════════════
    //  /setbal <joueur> <montant> — ADMIN
    // ════════════════════════════════════════════════

    public static class SetBalCommand implements CommandExecutor {
        private final PlayerDataManager pdm = SkyBlockPlugin.getInstance().getPlayerDataManager();

        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!sender.hasPermission("arcanium.admin")) {
                sender.sendMessage("§cPas la permission."); return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUsage : /setbal <joueur> <montant>"); return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) { sender.sendMessage("§cJoueur introuvable."); return true; }

            long amount;
            try { amount = Long.parseLong(args[1]); }
            catch (NumberFormatException e) { sender.sendMessage("§cMontant invalide."); return true; }

            PlayerData data = pdm.get(target.getUniqueId());
            if (data == null) return true;

            data.setCoins(amount);
            pdm.savePlayer(target.getUniqueId());

            sender.sendMessage("§aCoins de §e" + target.getName()
                    + " §afixés à §e" + fmt(amount) + " §6⬡");
            target.sendMessage("§7Ton solde a été modifié : §e" + fmt(amount) + " §6⬡");
            return true;
        }
    }

    // ════════════════════════════════════════════════
    //  /setgems <joueur> <montant> — ADMIN
    // ════════════════════════════════════════════════

    public static class SetGemsCommand implements CommandExecutor {
        private final PlayerDataManager pdm = SkyBlockPlugin.getInstance().getPlayerDataManager();

        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!sender.hasPermission("arcanium.admin")) {
                sender.sendMessage("§cPas la permission."); return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUsage : /setgems <joueur> <montant>"); return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) { sender.sendMessage("§cJoueur introuvable."); return true; }

            long amount;
            try { amount = Long.parseLong(args[1]); }
            catch (NumberFormatException e) { sender.sendMessage("§cMontant invalide."); return true; }

            PlayerData data = pdm.get(target.getUniqueId());
            if (data == null) return true;

            data.setGems(amount);
            pdm.savePlayer(target.getUniqueId());

            sender.sendMessage("§bGemmes de §e" + target.getName()
                    + " §bfixées à §3" + fmt(amount) + " §b💎");
            target.sendMessage("§7Tes gemmes ont été modifiées : §3" + fmt(amount) + " §b💎");
            return true;
        }
    }

    // ─────────────────────────────────────────────
    /** Format complet pour /bal : 1230456 → "1 230 456" */
    private static String fmt(long n) {
        return String.format("%,d", n).replace(',', ' ');
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) { return true; }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(a[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }
}