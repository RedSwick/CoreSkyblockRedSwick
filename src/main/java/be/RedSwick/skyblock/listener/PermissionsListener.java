package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.gui.PermissionsGUI;
import be.RedSwick.skyblock.island.*;
import be.RedSwick.skyblock.manager.IslandManager;
import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

public class PermissionsListener implements Listener {

    private final IslandManager manager =
            SkyBlockPlugin.getInstance().getIslandManager();

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!event.getView().getTitle().equals(PermissionsGUI.TITLE))
            return;

        event.setCancelled(true);

        Island island = manager.getIslandByMember(player.getUniqueId());
        if (island == null) return;

        if (!island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§cSeul le chef peut modifier !");
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String rawName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName())
                .toUpperCase()
                .replace(" ", "_");

        IslandPermission permission;
        try {
            permission = IslandPermission.valueOf(rawName);
        } catch (IllegalArgumentException e) {
            return; // Clic sur un bouton non reconnu (bordure, etc.)
        }

        IslandRole current = island.getPermissionRole(permission);
        IslandRole next = getNextRole(current);

        island.setPermissionRole(permission, next);

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);

        Bukkit.getScheduler().runTaskLater(
                SkyBlockPlugin.getInstance(),
                () -> player.openInventory(PermissionsGUI.create(island)),
                2L
        );
    }

    private IslandRole getNextRole(IslandRole role) {
        return switch (role) {
            case CHEF -> IslandRole.MANAGER;
            case MANAGER -> IslandRole.MEMBRE;
            case MEMBRE -> IslandRole.COOP;
            case COOP -> IslandRole.CHEF;
        };
    }
}