package be.RedSwick.skyblock.shop;

import org.bukkit.Material;
import java.util.List;

public enum ShopCategory {

    // ════════════════════════════════════════════════
    RESSOURCES("§7🪨 Ressources", Material.COBBLESTONE, List.of(
            ShopItem.of(Material.COBBLESTONE,       "Cobblestone",        5,   2),
            ShopItem.of(Material.STONE,             "Pierre",             8,   3),
            ShopItem.of(Material.GRANITE,           "Granite",            8,   3),
            ShopItem.of(Material.DIORITE,           "Diorite",            8,   3),
            ShopItem.of(Material.ANDESITE,          "Andesite",           8,   3),
            ShopItem.of(Material.GRAVEL,            "Gravier",            6,   2),
            ShopItem.of(Material.SAND,              "Sable",              6,   2),
            ShopItem.of(Material.RED_SAND,          "Sable Rouge",        8,   3),
            ShopItem.of(Material.OAK_LOG,           "Bois Chêne",        10,   4),
            ShopItem.of(Material.SPRUCE_LOG,        "Bois Épicéa",       10,   4),
            ShopItem.of(Material.BIRCH_LOG,         "Bois Bouleau",      10,   4),
            ShopItem.of(Material.JUNGLE_LOG,        "Bois Jungle",       10,   4),
            ShopItem.of(Material.ACACIA_LOG,        "Bois Acacia",       10,   4),
            ShopItem.of(Material.DARK_OAK_LOG,      "Bois Chêne Noir",   10,   4),
            ShopItem.of(Material.MANGROVE_LOG,      "Bois Palétuvier",   10,   4),
            ShopItem.of(Material.CHERRY_LOG,        "Bois Cerisier",     12,   5),
            ShopItem.of(Material.OBSIDIAN,          "Obsidienne",        50,  20),
            ShopItem.of(Material.NETHERRACK,        "Netherrack",         5,   1),
            ShopItem.of(Material.SOUL_SAND,         "Sable des Âmes",    20,   8),
            ShopItem.of(Material.SOUL_SOIL,         "Terre des Âmes",    15,   6),
            ShopItem.of(Material.BASALT,            "Basalte",           10,   4),
            ShopItem.of(Material.BLACKSTONE,        "Pierre Noire",      10,   4),
            ShopItem.of(Material.END_STONE,         "Pierre de l'End",   30,  12),
            ShopItem.of(Material.CLAY_BALL,         "Argile",            10,   4),
            ShopItem.of(Material.FLINT,             "Silex",             10,   4),
            ShopItem.of(Material.ICE,               "Glace",             15,   6),
            ShopItem.of(Material.PACKED_ICE,        "Glace Compacte",    25,  10),
            ShopItem.of(Material.BLUE_ICE,          "Glace Bleue",       40,  16)
    )),

