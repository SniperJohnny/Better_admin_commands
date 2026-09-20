package io.sniperjohnny.github.better_admin_commands.commands.home;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Teleports the sender to one of their homes. */
public class Home_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Home_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        String name = args.length >= 1 ? args[0].toLowerCase() : "home";
        Location home = plugin.homes().get(player.getUniqueId(), name);
        if (home == null) {
            if (args.length == 0 && plugin.homes().homesOf(player.getUniqueId()).isEmpty()) {
                Msg.error(player, "You have no homes yet. Use /sethome to create one.");
            } else {
                Msg.error(player, "You do not have a home called " + name + ".");
            }
            return true;
        }
        plugin.teleports().requestTeleport(player, home);
        Msg.success(player, "Teleported to your home " + name + ".");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            return Targets.completeFrom(args[0], plugin.homes().homesOf(player.getUniqueId()).keySet());
        }
        return Collections.emptyList();
    }
}
