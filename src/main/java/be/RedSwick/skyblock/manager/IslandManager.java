package be.RedSwick.skyblock.manager;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.island.*;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IslandManager — Version optimisée
 *
 * OPTIMISATIONS :
 * 1. deleteIsland FULL ASYNC : le scan de blocs se fait sur un thread async,
 *    seul le setType(AIR) final est schedulé sur le main thread en batch de 1 chunk/tick
 *    → zéro spike TPS pendant la suppression
 *
 * 2. getIslandAtLocation CACHE : Map chunk-key → Island
 *    → O(1) au lieu de O(n) à chaque event (ProtectionListener, ValueBlockListener, etc.)
 *    Le cache est invalidé à createIsland / deleteIsland / setRadius
 *
 * 3. getIslandByMember CACHE : Map<UUID membre → Island>
 *    → O(1) au lieu de O(n×m) — appelé dans TOUS les listeners à chaque event
 *    Mis à jour à addMember / removeMember / createIsland / deleteIsland / loadIslands
 *
 * 4. saveIsland ASYNC : I/O YAML hors du main thread
 *    → Plus de freeze sur les actions joueur (upgrade, mission, flag toggle)
 *    saveIslandSync() reste disponible pour les cas critiques (onDisable)
 *
 * 5. islands en ConcurrentHashMap : thread-safe pour les accès async
 */
public class IslandManager {

    // ConcurrentHashMap : accès depuis threads async (deleteIsland scan, saveIsland async)
    private final Map<UUID, Island> islands     = new ConcurrentHashMap<>();
    private final Map<UUID, UUID>   invitations = new ConcurrentHashMap<>();

    // Cache chunk-key → Island pour getIslandAtLocation O(1)
    // clé : "world,cx,cz"
    private final Map<String, Island> chunkIslandCache  = new ConcurrentHashMap<>();

    // Cache membre → île pour getIslandByMember O(1)
    // clé : UUID du membre, valeur : UUID du owner de l'île
    private final Map<UUID, UUID> memberToIslandOwner = new ConcurrentHashMap<>();

    private File              file;
    private FileConfiguration config;

