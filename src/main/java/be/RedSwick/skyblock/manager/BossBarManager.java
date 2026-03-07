package be.RedSwick.skyblock.manager;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.island.Island;
import be.RedSwick.skyblock.player.PlayerData;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * OPTIMISATIONS :
 *  - Cache du texte bossbar par joueur : rebuild uniquement si coins/gems/level/isLevel changent
 *  - Snapshot des valeurs précédentes → comparaison O(1) avant setTitle()
 *  - Ticker toujours à 20 ticks (1s) mais avec court-circuit si rien n'a changé
 */
public class BossBarManager {

    private final Map<UUID, BossBar> bars      = new HashMap<>();
    private final Map<UUID, long[]>  lastValues = new HashMap<>();
    // Index : [0]=coins [1]=gems [2]=level [3]=isLevel(long)

    public BossBarManager() {
        startTicker();
    }

    public void createBar(Player player) {
        removeBar(player);
        BossBar bar = Bukkit.createBossBar(
                buildText(player),
                BarColor.WHITE,
                BarStyle.SOLID
        );
        bar.setProgress(0.0);
        bar.addPlayer(player);
        bars.put(player.getUniqueId(), bar);
        cacheValues(player);
    }

    public void removeBar(Player player) {
        BossBar bar = bars.remove(player.getUniqueId());
        if (bar != null) bar.removeAll();
        lastValues.remove(player.getUniqueId());
    }

    private void startTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, BossBar> entry : bars.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player == null || !player.isOnline()) continue;

                    // Court-circuit : mettre à jour le texte SEULEMENT si les valeurs ont changé
                    if (!hasChanged(player)) continue;

                    entry.getValue().setTitle(buildText(player));
                    cacheValues(player);
                }
            }
        }.runTaskTimer(SkyBlockPlugin.getInstance(), 20L, 20L);
    }

    /** Vérifie si coins/gems/level/isLevel ont changé depuis le dernier rendu. */
    private boolean hasChanged(Player player) {
        UUID uuid = player.getUniqueId();
        long[] prev = lastValues.get(uuid);
        if (prev == null) return true;

        PlayerData data = SkyBlockPlugin.getInstance().getPlayerDataManager().get(uuid);
        if (data == null) return false;

        Island island = SkyBlockPlugin.getInstance().getIslandManager().getIslandByMember(uuid);
        long isLvl = island != null ? (long) island.getIsLevel() : -1;

        return data.getCoins()  != prev[0]
                || data.getGems()   != prev[1]
                || data.getLevel()  != prev[2]
                || isLvl            != prev[3];
    }

    private void cacheValues(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = SkyBlockPlugin.getInstance().getPlayerDataManager().get(uuid);
        if (data == null) return;
        Island island = SkyBlockPlugin.getInstance().getIslandManager().getIslandByMember(uuid);
        long isLvl = island != null ? (long) island.getIsLevel() : -1;
        lastValues.put(uuid, new long[]{ data.getCoins(), data.getGems(), data.getLevel(), isLvl });
    }

    private String buildText(Player player) {
        PlayerDataManager pdm = SkyBlockPlugin.getInstance().getPlayerDataManager();
        IslandManager im = SkyBlockPlugin.getInstance().getIslandManager();

        PlayerData data = pdm.get(player.getUniqueId());
        if (data == null) return "§fArcanium Skyblock";

        String coins  = pill("§6⬡ §e" + fmt(data.getCoins()));
        String gems   = pill("§b💎 §f" + fmt(data.getGems()));

        String gradeStr = data.getGrade() != be.RedSwick.skyblock.player.PlayerGrade.AUCUN
                ? data.getGrade().getPrefix() + " §f" : "§f";
        String pseudo = pill(gradeStr + player.getName());
        String level  = pill("§7Lv §f" + data.getLevel());

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
            long m = n / 1_000_000;
            long k = (n % 1_000_000) / 1_000;
            return k > 0 ? m + "M" + k : m + "M";
        }
        if (n >= 1_000) return (n / 1_000) + "K";
        return String.valueOf(n);
    }

    public void removeAll() {
        bars.values().forEach(BossBar::removeAll);
        bars.clear();
        lastValues.clear();
    }
}