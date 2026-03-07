package be.RedSwick.skyblock.customitem;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.Map;

/**
 * Définition de tous les items custom du serveur.
 */
public enum CustomItemType {

    // ── HAMMER ────────────────────────────────────────────────────────
    HAMMER(
            "hammer",
            "§6§lHammer §8[3x3]",
            Material.DIAMOND_PICKAXE,
            5000, 35, 50_000,
            Map.of(Enchantment.EFFICIENCY, 7, Enchantment.UNBREAKING, 5, Enchantment.FORTUNE, 5)
    ),

    // ── HACHE 3X3 ─────────────────────────────────────────────────────
    AXE_3X3(
            "axe_3x3",
            "§6§lHache §8[3x3]",
            Material.DIAMOND_AXE,
            5000, 35, 50_000,
            Map.of(Enchantment.EFFICIENCY, 7, Enchantment.UNBREAKING, 5, Enchantment.FORTUNE, 5)
    ),

    // ── HOUES ─────────────────────────────────────────────────────────
    FARMERS_HOE_1X1(
            "farmers_hoe_1x1",
            "§a§lFarmer's Hoe §8[1x1]",
            Material.DIAMOND_HOE,
            1000, 0, 0,  // pas réparable
            Map.of(Enchantment.EFFICIENCY, 3, Enchantment.UNBREAKING, 3, Enchantment.FORTUNE, 2)
    ),
    FARMERS_HOE_3X3(
            "farmers_hoe_3x3",
            "§a§lFarmer's Hoe §8[3x3]",
            Material.DIAMOND_HOE,
            3500, 25, 35_000,
            Map.of(Enchantment.EFFICIENCY, 5, Enchantment.UNBREAKING, 4, Enchantment.FORTUNE, 3)
    ),
    FARMERS_HOE_5X5(
            "farmers_hoe_5x5",
            "§a§lFarmer's Hoe §8[5x5]",
            Material.DIAMOND_HOE,
            5000, 35, 50_000,
            Map.of(Enchantment.EFFICIENCY, 5, Enchantment.UNBREAKING, 5, Enchantment.FORTUNE, 5)
    ),
    PLANTER_HOE(
            "planter_hoe",
            "§2§lPlanteur §8[3x3]",
            Material.DIAMOND_HOE,
            2000, 0, 0,  // pas réparable
            Map.of(Enchantment.UNBREAKING, 3)
    ),

    // ── MULTITOOLS ────────────────────────────────────────────────────
    MULTITOOL_BASIC(
            "multitool_basic",
            "§b§lMultiTool §8[Basique]",
            Material.DIAMOND_PICKAXE,
            500, 0, 0,  // pas réparable
            Map.of(Enchantment.EFFICIENCY, 3)
    ),
    MULTITOOL_ELITE(
            "multitool_elite",
            "§b§lMultiTool §8[Élite]",
            Material.DIAMOND_PICKAXE,
            5000, 35, 25_000,
            Map.of(Enchantment.EFFICIENCY, 5, Enchantment.FORTUNE, 3, Enchantment.UNBREAKING, 5)
    ),

    // ── BÂTONS DE VENTE ───────────────────────────────────────────────
    SELL_WAND_1X(
            "sell_wand_1x",
            "§e§lBâton de Vente §8[x1]",
            Material.BLAZE_ROD,
            100, 0, 0,
            Map.of()
    ),
    SELL_WAND_1_5X(
            "sell_wand_1_5x",
            "§6§lBâton de Vente §8[x1.5]",
            Material.BLAZE_ROD,
            100, 0, 0,
            Map.of()
    ),
    SELL_WAND_2X(
            "sell_wand_2x",
            "§c§lBâton de Vente §8[x2]",
            Material.BLAZE_ROD,
            150, 0, 0,
            Map.of()
    ),
    SELL_WAND_2_5X(
            "sell_wand_2_5x",
            "§5§lBâton de Vente §8[x2.5]",
            Material.BLAZE_ROD,
            75, 0, 0,
            Map.of()
    ),
    // ── ÉPÉES ─────────────────────────────────────────────────────────
    SWORD_BRUTE(
            "sword_brute",
            "§c§lÉpée Brute",
            Material.NETHERITE_SWORD,
            5000, 25, 30_000,
            Map.of(Enchantment.SHARPNESS, 4, Enchantment.UNBREAKING, 5)
    ),
    SWORD_CHASSEUR(
            "sword_chasseur",
            "§4§lÉpée du Chasseur",
            Material.NETHERITE_SWORD,
            5000, 30, 40_000,
            Map.of(Enchantment.SHARPNESS, 6, Enchantment.UNBREAKING, 5)
    ),
    SWORD_ARCANIUM(
            "sword_arcanium",
            "§5§lLame Arcanium",
            Material.NETHERITE_SWORD,
            5000, 35, 50_000,
            Map.of(Enchantment.SHARPNESS, 8, Enchantment.UNBREAKING, 5)
    ),

