package be.RedSwick.skyblock.island;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/**
 * Blocs qui comptent pour le niveau de l'île.
 * Chaque bloc a une valeur en points IS et une limite maximale comptabilisée.
 */
public enum IslandValueBlock {

    // ── Agricoles ──────────────────────────────────────
    WHEAT_BLOCK       (Material.HAY_BLOCK,           0.5,  25_000, "§eBloc de Blé"),
    NETHER_WART_BLOCK (Material.NETHER_WART_BLOCK,   0.5,  25_000, "§cNether Wart Block"),

    // ── Minerais communs ────────────────────────────────
    COPPER_BLOCK      (Material.COPPER_BLOCK,         0.5,  35_000, "§6Bloc de Cuivre"),
    LAPIS_BLOCK       (Material.LAPIS_BLOCK,          0.5,  35_000, "§9Bloc de Lapis"),
    REDSTONE_BLOCK    (Material.REDSTONE_BLOCK,       0.5,  35_000, "§cBloc de Redstone"),

    // ── Minerais intermédiaires ─────────────────────────
    IRON_BLOCK        (Material.IRON_BLOCK,           1.0,  20_000, "§7Bloc de Fer"),
    GOLD_BLOCK        (Material.GOLD_BLOCK,           2.0,  20_000, "§6Bloc d'Or"),

    // ── Minerais rares ──────────────────────────────────
    DIAMOND_BLOCK     (Material.DIAMOND_BLOCK,        5.0,  10_000, "§bBloc de Diamant"),
    EMERALD_BLOCK     (Material.EMERALD_BLOCK,        6.0,  10_000, "§aBloc d'Émeraude"),

    // ── Minerais très rares ─────────────────────────────
    NETHERITE_BLOCK   (Material.NETHERITE_BLOCK,     20.0,   5_000, "§8Bloc de Netherite");

    // ─────────────────────────────────────────────────────
    private final Material material;
    private final double   pointsPerBlock;
    private final int      limit;
    private final String   displayName;

    IslandValueBlock(Material material, double pointsPerBlock,
                     int limit, String displayName) {
        this.material       = material;
        this.pointsPerBlock = pointsPerBlock;
        this.limit          = limit;
        this.displayName    = displayName;
    }

    public Material getMaterial()     { return material; }
    public double   getPoints()       { return pointsPerBlock; }
    public int      getLimit()        { return limit; }
    public String   getDisplayName()  { return displayName; }

    // ── Lookup rapide Material → IslandValueBlock ──
    private static final Map<Material, IslandValueBlock> BY_MATERIAL = new HashMap<>();
    static {
        for (IslandValueBlock ivb : values()) {
            BY_MATERIAL.put(ivb.material, ivb);
        }
    }

    public static IslandValueBlock fromMaterial(Material mat) {
        return BY_MATERIAL.get(mat);
    }

    public static boolean isValueBlock(Material mat) {
        return BY_MATERIAL.containsKey(mat);
    }
}