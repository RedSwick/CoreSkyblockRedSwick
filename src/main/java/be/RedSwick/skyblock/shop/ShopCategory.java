package be.RedSwick.skyblock.shop;

import org.bukkit.Material;
import java.util.List;

/**
 * ShopCategory — Prix révisés
 *
 * RÈGLES PRIX :
 * - Blocs déco/bois/ressources achetables : 50 coins
 * - Terre, herbe, mousse : achat 100, vente 5
 * - Cobble, pierre : vente 1 seulement
 * - Canne à sucre, bamboo : vente 3/2
 * - Blé : vente 3, pain : vente 10, balle de foin : vente 29
 * - Pomme : vente 15, cactus : vente 1
 * - Graines/pousses/cultures achetables : 150
 * - Spawners : achat gems, revente coins
 * - Minerais : vente seulement
 * - Ratio vente/achat : ~35-40% max
 */
public enum ShopCategory {

    // ════════════════════════════════════════════════
    //  1. RESSOURCES
    // ════════════════════════════════════════════════
    RESSOURCES("§7🪨 Ressources", Material.COBBLESTONE, List.of(
            // Cobble/pierre : vente seulement à 1
            ShopItem.sellOnly(Material.COBBLESTONE,         "Cobblestone",          1),
            ShopItem.sellOnly(Material.STONE,               "Pierre",               1),
            ShopItem.sellOnly(Material.DEEPSLATE,           "Deepslate",            1),
            // Blocs achetables à 50
            ShopItem.of(Material.GRANITE,                   "Granite",          50,  2),
            ShopItem.of(Material.DIORITE,                   "Diorite",          50,  2),
            ShopItem.of(Material.ANDESITE,                  "Andesite",         50,  2),
            ShopItem.of(Material.TUFF,                      "Tuff",             50,  2),
            ShopItem.of(Material.CALCITE,                   "Calcite",          50,  2),
            ShopItem.of(Material.DRIPSTONE_BLOCK,           "Bloc Dripstone",   50,  2),
            // Terre / herbe / mousse : achat 100, vente 5
            ShopItem.of(Material.DIRT,                      "Terre",           100,  5),
            ShopItem.of(Material.GRASS_BLOCK,               "Herbe",           100,  5),
            ShopItem.of(Material.PODZOL,                    "Podzol",          100,  5),
            ShopItem.of(Material.MYCELIUM,                  "Mycélium",        100,  5),
            ShopItem.of(Material.MOSS_BLOCK,                "Bloc de Mousse",  100,  5),
            ShopItem.of(Material.MUD,                       "Boue",            100,  5),
            ShopItem.of(Material.ROOTED_DIRT,               "Terre Enracinée", 100,  5),
            // Sable / gravier
            ShopItem.of(Material.GRAVEL,                    "Gravier",          50,  2),
            ShopItem.of(Material.SAND,                      "Sable",            50,  2),
            ShopItem.of(Material.RED_SAND,                  "Sable Rouge",      50,  2),
            // Argile / terracotta
            ShopItem.of(Material.CLAY_BALL,                 "Argile",           50,  4),
            ShopItem.of(Material.CLAY,                      "Bloc d'Argile",    50, 14),
            ShopItem.of(Material.TERRACOTTA,                "Terre Cuite",      50,  5),
            // Divers
            ShopItem.of(Material.FLINT,                     "Silex",            50,  4),
            ShopItem.of(Material.OBSIDIAN,                  "Obsidienne",       50, 20),
            ShopItem.of(Material.CRYING_OBSIDIAN,           "Obsidienne Pleurante",50,20),
            ShopItem.of(Material.ICE,                       "Glace",            50,  6),
            ShopItem.of(Material.PACKED_ICE,                "Glace Compacte",   50, 10),
            ShopItem.of(Material.BLUE_ICE,                  "Glace Bleue",      50, 16),
            ShopItem.of(Material.SNOW_BLOCK,                "Bloc de Neige",    50,  4),
            ShopItem.of(Material.SNOWBALL,                  "Boule de Neige",   50,  1),
            ShopItem.of(Material.SPONGE,                    "Éponge",          200, 80),
            ShopItem.of(Material.MUDDY_MANGROVE_ROOTS,      "Racines Boueuses", 50,  3)
    )),

