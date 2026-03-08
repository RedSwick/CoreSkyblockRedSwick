package be.RedSwick.skyblock.player;

import java.util.*;

/**
 * PlayerData — Version optimisée
 *
 * OPTIMISATION : dirty flag
 * Chaque setter/increment lève un flag isDirty = true.
 * PlayerDataManager ne sauvegarde sur disque QUE les joueurs dirty.
 * Avec 300 joueurs dont 80% AFK → ~60 saves au lieu de 300 par flush.
 * GC et I/O divisés par ~5.
 *
 * MAX LIMITS : coins/gems/essence cappés à Long.MAX_VALUE/2 pour éviter
 * un overflow silencieux en cas de bug d'attribution en boucle.
 */
public class PlayerData {

    // ── Plafonds anti-overflow ──────────────────────────────────
    public static final long MAX_COINS   = 1_000_000_000_000L; // 1 trillion
    public static final long MAX_GEMS    =     500_000_000L;   // 500M
    public static final long MAX_ESSENCE = 1_000_000_000_000L; // 1 trillion

    private final UUID uuid;

    // ── dirty flag ──────────────────────────────────────────────
    // volatile : lisible depuis le thread async de flush sans synchronisation lourde
    private volatile boolean dirty = false;

    // ── BossBar refresh callback ─────────────────────────────────
    // Appelé quand coins/gems/grade changent pour mise à jour immédiate de la bossbar
    // Évite la dépendance circulaire PlayerData → BossBarManager
    private Runnable onDisplayChange = null;
    public void setOnDisplayChange(Runnable r) { this.onDisplayChange = r; }
    private void notifyDisplay() { if (onDisplayChange != null) onDisplayChange.run(); }

    // ===== ÉCONOMIE =====
    private long coins   = 0;
    private long essence = 0;
    private long gems    = 0;
    private long totalEssenceEarned = 0;

    // ===== RANG / GRADE =====
    private PlayerRank  rank  = PlayerRank.SERVITEUR;
    private PlayerGrade grade = PlayerGrade.AUCUN;

    // ===== NIVEAU GLOBAL =====
    private int  level = 1;
    private long xp    = 0;

    // ===== MÉTIERS =====
    private final Map<PlayerJob, Integer> jobLevels = new EnumMap<>(PlayerJob.class);
    private final Map<PlayerJob, Long>    jobXp     = new EnumMap<>(PlayerJob.class);

    // ===== STATS =====
    private long mobsKilled       = 0;
    private long blocksPlaced     = 0;
    private long blocksBroken     = 0;
    private long itemsCrafted     = 0;
    private long fishCaught       = 0;
    private long logsCut          = 0;
    private long playtimeTicks    = 0;
    private long totalCoinsEarned = 0;
    private long cropsBroken      = 0;

    // ===== TEAM CHAT =====
    // Flag RAM uniquement — pas sauvegardé sur disque (reset au reconnect, c'est voulu)
    private boolean teamChatEnabled = false;

    public boolean isTeamChatEnabled()           { return teamChatEnabled; }
    public void    setTeamChatEnabled(boolean v) { teamChatEnabled = v; }

