package io.sniperjohnny.github.better_admin_commands.commands.teleport;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
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

/** Teleports the sender to a named warp. */
public class Warp_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Warp_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }
        Location warp = plugin.warps().get(args[0]);
        if (warp == null) {
            Msg.error(player, "There is no warp called " + args[0] + ".");
            return true;
        }
        if (!player.hasPermission("betteradmincommands.warp." + args[0].toLowerCase())) {
            Msg.noPermission(player);
            return true;
        }
        plugin.teleports().requestTeleport(player, warp);
        Msg.success(player, "Teleported to warp " + args[0] + ".");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], plugin.warps().names());
        }
        return Collections.emptyList();
    }
}
