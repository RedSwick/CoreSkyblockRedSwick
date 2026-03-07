package be.RedSwick.skyblock.manager;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.island.*;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * IslandManager — Version étendue.
 * Nouveaux champs persistés : name, homeLocation, coops, flags, upgrades.
 */
public class IslandManager {

    private final Map<UUID, Island> islands     = new HashMap<>();
    private final Map<UUID, UUID>   invitations = new HashMap<>();

    private File              file;
    private FileConfiguration config;

    private static final int SPACING = 1000;
    private final Set<UUID>  deletingIslands = new HashSet<>();
    private int              gridIndex = 0;

    public IslandManager() {
        loadFile();
        loadIslands();
    }

    // ════════════════════════════════════════════════
    //  FILE
    // ════════════════════════════════════════════════

    private void loadFile() {
        file = new File(SkyBlockPlugin.getInstance().getDataFolder(), "islands.yml");
        if (!file.exists()) {
            try { file.getParentFile().mkdirs(); file.createNewFile(); }
            catch (Exception e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    private void saveFile() {
        try { config.save(file); }
        catch (IOException e) { e.printStackTrace(); }
    }

    // ════════════════════════════════════════════════
    //  CREATE / DELETE
    // ════════════════════════════════════════════════

    public Island createIsland(UUID owner, Location center) {
        Island island = new Island(owner, center);
        islands.put(owner, island);
        saveIsland(island);
        gridIndex++;
        return island;
    }

    public Location getNextIslandLocation(World world) {
        Set<String> usedKeys = new HashSet<>();
        for (Island island : islands.values()) {
            int gx = island.getCenter().getBlockX() / SPACING;
            int gz = island.getCenter().getBlockZ() / SPACING;
            usedKeys.add(gx + "," + gz);
        }
        for (UUID uid : deletingIslands) {
            Island di = islands.get(uid);
            if (di != null) {
                int gx = di.getCenter().getBlockX() / SPACING;
                int gz = di.getCenter().getBlockZ() / SPACING;
                usedKeys.add(gx + "," + gz);
            }
        }
        int[] pos = nextSpiralPos(usedKeys);
        return new Location(world, pos[0] * SPACING, 100, pos[1] * SPACING);
    }

    private int[] nextSpiralPos(Set<String> used) {
        int x = 0, z = 0, dx = 1, dz = 0, steps = 1, stepCount = 0, turnCount = 0;
        if (!used.contains("0,0")) return new int[]{0, 0};
        for (int i = 0; i < 10000; i++) {
            x += dx; z += dz; stepCount++;
            if (!used.contains(x + "," + z)) return new int[]{x, z};
            if (stepCount == steps) {
                stepCount = 0;
                int tmp = dx; dx = -dz; dz = tmp;
                turnCount++;
                if (turnCount % 2 == 0) steps++;
            }
        }
        return new int[]{islands.size(), 0};
    }

    public void deleteIsland(UUID owner) {
        Island island = islands.get(owner);
        if (island != null) {
            deletingIslands.add(owner);
            island.setIsLevel(0);
            island.getMissionProgressMap().clear();
            island.getCompletedMissions().clear();
            for (IslandValueBlock ivb : IslandValueBlock.values())
                island.setValueBlockCount(ivb, 0);

            be.RedSwick.skyblock.listener.PlayerDataListener.despawnIslandMobs(island);

            Location center = island.getCenter();
            World world = center.getWorld();
            int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
            int r = island.getRadius() + 20;

            int chunkMinX = (cx - r) >> 4, chunkMaxX = (cx + r) >> 4;
            int chunkMinZ = (cz - r) >> 4, chunkMaxZ = (cz + r) >> 4;
            int totalChunks = (chunkMaxX - chunkMinX + 1) * (chunkMaxZ - chunkMinZ + 1);
            final long cleanupDelay = totalChunks + 20L;
            int delay = 0;
            for (int chX = chunkMinX; chX <= chunkMaxX; chX++) {
                for (int chZ = chunkMinZ; chZ <= chunkMaxZ; chZ++) {
                    final int fcx = chX, fcz = chZ;
                    Bukkit.getScheduler().runTaskLater(SkyBlockPlugin.getInstance(), () -> {
                        Chunk chunk = world.getChunkAt(fcx, fcz);
                        for (int bx = 0; bx < 16; bx++) {
                            for (int bz = 0; bz < 16; bz++) {
                                int wx = (fcx << 4) + bx, wz = (fcz << 4) + bz;
                                if (Math.abs(wx - cx) > r || Math.abs(wz - cz) > r) continue;
                                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                                    org.bukkit.block.Block b = chunk.getBlock(bx, y, bz);
                                    if (b.getType() != Material.AIR) b.setType(Material.AIR, false);
                                }
                            }
                        }
                    }, delay++);
                }
            }
            Bukkit.getScheduler().runTaskLater(SkyBlockPlugin.getInstance(),
                    () -> deletingIslands.remove(owner), cleanupDelay);
        }
        islands.remove(owner);
        config.set(owner.toString(), null);
        saveFile();
    }

    // ════════════════════════════════════════════════
    //  SAVE
    // ════════════════════════════════════════════════

    public void saveIsland(Island island) {
        String p = island.getOwner().toString();

        // Base
        config.set(p + ".world",  island.getCenter().getWorld().getName());
        config.set(p + ".x",      island.getCenter().getBlockX());
        config.set(p + ".y",      island.getCenter().getBlockY());
        config.set(p + ".z",      island.getCenter().getBlockZ());
        config.set(p + ".radius", island.getRadius());
        config.set(p + ".isLevel", island.getIsLevel());
        config.set(p + ".open",   island.isOpen());

        // ── NOUVEAU : nom & home ──
        config.set(p + ".name", island.getName());
        if (island.hasHome()) {
            Location h = island.getHomeLocation();
            config.set(p + ".home.world", h.getWorld().getName());
            config.set(p + ".home.x",     h.getX());
            config.set(p + ".home.y",     h.getY());
            config.set(p + ".home.z",     h.getZ());
            config.set(p + ".home.yaw",   h.getYaw());
            config.set(p + ".home.pitch", h.getPitch());
        } else {
            config.set(p + ".home", null);
        }

        // Warp
        if (island.hasWarp()) {
            Location w = island.getWarpLocation();
            config.set(p + ".warp.world", w.getWorld().getName());
            config.set(p + ".warp.x",     w.getX());
            config.set(p + ".warp.y",     w.getY());
            config.set(p + ".warp.z",     w.getZ());
            config.set(p + ".warp.yaw",   w.getYaw());
            config.set(p + ".warp.pitch", w.getPitch());
        } else {
            config.set(p + ".warp", null);
        }

        // Membres
        config.set(p + ".members", null);
        for (Map.Entry<UUID, IslandRole> entry : island.getMembers().entrySet())
            config.set(p + ".members." + entry.getKey(), entry.getValue().name());

        // ── NOUVEAU : coops ──
        config.set(p + ".coops", null);
        List<String> coopList = new ArrayList<>();
        for (UUID uid : island.getCoops()) coopList.add(uid.toString());
        config.set(p + ".coops", coopList);

        // Permissions
        config.set(p + ".permissions", null);
        for (Map.Entry<IslandPermission, IslandRole> entry : island.getPermissions().entrySet())
            config.set(p + ".permissions." + entry.getKey().name(), entry.getValue().name());

        // ── NOUVEAU : flags ──
        config.set(p + ".flags", null);
        for (Map.Entry<String, Boolean> entry : island.getFlags().entrySet())
            config.set(p + ".flags." + entry.getKey(), entry.getValue());

        // ── NOUVEAU : upgrades ──
        config.set(p + ".upgrades", null);
        for (Map.Entry<String, Integer> entry : island.getUpgrades().entrySet())
            config.set(p + ".upgrades." + entry.getKey(), entry.getValue());

        // Blocs de valeur
        config.set(p + ".valueBlocks", null);
        for (Map.Entry<String, Integer> entry : island.getValueBlockCounts().entrySet())
            config.set(p + ".valueBlocks." + entry.getKey(), entry.getValue());

        // Bannis
        List<String> banned = new ArrayList<>();
        for (UUID uid : island.getBannedPlayers()) banned.add(uid.toString());
        config.set(p + ".banned", banned);

        // Missions
        config.set(p + ".missionProgress", null);
        for (var entry : island.getMissionProgressMap().entrySet())
            config.set(p + ".missionProgress." + entry.getKey(), entry.getValue());
        config.set(p + ".completedMissions", new ArrayList<>(island.getCompletedMissions()));

        saveFile();
    }

    // ════════════════════════════════════════════════
    //  LOAD
    // ════════════════════════════════════════════════

    private void loadIslands() {
        if (config.getKeys(false).isEmpty()) return;

        for (String key : config.getKeys(false)) {
            UUID  owner = UUID.fromString(key);
            World world = Bukkit.getWorld(config.getString(key + ".world", "skyblock"));
            if (world == null) {
                SkyBlockPlugin.getInstance().getLogger().warning("Monde introuvable : " + key);
                continue;
            }

            int x = config.getInt(key + ".x"), y = config.getInt(key + ".y"),
                    z = config.getInt(key + ".z"), radius = config.getInt(key + ".radius", 50);

            Island island = new Island(owner, new Location(world, x, y, z));
            island.setRadius(radius);
            island.setIsLevel(config.getDouble(key + ".isLevel", 0));
            island.setOpen(config.getBoolean(key + ".open", false));

            // ── Nom ──
            island.setName(config.getString(key + ".name", null));

            // ── Home ──
            if (config.isConfigurationSection(key + ".home")) {
                World hw = Bukkit.getWorld(config.getString(key + ".home.world", "skyblock"));
                if (hw != null) island.setHomeLocation(new Location(hw,
                        config.getDouble(key + ".home.x"), config.getDouble(key + ".home.y"),
                        config.getDouble(key + ".home.z"), (float) config.getDouble(key + ".home.yaw"),
                        (float) config.getDouble(key + ".home.pitch")));
            }

            // Warp
            if (config.isConfigurationSection(key + ".warp")) {
                World ww = Bukkit.getWorld(config.getString(key + ".warp.world", "skyblock"));
                if (ww != null) island.setWarpLocation(new Location(ww,
                        config.getDouble(key + ".warp.x"), config.getDouble(key + ".warp.y"),
                        config.getDouble(key + ".warp.z"), (float) config.getDouble(key + ".warp.yaw"),
                        (float) config.getDouble(key + ".warp.pitch")));
            }

            // Membres
            if (config.isConfigurationSection(key + ".members")) {
                for (String mk : config.getConfigurationSection(key + ".members").getKeys(false)) {
                    try { island.addMember(UUID.fromString(mk),
                            IslandRole.valueOf(config.getString(key + ".members." + mk)));
                    } catch (Exception ignored) {}
                }
            }

            // ── Coops ──
            for (String cs : config.getStringList(key + ".coops")) {
                try { island.addCoop(UUID.fromString(cs)); } catch (Exception ignored) {}
            }

            // Permissions
            if (config.isConfigurationSection(key + ".permissions")) {
                for (String pk : config.getConfigurationSection(key + ".permissions").getKeys(false)) {
                    try { island.setPermissionRole(IslandPermission.valueOf(pk),
                            IslandRole.valueOf(config.getString(key + ".permissions." + pk)));
                    } catch (Exception ignored) {}
                }
            }

            // ── Flags ──
            if (config.isConfigurationSection(key + ".flags")) {
                for (String fk : config.getConfigurationSection(key + ".flags").getKeys(false)) {
                    try { island.getFlags().put(fk, config.getBoolean(key + ".flags." + fk)); }
                    catch (Exception ignored) {}
                }
            }

            // ── Upgrades ──
            if (config.isConfigurationSection(key + ".upgrades")) {
                for (String uk : config.getConfigurationSection(key + ".upgrades").getKeys(false)) {
                    try { island.setUpgradeLevel(IslandUpgrade.valueOf(uk),
                            config.getInt(key + ".upgrades." + uk, 0));
                    } catch (Exception ignored) {}
                }
            }

            // Blocs de valeur
            if (config.isConfigurationSection(key + ".valueBlocks")) {
                for (String bk : config.getConfigurationSection(key + ".valueBlocks").getKeys(false)) {
                    try { island.setValueBlockCount(IslandValueBlock.valueOf(bk),
                            config.getInt(key + ".valueBlocks." + bk, 0));
                    } catch (Exception ignored) {}
                }
            }

            // Bannis
            for (String bs : config.getStringList(key + ".banned")) {
                try { island.banPlayer(UUID.fromString(bs)); } catch (Exception ignored) {}
            }

            // Missions
            if (config.isConfigurationSection(key + ".missionProgress")) {
                for (String mk : config.getConfigurationSection(key + ".missionProgress").getKeys(false)) {
                    try { island.getMissionProgressMap().put(mk, config.getInt(key + ".missionProgress." + mk)); }
                    catch (Exception ignored) {}
                }
            }
            for (String ms : config.getStringList(key + ".completedMissions"))
                island.getCompletedMissions().add(ms);

            // ← CRITIQUE : synchroniser le radius avec l'upgrade SIZE après chargement
            // (au cas où le radius YAML est obsolète par rapport à l'upgrade)
            int upgradeSize = island.getUpgradeValue(IslandUpgrade.SIZE);
            if (upgradeSize > island.getRadius()) {
                island.setRadius(upgradeSize);
            }

            islands.put(owner, island);
        }
        gridIndex = islands.size();
    }

    // ════════════════════════════════════════════════
    //  GETTERS
    // ════════════════════════════════════════════════

    public boolean hasIsland(UUID uuid)       { return islands.containsKey(uuid); }
    public Island  getIsland(UUID owner)      { return islands.get(owner); }
    public Collection<Island> getAllIslands()  { return islands.values(); }

    public Island getIslandByMember(UUID uuid) {
        for (Island island : islands.values())
            if (island.isMember(uuid)) return island;
        return null;
    }

    /** Cherche aussi les coops */
    public Island getIslandByAnyone(UUID uuid) {
        for (Island island : islands.values())
            if (island.isAnyone(uuid)) return island;
        return null;
    }

    public Island getIslandAtLocation(Location location) {
        if (location.getWorld() == null) return null;
        for (Island island : islands.values()) {
            Location center = island.getCenter();
            if (!center.getWorld().equals(location.getWorld())) continue;
            int dx = Math.abs(location.getBlockX() - center.getBlockX());
            int dz = Math.abs(location.getBlockZ() - center.getBlockZ());
            // getRadius() est synchronisé avec l'upgrade SIZE via setRadius() dans IslandGUIListener
            if (dx <= island.getRadius() && dz <= island.getRadius()) return island;
        }
        return null;
    }

    /** Cherche une île par son nom (case-insensitive, strip couleurs) */
    public Island getIslandByName(String name) {
        String clean = name.toLowerCase().replaceAll("§.", "");
        for (Island island : islands.values()) {
            if (!island.hasName()) continue;
            if (island.getName().toLowerCase().replaceAll("§.", "").equals(clean)) return island;
        }
        return null;
    }

    // ════════════════════════════════════════════════
    //  INVITATIONS
    // ════════════════════════════════════════════════

    public void    addInvitation(UUID target, UUID owner) { invitations.put(target, owner); }
    public UUID    getInvitation(UUID target)              { return invitations.get(target); }
    public void    removeInvitation(UUID target)           { invitations.remove(target); }
    public boolean hasInvitation(UUID target)              { return invitations.containsKey(target); }

    /**
     * Remplace une île (changement de propriétaire via /is transfer).
     * ORDRE CRITIQUE : sauvegarder la nouvelle AVANT de supprimer l'ancienne.
     */
    public void replaceIsland(UUID oldOwner, Island newIsland) {
        // 1. Enregistrer la nouvelle île dans la map
        islands.put(newIsland.getOwner(), newIsland);
        // 2. Sauvegarder la nouvelle île dans le YAML
        saveIsland(newIsland);
        // 3. Seulement APRÈS, supprimer l'ancienne entrée
        islands.remove(oldOwner);
        config.set(oldOwner.toString(), null);
        saveFile();
    }
}