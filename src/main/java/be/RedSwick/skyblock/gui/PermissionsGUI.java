package be.RedSwick.skyblock.gui;

import be.RedSwick.skyblock.island.*;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PermissionsGUI {

    public static final String TITLE = "§6Permissions Île";

    public static Inventory create(Island island) {

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        for (IslandPermission permission : IslandPermission.values()) {
            inv.addItem(createItem(permission, island));
        }

        return inv;
    }

    private static ItemStack createItem(IslandPermission permission, Island island) {

        IslandRole activeRole = island.getPermissionRole(permission);

        Material material = switch (permission) {
            case DAMAGE_HOSTILE -> Material.IRON_SWORD;
            case DAMAGE_PASSIVE -> Material.WOODEN_SWORD;
            case BREAK_BLOCK -> Material.IRON_PICKAXE;
            case PLACE_BLOCK -> Material.GRASS_BLOCK;
            case BREAK_SPAWNER -> Material.SPAWNER;
            case BANK_WITHDRAW -> Material.EMERALD;
            case FLY -> Material.FEATHER;
            case CHANGE_TIME -> Material.CLOCK;
            case CHANGE_WEATHER -> Material.GRAY_DYE;
            case OPEN_CLOSE -> Material.OAK_DOOR;
            case CREATE_WARP -> Material.ENDER_PEARL;
            case SETTINGS -> Material.BOOK;
            case UPGRADE -> Material.BEACON;
            case STACK_VALUE_BLOCKS -> Material.IRON_BLOCK;
        };

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§e" + formatName(permission));

        List<String> lore = new ArrayList<>();
        lore.add("§7Rôle minimum requis :");
        lore.add("");

        for (IslandRole role : IslandRole.values()) {

            if (role == activeRole) {
                lore.add("§a✔ " + role.name());
            } else {
                lore.add("§c✘ " + role.name());
            }
        }

        lore.add("");
        lore.add("§8Clique pour changer");

        meta.setLore(lore);

        item.setItemMeta(meta);

        return item;
    }

    private static String getColor(IslandRole role) {
        return switch (role) {
            case CHEF -> "§c";
            case MANAGER -> "§6";
            case MEMBRE -> "§a";
            case COOP -> "§b";
        };
    }

    private static String formatName(IslandPermission permission) {
        return permission.name().toLowerCase().replace("_", " ");
    }
}