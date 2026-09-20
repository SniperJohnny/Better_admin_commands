package io.sniperjohnny.github.better_admin_commands.commands.info;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** Shows when a player was last online. */
public class Seen_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Seen_Command(Better_Admin_Commands plugin) {
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
        if (target.isOnline()) {
            Msg.send(sender, "&f" + name + " &7is currently online.");
            return true;
        }
        Long lastSeen = plugin.database().lastSeen(target.getUniqueId());
        if (lastSeen == null || lastSeen <= 0) {
            Msg.send(sender, "&7No activity has been recorded for &f" + name + "&7 yet.");
            return true;
        }
        long minutes = Math.max(1, (System.currentTimeMillis() - lastSeen) / 60000L);
        Msg.send(sender, "&f" + name + " &7was last online &f" + Targets.formatDuration(minutes * 60L)
                + " &7ago &8(" + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(lastSeen)) + ")&7.");
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
