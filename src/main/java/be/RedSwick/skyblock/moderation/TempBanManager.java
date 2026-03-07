package be.RedSwick.skyblock.moderation;

import be.RedSwick.skyblock.SkyBlockPlugin;
import org.bukkit.BanList;
import org.bukkit.Bukkit;

import java.util.Date;
import java.util.UUID;

public class TempBanManager {

    private static final TempBanManager INSTANCE = new TempBanManager();
    public static TempBanManager get() { return INSTANCE; }

    /** Ban temporaire via le BanList natif Bukkit */
    public void tempBan(String playerName, long durationMs, String reason, String by) {
        Date expiry = new Date(System.currentTimeMillis() + durationMs);
        Bukkit.getBanList(BanList.Type.NAME).addBan(playerName, reason, expiry, by);
    }

    /** Ban permanent */
    public void ban(String playerName, String reason, String by) {
        Bukkit.getBanList(BanList.Type.NAME).addBan(playerName, reason, null, by);
    }

    public void unban(String playerName) {
        Bukkit.getBanList(BanList.Type.NAME).pardon(playerName);
    }

    public boolean isBanned(String playerName) {
        return Bukkit.getBanList(BanList.Type.NAME).isBanned(playerName);
    }

    public static String formatDuration(long days) {
        if (days < 1)    return "< 1 jour";
        if (days == 1)   return "1 jour";
        if (days < 30)   return days + " jours";
        if (days < 365)  return (days / 30) + " mois";
        return (days / 365) + " an(s)";
    }
}