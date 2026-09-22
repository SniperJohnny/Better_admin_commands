package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.PersonalDisplay;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sets a personal client side time for the sender. Without an argument this
 * opens the time menu instead.
 *
 * <p>The accepted values and the way they are applied live in
 * {@link PersonalDisplay}, which the menu uses as well, so the two cannot
 * disagree about what a value means.</p>
 */
public class Ptime_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Ptime_Command(Better_Admin_Commands plugin) {
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
            plugin.ptimeGui().open(player);
            return true;
        }
        String value = args[0];
        if (!PersonalDisplay.applyTime(player, value)) {
            Msg.error(player, "Use /ptime <reset|day|noon|sunset|night|midnight|sunrise> or a tick value.");
            return true;
        }
        if (PersonalDisplay.isReset(value)) {
            Msg.success(player, "Your personal time was reset to the server time.");
        } else {
            Msg.success(player, "Your personal time is now " + value + ".");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            List<String> values = new ArrayList<>();
            values.add("reset");
            for (PersonalDisplay.TimePreset preset : PersonalDisplay.TIME_PRESETS) {
                values.add(preset.id());
            }
            return Targets.completeFrom(args[0], values);
        }
        return Collections.emptyList();
    }
}
