package be.RedSwick.skyblock.island;

import org.bukkit.Location;

import java.util.*;

public class Island {

    private final UUID     owner;
    private final Location center;
    private int            radius = 50;

    // ── Nom de l'île ──
    private String name = null;

    // ── Home (point de tp personnel du chef) ──
    private Location homeLocation = null;

    // ── Membres & rôles ──
    private final Map<UUID, IslandRole> members     = new HashMap<>();
    private final Map<IslandPermission, IslandRole> permissions = new HashMap<>();

    // ── Coops (accès temporaire sans membership) ──
    private final Set<UUID> coopPlayers = new HashSet<>();

    // ── Niveau île ──
    private double isLevel = 0.0;
    private final Map<String, Integer> valueBlockCounts = new HashMap<>();

    // ── Warp & accès ──
    private Location warpLocation = null;
    private boolean  isOpen       = false;

    // ── Bannis ──
    private final Set<UUID> bannedPlayers = new HashSet<>();

    // ── Flags (comportement île) ──
    private final Map<String, Boolean> flags = new HashMap<>();

    // ── Upgrades (niveau par upgrade) ──
    private final Map<String, Integer> upgrades = new HashMap<>();

    // ── TeamChat ──
    private boolean teamChatEnabled = false;

    // ── Missions ──
    private final Map<String, Integer> missionProgress  = new HashMap<>();
    private final Set<String>          completedMissions = new HashSet<>();

    // ════════════════════════════════════════════════
    //  CONSTRUCTEUR
    // ════════════════════════════════════════════════

    public Island(UUID owner, Location center) {
        this.owner  = owner;
        this.center = center;

        // Permissions par défaut — tout réservé au CHEF
        for (IslandPermission perm : IslandPermission.values()) {
            permissions.put(perm, IslandRole.CHEF);
        }
        // Flags par défaut
        for (IslandFlag flag : IslandFlag.values()) {
            flags.put(flag.name(), flag.getDefaultValue());
        }
        // Upgrades niveau 0
        for (IslandUpgrade upgrade : IslandUpgrade.values()) {
            upgrades.put(upgrade.name(), 0);
        }
    }

    // ════════════════════════════════════════════════
    //  BASE
    // ════════════════════════════════════════════════

    public UUID     getOwner()  { return owner; }
    public Location getCenter() { return center; }
    public int      getRadius() { return radius; }
    public void     setRadius(int r) { this.radius = r; }

    // ── Nom ──
    public String  getName()           { return name; }
    public void    setName(String n)   { this.name = n; }
    public boolean hasName()           { return name != null && !name.isEmpty(); }
    public String  getDisplayName() {
        return hasName() ? name : "§7L'île de §e" + owner;
    }

    // ── Home ──
    public Location getHomeLocation() { return homeLocation != null ? homeLocation : warpLocation; }
    public void     setHomeLocation(Location loc) { this.homeLocation = loc; }
    public boolean  hasHome()         { return homeLocation != null; }

    // ════════════════════════════════════════════════
    //  MEMBRES & RÔLES
    // ════════════════════════════════════════════════

    public IslandRole getRole(UUID uuid) {
        if (uuid.equals(owner)) return IslandRole.CHEF;
        if (coopPlayers.contains(uuid)) return IslandRole.COOP;
        return members.getOrDefault(uuid, null);
    }

    public void addMember(UUID uuid, IslandRole role) {
        coopPlayers.remove(uuid); // retirer des coops si promu membre
        members.put(uuid, role);
    }
    public void removeMember(UUID uuid) { members.remove(uuid); }

    public boolean isMember(UUID uuid) {
        return uuid.equals(owner) || members.containsKey(uuid);
    }

    public boolean isAnyone(UUID uuid) {
        return isMember(uuid) || coopPlayers.contains(uuid);
    }

    public int getMemberCount() { return 1 + members.size(); } // owner + membres
    public int getMemberLimit() { return getUpgradeValue(IslandUpgrade.MEMBER_LIMIT); }

    public Map<UUID, IslandRole> getMembers() { return members; }

    public List<UUID> getAllMembers() {
        List<UUID> list = new ArrayList<>();
        list.add(owner);
        list.addAll(members.keySet());
        return list;
    }

    // ── Coops ──
    public void    addCoop(UUID uuid)    { if (!isMember(uuid)) coopPlayers.add(uuid); }
    public void    removeCoop(UUID uuid) { coopPlayers.remove(uuid); }
    public boolean isCoop(UUID uuid)     { return coopPlayers.contains(uuid); }
    public Set<UUID> getCoops()          { return coopPlayers; }

    // ════════════════════════════════════════════════
    //  PERMISSIONS
    // ════════════════════════════════════════════════

    public IslandRole getPermissionRole(IslandPermission perm) {
        return permissions.getOrDefault(perm, IslandRole.CHEF);
    }

    public void setPermissionRole(IslandPermission perm, IslandRole role) {
        permissions.put(perm, role);
    }

