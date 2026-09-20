package io.sniperjohnny.github.better_admin_commands.commands.moderation;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Removes a mute from a player. */
public class Unmute_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Unmute_Command(Better_Admin_Commands plugin) {
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
        if (!plugin.mutes().isMuted(target.getUniqueId())) {
            Msg.error(sender, target.getName() + " is not muted.");
            return true;
        }
        plugin.mutes().unmute(target.getUniqueId());
        Msg.success(sender, target.getName() + " was unmuted.");
        if (target.isOnline() && target.getPlayer() != null) {
            Msg.send(target.getPlayer(), "&aYou were unmuted.");
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
