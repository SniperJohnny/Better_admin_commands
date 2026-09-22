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

import java.util.Collections;
import java.util.List;

/**
 * Sets a personal client side weather for the sender. Without an argument this
 * opens the weather menu instead. Values are applied through
 * {@link PersonalDisplay}, the same code the menu uses.
 */
public class Pweather_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Pweather_Command(Better_Admin_Commands plugin) {
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
            plugin.pweatherGui().open(player);
            return true;
        }
        String value = args[0];
        if (!PersonalDisplay.applyWeather(player, value)) {
            Msg.error(player, "Use /pweather <reset|sun|rain>.");
            return true;
        }
        if (PersonalDisplay.isReset(value)) {
            Msg.success(player, "Your personal weather was reset to the server weather.");
        } else if (value.equalsIgnoreCase("sun")) {
            Msg.success(player, "You now always see clear weather.");
        } else {
            Msg.success(player, "You now always see rain.");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], "reset", "sun", "rain");
        }
        return Collections.emptyList();
    }
}
