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

/** Clears the inventory of a player. */
public class Clear_Command implements TabExecutor {

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

        target.getInventory().clear();
        Msg.success(sender, target.equals(sender) ? "Your inventory was cleared."
                : "Cleared the inventory of " + target.getName() + ".");
        if (!target.equals(sender)) {
            Msg.send(target, "&7Your inventory was cleared by &f" + sender.getName() + "&7.");
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
