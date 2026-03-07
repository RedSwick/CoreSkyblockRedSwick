package be.RedSwick.skyblock.moderation;

import be.RedSwick.skyblock.SkyBlockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Gère les permissions staff depuis staff.yml.
 *
 * FIX :
 *  - Suppression des logs de debug produit (6 logs par joueur connecté → pollution logs)
 *  - Consolidation fondateur hardcodé : plus de triple insertion (ensureFounder + hardcodé + addAll)
 *  - staffMap vide au join = warning léger, plus de rechargement forcé silencieux
 */
public class StaffPermissionManager {

    private static final StaffPermissionManager INSTANCE = new StaffPermissionManager();
    public static StaffPermissionManager get() { return INSTANCE; }

    private static final String FOUNDER_NAME = "RedSwick";

    private File file;
    private YamlConfiguration config;

    private final Map<String, StaffRank>             staffMap    = new HashMap<>();
    private final Map<UUID, PermissionAttachment>    attachments = new HashMap<>();

    public void init() {
        file = new File(SkyBlockPlugin.getInstance().getDataFolder(), "staff.yml");

        if (file.exists()) {
            YamlConfiguration test = YamlConfiguration.loadConfiguration(file);
            if (!test.contains("fondateurs")) {
                file.delete();
                SkyBlockPlugin.getInstance().getLogger().warning(
                        "[Staff] staff.yml corrompu — recréation...");
            }
        }

        if (!file.exists()) createDefault();
        config = YamlConfiguration.loadConfiguration(file);

        ensureFounder(FOUNDER_NAME);
        load();

        SkyBlockPlugin.getInstance().getLogger().info(
                "[Staff] " + staffMap.size() + " membres staff chargés.");
    }

    private void ensureFounder(String name) {
        List<String> fondateurs = new ArrayList<>(config.getStringList("fondateurs"));
        if (fondateurs.stream().noneMatch(n -> n.equalsIgnoreCase(name))) {
            fondateurs.add(name);
            config.set("fondateurs", fondateurs);
            try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
            SkyBlockPlugin.getInstance().getLogger().info(
                    "[Staff] " + name + " ajouté automatiquement comme Fondateur.");
        }
    }

    private void createDefault() {
        try {
            file.createNewFile();
            YamlConfiguration cfg = new YamlConfiguration();
            cfg.set("fondateurs", List.of(FOUNDER_NAME));
            cfg.set("admins",     List.of());
            cfg.set("moderateurs",List.of());
            cfg.set("guides",     List.of());
            cfg.save(file);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void load() {
        staffMap.clear();
        // Une seule source de vérité : le fichier YAML (ensureFounder() garantit qu'il contient le fondateur)
        addAll("fondateurs", StaffRank.FONDATEUR);
        addAll("admins",     StaffRank.ADMIN);
        addAll("moderateurs",StaffRank.MODERATEUR);
        addAll("guides",     StaffRank.GUIDE);
    }

    private void addAll(String key, StaffRank rank) {
        for (String name : config.getStringList(key)) {
            staffMap.put(name.toLowerCase(), rank);
        }
    }

    public void applyPermissions(Player player) {
        if (staffMap.isEmpty() && file != null) {
            SkyBlockPlugin.getInstance().getLogger().warning(
                    "[Staff] staffMap vide au join de " + player.getName() + " — vérifier l'init !");
            return;
        }
        StaffRank rank = staffMap.get(player.getName().toLowerCase());
        if (rank == null || rank == StaffRank.NONE) return;

        PermissionAttachment att = player.addAttachment(SkyBlockPlugin.getInstance());
        attachments.put(player.getUniqueId(), att);

        att.setPermission(rank.getPermission(), true);

        if (rank.isAtLeast(StaffRank.FONDATEUR)) {
            att.setPermission(StaffRank.ADMIN.getPermission(),      true);
            att.setPermission(StaffRank.MODERATEUR.getPermission(), true);
            att.setPermission(StaffRank.GUIDE.getPermission(),      true);
            player.setOp(true);
        } else if (rank.isAtLeast(StaffRank.ADMIN)) {
            att.setPermission(StaffRank.MODERATEUR.getPermission(), true);
            att.setPermission(StaffRank.GUIDE.getPermission(),      true);
            player.setOp(true);
        }

        SkyBlockPlugin.getInstance().getLogger().info(
                "[Staff] " + player.getName() + " → " + rank.getDisplay());
    }

    public void removePermissions(Player player) {
        PermissionAttachment att = attachments.remove(player.getUniqueId());
        if (att != null) player.removeAttachment(att);
    }

    public StaffRank getRank(String playerName) {
        return staffMap.getOrDefault(playerName.toLowerCase(), StaffRank.NONE);
    }

    public Map<String, StaffRank> getStaffMap() {
        return Collections.unmodifiableMap(staffMap);
    }

    public void setStaffRank(String playerName, StaffRank rank) {
        String key = playerName.toLowerCase();

        for (StaffRank r : StaffRank.values()) {
            if (r == StaffRank.NONE) continue;
            List<String> list = new ArrayList<>(config.getStringList(rankToKey(r)));
            list.removeIf(n -> n.equalsIgnoreCase(playerName));
            config.set(rankToKey(r), list);
        }

        if (rank == StaffRank.NONE) {
            staffMap.remove(key);
        } else {
            staffMap.put(key, rank);
            List<String> list = new ArrayList<>(config.getStringList(rankToKey(rank)));
            if (list.stream().noneMatch(n -> n.equalsIgnoreCase(playerName)))
                list.add(playerName);
            config.set(rankToKey(rank), list);
        }

        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }

        Player online = Bukkit.getPlayer(playerName);
        if (online != null) {
            removePermissions(online);
            if (rank != StaffRank.NONE) applyPermissions(online);
        }
    }

    private String rankToKey(StaffRank rank) {
        return switch (rank) {
            case FONDATEUR  -> "fondateurs";
            case ADMIN      -> "admins";
            case MODERATEUR -> "moderateurs";
            default         -> "guides";
        };
    }
}