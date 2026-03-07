package be.RedSwick.skyblock.command;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.listener.PlayerDataListener;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        // Despawn les mobs de son île avant de partir
        var island = SkyBlockPlugin.getInstance().getIslandManager()
                .getIslandByMember(player.getUniqueId());
        if (island != null) {
            boolean othersOnline = island.getAllMembers().stream()
                    .filter(uid -> !uid.equals(player.getUniqueId()))
                    .anyMatch(uid -> Bukkit.getPlayer(uid) != null);
            if (!othersOnline) {
                PlayerDataListener.despawnIslandMobs(island);
            }
        }

        World world = Bukkit.getWorld("world");
        if (world == null) { player.sendMessage("§cMonde introuvable."); return true; }
        player.teleport(world.getSpawnLocation());
        player.sendMessage("§aTéléportation au spawn !");
        return true;
    }
}