    // ── SEAU D'EAU INFINI ─────────────────────────────────────────────
    INFINITE_WATER_BUCKET(
            "infinite_water_bucket",
            "§b§lSeau d'Eau Infini",
            Material.WATER_BUCKET,
            0, 0, 0,
            Map.of()
    ),

    // ── FILET DE PÊCHE ────────────────────────────────────────────────
    FISHING_NET(
            "fishing_net",
            "§3§lFilet de Pêche §8[3x]",
            Material.FISHING_ROD,
            1000, 20, 15_000,
            Map.of(Enchantment.LUCK_OF_THE_SEA, 3, Enchantment.LURE, 3, Enchantment.UNBREAKING, 3)
    ),

    // ── ANNEAUX D'EXPÉRIENCE ──────────────────────────────────────────
    RING_XP_FARMER_5(   "ring_xp_farmer_5",   "§a§lAnneau Farmer §8[+5%]",   Material.EMERALD, 0,0,0, Map.of()),
    RING_XP_FARMER_10(  "ring_xp_farmer_10",  "§a§lAnneau Farmer §8[+10%]",  Material.EMERALD, 0,0,0, Map.of()),
    RING_XP_MINER_5(    "ring_xp_miner_5",    "§7§lAnneau Mineur §8[+5%]",   Material.IRON_INGOT, 0,0,0, Map.of()),
    RING_XP_MINER_10(   "ring_xp_miner_10",   "§7§lAnneau Mineur §8[+10%]",  Material.IRON_INGOT, 0,0,0, Map.of()),
    RING_XP_BUCHERON_5( "ring_xp_bucheron_5", "§6§lAnneau Bûcheron §8[+5%]", Material.GOLD_INGOT, 0,0,0, Map.of()),
    RING_XP_BUCHERON_10("ring_xp_bucheron_10","§6§lAnneau Bûcheron §8[+10%]",Material.GOLD_INGOT, 0,0,0, Map.of()),
    RING_XP_CHASSEUR_5( "ring_xp_chasseur_5", "§c§lAnneau Chasseur §8[+5%]", Material.REDSTONE,   0,0,0, Map.of()),
    RING_XP_CHASSEUR_10("ring_xp_chasseur_10","§c§lAnneau Chasseur §8[+10%]",Material.REDSTONE,   0,0,0, Map.of()),
    RING_XP_PECHEUR_5(  "ring_xp_pecheur_5",  "§b§lAnneau Pêcheur §8[+5%]",  Material.PRISMARINE_CRYSTALS, 0,0,0, Map.of()),
    RING_XP_PECHEUR_10( "ring_xp_pecheur_10", "§b§lAnneau Pêcheur §8[+10%]", Material.PRISMARINE_CRYSTALS, 0,0,0, Map.of()),
    RING_XP_ALCHIMISTE_5( "ring_xp_alchimiste_5",  "§d§lAnneau Alchimiste §8[+5%]",  Material.AMETHYST_SHARD, 0,0,0, Map.of()),
    RING_XP_ALCHIMISTE_10("ring_xp_alchimiste_10", "§d§lAnneau Alchimiste §8[+10%]", Material.AMETHYST_SHARD, 0,0,0, Map.of()),

    // ── SAC DE GRAINES ────────────────────────────────────────────────
    SEED_BAG(
            "seed_bag",
            "§2§lSac de Graines",
            Material.BUNDLE,
            0, 0, 0,
            Map.of()
    ),

    // ── SAC DE BUTIN ──────────────────────────────────────────────────
    LOOT_BAG(
            "loot_bag",
            "§6§lSac de Butin",
            Material.BUNDLE,
            0, 0, 0,
            Map.of()
    ),

    // ── SACOCHE DE MARCHAND ───────────────────────────────────────────
    MERCHANT_POUCH(
            "merchant_pouch",
            "§e§lSacoche de Marchand",
            Material.BUNDLE,
            3600, 0, 0, // 3600 secondes = 1h, pas réparable (consommable)
            Map.of()
    ),

    // ── CRISTAL D'XP ──────────────────────────────────────────────────
    XP_CRYSTAL_SMALL(
            "xp_crystal_small",
            "§b§lCristal d'XP §8[Petit]",
            Material.AMETHYST_SHARD,
            0, 0, 0,
            Map.of()
    ),
    XP_CRYSTAL_MEDIUM(
            "xp_crystal_medium",
            "§9§lCristal d'XP §8[Moyen]",
            Material.AMETHYST_SHARD,
            0, 0, 0,
            Map.of()
    ),
    XP_CRYSTAL_LARGE(
            "xp_crystal_large",
            "§5§lCristal d'XP §8[Grand]",
            Material.AMETHYST_SHARD,
            0, 0, 0,
            Map.of()
    ),

    // ── CHUNK HOPPER ──────────────────────────────────────────────────
    CHUNK_HOPPER(
            "chunk_hopper",
            "§8§lChunk Hopper",
            Material.HOPPER,
            0, 0, 0,
            Map.of()
    );