    // ════════════════════════════════════════════════
    //  2. BOIS
    // ════════════════════════════════════════════════
    BOIS("§6🪵 Bois", Material.OAK_LOG, List.of(
            // Bûches : achat 50, vente 4
            ShopItem.of(Material.OAK_LOG,               "Bois Chêne",          50,  4),
            ShopItem.of(Material.SPRUCE_LOG,             "Bois Épicéa",         50,  4),
            ShopItem.of(Material.BIRCH_LOG,              "Bois Bouleau",        50,  4),
            ShopItem.of(Material.JUNGLE_LOG,             "Bois Jungle",         50,  4),
            ShopItem.of(Material.ACACIA_LOG,             "Bois Acacia",         50,  4),
            ShopItem.of(Material.DARK_OAK_LOG,           "Bois Chêne Noir",     50,  4),
            ShopItem.of(Material.MANGROVE_LOG,           "Bois Palétuvier",     50,  5),
            ShopItem.of(Material.CHERRY_LOG,             "Bois Cerisier",       50,  6),
            ShopItem.of(Material.BAMBOO_BLOCK,           "Bloc Bambou",         50,  3),
            ShopItem.of(Material.CRIMSON_STEM,           "Tige Cramoisie",      50,  5),
            ShopItem.of(Material.WARPED_STEM,            "Tige Déformée",       50,  5),
            // Planches : achat 50, vente 1
            ShopItem.of(Material.OAK_PLANKS,             "Planches Chêne",      50,  1),
            ShopItem.of(Material.SPRUCE_PLANKS,          "Planches Épicéa",     50,  1),
            ShopItem.of(Material.BIRCH_PLANKS,           "Planches Bouleau",    50,  1),
            ShopItem.of(Material.JUNGLE_PLANKS,          "Planches Jungle",     50,  1),
            ShopItem.of(Material.ACACIA_PLANKS,          "Planches Acacia",     50,  1),
            ShopItem.of(Material.DARK_OAK_PLANKS,        "Planches Chêne Noir", 50,  1),
            ShopItem.of(Material.MANGROVE_PLANKS,        "Planches Palétuvier", 50,  1),
            ShopItem.of(Material.CHERRY_PLANKS,          "Planches Cerisier",   50,  2),
            ShopItem.of(Material.BAMBOO_PLANKS,          "Planches Bambou",     50,  1),
            ShopItem.of(Material.CRIMSON_PLANKS,         "Planches Cramoisies", 50,  1),
            ShopItem.of(Material.WARPED_PLANKS,          "Planches Déformées",  50,  1),
            // Bambou : achat 150, vente 2 (farmable)
            ShopItem.of(Material.BAMBOO,                 "Bambou",             150,  2),
            // Bâton
            ShopItem.of(Material.STICK,                  "Bâton",               50,  1),
            // Feuilles
            ShopItem.of(Material.OAK_LEAVES,             "Feuilles Chêne",      50,  1),
            ShopItem.of(Material.BIRCH_LEAVES,           "Feuilles Bouleau",    50,  1),
            ShopItem.of(Material.SPRUCE_LEAVES,          "Feuilles Épicéa",     50,  1),
            ShopItem.of(Material.JUNGLE_LEAVES,          "Feuilles Jungle",     50,  1),
            ShopItem.of(Material.CHERRY_LEAVES,          "Feuilles Cerisier",   50,  1),
            ShopItem.of(Material.MANGROVE_LEAVES,        "Feuilles Palétuvier", 50,  1),
            // Pousses : achat 250 (rares à trouver), vente 5
            ShopItem.of(Material.OAK_SAPLING,            "Pousse Chêne",       250,  5),
            ShopItem.of(Material.SPRUCE_SAPLING,         "Pousse Épicéa",      250,  5),
            ShopItem.of(Material.BIRCH_SAPLING,          "Pousse Bouleau",     250,  5),
            ShopItem.of(Material.JUNGLE_SAPLING,         "Pousse Jungle",      250,  5),
            ShopItem.of(Material.ACACIA_SAPLING,         "Pousse Acacia",      250,  5),
            ShopItem.of(Material.DARK_OAK_SAPLING,       "Pousse Chêne Noir",  250,  5),
            ShopItem.of(Material.CHERRY_SAPLING,         "Pousse Cerisier",    250,  5)
    )),

