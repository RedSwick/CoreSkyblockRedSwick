package be.RedSwick.skyblock.manager;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CooldownManager — gestion des cooldowns joueur en mémoire.
 *
 * Clé interne : "uuid:type"
 * Valeur      : timestamp d'expiration en ms (System.currentTimeMillis)
 *
 * Thread-safe : ConcurrentHashMap → utilisable depuis async.
 */
public final class CooldownManager {

    // clé : "uuid:type" → timestamp d'expiration
    private final ConcurrentHashMap<String, Long> cooldowns = new ConcurrentHashMap<>();

    private String key(UUID uuid, String type) {
        return uuid.toString() + ":" + type;
    }

    /**
     * Retourne le nombre de secondes restantes.
     * Retourne 0 si le cooldown est expiré ou inexistant.
     */
    public long getRemainingSeconds(UUID uuid, String type) {
        Long expiry = cooldowns.get(key(uuid, type));
        if (expiry == null) return 0;
        long remaining = (expiry - System.currentTimeMillis()) / 1000L;
        if (remaining <= 0) {
            cooldowns.remove(key(uuid, type));
            return 0;
        }
        return remaining;
    }

    /**
     * Définit un cooldown pour le joueur.
     *
     * @param seconds durée en secondes (≤ 0 = supprime le cooldown)
     */
    public void setCooldown(UUID uuid, String type, int seconds) {
        if (seconds <= 0) {
            cooldowns.remove(key(uuid, type));
            return;
        }
        cooldowns.put(key(uuid, type), System.currentTimeMillis() + (seconds * 1000L));
    }

    /** true si le joueur est encore en cooldown. */
    public boolean hasCooldown(UUID uuid, String type) {
        return getRemainingSeconds(uuid, type) > 0;
    }

    /** Supprime immédiatement le cooldown. */
    public void clearCooldown(UUID uuid, String type) {
        cooldowns.remove(key(uuid, type));
    }

    /** Supprime tous les cooldowns d'un joueur (utile au logout). */
    public void clearAll(UUID uuid) {
        String prefix = uuid.toString() + ":";
        cooldowns.keySet().removeIf(k -> k.startsWith(prefix));
    }
}
