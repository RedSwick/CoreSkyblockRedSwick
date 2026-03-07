package be.RedSwick.skyblock.command;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.gui.AdminItemGUI;
import be.RedSwick.skyblock.island.Island;
import be.RedSwick.skyblock.manager.*;
import be.RedSwick.skyblock.player.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class ArcCommand implements CommandExecutor {

    private final PlayerDataManager pdm = SkyBlockPlugin.getInstance().getPlayerDataManager();
    private final IslandManager     im  = SkyBlockPlugin.getInstance().getIslandManager();

    private static final String PERM = "arcanium.admin";

    private boolean isAdmin(CommandSender s) {
        return s.hasPermission(PERM) || (s instanceof Player p && p.isOp()) || !(s instanceof Player);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!isAdmin(sender)) {
            sender.sendMessage("§cPas la permission."); return true;
        }
        if (args.length == 0) { sendHelp(sender); return true; }

        return switch (args[0].toLowerCase()) {
            case "give"     -> handleGive(sender, args);
            case "setrank"  -> handleSetRank(sender, args);
            case "setgrade" -> handleSetGrade(sender, args);
            case "island"   -> handleIsland(sender, args);
            case "job"      -> handleJob(sender, args);
            case "item"     -> handleItem(sender, args);
            case "reload"   -> handleReload(sender);
            default         -> { sendHelp(sender); yield true; }
        };
    }

    // ════════════════════════════════════════════════
    //  /arc give <joueur> <coins|gems|essence|item> <montant|itemId>
    // ════════════════════════════════════════════════
    private boolean handleGive(CommandSender s, String[] a) {
        if (a.length < 4) { s.sendMessage("§cUsage : /arc give <joueur> <coins|gems|essence> <montant>"); return true; }
        Player target = Bukkit.getPlayer(a[1]);
        if (target == null) { s.sendMessage("§cJoueur introuvable."); return true; }
        var data = pdm.get(target.getUniqueId());
        if (data == null) { s.sendMessage("§cDonnées introuvables."); return true; }

        long amount;
        try { amount = Long.parseLong(a[3]); } catch (Exception e) { s.sendMessage("§cMontant invalide."); return true; }

        switch (a[2].toLowerCase()) {
            case "coins"   -> { data.addCoins(amount);   s.sendMessage("§a+" + amount + " coins → " + target.getName()); target.sendMessage("§a§lADMIN §r§aTu as reçu §e" + amount + " §6⬡ coins"); }
            case "gems"    -> { data.addGems(amount);    s.sendMessage("§a+" + amount + " gems → " + target.getName());  target.sendMessage("§a§lADMIN §r§aTu as reçu §3" + amount + " 💎 gemmes"); }
            case "essence" -> { data.addEssence(amount); s.sendMessage("§a+" + amount + " essence → " + target.getName()); target.sendMessage("§a§lADMIN §r§aTu as reçu §5" + amount + " ✦ essence"); }
            default        -> { s.sendMessage("§cType invalide. Utilise : coins, gems, essence"); return true; }
        }
        pdm.savePlayer(data.getUuid());
        return true;
    }

    // ════════════════════════════════════════════════
    //  /arc setrank <joueur> <rang>
    // ════════════════════════════════════════════════
    private boolean handleSetRank(CommandSender s, String[] a) {
        if (a.length < 3) {
            s.sendMessage("§cUsage : /arc setrank <joueur> <rang>");
            s.sendMessage("§7Rangs : " + java.util.Arrays.stream(PlayerRank.values())
                    .map(r -> r.getDisplay()).reduce((x, y) -> x + "§7, " + y).orElse(""));
            return true;
        }
        Player target = Bukkit.getPlayer(a[1]);
        if (target == null) { s.sendMessage("§cJoueur introuvable."); return true; }
        var data = pdm.get(target.getUniqueId());
        if (data == null) return true;

        PlayerRank rank;
        try { rank = PlayerRank.valueOf(a[2].toUpperCase()); }
        catch (Exception e) { s.sendMessage("§cRang invalide : §f" + a[2]); return true; }

        data.setRank(rank);
        pdm.savePlayer(data.getUuid());
        s.sendMessage("§aRang de §f" + target.getName() + " §aset → " + rank.getDisplay());
        target.sendMessage("§a§lADMIN §r§aTon rang est maintenant " + rank.getDisplay());
        return true;
    }

    // ════════════════════════════════════════════════
    //  /arc setgrade <joueur> <grade>
    // ════════════════════════════════════════════════
    private boolean handleSetGrade(CommandSender s, String[] a) {
        if (a.length < 3) {
            s.sendMessage("§cUsage : /arc setgrade <joueur> <grade>");
            s.sendMessage("§7Grades : AUCUN, NEXUS, ASCENDANT, ARCANIUM");
            return true;
        }
        Player target = Bukkit.getPlayer(a[1]);
        if (target == null) { s.sendMessage("§cJoueur introuvable."); return true; }
        var data = pdm.get(target.getUniqueId());
        if (data == null) return true;

        PlayerGrade grade;
        try { grade = PlayerGrade.valueOf(a[2].toUpperCase()); }
        catch (Exception e) { s.sendMessage("§cGrade invalide : §f" + a[2]); return true; }

        data.setGrade(grade);
        pdm.savePlayer(data.getUuid());
        s.sendMessage("§aGrade de §f" + target.getName() + " §aset → " + grade.getDisplay());
        target.sendMessage("§a§lADMIN §r§aTon grade est maintenant " + grade.getDisplay());
        return true;
    }

    // ════════════════════════════════════════════════
    //  /arc island <info|delete|tp> <joueur>
    // ════════════════════════════════════════════════
    private boolean handleIsland(CommandSender s, String[] a) {
        if (a.length < 3) { s.sendMessage("§cUsage : /arc island <info|delete|tp> <joueur>"); return true; }
        Player target = Bukkit.getPlayer(a[2]);
        if (target == null) { s.sendMessage("§cJoueur introuvable."); return true; }
        Island island = im.getIslandByMember(target.getUniqueId());
        if (island == null) { s.sendMessage("§cCe joueur n'a pas d'île."); return true; }

        switch (a[1].toLowerCase()) {
            case "info" -> {
                s.sendMessage("§8▬▬ §6Île de §e" + target.getName() + " §8▬▬");
                s.sendMessage("§7Centre : §f" + fmt(island.getCenter()));
                s.sendMessage("§7IS Level : §f" + String.format("%.1f", island.getIsLevel()));
                s.sendMessage("§7Rayon : §f" + island.getRadius());
                s.sendMessage("§7Membres : §f" + island.getAllMembers().size());
            }
            case "delete" -> {
                im.deleteIsland(island.getOwner());
                s.sendMessage("§aÎle de §f" + target.getName() + " §asupprimée.");
                target.sendMessage("§c§lADMIN §r§cTon île a été supprimée par un administrateur.");
            }
            case "tp" -> {
                if (!(s instanceof Player admin)) { s.sendMessage("§cCommande joueur uniquement pour tp."); return true; }
                admin.teleport(island.getCenter());
                s.sendMessage("§aTéléporté sur l'île de §f" + target.getName());
            }
            default -> s.sendMessage("§cSous-commande invalide : info | delete | tp");
        }
        return true;
    }

    // ════════════════════════════════════════════════
    //  /arc job <joueur> <métier> <niveau>
    // ════════════════════════════════════════════════
    private boolean handleJob(CommandSender s, String[] a) {
        if (a.length < 4) {
            s.sendMessage("§cUsage : /arc job <joueur> <métier> <niveau>");
            s.sendMessage("§7Métiers : CHASSEUR, FARMER, MINER, BUCHERON, ALCHIMISTE, PECHEUR");
            return true;
        }
        Player target = Bukkit.getPlayer(a[1]);
        if (target == null) { s.sendMessage("§cJoueur introuvable."); return true; }
        var data = pdm.get(target.getUniqueId());
        if (data == null) return true;

        PlayerJob job;
        try { job = PlayerJob.valueOf(a[2].toUpperCase()); }
        catch (Exception e) { s.sendMessage("§cMétier invalide : §f" + a[2]); return true; }

        int level;
        try { level = Integer.parseInt(a[3]); } catch (Exception e) { s.sendMessage("§cNiveau invalide."); return true; }
        level = Math.max(1, Math.min(level, 150));

        data.setJobLevel(job, level);
        pdm.savePlayer(data.getUuid());
        s.sendMessage("§aMétier §f" + job.getDisplay() + " §ade §f" + target.getName() + " §aset niveau §e" + level);
        target.sendMessage("§a§lADMIN §r§aTon niveau " + job.getDisplay() + " §aest maintenant §e" + level);
        return true;
    }

    // ════════════════════════════════════════════════
    //  /arc item — GUI items custom
    // ════════════════════════════════════════════════
    private boolean handleItem(CommandSender s, String[] a) {
        if (!(s instanceof Player p)) { s.sendMessage("§cCommande joueur uniquement."); return true; }
        p.openInventory(AdminItemGUI.create());
        return true;
    }

    // ════════════════════════════════════════════════
    //  /arc reload
    // ════════════════════════════════════════════════
    private boolean handleReload(CommandSender s) {
        // Recharge les données de tous les joueurs connectés
        for (Player p : Bukkit.getOnlinePlayers()) {
            pdm.savePlayer(p.getUniqueId());
        }
        s.sendMessage("§aArcanium rechargé.");
        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage("§8▬▬▬ §6§lArcanium Admin §8▬▬▬");
        s.sendMessage("§e/arc give §f<joueur> <coins|gems|essence> <montant>");
        s.sendMessage("§e/arc setrank §f<joueur> <rang>");
        s.sendMessage("§e/arc setgrade §f<joueur> <grade>");
        s.sendMessage("§e/arc island §f<info|delete|tp> <joueur>");
        s.sendMessage("§e/arc job §f<joueur> <métier> <niveau>");
        s.sendMessage("§e/arc item §f— GUI items custom");
        s.sendMessage("§e/arc reload §f— Recharger");
    }

    private String fmt(Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }
}