    // ════════════════════════════════════════════════
    //  3. AGRICULTURE
    // ════════════════════════════════════════════════
    AGRICULTURE("§a🌾 Agriculture", Material.WHEAT, List.of(
            // Blé : vente 3 seulement (farmable)
            ShopItem.sellOnly(Material.WHEAT,               "Blé",                  3),
            // Graines : achat 150
            ShopItem.of(Material.WHEAT_SEEDS,               "Graines de Blé",     150,  1),
            ShopItem.of(Material.BEETROOT_SEEDS,            "Graines Betterave",  150,  1),
            ShopItem.of(Material.PUMPKIN_SEEDS,             "Graines Citrouille", 150,  1),
            ShopItem.of(Material.MELON_SEEDS,               "Graines Melon",      150,  1),
            // Légumes : achat 150, vente logique
            ShopItem.of(Material.CARROT,                    "Carotte",            150,  3),
            ShopItem.of(Material.POTATO,                    "Pomme de Terre",     150,  3),
            ShopItem.of(Material.BEETROOT,                  "Betterave",          150,  3),
            ShopItem.sellOnly(Material.BAKED_POTATO,        "Pomme de Terre Cuite", 5),
            // Melons / citrouille : farmables
            ShopItem.of(Material.MELON_SLICE,               "Tranche de Melon",   150,  2),
            ShopItem.of(Material.PUMPKIN,                   "Citrouille",         150,  5),
            // Canne à sucre : achat 150, vente 3
            ShopItem.of(Material.SUGAR_CANE,                "Canne à Sucre",      150,  3),
            ShopItem.of(Material.SUGAR,                     "Sucre",              150,  2),
            // Cactus : vente 1 seulement (anti-farm abusif)
            ShopItem.sellOnly(Material.CACTUS,              "Cactus",               1),
            // Bambou : voir catégorie BOIS
            // Fèves de cacao : achat 150
            ShopItem.of(Material.COCOA_BEANS,               "Fèves de Cacao",     150,  4),
            // Nether Wart
            ShopItem.of(Material.NETHER_WART,               "Verrue du Nether",   150,  6),
            // Baies
            ShopItem.of(Material.SWEET_BERRIES,             "Baies Douces",       150,  4),
            ShopItem.of(Material.GLOW_BERRIES,              "Baies Lumineuses",   150,  5),
            // Autres farmables
            ShopItem.of(Material.CHORUS_FRUIT,              "Fruit Chorus",       150,  8),
            ShopItem.of(Material.KELP,                      "Algues",             150,  1),
            ShopItem.sellOnly(Material.DRIED_KELP,          "Algues Séchées",       2),
            ShopItem.of(Material.SEA_PICKLE,                "Cornichon de Mer",   150,  5),
            ShopItem.of(Material.TORCHFLOWER_SEEDS,         "Graines Torchfleur", 150,  8),
            ShopItem.of(Material.PITCHER_POD,               "Pod Pitcher",        150,  8),
            // Pain / aliments — vente seulement (craft depuis blé)
            ShopItem.sellOnly(Material.BREAD,               "Pain",                10),
            ShopItem.sellOnly(Material.APPLE,               "Pomme",               15),
            ShopItem.sellOnly(Material.HAY_BLOCK,           "Balle de Foin",       29),
            ShopItem.sellOnly(Material.COOKIE,              "Cookie",               4),
            ShopItem.sellOnly(Material.PUMPKIN_PIE,         "Tarte Citrouille",    10),
            ShopItem.sellOnly(Material.CAKE,                "Gâteau",              40),
            // Champignons
            ShopItem.of(Material.RED_MUSHROOM,              "Champignon Rouge",   150,  3),
            ShopItem.of(Material.BROWN_MUSHROOM,            "Champignon Marron",  150,  3),
            ShopItem.sellOnly(Material.RED_MUSHROOM_BLOCK,  "Bloc Champ. Rouge",   10),
            ShopItem.sellOnly(Material.BROWN_MUSHROOM_BLOCK,"Bloc Champ. Marron",  10),
            ShopItem.sellOnly(Material.MUSHROOM_STEW,       "Soupe Champignon",     8),
            // Fleurs
            ShopItem.of(Material.DANDELION,                 "Pissenlit",          150,  1),
            ShopItem.of(Material.POPPY,                     "Coquelicot",         150,  1),
            ShopItem.of(Material.BLUE_ORCHID,               "Orchidée Bleue",     150,  2),
            ShopItem.of(Material.ALLIUM,                    "Allium",             150,  1),
            ShopItem.of(Material.CORNFLOWER,                "Bleuet",             150,  1),
            ShopItem.of(Material.SUNFLOWER,                 "Tournesol",          150,  3),
            ShopItem.of(Material.ROSE_BUSH,                 "Buisson de Roses",   150,  2),
            ShopItem.of(Material.WITHER_ROSE,               "Rose Wither",        500, 20),
            ShopItem.of(Material.TORCHFLOWER,               "Torchfleur",         500, 12),
            // Miel
            ShopItem.of(Material.HONEYCOMB,                 "Rayon de Miel",      150,  8),
            ShopItem.of(Material.HONEY_BOTTLE,              "Bouteille de Miel",  150,  6),
            ShopItem.sellOnly(Material.HONEYCOMB_BLOCK,     "Bloc Rayon de Miel",  28)
    )),

