package be.RedSwick.skyblock.moderation;

import org.bukkit.entity.Player;

/**
 * Hiérarchie staff du serveur.
 * Niveau plus élevé = plus de droits.
 */
public enum StaffRank {

    NONE       (0, ""),
    GUIDE      (1, "arcanium.guide"),
    MODERATEUR (2, "arcanium.mod"),
    ADMIN      (3, "arcanium.admin"),
    FONDATEUR  (4, "arcanium.fondateur");

    private final int     level;
    private final String  permission;

    StaffRank(int level, String permission) {
        this.level      = level;
        this.permission = permission;
    }

    public int    getLevel()      { return level; }
    public String getPermission() { return permission; }

    /** Récupère le grade staff d'un joueur via StaffPermissionManager */
    public static StaffRank of(Player p) {
        StaffRank fromMap = be.RedSwick.skyblock.moderation.StaffPermissionManager
                .get().getRank(p.getName());
        return fromMap;
    }

    public boolean isAtLeast(StaffRank other) {
        return this.level >= other.level;
    }

    /** Peut-on effectuer une action de modération sur cette cible ? */
    public boolean canModerate(Player target) {
        StaffRank targetRank = of(target);
        // Doit avoir un niveau strictement supérieur à la cible
        return this.level > targetRank.level;
    }

    public String getPrefix() {
        return switch (this) {
            case FONDATEUR  -> "§4§lFONDATEUR §r";
            case ADMIN      -> "§c§lADMIN §r";
            case MODERATEUR -> "§9§lMOD §r";
            case GUIDE      -> "§a§lGUIDE §r";
            case NONE       -> "";
        };
    }

    public String getDisplay() {
        return switch (this) {
            case FONDATEUR  -> "§4Fondateur";
            case ADMIN      -> "§cAdmin";
            case MODERATEUR -> "§9Modérateur";
            case GUIDE      -> "§aGuide";
            case NONE       -> "§7Joueur";
        };
    }
}