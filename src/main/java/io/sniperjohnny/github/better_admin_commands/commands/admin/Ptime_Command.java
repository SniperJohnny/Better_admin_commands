package io.sniperjohnny.github.better_admin_commands.commands.admin;

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
import java.util.Locale;
import java.util.Map;

/** Sets a personal client side time for the sender. */
public class Ptime_Command implements TabExecutor {

    private static final Map<String, Long> PRESETS = Map.of(
            "day", 1000L,
            "noon", 6000L,
            "sunset", 12000L,
            "night", 13000L,
            "midnight", 18000L,
            "sunrise", 23000L);

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
        String value = args[0].toLowerCase(Locale.ROOT);
        if (value.equals("reset") || value.equals("off")) {
            player.resetPlayerTime();
            Msg.success(player, "Your personal time was reset to the server time.");
            return true;
        }
        Long ticks = PRESETS.get(value);
        if (ticks == null) {
            ticks = Time_Command.parseTicks(value);
        }
        if (ticks == null) {
            Msg.error(player, "Use /ptime <reset|day|noon|sunset|night|midnight|sunrise> or a tick value.");
            return true;
        }
        player.setPlayerTime(ticks, false);
        Msg.success(player, "Your personal time is now " + value + ".");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], "reset", "day", "noon", "sunset", "night", "midnight", "sunrise");
        }
        return Collections.emptyList();
    }
}
