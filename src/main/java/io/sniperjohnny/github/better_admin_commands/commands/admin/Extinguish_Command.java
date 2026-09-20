package io.sniperjohnny.github.better_admin_commands.commands.admin;

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

/** Puts out a burning player. */
public class Extinguish_Command implements TabExecutor {

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
        target.setFireTicks(0);
        Msg.success(sender, target.equals(sender) ? "You are no longer burning."
                : target.getName() + " is no longer burning.");
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
