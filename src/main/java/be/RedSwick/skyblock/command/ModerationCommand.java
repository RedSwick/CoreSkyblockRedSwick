package be.RedSwick.skyblock.command;

import be.RedSwick.skyblock.moderation.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ModerationCommand {

    // ════════════════════════════════════════════════
    //  /mute <joueur> <minutes> [raison]
    //  GUIDE+   — ne peut pas muter un grade >= soi
    // ════════════════════════════════════════════════
    public static class Mute implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (!(s instanceof Player staff)) { s.sendMessage("§cCommande joueur uniquement."); return true; }
            StaffRank staffRank = StaffRank.of(staff);
            if (!staffRank.isAtLeast(StaffRank.GUIDE)) { s.sendMessage("§cPas la permission."); return true; }
            if (a.length < 2) { s.sendMessage("§cUsage : /mute <joueur> <minutes> [raison]"); return true; }

            Player target = Bukkit.getPlayer(a[0]);
            if (target == null) { s.sendMessage("§cJoueur introuvable."); return true; }
            if (!staffRank.canModerate(target)) {
                s.sendMessage("§cTu ne peux pas muter §e" + target.getName()
                        + " §c(" + StaffRank.of(target).getDisplay() + "§c).");
                return true;
            }
            long minutes;
            try { minutes = Long.parseLong(a[1]); } catch (Exception e) { s.sendMessage("§cDurée invalide."); return true; }
            String reason = a.length > 2 ? join(a, 2) : "Aucune raison";

            MuteManager.get().mute(target.getUniqueId(), minutes * 60_000, reason);
            target.sendMessage("§cTu as été mute §e" + minutes + " min §cpour : §f" + reason);
            broadcast("§8[§cMOD§8] §e" + target.getName() + " §7mute §e" + minutes + "min §7— " + reason, staff, staffRank);
            StaffLogManager.get().log(staff.getName(), "MUTE " + minutes + "min", target.getName(), reason);
            return true;
        }
    }

    // ════════════════════════════════════════════════
    //  /unmute <joueur>
    // ════════════════════════════════════════════════
    public static class Unmute implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (!(s instanceof Player staff)) { s.sendMessage("§cCommande joueur uniquement."); return true; }
            StaffRank staffRank = StaffRank.of(staff);
            if (!staffRank.isAtLeast(StaffRank.GUIDE)) { s.sendMessage("§cPas la permission."); return true; }
            if (a.length < 1) { s.sendMessage("§cUsage : /unmute <joueur>"); return true; }

            Player target = Bukkit.getPlayer(a[0]);
            if (target == null) { s.sendMessage("§cJoueur introuvable (doit être connecté)."); return true; }
            if (!staffRank.canModerate(target)) {
                s.sendMessage("§cTu ne peux pas unmuter §e" + target.getName() + "§c."); return true;
            }
            if (!MuteManager.get().isMuted(target.getUniqueId())) {
                s.sendMessage("§cCe joueur n'est pas mute."); return true;
            }
            MuteManager.get().unmute(target.getUniqueId());
            target.sendMessage("§aTon mute a été retiré.");
            broadcast("§8[§cMOD§8] §e" + target.getName() + " §7unmute.", staff, staffRank);
            StaffLogManager.get().log(staff.getName(), "UNMUTE", target.getName(), "");
            return true;
        }
    }

    // ════════════════════════════════════════════════
    //  /kick <joueur> [raison]   — MODERATEUR+
    // ════════════════════════════════════════════════
    public static class Kick implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (!(s instanceof Player staff)) { s.sendMessage("§cCommande joueur uniquement."); return true; }
            StaffRank staffRank = StaffRank.of(staff);
            if (!staffRank.isAtLeast(StaffRank.MODERATEUR)) { s.sendMessage("§cPas la permission."); return true; }
            if (a.length < 1) { s.sendMessage("§cUsage : /kick <joueur> [raison]"); return true; }

            Player target = Bukkit.getPlayer(a[0]);
            if (target == null) { s.sendMessage("§cJoueur introuvable."); return true; }
            if (!staffRank.canModerate(target)) {
                s.sendMessage("§cTu ne peux pas kick §e" + target.getName() + "§c."); return true;
            }
            String reason = a.length > 1 ? join(a, 1) : "Aucune raison";
            target.kickPlayer("§cTu as été expulsé.\n§7Raison : §f" + reason);
            broadcast("§8[§cMOD§8] §e" + target.getName() + " §7kick — " + reason, staff, staffRank);
            StaffLogManager.get().log(staff.getName(), "KICK", target.getName(), reason);
            return true;
        }
    }

    // ════════════════════════════════════════════════
    //  /ban <joueur> [raison]   — MODERATEUR+
    // ════════════════════════════════════════════════
    public static class Ban implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (!(s instanceof Player staff)) { s.sendMessage("§cCommande joueur uniquement."); return true; }
            StaffRank staffRank = StaffRank.of(staff);
            if (!staffRank.isAtLeast(StaffRank.MODERATEUR)) { s.sendMessage("§cPas la permission."); return true; }
            if (a.length < 1) { s.sendMessage("§cUsage : /ban <joueur> [raison]"); return true; }

            // Vérifie le grade même si offline
            Player target = Bukkit.getPlayer(a[0]);
            if (target != null && !staffRank.canModerate(target)) {
                s.sendMessage("§cTu ne peux pas ban §e" + a[0] + "§c."); return true;
            }
            String reason = a.length > 1 ? join(a, 1) : "Aucune raison";
            TempBanManager.get().ban(a[0], reason, staff.getName());
            if (target != null) target.kickPlayer("§cTu as été banni.\n§7Raison : §f" + reason);
            broadcast("§8[§cMOD§8] §e" + a[0] + " §7banni — " + reason, staff, staffRank);
            StaffLogManager.get().log(staff.getName(), "BAN", a[0], reason);
            return true;
        }
    }

    // ════════════════════════════════════════════════
    //  /unban <joueur>   — MODERATEUR+
    // ════════════════════════════════════════════════
    public static class Unban implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (!(s instanceof Player staff)) { s.sendMessage("§cCommande joueur uniquement."); return true; }
            StaffRank staffRank = StaffRank.of(staff);
            if (!staffRank.isAtLeast(StaffRank.MODERATEUR)) { s.sendMessage("§cPas la permission."); return true; }
            if (a.length < 1) { s.sendMessage("§cUsage : /unban <joueur>"); return true; }
            if (!TempBanManager.get().isBanned(a[0])) { s.sendMessage("§cCe joueur n'est pas banni."); return true; }
            TempBanManager.get().unban(a[0]);
            broadcast("§8[§cMOD§8] §e" + a[0] + " §7débanni.", staff, staffRank);
            StaffLogManager.get().log(staff.getName(), "UNBAN", a[0], "");
            return true;
        }
    }

    // ════════════════════════════════════════════════
    //  /tempban <joueur> <jours> [raison]   — MODERATEUR+
    // ════════════════════════════════════════════════
    public static class TempBan implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (!(s instanceof Player staff)) { s.sendMessage("§cCommande joueur uniquement."); return true; }
            StaffRank staffRank = StaffRank.of(staff);
            if (!staffRank.isAtLeast(StaffRank.MODERATEUR)) { s.sendMessage("§cPas la permission."); return true; }
            if (a.length < 2) { s.sendMessage("§cUsage : /tempban <joueur> <jours> [raison]"); return true; }

            Player target = Bukkit.getPlayer(a[0]);
            if (target != null && !staffRank.canModerate(target)) {
                s.sendMessage("§cTu ne peux pas tempban §e" + a[0] + "§c."); return true;
            }
            long days;
            try { days = Long.parseLong(a[1]); } catch (Exception e) { s.sendMessage("§cDurée invalide."); return true; }
            String reason = a.length > 2 ? join(a, 2) : "Aucune raison";
            String dur = TempBanManager.formatDuration(days);
            TempBanManager.get().tempBan(a[0], days * 86_400_000L, reason, staff.getName());
            if (target != null) target.kickPlayer("§cTu as été banni temporairement.\n§7Durée : §e" + dur + "\n§7Raison : §f" + reason);
            broadcast("§8[§cMOD§8] §e" + a[0] + " §7tempban §e" + dur + " §7— " + reason, staff, staffRank);
            StaffLogManager.get().log(staff.getName(), "TEMPBAN " + dur, a[0], reason);
            return true;
        }
    }

    // ════════════════════════════════════════════════
    //  /warn <joueur> <raison>   — ADMIN+
    // ════════════════════════════════════════════════
    public static class Warn implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (!(s instanceof Player staff)) { s.sendMessage("§cCommande joueur uniquement."); return true; }
            StaffRank staffRank = StaffRank.of(staff);
            if (!staffRank.isAtLeast(StaffRank.ADMIN)) { s.sendMessage("§cPas la permission."); return true; }
            if (a.length < 2) { s.sendMessage("§cUsage : /warn <joueur> <raison>"); return true; }

            Player target = Bukkit.getPlayer(a[0]);
            if (target == null) { s.sendMessage("§cJoueur introuvable."); return true; }
            if (!staffRank.canModerate(target)) {
                s.sendMessage("§cTu ne peux pas warn §e" + target.getName() + "§c."); return true;
            }
            String reason = join(a, 1);
            WarnManager.get().warn(target.getUniqueId(), reason, staff.getName());
            int count = WarnManager.get().getWarnCount(target.getUniqueId());
            target.sendMessage("§c§lAVERTISSEMENT §r§7(#" + count + ") §cpour : §f" + reason);
            broadcast("§8[§cMOD§8] §e" + target.getName() + " §7warn #" + count + " — " + reason, staff, staffRank);
            StaffLogManager.get().log(staff.getName(), "WARN #" + count, target.getName(), reason);
            return true;
        }
    }

    // ════════════════════════════════════════════════
    //  /warns <joueur>   — ADMIN+
    // ════════════════════════════════════════════════
    public static class Warns implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (!(s instanceof Player staff)) { s.sendMessage("§cCommande joueur uniquement."); return true; }
            if (!StaffRank.of(staff).isAtLeast(StaffRank.ADMIN)) { s.sendMessage("§cPas la permission."); return true; }
            if (a.length < 1) { s.sendMessage("§cUsage : /warns <joueur>"); return true; }
            Player target = Bukkit.getPlayer(a[0]);
            if (target == null) { s.sendMessage("§cJoueur introuvable."); return true; }
            var list = WarnManager.get().getWarns(target.getUniqueId());
            s.sendMessage("§8▬▬ §eAvertissements de §f" + target.getName() + " §8(§c" + list.size() + "§8) ▬▬");
            if (list.isEmpty()) { s.sendMessage("§7Aucun avertissement."); return true; }
            for (int i = 0; i < list.size(); i++)
                s.sendMessage("§8#" + (i+1) + " §7par §f" + list.get(i).by() + " §8— §f" + list.get(i).reason());
            return true;
        }
    }

    // ════════════════════════════════════════════════
    //  /vanish   — ADMIN+
    // ════════════════════════════════════════════════
    public static class Vanish implements CommandExecutor {
        public static final Set<UUID> vanished = new HashSet<>();

        @Override
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (!(s instanceof Player p)) { s.sendMessage("§cCommande joueur uniquement."); return true; }
            if (!StaffRank.of(p).isAtLeast(StaffRank.ADMIN)) { s.sendMessage("§cPas la permission."); return true; }

            var plugin = be.RedSwick.skyblock.SkyBlockPlugin.getInstance();
            if (vanished.contains(p.getUniqueId())) {
                vanished.remove(p.getUniqueId());
                for (Player o : Bukkit.getOnlinePlayers()) o.showPlayer(plugin, p);
                p.sendMessage("§aVanish désactivé.");
                StaffLogManager.get().log(p.getName(), "VANISH OFF", "-", "");
            } else {
                vanished.add(p.getUniqueId());
                for (Player o : Bukkit.getOnlinePlayers()) {
                    if (!vanished.contains(o.getUniqueId())) o.hidePlayer(plugin, p);
                }
                p.sendMessage("§eVanish activé.");
                StaffLogManager.get().log(p.getName(), "VANISH ON", "-", "");
            }
            return true;
        }

        public static boolean isVanished(UUID uuid) { return vanished.contains(uuid); }
    }

    // ════════════════════════════════════════════════
    //  /tp <joueur>   — MODERATEUR+  (bypass is close)
    // ════════════════════════════════════════════════
    public static class Tp implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (!(s instanceof Player staff)) { s.sendMessage("§cCommande joueur uniquement."); return true; }
            if (!StaffRank.of(staff).isAtLeast(StaffRank.MODERATEUR)) { s.sendMessage("§cPas la permission."); return true; }
            if (a.length < 1) { s.sendMessage("§cUsage : /tp <joueur>"); return true; }
            Player target = Bukkit.getPlayer(a[0]);
            if (target == null) { s.sendMessage("§cJoueur introuvable."); return true; }
            staff.teleport(target.getLocation());
            staff.sendMessage("§aTéléporté sur §e" + target.getName());
            StaffLogManager.get().log(staff.getName(), "TP", target.getName(), "");
            return true;
        }
    }

    // ════════════════════════════════════════════════
    //  /tphere <joueur>   — MODERATEUR+
    // ════════════════════════════════════════════════
    public static class TpHere implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (!(s instanceof Player staff)) { s.sendMessage("§cCommande joueur uniquement."); return true; }
            if (!StaffRank.of(staff).isAtLeast(StaffRank.MODERATEUR)) { s.sendMessage("§cPas la permission."); return true; }
            if (a.length < 1) { s.sendMessage("§cUsage : /tphere <joueur>"); return true; }
            Player target = Bukkit.getPlayer(a[0]);
            if (target == null) { s.sendMessage("§cJoueur introuvable."); return true; }
            target.teleport(staff.getLocation());
            staff.sendMessage("§e" + target.getName() + " §atéléporté à toi.");
            target.sendMessage("§7Tu as été téléporté par §e" + staff.getName());
            StaffLogManager.get().log(staff.getName(), "TPHERE", target.getName(), "");
            return true;
        }
    }

    // ════════════════════════════════════════════════
    //  /tpisland <joueur>   — MODERATEUR+  (bypass is close)
    // ════════════════════════════════════════════════
    public static class TpIsland implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (!(s instanceof Player staff)) { s.sendMessage("§cCommande joueur uniquement."); return true; }
            if (!StaffRank.of(staff).isAtLeast(StaffRank.MODERATEUR)) { s.sendMessage("§cPas la permission."); return true; }
            if (a.length < 1) { s.sendMessage("§cUsage : /tpisland <joueur>"); return true; }
            Player target = Bukkit.getPlayer(a[0]);
            if (target == null) { s.sendMessage("§cJoueur introuvable."); return true; }
            var island = be.RedSwick.skyblock.SkyBlockPlugin.getInstance()
                    .getIslandManager().getIslandByMember(target.getUniqueId());
            if (island == null) { s.sendMessage("§cCe joueur n'a pas d'île."); return true; }
            // Bypass is close — tp direct sans vérification
            staff.teleport(island.getCenter());
            staff.sendMessage("§aTéléporté sur l'île de §e" + target.getName() + " §8(bypass close)");
            StaffLogManager.get().log(staff.getName(), "TPISLAND", target.getName(), "bypass close");
            return true;
        }
    }

    // ════════════════════════════════════════════════
    //  /serverclose et /serveropen   — FONDATEUR uniquement
    // ════════════════════════════════════════════════
    public static class ServerClose implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (!(s instanceof Player p) || !StaffRank.of(p).isAtLeast(StaffRank.FONDATEUR)) {
                s.sendMessage("§cRéservé aux Fondateurs."); return true;
            }
            ServerState.INSTANCE.setClosed(true);
            Bukkit.broadcastMessage("§4§lSERVEUR §r§cFermé par §4" + p.getName() + "§c. Connexions bloquées.");
            StaffLogManager.get().log(p.getName(), "SERVER CLOSE", "-", "");
            return true;
        }
    }

    public static class ServerOpen implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (!(s instanceof Player p) || !StaffRank.of(p).isAtLeast(StaffRank.FONDATEUR)) {
                s.sendMessage("§cRéservé aux Fondateurs."); return true;
            }
            ServerState.INSTANCE.setClosed(false);
            Bukkit.broadcastMessage("§a§lSERVEUR §r§aOuvert par §2" + p.getName() + "§a.");
            StaffLogManager.get().log(p.getName(), "SERVER OPEN", "-", "");
            return true;
        }
    }

    // ════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════

    private static String join(String[] a, int from) {
        return String.join(" ", Arrays.copyOfRange(a, from, a.length));
    }

    /** Broadcast aux staffs de rang >= MODERATEUR */
    private static void broadcast(String msg, Player actor, StaffRank actorRank) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (StaffRank.of(p).isAtLeast(StaffRank.MODERATEUR) || p.isOp())
                p.sendMessage(msg);
        }
        Bukkit.getConsoleSender().sendMessage(msg);
    }
}