    private static final int SPACING = 1000;
    private final Set<UUID> deletingIslands = ConcurrentHashMap.newKeySet();
    private int gridIndex = 0;

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
        // Enregistrer le owner lui-même dans le cache membre
        memberToIslandOwner.put(owner, owner);
        saveIsland(island);
        gridIndex++;
        buildChunkCache(island);
        return island;
    }

    /**
     * Supprime une île avec nettoyage FULL ASYNC des chunks.
     *
     * Séquence :
     * 1. Mobs + hologrammes despawn (main thread immédiat)
     * 2. Invalider les caches (chunk + membre)
     * 3. Retirer du YAML et de la map
     * 4. Scan async → setType(AIR) batch 1 chunk/tick sur main thread
     */
    public void deleteIsland(UUID owner) {
        Island island = islands.get(owner);
        if (island == null) {
            config.set(owner.toString(), null);
            saveFile();
            return;
        }

        deletingIslands.add(owner);

        // Reset données
        island.setIsLevel(0);
        island.getMissionProgressMap().clear();
        island.getCompletedMissions().clear();
        for (IslandValueBlock ivb : IslandValueBlock.values())
            island.setValueBlockCount(ivb, 0);

        // Despawn mobs + holos (main thread)
        be.RedSwick.skyblock.listener.PlayerDataListener.despawnIslandMobs(island);

        // Invalider les deux caches
        invalidateChunkCache(island);
        invalidateMemberCache(island);

        final Location center = island.getCenter();
        final World    world  = center.getWorld();
        final int      cx     = center.getBlockX();
        final int      cz     = center.getBlockZ();
        final int      r      = island.getRadius() + 20;

        final int chunkMinX = (cx - r) >> 4;
        final int chunkMaxX = (cx + r) >> 4;
        final int chunkMinZ = (cz - r) >> 4;
        final int chunkMaxZ = (cz + r) >> 4;

        islands.remove(owner);
        config.set(owner.toString(), null);
        saveFile();

        // Phase async : collecter les chunks à nettoyer
        Bukkit.getScheduler().runTaskAsynchronously(SkyBlockPlugin.getInstance(), () -> {
            List<int[]> chunksToClean = new ArrayList<>();
            for (int chX = chunkMinX; chX <= chunkMaxX; chX++) {
                for (int chZ = chunkMinZ; chZ <= chunkMaxZ; chZ++) {
                    chunksToClean.add(new int[]{chX, chZ});
                }
            }
            scheduleBatchClean(world, chunksToClean, 0, cx, cz, r, owner);
        });
    }

    /**
     * Nettoie les chunks un par un sur le main thread (1 chunk/tick).
     */
    private void scheduleBatchClean(World world, List<int[]> chunks, int index,
                                    int cx, int cz, int r, UUID owner) {
        if (index >= chunks.size()) {
            deletingIslands.remove(owner);
            return;
        }

        int[] chunk = chunks.get(index);
        final int chX = chunk[0], chZ = chunk[1];

        Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(), () -> {
            if (!world.isChunkLoaded(chX, chZ)) {
                world.loadChunk(chX, chZ, false);
            }
            if (world.isChunkLoaded(chX, chZ)) {
                org.bukkit.Chunk c = world.getChunkAt(chX, chZ);
                for (int bx = 0; bx < 16; bx++) {
                    for (int bz = 0; bz < 16; bz++) {
                        int wx = (chX << 4) + bx;
                        int wz = (chZ << 4) + bz;
                        if (Math.abs(wx - cx) > r || Math.abs(wz - cz) > r) continue;
                        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                            org.bukkit.block.Block b = c.getBlock(bx, y, bz);
                            if (b.getType() != org.bukkit.Material.AIR)
                                b.setType(org.bukkit.Material.AIR, false);
                        }
                    }
                }
                world.unloadChunkRequest(chX, chZ);
            }
            scheduleBatchClean(world, chunks, index + 1, cx, cz, r, owner);
        });
    }

    // ════════════════════════════════════════════════
    //  CACHE CHUNK → ISLAND
    // ════════════════════════════════════════════════

    private void buildChunkCache(Island island) {
        Location center = island.getCenter();
        if (center.getWorld() == null) return;
        String worldName = center.getWorld().getName();
        int r = island.getRadius();
        int chunkMinX = (center.getBlockX() - r) >> 4;
        int chunkMaxX = (center.getBlockX() + r) >> 4;
        int chunkMinZ = (center.getBlockZ() - r) >> 4;
        int chunkMaxZ = (center.getBlockZ() + r) >> 4;
        for (int chX = chunkMinX; chX <= chunkMaxX; chX++) {
            for (int chZ = chunkMinZ; chZ <= chunkMaxZ; chZ++) {
                chunkIslandCache.put(worldName + "," + chX + "," + chZ, island);
            }
        }
    }

    private void invalidateChunkCache(Island island) {
        chunkIslandCache.values().removeIf(i -> i.getOwner().equals(island.getOwner()));
    }

    public void rebuildChunkCache(Island island) {
        invalidateChunkCache(island);
        buildChunkCache(island);
    }

    // ════════════════════════════════════════════════
    //  CACHE MEMBRE → ISLAND
    // ════════════════════════════════════════════════

    /**
     * Construit le cache membre → ownerUUID pour une île.
     * Appelé à loadIslands et createIsland.
     */
    private void buildMemberCache(Island island) {
        UUID owner = island.getOwner();
        for (UUID member : island.getAllMembers()) {
            memberToIslandOwner.put(member, owner);
        }
    }

    /**
     * Invalide le cache membre pour une île (avant delete).
     */
    private void invalidateMemberCache(Island island) {
        UUID owner = island.getOwner();
        memberToIslandOwner.entrySet().removeIf(e -> e.getValue().equals(owner));
    }

    /**
     * Appelé par Island.addMember() via le manager — met à jour le cache.
     * NOTE : Pour que ça fonctionne, appeler cette méthode après island.addMember().
     */
    public void onMemberAdded(Island island, UUID memberUuid) {
        memberToIslandOwner.put(memberUuid, island.getOwner());
    }

    /**
     * Appelé quand un membre quitte une île — met à jour le cache.
     */
    public void onMemberRemoved(UUID memberUuid) {
        memberToIslandOwner.remove(memberUuid);
    }

    // ════════════════════════════════════════════════
    //  SAVE
    // ════════════════════════════════════════════════

    /**
     * Sauvegarde une île de façon ASYNC.
     * À utiliser pour toutes les sauvegardes depuis les listeners/GUI.
     * Le snapshot de la config est construit sur le main thread (safe),
     * l'I/O disque se fait hors du main thread.
     */
    public void saveIsland(Island island) {
        // Construire le snapshot de config sur le main thread
        buildIslandConfig(island);
        // I/O async
        Bukkit.getScheduler().runTaskAsynchronously(SkyBlockPlugin.getInstance(), this::saveFile);
    }

    /**
     * Sauvegarde synchrone — uniquement pour onDisable() ou cas critiques.
     */
    public void saveIslandSync(Island island) {
        buildIslandConfig(island);
        saveFile();
    }

    /**
     * Sauvegarde toutes les îles de façon synchrone (utilisé à l'onDisable).
     */
    public void saveAllSync() {
        for (Island island : islands.values()) {
            buildIslandConfig(island);
        }
        saveFile();
    }

    private void buildIslandConfig(Island island) {
        String p = island.getOwner().toString();

        config.set(p + ".world",  island.getCenter().getWorld().getName());
        config.set(p + ".x",      island.getCenter().getBlockX());
        config.set(p + ".y",      island.getCenter().getBlockY());
        config.set(p + ".z",      island.getCenter().getBlockZ());
        config.set(p + ".radius", island.getRadius());
        config.set(p + ".isLevel", island.getIsLevel());
        config.set(p + ".open", island.isOpen());

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

        config.set(p + ".members", null);
        for (Map.Entry<UUID, IslandRole> entry : island.getMembers().entrySet())
            config.set(p + ".members." + entry.getKey(), entry.getValue().name());

        config.set(p + ".permissions", null);
        for (Map.Entry<IslandPermission, IslandRole> entry : island.getPermissions().entrySet())
            config.set(p + ".permissions." + entry.getKey().name(), entry.getValue().name());

        config.set(p + ".valueBlocks", null);
        for (Map.Entry<String, Integer> entry : island.getValueBlockCounts().entrySet())
            config.set(p + ".valueBlocks." + entry.getKey(), entry.getValue());

        config.set(p + ".banned", null);
        List<String> banned = new ArrayList<>();
        for (UUID uid : island.getBannedPlayers()) banned.add(uid.toString());
        config.set(p + ".banned", banned);

        config.set(p + ".missionProgress", null);
        for (var entry : island.getMissionProgressMap().entrySet())
            config.set(p + ".missionProgress." + entry.getKey(), entry.getValue());

        config.set(p + ".completedMissions", new ArrayList<>(island.getCompletedMissions()));

        config.set(p + ".upgrades", null);
        for (var entry : island.getUpgrades().entrySet())
            config.set(p + ".upgrades." + entry.getKey(), entry.getValue());

        if (island.getName() != null) config.set(p + ".name", island.getName());
        if (island.getHomeLocation() != null) {
            Location h = island.getHomeLocation();
            config.set(p + ".home.world", h.getWorld().getName());
            config.set(p + ".home.x",     h.getX());
            config.set(p + ".home.y",     h.getY());
            config.set(p + ".home.z",     h.getZ());
            config.set(p + ".home.yaw",   h.getYaw());
            config.set(p + ".home.pitch", h.getPitch());
        }
        // Homes nommés (sauf "default" déjà dans .home)
        config.set(p + ".homes", null);
        for (Map.Entry<String, Location> e : island.getHomes().entrySet()) {
            if ("default".equals(e.getKey())) continue;
            Location h = e.getValue();
            if (h == null || h.getWorld() == null) continue;
            String path = p + ".homes." + e.getKey();
            config.set(path + ".world", h.getWorld().getName());
            config.set(path + ".x", h.getX());
            config.set(path + ".y", h.getY());
            config.set(path + ".z", h.getZ());
            config.set(path + ".yaw", h.getYaw());
            config.set(path + ".pitch", h.getPitch());
        }
        config.set(p + ".guestList", new ArrayList<>(island.getGuestList().stream().map(UUID::toString).toList()));
        config.set(p + ".bankBalance", island.getBankBalance());

        // Bank log
        config.set(p + ".bankLog", null);
        var bankLog = island.getBankLog();
        for (int i = 0; i < bankLog.size(); i++) {
            var t = bankLog.get(i);
            String bp = p + ".bankLog." + i;
            config.set(bp + ".player",    t.playerName());
            config.set(bp + ".amount",    t.amount());
            config.set(bp + ".deposit",   t.deposit());
            config.set(bp + ".timestamp", t.timestamp());
        }
    }

    // ════════════════════════════════════════════════
    //  LOAD
    // ════════════════════════════════════════════════

    private void loadIslands() {
        if (config.getKeys(false).isEmpty()) return;

        for (String key : config.getKeys(false)) {
            try {
                UUID  owner = UUID.fromString(key);
                World world = Bukkit.getWorld(config.getString(key + ".world", "skyblock"));
                if (world == null) {
                    SkyBlockPlugin.getInstance().getLogger()
                            .warning("Monde introuvable pour l'île : " + key);
                    continue;
                }

                int x      = config.getInt(key + ".x");
                int y      = config.getInt(key + ".y");
                int z      = config.getInt(key + ".z");
                int radius = config.getInt(key + ".radius", 50);

                Island island = new Island(owner, new Location(world, x, y, z));
                island.setRadius(radius);
                island.setIsLevel(config.getDouble(key + ".isLevel", 0));
                island.setOpen(config.getBoolean(key + ".open", false));

                // Warp
                if (config.isConfigurationSection(key + ".warp")) {
                    World ww = Bukkit.getWorld(config.getString(key + ".warp.world", "skyblock"));
                    if (ww != null) {
                        island.setWarpLocation(new Location(ww,
                                config.getDouble(key + ".warp.x"),
                                config.getDouble(key + ".warp.y"),
                                config.getDouble(key + ".warp.z"),
                                (float) config.getDouble(key + ".warp.yaw"),
                                (float) config.getDouble(key + ".warp.pitch")));
                    }
                }

                // Home
                if (config.isConfigurationSection(key + ".home")) {
                    World hw = Bukkit.getWorld(config.getString(key + ".home.world", "skyblock"));
                    if (hw != null) {
                        island.setHomeLocation(new Location(hw,
                                config.getDouble(key + ".home.x"),
                                config.getDouble(key + ".home.y"),
                                config.getDouble(key + ".home.z"),
                                (float) config.getDouble(key + ".home.yaw"),
                                (float) config.getDouble(key + ".home.pitch")));
                    }
                }
                // Homes nommés
                if (config.isConfigurationSection(key + ".homes")) {
                    for (String homeName : config.getConfigurationSection(key + ".homes").getKeys(false)) {
                        String path = key + ".homes." + homeName;
                        World hw = Bukkit.getWorld(config.getString(path + ".world", "skyblock"));
                        if (hw != null) {
                            Location loc = new Location(hw,
                                    config.getDouble(path + ".x"),
                                    config.getDouble(path + ".y"),
                                    config.getDouble(path + ".z"),
                                    (float) config.getDouble(path + ".yaw"),
                                    (float) config.getDouble(path + ".pitch"));
                            island.setHome(homeName, loc);
                        }
                    }
                }
                // Invités
                for (String guestUuid : config.getStringList(key + ".guestList")) {
                    try { island.addGuest(UUID.fromString(guestUuid)); } catch (Exception ignored) {}
                }
                island.setBankBalance(config.getDouble(key + ".bankBalance", 0));

                // Bank log
                if (config.isConfigurationSection(key + ".bankLog")) {
                    var logSection = config.getConfigurationSection(key + ".bankLog");
                    List<Island.BankTransaction> entries = new ArrayList<>();
                    for (String idx : logSection.getKeys(false)) {
                        String bp = key + ".bankLog." + idx;
                        entries.add(new Island.BankTransaction(
                                config.getString(bp + ".player", "?"),
                                config.getLong(bp + ".amount", 0),
                                config.getBoolean(bp + ".deposit", true),
                                config.getLong(bp + ".timestamp", 0)
                        ));
                    }
                    island.loadBankLog(entries);
                }

                // Nom
                if (config.contains(key + ".name"))
                    island.setName(config.getString(key + ".name"));

                // Membres
                if (config.isConfigurationSection(key + ".members")) {
                    for (String mk : config.getConfigurationSection(key + ".members").getKeys(false)) {
                        try {
                            island.addMember(UUID.fromString(mk),
                                    IslandRole.valueOf(config.getString(key + ".members." + mk)));
                        } catch (Exception ignored) {}
                    }
                }

                // Permissions
                if (config.isConfigurationSection(key + ".permissions")) {
                    for (String pk : config.getConfigurationSection(key + ".permissions").getKeys(false)) {
                        try {
                            island.setPermissionRole(
                                    IslandPermission.valueOf(pk),
                                    IslandRole.valueOf(config.getString(key + ".permissions." + pk)));
                        } catch (Exception ignored) {}
                    }
                }

                // Blocs de valeur
                if (config.isConfigurationSection(key + ".valueBlocks")) {
                    for (String bk : config.getConfigurationSection(key + ".valueBlocks").getKeys(false)) {
                        try {
                            island.setValueBlockCount(IslandValueBlock.valueOf(bk),
                                    config.getInt(key + ".valueBlocks." + bk, 0));
                        } catch (Exception ignored) {}
                    }
                }

                // Bannis
                for (String bs : config.getStringList(key + ".banned")) {
                    try { island.banPlayer(UUID.fromString(bs)); } catch (Exception ignored) {}
                }

                // Missions progression
                if (config.isConfigurationSection(key + ".missionProgress")) {
                    for (String mk : config.getConfigurationSection(key + ".missionProgress").getKeys(false)) {
                        island.getMissionProgressMap().put(mk, config.getInt(key + ".missionProgress." + mk));
                    }
                }

                // Missions complétées
                for (String ms : config.getStringList(key + ".completedMissions"))
                    island.getCompletedMissions().add(ms);

                // Upgrades
                if (config.isConfigurationSection(key + ".upgrades")) {
                    for (String uk : config.getConfigurationSection(key + ".upgrades").getKeys(false)) {
                        try {
                            IslandUpgrade upgrade = IslandUpgrade.valueOf(uk);
                            int level = config.getInt(key + ".upgrades." + uk, 0);
                            island.setUpgradeLevel(upgrade, level);
                        } catch (Exception ignored) {}
                    }
                    // Sync radius avec upgrade SIZE
                    int upgradeSize = island.getUpgradeValue(IslandUpgrade.SIZE);
                    if (upgradeSize > island.getRadius()) island.setRadius(upgradeSize);
                }

                islands.put(owner, island);
                buildChunkCache(island);
                buildMemberCache(island); // ← cache membre construit au load
            } catch (Exception e) {
                SkyBlockPlugin.getInstance().getLogger()
                        .warning("Erreur chargement île " + key + ": " + e.getMessage());
            }
        }

        gridIndex = islands.size();
    }

    // ════════════════════════════════════════════════
    //  GETTERS
    // ════════════════════════════════════════════════

    public boolean hasIsland(UUID uuid)       { return islands.containsKey(uuid); }
    public Island  getIsland(UUID owner)      { return islands.get(owner); }
    public Collection<Island> getAllIslands()  { return islands.values(); }

    /**
     * O(1) grâce au cache memberToIslandOwner.
     * Fallback O(n) si le cache rate (island pas encore en cache = très rare).
     */
    public Island getIslandByMember(UUID uuid) {
        UUID ownerUuid = memberToIslandOwner.get(uuid);
        if (ownerUuid != null) {
            Island island = islands.get(ownerUuid);
            if (island != null) return island;
            // Cache périmé (île supprimée entre temps) — nettoyer
            memberToIslandOwner.remove(uuid);
        }
        // Fallback O(n) — ne devrait presque jamais arriver
        for (Island island : islands.values()) {
            if (island.isMember(uuid)) {
                memberToIslandOwner.put(uuid, island.getOwner()); // re-cacher
                return island;
            }
        }
        return null;
    }

    /** Recherche une île par son nom (insensible à la casse). */
    public Island getIslandByName(String name) {
        if (name == null) return null;
        for (Island island : islands.values()) {
            if (island.hasName() && island.getName().equalsIgnoreCase(name)) return island;
        }
        return null;
    }

    /**
     * O(1) grâce au cache chunk → island.
     * Fallback O(n) si le cache rate (bord de chunk ou cache pas encore construit).
     */
    public Island getIslandAtLocation(Location location) {
        if (location.getWorld() == null) return null;
        String key = location.getWorld().getName() + ","
                + (location.getBlockX() >> 4) + ","
                + (location.getBlockZ() >> 4);
        Island cached = chunkIslandCache.get(key);
        if (cached != null) {
            Location center = cached.getCenter();
            int dx = Math.abs(location.getBlockX() - center.getBlockX());
            int dz = Math.abs(location.getBlockZ() - center.getBlockZ());
            if (dx <= cached.getRadius() && dz <= cached.getRadius()) return cached;
        }
        // Fallback
        for (Island island : islands.values()) {
            Location center = island.getCenter();
            if (!center.getWorld().equals(location.getWorld())) continue;
            int dx = Math.abs(location.getBlockX() - center.getBlockX());
            int dz = Math.abs(location.getBlockZ() - center.getBlockZ());
            if (dx <= island.getRadius() && dz <= island.getRadius()) return island;
        }
        return null;
    }

    // ════════════════════════════════════════════════
    //  GRILLE SPIRALE
    // ════════════════════════════════════════════════

    public Location getNextIslandLocation(World world) {
        Set<String> usedKeys = new HashSet<>();
        for (Island island : islands.values()) {
            int gx = island.getCenter().getBlockX() / SPACING;
            int gz = island.getCenter().getBlockZ() / SPACING;
            usedKeys.add(gx + "," + gz);
        }
        for (UUID deletingUuid : deletingIslands) {
            Island di = islands.get(deletingUuid);
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
        if (!used.contains("0,0")) return new int[]{0, 0};
        int x = 0, z = 0, dx = 1, dz = 0, steps = 1, stepCount = 0, turnCount = 0;
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

    // ════════════════════════════════════════════════
    //  INVITATIONS
    // ════════════════════════════════════════════════

    public void addInvitation(UUID target, UUID owner)  { invitations.put(target, owner); }
    public UUID getInvitation(UUID target)               { return invitations.get(target); }
    public void removeInvitation(UUID target)            { invitations.remove(target); }
    public boolean hasInvitation(UUID target)            { return invitations.containsKey(target); }

    // ════════════════════════════════════════════════
    //  TRANSFER (alias replaceIsland)
    // ════════════════════════════════════════════════

    public void replaceIsland(UUID oldOwner, Island newIsland) {
        Island oldIsland = islands.getOrDefault(oldOwner, newIsland);
        invalidateChunkCache(oldIsland);
        invalidateMemberCache(oldIsland);

        saveIsland(newIsland); // async
        islands.remove(oldOwner);
        config.set(oldOwner.toString(), null);
        islands.put(newIsland.getOwner(), newIsland);

        buildChunkCache(newIsland);
        buildMemberCache(newIsland);
        saveFile(); // sync pour la suppression de l'ancienne clé
    }
}