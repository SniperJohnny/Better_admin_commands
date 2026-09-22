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
import java.util.Map;

/** Sets or advances the time of a world. */
public class Time_Command implements TabExecutor {

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
        if (args.length < 2) {
            Msg.usage(sender, command);
            return true;
        }
        boolean add = args[0].equalsIgnoreCase("add");
        if (!add && !args[0].equalsIgnoreCase("set")) {
            Msg.usage(sender, command);
            return true;
        }

        Long ticks = PRESETS.get(args[1].toLowerCase(Locale.ROOT));
        if (ticks == null) {
            Long parsed = parseTicks(args[1]);
            if (parsed == null) {
                Msg.error(sender, "Use a preset (day, noon, sunset, night, midnight, sunrise) or a tick value.");
                return true;
            }
            ticks = parsed;
        }

        World world;
        if (args.length >= 3) {
            world = Bukkit.getWorld(args[2]);
            if (world == null) {
                Msg.error(sender, "There is no world called " + args[2] + ".");
                return true;
            }
        } else if (sender instanceof org.bukkit.entity.Player player) {
            world = player.getWorld();
        } else {
            Msg.error(sender, "From the console you have to name a world.");
            return true;
        }

        long time = add ? world.getTime() + ticks : ticks;
        world.setTime(time);
        Msg.success(sender, (add ? "Added " : "Set ") + ticks + " ticks in " + world.getName()
                + ". It is now tick " + (time % 24000L) + ".");
        return true;
    }

    public static Long parseTicks(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], "set", "add");
        }
        if (args.length == 2) {
            return Targets.completeFrom(args[1], PRESETS.keySet());
        }
        if (args.length == 3) {
            List<String> worlds = new ArrayList<>();
            Bukkit.getWorlds().forEach(world -> worlds.add(world.getName()));
            return Targets.completeFrom(args[2], worlds);
        }
        return Collections.emptyList();
    }
}
