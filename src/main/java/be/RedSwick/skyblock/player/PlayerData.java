package be.RedSwick.skyblock.player;

import java.util.*;

public class PlayerData {

    private final UUID uuid;

    // ===== ÉCONOMIE =====
    private long coins   = 0;
    private long essence = 0;
    private long gems    = 0;
    private long totalEssenceEarned = 0; // stat historique (jamais décrémentée)

    // ===== RANG FARMABLE =====
    private PlayerRank rank = PlayerRank.SERVITEUR;

    // ===== GRADE PAYANT =====
    private PlayerGrade grade = PlayerGrade.AUCUN;

    // ===== NIVEAU GLOBAL =====
    private int  level = 1;
    private long xp    = 0;

    // ===== MÉTIERS =====
    // Chaque joueur a un niveau + XP pour chacun des 5 métiers
    private final Map<PlayerJob, Integer> jobLevels = new EnumMap<>(PlayerJob.class);
    private final Map<PlayerJob, Long>    jobXp     = new EnumMap<>(PlayerJob.class);

    // ===== STATS DIVERSES =====
    private long mobsKilled    = 0;
    private long blocksPlaced  = 0;
    private long blocksBroken  = 0;
    private long itemsCrafted  = 0;
    private long fishCaught    = 0;
    private long logsCut       = 0;
    private long playtimeTicks = 0; // en ticks, converti en minutes pour affichage
    private long totalCoinsEarned = 0;
    private long cropsBroken      = 0;

    // ===== TEAMCHAT =====
    private boolean teamChatEnabled = false;

    // ===== CONSTRUCTEUR =====

    public PlayerData(UUID uuid) {
        this.uuid = uuid;

        // Initialise tous les métiers à niveau 1, 0 xp
        for (PlayerJob job : PlayerJob.values()) {
            jobLevels.put(job, 1);
            jobXp.put(job, 0L);
        }
    }

    // ===== UUID =====

    public UUID getUuid() { return uuid; }

    // ===== COINS =====

    public long getCoins() { return coins; }

    public void setCoins(long coins) {
        this.coins = Math.max(0, coins);
    }

    public void addCoins(long amount) {
        this.coins += amount;
    }

    public boolean removeCoins(long amount) {
        if (coins < amount) return false;
        coins -= amount;
        return true;
    }

    // ===== ESSENCE =====

    public long getEssence() { return essence; }

    public void setEssence(long essence) {
        this.essence = Math.max(0, essence);
    }

    public void addEssence(long amount) {
        this.essence += amount;
        this.totalEssenceEarned += amount;
    }

    public boolean removeEssence(long amount) {
        if (essence < amount) return false;
        essence -= amount;
        return true;
    }

    // ===== GEMMES =====

    public long getGems() { return gems; }

    public void setGems(long gems) {
        this.gems = Math.max(0, gems);
    }

    public void addGems(long amount) {
        this.gems += amount;
    }

    public boolean removeGems(long amount) {
        if (gems < amount) return false;
        gems -= amount;
        return true;
    }

    public long getTotalEssenceEarned() { return totalEssenceEarned; }

    public void setTotalEssenceEarned(long value) { this.totalEssenceEarned = value; }

    // ===== RANG =====

    public PlayerRank getRank() { return rank; }

    public void setRank(PlayerRank rank) { this.rank = rank; }

    // ===== GRADE =====

    public PlayerGrade getGrade() { return grade; }

    public void setGrade(PlayerGrade grade) { this.grade = grade; }

    // ===== NIVEAU GLOBAL =====

    public int getLevel()  { return level; }
    public long getXp()    { return xp; }

    public void setLevel(int level) { this.level = level; }
    public void setXp(long xp)     { this.xp = xp; }

    public void addXp(long amount) {
        this.xp += amount;
    }

    /**
     * XP nécessaire pour passer au niveau suivant.
     * Formule : 100 * niveau^1.5  (douce courbe de progression)
     */
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
        jobLevels.put(job, level);
    }

    public void setJobXp(PlayerJob job, long xp) {
        jobXp.put(job, xp);
    }

    public void addJobXp(PlayerJob job, long amount) {
        long current = jobXp.getOrDefault(job, 0L);
        jobXp.put(job, current + amount);
    }

    /**
     * XP nécessaire pour le prochain niveau de métier.
     * Formule : 50 * niveau^1.4  (plus rapide que le niveau global)
     */
    public long getJobXpRequired(PlayerJob job) {
        int lvl = getJobLevel(job);
        return (long) (50 * Math.pow(lvl, 1.4));
    }

    public Map<PlayerJob, Integer> getJobLevels() { return jobLevels; }
    public Map<PlayerJob, Long>    getJobXpMap()  { return jobXp; }

    // ===== STATS =====

    public long getMobsKilled()   { return mobsKilled; }
    public long getBlocksPlaced() { return blocksPlaced; }
    public long getBlocksBroken() { return blocksBroken; }
    public long getItemsCrafted() { return itemsCrafted; }

    public void incrementMobsKilled()   { mobsKilled++; }
    public void incrementBlocksPlaced() { blocksPlaced++; }
    public void incrementBlocksBroken() { blocksBroken++; }
    public void incrementItemsCrafted() { itemsCrafted++; }

    public void setMobsKilled(long v)   { mobsKilled = v; }
    public void setBlocksPlaced(long v) { blocksPlaced = v; }
    public void setBlocksBroken(long v) { blocksBroken = v; }
    public void setItemsCrafted(long v) { itemsCrafted = v; }

    // ── Stats étendues ────────────────────────────────────────────────
    public long getFishCaught()             { return fishCaught; }
    public void incrementFishCaught()       { fishCaught++; }
    public void setFishCaught(long v)       { fishCaught = v; }

    public long getLogsCut()                { return logsCut; }
    public void incrementLogsCut()          { logsCut++; }
    public void setLogsCut(long v)          { logsCut = v; }

    public long getPlaytimeTicks()          { return playtimeTicks; }
    public void addPlaytimeTicks(long t)    { playtimeTicks += t; }
    public void setPlaytimeTicks(long v)    { playtimeTicks = v; }
    public long getPlaytimeMinutes()        { return playtimeTicks / 1200L; }

    public long getTotalCoinsEarned()       { return totalCoinsEarned; }
    public void addTotalCoinsEarned(long v) { if (v > 0) totalCoinsEarned += v; }
    public void setTotalCoinsEarned(long v) { totalCoinsEarned = v; }

    public long getCropsBroken()            { return cropsBroken; }
    public void incrementCropsBroken()      { cropsBroken++; }
    public void addCropsBroken(int amount)  { cropsBroken += amount; }
    public void setCropsBroken(long v)      { cropsBroken = v; }

    public boolean isTeamChatEnabled()            { return teamChatEnabled; }
    public void    setTeamChatEnabled(boolean v)  { this.teamChatEnabled = v; }
}