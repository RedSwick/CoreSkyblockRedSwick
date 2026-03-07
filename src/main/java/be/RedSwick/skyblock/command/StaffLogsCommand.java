package be.RedSwick.skyblock.command;

import be.RedSwick.skyblock.gui.StaffLogsGUI;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class StaffLogsCommand implements CommandExecutor {

    private static final String PERM_ADMIN = "arcanium.admin";
    private static final String PERM_MOD   = "arcanium.mod";
    private static final String PERM_GUIDE = "arcanium.guide";

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande joueur uniquement."); return true;
        }
        if (!player.hasPermission(PERM_ADMIN)
                && !player.hasPermission(PERM_MOD)
                && !player.hasPermission(PERM_GUIDE)
                && !player.isOp()) {
            player.sendMessage("§cPas la permission."); return true;
        }
        Bukkit.getScheduler().runTask(
                be.RedSwick.skyblock.SkyBlockPlugin.getInstance(),
                () -> player.openInventory(StaffLogsGUI.createMain(player))
        );
        return true;
    }
}