    // ════════════════════════════════════════════════
    MINERAIS("§8⛏ Minerais", Material.DIAMOND, List.of(
            // ── Vente seulement ──
            ShopItem.sellOnly(Material.COAL,                   "Charbon",              6),
            ShopItem.sellOnly(Material.COAL_ORE,               "Minerai Charbon",      8),
            ShopItem.sellOnly(Material.DEEPSLATE_COAL_ORE,     "Minerai Charbon DS",  10),
            ShopItem.sellOnly(Material.RAW_IRON,               "Fer Brut",            12),
            ShopItem.sellOnly(Material.IRON_ORE,               "Minerai Fer",         15),
            ShopItem.sellOnly(Material.DEEPSLATE_IRON_ORE,     "Minerai Fer DS",      18),
            ShopItem.sellOnly(Material.IRON_INGOT,             "Lingot de Fer",       20),
            ShopItem.sellOnly(Material.RAW_COPPER,             "Cuivre Brut",          8),
            ShopItem.sellOnly(Material.COPPER_ORE,             "Minerai Cuivre",      10),
            ShopItem.sellOnly(Material.DEEPSLATE_COPPER_ORE,   "Minerai Cuivre DS",   12),
            ShopItem.sellOnly(Material.COPPER_INGOT,           "Lingot Cuivre",       12),
            ShopItem.sellOnly(Material.RAW_GOLD,               "Or Brut",             25),
            ShopItem.sellOnly(Material.GOLD_ORE,               "Minerai Or",          30),
            ShopItem.sellOnly(Material.DEEPSLATE_GOLD_ORE,     "Minerai Or DS",       35),
            ShopItem.sellOnly(Material.GOLD_INGOT,             "Lingot d'Or",         40),
            ShopItem.sellOnly(Material.LAPIS_ORE,              "Minerai Lapis",       18),
            ShopItem.sellOnly(Material.DEEPSLATE_LAPIS_ORE,    "Minerai Lapis DS",    22),
            ShopItem.sellOnly(Material.LAPIS_LAZULI,           "Lapis Lazuli",        10),
            ShopItem.sellOnly(Material.REDSTONE_ORE,           "Minerai Redstone",    15),
            ShopItem.sellOnly(Material.DEEPSLATE_REDSTONE_ORE, "Minerai Redstone DS", 18),
            ShopItem.sellOnly(Material.REDSTONE,               "Redstone",             8),
            ShopItem.sellOnly(Material.DIAMOND_ORE,            "Minerai Diamant",    200),
            ShopItem.sellOnly(Material.DEEPSLATE_DIAMOND_ORE,  "Minerai Diamant DS", 250),
            ShopItem.sellOnly(Material.DIAMOND,                "Diamant",            120),
            ShopItem.sellOnly(Material.EMERALD_ORE,            "Minerai Émeraude",   120),
            ShopItem.sellOnly(Material.DEEPSLATE_EMERALD_ORE,  "Minerai Émer. DS",   150),
            ShopItem.sellOnly(Material.EMERALD,                "Émeraude",            80),
            ShopItem.sellOnly(Material.ANCIENT_DEBRIS,         "Ancient Debris",    1000),
            ShopItem.sellOnly(Material.NETHERITE_SCRAP,        "Éclat Netherite",    800),
            ShopItem.sellOnly(Material.NETHERITE_INGOT,        "Lingot Netherite",  3500),
            ShopItem.sellOnly(Material.QUARTZ,                 "Quartz",              10),
            ShopItem.sellOnly(Material.NETHER_QUARTZ_ORE,             "Minerai Quartz",      12),
            ShopItem.sellOnly(Material.AMETHYST_SHARD,         "Éclat Améthyste",     30),
            ShopItem.sellOnly(Material.NETHER_QUARTZ_ORE,      "Minerai Quartz N.",   12)
    )),

    // ════════════════════════════════════════════════
    AGRICULTURE("§a🌾 Agriculture", Material.WHEAT, List.of(
            ShopItem.of(Material.WHEAT,             "Blé",                8,   3),
            ShopItem.of(Material.CARROT,            "Carotte",            8,   3),
            ShopItem.of(Material.POTATO,            "Pomme de Terre",     8,   3),
            ShopItem.of(Material.BAKED_POTATO,      "Pomme de Terre Cuite",12, 5),
            ShopItem.of(Material.BEETROOT,          "Betterave",          8,   3),
            ShopItem.of(Material.MELON_SLICE,       "Melon",              5,   2),
            ShopItem.of(Material.PUMPKIN,           "Citrouille",        12,   5),
            ShopItem.of(Material.SUGAR_CANE,        "Canne à Sucre",      6,   2),
            ShopItem.of(Material.SUGAR,             "Sucre",              5,   2),
            ShopItem.of(Material.CACTUS,            "Cactus",             5,   2),
            ShopItem.of(Material.BAMBOO,            "Bambou",             4,   1),
            ShopItem.of(Material.COCOA_BEANS,       "Fèves de Cacao",    10,   4),
            ShopItem.of(Material.NETHER_WART,       "Verrue du Nether",  15,   6),
            ShopItem.of(Material.SWEET_BERRIES,     "Baies Douces",      10,   4),
            ShopItem.of(Material.GLOW_BERRIES,      "Baies Lumineuses",  12,   5),
            ShopItem.of(Material.CHORUS_FRUIT,      "Fruit Chorus",      20,   8),
            ShopItem.of(Material.SEA_PICKLE,        "Cornichon de Mer",  12,   5),
            ShopItem.of(Material.KELP,              "Algues",             4,   1),
            ShopItem.of(Material.DRIED_KELP,        "Algues Séchées",     5,   2),
            ShopItem.of(Material.WHEAT_SEEDS,       "Graines de Blé",     3,   1),
            ShopItem.of(Material.APPLE,             "Pomme",             10,   4),
            ShopItem.of(Material.BREAD,             "Pain",              15,   6),
            ShopItem.of(Material.HAY_BLOCK,         "Balle de Foin",     60,  25),
            ShopItem.of(Material.PUMPKIN_SEEDS,     "Graines Citrouille", 5,   2),
            ShopItem.of(Material.MELON_SEEDS,       "Graines Melon",      5,   2),
            ShopItem.of(Material.TORCHFLOWER_SEEDS, "Graines Torchfleur",20,   8),
            ShopItem.of(Material.PITCHER_POD,       "Pod Pitcher",       20,   8)
    )),