    // ===== CONSTRUCTEUR =====

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        for (PlayerJob job : PlayerJob.values()) {
            jobLevels.put(job, 1);
            jobXp.put(job, 0L);
        }
    }

    // ===== DIRTY FLAG =====

    public boolean isDirty()   { return dirty; }
    public void    markDirty() { dirty = true; }
    /** Appelé par PlayerDataManager après sauvegarde réussie */
    public void    clearDirty() { dirty = false; }

    // ===== UUID =====

    public UUID getUuid() { return uuid; }

    // ===== COINS =====

    public long getCoins() { return coins; }

    public void setCoins(long coins) {
        long capped = Math.max(0, Math.min(coins, MAX_COINS));
        if (this.coins != capped) { this.coins = capped; dirty = true; }
    }

    public void addCoins(long amount) {
        if (amount == 0) return;
        coins = Math.min(coins + amount, MAX_COINS);
        dirty = true; notifyDisplay();
    }

    public boolean removeCoins(long amount) {
        if (coins < amount) return false;
        coins -= amount;
        dirty = true; notifyDisplay();
        return true;
    }

    // ===== ESSENCE =====

    public long getEssence() { return essence; }

    public void setEssence(long essence) {
        long capped = Math.max(0, Math.min(essence, MAX_ESSENCE));
        if (this.essence != capped) { this.essence = capped; dirty = true; }
    }

    public void addEssence(long amount) {
        if (amount == 0) return;
        essence = Math.min(essence + amount, MAX_ESSENCE);
        if (amount > 0) totalEssenceEarned = Math.min(totalEssenceEarned + amount, MAX_ESSENCE);
        dirty = true;
    }

    public boolean removeEssence(long amount) {
        if (essence < amount) return false;
        essence -= amount;
        dirty = true;
        return true;
    }

    // ===== GEMS =====

    public long getGems() { return gems; }

    public void setGems(long gems) {
        long capped = Math.max(0, Math.min(gems, MAX_GEMS));
        if (this.gems != capped) { this.gems = capped; dirty = true; }
    }

    public void addGems(long amount) {
        if (amount == 0) return;
        gems = Math.min(gems + amount, MAX_GEMS);
        dirty = true; notifyDisplay();
    }

    public boolean removeGems(long amount) {
        if (gems < amount) return false;
        gems -= amount;
        dirty = true;
        return true;
    }

    public long getTotalEssenceEarned() { return totalEssenceEarned; }

    public void setTotalEssenceEarned(long value) {
        if (this.totalEssenceEarned != value) { this.totalEssenceEarned = value; dirty = true; }
    }

    // ===== RANG =====

    public PlayerRank getRank() { return rank; }

    public void setRank(PlayerRank rank) {
        if (this.rank != rank) { this.rank = rank; dirty = true; }
    }

    // ===== GRADE =====

    public PlayerGrade getGrade() { return grade; }

    public void setGrade(PlayerGrade grade) {
        if (this.grade != grade) { this.grade = grade; dirty = true; notifyDisplay(); }
    }

    // ===== NIVEAU GLOBAL =====

    public int  getLevel() { return level; }
    public long getXp()    { return xp; }

    public void setLevel(int level) {
        if (this.level != level) { this.level = level; dirty = true; notifyDisplay(); }
    }

    public void setXp(long xp) {
        if (this.xp != xp) { this.xp = xp; dirty = true; }
    }

    public void addXp(long amount) {
        if (amount == 0) return;
        xp += amount;
        dirty = true;
    }

    public long getXpRequired() {
        return (long) (100 * Math.pow(level, 1.5));
    }

    // ===== MÉTIERS =====

    public int getJobLevel(PlayerJob job) {
        return jobLevels.getOrDefault(job, 1);
    }

    public long getJobXp(PlayerJob job) {
        return jobXp.getOrDefault(job, 0L);
    }

    public void setJobLevel(PlayerJob job, int level) {
        Integer prev = jobLevels.get(job);
        if (prev == null || prev != level) { jobLevels.put(job, level); dirty = true; }
    }

    public void setJobXp(PlayerJob job, long xp) {
        Long prev = jobXp.get(job);
        if (prev == null || prev != xp) { jobXp.put(job, xp); dirty = true; }
    }

    public void addJobXp(PlayerJob job, long amount) {
        if (amount == 0) return;
        jobXp.merge(job, amount, Long::sum);
        dirty = true;
    }

    public long getJobXpRequired(PlayerJob job) {
        return (long) (50 * Math.pow(getJobLevel(job), 1.4));
    }

    public Map<PlayerJob, Integer> getJobLevels() { return jobLevels; }
    public Map<PlayerJob, Long>    getJobXpMap()  { return jobXp; }

    // ===== STATS =====

    public long getMobsKilled()   { return mobsKilled; }
    public long getBlocksPlaced() { return blocksPlaced; }
    public long getBlocksBroken() { return blocksBroken; }
    public long getItemsCrafted() { return itemsCrafted; }

    public void incrementMobsKilled()   { mobsKilled++;   dirty = true; }
    public void incrementBlocksPlaced() { blocksPlaced++;  dirty = true; }
    public void incrementBlocksBroken() { blocksBroken++;  dirty = true; }
    public void incrementItemsCrafted() { itemsCrafted++;  dirty = true; }

    public void setMobsKilled(long v)   { mobsKilled = v;   dirty = true; }
    public void setBlocksPlaced(long v) { blocksPlaced = v;  dirty = true; }
    public void setBlocksBroken(long v) { blocksBroken = v;  dirty = true; }
    public void setItemsCrafted(long v) { itemsCrafted = v;  dirty = true; }

    public long getFishCaught()          { return fishCaught; }
    public void incrementFishCaught()    { fishCaught++;   dirty = true; }
    public void setFishCaught(long v)    { fishCaught = v;  dirty = true; }

    public long getLogsCut()             { return logsCut; }
    public void incrementLogsCut()       { logsCut++;      dirty = true; }
    public void setLogsCut(long v)       { logsCut = v;     dirty = true; }

    public long getPlaytimeTicks()       { return playtimeTicks; }
    public void addPlaytimeTicks(long t) { if (t > 0) { playtimeTicks += t; dirty = true; } }
    public void setPlaytimeTicks(long v) { playtimeTicks = v; dirty = true; }
    public long getPlaytimeMinutes()     { return playtimeTicks / 1200L; }

    public long getTotalCoinsEarned()       { return totalCoinsEarned; }
    public void addTotalCoinsEarned(long v) { if (v > 0) { totalCoinsEarned += v; dirty = true; } }
    public void setTotalCoinsEarned(long v) { totalCoinsEarned = v; dirty = true; }

    public long getCropsBroken()           { return cropsBroken; }
    public void incrementCropsBroken()     { cropsBroken++;  dirty = true; }
    public void addCropsBroken(int amount) { if (amount > 0) { cropsBroken += amount; dirty = true; } }
    public void setCropsBroken(long v)     { cropsBroken = v; dirty = true; }
}