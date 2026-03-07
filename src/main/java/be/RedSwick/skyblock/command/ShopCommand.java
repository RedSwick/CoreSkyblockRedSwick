package be.RedSwick.skyblock.command;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.manager.PlayerDataManager;
import be.RedSwick.skyblock.player.PlayerData;
import be.RedSwick.skyblock.shop.ShopGUI;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final PlayerDataManager pdm = SkyBlockPlugin.getInstance().getPlayerDataManager();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande joueur uniquement.");
            return true;
        }
        PlayerData data = pdm.get(player.getUniqueId());
        if (data == null) return true;

        player.openInventory(ShopGUI.createMain());
        return true;
    }
}