    // ════════════════════════════════════════════════
    MOBS("§c⚔️ Mobs", Material.BONE, List.of(
            // ── Passifs ──
            ShopItem.of(Material.PORKCHOP,          "Côtelette Porc Crue",   6,   3),
            ShopItem.of(Material.COOKED_PORKCHOP,   "Côtelette Porc Cuite", 12,   6),
            ShopItem.of(Material.BEEF,              "Bœuf Cru",              6,   3),
            ShopItem.of(Material.COOKED_BEEF,       "Steak",                12,   6),
            ShopItem.of(Material.MUTTON,            "Mouton Cru",            6,   3),
            ShopItem.of(Material.COOKED_MUTTON,     "Mouton Cuit",          12,   6),
            ShopItem.of(Material.CHICKEN,           "Poulet Cru",            5,   2),
            ShopItem.of(Material.COOKED_CHICKEN,    "Poulet Cuit",          10,   5),
            ShopItem.of(Material.RABBIT,            "Lapin Cru",             6,   3),
            ShopItem.of(Material.COOKED_RABBIT,     "Lapin Cuit",           12,   6),
            ShopItem.of(Material.LEATHER,           "Cuir",                 15,   6),
            ShopItem.of(Material.FEATHER,           "Plume",                10,   4),
            ShopItem.of(Material.WHITE_WOOL,        "Laine",                10,   4),
            ShopItem.of(Material.RABBIT_HIDE,       "Peau de Lapin",        10,   4),
            ShopItem.of(Material.RABBIT_FOOT,       "Pied de Lapin",        40,  18),
            ShopItem.of(Material.HONEYCOMB,         "Rayon de Miel",        20,   8),
            ShopItem.of(Material.HONEY_BOTTLE,      "Bouteille de Miel",    15,   6),
            // ── Hostile ──
            ShopItem.of(Material.ROTTEN_FLESH,      "Chair Pourrie",         4,   2),
            ShopItem.of(Material.BONE,              "Os",                   12,   5),
            ShopItem.of(Material.BONE_MEAL,         "Farine d'Os",           5,   2),
            ShopItem.of(Material.ARROW,             "Flèche",                5,   2),
            ShopItem.of(Material.GUNPOWDER,         "Poudre à Canon",       20,   8),
            ShopItem.of(Material.STRING,            "Ficelle",              12,   5),
            ShopItem.of(Material.SPIDER_EYE,        "Oeil d'Araignée",      15,   6),
            ShopItem.of(Material.ENDER_PEARL,       "Perle de l'Ender",     40,  15),
            ShopItem.of(Material.BLAZE_ROD,         "Baguette de Blaze",    50,  20),
            ShopItem.of(Material.BLAZE_POWDER,      "Poudre de Blaze",      25,  10),
            ShopItem.of(Material.MAGMA_CREAM,       "Crème de Magma",       30,  12),
            ShopItem.of(Material.SLIME_BALL,        "Boule de Slime",       20,   8),
            ShopItem.of(Material.GHAST_TEAR,        "Larme de Ghast",       80,  35),
            ShopItem.of(Material.GOLD_NUGGET,       "Pépite d'Or",           5,   2),
            ShopItem.of(Material.IRON_NUGGET,       "Pépite de Fer",         3,   1),
            ShopItem.of(Material.PRISMARINE_SHARD,  "Éclat Prismarine",     18,   7),
            ShopItem.of(Material.PRISMARINE_CRYSTALS,"Cristaux Prismarine", 25,  10),
            ShopItem.of(Material.SHULKER_SHELL,     "Coquille Shulker",    200,  80),
            ShopItem.of(Material.PHANTOM_MEMBRANE,  "Membrane Fantôme",     40,  16),
            ShopItem.of(Material.INK_SAC,           "Sac d'Encre",          12,   5),
            ShopItem.of(Material.GLOW_INK_SAC,      "Encre Lumineuse",      25,  10),
            ShopItem.of(Material.NETHER_STAR,       "Étoile du Nether",   5000,2000),
            ShopItem.of(Material.WITHER_SKELETON_SKULL,"Crâne Wither",      600, 250),
            ShopItem.of(Material.DRAGON_BREATH,     "Souffle du Dragon",   300, 100),
            ShopItem.of(Material.SUGAR,             "Sucre (Sorcière)",      5,   2),
            ShopItem.of(Material.STICK,             "Bâton (Sorcière)",      3,   1),
            // ── Poissons ──
            ShopItem.of(Material.COD,               "Morue Crue",            5,   2),
            ShopItem.of(Material.COOKED_COD,        "Morue Cuite",          10,   5),
            ShopItem.of(Material.SALMON,            "Saumon Cru",            6,   3),
            ShopItem.of(Material.COOKED_SALMON,     "Saumon Cuit",          12,   6),
            ShopItem.of(Material.TROPICAL_FISH,     "Poisson Tropical",     15,   6),
            ShopItem.of(Material.PUFFERFISH,        "Poisson Gonflant",     20,   8),
            ShopItem.of(Material.INK_SAC,           "Encre",                12,   5)
    )),

