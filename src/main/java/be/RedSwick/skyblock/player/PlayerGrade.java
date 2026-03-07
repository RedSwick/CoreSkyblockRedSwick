package be.RedSwick.skyblock.player;

public enum PlayerGrade {

    AUCUN,
    NEXUS,
    ASCENDANT,
    ARCANIUM;

    public String getDisplay() {
        return switch (this) {
            case AUCUN      -> "§7[Aucun]";
            case NEXUS      -> "§b[Nexus]";
            case ASCENDANT  -> "§9[Ascendant]";
            case ARCANIUM   -> "§5[Arcanium]";
        };
    }

    public String getPrefix() {
        return switch (this) {
            case AUCUN      -> "";
            case NEXUS      -> "§b§lNEXUS §r";
            case ASCENDANT  -> "§9§lASCENDANT §r";
            case ARCANIUM   -> "§5§lARCANIUM §r";
        };
    }
}

