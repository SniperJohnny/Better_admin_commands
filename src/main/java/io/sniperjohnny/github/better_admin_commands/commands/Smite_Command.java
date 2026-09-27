package io.sniperjohnny.github.better_admin_commands.commands;

import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class Smite_Command implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        if(!(sender instanceof final Player p))
        {
         sender.sendMessage("You need to be an Player to use this Command.");

        return true;

        }
        if(args.length < 1) {
            p.getWorld().strikeLightning(p.getLocation());
            return true;
        }
        Player target = Targets.online(p, args[0]);
        if (target == null) {
            return true;
        }
        target.getWorld().strikeLightning(target.getLocation());
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return Targets.complete(args[0]);
        }
        return Collections.emptyList();
    }
}
