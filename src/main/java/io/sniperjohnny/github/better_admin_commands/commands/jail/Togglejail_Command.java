package io.sniperjohnny.github.better_admin_commands.commands.jail;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Jails a player if they are free, releases them otherwise. */
public class Togglejail_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Togglejail_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }
        OfflinePlayer target = Targets.offline(sender, args[0]);
        if (target == null) {
            return true;
        }
        String name = target.getName() == null ? args[0] : target.getName();

        if (plugin.jails().isJailed(target.getUniqueId())) {
            plugin.jails().unjail(target.getUniqueId());
            Player online = target.getPlayer();
            if (online != null) {
                Msg.send(online, "&aYou were released from jail.");
            }
            Msg.success(sender, name + " was released.");
            return true;
        }

        if (plugin.jails().size() == 0) {
            Msg.error(sender, "There is no jail yet. Create one with /setjail <name>.");
            return true;
        }
        String cell = args.length >= 2 ? args[1].toLowerCase() : plugin.jails().names().iterator().next();
        if (!plugin.jails().exists(cell)) {
            Msg.error(sender, "There is no jail called " + cell + ".");
            return true;
        }
        plugin.jails().jail(target.getUniqueId(), cell, -1L);
        Player online = target.getPlayer();
        if (online != null) {
            plugin.jails().sendToCell(online);
            Msg.send(online, "&cYou were jailed.");
        }
        Msg.success(sender, name + " was jailed in " + cell + ".");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.complete(args[0]);
        }
        if (args.length == 2) {
            return Targets.completeFrom(args[1], plugin.jails().names());
        }
        return Collections.emptyList();
    }
}