    // ════════════════════════════════════════════════
    //  4. MOBS
    // ════════════════════════════════════════════════
    MOBS("§c⚔️ Mobs", Material.BONE, List.of(
            // Passifs — achat possible pour démarrer
            ShopItem.of(Material.PORKCHOP,              "Côtelette Porc Crue",   50,  3),
            ShopItem.of(Material.COOKED_PORKCHOP,       "Côtelette Porc Cuite",  50,  5),
            ShopItem.of(Material.BEEF,                  "Bœuf Cru",              50,  3),
            ShopItem.of(Material.COOKED_BEEF,           "Steak",                 50,  5),
            ShopItem.of(Material.MUTTON,                "Mouton Cru",            50,  3),
            ShopItem.of(Material.COOKED_MUTTON,         "Mouton Cuit",           50,  5),
            ShopItem.of(Material.CHICKEN,               "Poulet Cru",            50,  2),
            ShopItem.of(Material.COOKED_CHICKEN,        "Poulet Cuit",           50,  4),
            ShopItem.of(Material.RABBIT,                "Lapin Cru",             50,  3),
            ShopItem.of(Material.COOKED_RABBIT,         "Lapin Cuit",            50,  5),
            ShopItem.of(Material.LEATHER,               "Cuir",                  50,  6),
            ShopItem.of(Material.FEATHER,               "Plume",                 50,  4),
            ShopItem.of(Material.WHITE_WOOL,            "Laine",                 50,  4),
            ShopItem.of(Material.RABBIT_HIDE,           "Peau de Lapin",         50,  4),
            ShopItem.of(Material.RABBIT_FOOT,           "Pied de Lapin",        200, 18),
            ShopItem.of(Material.EGG,                   "Œuf",                   50,  1),
            ShopItem.of(Material.TURTLE_SCUTE,          "Écaille Tortue",       200, 25),
            ShopItem.of(Material.ARMADILLO_SCUTE,       "Écaille Armadillo",    200, 20),
            ShopItem.of(Material.GOAT_HORN,             "Corne de Chèvre",      300, 35),
            ShopItem.of(Material.HONEYCOMB,             "Rayon de Miel",         50,  8),
            // Hostile — vente seulement (obtenu via spawners)
            ShopItem.sellOnly(Material.ROTTEN_FLESH,    "Chair Pourrie",            2),
            ShopItem.sellOnly(Material.BONE,            "Os",                       5),
            ShopItem.sellOnly(Material.BONE_MEAL,       "Farine d'Os",              2),
            ShopItem.sellOnly(Material.ARROW,           "Flèche",                   2),
            ShopItem.sellOnly(Material.GUNPOWDER,       "Poudre à Canon",           8),
            ShopItem.sellOnly(Material.STRING,          "Ficelle",                  5),
            ShopItem.sellOnly(Material.SPIDER_EYE,      "Œil d'Araignée",           6),
            ShopItem.sellOnly(Material.FERMENTED_SPIDER_EYE,"Œil Fermenté",        12),
            ShopItem.sellOnly(Material.ENDER_PEARL,     "Perle de l'Ender",        16),
            ShopItem.sellOnly(Material.ENDER_EYE,       "Œil de l'Ender",          30),
            ShopItem.sellOnly(Material.BLAZE_ROD,       "Baguette de Blaze",       20),
            ShopItem.sellOnly(Material.BLAZE_POWDER,    "Poudre de Blaze",         10),
            ShopItem.sellOnly(Material.MAGMA_CREAM,     "Crème de Magma",          12),
            ShopItem.sellOnly(Material.SLIME_BALL,      "Boule de Slime",           8),
            ShopItem.sellOnly(Material.GHAST_TEAR,      "Larme de Ghast",          35),
            ShopItem.sellOnly(Material.GOLD_NUGGET,     "Pépite d'Or",              2),
            ShopItem.sellOnly(Material.IRON_NUGGET,     "Pépite de Fer",            1),
            ShopItem.sellOnly(Material.PRISMARINE_SHARD,"Éclat Prismarine",         7),
            ShopItem.sellOnly(Material.PRISMARINE_CRYSTALS,"Cristaux Prismarine",  10),
            ShopItem.sellOnly(Material.SHULKER_SHELL,   "Coquille Shulker",        80),
            ShopItem.sellOnly(Material.PHANTOM_MEMBRANE,"Membrane Fantôme",        16),
            ShopItem.sellOnly(Material.INK_SAC,         "Sac d'Encre",              5),
            ShopItem.sellOnly(Material.GLOW_INK_SAC,    "Encre Lumineuse",         10),
            ShopItem.sellOnly(Material.WITHER_SKELETON_SKULL,"Crâne Wither",      250),
            ShopItem.sellOnly(Material.NETHER_STAR,     "Étoile du Nether",      2000),
            ShopItem.sellOnly(Material.DRAGON_BREATH,   "Souffle du Dragon",      100),
            ShopItem.sellOnly(Material.HEART_OF_THE_SEA,"Cœur de la Mer",         600),
            ShopItem.sellOnly(Material.TRIDENT,         "Trident",                500),
            ShopItem.sellOnly(Material.NAUTILUS_SHELL,  "Coquillage Nautile",      60),
            ShopItem.sellOnly(Material.BREEZE_ROD,      "Baguette de Brise",       80),
            // Poissons
            ShopItem.sellOnly(Material.COD,             "Morue Crue",               2),
            ShopItem.sellOnly(Material.COOKED_COD,      "Morue Cuite",              4),
            ShopItem.sellOnly(Material.SALMON,          "Saumon Cru",               3),
            ShopItem.sellOnly(Material.COOKED_SALMON,   "Saumon Cuit",              5),
            ShopItem.sellOnly(Material.TROPICAL_FISH,   "Poisson Tropical",         6),
            ShopItem.sellOnly(Material.PUFFERFISH,      "Poisson Gonflant",         8)
    )),