    // ════════════════════════════════════════════════
    REDSTONE("§e🔴 Redstone", Material.REDSTONE, List.of(
            ShopItem.of(Material.REDSTONE,          "Redstone",             12,   4),
            ShopItem.of(Material.REPEATER,          "Répéteur",             30,  12),
            ShopItem.of(Material.COMPARATOR,        "Comparateur",          35,  14),
            ShopItem.of(Material.PISTON,            "Piston",               40,  16),
            ShopItem.of(Material.STICKY_PISTON,     "Piston Collant",       50,  20),
            ShopItem.of(Material.DISPENSER,         "Distributeur",         60,  25),
            ShopItem.of(Material.DROPPER,           "Droppeur",             50,  20),
            ShopItem.of(Material.HOPPER,            "Entonnoir",            80,  32),
            ShopItem.of(Material.OBSERVER,          "Observateur",          60,  25),
            ShopItem.of(Material.LEVER,             "Levier",               10,   4),
            ShopItem.of(Material.REDSTONE_TORCH,    "Torche Redstone",      10,   4),
            ShopItem.of(Material.TRIPWIRE_HOOK,     "Crochet Fil",          15,   6),
            ShopItem.of(Material.RAIL,              "Rail",                 15,   6),
            ShopItem.of(Material.POWERED_RAIL,      "Rail Propulseur",      40,  16),
            ShopItem.of(Material.DETECTOR_RAIL,     "Rail Détecteur",       35,  14),
            ShopItem.of(Material.ACTIVATOR_RAIL,    "Rail Activateur",      35,  14),
            ShopItem.of(Material.MINECART,          "Minecart",             60,  25),
            ShopItem.of(Material.TNT,               "TNT",                  80,  30),
            ShopItem.of(Material.DAYLIGHT_DETECTOR, "Détecteur Lumière",    50,  20),
            ShopItem.of(Material.LIGHTNING_ROD,     "Paratonnerre",         80,  35),
            ShopItem.of(Material.SCULK_SENSOR,      "Capteur Sculk",       120,  50),
            ShopItem.of(Material.CALIBRATED_SCULK_SENSOR,"Capteur Calibré",200, 80),
            ShopItem.of(Material.TARGET,            "Cible",                40,  16),
            ShopItem.of(Material.CRAFTER,           "Autocrafter",         200,  80)
    )),

