package io.sniperjohnny.github.better_admin_commands.commands.jail;

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

/** Creates a jail cell at your position. */
public class Setjail_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Setjail_Command(Better_Admin_Commands plugin) {
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
        String name = args[0].toLowerCase();
        if (!name.matches("[a-z0-9_\\-]{1,32}")) {
            Msg.error(player, "Jail names may only contain letters, numbers, '_' and '-'.");
            return true;
        }
        boolean existed = plugin.jails().exists(name);
        plugin.jails().set(name, player.getLocation());
        Msg.success(player, existed ? "Jail " + name + " was moved here." : "Jail " + name + " created.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], plugin.jails().names());
        }
        return Collections.emptyList();
    }
}
