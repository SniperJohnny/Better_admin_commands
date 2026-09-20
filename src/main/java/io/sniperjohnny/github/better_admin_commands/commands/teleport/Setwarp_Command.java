package io.sniperjohnny.github.better_admin_commands.commands.teleport;

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

/** Creates or moves a warp to the current position. */
public class Setwarp_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Setwarp_Command(Better_Admin_Commands plugin) {
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
        String name = args[0].toLowerCase();
        if (!name.matches("[a-z0-9_\\-]{1,32}")) {
            Msg.error(player, "Warp names may only contain letters, numbers, '_' and '-'.");
            return true;
        }
        boolean existed = plugin.warps().exists(name);
        plugin.warps().set(name, player.getLocation());
        Msg.success(player, existed ? "Warp " + name + " was moved to your position." : "Warp " + name + " created.");
        Msg.send(player, "&7Players need the permission &fbetteradmincommands.warp." + name + "&7 to use it.");
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
