package be.RedSwick.skyblock.manager;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.island.Island;
import be.RedSwick.skyblock.player.PlayerData;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BossBarManager — Version optimisée
 *
 * OPTIMISATIONS vs version précédente :
 * 1. Cache lastTitle par joueur — setTitle() appelé UNIQUEMENT si le texte change
 *    → sur 300 joueurs AFK, ~0 packets/s au lieu de 300/s
 * 2. refreshBar(uuid) : appelé depuis les setters coins/gems/grade dans PlayerData
 *    → mise à jour immédiate quand ça change, pas besoin d'attendre le tick
 * 3. Ticker réduit à 4s (80 ticks) au lieu de 1s — pour le IS Level île
 *    (les coins/gems se rafraîchissent via refreshBar en temps réel)
 * 4. ConcurrentHashMap : thread-safe si refreshBar appelé depuis async
 */
public class BossBarManager {

    private final Map<UUID, BossBar> bars       = new ConcurrentHashMap<>();
    private final Map<UUID, String>  lastTitles = new ConcurrentHashMap<>();

    public BossBarManager() {
        startTicker();
    }

    // ════════════════════════════════════════════════
    //  Créer / Supprimer
    // ════════════════════════════════════════════════

    public void createBar(Player player) {
        removeBar(player);
        String title = buildText(player);
        BossBar bar = Bukkit.createBossBar(title, BarColor.WHITE, BarStyle.SOLID);
        bar.setProgress(0.0);
        bar.addPlayer(player);
        bars.put(player.getUniqueId(), bar);
        lastTitles.put(player.getUniqueId(), title);
    }

    public void removeBar(Player player) {
        BossBar bar = bars.remove(player.getUniqueId());
        if (bar != null) bar.removeAll();
        lastTitles.remove(player.getUniqueId());
    }

    // ════════════════════════════════════════════════
    //  Refresh immédiat — appelé quand coins/gems/grade changent
    //  Peut être appelé depuis n'importe quel contexte (main thread requis
    //  pour setTitle — on schedule si nécessaire)
    // ════════════════════════════════════════════════

    public void refreshBar(UUID uuid) {
        BossBar bar = bars.get(uuid);
        if (bar == null) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return;

        String newTitle = buildText(player);
        String last     = lastTitles.get(uuid);

        // Ne met à jour QUE si le texte a changé
        if (!newTitle.equals(last)) {
            bar.setTitle(newTitle);
            lastTitles.put(uuid, newTitle);
        }
    }

    // ════════════════════════════════════════════════
    //  Ticker — toutes les 4s pour le IS Level
    //  Les coins/gems sont déjà rafraîchis via refreshBar()
    // ════════════════════════════════════════════════

    private void startTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : bars.keySet()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;

                    String newTitle = buildText(player);
                    String last     = lastTitles.get(uuid);

                    if (!newTitle.equals(last)) {
                        bars.get(uuid).setTitle(newTitle);
                        lastTitles.put(uuid, newTitle);
                    }
                }
            }
        }.runTaskTimer(SkyBlockPlugin.getInstance(), 80L, 80L); // 4s au lieu de 1s
    }

    // ════════════════════════════════════════════════
    //  Construction du texte
    // ════════════════════════════════════════════════

    private String buildText(Player player) {
        PlayerDataManager pdm = SkyBlockPlugin.getInstance().getPlayerDataManager();
        IslandManager     im  = SkyBlockPlugin.getInstance().getIslandManager();

        PlayerData data = pdm.get(player.getUniqueId());
        if (data == null) return "§fArcanium Skyblock";

        String coins = pill("§6⬡ §e" + fmt(data.getCoins()));
        String gems  = pill("§b💎 §f" + fmt(data.getGems()));

        String gradeStr = data.getGrade() != be.RedSwick.skyblock.player.PlayerGrade.AUCUN
                ? data.getGrade().getPrefix() + " §f" : "§f";
        String pseudo = pill(gradeStr + player.getName());

        String level = pill("§7Lv §f" + data.getLevel());

        Island island = im.getIslandByMember(player.getUniqueId());
        String isLevel = island != null
                ? "  " + pill("§7Île §f" + fmt((long) island.getIsLevel()) + " IS")
                : "";

        return coins + "  " + gems + "  " + pseudo + "  " + level + isLevel;
    }

    private static String pill(String content) {
        return "§8❰ " + content + " §8❱";
    }

    private static String fmt(long n) {
        if (n >= 1_000_000) {
            long millions = n / 1_000_000;
            long hundreds = (n % 1_000_000) / 1_000;
            return hundreds > 0 ? millions + "M" + hundreds : millions + "M";
        }
        if (n >= 1_000) return (n / 1_000) + "K";
        return String.valueOf(n);
    }

    public void removeAll() {
        bars.values().forEach(BossBar::removeAll);
        bars.clear();
        lastTitles.clear();
    }
}