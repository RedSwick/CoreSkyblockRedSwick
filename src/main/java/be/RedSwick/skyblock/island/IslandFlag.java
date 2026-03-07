package be.RedSwick.skyblock.island;

/**
 * Flags de comportement d'une île — activés/désactivés par le chef.
 * Inspiré de SuperiorSkyblock2 Island Flags.
 */
public enum IslandFlag {

    PVP("§cPVP",                    "§7Combat joueur vs joueur",            false),
    MOB_DAMAGE("§eDégâts mobs",     "§7Les mobs peuvent blesser",           true),
    MOB_SPAWNING("§eSpawn mobs",    "§7Spawn naturel de mobs",              true),
    FIRE_SPREAD("§6Propagation feu","§7Le feu se propage",                  false),
    LEAF_DECAY("§aChute feuilles",  "§7Les feuilles tombent naturellement", true),
    ANIMAL_BREEDING("§aÉlevage",    "§7Reproduction des animaux",           true),
    VISITOR_DROP("§7Drops visiteurs","§7Les visiteurs peuvent drop items",  false),
    VISITOR_PICKUP("§7Pickup visit.","§7Les visiteurs ramassent les items",  false),
    TNT("§cTNT",                    "§7Les TNT explosent",                  false);

    private final String displayName;
    private final String description;
    private final boolean defaultValue;

    IslandFlag(String displayName, String description, boolean defaultValue) {
        this.displayName  = displayName;
        this.description  = description;
        this.defaultValue = defaultValue;
    }

    public String  getDisplayName()  { return displayName; }
    public String  getDescription()  { return description; }
    public boolean getDefaultValue() { return defaultValue; }
}