    // ════════════════════════════════════════════════
    //  5. MINERAIS — vente seulement
    // ════════════════════════════════════════════════
    MINERAIS("§8⛏ Minerais", Material.DIAMOND, List.of(
            ShopItem.sellOnly(Material.COAL,                    "Charbon",               6),
            ShopItem.sellOnly(Material.COAL_ORE,               "Minerai Charbon",        8),
            ShopItem.sellOnly(Material.DEEPSLATE_COAL_ORE,     "Minerai Charbon DS",    10),
            ShopItem.sellOnly(Material.RAW_IRON,               "Fer Brut",              12),
            ShopItem.sellOnly(Material.IRON_ORE,               "Minerai Fer",           15),
            ShopItem.sellOnly(Material.DEEPSLATE_IRON_ORE,     "Minerai Fer DS",        18),
            ShopItem.sellOnly(Material.IRON_INGOT,             "Lingot de Fer",         20),
            ShopItem.sellOnly(Material.RAW_COPPER,             "Cuivre Brut",            8),
            ShopItem.sellOnly(Material.COPPER_ORE,             "Minerai Cuivre",        10),
            ShopItem.sellOnly(Material.DEEPSLATE_COPPER_ORE,   "Minerai Cuivre DS",     12),
            ShopItem.sellOnly(Material.COPPER_INGOT,           "Lingot Cuivre",         12),
            ShopItem.sellOnly(Material.RAW_GOLD,               "Or Brut",               25),
            ShopItem.sellOnly(Material.GOLD_ORE,               "Minerai Or",            30),
            ShopItem.sellOnly(Material.DEEPSLATE_GOLD_ORE,     "Minerai Or DS",         35),
            ShopItem.sellOnly(Material.GOLD_INGOT,             "Lingot d'Or",           40),
            ShopItem.sellOnly(Material.LAPIS_LAZULI,           "Lapis Lazuli",          10),
            ShopItem.sellOnly(Material.LAPIS_ORE,              "Minerai Lapis",         18),
            ShopItem.sellOnly(Material.DEEPSLATE_LAPIS_ORE,    "Minerai Lapis DS",      22),
            ShopItem.sellOnly(Material.REDSTONE,               "Redstone",               8),
            ShopItem.sellOnly(Material.REDSTONE_ORE,           "Minerai Redstone",      15),
            ShopItem.sellOnly(Material.DEEPSLATE_REDSTONE_ORE, "Minerai Redstone DS",   18),
            ShopItem.sellOnly(Material.DIAMOND,                "Diamant",              120),
            ShopItem.sellOnly(Material.DIAMOND_ORE,            "Minerai Diamant",      200),
            ShopItem.sellOnly(Material.DEEPSLATE_DIAMOND_ORE,  "Minerai Diamant DS",   250),
            ShopItem.sellOnly(Material.EMERALD,                "Émeraude",              80),
            ShopItem.sellOnly(Material.EMERALD_ORE,            "Minerai Émeraude",     120),
            ShopItem.sellOnly(Material.DEEPSLATE_EMERALD_ORE,  "Minerai Émer. DS",     150),
            ShopItem.sellOnly(Material.QUARTZ,                 "Quartz",                10),
            ShopItem.sellOnly(Material.NETHER_QUARTZ_ORE,      "Minerai Quartz",        12),
            ShopItem.sellOnly(Material.AMETHYST_SHARD,         "Éclat Améthyste",       30),
            ShopItem.sellOnly(Material.ANCIENT_DEBRIS,         "Ancient Debris",      1000),
            ShopItem.sellOnly(Material.NETHERITE_SCRAP,        "Éclat Netherite",      800),
            ShopItem.sellOnly(Material.NETHERITE_INGOT,        "Lingot Netherite",    3500),
            ShopItem.sellOnly(Material.GOLD_NUGGET,            "Pépite d'Or",            5),
            ShopItem.sellOnly(Material.IRON_NUGGET,            "Pépite de Fer",          3)
    )),

    // ════════════════════════════════════════════════
    //  6. NETHER & END
    // ════════════════════════════════════════════════
    NETHER_END("§4🔥 Nether & End", Material.NETHERRACK, List.of(
            ShopItem.of(Material.NETHERRACK,            "Netherrack",           50,  1),
            ShopItem.of(Material.SOUL_SAND,             "Sable des Âmes",       50,  8),
            ShopItem.of(Material.SOUL_SOIL,             "Terre des Âmes",       50,  6),
            ShopItem.of(Material.BASALT,                "Basalte",              50,  4),
            ShopItem.of(Material.BLACKSTONE,            "Pierre Noire",         50,  4),
            ShopItem.of(Material.NETHER_BRICK,          "Brique du Nether",     50,  3),
            ShopItem.of(Material.NETHER_BRICKS,         "Briques Nether",       50, 12),
            ShopItem.of(Material.MAGMA_BLOCK,           "Bloc Magma",           50, 10),
            ShopItem.of(Material.GLOWSTONE,             "Glowstone",            50, 16),
            ShopItem.of(Material.GLOWSTONE_DUST,        "Poudre Glowstone",     50,  5),
            ShopItem.of(Material.NETHER_WART,           "Verrue du Nether",    150,  6),
            ShopItem.of(Material.NETHER_WART_BLOCK,     "Bloc Verrue",          50, 16),
            ShopItem.of(Material.SHROOMLIGHT,           "Shroomlight",          50, 16),
            ShopItem.of(Material.CRIMSON_NYLIUM,        "Nylium Cramoisi",      50,  6),
            ShopItem.of(Material.WARPED_NYLIUM,         "Nylium Déformé",       50,  6),
            ShopItem.of(Material.QUARTZ_BLOCK,          "Bloc Quartz",          50, 14),
            ShopItem.of(Material.SMOOTH_QUARTZ,         "Quartz Lisse",         50, 16),
            ShopItem.of(Material.END_STONE,             "Pierre de l'End",      50, 12),
            ShopItem.of(Material.END_STONE_BRICKS,      "Briques End Stone",    50, 16),
            ShopItem.of(Material.PURPUR_BLOCK,          "Bloc Pourpre",         50, 20),
            ShopItem.of(Material.PURPUR_PILLAR,         "Pilier Pourpre",       50, 22),
            ShopItem.of(Material.CHORUS_FRUIT,          "Fruit Chorus",        150,  8),
            ShopItem.of(Material.POPPED_CHORUS_FRUIT,   "Fruit Chorus Traité",  50,  6)
    )),

