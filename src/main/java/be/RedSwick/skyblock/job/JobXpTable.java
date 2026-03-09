package be.RedSwick.skyblock.job;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

/**
 * TABLE CENTRALISÉE — XP + Coins par action
 *
 * CORRECTIONS APPLIQUÉES :
 *  - Material.CARROT/POTATO/BEETROOT = ITEMS, pas des blocs en Bukkit 1.21!
 *    Les blocs s'appellent CARROTS, POTATOES, BEETROOTS.
 *  - Material.COCOA_BEANS = item, le bloc = COCOA
 *  - Material.SWEET_BERRIES = item, le bloc = SWEET_BERRY_BUSH
 *  - Material.GLOW_BERRIES  = item, le bloc = CAVE_VINES / CAVE_VINES_PLANT
 *  - Material.MELON_SLICE   = item seulement (pas un bloc cassable)
 *  - Material.CHORUS_FRUIT  = item seulement, le bloc = CHORUS_PLANT
 *
 * FORMULE XP REQUIS PAR NIVEAU :
 *   (100 + (N² × 2.2)) × 1.5
 *   Total niv 1→150 ≈ 3 735 000 XP
 */
public class JobXpTable {

    public record JobAction(double xp, double coins) {}

    // ══════════════════════════════════════════════════════
    //  FORMULE NIVEAU
    // ══════════════════════════════════════════════════════

    public static long getXpRequired(int level) {
        return Math.round((100 + (level * level * 2.2)) * 1.5);
    }

    public static long getLevelUpCoins(int newLevel) {
        if (newLevel == 150) return 500_000;
        if (newLevel == 100) return 200_000;
        if (newLevel == 50)  return 100_000;
        if (newLevel % 25 == 0) return 25_000;
        if (newLevel % 10 == 0) return 5_000;
        if (newLevel % 5  == 0) return 1_500;
        return 200 + (newLevel * 10L);
    }

    // ══════════════════════════════════════════════════════
    //  FARMER
    //  !! Utiliser les Material de BLOCS, pas les items !!
    //     CARROTS (bloc)  != CARROT (item)
    //     POTATOES (bloc) != POTATO (item)
    //     BEETROOTS (bloc)!= BEETROOT (item)
    //     COCOA (bloc)    != COCOA_BEANS (item)
    // ══════════════════════════════════════════════════════
    public static final Map<Material, JobAction> FARMER_CROPS = new HashMap<>();
    static {
        // ─── Cultures Ageable (ont une maturité) ───
        FARMER_CROPS.put(Material.WHEAT,            new JobAction(0.5,  1));
        FARMER_CROPS.put(Material.CARROTS,          new JobAction(0.5,  1)); // FIX: était CARROT (item)
        FARMER_CROPS.put(Material.POTATOES,         new JobAction(0.5,  1)); // FIX: était POTATO (item)
        FARMER_CROPS.put(Material.BEETROOTS,        new JobAction(0.5,  1)); // FIX: était BEETROOT (item)
        FARMER_CROPS.put(Material.NETHER_WART,      new JobAction(0.8,  2));
        FARMER_CROPS.put(Material.COCOA,            new JobAction(0.8,  2)); // FIX: était COCOA_BEANS (item)
        FARMER_CROPS.put(Material.TORCHFLOWER_CROP, new JobAction(3.0, 10)); // bloc en croissance
        FARMER_CROPS.put(Material.PITCHER_CROP,     new JobAction(3.0, 10)); // bloc en croissance

        // ─── Cultures non-Ageable ───
        FARMER_CROPS.put(Material.SUGAR_CANE,       new JobAction(0.3,  1));
        FARMER_CROPS.put(Material.CACTUS,           new JobAction(0.2,  1));
        FARMER_CROPS.put(Material.BAMBOO,           new JobAction(0.2,  1));
        FARMER_CROPS.put(Material.KELP,             new JobAction(0.2,  1));
        FARMER_CROPS.put(Material.KELP_PLANT,       new JobAction(0.2,  1));
        FARMER_CROPS.put(Material.SWEET_BERRY_BUSH, new JobAction(1.0,  3)); // FIX: était SWEET_BERRIES (item)
        FARMER_CROPS.put(Material.CAVE_VINES,       new JobAction(1.2,  3)); // FIX: était GLOW_BERRIES (item)
        FARMER_CROPS.put(Material.CAVE_VINES_PLANT, new JobAction(1.2,  3)); // corps de la vigne
        FARMER_CROPS.put(Material.SEA_PICKLE,       new JobAction(1.0,  2));
        FARMER_CROPS.put(Material.PUMPKIN,          new JobAction(1.5,  5));
        FARMER_CROPS.put(Material.MELON,            new JobAction(1.5,  4)); // FIX: retiré MELON_SLICE (item)
        FARMER_CROPS.put(Material.BROWN_MUSHROOM,   new JobAction(1.2,  3));
        FARMER_CROPS.put(Material.RED_MUSHROOM,     new JobAction(1.2,  3));
        FARMER_CROPS.put(Material.CHORUS_FLOWER,    new JobAction(2.0,  6));
        FARMER_CROPS.put(Material.CHORUS_PLANT,     new JobAction(1.5,  4)); // FIX: était CHORUS_FRUIT (item)
        FARMER_CROPS.put(Material.TORCHFLOWER,      new JobAction(3.0, 10)); // fleur mature
        FARMER_CROPS.put(Material.PITCHER_PLANT,    new JobAction(3.0, 10)); // plante mature
    }

