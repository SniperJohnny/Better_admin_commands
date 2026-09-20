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

/** Releases a player from jail. */
public class Unjail_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Unjail_Command(Better_Admin_Commands plugin) {
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
        if (!plugin.jails().isJailed(target.getUniqueId())) {
            Msg.error(sender, (target.getName() == null ? args[0] : target.getName()) + " is not jailed.");
            return true;
        }
        plugin.jails().unjail(target.getUniqueId());
        Player online = target.getPlayer();
        if (online != null) {
            Msg.send(online, "&aYou were released from jail.");
        }
        Msg.success(sender, (target.getName() == null ? args[0] : target.getName()) + " was released.");
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