    // ════════════════════════════════════════════════
    //  7. REDSTONE
    // ════════════════════════════════════════════════
    REDSTONE("§e⚡ Redstone", Material.REDSTONE, List.of(
            ShopItem.of(Material.REDSTONE,              "Redstone",             50,  4),
            ShopItem.of(Material.REDSTONE_BLOCK,        "Bloc Redstone",        50, 36),
            ShopItem.of(Material.REPEATER,              "Répéteur",             50, 12),
            ShopItem.of(Material.COMPARATOR,            "Comparateur",          50, 14),
            ShopItem.of(Material.PISTON,                "Piston",               50, 16),
            ShopItem.of(Material.STICKY_PISTON,         "Piston Collant",       50, 20),
            ShopItem.of(Material.DISPENSER,             "Distributeur",         50, 25),
            ShopItem.of(Material.DROPPER,               "Droppeur",             50, 20),
            ShopItem.of(Material.HOPPER,                "Entonnoir",            50, 32),
            ShopItem.of(Material.OBSERVER,              "Observateur",          50, 25),
            ShopItem.of(Material.LEVER,                 "Levier",               50,  4),
            ShopItem.of(Material.STONE_BUTTON,          "Bouton Pierre",        50,  3),
            ShopItem.of(Material.OAK_BUTTON,            "Bouton Bois",          50,  2),
            ShopItem.of(Material.STONE_PRESSURE_PLATE,  "Dalle Pression",       50,  6),
            ShopItem.of(Material.OAK_PRESSURE_PLATE,    "Dalle Pression Bois",  50,  5),
            ShopItem.of(Material.REDSTONE_TORCH,        "Torche Redstone",      50,  4),
            ShopItem.of(Material.REDSTONE_LAMP,         "Lampe Redstone",       50, 20),
            ShopItem.of(Material.TRIPWIRE_HOOK,         "Crochet Fil",          50,  6),
            ShopItem.of(Material.RAIL,                  "Rail",                 50,  6),
            ShopItem.of(Material.POWERED_RAIL,          "Rail Propulseur",      50, 16),
            ShopItem.of(Material.DETECTOR_RAIL,         "Rail Détecteur",       50, 14),
            ShopItem.of(Material.ACTIVATOR_RAIL,        "Rail Activateur",      50, 14),
            ShopItem.of(Material.MINECART,              "Minecart",             50, 25),
            ShopItem.of(Material.TNT,                   "TNT",                  50, 30),
            ShopItem.of(Material.DAYLIGHT_DETECTOR,     "Détecteur Lumière",    50, 20),
            ShopItem.of(Material.LIGHTNING_ROD,         "Paratonnerre",         50, 35),
            ShopItem.of(Material.SCULK_SENSOR,          "Capteur Sculk",       100, 50),
            ShopItem.of(Material.CALIBRATED_SCULK_SENSOR,"Capteur Calibré",    200, 80),
            ShopItem.of(Material.TARGET,                "Cible",                50, 16),
            ShopItem.of(Material.CRAFTER,               "Autocrafter",         200, 80),
            ShopItem.of(Material.COPPER_BULB,           "Ampoule Cuivre",       50, 25)
    )),

