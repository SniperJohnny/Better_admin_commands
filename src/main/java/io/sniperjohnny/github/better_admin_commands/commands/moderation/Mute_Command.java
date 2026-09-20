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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Mutes a player for a duration such as 30m, 2h or 7d. Without a duration the
 * configured default is used, {@code perm} mutes permanently.
 */
public class Mute_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Mute_Command(Better_Admin_Commands plugin) {
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

        long seconds = plugin.getConfig().getLong("moderation.default-mute-seconds", -1L);
        int reasonStart = 1;
        if (args.length >= 2) {
            Long parsed = Targets.parseDuration(args[1]);
            if (parsed != null) {
                seconds = parsed;
                reasonStart = 2;
            }
        }
        String reason = args.length > reasonStart
                ? String.join(" ", Arrays.copyOfRange(args, reasonStart, args.length))
                : "No reason given";

        long until = seconds < 0 ? -1L : System.currentTimeMillis() + (seconds * 1000L);
        plugin.mutes().mute(target.getUniqueId(), until, reason);

        Msg.success(sender, target.getName() + " was muted"
                + (seconds < 0 ? " permanently." : " for " + Targets.formatDuration(seconds) + "."));
        if (target.isOnline() && target.getPlayer() != null) {
            Msg.send(target.getPlayer(), "&cYou were muted. &7Reason: &f" + reason);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.complete(args[0]);
        }
        if (args.length == 2) {
            return Targets.completeFrom(args[1], "5m", "30m", "1h", "1d", "7d", "perm");
        }
        return Collections.emptyList();
    }
}
