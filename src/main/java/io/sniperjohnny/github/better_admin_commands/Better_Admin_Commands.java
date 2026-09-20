package io.sniperjohnny.github.better_admin_commands;

import io.sniperjohnny.github.better_admin_commands.commands.Enchant_Command;
import io.sniperjohnny.github.better_admin_commands.commands.Fly_Command;
import io.sniperjohnny.github.better_admin_commands.commands.Gamemode_Command;
import io.sniperjohnny.github.better_admin_commands.commands.Smite_Command;
import io.sniperjohnny.github.better_admin_commands.commands.Unban_Command;
import io.sniperjohnny.github.better_admin_commands.backup.BackupService;
import io.sniperjohnny.github.better_admin_commands.commands.Plugin_Command;
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
import io.sniperjohnny.github.better_admin_commands.economy.EconomyService;
import io.sniperjohnny.github.better_admin_commands.economy.VaultEconomy;
import io.sniperjohnny.github.better_admin_commands.home.HomeManager;
import io.sniperjohnny.github.better_admin_commands.kit.KitManager;
import io.sniperjohnny.github.better_admin_commands.listeners.Activity_Listener;
import io.sniperjohnny.github.better_admin_commands.listeners.Chat_Listener;
import io.sniperjohnny.github.better_admin_commands.listeners.Join_Listener;
import io.sniperjohnny.github.better_admin_commands.listeners.Quit_Listener;
import io.sniperjohnny.github.better_admin_commands.listeners.Respawn_Listener;
import io.sniperjohnny.github.better_admin_commands.mail.MailService;
import io.sniperjohnny.github.better_admin_commands.moderation.MuteService;
import io.sniperjohnny.github.better_admin_commands.player.AfkService;
import io.sniperjohnny.github.better_admin_commands.player.PlayerPreferences;
import io.sniperjohnny.github.better_admin_commands.player.VanishService;
import io.sniperjohnny.github.better_admin_commands.spawn.SpawnManager;
import io.sniperjohnny.github.better_admin_commands.storage.Database;
import io.sniperjohnny.github.better_admin_commands.teleport.TeleportService;
import io.sniperjohnny.github.better_admin_commands.teleport.TpaService;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.warp.WarpManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;

public final class Better_Admin_Commands extends JavaPlugin {

    private static Better_Admin_Commands instance;

    private Database database;
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

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        Msg.setPrefix(getConfig().getString("messages.prefix", "&8[&6BetterAdmin&8] &r"));

        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Could not create the plugin data folder.");
        }

        // --- storage -------------------------------------------------------
        try {
            database = new Database(this);
            database.connect();
        } catch (SQLException e) {
            getLogger().severe("Could not connect to MySQL: " + e.getMessage());
            getLogger().severe("Check the 'database' section in config.yml. Disabling the plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // --- economy -------------------------------------------------------
        economy = new EconomyService(this, database);
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

        homes = new HomeManager(this, database);
        try {
            homes.loadAll();
        } catch (SQLException e) {
            getLogger().severe("Could not load homes: " + e.getMessage());
        }

        mutes = new MuteService(this, database);
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
        preferences = new PlayerPreferences(this, database);
        mail = new MailService(this, database);
        afk = new AfkService(this);
        backups = new BackupService(this, database);
        powerTools = new PowertoolService(this);
        unlimited = new UnlimitedService(this);
        worth = new WorthManager(this);
        playtime = new PlaytimeService(this);

        jails = new JailManager(this);
        jails.load();

        // --- wiring ---------------------------------------------------------
        registerCommands();
        registerListeners();
        startSaveTask();
        startAfkTask();
        startJailTask();

        getLogger().info("Better_Admin_Commands enabled with " + warps.size() + " warp(s).");
    }

    @Override
    public void onDisable() {
        if (saveTask != null) {
            saveTask.cancel();
        }
        if (afkTask != null) {
            afkTask.cancel();
        }
        if (jailTask != null) {
            jailTask.cancel();
        }
        if (economy != null) {
            economy.saveBlocking();
        }
        if (database != null) {
            database.close();
        }
        getLogger().info("Better_Admin_Commands disabled.");
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

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new Join_Listener(this), this);
        getServer().getPluginManager().registerEvents(new Quit_Listener(this), this);
        getServer().getPluginManager().registerEvents(new Chat_Listener(this), this);
        getServer().getPluginManager().registerEvents(new Respawn_Listener(this), this);
        getServer().getPluginManager().registerEvents(new Activity_Listener(this), this);
        getServer().getPluginManager().registerEvents(new Powertool_Listener(this), this);
        getServer().getPluginManager().registerEvents(new Unlimited_Listener(this), this);
        getServer().getPluginManager().registerEvents(new Disposal_Command.Disposal_Listener(), this);
        jailListener = new Jail_Listener(this);
        getServer().getPluginManager().registerEvents(jailListener, this);
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
        register("tpaccept", new Tpaccept_Command(this));
        register("tpdeny", new Tpdeny_Command(this));
        register("warp", new Warp_Command(this));
        register("setwarp", new Setwarp_Command(this));
        register("delwarp", new Delwarp_Command(this));
        register("warps", new Warps_Command(this));

        register("nick", new Nick_Command(this));
        register("hat", new Hat_Command());
        register("craft", new Craft_Command());
        register("enderchest", new Enderchest_Command());
        register("invsee", new Invsee_Command());
        register("sudo", new Sudo_Command());
        register("exp", new Exp_Command());
        register("time", new Time_Command());
        register("weather", new Weather_Command());
        register("ptime", new Ptime_Command());
        register("pweather", new Pweather_Command());
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
        register("betteradmincommands", new Plugin_Command(this));
    }

    private void register(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command '" + name + "' is missing from plugin.yml.");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void register(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command '" + name + "' is missing from plugin.yml.");
            return;
        }
        command.setExecutor(executor);
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
}