    // ════════════════════════════════════════════════
    //  8. DECORATION
    // ════════════════════════════════════════════════
    DECORATION("§b🎨 Décoration", Material.BOOKSHELF, List.of(
            ShopItem.of(Material.GLASS,                 "Verre",                50,  3),
            ShopItem.of(Material.GLASS_PANE,            "Vitre",                50,  2),
            ShopItem.of(Material.BOOK,                  "Livre",                50,  8),
            ShopItem.of(Material.BOOKSHELF,             "Bibliothèque",         50, 30),
            ShopItem.of(Material.CHISELED_BOOKSHELF,    "Biblio. Ciselée",      50, 40),
            ShopItem.of(Material.ENCHANTING_TABLE,      "Table Enchantement",  500,200),
            ShopItem.of(Material.ANVIL,                 "Enclume",             200, 80),
            ShopItem.of(Material.GRINDSTONE,            "Meule",                50, 25),
            ShopItem.of(Material.SMITHING_TABLE,        "Table de Forge",       50, 30),
            ShopItem.of(Material.CRAFTING_TABLE,        "Table de Craft",       50,  8),
            ShopItem.of(Material.FURNACE,               "Fourneau",             50, 10),
            ShopItem.of(Material.BLAST_FURNACE,         "Haut Fourneau",        50, 30),
            ShopItem.of(Material.SMOKER,                "Fumoir",               50, 30),
            ShopItem.of(Material.CHEST,                 "Coffre",               50,  8),
            ShopItem.of(Material.TRAPPED_CHEST,         "Coffre Piégé",         50, 10),
            ShopItem.of(Material.BARREL,                "Tonneau",              50, 10),
            ShopItem.of(Material.SHULKER_BOX,           "Boîte Shulker",       300,120),
            ShopItem.of(Material.COMPOSTER,             "Composteur",           50, 16),
            ShopItem.of(Material.BELL,                  "Cloche",              200, 80),
            ShopItem.of(Material.ITEM_FRAME,            "Cadre",                50,  6),
            ShopItem.of(Material.GLOW_ITEM_FRAME,       "Cadre Lumineux",       50, 10),
            ShopItem.of(Material.PAINTING,              "Tableau",              50,  6),
            ShopItem.of(Material.TORCH,                 "Torche",               50,  1),
            ShopItem.of(Material.SOUL_TORCH,            "Torche des Âmes",      50,  2),
            ShopItem.of(Material.LANTERN,               "Lanterne",             50,  6),
            ShopItem.of(Material.SOUL_LANTERN,          "Lanterne des Âmes",    50,  7),
            ShopItem.of(Material.SEA_LANTERN,           "Lanterne de Mer",      50, 20),
            ShopItem.of(Material.NAME_TAG,              "Étiquette",           150, 60),
            ShopItem.of(Material.SADDLE,                "Selle",               120, 50),
            ShopItem.of(Material.LEAD,                  "Laisse",               50, 12),
            ShopItem.of(Material.WATER_BUCKET,          "Seau d'Eau",           50, 10),
            ShopItem.of(Material.LAVA_BUCKET,           "Seau de Lave",         50, 20),
            ShopItem.of(Material.MILK_BUCKET,           "Seau de Lait",         50,  8),
            // Teintures
            ShopItem.of(Material.WHITE_DYE,             "Teinture Blanche",     50,  2),
            ShopItem.of(Material.ORANGE_DYE,            "Teinture Orange",      50,  2),
            ShopItem.of(Material.MAGENTA_DYE,           "Teinture Magenta",     50,  2),
            ShopItem.of(Material.LIGHT_BLUE_DYE,        "Teinture Bleu Clair",  50,  2),
            ShopItem.of(Material.YELLOW_DYE,            "Teinture Jaune",       50,  2),
            ShopItem.of(Material.LIME_DYE,              "Teinture Vert Clair",  50,  2),
            ShopItem.of(Material.PINK_DYE,              "Teinture Rose",        50,  2),
            ShopItem.of(Material.GRAY_DYE,              "Teinture Grise",       50,  2),
            ShopItem.of(Material.LIGHT_GRAY_DYE,        "Teinture Gris Clair",  50,  2),
            ShopItem.of(Material.CYAN_DYE,              "Teinture Cyan",        50,  2),
            ShopItem.of(Material.PURPLE_DYE,            "Teinture Violette",    50,  2),
            ShopItem.of(Material.BLUE_DYE,              "Teinture Bleue",       50,  2),
            ShopItem.of(Material.BROWN_DYE,             "Teinture Marron",      50,  2),
            ShopItem.of(Material.GREEN_DYE,             "Teinture Verte",       50,  2),
            ShopItem.of(Material.RED_DYE,               "Teinture Rouge",       50,  2),
            ShopItem.of(Material.BLACK_DYE,             "Teinture Noire",       50,  2),
            // Laines ×16
            ShopItem.of(Material.WHITE_WOOL,            "Laine Blanche",        50,  4),
            ShopItem.of(Material.ORANGE_WOOL,           "Laine Orange",         50,  4),
            ShopItem.of(Material.MAGENTA_WOOL,          "Laine Magenta",        50,  4),
            ShopItem.of(Material.LIGHT_BLUE_WOOL,       "Laine Bleu Clair",     50,  4),
            ShopItem.of(Material.YELLOW_WOOL,           "Laine Jaune",          50,  4),
            ShopItem.of(Material.LIME_WOOL,             "Laine Verte Claire",   50,  4),
            ShopItem.of(Material.PINK_WOOL,             "Laine Rose",           50,  4),
            ShopItem.of(Material.GRAY_WOOL,             "Laine Grise",          50,  4),
            ShopItem.of(Material.LIGHT_GRAY_WOOL,       "Laine Gris Clair",     50,  4),
            ShopItem.of(Material.CYAN_WOOL,             "Laine Cyan",           50,  4),
            ShopItem.of(Material.PURPLE_WOOL,           "Laine Violette",       50,  4),
            ShopItem.of(Material.BLUE_WOOL,             "Laine Bleue",          50,  4),
            ShopItem.of(Material.BROWN_WOOL,            "Laine Marron",         50,  4),
            ShopItem.of(Material.GREEN_WOOL,            "Laine Verte",          50,  4),
            ShopItem.of(Material.RED_WOOL,              "Laine Rouge",          50,  4),
            ShopItem.of(Material.BLACK_WOOL,            "Laine Noire",          50,  4),
            // Bétons ×16
            ShopItem.of(Material.WHITE_CONCRETE,        "Béton Blanc",          50,  5),
            ShopItem.of(Material.ORANGE_CONCRETE,       "Béton Orange",         50,  5),
            ShopItem.of(Material.MAGENTA_CONCRETE,      "Béton Magenta",        50,  5),
            ShopItem.of(Material.LIGHT_BLUE_CONCRETE,   "Béton Bleu Clair",     50,  5),
            ShopItem.of(Material.YELLOW_CONCRETE,       "Béton Jaune",          50,  5),
            ShopItem.of(Material.LIME_CONCRETE,         "Béton Vert Clair",     50,  5),
            ShopItem.of(Material.PINK_CONCRETE,         "Béton Rose",           50,  5),
            ShopItem.of(Material.GRAY_CONCRETE,         "Béton Gris",           50,  5),
            ShopItem.of(Material.LIGHT_GRAY_CONCRETE,   "Béton Gris Clair",     50,  5),
            ShopItem.of(Material.CYAN_CONCRETE,         "Béton Cyan",           50,  5),
            ShopItem.of(Material.PURPLE_CONCRETE,       "Béton Violet",         50,  5),
            ShopItem.of(Material.BLUE_CONCRETE,         "Béton Bleu",           50,  5),
            ShopItem.of(Material.BROWN_CONCRETE,        "Béton Marron",         50,  5),
            ShopItem.of(Material.GREEN_CONCRETE,        "Béton Vert",           50,  5),
            ShopItem.of(Material.RED_CONCRETE,          "Béton Rouge",          50,  5),
            ShopItem.of(Material.BLACK_CONCRETE,        "Béton Noir",           50,  5),
            // Verre coloré
            ShopItem.of(Material.WHITE_STAINED_GLASS,   "Verre Blanc",          50,  3),
            ShopItem.of(Material.ORANGE_STAINED_GLASS,  "Verre Orange",         50,  3),
            ShopItem.of(Material.YELLOW_STAINED_GLASS,  "Verre Jaune",          50,  3),
            ShopItem.of(Material.RED_STAINED_GLASS,     "Verre Rouge",          50,  3),
            ShopItem.of(Material.BLUE_STAINED_GLASS,    "Verre Bleu",           50,  3),
            ShopItem.of(Material.GREEN_STAINED_GLASS,   "Verre Vert",           50,  3),
            ShopItem.of(Material.CYAN_STAINED_GLASS,    "Verre Cyan",           50,  3),
            ShopItem.of(Material.PURPLE_STAINED_GLASS,  "Verre Violet",         50,  3),
            ShopItem.of(Material.BLACK_STAINED_GLASS,   "Verre Noir",           50,  3),
            // Terracotta colorée
            ShopItem.of(Material.WHITE_TERRACOTTA,      "Terracotta Blanche",   50,  5),
            ShopItem.of(Material.ORANGE_TERRACOTTA,     "Terracotta Orange",    50,  5),
            ShopItem.of(Material.YELLOW_TERRACOTTA,     "Terracotta Jaune",     50,  5),
            ShopItem.of(Material.RED_TERRACOTTA,        "Terracotta Rouge",     50,  5),
            ShopItem.of(Material.BROWN_TERRACOTTA,      "Terracotta Marron",    50,  5),
            ShopItem.of(Material.GREEN_TERRACOTTA,      "Terracotta Verte",     50,  5),
            ShopItem.of(Material.BLUE_TERRACOTTA,       "Terracotta Bleue",     50,  5),
            ShopItem.of(Material.BLACK_TERRACOTTA,      "Terracotta Noire",     50,  5)
    )),

