package io.sniperjohnny.github.better_admin_commands.commands.social;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Lists all players the sender is ignoring. */
public class IgnoreList_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public IgnoreList_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        Set<String> ignored = plugin.preferences().ignored(player.getUniqueId());
        if (ignored.isEmpty()) {
            Msg.send(player, "&7You are not ignoring anyone.");
            return true;
        }
        Msg.raw(player, "&6You are ignoring &7(" + ignored.size() + ")&6:");
        for (String name : ignored) {
            Msg.raw(player, " &8- &f" + name);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
