package be.RedSwick.skyblock.hologram;

import be.RedSwick.skyblock.SkyBlockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gère les hologrammes de blocs de valeur et de mobs stackés.
 * OPTIMISATION : TextDisplay à la place des ArmorStands.
 *  - Zéro AI, zéro physique, zéro impact TPS
 *  - Pas de système passenger → pas d'orphelins
 *  - tickMobHolograms() simplifié (position fixe via TextDisplay)
 */
public class HologramManager {

    // Location key → TextDisplay (hologrammes de blocs de valeur)
    private final Map<String, TextDisplay> blockHolos = new HashMap<>();

    // Mob UUID → TextDisplay (hologrammes de mobs stackés)
    private final Map<UUID, TextDisplay> mobHolos = new HashMap<>();

    // ════════════════════════════════════════════════
    //  HOLOGRAMMES DE BLOCS
    // ════════════════════════════════════════════════

    public void setHologram(Location loc, String text) {
        String key = locKey(loc);
        // Supprimer l'ancien
        TextDisplay old = blockHolos.remove(key);
        if (old != null && !old.isDead()) old.remove();

        // Créer le nouveau TextDisplay au-dessus du bloc
        Location holoLoc = loc.clone().add(0.5, 1.5, 0.5);
        TextDisplay display = spawnTextDisplay(holoLoc, text);
        if (display != null) blockHolos.put(key, display);
    }

    public void removeHologram(Location loc) {
        TextDisplay display = blockHolos.remove(locKey(loc));
        if (display != null && !display.isDead()) display.remove();
    }

    public boolean hasHologram(Location loc) {
        return blockHolos.containsKey(locKey(loc));
    }

    // ════════════════════════════════════════════════
    //  HOLOGRAMMES DE MOBS STACKÉS
    // ════════════════════════════════════════════════

    public void setMobHologram(UUID mobId, Location mobLoc, String text) {
        // Supprimer l'ancien si présent
        removeMobHologram(mobId);

        // Position au-dessus du mob
        Location holoLoc = mobLoc.clone().add(0, 2.2, 0);
        TextDisplay display = spawnTextDisplay(holoLoc, text);
        if (display != null) mobHolos.put(mobId, display);
    }

    public void updateMobHologramText(UUID mobId, String text) {
        TextDisplay display = mobHolos.get(mobId);
        if (display != null && !display.isDead()) {
            display.setText(text);
        }
    }

    public void removeMobHologram(UUID mobId) {
        TextDisplay display = mobHolos.remove(mobId);
        if (display != null && !display.isDead()) display.remove();
    }

    /**
     * Tick des hologrammes de mobs — repositionne les TextDisplay sur le mob.
     * Contrairement aux ArmorStands (passengers), les TextDisplay n'ont pas de
     * mécanisme de suivi automatique → on met à jour la position toutes les 2s.
     * Coût faible : juste un setLocation() par mob stacké.
     */
    public void tickMobHolograms() {
        for (Map.Entry<UUID, TextDisplay> entry : new HashMap<>(mobHolos).entrySet()) {
            UUID mobId = entry.getKey();
            TextDisplay display = entry.getValue();

            if (display == null || display.isDead()) {
                mobHolos.remove(mobId);
                continue;
            }

            // Trouver l'entité mob correspondante
            Entity entity = Bukkit.getEntity(mobId);
            if (entity == null || !entity.isValid()) {
                display.remove();
                mobHolos.remove(mobId);
                continue;
            }

            // Repositionner le TextDisplay au-dessus du mob
            Location newLoc = entity.getLocation().add(0, 2.2, 0);
            display.teleport(newLoc);
        }
    }

    /**
     * Nettoyage au démarrage — supprime les TextDisplay orphelins qui auraient
     * survécu à un restart (théoriquement impossible avec setPersistent(false),
     * mais par sécurité).
     */
    public void cleanupOrphanHolograms() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof TextDisplay td) {
                    // TextDisplay non enregistré dans nos maps → orphelin
                    boolean isBlockHolo = blockHolos.containsValue(td);
                    boolean isMobHolo   = mobHolos.containsValue(td);
                    if (!isBlockHolo && !isMobHolo) {
                        td.remove();
                    }
                }
            }
        }
    }

    public void removeAll() {
        blockHolos.values().forEach(d -> { if (d != null && !d.isDead()) d.remove(); });
        blockHolos.clear();
        mobHolos.values().forEach(d -> { if (d != null && !d.isDead()) d.remove(); });
        mobHolos.clear();
    }

    // ════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════

    private TextDisplay spawnTextDisplay(Location loc, String text) {
        if (loc.getWorld() == null) return null;
        return loc.getWorld().spawn(loc, TextDisplay.class, display -> {
            display.setText(text);
            display.setGravity(false);
            display.setPersistent(false); // Disparaît au restart → pas d'orphelins
            display.setBillboard(Display.Billboard.CENTER);
            display.setViewRange(24f);
        });
    }

    private static String locKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }
}