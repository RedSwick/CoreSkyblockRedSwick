package be.RedSwick.skyblock.island;

import org.bukkit.Location;

import java.util.*;

public class Island {

    private final UUID     owner;
    private final Location center;
    private int            radius = 50;

    // ── Membres & rôles ──
    private final Map<UUID, IslandRole> members     = new HashMap<>();
    private final Map<IslandPermission, IslandRole> permissions = new HashMap<>();

    // ── Niveau île ──
    private double isLevel = 0.0;
    // Compteur de blocs de valeur posés (Material.name() → count)
    private final Map<String, Integer> valueBlockCounts = new HashMap<>();

    // ── Warp & accès ──
    private Location warpLocation  = null;
    private Location homeLocation  = null;   // point de téléportation home
    private boolean  isOpen        = false;
    private String   name          = null;   // nom personnalisé de l'île

    // ── Bannis ──
    private final Set<UUID> bannedPlayers = new HashSet<>();

    // ── Constructeur ──
    public Island(UUID owner, Location center) {
        this.owner  = owner;
        this.center = center;

        for (IslandPermission perm : IslandPermission.values()) {
            permissions.put(perm, IslandRole.CHEF);
        }
    }

    // ════════════════════════════════════════════════
    //  BASE
    // ════════════════════════════════════════════════

    public UUID     getOwner()  { return owner; }
    public Location getCenter() { return center; }
    public int      getRadius() { return radius; }
    public void     setRadius(int radius) { this.radius = radius; }

    // ════════════════════════════════════════════════
    //  MEMBRES & RÔLES
    // ════════════════════════════════════════════════

    public IslandRole getRole(UUID uuid) {
        if (uuid.equals(owner)) return IslandRole.CHEF;
        return members.getOrDefault(uuid, null);
    }

    public void addMember(UUID uuid, IslandRole role) { members.put(uuid, role); }
    public void removeMember(UUID uuid)               { members.remove(uuid); }

    public boolean isMember(UUID uuid) {
        return uuid.equals(owner) || members.containsKey(uuid);
    }

    public Map<UUID, IslandRole> getMembers() { return members; }

    public List<UUID> getAllMembers() {
        List<UUID> list = new ArrayList<>();
        list.add(owner);
        list.addAll(members.keySet());
        return list;
    }

    // ════════════════════════════════════════════════
    //  PERMISSIONS
    // ════════════════════════════════════════════════

    public IslandRole getPermissionRole(IslandPermission perm) {
        return permissions.get(perm);
    }

    public void setPermissionRole(IslandPermission perm, IslandRole role) {
        permissions.put(perm, role);
    }

    public Map<IslandPermission, IslandRole> getPermissions() { return permissions; }

    public boolean hasPermission(UUID uuid, IslandPermission permission) {
        IslandRole playerRole = getRole(uuid);
        IslandRole required   = permissions.get(permission);
        if (playerRole == null || required == null) return false;
        return playerRole.ordinal() <= required.ordinal();
    }

    // ════════════════════════════════════════════════
    //  NIVEAU ÎLE
    // ════════════════════════════════════════════════

    public double getIsLevel() { return isLevel; }
    public void   setIsLevel(double level) { this.isLevel = level; }

    /**
     * Recalcule le niveau île à partir des compteurs de blocs.
     * Appelé par /is recalc ou après chaque pose/casse de bloc de valeur.
     */
    public void recalculateLevel() {
        double total = 0.0;
        for (IslandValueBlock ivb : IslandValueBlock.values()) {
            int count = valueBlockCounts.getOrDefault(ivb.getMaterial().name(), 0);
            int effective = Math.min(count, ivb.getLimit());
            total += effective * ivb.getPoints();
        }
        this.isLevel = total;
    }

    // ── Compteurs de blocs de valeur ──

    public int getValueBlockCount(IslandValueBlock ivb) {
        return valueBlockCounts.getOrDefault(ivb.getMaterial().name(), 0);
    }

    public void setValueBlockCount(IslandValueBlock ivb, int count) {
        valueBlockCounts.put(ivb.getMaterial().name(), Math.max(0, count));
    }

    public void incrementValueBlock(IslandValueBlock ivb) {
        String key = ivb.getMaterial().name();
        valueBlockCounts.merge(key, 1, Integer::sum);
        recalculateLevel();
    }

    public void decrementValueBlock(IslandValueBlock ivb) {
        String key = ivb.getMaterial().name();
        int current = valueBlockCounts.getOrDefault(key, 0);
        valueBlockCounts.put(key, Math.max(0, current - 1));
        recalculateLevel();
    }

    public Map<String, Integer> getValueBlockCounts() { return valueBlockCounts; }

    // ════════════════════════════════════════════════
    //  WARP & OPEN/CLOSE
    // ════════════════════════════════════════════════

    public Location getWarpLocation()  { return warpLocation; }
    public void     setWarpLocation(Location loc) { this.warpLocation = loc; }
    public boolean  hasWarp()          { return warpLocation != null; }

