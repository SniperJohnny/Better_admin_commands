package io.sniperjohnny.github.better_admin_commands.commands.home;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Stores the current position as one of the player's homes. */
public class Sethome_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Sethome_Command(Better_Admin_Commands plugin) {
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
        if (!name.matches("[a-z0-9_\\-]{1,32}")) {
            Msg.error(player, "Home names may only contain letters, numbers, '_' and '-'.");
            return true;
        }
        if (!plugin.homes().set(player, name, player.getLocation())) {
            Msg.error(player, "You reached your limit of " + plugin.homes().limitFor(player)
                    + " homes. Delete one with /delhome <name>.");
            return true;
        }
        Msg.success(player, "Home " + name + " set to your current position.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Collections.singletonList("home");
        }
        return Collections.emptyList();
    }
}
