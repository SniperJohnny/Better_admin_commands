package io.sniperjohnny.github.better_admin_commands;

import io.sniperjohnny.github.better_admin_commands.commands.Enchant_Command;
import io.sniperjohnny.github.better_admin_commands.commands.Fly_Command;
import io.sniperjohnny.github.better_admin_commands.commands.Gamemode_Command;
import io.sniperjohnny.github.better_admin_commands.commands.Smite_Command;
import io.sniperjohnny.github.better_admin_commands.commands.Unban_Command;
import io.sniperjohnny.github.better_admin_commands.auction.AuctionService;
import io.sniperjohnny.github.better_admin_commands.auction.Auction_Command;
import io.sniperjohnny.github.better_admin_commands.backup.BackupService;
import io.sniperjohnny.github.better_admin_commands.report.ReportService;
import io.sniperjohnny.github.better_admin_commands.report.Report_Command;
import io.sniperjohnny.github.better_admin_commands.commands.Disabled_Command;
import io.sniperjohnny.github.better_admin_commands.commands.Plugin_Command;
import io.sniperjohnny.github.better_admin_commands.commands.Root_Command;
import io.sniperjohnny.github.better_admin_commands.config.ConfigUpdater;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Break_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Burn_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Entity_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Extinguish_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Fireball_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Nuke_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Potion_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Recipe_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Spawner_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Spawnmob_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Tree_Command;
import io.sniperjohnny.github.better_admin_commands.commands.economy.Sell_Command;
import io.sniperjohnny.github.better_admin_commands.commands.economy.Worth_Command;
import io.sniperjohnny.github.better_admin_commands.commands.info.Depth_Command;
import io.sniperjohnny.github.better_admin_commands.commands.info.Getpos_Command;
import io.sniperjohnny.github.better_admin_commands.commands.info.Motd_Command;
import io.sniperjohnny.github.better_admin_commands.commands.info.Playtime_Command;
import io.sniperjohnny.github.better_admin_commands.commands.info.Realname_Command;
import io.sniperjohnny.github.better_admin_commands.commands.info.Rules_Command;
import io.sniperjohnny.github.better_admin_commands.commands.items.Book_Command;
import io.sniperjohnny.github.better_admin_commands.commands.items.Condense_Command;
import io.sniperjohnny.github.better_admin_commands.commands.items.Disposal_Command;
import io.sniperjohnny.github.better_admin_commands.commands.items.Lore_Command;
import io.sniperjohnny.github.better_admin_commands.commands.items.More_Command;
import io.sniperjohnny.github.better_admin_commands.commands.items.Powertool_Command;
import io.sniperjohnny.github.better_admin_commands.commands.items.Rename_Command;
import io.sniperjohnny.github.better_admin_commands.commands.items.Skull_Command;
import io.sniperjohnny.github.better_admin_commands.commands.items.Sort_Command;
import io.sniperjohnny.github.better_admin_commands.commands.items.Stack_Command;
import io.sniperjohnny.github.better_admin_commands.commands.items.Unlimited_Command;
import io.sniperjohnny.github.better_admin_commands.commands.jail.Deljail_Command;
import io.sniperjohnny.github.better_admin_commands.commands.jail.Jail_Command;
import io.sniperjohnny.github.better_admin_commands.commands.jail.Jails_Command;
import io.sniperjohnny.github.better_admin_commands.commands.jail.Setjail_Command;
import io.sniperjohnny.github.better_admin_commands.commands.jail.Togglejail_Command;
import io.sniperjohnny.github.better_admin_commands.commands.jail.Unjail_Command;
import io.sniperjohnny.github.better_admin_commands.commands.moderation.Kill_Command;
import io.sniperjohnny.github.better_admin_commands.commands.moderation.Suicide_Command;
import io.sniperjohnny.github.better_admin_commands.commands.moderation.Unbanip_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.Jump_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.Rtp_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.Vertical_Command;
import io.sniperjohnny.github.better_admin_commands.economy.WorthManager;
import io.sniperjohnny.github.better_admin_commands.jail.JailManager;
import io.sniperjohnny.github.better_admin_commands.listeners.Jail_Listener;
import io.sniperjohnny.github.better_admin_commands.gui.ChatPromptService;
import io.sniperjohnny.github.better_admin_commands.gui.Gui_Listener;
import io.sniperjohnny.github.better_admin_commands.listeners.Powertool_Listener;
import io.sniperjohnny.github.better_admin_commands.listeners.Unlimited_Listener;
import io.sniperjohnny.github.better_admin_commands.player.PlaytimeService;
import io.sniperjohnny.github.better_admin_commands.player.UnlimitedService;
import io.sniperjohnny.github.better_admin_commands.powertool.PowertoolService;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Broadcast_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Clear_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Feed_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Give_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.God_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Heal_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Kit_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Near_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Repair_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Speed_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Craft_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Enderchest_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Exp_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Hat_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Invsee_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Nick_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Ptime_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Pweather_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Skinchange_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Sudo_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Time_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Vanish_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.Weather_Command;
import io.sniperjohnny.github.better_admin_commands.commands.admin.World_Command;
import io.sniperjohnny.github.better_admin_commands.commands.economy.Balance_Command;
import io.sniperjohnny.github.better_admin_commands.commands.economy.Baltop_Command;
import io.sniperjohnny.github.better_admin_commands.commands.economy.Eco_Command;
import io.sniperjohnny.github.better_admin_commands.commands.economy.Pay_Command;
import io.sniperjohnny.github.better_admin_commands.commands.home.Delhome_Command;
import io.sniperjohnny.github.better_admin_commands.commands.home.Home_Command;
import io.sniperjohnny.github.better_admin_commands.commands.home.Homes_Command;
import io.sniperjohnny.github.better_admin_commands.commands.home.Sethome_Command;
import io.sniperjohnny.github.better_admin_commands.commands.info.Afk_Command;
import io.sniperjohnny.github.better_admin_commands.commands.info.Gc_Command;
import io.sniperjohnny.github.better_admin_commands.commands.info.List_Command;
import io.sniperjohnny.github.better_admin_commands.commands.info.Ping_Command;
import io.sniperjohnny.github.better_admin_commands.commands.info.Seen_Command;
import io.sniperjohnny.github.better_admin_commands.commands.info.Whois_Command;
import io.sniperjohnny.github.better_admin_commands.commands.moderation.Ban_Command;
import io.sniperjohnny.github.better_admin_commands.commands.moderation.Banlist_Command;
import io.sniperjohnny.github.better_admin_commands.commands.moderation.Ipban_Command;
import io.sniperjohnny.github.better_admin_commands.commands.moderation.Kickall_Command;
import io.sniperjohnny.github.better_admin_commands.commands.moderation.Tempban_Command;
import io.sniperjohnny.github.better_admin_commands.commands.moderation.Kick_Command;
import io.sniperjohnny.github.better_admin_commands.commands.moderation.Mute_Command;
import io.sniperjohnny.github.better_admin_commands.commands.moderation.Unmute_Command;
import io.sniperjohnny.github.better_admin_commands.commands.social.IgnoreList_Command;
import io.sniperjohnny.github.better_admin_commands.commands.social.Ignore_Command;
import io.sniperjohnny.github.better_admin_commands.commands.social.Mail_Command;
import io.sniperjohnny.github.better_admin_commands.commands.social.Me_Command;
import io.sniperjohnny.github.better_admin_commands.commands.social.Msg_Command;
import io.sniperjohnny.github.better_admin_commands.commands.social.Reply_Command;
import io.sniperjohnny.github.better_admin_commands.commands.social.SocialSpy_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.Back_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.Delwarp_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.SetSpawn_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.Setwarp_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.Spawn_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.Tp_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.Tpa_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.Tpaccept_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.Tpall_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.Tpdeny_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.Tphere_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.Tpaall_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.Tppos_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.Tptoggle_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.Tpahere_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.Warp_Command;
import io.sniperjohnny.github.better_admin_commands.commands.teleport.Warps_Command;
import io.sniperjohnny.github.better_admin_commands.economy.Balance_Gui;
import io.sniperjohnny.github.better_admin_commands.economy.Baltop_Gui;
import io.sniperjohnny.github.better_admin_commands.economy.EconomyService;
import io.sniperjohnny.github.better_admin_commands.economy.VaultEconomy;
import io.sniperjohnny.github.better_admin_commands.jail.Jails_Gui;
import io.sniperjohnny.github.better_admin_commands.mail.Mail_Gui;
import io.sniperjohnny.github.better_admin_commands.player.IgnoreList_Gui;
import io.sniperjohnny.github.better_admin_commands.player.Ptime_Gui;
import io.sniperjohnny.github.better_admin_commands.player.Pweather_Gui;
import io.sniperjohnny.github.better_admin_commands.player.Realname_Gui;
import io.sniperjohnny.github.better_admin_commands.player.Unlimited_Gui;
import io.sniperjohnny.github.better_admin_commands.home.HomeManager;
import io.sniperjohnny.github.better_admin_commands.home.Home_Gui;
import io.sniperjohnny.github.better_admin_commands.kit.Kit_Gui;
import io.sniperjohnny.github.better_admin_commands.shop.EconomyShopGuiImporter;
import io.sniperjohnny.github.better_admin_commands.shop.ShopService;
import io.sniperjohnny.github.better_admin_commands.shop.Shop_Command;
import io.sniperjohnny.github.better_admin_commands.shop.Shop_Editor;
import io.sniperjohnny.github.better_admin_commands.warp.Warp_Gui;
import io.sniperjohnny.github.better_admin_commands.kit.KitManager;
import io.sniperjohnny.github.better_admin_commands.listeners.Activity_Listener;
import io.sniperjohnny.github.better_admin_commands.listeners.Chat_Listener;
import io.sniperjohnny.github.better_admin_commands.listeners.Join_Listener;
import io.sniperjohnny.github.better_admin_commands.listeners.Quit_Listener;
import io.sniperjohnny.github.better_admin_commands.listeners.Respawn_Listener;
import io.sniperjohnny.github.better_admin_commands.mail.MailService;
import io.sniperjohnny.github.better_admin_commands.moderation.MuteService;
import io.sniperjohnny.github.better_admin_commands.placeholder.NicknamePlaceholders;
import io.sniperjohnny.github.better_admin_commands.player.AfkService;
import io.sniperjohnny.github.better_admin_commands.player.NickService;
import io.sniperjohnny.github.better_admin_commands.player.PlayerPreferences;
import io.sniperjohnny.github.better_admin_commands.player.VanishService;
import io.sniperjohnny.github.better_admin_commands.skin.SkinService;
import io.sniperjohnny.github.better_admin_commands.spawn.SpawnManager;
import io.sniperjohnny.github.better_admin_commands.storage.Database;
import io.sniperjohnny.github.better_admin_commands.storage.LocalStore;
import io.sniperjohnny.github.better_admin_commands.teleport.TeleportService;
import io.sniperjohnny.github.better_admin_commands.teleport.TpaService;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.warp.WarpManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class Better_Admin_Commands extends JavaPlugin {

    /** Name of the management command as declared in plugin.yml. */
    public static final String DEFAULT_ROOT_COMMAND = "betteradmincommands";

    private static Better_Admin_Commands instance;

    private Database database;
    private LocalStore localPlayers;
    private LocalStore localHomes;
    private LocalStore localSettings;
    private LocalStore localMail;
    private LocalStore localAuction;
    private LocalStore localShops;
    private LocalStore localShopItems;
    private EconomyService economy;
    private VaultEconomy currency;

    private SpawnManager spawns;
    private WarpManager warps;
    private HomeManager homes;
    private TeleportService teleports;
    private TpaService tpa;
    private KitManager kits;
    private MuteService mutes;
    private VanishService vanish;
    private PlayerPreferences preferences;
    private NickService nicks;
    private SkinService skins;
    private AuctionService auctions;
    private ShopService shops;
    private Shop_Editor shopEditor;
    private Shop_Command shopCommand;
    private EconomyShopGuiImporter shopImporter;
    private Warp_Gui warpGui;
    private Home_Gui homeGui;
    private Kit_Gui kitGui;
    private Balance_Gui balanceGui;
    private Baltop_Gui baltopGui;
    private Mail_Gui mailGui;
    private IgnoreList_Gui ignoreListGui;
    private Jails_Gui jailsGui;
    private Ptime_Gui ptimeGui;
    private Pweather_Gui pweatherGui;
    private Unlimited_Gui unlimitedGui;
    private Realname_Gui realnameGui;
    private ChatPromptService chatPrompts;
    private ReportService reports;
    private MailService mail;
    private AfkService afk;
    private BackupService backups;
    private JailManager jails;
    private PowertoolService powerTools;
    private UnlimitedService unlimited;
    private WorthManager worth;
    private PlaytimeService playtime;
    private Jail_Listener jailListener;

    private BukkitTask saveTask;
    private BukkitTask afkTask;
    private BukkitTask jailTask;
    private BukkitTask reconnectTask;
    private BukkitTask auctionTask;

    private ConfigUpdater configUpdater;
    private Plugin_Command rootCommand;
    private Disabled_Command disabledCommand;

    /** The command declared in plugin.yml, kept so it can be registered back. */
    private PluginCommand declaredRoot;
    /** Replacement command when commands.root-name is changed, else {@code null}. */
    private Root_Command customRoot;
    /** Name the management command currently answers to. */
    private String rootLabel = DEFAULT_ROOT_COMMAND;

    /** Executors and completers of every command, needed for the soft toggle. */
    private final Map<String, CommandExecutor> executors = new LinkedHashMap<>();
    private final Map<String, TabExecutor> completers = new LinkedHashMap<>();
    private final List<Listener> listeners = new ArrayList<>();

    /** True while the plugin was switched off with /betteradmincommands disable. */
    private boolean disabled;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        declaredRoot = getCommand(DEFAULT_ROOT_COMMAND);
        disabledCommand = new Disabled_Command(this);

        // Bring an older config.yml up to date before anything reads from it.
        // Missing options are added, values already in the file are left alone.
        configUpdater = new ConfigUpdater(this);
        configUpdater.update();

        Msg.setPrefix(getConfig().getString("messages.prefix", "&8[&6BetterAdmin&8] &r"));

        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Could not create the plugin data folder.");
        }

        // --- local safe files ----------------------------------------------
        // These always mirror the database, and become the source of truth
        // while the database is unreachable. They never require MySQL.
        File dataFolder = new File(getDataFolder(), "data");
        localPlayers = new LocalStore(this, new File(dataFolder, "players.yml"));
        localHomes = new LocalStore(this, new File(dataFolder, "homes.yml"));
        localSettings = new LocalStore(this, new File(dataFolder, "player_settings.yml"));
        localMail = new LocalStore(this, new File(dataFolder, "mail.yml"));
        localAuction = new LocalStore(this, new File(dataFolder, "auction.yml"));
        localShops = new LocalStore(this, new File(dataFolder, "shops.yml"));
        localShopItems = new LocalStore(this, new File(dataFolder, "shop_items.yml"));
        localPlayers.load();
        localHomes.load();
        localSettings.load();
        localMail.load();
        localAuction.load();
        localShops.load();
        localShopItems.load();

        // --- storage -------------------------------------------------------
        // A missing database is not fatal: the plugin starts anyway and runs
        // from the local safe files until the connection comes back.
        database = new Database(this);
        try {
            database.connect();
            getLogger().info("Connected to the MySQL database.");
        } catch (SQLException e) {
            getLogger().severe("Could not connect to MySQL: " + e.getMessage());
            getLogger().warning("Running from the local safe files instead. The plugin will retry "
                    + "the connection every "
                    + Math.max(1L, getConfig().getLong("database.reconnect-interval-minutes", 30L))
                    + " minute(s).");
        }

        // --- economy -------------------------------------------------------
        economy = new EconomyService(this, database, localPlayers);
        try {
            economy.loadAll();
        } catch (SQLException e) {
            getLogger().severe("Could not load balances: " + e.getMessage());
        }
        registerVaultEconomy();

        // --- files & data --------------------------------------------------
        spawns = new SpawnManager(this);
        spawns.load();

        warps = new WarpManager(this);
        warps.load();

        homes = new HomeManager(this, database, localHomes);
        try {
            homes.loadAll();
        } catch (SQLException e) {
            getLogger().severe("Could not load homes: " + e.getMessage());
        }

        mutes = new MuteService(this, database, localPlayers);
        try {
            mutes.loadAll();
        } catch (SQLException e) {
            getLogger().severe("Could not load mutes: " + e.getMessage());
        }

        // --- runtime services ----------------------------------------------
        teleports = new TeleportService(this);
        tpa = new TpaService(this);
        kits = new KitManager(this);
        vanish = new VanishService(this);
        nicks = new NickService(this);
        skins = new SkinService(this);
        preferences = new PlayerPreferences(this, database, localSettings);
        mail = new MailService(this, database, localMail);
        afk = new AfkService(this);
        backups = new BackupService(this, database, localPlayers, localHomes, localSettings, localMail);
        powerTools = new PowertoolService(this);
        unlimited = new UnlimitedService(this);
        worth = new WorthManager(this);
        playtime = new PlaytimeService(this);
        auctions = new AuctionService(this, database, localAuction);
        auctions.loadAll();
        shops = new ShopService(this, database, localShops, localShopItems);
        shops.loadAll();
        shopImporter = new EconomyShopGuiImporter(this);
        shopEditor = new Shop_Editor(this);
        importShopsIfNeeded();
        warpGui = new Warp_Gui(this);
        homeGui = new Home_Gui(this);
        kitGui = new Kit_Gui(this);
        balanceGui = new Balance_Gui(this);
        baltopGui = new Baltop_Gui(this);
        mailGui = new Mail_Gui(this);
        ignoreListGui = new IgnoreList_Gui(this);
        jailsGui = new Jails_Gui(this);
        ptimeGui = new Ptime_Gui(this);
        pweatherGui = new Pweather_Gui(this);
        unlimitedGui = new Unlimited_Gui(this);
        realnameGui = new Realname_Gui(this);
        chatPrompts = new ChatPromptService(this);
        reports = new ReportService(this, database);
        reports.loadAll();

        jails = new JailManager(this);
        jails.load();

        // --- wiring ---------------------------------------------------------
        rootCommand = new Plugin_Command(this);
        registerCommands();
        applyRootCommand();
        registerListeners();
        startTasks();
        registerPlaceholders();

        getLogger().info("Better_Admin_Commands enabled with " + warps.size() + " warp(s).");
    }

    @Override
    public void onDisable() {
        cancelTasks();
        if (economy != null) {
            economy.saveBlocking();
        }
        if (database != null) {
            database.close();
        }
        getLogger().info("Better_Admin_Commands disabled.");
    }

    /* -------------------------------------------- config and soft toggle --- */

    /**
     * Reloads config.yml (adding any new options), the file based stores and the
     * configured command name. Used by {@code /betteradmincommands reload}.
     *
     * @return the number of new options that were added to config.yml
     */
    public int reloadAll() {
        int added = configUpdater.update();
        Msg.setPrefix(getConfig().getString("messages.prefix", "&8[&6BetterAdmin&8] &r"));
        spawns.load();
        warps.load();
        jails.load();
        applyRootCommand();
        if (shops != null && shops.enabled() && shops.importForced()) {
            importShops();
        }
        // Re-apply stored names, so a nickname that something else overwrote is
        // restored without a relog.
        for (org.bukkit.entity.Player online : getServer().getOnlinePlayers()) {
            preferences.applyNickname(online);
        }
        return added;
    }

    /** Name the plugin management command currently answers to. */
    public String rootLabel() {
        return rootLabel;
    }

    /** Whether the plugin was switched off with {@code /betteradmincommands disable}. */
    public boolean isPluginDisabled() {
        return disabled;
    }

    /**
     * Turns every feature of the plugin on or off without unloading it.
     *
     * <p>While it is off no listener runs, every command except the management
     * command answers with a notice, and the repeating tasks are stopped.
     * Balances are written back before the features stop, so a maintenance
     * window cannot lose data. The plugin stays loaded, so replacing the jar
     * file still needs a server restart.</p>
     */
    public void setPluginDisabled(boolean value) {
        if (disabled == value) {
            return;
        }
        disabled = value;
        if (value) {
            cancelTasks();
            if (economy != null) {
                economy.saveBlocking();
            }
            for (Listener listener : listeners) {
                HandlerList.unregisterAll(listener);
            }
            listeners.clear();
            for (String name : executors.keySet()) {
                if (name.equals(DEFAULT_ROOT_COMMAND)) {
                    continue; // the management command has to stay usable
                }
                PluginCommand command = getCommand(name);
                if (command != null) {
                    command.setExecutor(disabledCommand);
                }
            }
            for (String name : completers.keySet()) {
                if (name.equals(DEFAULT_ROOT_COMMAND)) {
                    continue;
                }
                PluginCommand command = getCommand(name);
                if (command != null) {
                    command.setTabCompleter(disabledCommand);
                }
            }
            getLogger().info("The plugin was disabled - only /" + rootLabel + " is still answering.");
        } else {
            for (Map.Entry<String, CommandExecutor> entry : executors.entrySet()) {
                PluginCommand command = getCommand(entry.getKey());
                if (command != null) {
                    command.setExecutor(entry.getValue());
                }
            }
            for (Map.Entry<String, TabExecutor> entry : completers.entrySet()) {
                PluginCommand command = getCommand(entry.getKey());
                if (command != null) {
                    command.setTabCompleter(entry.getValue());
                }
            }
            registerListeners();
            startTasks();
            getLogger().info("The plugin was enabled again.");
        }
    }

    /**
     * Registers the management command under the name and aliases from
     * config.yml. Command names cannot be changed once plugin.yml is loaded, so
     * when the configured name differs the plugin.yml command is replaced by a
     * {@link Root_Command} that is registered in the command map directly.
     */
    private void applyRootCommand() {
        String configuredName = normalizeCommandName(getConfig().getString("commands.root-name", DEFAULT_ROOT_COMMAND));
        if (configuredName == null) {
            getLogger().warning("commands.root-name is not a valid command name - using "
                    + DEFAULT_ROOT_COMMAND + ".");
            configuredName = DEFAULT_ROOT_COMMAND;
        }
        List<String> configuredAliases = new ArrayList<>();
        for (String alias : getConfig().getStringList("commands.root-aliases")) {
            String normalized = normalizeCommandName(alias);
            if (normalized == null || normalized.equals(configuredName) || configuredAliases.contains(normalized)) {
                continue;
            }
            configuredAliases.add(normalized);
        }

        CommandMap commandMap = commandMap();
        if (commandMap == null) {
            getLogger().warning("The command map is not reachable - commands.root-name is ignored.");
            return;
        }

        // Drop whatever this method registered the last time.
        if (customRoot != null) {
            customRoot.unregister(commandMap);
            customRoot = null;
        }

        Set<String> declared = new LinkedHashSet<>();
        declared.add(DEFAULT_ROOT_COMMAND);
        if (declaredRoot != null) {
            declared.addAll(declaredRoot.getAliases());
        }

        Set<String> wanted = new LinkedHashSet<>();
        wanted.add(configuredName);
        wanted.addAll(configuredAliases);

        if (wanted.equals(declared)) {
            registerDeclaredRoot(commandMap);
            rootLabel = DEFAULT_ROOT_COMMAND;
            return;
        }

        if (declaredRoot != null) {
            declaredRoot.unregister(commandMap);
        }
        Root_Command custom = new Root_Command(configuredName, configuredAliases, rootCommand);
        if (commandMap.register(getName().toLowerCase(Locale.ROOT), custom)) {
            customRoot = custom;
            rootLabel = configuredName;
            getLogger().info("The plugin management command is /" + configuredName
                    + (configuredAliases.isEmpty()
                    ? "" : " (aliases: /" + String.join(", /", configuredAliases) + ")") + ".");
            return;
        }

        getLogger().warning("The command name '" + configuredName + "' is already in use - keeping /"
                + DEFAULT_ROOT_COMMAND + ".");
        registerDeclaredRoot(commandMap);
        rootLabel = DEFAULT_ROOT_COMMAND;
    }

    /** Puts the plugin.yml command back when the configured name was dropped. */
    private void registerDeclaredRoot(CommandMap commandMap) {
        if (declaredRoot == null || commandMap.getCommand(DEFAULT_ROOT_COMMAND) != null) {
            return;
        }
        commandMap.register(getName().toLowerCase(Locale.ROOT), declaredRoot);
    }

    /** Lowercases a configured command name, or {@code null} when it is unusable. */
    private static String normalizeCommandName(String value) {
        if (value == null) {
            return null;
        }
        String name = value.trim().toLowerCase(Locale.ROOT);
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        return name.matches("[a-z0-9_\\-]{1,32}") ? name : null;
    }

    /** The server's command map, or {@code null} when it cannot be reached. */
    private CommandMap commandMap() {
        try {
            Object server = getServer();
            return (CommandMap) server.getClass().getMethod("getCommandMap").invoke(server);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /* ------------------------------------------------------------ wiring --- */

    private void registerVaultEconomy() {
        if (!getConfig().getBoolean("economy.enabled", true)) {
            getLogger().info("The built-in economy is disabled in config.yml.");
            return;
        }
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault is not installed - the economy will not be available to other plugins.");
            return;
        }
        // Only touch the Vault classes when Vault is actually installed.
        currency = new VaultEconomy(this, economy);
        var registration = getServer().getServicesManager().getRegistration(Economy.class);
        if (registration != null) {
            getLogger().warning("Another economy plugin is already registered with Vault. "
                    + "Registering ours with the highest priority anyway.");
        }
        getServer().getServicesManager().register(Economy.class, currency, this, ServicePriority.Highest);
        getLogger().info("Registered the MySQL economy with Vault.");
    }

    /** Starts every repeating task - on enable and after the plugin is switched back on. */
    private void startTasks() {
        startSaveTask();
        startAfkTask();
        startJailTask();
        startReconnectTask();
        startAuctionTask();
    }

    /** Stops every repeating task - on disable and when the plugin is switched off. */
    private void cancelTasks() {
        for (BukkitTask task : new BukkitTask[]{saveTask, afkTask, jailTask, reconnectTask, auctionTask}) {
            if (task != null) {
                task.cancel();
            }
        }
        saveTask = null;
        afkTask = null;
        jailTask = null;
        reconnectTask = null;
        auctionTask = null;
    }

    private void startSaveTask() {
        long seconds = Math.max(30L, getConfig().getLong("economy.save-interval-seconds", 300L));
        saveTask = getServer().getScheduler().runTaskTimerAsynchronously(
                this, economy::saveDirty, seconds * 20L, seconds * 20L);
    }

    private void startAfkTask() {
        int minutes = Math.max(1, getConfig().getInt("afk.auto-afk-minutes", 10));
        if (!getConfig().getBoolean("afk.auto-afk-enabled", true)) {
            return;
        }
        afkTask = getServer().getScheduler().runTaskTimer(
                this, () -> afk.checkIdle(minutes), 20L * 60L, 20L * 60L);
    }

    private void startJailTask() {
        jailTask = getServer().getScheduler().runTaskTimer(
                this, () -> jailListener.checkExpired(), 20L * 15L, 20L * 15L);
    }

    /**
     * Periodically checks the database connection and, when it has just come
     * back, pushes everything that was written to the local safe files.
     */
    private void startReconnectTask() {
        long minutes = Math.max(1L, getConfig().getLong("database.reconnect-interval-minutes", 30L));
        long ticks = minutes * 60L * 20L;
        reconnectTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            boolean wasAvailable = database.isAvailable();
            boolean nowAvailable = database.checkConnection();
            if (nowAvailable && !wasAvailable) {
                resyncLocalToDatabase();
            }
        }, ticks, ticks);
    }

    /**
     * Forced by {@code /betteradmincommands reconnect}. Attempts the connection
     * immediately and, when it succeeds, pushes the local safe files into the
     * database. Returns whether the database is reachable afterwards.
     */
    public boolean reconnectDatabase() {
        boolean nowAvailable = database.checkConnection();
        if (nowAvailable) {
            resyncLocalToDatabase();
        }
        return nowAvailable;
    }

    /** Marks overdue auction listings as expired once a minute. */
    private void startAuctionTask() {
        if (!getConfig().getBoolean("auction.enabled", true)) {
            return;
        }
        auctionTask = getServer().getScheduler().runTaskTimer(
                this, () -> auctions.expire(), 20L * 60L, 20L * 60L);
    }

    /** Writes the local safe files back into the database after a reconnect. */
    private void resyncLocalToDatabase() {
        getLogger().info("Database is back - syncing the local safe files...");
        economy.resyncToDatabase();
        mutes.resyncToDatabase();
        homes.resyncToDatabase();
        preferences.resyncToDatabase();
        mail.resyncToDatabase();
        shops.resyncToDatabase();
        getLogger().info("Local safe files synced back into the database.");
    }

    /* --------------------------------------------------------------- shop --- */

    /** Runs the shop import at start-up when the shop is empty or a re-import was asked for. */
    private void importShopsIfNeeded() {
        if (!shops.enabled() || !shops.importOnStartup()) {
            return;
        }
        if (!shops.isEmpty() && !shops.importForced()) {
            getLogger().info("Using the stored shop with " + shops.shopCount() + " shop(s) and "
                    + shops.itemCount() + " item(s). Run /shop import to read the EconomyShopGUI files again.");
            return;
        }
        // Wait one tick: EconomyShopGUI has to be running before it can be turned
        // off, and plugins are enabled one after another during start-up.
        getServer().getScheduler().runTask(this, this::importShops);
    }

    /**
     * Scans the EconomyShopGUI files and replaces the stored shop with what was
     * found. Reading the files happens off the server thread; swapping the shop
     * over happens back on it.
     *
     * @return a message describing what happened, for whoever asked (chat or log)
     */
    public CompletableFuture<String> importShops() {
        CompletableFuture<String> future = new CompletableFuture<>();
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            EconomyShopGuiImporter.Imported imported;
            try {
                imported = shopImporter.scan();
            } catch (RuntimeException e) {
                getLogger().warning("The shop import failed: " + e.getMessage());
                getServer().getScheduler().runTask(this,
                        () -> future.complete("&cThe import failed: " + e.getMessage()));
                return;
            }
            if (imported.shops().isEmpty()) {
                getLogger().warning("No EconomyShopGUI shop files were found - the shop stays empty.");
                getServer().getScheduler().runTask(this, () -> future.complete(
                        "&cNo EconomyShopGUI shop files were found. Put them in plugins/EconomyShopGUI*/shops/."));
                return;
            }
            // Disabling a plugin and swapping the shop over both have to happen
            // on the server thread, so only the reading is done off it.
            boolean disableWanted = getConfig().getBoolean("shop.import.disable-plugin", true);
            getServer().getScheduler().runTask(this, () -> {
                String disabled = disableWanted ? shopImporter.disablePluginIfPresent() : null;
                if (disabled != null) {
                    // The plugin that just went off may still hold our command
                    // names, so bind ours again before players can click them.
                    rebindCommands();
                }
                shops.replaceAll(imported.shops(), imported.items());
                StringBuilder summary = new StringBuilder("&aImported ")
                        .append(imported.shops().size()).append(" shop(s) with ")
                        .append(imported.itemCount()).append(" item(s)");
                if (disabled != null) {
                    summary.append(", and turned off ").append(disabled);
                }
                summary.append(".");
                getLogger().info("Imported " + imported.shops().size() + " shop(s) with "
                        + imported.itemCount() + " item(s) from EconomyShopGUI ("
                        + String.join(", ", imported.sources()) + ").");
                future.complete(summary.toString());
            });
        });
        return future;
    }

    private void registerListeners() {
        listeners.clear();
        listen(new Join_Listener(this));
        listen(new Quit_Listener(this));
        listen(new Chat_Listener(this));
        listen(new Respawn_Listener(this));
        listen(new Activity_Listener(this));
        listen(new Powertool_Listener(this));
        listen(new Unlimited_Listener(this));
        listen(new Disposal_Command.Disposal_Listener());
        listen(new Gui_Listener());
        jailListener = new Jail_Listener(this);
        listen(jailListener);
    }

    /**
     * Hands the nickname to PlaceholderAPI when it is installed, so a tab list
     * or name tag plugin can render it. The expansion class is only touched when
     * PlaceholderAPI is present, which keeps the dependency optional.
     */
    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        new NicknamePlaceholders(this).register();
        getLogger().info("Registered the PlaceholderAPI expansion %betteradmincommands_nickname%.");
    }

    /** Registers a listener and remembers it, so it can be taken off again. */
    private void listen(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
        listeners.add(listener);
    }

    private void registerCommands() {
        // ---- admin ---------------------------------------------------------
        register("enchant", new Enchant_Command());
        register("gm", new Gamemode_Command());
        register("fly", new Fly_Command());
        register("smite", new Smite_Command());
        register("unban", new Unban_Command());
        register("heal", new Heal_Command());
        register("feed", new Feed_Command());
        register("god", new God_Command());
        register("speed", new Speed_Command());
        register("repair", new Repair_Command());
        register("vanish", new Vanish_Command(this));
        register("near", new Near_Command());
        register("give", new Give_Command());
        register("clear", new Clear_Command());
        register("kit", new Kit_Command(this));
        register("broadcast", new Broadcast_Command());

        // ---- moderation ----------------------------------------------------
        register("kick", new Kick_Command());
        register("ban", new Ban_Command());
        register("mute", new Mute_Command(this));
        register("unmute", new Unmute_Command(this));

        // ---- economy -------------------------------------------------------
        register("balance", new Balance_Command(this));
        register("baltop", new Baltop_Command(this));
        register("pay", new Pay_Command(this));
        register("eco", new Eco_Command(this));

        // ---- teleporting ---------------------------------------------------
        register("spawn", new Spawn_Command(this));
        register("setspawn", new SetSpawn_Command(this));
        register("back", new Back_Command(this));
        register("tp", new Tp_Command(this));
        register("tppos", new Tppos_Command(this));
        register("tphere", new Tphere_Command(this));
        register("tpall", new Tpall_Command(this));
        register("tpa", new Tpa_Command(this));
        register("tpahere", new Tpahere_Command(this));
        register("tpaaccept", new Tpaccept_Command(this));
        register("tpadeny", new Tpdeny_Command(this));
        register("warp", new Warp_Command(this));
        register("setwarp", new Setwarp_Command(this));
        register("delwarp", new Delwarp_Command(this));
        register("warps", new Warps_Command(this));

        register("nick", new Nick_Command(this));
        register("hat", new Hat_Command());
        register("skinchange", new Skinchange_Command(this));
        register("ah", new Auction_Command(this));
        shopCommand = new Shop_Command(this);
        register("shop", shopCommand);
        register("report", new Report_Command(this));
        register("reports", new Report_Command(this));
        register("craft", new Craft_Command());
        register("enderchest", new Enderchest_Command());
        register("invsee", new Invsee_Command());
        register("sudo", new Sudo_Command());
        register("exp", new Exp_Command());
        register("time", new Time_Command());
        register("weather", new Weather_Command());
        register("ptime", new Ptime_Command(this));
        register("pweather", new Pweather_Command(this));
        register("world", new World_Command(this));

        // ---- moderation ----------------------------------------------------
        register("tempban", new Tempban_Command());
        register("ipban", new Ipban_Command());
        register("kickall", new Kickall_Command());
        register("banlist", new Banlist_Command());

        // ---- social --------------------------------------------------------
        register("msg", new Msg_Command(this));
        register("reply", new Reply_Command(this));
        register("socialspy", new SocialSpy_Command(this));
        register("ignore", new Ignore_Command(this));
        register("ignorelist", new IgnoreList_Command(this));
        register("me", new Me_Command(this));
        register("mail", new Mail_Command(this));

        // ---- info ----------------------------------------------------------
        register("whois", new Whois_Command(this));
        register("seen", new Seen_Command(this));
        register("list", new List_Command());
        register("ping", new Ping_Command());
        register("gc", new Gc_Command());
        register("afk", new Afk_Command(this));

        // ---- homes ---------------------------------------------------------
        register("home", new Home_Command(this));
        register("sethome", new Sethome_Command(this));
        register("delhome", new Delhome_Command(this));
        register("homes", new Homes_Command(this));

        // ---- teleport extras -----------------------------------------------
        register("tptoggle", new Tptoggle_Command(this));
        register("tpaall", new Tpaall_Command(this));

        // ---- items ---------------------------------------------------------
        register("more", new More_Command());
        register("rename", new Rename_Command());
        register("lore", new Lore_Command());
        register("skull", new Skull_Command());
        register("book", new Book_Command());
        register("condense", new Condense_Command());
        register("stack", new Stack_Command());
        register("sort", new Sort_Command());
        register("unlimited", new Unlimited_Command(this));
        register("disposal", new Disposal_Command());
        register("powertool", new Powertool_Command(this));

        // ---- movement ------------------------------------------------------
        register("top", new Vertical_Command(this));
        register("bottom", new Vertical_Command(this));
        register("descend", new Vertical_Command(this));
        register("jump", new Jump_Command(this));
        register("rtp", new Rtp_Command(this));

        // ---- world and mobs ------------------------------------------------
        register("break", new Break_Command());
        register("tree", new Tree_Command());
        register("bigtree", new Tree_Command());
        register("spawner", new Spawner_Command());
        register("spawnmob", new Spawnmob_Command());
        register("nuke", new Nuke_Command());
        register("fireball", new Fireball_Command());
        register("potion", new Potion_Command());
        register("burn", new Burn_Command());
        register("ext", new Extinguish_Command());
        register("recipe", new Recipe_Command());
        register("killall", new Entity_Command());
        register("butcher", new Entity_Command());
        register("remove", new Entity_Command());
        register("kill", new Kill_Command());
        register("suicide", new Suicide_Command());

        // ---- jail ----------------------------------------------------------
        register("jail", new Jail_Command(this));
        register("setjail", new Setjail_Command(this));
        register("deljail", new Deljail_Command(this));
        register("jails", new Jails_Command(this));
        register("unjail", new Unjail_Command(this));
        register("togglejail", new Togglejail_Command(this));

        // ---- economy and info ----------------------------------------------
        register("worth", new Worth_Command(this));
        register("sell", new Sell_Command(this));
        register("depth", new Depth_Command());
        register("getpos", new Getpos_Command());
        register("playtime", new Playtime_Command(this));
        register("realname", new Realname_Command(this));
        register("motd", new Motd_Command(this));
        register("rules", new Rules_Command(this));
        register("unbanip", new Unbanip_Command());

        // ---- plugin management ---------------------------------------------
        register(DEFAULT_ROOT_COMMAND, rootCommand);
    }

    private void register(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            warnCommandUnavailable(name);
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        executors.put(name, executor);
        completers.put(name, executor);
    }

    private void register(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            warnCommandUnavailable(name);
            return;
        }
        command.setExecutor(executor);
        executors.put(name, executor);
    }

    /**
     * Binds every command executor again. Used after another plugin that held one
     * of our command names was switched off, so for example {@code /shop} answers
     * with our shop instead of the disabled plugin's command.
     */
    public void rebindCommands() {
        for (Map.Entry<String, CommandExecutor> entry : executors.entrySet()) {
            PluginCommand command = getCommand(entry.getKey());
            if (command == null) {
                continue;
            }
            command.setExecutor(entry.getValue());
            TabExecutor completer = completers.get(entry.getKey());
            if (completer != null) {
                command.setTabCompleter(completer);
            }
        }
    }

    /**
     * Explains a command this plugin cannot use. The usual reason is another
     * plugin that declares the same name and got to it first, which would
     * otherwise look like this plugin's command silently doing nothing.
     */
    private void warnCommandUnavailable(String name) {
        getLogger().warning("Command '" + name + "' is not available to this plugin. Most likely "
                + "another plugin also declares '/" + name + "' and won that name - check the "
                + "start-up log for a duplicate command warning.");
    }

    /**
     * Commands this plugin registered that it can no longer reach, for example
     * because another plugin took the name over.
     */
    public List<String> unboundCommands() {
        List<String> unbound = new ArrayList<>();
        for (String name : executors.keySet()) {
            if (!name.equals(DEFAULT_ROOT_COMMAND) && getCommand(name) == null) {
                unbound.add(name);
            }
        }
        return unbound;
    }

    /* ----------------------------------------------------------- getters --- */

    public static Better_Admin_Commands get_Instance() {
        return instance;
    }

    public EconomyService economy() {
        return economy;
    }

    public VaultEconomy currency() {
        return currency;
    }

    public SpawnManager spawns() {
        return spawns;
    }

    public WarpManager warps() {
        return warps;
    }

    public HomeManager homes() {
        return homes;
    }

    public TeleportService teleports() {
        return teleports;
    }

    public TpaService tpa() {
        return tpa;
    }

    public KitManager kits() {
        return kits;
    }

    public MuteService mutes() {
        return mutes;
    }

    public VanishService vanish() {
        return vanish;
    }

    public PlayerPreferences preferences() {
        return preferences;
    }

    public NickService nicks() {
        return nicks;
    }

    public SkinService skins() {
        return skins;
    }

    public MailService mail() {
        return mail;
    }

    public AfkService afk() {
        return afk;
    }

    public BackupService backups() {
        return backups;
    }

    public Database database() {
        return database;
    }

    public LocalStore localPlayers() {
        return localPlayers;
    }

    public LocalStore localHomes() {
        return localHomes;
    }

    public LocalStore localSettings() {
        return localSettings;
    }

    public LocalStore localMail() {
        return localMail;
    }

    public LocalStore localAuction() {
        return localAuction;
    }

    public JailManager jails() {
        return jails;
    }

    public PowertoolService powerTools() {
        return powerTools;
    }

    public UnlimitedService unlimited() {
        return unlimited;
    }

    public WorthManager worth() {
        return worth;
    }

    public PlaytimeService playtime() {
        return playtime;
    }

    public AuctionService auctions() {
        return auctions;
    }

    public ShopService shops() {
        return shops;
    }

    public EconomyShopGuiImporter shopImporter() {
        return shopImporter;
    }

    public Shop_Editor shopEditor() {
        return shopEditor;
    }

    /** The /shop command, so the editor can hand control back to the normal view. */
    public Shop_Command shopCommand() {
        return shopCommand;
    }

    public Warp_Gui warpGui() {
        return warpGui;
    }

    public Home_Gui homeGui() {
        return homeGui;
    }

    public Kit_Gui kitGui() {
        return kitGui;
    }

    public Balance_Gui balanceGui() {
        return balanceGui;
    }

    public Baltop_Gui baltopGui() {
        return baltopGui;
    }

    public Mail_Gui mailGui() {
        return mailGui;
    }

    public IgnoreList_Gui ignoreListGui() {
        return ignoreListGui;
    }

    public Jails_Gui jailsGui() {
        return jailsGui;
    }

    public Ptime_Gui ptimeGui() {
        return ptimeGui;
    }

    public Pweather_Gui pweatherGui() {
        return pweatherGui;
    }

    public Unlimited_Gui unlimitedGui() {
        return unlimitedGui;
    }

    public Realname_Gui realnameGui() {
        return realnameGui;
    }

    public ChatPromptService chatPrompts() {
        return chatPrompts;
    }

    public ReportService reports() {
        return reports;
    }
}
