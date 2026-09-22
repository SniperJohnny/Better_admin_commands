package io.sniperjohnny.github.better_admin_commands.commands.admin;

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

/** Gives a kit defined in config.yml to the sender. */
public class Kit_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Kit_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        // Without a kit name this opens the kit menu, where the kits can be
        // seen with their cooldowns before one is claimed.
        if (args.length < 1) {
            if (!player.hasPermission("betteradmincommands.kit")) {
                Msg.noPermission(player);
                return true;
            }
            plugin.kitGui().open(player, 0);
            return true;
        }
        String name = args[0].toLowerCase();
        if (!plugin.kits().exists(name)) {
            Msg.error(player, "There is no kit called " + name + ".");
            return true;
        }
        // -1 leaves the menu closed; the messages are enough for a command.
        plugin.kitGui().claim(player, name, -1);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], plugin.kits().names());
        }
        return Collections.emptyList();
    }
}
