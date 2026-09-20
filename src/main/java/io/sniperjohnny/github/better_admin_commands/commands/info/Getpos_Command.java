package io.sniperjohnny.github.better_admin_commands.commands.info;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Shows the position of a player. */
public class Getpos_Command implements TabExecutor {

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
        Location location = target.getLocation();
        Msg.send(sender, String.format(Locale.ROOT,
                "%s&7: &f%s &7| &fx &f%.1f &7y &f%.1f &7z &f%.1f &7| yaw &f%.0f &7pitch &f%.0f",
                target.equals(sender) ? "&7Your position" : "&f" + target.getName() + "&7's position",
                location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch()));

        if (target.hasPermission("betteradmincommands.getpos.others")) {
            Msg.raw(sender, "&8(click to copy: &f" + location.getWorld().getName() + " "
                    + Math.round(location.getX()) + " " + Math.round(location.getY()) + " "
                    + Math.round(location.getZ()) + "&8)");
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
