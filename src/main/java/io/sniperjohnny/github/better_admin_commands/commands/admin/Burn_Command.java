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

/** Sets a player on fire. */
public class Burn_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }
        Player target = Targets.online(sender, args[0]);
        if (target == null) {
            return true;
        }
        int seconds = 10;
        if (args.length >= 2) {
            Integer parsed = Targets.parseInt(args[1]);
            if (parsed == null || parsed < 1) {
                Msg.error(sender, "The duration has to be a positive number of seconds.");
                return true;
            }
            seconds = parsed;
        }
        target.setFireTicks(seconds * 20);
        Msg.success(sender, "Set " + target.getName() + " on fire for " + seconds + " seconds.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.complete(args[0]);
        }
        if (args.length == 2) {
            return Targets.completeFrom(args[1], "5", "10", "30");
        }
        return Collections.emptyList();
    }
}
