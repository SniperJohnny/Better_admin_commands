package io.sniperjohnny.github.better_admin_commands.commands.teleport;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Teleports the sender to coordinates, optionally in another world. */
public class Tppos_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Tppos_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (args.length < 3) {
            Msg.usage(sender, command);
            return true;
        }
        Double x = Targets.parseDouble(args[0]);
        Double y = Targets.parseDouble(args[1]);
        Double z = Targets.parseDouble(args[2]);
        if (x == null || y == null || z == null) {
            Msg.error(sender, "The first three arguments have to be numbers.");
            return true;
        }
        World world = player.getWorld();
        if (args.length >= 4) {
            World requested = Bukkit.getWorld(args[3]);
            if (requested == null) {
                Msg.error(sender, "There is no world called " + args[3] + ".");
                return true;
            }
            world = requested;
        }
        Location destination = new Location(world, x, y, z, player.getLocation().getYaw(), player.getLocation().getPitch());
        plugin.teleports().requestTeleport(player, destination);
        Msg.success(player, "Teleported to " + Tp_Command.format(destination) + " in " + world.getName() + ".");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 4) {
            List<String> worlds = new ArrayList<>();
            for (World world : Bukkit.getWorlds()) {
                worlds.add(world.getName());
            }
            return Targets.completeFrom(args[3], worlds);
        }
        return java.util.Collections.emptyList();
    }
}