    // ══════════════════════════════════════════════════════
    //  BÛCHERON
    // ══════════════════════════════════════════════════════
    public static final Map<Material, JobAction> BUCHERON_LOGS = new HashMap<>();
    static {
        BUCHERON_LOGS.put(Material.OAK_LOG,              new JobAction(0.5,  1));
        BUCHERON_LOGS.put(Material.BIRCH_LOG,            new JobAction(0.5,  1));
        BUCHERON_LOGS.put(Material.SPRUCE_LOG,           new JobAction(0.6,  2));
        BUCHERON_LOGS.put(Material.ACACIA_LOG,           new JobAction(0.6,  2));
        BUCHERON_LOGS.put(Material.JUNGLE_LOG,           new JobAction(0.7,  2));
        BUCHERON_LOGS.put(Material.DARK_OAK_LOG,         new JobAction(0.7,  2));
        BUCHERON_LOGS.put(Material.MANGROVE_LOG,         new JobAction(1.5,  5));
        BUCHERON_LOGS.put(Material.CHERRY_LOG,           new JobAction(2.0,  6));
        BUCHERON_LOGS.put(Material.CRIMSON_STEM,         new JobAction(1.2,  4));
        BUCHERON_LOGS.put(Material.WARPED_STEM,          new JobAction(1.2,  4));
        BUCHERON_LOGS.put(Material.BAMBOO_BLOCK,              new JobAction(0.2,  1));
        BUCHERON_LOGS.put(Material.BROWN_MUSHROOM_BLOCK,      new JobAction(1.0,  3));
        BUCHERON_LOGS.put(Material.RED_MUSHROOM_BLOCK,        new JobAction(1.0,  3));
        // ─── Feuilles ───
        BUCHERON_LOGS.put(Material.OAK_LEAVES,                new JobAction(0.1,  0));
        BUCHERON_LOGS.put(Material.BIRCH_LEAVES,              new JobAction(0.1,  0));
        BUCHERON_LOGS.put(Material.SPRUCE_LEAVES,             new JobAction(0.1,  0));
        BUCHERON_LOGS.put(Material.JUNGLE_LEAVES,             new JobAction(0.1,  0));
        BUCHERON_LOGS.put(Material.ACACIA_LEAVES,             new JobAction(0.1,  0));
        BUCHERON_LOGS.put(Material.DARK_OAK_LEAVES,           new JobAction(0.1,  0));
        BUCHERON_LOGS.put(Material.MANGROVE_LEAVES,           new JobAction(0.2,  0));
        BUCHERON_LOGS.put(Material.CHERRY_LEAVES,             new JobAction(0.2,  0));
        BUCHERON_LOGS.put(Material.AZALEA_LEAVES,             new JobAction(0.1,  0));
        BUCHERON_LOGS.put(Material.FLOWERING_AZALEA_LEAVES,   new JobAction(0.2,  0));
    }

