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

/** Toggles vanish for a player. */
public class Vanish_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Vanish_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        Player target;
        if (args.length >= 1) {
            target = Targets.online(sender, args[0]);
            if (target == null) {
                return true;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            Msg.playerOnly(sender);
            return true;
        }

        boolean vanished = plugin.vanish().toggle(target);
        Msg.success(sender, target.equals(sender)
                ? (vanished ? "You are now hidden." : "You are visible again.")
                : target.getName() + (vanished ? " is now hidden." : " is visible again."));
        if (!target.equals(sender)) {
            Msg.send(target, vanished ? "&7You are now hidden." : "&7You are visible again.");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.complete(args[0]);
        }
        return Collections.emptyList();
    }
}
