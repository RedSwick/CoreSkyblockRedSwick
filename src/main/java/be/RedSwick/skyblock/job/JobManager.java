package be.RedSwick.skyblock.job;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.listener.NewItemsListener;
import be.RedSwick.skyblock.player.PlayerData;
import be.RedSwick.skyblock.player.PlayerJob;
import be.RedSwick.skyblock.util.ChatUtil;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Moteur XP / Coins / Level-up des métiers.
 *
 * OPTIMISATIONS :
 *
 *  rewardAction() — appel unitaire (bloc par bloc, mob par mob) :
 *    checkLevelUp + updateActionBar à chaque appel = OK pour 1 bloc/mob
 *
 *  rewardActionBatch() — appel groupé CRITIQUE pour hoe 5x5 / hammer 3x3 :
 *    Hoe 5x5 = 25 crops × 2-3 drops = 50-75 appels rewardAction() par CLIC
 *    → 50-75 × checkLevelUp() + 50-75 × sendActionBar() = spike TPS garanti
 *    Solution : accumuler tout en une passe, checkLevelUp() UNE SEULE FOIS,
 *    sendActionBar() UNE SEULE FOIS.
 *    CustomItemListener.handleHoe() et handleHammer() doivent utiliser
 *    rewardActionBatch() au lieu de triggerFarmerJob()/triggerMinerJob().
 *
 *  tickResetDisplay() : iterator.remove() → zéro allocation intermédiaire
 *  rewardActionChasseur() → appelle rewardAction() (déduplique le code)
 */
public class JobManager {

    private final Map<UUID, Map<PlayerJob, Double>> displayXp    = new HashMap<>();
    private final Map<UUID, Map<PlayerJob, Double>> displayCoins = new HashMap<>();
    private final Map<UUID, Long>                   lastReset    = new HashMap<>();

    private static final long DISPLAY_DURATION_MS = 3000;
    private static final int  MAX_LEVEL           = 150;

    // ═══════════════════════════════════════════════════
    //  RECOMPENSE UNITAIRE — 1 bloc / 1 mob / 1 poisson
    // ═══════════════════════════════════════════════════

    public void rewardAction(Player player, PlayerJob job, double xpAmount, double coinsAmount) {
        PlayerData data = SkyBlockPlugin.getInstance().getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        double ringBonus = NewItemsListener.getRingBonus(player, job);
        double finalXp   = xpAmount * (1.0 + ringBonus);

        data.addJobXp(job, (long) (finalXp * 100));

        int levelBefore = data.getJobLevel(job);
        checkLevelUp(data, job);
        int levelAfter  = data.getJobLevel(job);

        if (levelAfter > levelBefore) onLevelUp(player, data, job, levelAfter);

        if (coinsAmount > 0) data.addCoins((long) coinsAmount);

        UUID uuid = player.getUniqueId();
        accumulate(uuid, job, finalXp, coinsAmount);
        updateActionBar(player, job, data);
    }

    // ═══════════════════════════════════════════════════
    //  RECOMPENSE GROUPEE — hoe 5x5, hammer 3x3
    //
    //  Appeler UNE SEULE FOIS pour toute la passe multi-blocs.
    //  xpTotal  = somme de tous les xp individuels (avant ring bonus)
    //  coinTotal = somme de tous les coins individuels
    //
    //  Gain : N appels rewardAction() → 1 seul checkLevelUp + 1 seul sendActionBar
    // ═══════════════════════════════════════════════════

    public void rewardActionBatch(Player player, PlayerJob job, double xpTotal, double coinTotal) {
        if (xpTotal <= 0 && coinTotal <= 0) return;
        PlayerData data = SkyBlockPlugin.getInstance().getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        double ringBonus = NewItemsListener.getRingBonus(player, job);
        double finalXp   = xpTotal * (1.0 + ringBonus);

        data.addJobXp(job, (long) (finalXp * 100));

        int levelBefore = data.getJobLevel(job);
        checkLevelUp(data, job);
        int levelAfter  = data.getJobLevel(job);

        if (levelAfter > levelBefore) onLevelUp(player, data, job, levelAfter);

        if (coinTotal > 0) data.addCoins((long) coinTotal);

        UUID uuid = player.getUniqueId();
        accumulate(uuid, job, finalXp, coinTotal);
        // Une seule ActionBar pour toute la passe
        updateActionBar(player, job, data);
    }

    // ═══════════════════════════════════════════════════
    //  CHASSEUR — essence en plus
    // ═══════════════════════════════════════════════════

