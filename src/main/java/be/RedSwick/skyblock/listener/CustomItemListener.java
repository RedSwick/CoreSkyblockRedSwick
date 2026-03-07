package be.RedSwick.skyblock.listener;

import be.RedSwick.skyblock.SkyBlockPlugin;
import be.RedSwick.skyblock.customitem.*;
import be.RedSwick.skyblock.customitem.CustomItemConfig.*;
import be.RedSwick.skyblock.job.JobXpTable;
import be.RedSwick.skyblock.manager.PlayerDataManager;
import be.RedSwick.skyblock.player.PlayerData;
import be.RedSwick.skyblock.player.PlayerJob;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.Collections;

public class CustomItemListener implements Listener {

    private final PlayerDataManager pdm = SkyBlockPlugin.getInstance().getPlayerDataManager();
    private static final java.util.Random RANDOM = new java.util.Random();

    // Multitool swap : stocke l'item multitool original pendant le swap
    private static final Map<UUID, ItemStack> multiSwapOriginal = new HashMap<>();
    private static final Map<UUID, Material>  multiSwapCurrent  = new HashMap<>();
    private static final Map<UUID, Material>  multiLastTarget   = new HashMap<>();

    // Anti-farm : set des locations posées par des joueurs
    // On ne donne pas d'XP si le bloc a été posé par un joueur
    public static final Set<String> playerPlaced = Collections.synchronizedSet(new HashSet<>());

    // Blocs valides Hammer
    private static final Set<Material> HAMMER_BLOCKS = Set.of(
            Material.STONE, Material.COBBLESTONE, Material.GRANITE, Material.DIORITE,
            Material.ANDESITE, Material.DEEPSLATE, Material.COBBLED_DEEPSLATE,
            Material.IRON_ORE, Material.GOLD_ORE, Material.COAL_ORE, Material.COPPER_ORE,
            Material.DIAMOND_ORE, Material.EMERALD_ORE, Material.LAPIS_ORE, Material.REDSTONE_ORE,
            Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.DEEPSLATE_EMERALD_ORE, Material.DEEPSLATE_LAPIS_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE, Material.ANCIENT_DEBRIS
    );

    // Blocs hache 3x3
    private static final Set<Material> AXE_BLOCKS = Set.of(
            Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.JUNGLE_LOG,
            Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.OAK_WOOD, Material.BIRCH_WOOD, Material.SPRUCE_WOOD, Material.JUNGLE_WOOD,
            Material.ACACIA_WOOD, Material.DARK_OAK_WOOD, Material.STRIPPED_OAK_LOG,
            Material.STRIPPED_BIRCH_LOG, Material.STRIPPED_SPRUCE_LOG
    );

