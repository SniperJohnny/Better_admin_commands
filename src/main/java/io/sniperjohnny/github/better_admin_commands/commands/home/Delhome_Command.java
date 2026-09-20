package io.sniperjohnny.github.better_admin_commands.commands.home;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Deletes one of the player's homes. */
public class Delhome_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Delhome_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }
        if (plugin.homes().delete(player.getUniqueId(), args[0])) {
            Msg.success(player, "Home " + args[0] + " deleted.");
        } else {
            Msg.error(player, "You do not have a home called " + args[0] + ".");
        }
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
