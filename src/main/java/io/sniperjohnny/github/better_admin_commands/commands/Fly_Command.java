package io.sniperjohnny.github.better_admin_commands.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Fly_Command implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Du kannst das nicht machen");
            return true;
        }

        final Player p = (Player) sender;
        if (!p.getAllowFlight()) {
            p.setAllowFlight(true);
            p.setFlying(true);
            p.sendMessage(ChatColor.GREEN + "Du fliegst jetzt");
        } else {
            p.setAllowFlight(false);
            p.setFlying(false);
            p.sendMessage(ChatColor.DARK_RED + "Du kannst jetzt nichtmehr Fliegen");
        }






        return true;
    }
}