    // ════════════════════════════════════════════════
    DIVERS("§b🎲 Divers", Material.BOOKSHELF, List.of(
            ShopItem.of(Material.GLASS,             "Verre",                10,   3),
            ShopItem.of(Material.GLASS_PANE,        "Vitre",                 5,   2),
            ShopItem.of(Material.BOOK,              "Livre",                20,   8),
            ShopItem.of(Material.BOOKSHELF,         "Bibliothèque",         80,  30),
            ShopItem.of(Material.ENCHANTING_TABLE,  "Table Enchantement",  500, 200),
            ShopItem.of(Material.ANVIL,             "Enclume",             200,  80),
            ShopItem.of(Material.GRINDSTONE,        "Meule",                60,  25),
            ShopItem.of(Material.SMITHING_TABLE,    "Table de Forge",       80,  30),
            ShopItem.of(Material.CRAFTING_TABLE,    "Table de Craft",       20,   8),
            ShopItem.of(Material.FURNACE,           "Fourneau",             25,  10),
            ShopItem.of(Material.BLAST_FURNACE,     "Haut Fourneau",        80,  30),
            ShopItem.of(Material.SMOKER,            "Fumoir",               80,  30),
            ShopItem.of(Material.CHEST,             "Coffre",               20,   8),
            ShopItem.of(Material.TRAPPED_CHEST,     "Coffre Piégé",         25,  10),
            ShopItem.of(Material.BARREL,            "Tonneau",              25,  10),
            ShopItem.of(Material.SHULKER_BOX,       "Boîte Shulker",       300, 120),
            ShopItem.of(Material.WATER_BUCKET,      "Seau d'Eau",           30,  10),
            ShopItem.of(Material.LAVA_BUCKET,       "Seau de Lave",         50,  20),
            ShopItem.of(Material.MILK_BUCKET,       "Seau de Lait",         20,   8),
            ShopItem.of(Material.NAME_TAG,          "Étiquette",           150,  60),
            ShopItem.of(Material.SADDLE,            "Selle",               120,  50),
            ShopItem.of(Material.LEAD,              "Laisse",               30,  12),
            ShopItem.of(Material.GLOWSTONE,         "Glowstone",            40,  16),
            ShopItem.of(Material.GLOWSTONE_DUST,    "Poudre Glowstone",     12,   5),
            ShopItem.of(Material.SEA_LANTERN,       "Lanterne de Mer",      50,  20),
            ShopItem.of(Material.TORCH,             "Torche",                2,   1),
            ShopItem.of(Material.SOUL_TORCH,        "Torche des Âmes",       5,   2),
            ShopItem.of(Material.LANTERN,           "Lanterne",             15,   6),
            ShopItem.of(Material.SOUL_LANTERN,      "Lanterne des Âmes",    18,   7),
            ShopItem.of(Material.SHROOMLIGHT,       "Shroomlight",          40,  16),
            ShopItem.of(Material.COMPOSTER,         "Composteur",           40,  16),
            ShopItem.of(Material.BELL,              "Cloche",              200,  80),
            ShopItem.of(Material.EXPERIENCE_BOTTLE, "Bouteille XP",         50,  15),
            ShopItem.of(Material.NETHER_BRICK,      "Brique du Nether",      8,   3),
            ShopItem.of(Material.NETHER_BRICKS,     "Briques Nether",       30,  12),
            ShopItem.of(Material.ITEM_FRAME,        "Cadre",                15,   6),
            ShopItem.of(Material.PAINTING,          "Tableau",              15,   6),
            // Laines ×16
            ShopItem.of(Material.WHITE_WOOL,        "Laine Blanche",        10,   4),
            ShopItem.of(Material.ORANGE_WOOL,       "Laine Orange",         10,   4),
            ShopItem.of(Material.MAGENTA_WOOL,      "Laine Magenta",        10,   4),
            ShopItem.of(Material.LIGHT_BLUE_WOOL,   "Laine Bleu Clair",     10,   4),
            ShopItem.of(Material.YELLOW_WOOL,       "Laine Jaune",          10,   4),
            ShopItem.of(Material.LIME_WOOL,         "Laine Verte Claire",   10,   4),
            ShopItem.of(Material.PINK_WOOL,         "Laine Rose",           10,   4),
            ShopItem.of(Material.GRAY_WOOL,         "Laine Grise",          10,   4),
            ShopItem.of(Material.LIGHT_GRAY_WOOL,   "Laine Gris Clair",     10,   4),
            ShopItem.of(Material.CYAN_WOOL,         "Laine Cyan",           10,   4),
            ShopItem.of(Material.PURPLE_WOOL,       "Laine Violette",       10,   4),
            ShopItem.of(Material.BLUE_WOOL,         "Laine Bleue",          10,   4),
            ShopItem.of(Material.BROWN_WOOL,        "Laine Marron",         10,   4),
            ShopItem.of(Material.GREEN_WOOL,        "Laine Verte",          10,   4),
            ShopItem.of(Material.RED_WOOL,          "Laine Rouge",          10,   4),
            ShopItem.of(Material.BLACK_WOOL,        "Laine Noire",          10,   4),
            // Bétons ×16
            ShopItem.of(Material.WHITE_CONCRETE,    "Béton Blanc",          12,   5),
            ShopItem.of(Material.ORANGE_CONCRETE,   "Béton Orange",         12,   5),
            ShopItem.of(Material.MAGENTA_CONCRETE,  "Béton Magenta",        12,   5),
            ShopItem.of(Material.LIGHT_BLUE_CONCRETE,"Béton Bleu Clair",    12,   5),
            ShopItem.of(Material.YELLOW_CONCRETE,   "Béton Jaune",          12,   5),
            ShopItem.of(Material.LIME_CONCRETE,     "Béton Vert Clair",     12,   5),
            ShopItem.of(Material.PINK_CONCRETE,     "Béton Rose",           12,   5),
            ShopItem.of(Material.GRAY_CONCRETE,     "Béton Gris",           12,   5),
            ShopItem.of(Material.LIGHT_GRAY_CONCRETE,"Béton Gris Clair",    12,   5),
            ShopItem.of(Material.CYAN_CONCRETE,     "Béton Cyan",           12,   5),
            ShopItem.of(Material.PURPLE_CONCRETE,   "Béton Violet",         12,   5),
            ShopItem.of(Material.BLUE_CONCRETE,     "Béton Bleu",           12,   5),
            ShopItem.of(Material.BROWN_CONCRETE,    "Béton Marron",         12,   5),
            ShopItem.of(Material.GREEN_CONCRETE,    "Béton Vert",           12,   5),
            ShopItem.of(Material.RED_CONCRETE,      "Béton Rouge",          12,   5),
            ShopItem.of(Material.BLACK_CONCRETE,    "Béton Noir",           12,   5)
    )),

