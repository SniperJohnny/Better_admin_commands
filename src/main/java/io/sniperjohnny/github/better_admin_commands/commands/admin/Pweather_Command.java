package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.WeatherType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Sets a personal client side weather for the sender. */
public class Pweather_Command implements TabExecutor {

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
        switch (value) {
            case "reset", "off", "server" -> {
                player.resetPlayerWeather();
                Msg.success(player, "Your personal weather was reset to the server weather.");
            }
            case "sun", "clear" -> {
                player.setPlayerWeather(WeatherType.CLEAR);
                Msg.success(player, "You now always see clear weather.");
            }
            case "rain", "storm", "downfall" -> {
                player.setPlayerWeather(WeatherType.DOWNFALL);
                Msg.success(player, "You now always see rain.");
            }
            default -> Msg.error(player, "Use /pweather <reset|sun|rain>.");
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