    // ════════════════════════════════════════════════
    //  9. SPAWNERS
    // ════════════════════════════════════════════════
    SPAWNERS("§d🐾 Spawners", Material.SPAWNER, List.of(
            ShopItem.gemSell(Material.SPAWNER, "Spawner Cochon",         100,  2000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Vache",          100,  2000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Mouton",         100,  2000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Poulet",         100,  2000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Lapin",          100,  2000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Tortue",         150,  3000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Zombie",         200,  4000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Squelette",      200,  4000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Araignée",       200,  4000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Creeper",        250,  5000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Slime",          250,  5000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Sorcière",       300,  6000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Blaze",          400,  8000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Enderman",       500, 10000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Guardian",       600, 12000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Wither Skelet",  700, 14000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Golem de Fer",   800, 16000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Ghast",          600, 12000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Magma Cube",     350,  7000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Strider",        300,  6000),
            ShopItem.gemSell(Material.SPAWNER, "Spawner Hoglin",         400,  8000)
    ));

    private final String         displayName;
    private final Material       icon;
    private final List<ShopItem> items;

    ShopCategory(String displayName, Material icon, List<ShopItem> items) {
        this.displayName = displayName;
        this.icon        = icon;
        this.items       = items;
    }

    public String         getDisplayName() { return displayName; }
    public Material       getIcon()        { return icon; }
    public List<ShopItem> getItems()       { return items; }

    public static java.util.Map<Material, ShopItem> buildSellMap() {
        java.util.Map<Material, ShopItem> map = new java.util.HashMap<>();
        for (ShopCategory cat : values())
            for (ShopItem si : cat.getItems())
                if (si.isSellable()) map.put(si.material(), si);
        return map;
    }
}