    // ══════════════════════════════════════════════════════
    //  MINEUR
    // ══════════════════════════════════════════════════════
    public static final Map<Material, JobAction> MINER_BLOCKS = new HashMap<>();
    static {
        MINER_BLOCKS.put(Material.STONE,                    new JobAction(0.1,  0));
        MINER_BLOCKS.put(Material.COBBLESTONE,              new JobAction(0.1,  0));
        MINER_BLOCKS.put(Material.DEEPSLATE,                new JobAction(0.15, 0));
        MINER_BLOCKS.put(Material.COBBLED_DEEPSLATE,        new JobAction(0.15, 0));
        MINER_BLOCKS.put(Material.COAL_ORE,                 new JobAction(0.5,  1));
        MINER_BLOCKS.put(Material.DEEPSLATE_COAL_ORE,       new JobAction(0.6,  1));
        MINER_BLOCKS.put(Material.IRON_ORE,                 new JobAction(1.0,  2));
        MINER_BLOCKS.put(Material.DEEPSLATE_IRON_ORE,       new JobAction(1.2,  2));
        MINER_BLOCKS.put(Material.GOLD_ORE,                 new JobAction(1.5,  3));
        MINER_BLOCKS.put(Material.DEEPSLATE_GOLD_ORE,       new JobAction(1.8,  3));
        MINER_BLOCKS.put(Material.LAPIS_ORE,                new JobAction(2.0,  5));
        MINER_BLOCKS.put(Material.DEEPSLATE_LAPIS_ORE,      new JobAction(2.4,  5));
        MINER_BLOCKS.put(Material.REDSTONE_ORE,             new JobAction(2.0,  4));
        MINER_BLOCKS.put(Material.DEEPSLATE_REDSTONE_ORE,   new JobAction(2.4,  4));
        MINER_BLOCKS.put(Material.DIAMOND_ORE,              new JobAction(5.0, 15));
        MINER_BLOCKS.put(Material.DEEPSLATE_DIAMOND_ORE,    new JobAction(6.0, 15));
        MINER_BLOCKS.put(Material.EMERALD_ORE,              new JobAction(6.0, 20));
        MINER_BLOCKS.put(Material.DEEPSLATE_EMERALD_ORE,    new JobAction(7.0, 20));
        MINER_BLOCKS.put(Material.ANCIENT_DEBRIS,           new JobAction(15.0, 50));
        MINER_BLOCKS.put(Material.NETHER_QUARTZ_ORE,        new JobAction(0.8,  2));
        MINER_BLOCKS.put(Material.NETHER_GOLD_ORE,          new JobAction(1.0,  2));
        MINER_BLOCKS.put(Material.COPPER_ORE,               new JobAction(0.8,  2));
        MINER_BLOCKS.put(Material.DEEPSLATE_COPPER_ORE,     new JobAction(1.0,  2));
    }

