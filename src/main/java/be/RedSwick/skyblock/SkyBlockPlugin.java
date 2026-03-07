package be.RedSwick.skyblock;

import be.RedSwick.skyblock.command.*;
import be.RedSwick.skyblock.hologram.HologramManager;
import be.RedSwick.skyblock.job.JobManager;
import be.RedSwick.skyblock.listener.*;
import be.RedSwick.skyblock.manager.*;
import be.RedSwick.skyblock.world.VoidChunkGenerator;
import org.bukkit.Bukkit;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class SkyBlockPlugin extends JavaPlugin {

    private static SkyBlockPlugin instance;

    private IslandManager         islandManager;
    private PlayerDataManager     playerDataManager;
    private JobManager            jobManager;
    private WarpManager           warpManager;
    private HologramManager       hologramManager;
    private be.RedSwick.skyblock.leaderboard.LeaderboardManager leaderboardManager;
    private SpawnerManager        spawnerManager;
    private BossBarManager        bossBarManager;
    private IslandTeamChatManager teamChatManager;   // ← NOUVEAU

    @Override
    public void onEnable() {
        instance = this;

        createSkyblockWorld();

        // ── Managers ──
        islandManager      = new IslandManager();
        playerDataManager  = new PlayerDataManager();
        jobManager         = new JobManager();
        warpManager        = new WarpManager();
        hologramManager    = new HologramManager();
        leaderboardManager = new be.RedSwick.skyblock.leaderboard.LeaderboardManager(this);
        spawnerManager     = new SpawnerManager();
        bossBarManager     = new BossBarManager();
        teamChatManager    = new IslandTeamChatManager();  // ← NOUVEAU

        // ── Init avant listeners ──
        be.RedSwick.skyblock.customitem.CustomItemConfig.get().init();
        be.RedSwick.skyblock.moderation.StaffPermissionManager.get().init();
        be.RedSwick.skyblock.moderation.WarnManager.get().init();
        be.RedSwick.skyblock.moderation.StaffLogManager.get().init();

        // ── Commandes île ──
        IslandCommand islandCommand = new IslandCommand();
        getCommand("is").setExecutor(islandCommand);
        getCommand("is").setTabCompleter(islandCommand);

        // ── Commandes globales ──
        getCommand("spawn").setExecutor(new SpawnCommand());
        getCommand("rang").setExecutor(new RankCommand());
        getCommand("grade").setExecutor(new GradeCommand());
        getCommand("job").setExecutor(new JobCommand());
        getCommand("warp").setExecutor(new WarpCommand());

        getCommand("bal").setExecutor(new EconomyCommand.BalCommand());
        getCommand("pay").setExecutor(new EconomyCommand.PayCommand());
        getCommand("essence").setExecutor(new EconomyCommand.EssenceCommand());
        getCommand("baltop").setExecutor(new EconomyCommand.BaltopCommand());
        getCommand("setbal").setExecutor(new EconomyCommand.SetBalCommand());
        getCommand("setgems").setExecutor(new EconomyCommand.SetGemsCommand());
        getCommand("shop").setExecutor(new ShopCommand());
        getCommand("arc").setExecutor(new ArcCommand());

        be.RedSwick.skyblock.command.HoloCommand holoCmd = new be.RedSwick.skyblock.command.HoloCommand();
        getCommand("holo").setExecutor(holoCmd);
        getCommand("holo").setTabCompleter(holoCmd);

        be.RedSwick.skyblock.command.LeaderboardCommand lbCmd = new be.RedSwick.skyblock.command.LeaderboardCommand();
        getCommand("classement").setExecutor(lbCmd);
        getCommand("classement").setTabCompleter(lbCmd);

        SetStaffGradeCommand ssgCmd = new SetStaffGradeCommand();
        getCommand("setstaffgrade").setExecutor(ssgCmd);
        getCommand("setstaffgrade").setTabCompleter(ssgCmd);

        getCommand("mute").setExecutor(new ModerationCommand.Mute());
        getCommand("unmute").setExecutor(new ModerationCommand.Unmute());
        getCommand("kick").setExecutor(new ModerationCommand.Kick());
        getCommand("ban").setExecutor(new ModerationCommand.Ban());
        getCommand("unban").setExecutor(new ModerationCommand.Unban());
        getCommand("tempban").setExecutor(new ModerationCommand.TempBan());
        getCommand("warn").setExecutor(new ModerationCommand.Warn());
        getCommand("warns").setExecutor(new ModerationCommand.Warns());
        getCommand("vanish").setExecutor(new ModerationCommand.Vanish());
        getCommand("tp").setExecutor(new ModerationCommand.Tp());
        getCommand("tphere").setExecutor(new ModerationCommand.TpHere());
        getCommand("tpisland").setExecutor(new ModerationCommand.TpIsland());
        getCommand("serverclose").setExecutor(new ModerationCommand.ServerClose());
        getCommand("serveropen").setExecutor(new ModerationCommand.ServerOpen());
        getCommand("stafflogs").setExecutor(new StaffLogsCommand());

        StaffTabCompleter staffTab = new StaffTabCompleter();
        for (String cmd : List.of("mute","unmute","kick","ban","unban","tempban","warn","warns",
                "tp","tphere","tpisland","vanish","serverclose","serveropen")) {
            if (getCommand(cmd) != null) getCommand(cmd).setTabCompleter(staffTab);
        }

        getCommand("arc").setTabCompleter((sender, command, alias, args) -> {
            if (args.length == 1) return List.of("give","setrank","setgrade","island","job","item","reload");
            if (args.length == 2 && args[0].equalsIgnoreCase("give")) return List.of("coins","gems","essence");
            if (args.length == 2 && (args[0].equalsIgnoreCase("setrank") || args[0].equalsIgnoreCase("setgrade")
                    || args[0].equalsIgnoreCase("island") || args[0].equalsIgnoreCase("job")))
                return StaffTabCompleter.playerNamesStatic(args[1]);
            if (args.length == 3 && args[0].equalsIgnoreCase("island")) return List.of("info","delete","tp");
            return List.of();
        });

        // ── Listeners ──
        getServer().getPluginManager().registerEvents(new StaffPermissionListener(),   this);
        getServer().getPluginManager().registerEvents(new ChatAndTabListener(),         this);
        getServer().getPluginManager().registerEvents(new JoinListener(),               this);
        getServer().getPluginManager().registerEvents(new PlayerDataListener(),         this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(),         this);
        getServer().getPluginManager().registerEvents(new PermissionsListener(),        this);
        getServer().getPluginManager().registerEvents(new MenuListener(),               this);
        getServer().getPluginManager().registerEvents(new GuiProtectionListener(),      this);
        getServer().getPluginManager().registerEvents(new JobListener(),                this);
        getServer().getPluginManager().registerEvents(new ValueBlockListener(),         this);
        getServer().getPluginManager().registerEvents(new MissionListener(),            this);
        getServer().getPluginManager().registerEvents(new IslandFlyListener(),          this); // ← NOUVEAU
        getServer().getPluginManager().registerEvents(new IslandGUIListener(),          this); // ← NOUVEAU
        getServer().getPluginManager().registerEvents(new ItemStackListener(),          this); // ← NOUVEAU

        SpawnerListener spawnerListener = new SpawnerListener();
        getServer().getPluginManager().registerEvents(spawnerListener,                  this);
        getServer().getPluginManager().registerEvents(new MobStackListener(),           this);
        getServer().getPluginManager().registerEvents(new CustomItemListener(),         this);
        getServer().getPluginManager().registerEvents(new be.RedSwick.skyblock.listener.NewItemsListener(),      this);
        getServer().getPluginManager().registerEvents(new be.RedSwick.skyblock.listener.LeaderboardGuiListener(), this);
        getServer().getPluginManager().registerEvents(new be.RedSwick.skyblock.listener.DeathRespawnListener(),   this);
        getServer().getPluginManager().registerEvents(new be.RedSwick.skyblock.listener.NewItemsGuiListener(),    this);
        getServer().getPluginManager().registerEvents(new ShopListener(),               this);
        getServer().getPluginManager().registerEvents(new MuteListener(),               this);
        getServer().getPluginManager().registerEvents(new AdminItemListener(),          this);
        getServer().getPluginManager().registerEvents(new StaffLogsListener(),          this);
        getServer().getPluginManager().registerEvents(new ServerCloseListener(),        this);

        // ── Tâches planifiées ──
        be.RedSwick.skyblock.listener.PlaytimeListener.startTicker();

        Bukkit.getScheduler().runTaskTimer(this, () -> leaderboardManager.refreshAll(), 6000L, 6000L);

        Bukkit.getScheduler().runTaskLater(this, spawnerListener::startAllCycles, 40L);

        Bukkit.getScheduler().runTaskTimer(this, () -> warpManager.cleanExpired(), 1200L, 1200L);

        Bukkit.getScheduler().runTaskTimer(this, () -> getJobManager().tickResetDisplay(), 40L, 40L);

        Bukkit.getScheduler().runTaskTimer(this, () -> hologramManager.tickMobHolograms(), 40L, 40L);

        Bukkit.getScheduler().runTaskLater(this, () -> hologramManager.cleanupOrphanHolograms(), 60L);

        // Gravité mobs stackés
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (org.bukkit.World w : getServer().getWorlds()) {
                for (org.bukkit.entity.Entity e : w.getEntities()) {
                    if (e instanceof org.bukkit.entity.Mob mob
                            && !mob.getMetadata("stack_count").isEmpty()
                            && !mob.hasGravity()) {
                        mob.setGravity(true);
                    }
                }
            }
        }, 4L, 4L);

        // Auto-save PlayerData toutes les 5 minutes
        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> playerDataManager.saveAll(), 6000L, 6000L);

        // Auto-save IslandData toutes les 5 minutes
        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> islandManager.getAllIslands().forEach(islandManager::saveIsland), 6000L, 6000L);

        be.RedSwick.skyblock.listener.MerchantPouchTickListener.start();

        be.RedSwick.skyblock.customitem.SwordConfig.get().init();
        be.RedSwick.skyblock.customitem.LootBagData.get().init();
        be.RedSwick.skyblock.customitem.MerchantPouchData.get().init();
        be.RedSwick.skyblock.customitem.ChunkHopperManager.get().init();

        getLogger().info("[Arcanium] SkyBlock activé !");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) playerDataManager.saveAll();
        if (hologramManager   != null) hologramManager.removeAll();
        getLogger().info("[Arcanium] SkyBlock désactivé.");
    }

    private void createSkyblockWorld() {
        if (Bukkit.getWorld("skyblock") != null) return;
        WorldCreator creator = new WorldCreator("skyblock");
        creator.generator(new VoidChunkGenerator());
        Bukkit.createWorld(creator);
    }

    public static SkyBlockPlugin getInstance()      { return instance; }
    public IslandManager    getIslandManager()      { return islandManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public JobManager       getJobManager()         { return jobManager; }
    public WarpManager      getWarpManager()        { return warpManager; }
    public HologramManager  getHologramManager()    { return hologramManager; }
    public be.RedSwick.skyblock.leaderboard.LeaderboardManager getLeaderboardManager() { return leaderboardManager; }
    public SpawnerManager   getSpawnerManager()     { return spawnerManager; }
    public BossBarManager   getBossBarManager()      { return bossBarManager; }
    public IslandTeamChatManager getTeamChatManager() { return teamChatManager; } // ← NOUVEAU
}