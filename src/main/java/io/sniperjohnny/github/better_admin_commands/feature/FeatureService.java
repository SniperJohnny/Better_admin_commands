package io.sniperjohnny.github.better_admin_commands.feature;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import org.bukkit.command.PluginCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The switchboard behind the {@code modules} section of {@code config.yml}.
 *
 * <p>Every command of the plugin belongs to exactly one feature module, so a
 * server owner can switch whole areas off - the auction house, the shop, trade,
 * the jail, the social commands, and so on - without taking the plugin apart.
 * {@code modules.disabled-commands} switches individual commands off on top of
 * that, for the cases where only one command out of a module should be gone.</p>
 *
 * <p>A module that is off keeps its commands registered (they are declared in
 * {@code plugin.yml}, and a command that suddenly does not exist confuses more
 * than it helps) but answers with a short note naming the option that switched
 * it off. The listeners and repeating tasks of a module are not registered at
 * all, so a switched-off feature costs nothing at runtime.</p>
 *
 * <p>Four modules - {@code economy}, {@code trade}, {@code auction} and
 * {@code shop} - grew their own {@code enabled:} switch before modules existed.
 * Those keys still work and are honoured: such a module runs only while
 * <em>both</em> switches are on, so an old config that turned the shop off stays
 * off after updating.</p>
 */
public final class FeatureService {

    /** Config path of the whole section. */
    public static final String PATH = "modules";

    /** One feature module: its id, the name shown to players and the commands it owns. */
    public record Module(String id, String display, List<String> commands) {
    }

    /** Every module, in the order they are shown by {@code /betteradmincommands modules}. */
    private static final List<Module> DEFINITIONS = List.of(
            new Module("admin", "Administration", List.of(
                    "enchant", "gm", "fly", "smite", "heal", "feed", "god", "speed", "repair", "vanish",
                    "near", "give", "clear", "broadcast", "hat", "craft", "enderchest", "invsee", "sudo",
                    "exp", "time", "weather", "ptime", "pweather", "world", "break", "tree", "bigtree",
                    "spawner", "spawnmob", "nuke", "fireball", "potion", "burn", "ext", "recipe",
                    "killall", "butcher", "remove")),
            new Module("moderation", "Moderation", List.of(
                    "kick", "ban", "tempban", "ipban", "unban", "unbanip", "kickall", "banlist",
                    "mute", "unmute", "kill", "suicide")),
            new Module("economy", "Economy", List.of(
                    "balance", "baltop", "pay", "eco", "worth", "sell")),
            new Module("trade", "Trading", List.of("trade")),
            new Module("auction", "Auction house", List.of("ah")),
            new Module("shop", "Shop", List.of("shop")),
            new Module("report", "Reports", List.of("report", "reports")),
            new Module("teleport", "Teleporting", List.of(
                    "back", "tp", "tppos", "tphere", "tpall", "tpa", "tpahere", "tpaaccept", "tpadeny",
                    "tptoggle", "tpaall", "rtp", "jump", "top", "bottom", "descend")),
            new Module("spawn", "Spawn", List.of("spawn", "setspawn")),
            new Module("homes", "Homes", List.of("home", "sethome", "delhome", "homes")),
            new Module("warps", "Warps", List.of("warp", "setwarp", "delwarp", "warps")),
            new Module("social", "Social", List.of(
                    "msg", "reply", "socialspy", "ignore", "ignorelist", "me", "mail")),
            new Module("info", "Information", List.of(
                    "whois", "seen", "list", "ping", "gc", "depth", "getpos", "playtime", "realname",
                    "reveal", "motd", "rules", "notify")),
            new Module("items", "Items", List.of(
                    "more", "rename", "lore", "skull", "book", "condense", "stack", "sort", "unlimited",
                    "disposal", "powertool")),
            new Module("jail", "Jail", List.of(
                    "jail", "setjail", "deljail", "jails", "unjail", "togglejail")),
            new Module("kits", "Kits", List.of("kit")),
            new Module("nick", "Nicknames", List.of("nick")),
            new Module("skin", "Skins", List.of("skinchange")),
            new Module("afk", "AFK", List.of("afk")));

    /**
     * The pre-existing {@code enabled:} switch of a module, when it has one. A
     * module with a legacy switch only runs while both are on.
     */
    private static final Map<String, String> LEGACY = Map.of(
            "economy", "economy.enabled",
            "trade", "trade.enabled",
            "auction", "auction.enabled",
            "shop", "shop.enabled");

