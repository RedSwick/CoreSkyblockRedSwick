package be.RedSwick.skyblock.command;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.gui.GradeGUI;
import be.RedSwick.skyblock.manager.PlayerDataManager;
import be.RedSwick.skyblock.player.PlayerData;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class GradeCommand implements CommandExecutor {

    private final PlayerDataManager pdm =
            SkyBlockPlugin.getInstance().getPlayerDataManager();

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

        if (!(sender instanceof Player player)) return true;

        PlayerData data = pdm.get(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§cErreur : données joueur non chargées.");
            return true;
        }

        player.openInventory(GradeGUI.create(data));
        return true;
    }
}