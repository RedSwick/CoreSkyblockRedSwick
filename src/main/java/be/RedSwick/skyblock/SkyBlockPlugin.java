package be.RedSwick.skyblock;

import be.RedSwick.skyblock.command.*;
import be.RedSwick.skyblock.hologram.HologramManager;
import be.RedSwick.skyblock.job.JobManager;
import be.RedSwick.skyblock.listener.*;
import be.RedSwick.skyblock.manager.*;
import be.RedSwick.skyblock.listener.SpawnerListener;
import be.RedSwick.skyblock.world.VoidChunkGenerator;
import org.bukkit.Bukkit;
import org.bukkit.WorldCreator;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkyBlockPlugin extends JavaPlugin {

    private static SkyBlockPlugin instance;

    private IslandManager     islandManager;
    private PlayerDataManager playerDataManager;
    private JobManager        jobManager;
    private WarpManager       warpManager;
    private be.RedSwick.skyblock.manager.IslandTeamChatManager teamChatManager;
    private HologramManager   hologramManager;
    private be.RedSwick.skyblock.leaderboard.LeaderboardManager leaderboardManager;
    private SpawnerManager    spawnerManager;
    private BossBarManager    bossBarManager;
    private be.RedSwick.skyblock.manager.CooldownManager  cooldownManager;
    private be.RedSwick.skyblock.manager.SchematicManager schematicManager;

    @Override
    public void onEnable() {
        instance = this;

        createSkyblockWorld();

        islandManager     = new IslandManager();
        playerDataManager = new PlayerDataManager();
        jobManager        = new JobManager();
        warpManager       = new WarpManager();
        teamChatManager   = new be.RedSwick.skyblock.manager.IslandTeamChatManager();
        hologramManager   = new HologramManager();
        leaderboardManager = new be.RedSwick.skyblock.leaderboard.LeaderboardManager(this);
        spawnerManager    = new SpawnerManager();
        bossBarManager    = new BossBarManager();
        cooldownManager   = new be.RedSwick.skyblock.manager.CooldownManager();
        schematicManager  = new be.RedSwick.skyblock.manager.SchematicManager(this);

        // Init managers moderation — AVANT les listeners
        be.RedSwick.skyblock.customitem.CustomItemConfig.get().init();
        be.RedSwick.skyblock.moderation.StaffPermissionManager.get().init();
        be.RedSwick.skyblock.moderation.WarnManager.get().init();
        be.RedSwick.skyblock.moderation.StaffLogManager.get().init();

        // Commandes île
        IslandCommand islandCommand = new IslandCommand();
        getCommand("is").setExecutor(islandCommand);
        getCommand("is").setTabCompleter(islandCommand);

        // Commandes globales
        getCommand("spawn").setExecutor(new SpawnCommand());
        getCommand("rang").setExecutor(new RankCommand());
        getCommand("grade").setExecutor(new GradeCommand());
        getCommand("job").setExecutor(new JobCommand());
        getCommand("warp").setExecutor(new WarpCommand());

        // Économie
        getCommand("bal").setExecutor(new EconomyCommand.BalCommand());
        getCommand("pay").setExecutor(new EconomyCommand.PayCommand());
        getCommand("essence").setExecutor(new EconomyCommand.EssenceCommand());
        getCommand("baltop").setExecutor(new EconomyCommand.BaltopCommand());
        getCommand("setbal").setExecutor(new EconomyCommand.SetBalCommand());
        getCommand("setgems").setExecutor(new EconomyCommand.SetGemsCommand());
        getCommand("shop").setExecutor(new ShopCommand());
        getCommand("arc").setExecutor(new ArcCommand());

        // Classements & Hologrammes
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

        // Tab completers
        StaffTabCompleter staffTab = new StaffTabCompleter();
        for (String cmd : List.of("mute","unmute","kick","ban","unban","tempban","warn","warns","tp","tphere","tpisland","vanish","serverclose","serveropen")) {
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

        // Listeners
        getServer().getPluginManager().registerEvents(new StaffPermissionListener(), this);
        getServer().getPluginManager().registerEvents(new ChatAndTabListener(),       this);
        getServer().getPluginManager().registerEvents(new JoinListener(),             this);
        getServer().getPluginManager().registerEvents(new PlayerDataListener(),       this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(),       this);
        getServer().getPluginManager().registerEvents(new PermissionsListener(),      this);
        getServer().getPluginManager().registerEvents(new MenuListener(),             this);
        getServer().getPluginManager().registerEvents(new GuiProtectionListener(),    this);
        getServer().getPluginManager().registerEvents(new be.RedSwick.skyblock.listener.IslandGUIListener(),  this);
        getServer().getPluginManager().registerEvents(new be.RedSwick.skyblock.listener.IslandFlyListener(),  this);
        getServer().getPluginManager().registerEvents(new JobListener(),              this);
        getServer().getPluginManager().registerEvents(new ValueBlockListener(),       this);
        getServer().getPluginManager().registerEvents(new MissionListener(),          this);

        SpawnerListener spawnerListener = new SpawnerListener();
        getServer().getPluginManager().registerEvents(spawnerListener,                this);
        getServer().getPluginManager().registerEvents(new MobStackListener(),         this);
        getServer().getPluginManager().registerEvents(new CustomItemListener(),       this);
        getServer().getPluginManager().registerEvents(new be.RedSwick.skyblock.listener.NewItemsListener(),     this);
        getServer().getPluginManager().registerEvents(new be.RedSwick.skyblock.listener.LeaderboardGuiListener(), this);
        getServer().getPluginManager().registerEvents(new be.RedSwick.skyblock.listener.DeathRespawnListener(), this);

        // Playtime ticker
        be.RedSwick.skyblock.listener.PlaytimeListener.startTicker();

        // Refresh classements toutes les 5 minutes
        Bukkit.getScheduler().runTaskTimer(this, () -> leaderboardManager.refreshAll(), 6000L, 6000L);

        getServer().getPluginManager().registerEvents(new be.RedSwick.skyblock.listener.NewItemsGuiListener(), this);

        // Init des managers custom items
        be.RedSwick.skyblock.customitem.SwordConfig.get().init();
        be.RedSwick.skyblock.customitem.LootBagData.get().init();
        be.RedSwick.skyblock.customitem.MerchantPouchData.get().init();
        be.RedSwick.skyblock.customitem.ChunkHopperManager.get().init();

        // Démarrer le ticker Sacoche de Marchand
        be.RedSwick.skyblock.listener.MerchantPouchTickListener.start();

        getServer().getPluginManager().registerEvents(new ShopListener(),          this);
        getServer().getPluginManager().registerEvents(new MuteListener(),          this);
        getServer().getPluginManager().registerEvents(new AdminItemListener(),     this);
        getServer().getPluginManager().registerEvents(new StaffLogsListener(),     this);
        getServer().getPluginManager().registerEvents(new ServerCloseListener(),   this);

        // Démarre les cycles de spawn pour tous les spawners existants
        Bukkit.getScheduler().runTaskLater(this, spawnerListener::startAllCycles, 40L);

        // Nettoyage warps sponsorisés expirés toutes les minutes
        Bukkit.getScheduler().runTaskTimer(this, () -> warpManager.cleanExpired(), 1200L, 1200L);

        // Reset affichage ActionBar après inactivité (2s)
        Bukkit.getScheduler().runTaskTimer(this, () -> getJobManager().tickResetDisplay(), 40L, 40L);

        // Ticker hologrammes mobs stackés (2s)
        Bukkit.getScheduler().runTaskTimer(this, () -> hologramManager.tickMobHolograms(), 40L, 40L);

        // Nettoyage hologrammes orphelins au démarrage
        Bukkit.getScheduler().runTaskLater(this, () -> hologramManager.cleanupOrphanHolograms(), 60L);

        // ── AUTO-SAVE PlayerData — CORRIGÉ ──────────────────────────────────────
        // FIX : saveAll() est synchrone par design (shutdown safety).
        //       Appelé depuis un thread async = risque de corruption YAML.
        //       On utilise flushDirty() qui est conçu pour l'async :
        //       - Ne sauvegarde QUE les joueurs dirty
        //       - Lance chaque save dans son propre thread async
        //       - Sur 300 joueurs : ~60 saves au lieu de 300 (80% AFK)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> playerDataManager.flushDirty(), 6000L, 6000L);

        // ── TICKER GRAVITÉ SUPPRIMÉ ──────────────────────────────────────────────
        // RAISON : GlobalSpawnerEngine.applyStackAttributes() appelle déjà setGravity(true)
        //          sur chaque mob stacké à chaque tick d'engine (toutes les 3s).
        //          Scanner TOUS les mobs de TOUS les mondes toutes les 4 ticks était
        //          une opération coûteuse pour zéro bénéfice réel.

        getLogger().info("Arcanium SkyBlock active !");
    }

    @Override
    public void onDisable() {
        // saveAll() sync ici = correct (shutdown, main thread garanti)
        if (playerDataManager != null) playerDataManager.saveAll();
        if (hologramManager != null)   hologramManager.removeAll();
        getLogger().info("Arcanium SkyBlock desactive.");
    }

    private void createSkyblockWorld() {
        if (Bukkit.getWorld("skyblock") != null) return;
        WorldCreator creator = new WorldCreator("skyblock");
        creator.generator(new VoidChunkGenerator());
        Bukkit.createWorld(creator);
    }

    public static SkyBlockPlugin getInstance()      { return instance; }
    public IslandManager getIslandManager()         { return islandManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public JobManager getJobManager()               { return jobManager; }
    public WarpManager getWarpManager()             { return warpManager; }
    public be.RedSwick.skyblock.manager.IslandTeamChatManager getTeamChatManager() { return teamChatManager; }
    public HologramManager getHologramManager()     { return hologramManager; }
    public be.RedSwick.skyblock.leaderboard.LeaderboardManager getLeaderboardManager() { return leaderboardManager; }
    public SpawnerManager  getSpawnerManager()      { return spawnerManager; }
    public BossBarManager  getBossBarManager()       { return bossBarManager; }
    public be.RedSwick.skyblock.manager.CooldownManager  getCooldownManager()  { return cooldownManager; }
    public be.RedSwick.skyblock.manager.SchematicManager getSchematicManager() { return schematicManager; }
}