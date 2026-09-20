package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Changes the weather of a world. */
public class Weather_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }
        String type = args[0].toLowerCase(Locale.ROOT);
        if (!type.equals("sun") && !type.equals("clear") && !type.equals("rain")
                && !type.equals("storm") && !type.equals("thunder")) {
            Msg.error(sender, "Use /weather <sun|rain|thunder> [world].");
            return true;
        }

        World world;
        if (args.length >= 2) {
            world = Bukkit.getWorld(args[1]);
            if (world == null) {
                Msg.error(sender, "There is no world called " + args[1] + ".");
                return true;
            }
        } else if (sender instanceof org.bukkit.entity.Player player) {
            world = player.getWorld();
        } else {
            Msg.error(sender, "From the console you have to name a world.");
            return true;
        }

        switch (type) {
            case "sun", "clear" -> {
                world.setStorm(false);
                world.setThundering(false);
            }
            case "rain", "storm" -> {
                world.setStorm(true);
                world.setThundering(false);
            }
            default -> {
                world.setStorm(true);
                world.setThundering(true);
            }
        }
        Msg.success(sender, "Weather in " + world.getName() + " is now " + type + ".");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], "sun", "rain", "thunder");
        }
        if (args.length == 2) {
            List<String> worlds = new ArrayList<>();
            Bukkit.getWorlds().forEach(world -> worlds.add(world.getName()));
            return Targets.completeFrom(args[1], worlds);
        }
        return Collections.emptyList();
    }
}
