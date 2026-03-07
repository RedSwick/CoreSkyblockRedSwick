package be.RedSwick.skyblock.leaderboard;

public enum LeaderboardType {

    COINS          ("coins",        "§6⬡ Coins",             "§6"),
    LEVEL          ("level",        "§e✦ Niveau",            "§e"),
    BLOCS_MINES    ("blockminer",   "§7⛏ Blocs Minés",      "§7"),
    MOBS_TUES      ("mobkills",     "§c⚔ Mobs Tués",        "§c"),
    TEMPS_JEU      ("playtime",     "§b⏱ Temps de Jeu",     "§b"),
    NIVEAU_ILE     ("islandlevel",  "§a🏝 Niveau d'Île",    "§a"),
    JOB_CHASSEUR   ("job_chasseur",   "§c⚔ Chasseur",       "§c"),
    JOB_FARMER     ("job_farmer",     "§a🌾 Farmer",         "§a"),
    JOB_MINER      ("job_miner",      "§7⛏ Mineur",         "§7"),
    JOB_BUCHERON   ("job_bucheron",   "§6🪓 Bûcheron",      "§6"),
    JOB_ALCHIMISTE ("job_alchimiste", "§d⚗ Alchimiste",     "§d"),
    JOB_PECHEUR    ("job_pecheur",    "§b🎣 Pêcheur",       "§b"),
    POISSONS_PECHES("fishcaught",   "§b🎣 Poissons Pêchés", "§b"),
    BUCHES_COUPEES  ("logscutting",   "§6🪓 Bûches Coupées",   "§6"),
    CULTURES_RECOLTEES("cropsharvested","§a🌾 Cultures Récoltées","§a");

    private final String id;
    private final String display;
    private final String color;

    LeaderboardType(String id, String display, String color) {
        this.id = id; this.display = display; this.color = color;
    }

    public String getId()      { return id; }
    public String getDisplay() { return display; }
    public String getColor()   { return color; }

    public static LeaderboardType fromId(String id) {
        for (LeaderboardType t : values())
            if (t.id.equalsIgnoreCase(id)) return t;
        return null;
    }

    public static java.util.List<String> allIds() {
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (LeaderboardType t : values()) ids.add(t.id);
        return ids;
    }
}