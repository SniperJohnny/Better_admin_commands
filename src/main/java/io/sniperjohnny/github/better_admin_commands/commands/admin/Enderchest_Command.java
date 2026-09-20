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

/** Opens an ender chest. */
public class Enderchest_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (args.length < 1) {
            player.openInventory(player.getEnderChest());
            return true;
        }
        if (!player.hasPermission("betteradmincommands.enderchest.others")) {
            Msg.noPermission(player);
            return true;
        }
        Player target = Targets.online(sender, args[0]);
        if (target == null) {
            return true;
        }
        try {
            player.openInventory(target.getEnderChest());
            Msg.send(player, "&7Viewing the ender chest of &f" + target.getName() + "&7.");
        } catch (Exception e) {
            Msg.error(player, "The ender chest of " + target.getName() + " cannot be opened from here.");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1 && sender.hasPermission("betteradmincommands.enderchest.others")) {
            return Targets.complete(args[0]);
        }
        return Collections.emptyList();
    }
}