    // ════════════════════════════════════════════════
    SPAWNERS("§d🐾 Spawners", Material.SPAWNER, List.of(
            ShopItem.gem(Material.SPAWNER, "Spawner Cochon",        150),
            ShopItem.gem(Material.SPAWNER, "Spawner Vache",         150),
            ShopItem.gem(Material.SPAWNER, "Spawner Mouton",        150),
            ShopItem.gem(Material.SPAWNER, "Spawner Poulet",        150),
            ShopItem.gem(Material.SPAWNER, "Spawner Lapin",         150),
            ShopItem.gem(Material.SPAWNER, "Spawner Zombie",        200),
            ShopItem.gem(Material.SPAWNER, "Spawner Squelette",     200),
            ShopItem.gem(Material.SPAWNER, "Spawner Araignée",      200),
            ShopItem.gem(Material.SPAWNER, "Spawner Creeper",       250),
            ShopItem.gem(Material.SPAWNER, "Spawner Slime",         250),
            ShopItem.gem(Material.SPAWNER, "Spawner Sorcière",      300),
            ShopItem.gem(Material.SPAWNER, "Spawner Blaze",         400),
            ShopItem.gem(Material.SPAWNER, "Spawner Enderman",      500),
            ShopItem.gem(Material.SPAWNER, "Spawner Guardian",      600),
            ShopItem.gem(Material.SPAWNER, "Spawner Wither Skelet", 700),
            ShopItem.gem(Material.SPAWNER, "Spawner Golem de Fer",  800)
    ));

    private final String                  displayName;
    private final Material                icon;
    private final java.util.List<ShopItem> items;

    ShopCategory(String displayName, Material icon, java.util.List<ShopItem> items) {
        this.displayName = displayName;
        this.icon        = icon;
        this.items       = items;
    }

    public String                   getDisplayName() { return displayName; }
    public Material                 getIcon()        { return icon; }
    public java.util.List<ShopItem> getItems()       { return items; }

    /** Retourne une Map<Material, ShopItem> de tous les items vendables du shop */
    public static java.util.Map<org.bukkit.Material, ShopItem> buildSellMap() {
        java.util.Map<org.bukkit.Material, ShopItem> map = new java.util.HashMap<>();
        for (ShopCategory cat : values()) {
            for (ShopItem si : cat.getItems()) {
                if (si.isSellable()) map.put(si.material(), si);
            }
        }
        return map;
    }
}