    // ══════════════════════════════════════════════════════
    //  CHASSEUR
    // ══════════════════════════════════════════════════════
    public static final Map<EntityType, JobAction> CHASSEUR_MOBS    = new HashMap<>();
    public static final Map<EntityType, Double>    CHASSEUR_ESSENCE = new HashMap<>();
    static {
        CHASSEUR_MOBS.put(EntityType.ZOMBIE,          new JobAction(1.0,  2));
        CHASSEUR_MOBS.put(EntityType.SKELETON,        new JobAction(1.0,  2));
        CHASSEUR_MOBS.put(EntityType.CREEPER,         new JobAction(2.0,  4));
        CHASSEUR_MOBS.put(EntityType.SPIDER,          new JobAction(1.0,  2));
        CHASSEUR_MOBS.put(EntityType.CAVE_SPIDER,     new JobAction(1.5,  3));
        CHASSEUR_MOBS.put(EntityType.WITCH,           new JobAction(3.0,  8));
        CHASSEUR_MOBS.put(EntityType.SLIME,           new JobAction(0.5,  1));
        CHASSEUR_MOBS.put(EntityType.MAGMA_CUBE,      new JobAction(1.0,  3));
        CHASSEUR_MOBS.put(EntityType.BLAZE,           new JobAction(3.0, 10));
        CHASSEUR_MOBS.put(EntityType.ENDERMAN,        new JobAction(4.0, 12));
        CHASSEUR_MOBS.put(EntityType.GUARDIAN,        new JobAction(5.0, 15));
        CHASSEUR_MOBS.put(EntityType.WITHER_SKELETON, new JobAction(5.0, 15));
        CHASSEUR_MOBS.put(EntityType.IRON_GOLEM,      new JobAction(8.0, 25));
        CHASSEUR_MOBS.put(EntityType.PIG,             new JobAction(0.5,  1));
        CHASSEUR_MOBS.put(EntityType.COW,             new JobAction(0.5,  1));
        CHASSEUR_MOBS.put(EntityType.SHEEP,           new JobAction(0.5,  1));
        CHASSEUR_MOBS.put(EntityType.CHICKEN,         new JobAction(0.5,  1));
        CHASSEUR_MOBS.put(EntityType.RABBIT,          new JobAction(0.8,  2));
        CHASSEUR_MOBS.put(EntityType.POLAR_BEAR,      new JobAction(3.0,  8));
        CHASSEUR_MOBS.put(EntityType.PIGLIN,          new JobAction(2.0,  5));

        CHASSEUR_ESSENCE.put(EntityType.ENDERMAN,         2.0);
        CHASSEUR_ESSENCE.put(EntityType.BLAZE,            1.5);
        CHASSEUR_ESSENCE.put(EntityType.WITHER_SKELETON,  2.0);
        CHASSEUR_ESSENCE.put(EntityType.GUARDIAN,         2.5);
        CHASSEUR_ESSENCE.put(EntityType.IRON_GOLEM,       3.0);
        CHASSEUR_ESSENCE.put(EntityType.WITCH,            1.0);
    }

    // ══════════════════════════════════════════════════════
    //  PÊCHEUR
    // ══════════════════════════════════════════════════════
    public static final Map<Material, JobAction> PECHEUR_FISH = new HashMap<>();
    static {
        PECHEUR_FISH.put(Material.COD,             new JobAction(1.0,  2));
        PECHEUR_FISH.put(Material.SALMON,          new JobAction(1.5,  3));
        PECHEUR_FISH.put(Material.TROPICAL_FISH,   new JobAction(2.0,  5));
        PECHEUR_FISH.put(Material.PUFFERFISH,      new JobAction(2.5,  6));
        PECHEUR_FISH.put(Material.INK_SAC,         new JobAction(0.5,  1));
        PECHEUR_FISH.put(Material.LILY_PAD,        new JobAction(0.3,  1));
        PECHEUR_FISH.put(Material.NAUTILUS_SHELL,  new JobAction(5.0, 15));
    }

    // ══════════════════════════════════════════════════════
    //  ALCHIMISTE
    // ══════════════════════════════════════════════════════
    public static final Map<Material, JobAction> ALCHIMISTE_POTIONS = new HashMap<>();
    static {
        ALCHIMISTE_POTIONS.put(Material.POTION,           new JobAction(2.0,  5));
        ALCHIMISTE_POTIONS.put(Material.SPLASH_POTION,    new JobAction(2.5,  6));
        ALCHIMISTE_POTIONS.put(Material.LINGERING_POTION, new JobAction(3.0,  8));
    }
}