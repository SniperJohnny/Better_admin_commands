package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * World management and teleporting:
 * {@code /world [name]}, {@code /world list}, {@code /world tp <name>},
 * {@code /world create <name> [type]}, {@code /world load <name>},
 * {@code /world unload <name>} and {@code /world delete <name> confirm}.
 */
public class World_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public World_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 0) {
            list(sender);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "list" -> list(sender);
            case "tp", "teleport", "go" -> {
                if (args.length < 2) {
                    Msg.usage(sender, command);
                    return true;
                }
                teleport(sender, args[1]);
            }
            case "create" -> create(sender, args);
            case "load" -> load(sender, args);
            case "unload" -> unload(sender, args);
            case "delete", "remove" -> delete(sender, args);
            default -> teleport(sender, args[0]);
        }
        return true;
    }

    private void list(CommandSender sender) {
        Msg.raw(sender, "&6Worlds &7(" + Bukkit.getWorlds().size() + ")&6:");
        for (World world : Bukkit.getWorlds()) {
            Msg.raw(sender, String.format(Locale.ROOT, " &8- &f%s &7(%s, %d players, seed %d)",
                    world.getName(), world.getEnvironment().name().toLowerCase(Locale.ROOT),
                    world.getPlayers().size(), world.getSeed()));
        }
        Msg.raw(sender, "&7Subcommands: &flist&7, &ftp <name>&7, &fcreate&7, &fload&7, &funload&7, &fdelete");
    }

    private void teleport(CommandSender sender, String name) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return;
        }
        World world = Bukkit.getWorld(name);
        if (world == null) {
            Msg.error(sender, "There is no world called " + name + ".");
            return;
        }
        plugin.teleports().requestTeleport(player, world.getSpawnLocation());
        Msg.success(player, "Teleported to world " + world.getName() + ".");
    }

    private void create(CommandSender sender, String[] args) {
        if (!sender.hasPermission("betteradmincommands.world.manage")) {
            Msg.noPermission(sender);
            return;
        }
        if (args.length < 2) {
            Msg.error(sender, "Usage: /world create <name> [normal|nether|the_end] [<WorldType>]");
            return;
        }
        String name = args[1];
        if (Bukkit.getWorld(name) != null) {
            Msg.error(sender, "A world called " + name + " already exists.");
            return;
        }
        WorldCreator creator = new WorldCreator(name);
        if (args.length >= 3) {
            switch (args[2].toLowerCase(Locale.ROOT)) {
                case "nether" -> creator.environment(World.Environment.NETHER);
                case "the_end", "end" -> creator.environment(World.Environment.THE_END);
                default -> creator.environment(World.Environment.NORMAL);
            }
        }
        if (args.length >= 4) {
            try {
                creator.type(WorldType.valueOf(args[3].toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                Msg.error(sender, "Unknown world type " + args[3] + ".");
                return;
            }
        }
        World world = creator.createWorld();
        if (world == null) {
            Msg.error(sender, "The world could not be created.");
            return;
        }
        Msg.success(sender, "World " + world.getName() + " created.");
    }

    private void load(CommandSender sender, String[] args) {
        if (!sender.hasPermission("betteradmincommands.world.manage")) {
            Msg.noPermission(sender);
            return;
        }
        if (args.length < 2) {
            Msg.error(sender, "Usage: /world load <name>");
            return;
        }
        String name = args[1];
        if (Bukkit.getWorld(name) != null) {
            Msg.error(sender, "World " + name + " is already loaded.");
            return;
        }
        World world = new WorldCreator(name).createWorld();
        if (world == null) {
            Msg.error(sender, "The world " + name + " does not exist on disk.");
            return;
        }
        Msg.success(sender, "World " + world.getName() + " loaded.");
    }

    private void unload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("betteradmincommands.world.manage")) {
            Msg.noPermission(sender);
            return;
        }
        if (args.length < 2) {
            Msg.error(sender, "Usage: /world unload <name>");
            return;
        }
        World world = Bukkit.getWorld(args[1]);
        if (world == null) {
            Msg.error(sender, "There is no world called " + args[1] + ".");
            return;
        }
        if (!world.getPlayers().isEmpty()) {
            Msg.error(sender, "There are still players in that world.");
            return;
        }
        if (world.equals(Bukkit.getWorlds().get(0))) {
            Msg.error(sender, "The main world cannot be unloaded.");
            return;
        }
        Bukkit.unloadWorld(world, true);
        Msg.success(sender, "World " + args[1] + " unloaded and saved.");
    }

    private void delete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("betteradmincommands.world.manage")) {
            Msg.noPermission(sender);
            return;
        }
        if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
            Msg.error(sender, "This deletes the world folder forever. Confirm with: /world delete <name> confirm");
            return;
        }
        World world = Bukkit.getWorld(args[1]);
        if (world != null) {
            if (!world.getPlayers().isEmpty()) {
                Msg.error(sender, "There are still players in that world.");
                return;
            }
            if (world.equals(Bukkit.getWorlds().get(0))) {
                Msg.error(sender, "The main world cannot be deleted.");
                return;
            }
            Bukkit.unloadWorld(world, false);
        }
        Path folder = new File(Bukkit.getWorldContainer(), args[1]).toPath();
        if (!Files.exists(folder)) {
            Msg.error(sender, "No world folder called " + args[1] + " was found.");
            return;
        }
        try (Stream<Path> paths = Files.walk(folder)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // best effort
                }
            });
        } catch (IOException e) {
            Msg.error(sender, "The world folder could not be deleted: " + e.getMessage());
            return;
        }
        Msg.success(sender, "World " + args[1] + " was deleted.");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("list", "tp", "create", "load", "unload", "delete"));
            for (World world : Bukkit.getWorlds()) {
                options.add(world.getName());
            }
            return Targets.completeFrom(args[0], options);
        }
        if (args.length == 2) {
            String action = args[0].toLowerCase(Locale.ROOT);
            if (action.equals("create")) {
                return Collections.emptyList();
            }
            List<String> worlds = new ArrayList<>();
            Bukkit.getWorlds().forEach(world -> worlds.add(world.getName()));
            return Targets.completeFrom(args[1], worlds);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return Targets.completeFrom(args[2], "normal", "nether", "the_end");
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("remove"))) {
            return Targets.completeFrom(args[2], "confirm");
        }
        return Collections.emptyList();
    }
}
