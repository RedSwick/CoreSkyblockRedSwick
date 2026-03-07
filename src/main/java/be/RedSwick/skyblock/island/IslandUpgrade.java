package be.RedSwick.skyblock.island;

/**
 * Upgrades d'île — achetés avec des coins.
 * Inspiré de SuperiorSkyblock2 Upgrades.
 *
 * Chaque upgrade a plusieurs niveaux (0 = base vanilla, max = niveau max).
 */
public enum IslandUpgrade {

    // Taille de l'île (rayon en blocs)
    SIZE("§bTaille île", "§7Augmente la taille de l'île",
            new int[]{50, 75, 100, 150, 200},
            new int[]{0, 5_000, 15_000, 40_000, 100_000}),

    // Membres maximum sur l'île
    MEMBER_LIMIT("§eMembres max", "§7Augmente le nombre max de membres",
            new int[]{3, 5, 8, 12, 20},
            new int[]{0, 2_000, 8_000, 20_000, 50_000}),

    // Spawners max sur l'île
    SPAWNER_LIMIT("§6Spawners max", "§7Augmente la limite de spawners",
            new int[]{50, 75, 100, 150, 200},
            new int[]{0, 3_000, 10_000, 25_000, 60_000}),

    // Vitesse de pousse des cultures (en %)
    CROP_SPEED("§aCroissance cultures", "§7Accélère la pousse des cultures",
            new int[]{100, 125, 150, 200, 300},
            new int[]{0, 4_000, 12_000, 30_000, 75_000}),

    // Taux de spawn des mobs spawner (en %)
    MOB_RATE("§cTaux spawn mobs", "§7Augmente la fréquence des spawners",
            new int[]{100, 125, 150, 200, 300},
            new int[]{0, 5_000, 15_000, 35_000, 80_000});

    private final String   displayName;
    private final String   description;
    private final int[]    values;   // valeur à chaque niveau
    private final int[]    costs;    // coût en coins pour passer au niveau suivant

    IslandUpgrade(String displayName, String description, int[] values, int[] costs) {
        this.displayName = displayName;
        this.description = description;
        this.values      = values;
        this.costs       = costs;
    }

    public String getDisplayName()   { return displayName; }
    public String getDescription()   { return description; }
    public int    getMaxLevel()       { return values.length - 1; }
    public int    getValue(int level) { return values[Math.min(level, values.length - 1)]; }
    public int    getCost(int level)  { return level >= costs.length ? -1 : costs[level]; }
    public String getUnit() {
        return switch (this) {
            case SIZE -> " blocs rayon";
            case MEMBER_LIMIT, SPAWNER_LIMIT -> " max";
            case CROP_SPEED, MOB_RATE -> "%";
        };
    }
}