    // ─── Champs ───────────────────────────────────────────────────────
    private final String   id;
    private final String   displayName;
    private final Material material;
    private final int      maxDurability;   // 0 = casse à 0
    private final int      repairLevels;    // 0 = pas réparable
    private final long     repairCoins;
    private final Map<Enchantment, Integer> enchants;

    CustomItemType(String id, String displayName, Material material,
                   int maxDurability, int repairLevels, long repairCoins,
                   Map<Enchantment, Integer> enchants) {
        this.id            = id;
        this.displayName   = displayName;
        this.material      = material;
        this.maxDurability = maxDurability;
        this.repairLevels  = repairLevels;
        this.repairCoins   = repairCoins;
        this.enchants      = enchants;
    }

    public String   getId()            { return id; }
    public String   getDisplayName()   { return displayName; }
    public Material getMaterial()      { return material; }
    public int      getMaxDurability() { return maxDurability; }
    public int      getRepairLevels()  { return repairLevels; }
    public long     getRepairCoins()   { return repairCoins; }
    public Map<Enchantment, Integer> getEnchants() { return enchants; }
    public boolean  isRepairable()     { return repairLevels > 0; }

    public static CustomItemType fromId(String id) {
        for (CustomItemType t : values())
            if (t.id.equals(id)) return t;
        return null;
    }

    /** Multiplicateur de vente pour les bâtons */
    public double getSellMultiplier() {
        return switch (this) {
            case SELL_WAND_1X   -> 1.0;
            case SELL_WAND_1_5X -> 1.5;
            case SELL_WAND_2X   -> 2.0;
            case SELL_WAND_2_5X -> 2.5;
            default             -> 1.0;
        };
    }

    public boolean isHoe()        { return name().startsWith("FARMERS_HOE") || this == PLANTER_HOE; }
    public boolean isHammer()     { return this == HAMMER; }
    public boolean isAxe3x3()     { return this == AXE_3X3; }
    public boolean isMultitool()  { return name().startsWith("MULTITOOL"); }
    public boolean isSellWand()   { return name().startsWith("SELL_WAND"); }
    public boolean isSword()      { return name().startsWith("SWORD_"); }
    public boolean isRing()       { return name().startsWith("RING_XP_"); }
    public boolean isAutoSellSword() { return this == SWORD_ARCANIUM; }

    /** Bonus XP de l'anneau (0.0 si pas un anneau) */
    public double getRingBonus() {
        return switch (this) {
            case RING_XP_FARMER_5, RING_XP_MINER_5, RING_XP_BUCHERON_5,
                 RING_XP_CHASSEUR_5, RING_XP_PECHEUR_5, RING_XP_ALCHIMISTE_5 -> 0.05;
            case RING_XP_FARMER_10, RING_XP_MINER_10, RING_XP_BUCHERON_10,
                 RING_XP_CHASSEUR_10, RING_XP_PECHEUR_10, RING_XP_ALCHIMISTE_10 -> 0.10;
            default -> 0.0;
        };
    }

    /** Job associé à l'anneau */
    public be.RedSwick.skyblock.player.PlayerJob getRingJob() {
        return switch (this) {
            case RING_XP_FARMER_5,    RING_XP_FARMER_10    -> be.RedSwick.skyblock.player.PlayerJob.FARMER;
            case RING_XP_MINER_5,     RING_XP_MINER_10     -> be.RedSwick.skyblock.player.PlayerJob.MINER;
            case RING_XP_BUCHERON_5,  RING_XP_BUCHERON_10  -> be.RedSwick.skyblock.player.PlayerJob.BUCHERON;
            case RING_XP_CHASSEUR_5,  RING_XP_CHASSEUR_10  -> be.RedSwick.skyblock.player.PlayerJob.CHASSEUR;
            case RING_XP_PECHEUR_5,   RING_XP_PECHEUR_10   -> be.RedSwick.skyblock.player.PlayerJob.PECHEUR;
            case RING_XP_ALCHIMISTE_5,RING_XP_ALCHIMISTE_10-> be.RedSwick.skyblock.player.PlayerJob.ALCHIMISTE;
            default -> null;
        };
    }

    /** XP donnée par le cristal */
    public double getCrystalXp() {
        return switch (this) {
            case XP_CRYSTAL_SMALL  -> 500.0;
            case XP_CRYSTAL_MEDIUM -> 1000.0;
            case XP_CRYSTAL_LARGE  -> 2000.0;
            default -> 0.0;
        };
    }

    public int getRadius() {
        return switch (this) {
            case FARMERS_HOE_1X1 -> 0; // 1x1
            case FARMERS_HOE_3X3, PLANTER_HOE -> 1; // 3x3
            case FARMERS_HOE_5X5 -> 2; // 5x5
            case HAMMER, AXE_3X3 -> 1; // 3x3
            default -> 0;
        };
    }
}