    private final Better_Admin_Commands plugin;
    private final Map<String, Module> byId = new LinkedHashMap<>();
    private final Map<String, Module> byCommand = new LinkedHashMap<>();

    public FeatureService(Better_Admin_Commands plugin) {
        this.plugin = plugin;
        for (Module module : DEFINITIONS) {
            byId.put(module.id(), module);
            for (String command : module.commands()) {
                byCommand.put(command.toLowerCase(Locale.ROOT), module);
                // Aliases answer to the same command, so they follow their module
                // - /money disappears together with /balance.
                PluginCommand declared = plugin.getCommand(command);
                if (declared == null) {
                    continue;
                }
                for (String alias : declared.getAliases()) {
                    byCommand.putIfAbsent(alias.toLowerCase(Locale.ROOT), module);
                }
            }
        }
    }

    /* ------------------------------------------------------------ queries --- */

    /** Every module, in a stable order. */
    public List<Module> modules() {
        return new ArrayList<>(byId.values());
    }

    /** All module ids, used for tab completion. */
    public List<String> moduleIds() {
        return new ArrayList<>(byId.keySet());
    }

    /** The module with that id, or {@code null} when there is no such module. */
    public Module module(String id) {
        return id == null ? null : byId.get(id.toLowerCase(Locale.ROOT));
    }

    /** The module a command (or one of its aliases) belongs to, or {@code null}. */
    public String moduleOf(String command) {
        if (command == null) {
            return null;
        }
        Module module = byCommand.get(command.toLowerCase(Locale.ROOT));
        return module == null ? null : module.id();
    }

    /** The commands of a module, or an empty list when the id is unknown. */
    public List<String> commandsOf(String id) {
        Module module = module(id);
        return module == null ? List.of() : module.commands();
    }

    /** The name of a module shown to players, falling back to the raw id. */
    public String display(String id) {
        Module module = module(id);
        return module == null ? String.valueOf(id) : module.display();
    }

    /** Whether a module is switched on right now, legacy switch included. */
    public boolean enabled(String id) {
        if (!plugin.getConfig().getBoolean(PATH + "." + id + ".enabled", true)) {
            return false;
        }
        String legacy = LEGACY.get(id);
        return legacy == null || plugin.getConfig().getBoolean(legacy, true);
    }

    /** The commands switched off one by one through {@code modules.disabled-commands}. */
    public Set<String> disabledCommands() {
        Set<String> names = new LinkedHashSet<>();
        for (String entry : plugin.getConfig().getStringList(PATH + ".disabled-commands")) {
            if (entry != null && !entry.isBlank()) {
                names.add(entry.trim().toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    /** Whether one command may run: its module is on and it is not switched off on its own. */
    public boolean commandEnabled(String command) {
        String id = moduleOf(command);
        if (id != null && !enabled(id)) {
            return false;
        }
        return !disabledCommands().contains(String.valueOf(command).toLowerCase(Locale.ROOT));
    }

    /**
     * Explains why a command does not run, or {@code null} when it does. Used as
     * the answer of a command whose feature was switched off.
     */
    public String blockReason(String command) {
        String name = command == null ? "" : command.toLowerCase(Locale.ROOT);
        if (disabledCommands().contains(name)) {
            return "The command &f/" + name + "&c is switched off in config.yml"
                    + " &8(" + PATH + ".disabled-commands&8).";
        }
        String id = moduleOf(name);
        if (id == null) {
            return "&cThat command is switched off in config.yml.";
        }
        String legacy = LEGACY.get(id);
        if (legacy != null && !plugin.getConfig().getBoolean(legacy, true)) {
            return "The &f" + display(id) + "&c feature is switched off in config.yml"
                    + " &8(" + legacy + "&8).";
        }
        return "The &f" + display(id) + "&c feature is switched off in config.yml"
                + " &8(" + PATH + "." + id + ".enabled&8).";
    }

    /** One line per module: id, state and command count. Used by {@code /betteradmincommands modules}. */
    public List<String> describeAll() {
        List<String> lines = new ArrayList<>();
        for (Module module : byId.values()) {
            lines.add((enabled(module.id()) ? " &aON  &7" : " &cOFF &7")
                    + module.id() + " &8(" + module.display() + ", "
                    + module.commands().size() + " command(s))");
        }
        return Collections.unmodifiableList(lines);
    }
}