    public void rewardActionChasseur(Player player, double xpAmount, double coinsAmount, double essenceAmount) {
        rewardAction(player, PlayerJob.CHASSEUR, xpAmount, coinsAmount);
        if (essenceAmount > 0) {
            PlayerData data = SkyBlockPlugin.getInstance().getPlayerDataManager().get(player.getUniqueId());
            if (data != null) data.addEssence(Math.round(essenceAmount));
        }
    }

    // ═══════════════════════════════════════════════════
    //  DISPLAY SELL COINS
    // ═══════════════════════════════════════════════════

    public void displaySellCoins(Player player, long earned) {
        if (earned <= 0) return;
        UUID uuid = player.getUniqueId();
        displayCoins.computeIfAbsent(uuid, k -> new EnumMap<>(PlayerJob.class))
                .merge(PlayerJob.MINER, (double) earned, Double::sum);
        lastReset.put(uuid, System.currentTimeMillis());
        ChatUtil.actionBar(player, "§6+" + fmt(earned) + " ⬡ §7(vente)");
    }

    // ═══════════════════════════════════════════════════
    //  TICKER RESET — toutes les 2 secondes
    // ═══════════════════════════════════════════════════

    public void tickResetDisplay() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> it = lastReset.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (now - entry.getValue() >= DISPLAY_DURATION_MS) {
                UUID uuid = entry.getKey();
                displayXp.remove(uuid);
                displayCoins.remove(uuid);
                it.remove();
            }
        }
    }

    // ═══════════════════════════════════════════════════
    //  HELPERS INTERNES
    // ═══════════════════════════════════════════════════

    private void checkLevelUp(PlayerData data, PlayerJob job) {
        while (data.getJobLevel(job) < MAX_LEVEL) {
            long xpNeeded = JobXpTable.getXpRequired(data.getJobLevel(job)) * 100L;
            if (data.getJobXp(job) < xpNeeded) break;
            data.addJobXp(job, -xpNeeded);
            data.setJobLevel(job, data.getJobLevel(job) + 1);
        }
    }

    private void onLevelUp(Player player, PlayerData data, PlayerJob job, int newLevel) {
        long bonusCoins = JobXpTable.getLevelUpCoins(newLevel);
        data.addCoins(bonusCoins);
        player.sendTitle(
                "§6✦ " + job.getDisplay() + " §eniveau " + newLevel,
                "§7+" + fmt(bonusCoins) + " ⬡  |  §b" + fmt(data.getJobXp(job) / 100) + " XP total",
                10, 60, 20
        );
        SkyBlockPlugin.getInstance().getPlayerDataManager().savePlayer(player.getUniqueId());
    }

    private void accumulate(UUID uuid, PlayerJob job, double xp, double coins) {
        displayXp.computeIfAbsent(uuid, k -> new EnumMap<>(PlayerJob.class))
                .merge(job, xp, Double::sum);
        displayCoins.computeIfAbsent(uuid, k -> new EnumMap<>(PlayerJob.class))
                .merge(job, coins, Double::sum);
        lastReset.put(uuid, System.currentTimeMillis());
    }

    private void updateActionBar(Player player, PlayerJob job, PlayerData data) {
        UUID uuid = player.getUniqueId();
        double xpShown  = displayXp.getOrDefault(uuid, Map.of()).getOrDefault(job, 0.0);
        double coinsAll = sumCoins(displayCoins.get(uuid));
        long required   = JobXpTable.getXpRequired(data.getJobLevel(job));
        long currentXp  = data.getJobXp(job) / 100;

        String bar = job.getIcon() + " §7" + job.getDisplay()
                + " §8[§a" + currentXp + "§7/§a" + required + " XP§8]"
                + " §8(§a+" + fmtD(xpShown) + " XP§8)"
                + (coinsAll > 0 ? " §6+" + fmt((long) coinsAll) + " ⬡" : "");
        ChatUtil.actionBar(player, bar);
    }

    private double sumCoins(Map<PlayerJob, Double> map) {
        if (map == null) return 0;
        double sum = 0;
        for (double v : map.values()) sum += v;
        return sum;
    }

    // ═══════════════════════════════════════════════════
    //  FORMATAGE
    // ═══════════════════════════════════════════════════

    public static String fmt(long n) {
        if (n >= 1_000_000) return (n / 1_000_000) + "M";
        if (n >= 1_000)     return (n / 1_000) + "K";
        return String.valueOf(n);
    }

    public static String fmtD(double n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000);
        if (n >= 1_000)     return String.format("%.1fK", n / 1_000);
        return String.format("%.0f", n);
    }
}