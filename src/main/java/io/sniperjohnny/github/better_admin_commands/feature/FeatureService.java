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

public final class FeatureService {

    public static final String PATH = "modules";

    public record Module(String id, String display, List<String> commands) {
    }

    private static final List<Module> DEFINITIONS = List.of(
            new Module("admin", "Administration", List.of(
                    "enchant", "gm", "fly", "smite", "heal", "feed", "god", "speed", "repair", "vanish",
                    "near", "give", "clear", "broadcast", "hat", "craft", "enderchest", "ecsee", "invsee", "sudo",
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
            new Module("nick", "Nicknames", List.of("nick", "nametags")),
            new Module("skin", "Skins", List.of("skinchange")),
            new Module("afk", "AFK", List.of("afk")));

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

    public List<Module> modules() {
        return new ArrayList<>(byId.values());
    }

    public List<String> moduleIds() {
        return new ArrayList<>(byId.keySet());
    }

    public Module module(String id) {
        return id == null ? null : byId.get(id.toLowerCase(Locale.ROOT));
    }

    public String moduleOf(String command) {
        if (command == null) {
            return null;
        }
        Module module = byCommand.get(command.toLowerCase(Locale.ROOT));
        return module == null ? null : module.id();
    }

    public List<String> commandsOf(String id) {
        Module module = module(id);
        return module == null ? List.of() : module.commands();
    }

    public String display(String id) {
        Module module = module(id);
        return module == null ? String.valueOf(id) : module.display();
    }

    public boolean enabled(String id) {
        if (!plugin.getConfig().getBoolean(PATH + "." + id + ".enabled", true)) {
            return false;
        }
        String legacy = LEGACY.get(id);
        return legacy == null || plugin.getConfig().getBoolean(legacy, true);
    }

    public Set<String> disabledCommands() {
        Set<String> names = new LinkedHashSet<>();
        for (String entry : plugin.getConfig().getStringList(PATH + ".disabled-commands")) {
            if (entry != null && !entry.isBlank()) {
                names.add(entry.trim().toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    public boolean commandEnabled(String command) {
        String id = moduleOf(command);
        if (id != null && !enabled(id)) {
            return false;
        }
        return !disabledCommands().contains(String.valueOf(command).toLowerCase(Locale.ROOT));
    }

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
