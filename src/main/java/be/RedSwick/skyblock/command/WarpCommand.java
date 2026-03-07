package be.RedSwick.skyblock.command;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.gui.WarpGUI;
import be.RedSwick.skyblock.manager.IslandManager;
import be.RedSwick.skyblock.manager.WarpManager;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class WarpCommand implements CommandExecutor {

    private final IslandManager islandManager = SkyBlockPlugin.getInstance().getIslandManager();
    private final WarpManager   warpManager   = SkyBlockPlugin.getInstance().getWarpManager();

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

        if (!(sender instanceof Player player)) return true;

        player.openInventory(WarpGUI.create(islandManager, warpManager));
        return true;
    }
}
