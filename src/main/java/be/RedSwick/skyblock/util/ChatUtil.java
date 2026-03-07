package be.RedSwick.skyblock.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

/**
 * Utilitaires de chat compatibles Paper 1.21.
 */
public class ChatUtil {

    /**
     * Envoie un message en ActionBar au joueur.
     * Compatible Paper 1.21 (API BungeeCord).
     */
    public static void actionBar(Player player, String message) {
        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent(message)
        );
    }

    /**
     * Formate un nombre long en format compact (1K, 1M...).
     * Source unique — évite les copies dans JobManager, BossBarManager, ShopGUI, etc.
     */
    public static String fmt(long n) {
        if (n >= 1_000_000_000) return (n / 1_000_000_000) + "G";
        if (n >= 1_000_000)     return (n / 1_000_000) + "M";
        if (n >= 1_000)         return (n / 1_000) + "K";
        return String.valueOf(n);
    }

    public static String fmtFull(long n) {
        return String.format("%,d", n);
    }
}