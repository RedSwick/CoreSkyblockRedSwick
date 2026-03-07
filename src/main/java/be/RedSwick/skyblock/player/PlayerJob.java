package be.RedSwick.skyblock.player;

public enum PlayerJob {

    CHASSEUR   ("§cChasseur",    "§c⚔"),
    FARMER     ("§aFarmer",      "§a🌾"),
    MINER      ("§7Mineur",      "§7⛏"),
    BUCHERON   ("§6Bûcheron",    "§6🪓"),
    ALCHIMISTE ("§dAlchimiste",  "§d⚗"),
    PECHEUR    ("§bPêcheur",     "§b🎣");

    private final String display;
    private final String icon;

    PlayerJob(String display, String icon) {
        this.display = display;
        this.icon    = icon;
    }

    public String getDisplay() { return display; }
    public String getIcon()    { return icon; }
}