    public Map<IslandPermission, IslandRole> getPermissions() { return permissions; }

    public boolean hasPermission(UUID uuid, IslandPermission permission) {
        IslandRole playerRole = getRole(uuid);
        IslandRole required   = permissions.getOrDefault(permission, IslandRole.CHEF);
        if (playerRole == null) return false;
        return playerRole.ordinal() <= required.ordinal();
    }

    // ════════════════════════════════════════════════
    //  FLAGS
    // ════════════════════════════════════════════════

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

    // ════════════════════════════════════════════════
    //  UPGRADES
    // ════════════════════════════════════════════════

    public int getUpgradeLevel(IslandUpgrade upgrade) {
        return upgrades.getOrDefault(upgrade.name(), 0);
    }

    public int getUpgradeValue(IslandUpgrade upgrade) {
        return upgrade.getValue(getUpgradeLevel(upgrade));
    }

    public boolean canUpgrade(IslandUpgrade upgrade) {
        return getUpgradeLevel(upgrade) < upgrade.getMaxLevel();
    }

    public void setUpgradeLevel(IslandUpgrade upgrade, int level) {
        upgrades.put(upgrade.name(), Math.min(level, upgrade.getMaxLevel()));
    }

    public void incrementUpgrade(IslandUpgrade upgrade) {
        int current = getUpgradeLevel(upgrade);
        if (current < upgrade.getMaxLevel())
            upgrades.put(upgrade.name(), current + 1);
    }

    public Map<String, Integer> getUpgrades() { return upgrades; }

    // ════════════════════════════════════════════════
    //  NIVEAU ÎLE
    // ════════════════════════════════════════════════

    public double getIsLevel() { return isLevel; }
    public void   setIsLevel(double level) { this.isLevel = level; }

    public void recalculateLevel() {
        double total = 0.0;
        for (IslandValueBlock ivb : IslandValueBlock.values()) {
            int count     = valueBlockCounts.getOrDefault(ivb.getMaterial().name(), 0);
            int effective = Math.min(count, ivb.getLimit());
            total += effective * ivb.getPoints();
        }
        for (String mName : completedMissions) {
            try {
                IslandMission m = IslandMission.valueOf(mName);
                total += m.getIsLevelReward();
            } catch (IllegalArgumentException ignored) {}
        }
        this.isLevel = total;
    }

    public int getValueBlockCount(IslandValueBlock ivb) {
        return valueBlockCounts.getOrDefault(ivb.getMaterial().name(), 0);
    }

    public void setValueBlockCount(IslandValueBlock ivb, int count) {
        valueBlockCounts.put(ivb.getMaterial().name(), Math.max(0, count));
    }

    public void incrementValueBlock(IslandValueBlock ivb) {
        valueBlockCounts.merge(ivb.getMaterial().name(), 1, Integer::sum);
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

    public Location getWarpLocation()             { return warpLocation; }
    public void     setWarpLocation(Location loc) { this.warpLocation = loc; }
    public boolean  hasWarp()                     { return warpLocation != null; }
    public boolean  isOpen()                      { return isOpen; }
    public void     setOpen(boolean open)         { this.isOpen = open; }

    // ════════════════════════════════════════════════
    //  BAN
    // ════════════════════════════════════════════════

    public void    banPlayer(UUID uuid)   { bannedPlayers.add(uuid); }
    public void    unbanPlayer(UUID uuid) { bannedPlayers.remove(uuid); }
    public boolean isBanned(UUID uuid)    { return bannedPlayers.contains(uuid); }
    public Set<UUID> getBannedPlayers()   { return bannedPlayers; }

    // ════════════════════════════════════════════════
    //  TEAMCHAT
    // ════════════════════════════════════════════════

    public boolean isTeamChatEnabled() { return teamChatEnabled; }
    public void setTeamChatEnabled(boolean enabled) { this.teamChatEnabled = enabled; }

    // ════════════════════════════════════════════════
    //  MISSIONS
    // ════════════════════════════════════════════════

    public int getMissionProgress(IslandMission mission) {
        return missionProgress.getOrDefault(mission.name(), 0);
    }

    public void addMissionProgress(IslandMission mission, int amount) {
        if (completedMissions.contains(mission.name())) return;
        int current = missionProgress.getOrDefault(mission.name(), 0);
        int newVal  = current + amount;
        missionProgress.put(mission.name(), newVal);

        if (newVal >= mission.getTarget().required()) {
            completedMissions.add(mission.name());
            missionProgress.put(mission.name(), mission.getTarget().required());
            this.isLevel += mission.getIsLevelReward();
        }
    }

    public boolean isMissionCompleted(IslandMission mission) {
        return completedMissions.contains(mission.name());
    }

    public Map<String, Integer> getMissionProgressMap() { return missionProgress; }
    public Set<String>          getCompletedMissions()  { return completedMissions; }

    // ════════════════════════════════════════════════
    //  VISITEURS
    // ════════════════════════════════════════════════

    public boolean isVisitor(UUID uuid) { return !isAnyone(uuid); }
}