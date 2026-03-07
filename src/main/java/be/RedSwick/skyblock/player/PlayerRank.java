package be.RedSwick.skyblock.player;

public enum PlayerRank {

    SERVITEUR   (1,  "§8Serviteur"),
    SENTINELLE  (2,  "§7Sentinelle"),
    DISCIPLE    (3,  "§fDisciple"),
    ADEPTE      (4,  "§aAdepte"),
    INVOCATEUR  (5,  "§2Invocateur"),
    MYSTIQUE    (6,  "§bMystique"),
    ARCANISTE   (7,  "§3Arcaniste"),
    MAITRE      (8,  "§9Maître"),
    SEIGNEUR    (9,  "§1Seigneur"),
    SOUVERAIN   (10, "§5Souverain"),
    ARCHIMAGE   (11, "§dArchimage"),
    ETERNEL     (12, "§6§lÉternel");

    private final int level;
    private final String display;

    PlayerRank(int level, String display) {
        this.level   = level;
        this.display = display;
    }

    public int getLevel()      { return level; }
    public String getDisplay() { return display; }

    /**
     * Retourne le rang suivant, ou null si déjà ETERNEL.
     */
    public PlayerRank next() {
        PlayerRank[] values = values();
        int nextOrdinal = this.ordinal() + 1;
        if (nextOrdinal >= values.length) return null;
        return values[nextOrdinal];
    }

    public static PlayerRank fromLevel(int level) {
        for (PlayerRank rank : values()) {
            if (rank.level == level) return rank;
        }
        return SERVITEUR;
    }
}