    // Cultures
    private static final Set<Material> CROPS = Set.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.NETHER_WART, Material.COCOA, Material.MELON, Material.PUMPKIN
    );

    private static final Map<Material, Material> REPLANT_SEEDS = Map.of(
            Material.WHEAT,       Material.WHEAT_SEEDS,
            Material.CARROTS,     Material.CARROT,
            Material.POTATOES,    Material.POTATO,
            Material.BEETROOTS,   Material.BEETROOT_SEEDS,
            Material.NETHER_WART, Material.NETHER_WART
    );

    // Fonte : RAW = résultat des mines vanilla + minerais directs
    private static final Map<Material, Material> SMELT_MAP = new HashMap<>();
    static {
        // Drops vanilla avec Fortune = raw form
        SMELT_MAP.put(Material.RAW_IRON,            Material.IRON_INGOT);
        SMELT_MAP.put(Material.RAW_GOLD,            Material.GOLD_INGOT);
        SMELT_MAP.put(Material.RAW_COPPER,          Material.COPPER_INGOT);
        // Minerais directs (sans silk touch)
        SMELT_MAP.put(Material.IRON_ORE,            Material.IRON_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_IRON_ORE,  Material.IRON_INGOT);
        SMELT_MAP.put(Material.GOLD_ORE,            Material.GOLD_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_GOLD_ORE,  Material.GOLD_INGOT);
        SMELT_MAP.put(Material.COPPER_ORE,          Material.COPPER_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_COPPER_ORE,Material.COPPER_INGOT);
        SMELT_MAP.put(Material.ANCIENT_DEBRIS,      Material.NETHERITE_SCRAP);
    }

    // ════════════════════════════════════════════════
    //  CLIC DROIT
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        CustomItemType type = CustomItemManager.getType(hand);
        if (type == null) return;

        Action action = event.getAction();

        // Shift + Clic droit → GUI config
        if (p.isSneaking() && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            if (type == CustomItemType.HAMMER) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                        () -> p.openInventory(CustomToolGUI.createHammer(p)));
                return;
            }
            if (type == CustomItemType.AXE_3X3) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                        () -> p.openInventory(CustomToolGUI.createAxe(p)));
                return;
            }
            if (type == CustomItemType.FARMERS_HOE_5X5) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                        () -> p.openInventory(CustomToolGUI.createHoe5x5(p)));
                return;
            }
            if (type == CustomItemType.MULTITOOL_ELITE) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                        () -> p.openInventory(CustomToolGUI.createMultitool(p)));
                return;
            }
            // Filet de pêche
            if (type == CustomItemType.FISHING_NET) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                        () -> p.openInventory(CustomToolGUI.createFishingNet(p)));
                return;
            }
            // Épées réparables
            if (type.isSword() && type.isRepairable()) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                        () -> p.openInventory(CustomToolGUI.createSword(p, type)));
                return;
            }
            // Fallback : tout item réparable sans GUI spécifique
            if (type.isRepairable()) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                        () -> p.openInventory(CustomToolGUI.createRepairOnly(p, type)));
                return;
            }
        }

        // La réparation se fait dans le GUI (Shift+Clic droit)

        // Bâton de vente → clic droit sur coffre
        if (type.isSellWand() && action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            Block b = event.getClickedBlock();
            if (b == null) return;
            if (b.getType() != Material.CHEST && b.getType() != Material.TRAPPED_CHEST) {
                p.sendMessage("§cClique sur un coffre !"); return;
            }
            int dur = CustomItemManager.getDurability(hand);
            if (dur <= 0) { p.sendMessage("§cBâton épuisé !"); return; }

            org.bukkit.block.Chest chest = (org.bukkit.block.Chest) b.getState();
            long earned = SellWandHelper.sellInventory(p, chest.getInventory(), type.getSellMultiplier());
            p.getInventory().setItemInMainHand(CustomItemManager.useDurability(hand, 1));

            if (earned > 0) sendSellActionBar(p, earned);
            else p.sendMessage("§7Aucun item vendable dans ce coffre.");
        }
    }

    // ════════════════════════════════════════════════
    //  CASSE DE BLOCS
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        CustomItemType type = CustomItemManager.getType(hand);

        // Si l'item en main n'est pas reconnu, vérifier si c'est un outil swappé par le multitool
        if (type == null) {
            UUID uuid = p.getUniqueId();
            ItemStack original = multiSwapOriginal.get(uuid);
            if (original != null) {
                CustomItemType origType = CustomItemManager.getType(original);
                if (origType != null && origType.isMultitool()) {
                    // On est en mode swap — traiter comme un multitool
                    handleMultitool(event, p, original, origType, event.getBlock());
                }
            }
            return;
        }

        Block center = event.getBlock();

        if (type == CustomItemType.HAMMER && HAMMER_BLOCKS.contains(center.getType())) {
            handleHammer(event, p, hand, type, center);
            return;
        }
        if (type == CustomItemType.AXE_3X3 && AXE_BLOCKS.contains(center.getType())) {
            handleAxe(event, p, hand, type, center);
            return;
        }
        if (type.isMultitool()) {
            handleMultitool(event, p, hand, type, center);
            return;
        }
        if (type.isHoe() && type != CustomItemType.PLANTER_HOE && CROPS.contains(center.getType())) {
            handleHoe(event, p, hand, type, center);
        }
    }

    // ════════════════════════════════════════════════
    //  PLANTEUR HOE
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlanterHoe(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = event.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        CustomItemType type = CustomItemManager.getType(hand);
        if (type != CustomItemType.PLANTER_HOE) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        event.setCancelled(true);
        int radius = 1;
        int count = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Block b = clicked.getRelative(dx, 0, dz);
                Block above = b.getRelative(BlockFace.UP);

                if (b.getType() == Material.DIRT || b.getType() == Material.GRASS_BLOCK) {
                    b.setType(Material.FARMLAND);
                    count++;
                }
                if (b.getType() == Material.FARMLAND && above.getType() == Material.AIR) {
                    for (Map.Entry<Material, Material> entry : REPLANT_SEEDS.entrySet()) {
                        if (hasSeed(p, entry.getValue())) {
                            removeSeed(p, entry.getValue());
                            above.setType(entry.getKey());
                            break;
                        }
                    }
                }
            }
        }
        applyDurability(p, hand, type, Math.max(1, count));
    }

    // ════════════════════════════════════════════════
    //  GUI CONFIG
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onConfigClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        String title = event.getView().getTitle();
        boolean isHammer = title.equals(CustomToolGUI.TITLE_HAMMER);
        boolean isAxe    = title.equals(CustomToolGUI.TITLE_AXE);
        boolean isMulti  = title.equals(CustomToolGUI.TITLE_MULTITOOL);
        boolean isHoe      = title.equals(CustomToolGUI.TITLE_HOE) || title.equals(CustomToolGUI.TITLE_HOE_5X5);
        boolean isFishNet  = title.equals(CustomToolGUI.TITLE_FISHING_NET);
        boolean isSword    = title.startsWith(CustomToolGUI.TITLE_SWORD_PREFIX);
        boolean isRepair   = title.startsWith(CustomToolGUI.TITLE_REPAIR_PREFIX);
        if (!isHammer && !isAxe && !isHoe && !isMulti && !isFishNet && !isSword && !isRepair) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        if (clicked.getType() == Material.BARRIER) { p.closeInventory(); return; }

        String actionId = CustomToolGUI.getActionId(clicked);
        if (actionId == null) return;

        if (isFishNet || isSword || isRepair) {
            ItemStack hand2 = p.getInventory().getItemInMainHand();
            CustomItemType t2 = CustomItemManager.getType(hand2);

            if ("repair".equals(actionId)) {
                if (t2 != null && t2.isRepairable()) tryRepair(p, hand2, t2);
            } else if ("sword_autosell".equals(actionId)) {
                be.RedSwick.skyblock.customitem.SwordConfig.ArcaniumConfig cfg =
                        be.RedSwick.skyblock.customitem.SwordConfig.get().getArcanium(p.getUniqueId());
                be.RedSwick.skyblock.customitem.SwordConfig.get().setArcanium(
                        p.getUniqueId(),
                        new be.RedSwick.skyblock.customitem.SwordConfig.ArcaniumConfig(!cfg.autoSell()));
            } else if (clicked.getType() == Material.BARRIER) {
                p.closeInventory();
                return;
            }
            // Re-ouvrir le bon GUI
            final CustomItemType finalT2 = t2;
            if (isFishNet)
                Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                        () -> p.openInventory(CustomToolGUI.createFishingNet(p)));
            else if (isSword && finalT2 != null && finalT2.isSword())
                Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                        () -> p.openInventory(CustomToolGUI.createSword(p, finalT2)));
            else if (isRepair && finalT2 != null)
                Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                        () -> p.openInventory(CustomToolGUI.createRepairOnly(p, finalT2)));
            return;
        }
        if (isMulti) {
            CustomItemConfig.MultitoolConfig cfg = CustomItemConfig.get().getMultitool(p.getUniqueId());
            CustomItemConfig.MultitoolConfig newCfg = switch (actionId) {
                case "mt_autosell" -> new CustomItemConfig.MultitoolConfig(!cfg.autoSell(), cfg.smelt(), cfg.silkTouch());
                case "mt_smelt"    -> new CustomItemConfig.MultitoolConfig(cfg.autoSell(), !cfg.smelt(), cfg.silkTouch() && cfg.smelt());
                case "mt_silk"     -> new CustomItemConfig.MultitoolConfig(cfg.autoSell(), cfg.smelt() && !cfg.silkTouch(), !cfg.silkTouch());
                default -> cfg;
            };
            CustomItemConfig.get().setMultitool(p.getUniqueId(), newCfg);
            if ("repair".equals(actionId)) {
                ItemStack hand2 = p.getInventory().getItemInMainHand();
                CustomItemType t2 = CustomItemManager.getType(hand2);
                if (t2 != null && t2.isRepairable()) tryRepair(p, hand2, t2);
            }
            Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(), () -> p.openInventory(CustomToolGUI.createMultitool(p)));
            return;
        }
        if (isAxe) {
            CustomItemConfig.AxeConfig cfg = CustomItemConfig.get().getAxe(p.getUniqueId());
            if ("axe_autosell".equals(actionId)) {
                CustomItemConfig.get().setAxe(p.getUniqueId(), new CustomItemConfig.AxeConfig(!cfg.autoSell()));
            }
            if ("repair".equals(actionId)) {
                ItemStack hand2 = p.getInventory().getItemInMainHand();
                CustomItemType t2 = CustomItemManager.getType(hand2);
                if (t2 != null && t2.isRepairable()) tryRepair(p, hand2, t2);
            }
            Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(), () -> p.openInventory(CustomToolGUI.createAxe(p)));
            return;
        }
        if (isHammer) {
            if ("repair".equals(actionId)) {
                ItemStack hand2 = p.getInventory().getItemInMainHand();
                CustomItemType t2 = CustomItemManager.getType(hand2);
                if (t2 != null && t2.isRepairable()) tryRepair(p, hand2, t2);
            }
            HammerConfig cfg = CustomItemConfig.get().getHammer(p.getUniqueId());
            HammerConfig newCfg = switch (actionId) {
                case "hammer_autosell"         -> new HammerConfig(!cfg.autoSell(), cfg.smelt(), cfg.autoSellSmelted());
                case "hammer_smelt"            -> new HammerConfig(cfg.autoSell(), !cfg.smelt(), cfg.autoSellSmelted());
                case "hammer_autosell_smelted" -> new HammerConfig(cfg.autoSell(), cfg.smelt(), !cfg.autoSellSmelted());
                default -> cfg;
            };
            CustomItemConfig.get().setHammer(p.getUniqueId(), newCfg);
            Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(), () -> p.openInventory(CustomToolGUI.createHammer(p)));
        } else {
            HoeConfig cfg = CustomItemConfig.get().getHoe(p.getUniqueId());
            HoeConfig newCfg = switch (actionId) {
                case "hoe_inventory" -> new HoeConfig(!cfg.toInventory(), cfg.autoSell(), cfg.radius());
                case "hoe_autosell"  -> new HoeConfig(cfg.toInventory(), !cfg.autoSell(), cfg.radius());
                case "hoe_radius"    -> new HoeConfig(cfg.toInventory(), cfg.autoSell(), (cfg.radius() + 1) % 3);
                default -> cfg;
            };
            CustomItemConfig.get().setHoe(p.getUniqueId(), newCfg);
            Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(), () -> p.openInventory(CustomToolGUI.createHoe5x5(p)));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onConfigDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.equals(CustomToolGUI.TITLE_HAMMER) || title.equals(CustomToolGUI.TITLE_AXE)
                || title.equals(CustomToolGUI.TITLE_HOE) || title.equals(CustomToolGUI.TITLE_MULTITOOL)
                || title.equals(CustomToolGUI.TITLE_HOE_5X5)
                || title.equals(CustomToolGUI.TITLE_FISHING_NET)
                || title.startsWith(CustomToolGUI.TITLE_SWORD_PREFIX)
                || title.startsWith(CustomToolGUI.TITLE_REPAIR_PREFIX))
            event.setCancelled(true);
    }

    // ════════════════════════════════════════════════
    //  LOGIQUE HAMMER
    // ════════════════════════════════════════════════

    private void handleHammer(BlockBreakEvent event, Player p, ItemStack hand,
                              CustomItemType type, Block center) {
        HammerConfig cfg = CustomItemConfig.get().getHammer(p.getUniqueId());
        event.setDropItems(false);

        BlockFace face = getTargetFace(p);
        int radius = 1;
        int broken = 0;
        long sellTotal = 0;

        // OPTIMISATION : accumuler XP/coins pour les 9 blocs du 3x3
        // puis rewardActionBatch() UNE SEULE FOIS — au lieu de 9 appels rewardAction()
        double batchXp    = 0;
        double batchCoins = 0;

        for (int a = -radius; a <= radius; a++) {
            for (int b2 = -radius; b2 <= radius; b2++) {
                Block b = getRelativeOnFace(center, face, a, b2);
                Material blockMat = b.getType();
                if (!HAMMER_BLOCKS.contains(blockMat)) continue;

                Collection<ItemStack> drops = b.getDrops(hand);
                b.setType(Material.AIR);
                broken++;

                // Accumuler XP miner — anti-farm via playerPlaced
                String locK = locKey(b.getLocation());
                if (!playerPlaced.remove(locK)) {
                    var minerAction = be.RedSwick.skyblock.job.JobXpTable.MINER_BLOCKS.get(blockMat);
                    if (minerAction != null) {
                        batchXp    += minerAction.xp();
                        batchCoins += minerAction.coins();
                    }
                }

                for (ItemStack drop : drops) {
                    ItemStack finalDrop = applySmelt(drop, cfg.smelt());

                    boolean sold = false;
                    if (cfg.autoSell() && !cfg.smelt()) {
                        long earned = SellWandHelper.sellStack(p, finalDrop, 1.0);
                        if (earned > 0) { sellTotal += earned; sold = true; }
                    } else if (cfg.smelt() && cfg.autoSellSmelted()) {
                        long earned = SellWandHelper.sellStack(p, finalDrop, 1.0);
                        if (earned > 0) { sellTotal += earned; sold = true; }
                    }

                    if (!sold) giveOrDrop(p, finalDrop);
                }
            }
        }

        // 1 seul appel pour les 9 blocs
        if (batchXp > 0 || batchCoins > 0)
            SkyBlockPlugin.getInstance().getJobManager()
                    .rewardActionBatch(p, be.RedSwick.skyblock.player.PlayerJob.MINER, batchXp, batchCoins);

        if (sellTotal > 0)
            sendSellActionBar(p, sellTotal);

        applyDurability(p, hand, type, broken);
    }

    // ════════════════════════════════════════════════
    //  LOGIQUE MULTITOOL
    // ════════════════════════════════════════════════

    // Blocs qui nécessitent une pelle
    private static final Set<Material> SHOVEL_BLOCKS = Set.of(
            Material.DIRT, Material.GRASS_BLOCK, Material.GRAVEL, Material.SAND,
            Material.RED_SAND, Material.COARSE_DIRT, Material.PODZOL, Material.MYCELIUM,
            Material.SOUL_SAND, Material.SOUL_SOIL, Material.CLAY, Material.SNOW,
            Material.SNOW_BLOCK, Material.POWDER_SNOW, Material.MUD
    );
    // Blocs qui nécessitent une hache
    private static final Set<Material> AXE_BREAK_BLOCKS = Set.of(
            Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.JUNGLE_LOG,
            Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.OAK_WOOD, Material.BIRCH_WOOD, Material.SPRUCE_WOOD, Material.JUNGLE_WOOD,
            Material.ACACIA_WOOD, Material.DARK_OAK_WOOD, Material.OAK_PLANKS, Material.BIRCH_PLANKS,
            Material.SPRUCE_PLANKS, Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS,
            Material.DARK_OAK_PLANKS, Material.BAMBOO_BLOCK, Material.CRAFTING_TABLE,
            Material.CHEST, Material.BARREL, Material.BOOKSHELF
    );

    private void handleMultitool(BlockBreakEvent event, Player p, ItemStack hand,
                                 CustomItemType type, Block center) {
        CustomItemConfig.MultitoolConfig cfg = type == CustomItemType.MULTITOOL_ELITE
                ? CustomItemConfig.get().getMultitool(p.getUniqueId())
                : new CustomItemConfig.MultitoolConfig(false, false, false);

        Material blockMat = center.getType();

        // Cancel les drops vanilla, on les recalcule avec le bon outil
        event.setDropItems(false);

        // "hand" est toujours le multitool original (passé depuis onBlockBreak)
        ItemStack effectiveTool = getEffectiveTool(blockMat, hand);
        Collection<ItemStack> drops;

        if (cfg.silkTouch()) {
            drops = List.of(new ItemStack(blockMat));
        } else {
            drops = center.getDrops(effectiveTool);
        }

        long sellTotal = 0;
        for (ItemStack drop : drops) {
            ItemStack finalDrop = (!cfg.silkTouch() && cfg.smelt()) ? applySmelt(drop, true) : drop;
            if (cfg.autoSell()) {
                long earned = SellWandHelper.sellStack(p, finalDrop, 1.0);
                if (earned > 0) { sellTotal += earned; continue; }
            }
            giveOrDrop(p, finalDrop);
        }
        if (sellTotal > 0) sendSellActionBar(p, sellTotal);

        // XP miner uniquement sur les blocs de type pioche (pas pelle/hache)
        if (!SHOVEL_BLOCKS.contains(blockMat) && !AXE_BREAK_BLOCKS.contains(blockMat)) {
            // Anti-farm : pas d'XP si le joueur a posé ce bloc lui-même
            String locKey = locKey(center.getLocation());
            if (!playerPlaced.remove(locKey)) {
                triggerMinerJob(p, blockMat, center.getLocation());
            }
        }

        UUID uuid = p.getUniqueId();
        multiLastTarget.remove(uuid);
        ItemStack multitoolItem = hand;

        // Durabilité sur le multitool original
        int unbreaking = multitoolItem.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.UNBREAKING);
        boolean consume = (unbreaking == 0 || RANDOM.nextInt(unbreaking + 1) == 0);

        ItemStack finalMultitool = multitoolItem;

        if (!consume) {
            multiSwapOriginal.put(uuid, finalMultitool);
            multiSwapCurrent.remove(uuid); // forcer re-swap au prochain move
            Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                    () -> performSwap(p, uuid, finalMultitool));
            return;
        }

        int dur = CustomItemManager.getDurability(multitoolItem);
        if (dur <= 0) {
            multiSwapOriginal.put(uuid, finalMultitool);
            multiSwapCurrent.remove(uuid);
            Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                    () -> performSwap(p, uuid, finalMultitool));
            return;
        }
        if (dur == 1 && !type.isRepairable()) {
            multiSwapOriginal.remove(uuid);
            multiSwapCurrent.remove(uuid);
            p.getInventory().setItemInMainHand(null);
            p.sendMessage("§cTon §f" + type.getDisplayName() + " §cs'est cassé !");
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
        } else {
            ItemStack updated = CustomItemManager.useDurability(multitoolItem, 1);
            if (updated != null) {
                multiSwapOriginal.put(uuid, updated);
                multiSwapCurrent.remove(uuid); // forcer re-swap immédiat
                Bukkit.getScheduler().runTask(SkyBlockPlugin.getInstance(),
                        () -> performSwap(p, uuid, updated));
                int newDur = CustomItemManager.getDurability(updated);
                int max = type.getMaxDurability();
                if (newDur > 0 && newDur <= max * 0.1)
                    p.sendMessage("§c⚠ §f" + newDur + " / " + max);
            }
        }
    }

    // ════════════════════════════════════════════════
    //  MULTITOOL SWAP — PlayerMoveEvent (rotation tête uniquement)
    //  Optimisé : swap seulement si le type de bloc visé change
    // ════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        org.bukkit.Location from = event.getFrom();
        org.bukkit.Location to   = event.getTo();
        if (to == null) return;
        // Ignorer si la tête n'a pas bougé
        if (from.getPitch() == to.getPitch() && from.getYaw() == to.getYaw()) return;

        Player p = event.getPlayer();
        UUID uuid = p.getUniqueId();

        // Cas 1 : joueur a un multitool en main
        ItemStack hand = p.getInventory().getItemInMainHand();
        CustomItemType type = CustomItemManager.getType(hand);
        if (type != null && type.isMultitool()) {
            multiSwapOriginal.put(uuid, hand.clone()); // toujours mettre à jour l'original
            performSwap(p, uuid, hand);
            return;
        }

        // Cas 2 : joueur a un outil swappé en main (original sauvegardé)
        if (multiSwapOriginal.containsKey(uuid)) {
            // On est en mode swap — vérifier le bloc visé et re-swapper si besoin
            ItemStack original = multiSwapOriginal.get(uuid);
            performSwap(p, uuid, original);
        }
        // Cas 3 : ni multitool ni swappé → rien à faire
    }

    private void performSwap(Player p, UUID uuid, ItemStack multitoolItem) {
        @SuppressWarnings("deprecation")
        Block target = p.getTargetBlock(null, 5);
        Material blockMat = (target == null || target.getType().isAir())
                ? null : target.getType();

        // Déterminer l'outil nécessaire
        // null = vise le vide → garder le multitool en main tel quel (pioche de base)
        if (blockMat == null) {
            // Remettre le multitool original si on avait swappé
            Material cur = multiSwapCurrent.get(uuid);
            if (cur != null) {
                multiSwapCurrent.remove(uuid);
                p.getInventory().setItemInMainHand(multiSwapOriginal.get(uuid).clone());
            }
            return;
        }

        Material needed = getNeededToolMaterial(blockMat);

        // Si c'est une pioche → garder le multitool original (il EST une pioche)
        if (needed == Material.DIAMOND_PICKAXE) {
            Material cur = multiSwapCurrent.get(uuid);
            if (cur != null && cur != Material.DIAMOND_PICKAXE) {
                // On était sur pelle/hache → remettre le multitool
                multiSwapCurrent.remove(uuid);
                p.getInventory().setItemInMainHand(multiSwapOriginal.get(uuid).clone());
            }
            return;
        }

        // Pelle ou hache nécessaire
        if (needed == multiSwapCurrent.get(uuid)) return; // déjà le bon outil

        multiSwapCurrent.put(uuid, needed);
        p.getInventory().setItemInMainHand(getEffectiveTool(blockMat, multitoolItem));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeldItemChange(org.bukkit.event.player.PlayerItemHeldEvent event) {
        restoreMultitool(event.getPlayer());
    }

    public void restoreMultitool(Player p) {
        UUID uuid = p.getUniqueId();
        ItemStack original = multiSwapOriginal.remove(uuid);
        multiSwapCurrent.remove(uuid);
        multiLastTarget.remove(uuid);
        if (original != null) {
            // Remettre seulement si l'item actuel n'est pas déjà le multitool
            CustomItemType cur = CustomItemManager.getType(p.getInventory().getItemInMainHand());
            if (cur == null) {
                p.getInventory().setItemInMainHand(original);
            }
        }
    }

    private Material getNeededToolMaterial(Material blockMat) {
        if (SHOVEL_BLOCKS.contains(blockMat)) return Material.DIAMOND_SHOVEL;
        if (AXE_BREAK_BLOCKS.contains(blockMat)) return Material.DIAMOND_AXE;
        return Material.DIAMOND_PICKAXE; // pioche = pas de swap, garder le multitool
    }

    /** Retourne un ItemStack avec le bon matériau pour les drops selon le bloc.
     *  Conserve le nom d'affichage et le lore du multitool original. */
    private ItemStack getEffectiveTool(Material blockMat, ItemStack originalHand) {
        Material toolMat;
        if (SHOVEL_BLOCKS.contains(blockMat)) {
            toolMat = Material.DIAMOND_SHOVEL;
        } else if (AXE_BREAK_BLOCKS.contains(blockMat)) {
            toolMat = Material.DIAMOND_AXE;
        } else {
            toolMat = Material.DIAMOND_PICKAXE;
        }

        ItemStack tool = new ItemStack(toolMat);
        org.bukkit.inventory.meta.ItemMeta newMeta = tool.getItemMeta();
        org.bukkit.inventory.meta.ItemMeta origMeta = originalHand.getItemMeta();

        // Copier nom, lore et enchants du multitool
        if (origMeta != null && newMeta != null) {
            if (origMeta.hasDisplayName())
                newMeta.setDisplayName(origMeta.getDisplayName());
            if (origMeta.hasLore())
                newMeta.setLore(origMeta.getLore());
            newMeta.setUnbreakable(true); // dura gérée par notre système
            originalHand.getEnchantments().forEach((ench, lvl) -> newMeta.addEnchant(ench, lvl, true));
            tool.setItemMeta(newMeta);
        }
        return tool;
    }

    // ════════════════════════════════════════════════
    //  LOGIQUE HOE
    // ════════════════════════════════════════════════

    private void handleHoe(BlockBreakEvent event, Player p, ItemStack hand,
                           CustomItemType type, Block center) {
        // Config perso seulement pour la 5x5 (auto-sell)
        HoeConfig cfg = type == CustomItemType.FARMERS_HOE_5X5
                ? CustomItemConfig.get().getHoe(p.getUniqueId())
                : new HoeConfig(true, false, type.getRadius());

        event.setCancelled(true);
        int radius = type.getRadius();
        int broken = 0;
        long sellTotal = 0;

        // OPTIMISATION CRITIQUE : accumuler XP/coins pour toute la passe 5x5
        // puis appeler rewardActionBatch() UNE SEULE FOIS à la fin.
        // Avant : triggerFarmerJob() par drop → 50-75 appels rewardAction() par clic
        // Après : 1 seul checkLevelUp + 1 seul sendActionBar pour toute la passe.
        double batchXp    = 0;
        double batchCoins = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Block b = center.getRelative(dx, 0, dz);
                if (!CROPS.contains(b.getType())) continue;
                if (b.getBlockData() instanceof Ageable ageable
                        && ageable.getAge() < ageable.getMaximumAge()) continue;

                Material cropType = b.getType();
                Collection<ItemStack> drops = b.getDrops(hand);
                Collection<ItemStack> dropsNoTool = b.getDrops();
                broken++;

                Material seed = REPLANT_SEEDS.get(cropType);
                if (seed != null) {
                    if (b.getBlockData() instanceof Ageable ag) {
                        ag.setAge(0);
                        b.setBlockData(ag);
                    }
                } else {
                    b.setType(Material.AIR);
                }

                int fortuneLevel = hand.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.FORTUNE);

                List<ItemStack> finalDrops = new ArrayList<>();
                for (ItemStack d : dropsNoTool) {
                    if (seed != null && d.getType() == seed) finalDrops.add(d.clone());
                }
                for (ItemStack d : drops) {
                    if (seed == null || d.getType() != seed)
                        finalDrops.add(applyFortune(d, cropType, fortuneLevel));
                }

                // Accumuler l'XP du type de bloc (pas du drop) — clé correcte dans FARMER_CROPS
                var cropAction = be.RedSwick.skyblock.job.JobXpTable.FARMER_CROPS.get(cropType);
                if (cropAction != null) {
                    batchXp    += cropAction.xp();
                    batchCoins += cropAction.coins();
                }

                boolean seedRemoved = false;
                for (ItemStack drop : finalDrops) {
                    if (!seedRemoved && seed != null && drop.getType() == seed) {
                        drop.setAmount(Math.max(0, drop.getAmount() - 1));
                        seedRemoved = true;
                    }
                    if (drop.getAmount() <= 0) continue;

                    if (cfg.autoSell()) {
                        long earned = SellWandHelper.sellStack(p, drop, 1.0);
                        if (earned > 0) { sellTotal += earned; continue; }
                    }
                    if (cfg.toInventory()) giveOrDrop(p, drop);
                    else p.getWorld().dropItemNaturally(b.getLocation(), drop);
                }
            }
        }

        // 1 seul appel rewardActionBatch pour toute la passe — au lieu de 50-75
        if (batchXp > 0 || batchCoins > 0)
            SkyBlockPlugin.getInstance().getJobManager()
                    .rewardActionBatch(p, be.RedSwick.skyblock.player.PlayerJob.FARMER, batchXp, batchCoins);

        if (sellTotal > 0)
            sendSellActionBar(p, sellTotal);

        // Stat cultures récoltées
        if (broken > 0) {
            var pData = SkyBlockPlugin.getInstance().getPlayerDataManager().get(p.getUniqueId());
            if (pData != null) pData.addCropsBroken(broken);
        }

        applyDurability(p, hand, type, broken);
    }

    private void handleAxe(BlockBreakEvent event, Player p, ItemStack hand,
                           CustomItemType type, Block center) {
        CustomItemConfig.AxeConfig cfg = CustomItemConfig.get().getAxe(p.getUniqueId());
        event.setDropItems(false);
        BlockFace face = getTargetFace(p);
        int radius = 1;
        int broken = 0;
        long sellTotal = 0;

        for (int a = -radius; a <= radius; a++) {
            for (int b2 = -radius; b2 <= radius; b2++) {
                Block b = getRelativeOnFace(center, face, a, b2);
                Material blockMat = b.getType();
                if (!AXE_BLOCKS.contains(blockMat)) continue;
                Collection<ItemStack> drops = b.getDrops(hand);
                b.setType(Material.AIR);
                broken++;
                // XP bucheron
                triggerBucheronJob(p, blockMat, b.getLocation());
                for (ItemStack drop : drops) {
                    if (cfg.autoSell()) {
                        long earned = SellWandHelper.sellStack(p, drop, 1.0);
                        if (earned > 0) { sellTotal += earned; continue; }
                    }
                    giveOrDrop(p, drop);
                }
            }
        }
        if (sellTotal > 0) sendSellActionBar(p, sellTotal);
        applyDurability(p, hand, type, broken);
    }

    private void handleArea(BlockBreakEvent event, Player p, ItemStack hand,
                            CustomItemType type, Block center, Set<Material> validBlocks) {
        event.setDropItems(false);
        int radius = type.getRadius();
        int broken = 0;
        BlockFace face = getTargetFace(p);

        for (int a = -radius; a <= radius; a++) {
            for (int b2 = -radius; b2 <= radius; b2++) {
                Block b = getRelativeOnFace(center, face, a, b2);
                if (!validBlocks.contains(b.getType())) continue;
                Collection<ItemStack> drops = b.getDrops(hand);
                b.setType(Material.AIR);
                broken++;
                for (ItemStack drop : drops) giveOrDrop(p, drop);
            }
        }
        applyDurability(p, hand, type, broken);
    }

    // ════════════════════════════════════════════════
    //  RÉPARATION
    // ════════════════════════════════════════════════

    private void tryRepair(Player p, ItemStack hand, CustomItemType type) {
        int dur = CustomItemManager.getDurability(hand);
        if (dur >= type.getMaxDurability()) { p.sendMessage("§7Déjà en pleine durabilité."); return; }

        PlayerData data = pdm.get(p.getUniqueId());
        if (data == null) return;

        if (p.getLevel() < type.getRepairLevels()) {
            p.sendMessage("§cIl te faut §e" + type.getRepairLevels() + " niveaux §c(tu as " + p.getLevel() + ")");
            return;
        }
        if (data.getCoins() < type.getRepairCoins()) {
            p.sendMessage("§cIl te faut §6" + String.format("%,d", type.getRepairCoins())
                    + " coins §c(tu as " + String.format("%,d", data.getCoins()) + ")");
            return;
        }

        p.setLevel(p.getLevel() - type.getRepairLevels());
        data.addCoins(-type.getRepairCoins());
        pdm.savePlayer(p.getUniqueId());

        p.getInventory().setItemInMainHand(CustomItemManager.repair(hand));
        p.sendMessage("§a✔ Réparé ! §8(-§e" + type.getRepairLevels() + " niveaux §8-§6"
                + String.format("%,d", type.getRepairCoins()) + " coins§8)");
        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
    }

    // ════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════

    /**
     * Applique la fortune sur un drop de culture.
     * Vanilla : blé/betterave = 1 récolte de base + fortune bonus
     * Carotte/Patate = 1-4 de base, fortune augmente le max
     */
    private ItemStack applyFortune(ItemStack drop, Material cropType, int fortuneLevel) {
        if (fortuneLevel == 0) return drop.clone();
        int base = drop.getAmount();
        // Multiplicateur fortune : entre 1 et (fortuneLevel + 1)
        // Fortune 5 → multiplicateur entre 1 et 6
        int multiplier = 1 + RANDOM.nextInt(fortuneLevel + 1);
        int result = Math.min(base * multiplier, 64);
        ItemStack out = drop.clone();
        out.setAmount(result);
        return out;
    }

    /**
     * Applique la fonte automatique sur un drop.
     * Crée un nouvel ItemStack avec le matériau fondu.
     */
    private ItemStack applySmelt(ItemStack drop, boolean smeltEnabled) {
        if (!smeltEnabled) return drop;
        Material smelted = SMELT_MAP.get(drop.getType());
        if (smelted == null) return drop;
        return new ItemStack(smelted, drop.getAmount());
    }

    private void applyDurability(Player p, ItemStack hand, CustomItemType type, int usage) {
        if (usage == 0) return;

        // Toujours lire depuis l'inventaire — la référence "hand" peut être obsolète
        ItemStack current = p.getInventory().getItemInMainHand();
        if (current != null && CustomItemManager.getType(current) == type) {
            hand = current;
        }

        int dur = CustomItemManager.getDurability(hand);
        if (dur <= 0) {
            if (type.isRepairable()) p.sendMessage("§cÉpuisé ! Répare ton §f" + type.getDisplayName());
            return;
        }

        // Unbreaking : chaque utilisation a une chance de ne pas user
        int unbreakingLevel = hand.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.UNBREAKING);
        int realUsage = 0;
        for (int i = 0; i < usage; i++) {
            if (unbreakingLevel == 0 || RANDOM.nextInt(unbreakingLevel + 1) == 0) {
                realUsage++;
            }
        }
        if (realUsage == 0) return;

        ItemStack updated = CustomItemManager.useDurability(hand, realUsage);
        if (updated == null) {
            if (!type.isRepairable()) {
                p.getInventory().setItemInMainHand(null);
                p.sendMessage("§cTon §f" + type.getDisplayName() + " §cs'est cassé !");
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            } else {
                // Réparable → reste à 0, bloqué
                p.getInventory().setItemInMainHand(CustomItemManager.useDurability(hand, dur)); // force à 0
                p.sendMessage("§cTon §f" + type.getDisplayName() + " §cest épuisé ! Répare-le.");
            }
        } else {
            p.getInventory().setItemInMainHand(updated);
            int newDur = CustomItemManager.getDurability(updated);
            int max = type.getMaxDurability();
            if (newDur > 0 && newDur <= max * 0.1)
                p.sendMessage("§c⚠ Durabilité faible : §f" + newDur + " / " + max);
        }
    }

    private void giveOrDrop(Player p, ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) return;
        p.getInventory().addItem(item).values().forEach(
                i -> p.getWorld().dropItemNaturally(p.getLocation(), i));
    }

    private void triggerFarmerJob(Player p, ItemStack drop) {
        var action = JobXpTable.FARMER_CROPS.get(drop.getType());
        if (action != null)
            SkyBlockPlugin.getInstance().getJobManager()
                    .rewardAction(p, PlayerJob.FARMER, action.xp() * drop.getAmount(), action.coins() * drop.getAmount());
    }

    private void triggerMinerJob(Player p, Material blockType, org.bukkit.Location loc) {
        if (playerPlaced.remove(locKey(loc))) return; // bloc posé par un joueur → pas d'XP
        var action = JobXpTable.MINER_BLOCKS.get(blockType);
        if (action != null)
            SkyBlockPlugin.getInstance().getJobManager()
                    .rewardAction(p, PlayerJob.MINER, action.xp(), action.coins());
    }

    private void triggerBucheronJob(Player p, Material blockType, org.bukkit.Location loc) {
        if (playerPlaced.remove(locKey(loc))) return; // bloc posé par un joueur → pas d'XP
        var action = JobXpTable.BUCHERON_LOGS.get(blockType);
        if (action != null)
            SkyBlockPlugin.getInstance().getJobManager()
                    .rewardAction(p, PlayerJob.BUCHERON, action.xp(), action.coins());
    }

    public static String locKey(org.bukkit.Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private boolean hasSeed(Player p, Material seed) { return p.getInventory().contains(seed); }
    private void removeSeed(Player p, Material seed) { p.getInventory().removeItem(new ItemStack(seed, 1)); }

    // ════════════════════════════════════════════════
    //  ACTIONBAR VENTE
    // ════════════════════════════════════════════════

    private void sendSellActionBar(Player p, long earned) {
        // Affiche via JobManager.displaySellCoins pour s'intégrer à l'ActionBar des métiers
        SkyBlockPlugin.getInstance().getJobManager().displaySellCoins(p, earned);
    }

    // ════════════════════════════════════════════════
    //  DIRECTION
    // ════════════════════════════════════════════════

    private BlockFace getTargetFace(Player p) {
        float pitch = p.getEyeLocation().getPitch();
        if (pitch > 45)  return BlockFace.DOWN;
        if (pitch < -45) return BlockFace.UP;
        float yaw = ((p.getEyeLocation().getYaw() % 360) + 360) % 360;
        if (yaw < 45 || yaw >= 315) return BlockFace.NORTH;
        if (yaw < 135)              return BlockFace.EAST;
        if (yaw < 225)              return BlockFace.SOUTH;
        return BlockFace.WEST;
    }

    private Block getRelativeOnFace(Block center, BlockFace face, int a, int b) {
        return switch (face) {
            case NORTH, SOUTH -> center.getRelative(a, b, 0);
            case EAST,  WEST  -> center.getRelative(0, b, a);
            case UP,    DOWN  -> center.getRelative(a, 0, b);
            default           -> center.getRelative(a, b, 0);
        };
    }
}