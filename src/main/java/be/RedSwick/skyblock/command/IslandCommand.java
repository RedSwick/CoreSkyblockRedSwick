package be.RedSwick.skyblock.command;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.gui.*;
import be.RedSwick.skyblock.island.*;
import be.RedSwick.skyblock.listener.IslandFlyListener;
import be.RedSwick.skyblock.manager.*;
import be.RedSwick.skyblock.player.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class IslandCommand implements CommandExecutor, TabCompleter {

    private final IslandManager          manager     = SkyBlockPlugin.getInstance().getIslandManager();
    private final PlayerDataManager      pdm         = SkyBlockPlugin.getInstance().getPlayerDataManager();
    private final WarpManager            warpManager = SkyBlockPlugin.getInstance().getWarpManager();
    private final IslandTeamChatManager  tcManager   = SkyBlockPlugin.getInstance().getTeamChatManager();

    private static final List<String> SUB_COMMANDS = Arrays.asList(
            // existants
            "create", "delete", "go", "border",
            "invite", "join", "leave",
            "promote", "demote",
            "kick", "ban", "unban",
            "open", "close",
            "warp", "block", "recalc", "top",
            "permissions", "missions",
            // nouveaux
            "name", "sethome", "home", "info",
            "team", "teamchat", "tc",
            "fly",
            "transfer",
            "coop", "uncoop",
            "visit",
            "expel",
            "upgrade", "upgrades",
            "settings",
            "banlist"
    );

    // ════════════════════════════════════════════════
    //  DISPATCH
    // ════════════════════════════════════════════════

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length == 0) { sendHelp(player); return true; }

        String sub    = args[0].toLowerCase();
        Island island = manager.getIslandByMember(player.getUniqueId());

        return switch (sub) {
            case "create"       -> cmdCreate(player, island);
            case "delete"       -> cmdDelete(player, island);
            case "go", "home"   -> cmdGo(player, island);
            case "border"       -> cmdBorder(player, island);
            case "permissions"  -> cmdPermissions(player, island);
            case "invite"       -> cmdInvite(player, island, args);
            case "join"         -> cmdJoin(player, island, args);
            case "leave"        -> cmdLeave(player, island);
            case "promote"      -> cmdPromote(player, island, args);
            case "demote"       -> cmdDemote(player, island, args);
            case "kick"         -> cmdKick(player, island, args);
            case "ban"          -> cmdBan(player, island, args);
            case "unban"        -> cmdUnban(player, island, args);
            case "open"         -> cmdOpen(player, island);
            case "close"        -> cmdClose(player, island);
            case "warp"         -> cmdWarp(player, island, args);
            case "block"        -> cmdBlock(player, island);
            case "recalc"       -> cmdRecalc(player, island);
            case "top"          -> cmdTop(player);
            case "missions"     -> cmdMissions(player, island);
            // ── NOUVEAUX ──
            case "name"         -> cmdName(player, island, args);
            case "sethome"      -> cmdSethome(player, island);
            case "info"         -> cmdInfo(player, island, args);
            case "team"         -> cmdTeam(player, island, args);
            case "teamchat","tc"-> cmdTeamchat(player, island, args);
            case "fly"          -> cmdFly(player, island);
            case "transfer"     -> cmdTransfer(player, island, args);
            case "coop"         -> cmdCoop(player, island, args);
            case "uncoop"       -> cmdUncoop(player, island, args);
            case "visit"        -> cmdVisit(player, args);
            case "expel"        -> cmdExpel(player, island, args);
            case "upgrade","upgrades" -> cmdUpgrade(player, island);
            case "settings"     -> cmdSettings(player, island);
            case "banlist"      -> cmdBanlist(player, island);
            default             -> {
                player.sendMessage("§cCommande inconnue. §e/is §cpour l'aide.");
                yield true;
            }
        };
    }

    // ════════════════════════════════════════════════
    //  CREATE
    // ════════════════════════════════════════════════

    private boolean cmdCreate(Player player, Island island) {
        if (island != null) { player.sendMessage("§cTu as déjà une île !"); return true; }
        World world = Bukkit.getWorld("skyblock");
        if (world == null) return true;

        Location center   = manager.getNextIslandLocation(world);
        Island   newIsland = manager.createIsland(player.getUniqueId(), center);

        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();

        // Colle le schéma via WorldEdit si disponible, sinon fallback bedrock+grass
        boolean pasted = SkyBlockPlugin.getInstance().getSchematicManager().pasteAt(center);
        if (!pasted) {
            world.getBlockAt(cx, cy - 1, cz).setType(Material.BEDROCK);
            world.getBlockAt(cx, cy,     cz).setType(Material.GRASS_BLOCK);
        }

        Location spawnLoc = findSafeSpawn(center);
        player.teleport(spawnLoc);
        applyBorder(player, newIsland);
        player.sendMessage("§a§lÎle créée ! Bienvenue sur Arcanium ✨");
        return true;
    }

    // ════════════════════════════════════════════════
    //  GO / HOME
    // ════════════════════════════════════════════════

    private boolean cmdGo(Player player, Island island) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }

        // Priorité : home perso → warp île → centre
        Location dest = island.hasHome()
                ? island.getHomeLocation()
                : (island.hasWarp() ? island.getWarpLocation() : findSafeSpawn(island.getCenter()));

        boolean othersOnline = island.getAllMembers().stream()
                .filter(uid -> !uid.equals(player.getUniqueId()))
                .anyMatch(uid -> Bukkit.getPlayer(uid) != null);
        if (!othersOnline)
            be.RedSwick.skyblock.listener.PlayerDataListener.despawnIslandMobs(island);

        player.teleport(dest);
        applyBorder(player, island);
        player.sendMessage("§aTéléporté sur ton île !");
        return true;
    }

    // ════════════════════════════════════════════════
    //  NAME  — /is name <nom> | /is name reset
    // ════════════════════════════════════════════════

    private boolean cmdName(Player player, Island island, String[] args) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        if (!island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§cSeul le chef peut renommer l'île !"); return true;
        }
        if (args.length < 2) {
            player.sendMessage("§cUsage : §e/is name <nom> §cou §e/is name reset");
            return true;
        }

        if (args[1].equalsIgnoreCase("reset")) {
            island.setName(null);
            manager.saveIsland(island);
            player.sendMessage("§7Nom de l'île réinitialisé.");
            return true;
        }

        // Reconstruction du nom (support espaces)
        String nom = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        // Sécurité : max 32 chars, strip codes couleur sauf §
        if (nom.replaceAll("§.", "").length() > 32) {
            player.sendMessage("§cNom trop long (max 32 caractères) !"); return true;
        }

        // Vérifier unicité
        Island existing = manager.getIslandByName(nom);
        if (existing != null && !existing.getOwner().equals(island.getOwner())) {
            player.sendMessage("§cCe nom est déjà pris !"); return true;
        }

        island.setName(ChatColor.translateAlternateColorCodes('&', nom));
        manager.saveIsland(island);
        player.sendMessage("§aÎle renommée : " + island.getName());
        return true;
    }

    // ════════════════════════════════════════════════
    //  SETHOME  — /is sethome
    // ════════════════════════════════════════════════

    private boolean cmdSethome(Player player, Island island) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        if (!island.hasPermission(player.getUniqueId(), IslandPermission.CREATE_WARP)) {
            player.sendMessage("§cTu n'as pas la permission !"); return true;
        }
        Island atLoc = manager.getIslandAtLocation(player.getLocation());
        if (atLoc == null || !atLoc.getOwner().equals(island.getOwner())) {
            player.sendMessage("§cTu dois être sur ton île !"); return true;
        }
        island.setHomeLocation(player.getLocation());
        manager.saveIsland(island);
        player.sendMessage("§a✦ Point d'accueil de l'île défini ici !");
        return true;
    }

    // ════════════════════════════════════════════════
    //  INFO  — /is info [joueur]
    // ════════════════════════════════════════════════

    private boolean cmdInfo(Player player, Island island, String[] args) {
        Island target = island;

        if (args.length >= 2) {
            // Chercher l'île d'un autre joueur
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
            Island found = manager.getIsland(op.getUniqueId());
            if (found == null) found = manager.getIslandByMember(op.getUniqueId());
            if (found == null) found = manager.getIslandByName(args[1]);
            if (found == null) { player.sendMessage("§cÎle introuvable pour §e" + args[1] + "§c."); return true; }
            target = found;
        }

        if (target == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }

        String ownerName = Bukkit.getOfflinePlayer(target.getOwner()).getName();
        String nom       = target.hasName() ? target.getName() : "§7(sans nom)";
        long members     = target.getMemberCount();
        long memberLimit = target.getMemberLimit();
        String status    = target.isOpen() ? "§aOuverte" : "§cFermée";

        player.sendMessage("§b§l╔══ Informations sur l'île ══╗");
        player.sendMessage("§b Nom      §7: " + nom);
        player.sendMessage("§b Chef     §7: §e" + ownerName);
        player.sendMessage("§b Niveau   §7: §e" + String.format("%.1f", target.getIsLevel()));
        player.sendMessage("§b Membres  §7: §e" + members + "§7/§e" + memberLimit);
        player.sendMessage("§b Statut   §7: " + status);
        player.sendMessage("§b Taille   §7: §e" + target.getUpgradeValue(IslandUpgrade.SIZE) + " blocs rayon");
        player.sendMessage("§b Spawners §7: §emax " + target.getUpgradeValue(IslandUpgrade.SPAWNER_LIMIT));

        // Liste membres online
        StringBuilder online = new StringBuilder();
        for (UUID uid : target.getAllMembers()) {
            Player m = Bukkit.getPlayer(uid);
            if (m != null) {
                IslandRole role = target.getRole(uid);
                online.append("§a").append(m.getName()).append(" §7(").append(role.name()).append("§7) ");
            }
        }
        if (!online.isEmpty()) player.sendMessage("§b Online    §7: " + online);
        player.sendMessage("§b§l╚════════════════════════════╝");
        return true;
    }

    // ════════════════════════════════════════════════
    //  TEAM  — /is team
    // ════════════════════════════════════════════════

    private boolean cmdTeam(Player player, Island island, String[] args) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }

        player.sendMessage("§6§l╔══ Équipe de l'île ══╗");
        for (UUID uid : island.getAllMembers()) {
            IslandRole     role  = island.getRole(uid);
            OfflinePlayer  op    = Bukkit.getOfflinePlayer(uid);
            String         name  = op.getName() != null ? op.getName() : "Inconnu";
            boolean        on    = op.isOnline();
            String         dot   = on ? "§a●" : "§7●";
            String         onoff = on ? "§aEn ligne" : "§7Hors ligne";

            String roleColor = switch (role) {
                case CHEF    -> "§6";
                case MANAGER -> "§e";
                case MEMBRE  -> "§a";
                case COOP    -> "§b";
            };
            player.sendMessage(dot + " " + roleColor + name + " §7— " + roleColor + role.name() + " §7— " + onoff);
        }
        // Coops séparés
        if (!island.getCoops().isEmpty()) {
            player.sendMessage("§b Coops temporaires :");
            for (UUID uid : island.getCoops()) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uid);
                String name = op.getName() != null ? op.getName() : "Inconnu";
                player.sendMessage("  §b○ " + name);
            }
        }
        player.sendMessage("§6§l╚═══════════════════╝");
        return true;
    }

    // ════════════════════════════════════════════════
    //  TEAMCHAT  — /is tc [message]
    // ════════════════════════════════════════════════

    private boolean cmdTeamchat(Player player, Island island, String[] args) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }

        // /is tc <message> → envoyer directement sans toggle
        if (args.length >= 2) {
            String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            tcManager.sendTeamMessage(player, msg);
            return true;
        }
        // /is tc seul → toggle
        tcManager.toggle(player);
        return true;
    }

    // ════════════════════════════════════════════════
    //  FLY  — /is fly
    // ════════════════════════════════════════════════

    private boolean cmdFly(Player player, Island island) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }

        // Vérifier grade
        PlayerData data = pdm.get(player.getUniqueId());
        if (data == null || (data.getGrade() == PlayerGrade.AUCUN)) {
            player.sendMessage("§cTu as besoin du grade §5§lARCANIUM §c(ou supérieur) pour le fly !");
            return true;
        }

        // Vérifier permission île
        if (!island.hasPermission(player.getUniqueId(), IslandPermission.FLY)) {
            player.sendMessage("§cLe fly est désactivé pour ton rôle sur cette île !"); return true;
        }

        // Vérifier qu'on est sur son île
        Island atLoc = manager.getIslandAtLocation(player.getLocation());
        if (atLoc == null || !atLoc.getOwner().equals(island.getOwner())) {
            player.sendMessage("§cTu dois être sur ton île pour activer le fly !"); return true;
        }

        if (player.getAllowFlight()) {
            player.setFlying(false);
            player.setAllowFlight(false);
            player.sendMessage("§c✈ Fly désactivé.");
        } else {
            player.setAllowFlight(true);
            player.setFlying(true);
            player.sendMessage("§a✈ Fly activé ! §7(sera retiré si tu quittes l'île)");
        }
        return true;
    }

    // ════════════════════════════════════════════════
    //  TRANSFER  — /is transfer <joueur>
    // ════════════════════════════════════════════════

    private boolean cmdTransfer(Player player, Island island, String[] args) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        if (!island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§cSeul le chef peut transférer l'île !"); return true;
        }
        if (args.length < 2) { player.sendMessage("§cUsage : §e/is transfer <joueur>"); return true; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage("§cJoueur introuvable (doit être en ligne)."); return true; }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cTu es déjà le chef !"); return true;
        }
        if (!island.isMember(target.getUniqueId())) {
            player.sendMessage("§e" + target.getName() + " §cdoit être membre de ton île."); return true;
        }

        // transferOwner recrée l'île + appelle replaceIsland (save atomique)
        Island newIsland = transferOwner(island, target.getUniqueId(), player.getUniqueId());

        player.sendMessage("§7Tu as transféré la direction de l'île à §e" + target.getName() + "§7.");
        target.sendMessage("§a§lTu es maintenant le chef de l'île ! ✨");

        // Notifier le reste de l'équipe
        for (UUID uid : newIsland.getAllMembers()) {
            Player m = Bukkit.getPlayer(uid);
            if (m != null && !m.equals(player) && !m.equals(target))
                m.sendMessage("§e" + target.getName() + " §7est le nouveau chef de l'île.");
        }
        return true;
    }

    /** Crée une nouvelle instance Island avec un nouvel owner, copiant tout l'état. */
    private Island transferOwner(Island old, UUID newOwner, UUID oldOwner) {
        // Nouvelle île avec newOwner
        Island neo = new Island(newOwner, old.getCenter());
        neo.setRadius(old.getRadius());
        neo.setIsLevel(old.getIsLevel());
        neo.setOpen(old.isOpen());
        neo.setName(old.getName());
        if (old.hasWarp())  neo.setWarpLocation(old.getWarpLocation());
        if (old.hasHome())  neo.setHomeLocation(old.getHomeLocation());

        // Membres — retirer newOwner (car il devient owner), ajouter oldOwner comme MANAGER
        for (Map.Entry<UUID, IslandRole> e : old.getMembers().entrySet()) {
            if (e.getKey().equals(newOwner)) continue; // skipped — devient owner
            neo.addMember(e.getKey(), e.getValue());
        }
        neo.addMember(oldOwner, IslandRole.MANAGER);

        // Coops, bannis, permissions, flags, upgrades, blocs, missions
        for (UUID c : old.getCoops()) neo.addCoop(c);
        for (UUID b : old.getBannedPlayers()) neo.banPlayer(b);
        for (Map.Entry<IslandPermission, IslandRole> e : old.getPermissions().entrySet())
            neo.setPermissionRole(e.getKey(), e.getValue());
        for (Map.Entry<String, Boolean> e : old.getFlags().entrySet())
            neo.getFlags().put(e.getKey(), e.getValue());
        for (Map.Entry<String, Integer> e : old.getUpgrades().entrySet())
            neo.getUpgrades().put(e.getKey(), e.getValue());
        for (Map.Entry<String, Integer> e : old.getValueBlockCounts().entrySet())
            neo.getValueBlockCounts().put(e.getKey(), e.getValue());
        neo.getMissionProgressMap().putAll(old.getMissionProgressMap());
        neo.getCompletedMissions().addAll(old.getCompletedMissions());

        // Supprimer l'ancienne île du manager et enregistrer la nouvelle
        // (on passe par réflexion via le manager)
        // Note : dans IslandManager, islands est une Map<UUID, Island>
        // On doit supprimer l'ancienne clé et ajouter la nouvelle
        // On expose une méthode replaceIsland pour ça (voir IslandManager)
        manager.replaceIsland(old.getOwner(), neo);
        return neo;
    }

    // ════════════════════════════════════════════════
    //  COOP  — /is coop <joueur>
    // ════════════════════════════════════════════════

    private boolean cmdCoop(Player player, Island island, String[] args) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        IslandRole role = island.getRole(player.getUniqueId());
        if (role != IslandRole.CHEF && role != IslandRole.MANAGER) {
            player.sendMessage("§cSeul le Chef ou Manager peut gérer les coops !"); return true;
        }
        if (args.length < 2) { player.sendMessage("§cUsage : §e/is coop <joueur>"); return true; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage("§cJoueur introuvable (doit être en ligne)."); return true; }
        if (island.isMember(target.getUniqueId())) {
            player.sendMessage("§e" + target.getName() + " §cest déjà membre !"); return true;
        }
        if (island.isCoop(target.getUniqueId())) {
            player.sendMessage("§e" + target.getName() + " §cest déjà coop !"); return true;
        }
        if (island.isBanned(target.getUniqueId())) {
            player.sendMessage("§e" + target.getName() + " §cest banni."); return true;
        }

        island.addCoop(target.getUniqueId());
        manager.saveIsland(island);
        player.sendMessage("§e" + target.getName() + " §aa obtenu un accès coop temporaire.");
        target.sendMessage("§a§lAccès coop accordé sur l'île de §e" +
                Bukkit.getOfflinePlayer(island.getOwner()).getName() + "§a !");
        return true;
    }

    // ════════════════════════════════════════════════
    //  UNCOOP  — /is uncoop <joueur>
    // ════════════════════════════════════════════════

    private boolean cmdUncoop(Player player, Island island, String[] args) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        IslandRole role = island.getRole(player.getUniqueId());
        if (role != IslandRole.CHEF && role != IslandRole.MANAGER) {
            player.sendMessage("§cSeul le Chef ou Manager peut gérer les coops !"); return true;
        }
        if (args.length < 2) { player.sendMessage("§cUsage : §e/is uncoop <joueur>"); return true; }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!island.isCoop(target.getUniqueId())) {
            player.sendMessage("§e" + args[1] + " §cn'est pas coop sur ton île."); return true;
        }
        island.removeCoop(target.getUniqueId());
        manager.saveIsland(island);
        player.sendMessage("§7Accès coop retiré à §e" + args[1] + "§7.");
        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online != null) online.sendMessage("§cTon accès coop sur cette île a été retiré.");
        return true;
    }

    // ════════════════════════════════════════════════
    //  VISIT  — /is visit <joueur>
    // ════════════════════════════════════════════════

    private boolean cmdVisit(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage("§cUsage : §e/is visit <joueur>"); return true; }

        // Chercher par pseudo ou nom d'île
        OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
        Island target = manager.getIsland(op.getUniqueId());
        if (target == null) target = manager.getIslandByMember(op.getUniqueId());
        if (target == null) target = manager.getIslandByName(args[1]);

        if (target == null) { player.sendMessage("§cÎle introuvable pour §e" + args[1] + "§c."); return true; }
        if (target.isBanned(player.getUniqueId())) { player.sendMessage("§cTu es banni de cette île."); return true; }
        if (!target.isOpen() && !target.isMember(player.getUniqueId()) && !target.isCoop(player.getUniqueId())) {
            player.sendMessage("§cCette île est fermée aux visiteurs."); return true;
        }
        if (!target.hasWarp()) { player.sendMessage("§cCette île n'a pas de point de visite."); return true; }

        player.teleport(target.getWarpLocation());
        player.sendMessage("§aTéléporté sur l'île de §e" +
                Bukkit.getOfflinePlayer(target.getOwner()).getName() + "§a !");
        return true;
    }

    // ════════════════════════════════════════════════
    //  EXPEL  — /is expel <joueur>  (kick visiteur)
    // ════════════════════════════════════════════════

    private boolean cmdExpel(Player player, Island island, String[] args) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        IslandRole role = island.getRole(player.getUniqueId());
        if (role != IslandRole.CHEF && role != IslandRole.MANAGER) {
            player.sendMessage("§cSeul le Chef ou Manager peut expulser !"); return true;
        }
        if (args.length < 2) { player.sendMessage("§cUsage : §e/is expel <joueur>"); return true; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage("§cJoueur introuvable."); return true; }
        if (island.isMember(target.getUniqueId())) {
            player.sendMessage("§cUtilise §e/is kick §cpour les membres."); return true;
        }
        Island atLoc = manager.getIslandAtLocation(target.getLocation());
        if (atLoc == null || !atLoc.getOwner().equals(island.getOwner())) {
            player.sendMessage("§eCe joueur n'est pas sur ton île."); return true;
        }

        island.removeCoop(target.getUniqueId());
        target.teleport(Bukkit.getWorld("world").getSpawnLocation());
        target.setWorldBorder(null);
        target.sendMessage("§cTu as été expulsé de l'île de §e" + player.getName() + "§c.");
        player.sendMessage("§e" + target.getName() + " §aexpulsé.");
        return true;
    }

    // ════════════════════════════════════════════════
    //  UPGRADE  — /is upgrade
    // ════════════════════════════════════════════════

    private boolean cmdUpgrade(Player player, Island island) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        if (!island.hasPermission(player.getUniqueId(), IslandPermission.UPGRADE)) {
            player.sendMessage("§cTu n'as pas la permission d'améliorer l'île !"); return true;
        }
        player.openInventory(IslandUpgradeGUI.create(island));
        return true;
    }

    // ════════════════════════════════════════════════
    //  SETTINGS  — /is settings
    // ════════════════════════════════════════════════

    private boolean cmdSettings(Player player, Island island) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        if (!island.hasPermission(player.getUniqueId(), IslandPermission.SETTINGS)) {
            player.sendMessage("§cTu n'as pas la permission de modifier les paramètres !"); return true;
        }
        player.openInventory(IslandSettingsGUI.create(island));
        return true;
    }

    // ════════════════════════════════════════════════
    //  BANLIST  — /is banlist
    // ════════════════════════════════════════════════

    private boolean cmdBanlist(Player player, Island island) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        if (!island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§cSeul le chef peut voir la liste des bannis."); return true;
        }
        Set<UUID> banned = island.getBannedPlayers();
        if (banned.isEmpty()) { player.sendMessage("§7Aucun joueur banni."); return true; }
        player.sendMessage("§c§l=== Bannis (" + banned.size() + ") ===");
        for (UUID uid : banned) {
            String name = Bukkit.getOfflinePlayer(uid).getName();
            player.sendMessage("§c- §e" + (name != null ? name : uid.toString()));
        }
        return true;
    }

    // ════════════════════════════════════════════════
    //  COMMANDES EXISTANTES (inchangées, refactorisées)
    // ════════════════════════════════════════════════

    private boolean cmdDelete(Player player, Island island) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        if (!island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§cSeul le chef peut supprimer !"); return true;
        }
        for (UUID mid : island.getAllMembers()) {
            Player m = Bukkit.getPlayer(mid);
            if (m != null) {
                m.teleport(Bukkit.getWorld("world").getSpawnLocation());
                m.setWorldBorder(null);
                if (!mid.equals(player.getUniqueId())) m.sendMessage("§cL'île a été supprimée.");
            }
        }
        player.sendMessage("§cSuppression en cours...");
        manager.deleteIsland(player.getUniqueId());
        return true;
    }

    private boolean cmdBorder(Player player, Island island) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        if (player.getWorldBorder() != null) {
            player.setWorldBorder(null);
            player.sendMessage("§cBorder désactivée.");
        } else {
            applyBorder(player, island);
            player.sendMessage("§aBorder activée.");
        }
        return true;
    }

    private boolean cmdPermissions(Player player, Island island) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        if (!island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§cSeul le chef peut modifier les permissions !"); return true;
        }
        player.openInventory(PermissionsGUI.create(island));
        return true;
    }

    private boolean cmdInvite(Player player, Island island, String[] args) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        IslandRole role = island.getRole(player.getUniqueId());
        if (role != IslandRole.CHEF && role != IslandRole.MANAGER) {
            player.sendMessage("§cSeul le Chef ou Manager peut inviter !"); return true;
        }
        if (args.length < 2) { player.sendMessage("§cUsage : §e/is invite <joueur>"); return true; }

        // Vérifier limite membres
        if (island.getMemberCount() >= island.getMemberLimit()) {
            player.sendMessage("§cLimite de membres atteinte ! Upgrade §e/is upgrade §cpour augmenter.");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage("§cJoueur introuvable."); return true; }
        if (island.isMember(target.getUniqueId())) {
            player.sendMessage("§c" + target.getName() + " est déjà membre !"); return true;
        }
        if (island.isBanned(target.getUniqueId())) {
            player.sendMessage("§c" + target.getName() + " est banni !"); return true;
        }
        manager.addInvitation(target.getUniqueId(), island.getOwner());
        player.sendMessage("§aInvitation envoyée à §e" + target.getName() + "§a !");
        target.sendMessage("§6§lArcanium §r§7» §e" + player.getName()
                + " §7t'invite. §e/is join " + player.getName() + " §7pour accepter.");
        return true;
    }

    private boolean cmdJoin(Player player, Island island, String[] args) {
        if (manager.getIslandByMember(player.getUniqueId()) != null) {
            player.sendMessage("§cTu es déjà sur une île !"); return true;
        }
        if (args.length < 2) { player.sendMessage("§cUsage : §e/is join <chef>"); return true; }
        Player owner = Bukkit.getPlayer(args[1]);
        if (owner == null) { player.sendMessage("§cJoueur introuvable."); return true; }
        UUID invitedBy = manager.getInvitation(player.getUniqueId());
        if (invitedBy == null || !invitedBy.equals(owner.getUniqueId())) {
            player.sendMessage("§cPas d'invitation de §e" + owner.getName() + "§c."); return true;
        }
        Island targetIsland = manager.getIsland(owner.getUniqueId());
        if (targetIsland == null) { player.sendMessage("§cCette île n'existe plus."); return true; }
        if (targetIsland.getMemberCount() >= targetIsland.getMemberLimit()) {
            player.sendMessage("§cL'île est pleine !"); return true;
        }
        targetIsland.addMember(player.getUniqueId(), IslandRole.MEMBRE);
        manager.removeInvitation(player.getUniqueId());
        manager.saveIsland(targetIsland);
        Location dest = targetIsland.hasHome() ? targetIsland.getHomeLocation()
                : (targetIsland.hasWarp() ? targetIsland.getWarpLocation()
                : findSafeSpawn(targetIsland.getCenter()));
        player.teleport(dest);
        applyBorder(player, targetIsland);
        player.sendMessage("§aTu as rejoint l'île de §e" + owner.getName() + "§a !");
        owner.sendMessage("§e" + player.getName() + " §aa rejoint ton île !");
        return true;
    }

    private boolean cmdLeave(Player player, Island island) {
        if (island == null) { player.sendMessage("§cTu n'es sur aucune île !"); return true; }
        if (island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§cTu es le chef ! Utilise §e/is delete§c."); return true;
        }
        island.removeMember(player.getUniqueId());
        island.removeCoop(player.getUniqueId());
        manager.saveIsland(island);
        player.teleport(Bukkit.getWorld("world").getSpawnLocation());
        player.setWorldBorder(null);
        player.sendMessage("§7Tu as quitté l'île.");
        Player chef = Bukkit.getPlayer(island.getOwner());
        if (chef != null) chef.sendMessage("§e" + player.getName() + " §7a quitté ton île.");
        return true;
    }

    private boolean cmdPromote(Player player, Island island, String[] args) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        if (!island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§cSeul le chef peut promouvoir !"); return true;
        }
        if (args.length < 2) { player.sendMessage("§cUsage : §e/is promote <joueur>"); return true; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !island.isMember(target.getUniqueId())
                || island.getOwner().equals(target.getUniqueId())) {
            player.sendMessage("§cJoueur introuvable ou non membre."); return true;
        }
        IslandRole promoted = promoteRole(island.getRole(target.getUniqueId()));
        if (promoted == null) { player.sendMessage("§cDéjà au rang maximum."); return true; }
        island.addMember(target.getUniqueId(), promoted);
        manager.saveIsland(island);
        player.sendMessage("§e" + target.getName() + " §apromû §6" + promoted.name() + "§a !");
        target.sendMessage("§aTu es maintenant §6" + promoted.name() + " §asur l'île !");
        return true;
    }

    private boolean cmdDemote(Player player, Island island, String[] args) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        if (!island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§cSeul le chef peut rétrograder !"); return true;
        }
        if (args.length < 2) { player.sendMessage("§cUsage : §e/is demote <joueur>"); return true; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !island.isMember(target.getUniqueId())
                || island.getOwner().equals(target.getUniqueId())) {
            player.sendMessage("§cJoueur introuvable ou non membre."); return true;
        }
        IslandRole demoted = demoteRole(island.getRole(target.getUniqueId()));
        if (demoted == null) {
            island.removeMember(target.getUniqueId());
            manager.saveIsland(island);
            player.sendMessage("§e" + target.getName() + " §ca été expulsé.");
            target.sendMessage("§cTu as été expulsé de l'île.");
            if (target.getWorld().getName().equals("skyblock")) {
                target.teleport(Bukkit.getWorld("world").getSpawnLocation());
                target.setWorldBorder(null);
            }
            return true;
        }
        island.addMember(target.getUniqueId(), demoted);
        manager.saveIsland(island);
        player.sendMessage("§e" + target.getName() + " §7rétrogradé §6" + demoted.name() + "§7.");
        target.sendMessage("§7Tu es maintenant §6" + demoted.name() + "§7.");
        return true;
    }

    private boolean cmdKick(Player player, Island island, String[] args) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        IslandRole role = island.getRole(player.getUniqueId());
        if (role != IslandRole.CHEF && role != IslandRole.MANAGER) {
            player.sendMessage("§cSeul le Chef ou Manager peut kick !"); return true;
        }
        if (args.length < 2) { player.sendMessage("§cUsage : §e/is kick <joueur>"); return true; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage("§cJoueur introuvable."); return true; }
        if (!island.isMember(target.getUniqueId())) {
            player.sendMessage("§cCe joueur n'est pas membre de ton île."); return true;
        }
        island.removeMember(target.getUniqueId());
        manager.saveIsland(island);
        target.teleport(Bukkit.getWorld("world").getSpawnLocation());
        target.setWorldBorder(null);
        target.sendMessage("§cTu as été expulsé de l'île de §e" + player.getName() + "§c.");
        player.sendMessage("§e" + target.getName() + " §aexpulsé de l'île.");
        return true;
    }

    private boolean cmdBan(Player player, Island island, String[] args) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        if (!island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§cSeul le chef peut bannir !"); return true;
        }
        if (args.length < 2) { player.sendMessage("§cUsage : §e/is ban <joueur>"); return true; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cTu ne peux pas te bannir !"); return true;
        }
        island.banPlayer(target.getUniqueId());
        island.removeMember(target.getUniqueId());
        island.removeCoop(target.getUniqueId());
        manager.saveIsland(island);
        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online != null) {
            Island onIsland = manager.getIslandAtLocation(online.getLocation());
            if (onIsland != null && onIsland.getOwner().equals(island.getOwner())) {
                online.teleport(Bukkit.getWorld("world").getSpawnLocation());
                online.setWorldBorder(null);
                online.sendMessage("§cTu as été banni de l'île de §e" + player.getName() + "§c.");
            }
        }
        player.sendMessage("§e" + args[1] + " §cbanni.");
        return true;
    }

    private boolean cmdUnban(Player player, Island island, String[] args) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        if (!island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§cSeul le chef peut débannir !"); return true;
        }
        if (args.length < 2) { player.sendMessage("§cUsage : §e/is unban <joueur>"); return true; }
        island.unbanPlayer(Bukkit.getOfflinePlayer(args[1]).getUniqueId());
        manager.saveIsland(island);
        player.sendMessage("§e" + args[1] + " §adébanni.");
        return true;
    }

    private boolean cmdOpen(Player player, Island island) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        if (!island.hasPermission(player.getUniqueId(), IslandPermission.OPEN_CLOSE)) {
            player.sendMessage("§cTu n'as pas la permission !"); return true;
        }
        if (!island.hasWarp()) {
            player.sendMessage("§cDéfinis d'abord un point : §e/is sethome §cou §e/is warp create"); return true;
        }
        island.setOpen(true);
        manager.saveIsland(island);
        player.sendMessage("§aÎle ouverte aux visiteurs !");
        return true;
    }

    private boolean cmdClose(Player player, Island island) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        if (!island.hasPermission(player.getUniqueId(), IslandPermission.OPEN_CLOSE)) {
            player.sendMessage("§cTu n'as pas la permission !"); return true;
        }
        island.setOpen(false);
        manager.saveIsland(island);
        player.sendMessage("§cÎle fermée.");
        return true;
    }

    private boolean cmdWarp(Player player, Island island, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage : §e/is warp create §cou §e/is warp <joueur>"); return true;
        }
        if (args[1].equalsIgnoreCase("create")) {
            if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
            if (!island.hasPermission(player.getUniqueId(), IslandPermission.CREATE_WARP)) {
                player.sendMessage("§cTu n'as pas la permission !"); return true;
            }
            Island atLoc = manager.getIslandAtLocation(player.getLocation());
            if (atLoc == null || !atLoc.getOwner().equals(island.getOwner())) {
                player.sendMessage("§cTu dois être sur ton île !"); return true;
            }
            island.setWarpLocation(player.getLocation());
            manager.saveIsland(island);
            player.sendMessage("§a§lWarp créé ! §r§e/is open §7pour ouvrir l'île.");
            return true;
        }
        // Visiter
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage("§cJoueur introuvable."); return true; }
        Island targetIsland = manager.getIsland(target.getUniqueId());
        if (targetIsland == null) { player.sendMessage("§c" + target.getName() + " n'a pas d'île."); return true; }
        if (!targetIsland.isOpen()) { player.sendMessage("§cL'île est fermée."); return true; }
        if (!targetIsland.hasWarp()) { player.sendMessage("§cPas de warp défini."); return true; }
        if (targetIsland.isBanned(player.getUniqueId())) { player.sendMessage("§cTu es banni."); return true; }
        player.teleport(targetIsland.getWarpLocation());
        player.sendMessage("§aTéléporté sur l'île de §e" + target.getName() + "§a !");
        return true;
    }

    private boolean cmdBlock(Player player, Island island) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        player.openInventory(IslandBlockGUI.create(island));
        return true;
    }

    private boolean cmdRecalc(Player player, Island island) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        if (!island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§cSeul le chef peut recalculer !"); return true;
        }
        double before = island.getIsLevel();
        island.recalculateLevel();
        manager.saveIsland(island);
        player.sendMessage("§b✦ Recalcul : §7" + String.format("%.1f", before)
                + " §7→ §e" + String.format("%.1f", island.getIsLevel()));
        return true;
    }

    private boolean cmdTop(Player player) {
        List<Island> sorted = manager.getAllIslands().stream()
                .sorted((a, b) -> Double.compare(b.getIsLevel(), a.getIsLevel()))
                .limit(10).toList();
        player.sendMessage("§b§l╔══ Top 10 Îles ══╗");
        for (int i = 0; i < sorted.size(); i++) {
            Island top = sorted.get(i);
            String nom = top.hasName() ? top.getName()
                    : "§7L'île de §e" + Bukkit.getOfflinePlayer(top.getOwner()).getName();
            String med = switch (i) {
                case 0 -> "§6#1"; case 1 -> "§7#2"; case 2 -> "§c#3"; default -> "§f#" + (i+1);
            };
            player.sendMessage(med + " " + nom + " §7— IS Level §b" + String.format("%.1f", top.getIsLevel()));
        }
        player.sendMessage("§b§l╚════════════════╝");
        return true;
    }

    private boolean cmdMissions(Player player, Island island) {
        if (island == null) { player.sendMessage("§cTu n'as pas d'île !"); return true; }
        player.openInventory(be.RedSwick.skyblock.gui.MissionGUI.createCategories(island));
        return true;
    }

    // ════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════

    private Location findSafeSpawn(Location center) {
        World w = center.getWorld();
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        for (int dy = 5; dy >= -5; dy--) {
            int y = cy + dy;
            if (y < 1 || y >= w.getMaxHeight() - 1) continue;
            if (w.getBlockAt(cx, y, cz).getType().isSolid()
                    && !w.getBlockAt(cx, y+1, cz).getType().isSolid()
                    && !w.getBlockAt(cx, y+2, cz).getType().isSolid()) {
                return new Location(w, cx + 0.5, y + 1, cz + 0.5);
            }
        }
        return new Location(w, cx + 0.5, cy + 1, cz + 0.5);
    }

    private void applyBorder(Player player, Island island) {
        WorldBorder border = Bukkit.createWorldBorder();
        border.setCenter(island.getCenter());
        border.setSize(island.getUpgradeValue(IslandUpgrade.SIZE) * 2);
        border.setWarningDistance(0);
        player.setWorldBorder(border);
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§l=== Commandes Île (§e/is§6§l) ===");
        player.sendMessage("§e/is create §7• Créer ton île");
        player.sendMessage("§e/is go §7• Aller sur ton île");
        player.sendMessage("§e/is name <nom> §7• Renommer ton île");
        player.sendMessage("§e/is sethome §7• Définir le point d'accueil");
        player.sendMessage("§e/is info [joueur] §7• Infos sur une île");
        player.sendMessage("§e/is team §7• Voir les membres");
        player.sendMessage("§e/is tc [msg] §7• TeamChat");
        player.sendMessage("§e/is fly §7• Vol sur ton île §5(Arcanium+)");
        player.sendMessage("§e/is invite/join/leave §7• Membres");
        player.sendMessage("§e/is promote/demote/kick/ban §7• Gestion");
        player.sendMessage("§e/is coop/uncoop <joueur> §7• Accès temporaire");
        player.sendMessage("§e/is visit <joueur> §7• Visiter une île");
        player.sendMessage("§e/is transfer <joueur> §7• Transférer le chef");
        player.sendMessage("§e/is upgrade §7• Améliorer l'île");
        player.sendMessage("§e/is settings §7• Paramètres (flags)");
        player.sendMessage("§e/is open §8| §e/is close §7• Ouvrir/Fermer");
        player.sendMessage("§e/is warp create §8| §e/is warp <joueur>");
        player.sendMessage("§e/is block §8| §e/is recalc §8| §e/is top");
        player.sendMessage("§e/is permissions §8| §e/is missions §8| §e/is banlist");
    }

    private IslandRole promoteRole(IslandRole role) {
        return switch (role) { case COOP -> IslandRole.MEMBRE; case MEMBRE -> IslandRole.MANAGER; default -> null; };
    }

    private IslandRole demoteRole(IslandRole role) {
        return switch (role) { case MANAGER -> IslandRole.MEMBRE; case MEMBRE -> IslandRole.COOP; default -> null; };
    }

    // ════════════════════════════════════════════════
    //  TAB COMPLETE
    // ════════════════════════════════════════════════

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return null;

        // ── args[0] : sous-commande ─────────────────────────────────────
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return SUB_COMMANDS.stream()
                    .filter(s -> s.startsWith(input))
                    .sorted()
                    .toList();
        }

        // ── args[1] ──────────────────────────────────────────────────────
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String in  = args[1].toLowerCase();

            // Commandes nécessitant un nom de joueur
            if (List.of("invite","kick","ban","unban","promote","demote",
                    "join","coop","uncoop","expel","transfer","visit","info","team").contains(sub)) {
                return onlinePlayers(in);
            }
            // /is warp create | <joueur>
            if (sub.equals("warp")) {
                List<String> opts = new ArrayList<>();
                if ("create".startsWith(in)) opts.add("create");
                onlinePlayers(in).forEach(opts::add);
                return opts;
            }
            // /is name reset | <nom>
            if (sub.equals("name")) {
                return "reset".startsWith(in) ? List.of("reset") : List.of();
            }
            // /is home <nom>  (les homes perso ne sont pas stockés, on propose les îles par nom)
            if (sub.equals("home")) return List.of();

            // /is teamchat / tc → texte libre, pas de suggestions
            if (sub.equals("teamchat") || sub.equals("tc")) return List.of();
        }

        // ── args[2] ──────────────────────────────────────────────────────
        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            // /is team add|remove|list <joueur>
            if (sub.equals("team")) {
                String action = args[1].toLowerCase();
                String in     = args[2].toLowerCase();
                if (action.equals("add") || action.equals("remove")) return onlinePlayers(in);
            }
        }

        return List.of();
    }

    private List<String> onlinePlayers(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(prefix))
                .sorted()
                .toList();
    }
}