    public Location getHomeLocation()  { return homeLocation; }
    public void     setHomeLocation(Location loc) { this.homeLocation = loc; }
    public boolean  hasHome()          { return homeLocation != null; }

    public String   getName()          { return name; }
    public void     setName(String n)  { this.name = n; }
    public boolean  hasName()          { return name != null && !name.isEmpty(); }

    public boolean isOpen()            { return isOpen; }
    public void    setOpen(boolean open) { this.isOpen = open; }

    // ════════════════════════════════════════════════
    //  BAN
    // ════════════════════════════════════════════════

    public void    banPlayer(UUID uuid)   { bannedPlayers.add(uuid); }
    public void    unbanPlayer(UUID uuid) { bannedPlayers.remove(uuid); }
    public boolean isBanned(UUID uuid)    { return bannedPlayers.contains(uuid); }
    public Set<UUID> getBannedPlayers()   { return bannedPlayers; }

    // ════════════════════════════════════════════════
    //  MISSIONS
    // ════════════════════════════════════════════════

    // Progression par mission (nom enum → count)
    private final Map<String, Integer> missionProgress  = new HashMap<>();
    // Missions complétées
    private final Set<String>          completedMissions = new HashSet<>();

    public int getMissionProgress(IslandMission mission) {
        return missionProgress.getOrDefault(mission.name(), 0);
    }

    public void addMissionProgress(IslandMission mission, int amount) {
        if (completedMissions.contains(mission.name())) return;
        int current  = missionProgress.getOrDefault(mission.name(), 0);
        int newVal   = current + amount;
        missionProgress.put(mission.name(), newVal);

        // Vérifie si complétée
        if (newVal >= mission.getTarget().required()) {
            completedMissions.add(mission.name());
            missionProgress.put(mission.name(), mission.getTarget().required());
            // Ajoute le IS level de récompense
            this.isLevel += mission.getIsLevelReward();
        }
    }

    public boolean isMissionCompleted(IslandMission mission) {
        return completedMissions.contains(mission.name());
    }

    public Map<String, Integer> getMissionProgressMap()  { return missionProgress; }
    public Set<String>          getCompletedMissions()   { return completedMissions; }

    // ════════════════════════════════════════════════
    //  VISITEURS (joueurs présents sur l'île non membres)
    // ════════════════════════════════════════════════

    /**
     * Un visiteur est un joueur sur l'île qui n'est pas membre.
     * Les visiteurs n'ont aucune permission par défaut.
     */
    public boolean isVisitor(UUID uuid) {
        return !isMember(uuid);
    }
    // ════════════════════════════════════════════════
    //  UPGRADES
    // ════════════════════════════════════════════════

    private final Map<String, Integer> upgrades = new HashMap<>();

    // ── Flags ──
    private final Map<String, Boolean> flags = new HashMap<>();

    public boolean getFlag(IslandFlag flag) {
        return flags.getOrDefault(flag.name(), flag.getDefaultValue());
    }

    public void setFlag(IslandFlag flag, boolean value) {
        flags.put(flag.name(), value);
    }

    public void toggleFlag(IslandFlag flag) {
        flags.put(flag.name(), !getFlag(flag));
    }

    public Map<String, Boolean> getFlags() { return flags; }

    public int getUpgradeLevel(IslandUpgrade upgrade) {
        return upgrades.getOrDefault(upgrade.name(), 0);
    }

    public int getUpgradeValue(IslandUpgrade upgrade) {
        return upgrade.getValue(getUpgradeLevel(upgrade));
    }

    public boolean canUpgrade(IslandUpgrade upgrade) {
        return getUpgradeLevel(upgrade) < upgrade.getMaxLevel();
    }

    public void incrementUpgrade(IslandUpgrade upgrade) {
        int current = getUpgradeLevel(upgrade);
        if (current < upgrade.getMaxLevel())
            upgrades.put(upgrade.name(), current + 1);
    }

    public void setUpgradeLevel(IslandUpgrade upgrade, int level) {
        upgrades.put(upgrade.name(), Math.max(0, Math.min(level, upgrade.getMaxLevel())));
    }

    public Map<String, Integer> getUpgrades() { return upgrades; }

    // ── Coops (accès temporaire, non membres permanents) ──
    private final Set<UUID> coops = new HashSet<>();

    public void    addCoop(UUID uuid)    { coops.add(uuid); }
    public void    removeCoop(UUID uuid) { coops.remove(uuid); }
    public boolean isCoop(UUID uuid)     { return coops.contains(uuid); }
    public Set<UUID> getCoops()          { return coops; }

    // ── Membres helpers ──
    public int getMemberCount() {
        return 1 + members.size(); // owner + membres
    }

    public int getMemberLimit() {
        return getUpgradeValue(IslandUpgrade.MEMBER_LIMIT);
    }
}