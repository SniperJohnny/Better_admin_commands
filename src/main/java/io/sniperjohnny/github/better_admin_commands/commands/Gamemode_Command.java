package io.sniperjohnny.github.better_admin_commands.commands;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Gamemode_Command implements TabExecutor {

    public static final String ALL = "betteradmincommands.gamemode";

    public static final String OTHERS = ALL + ".others";

    public static final String MODE_PREFIX = ALL + ".";

    private static final List<String> CHOICES = List.of(
            "0", "survival", "s",
            "1", "creative", "c",
            "2", "adventure", "a",
            "3", "spectator", "sp");

    private final Better_Admin_Commands plugin;

    public Gamemode_Command(Better_Admin_Commands plugin) {
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
            Msg.usage(sender, command);
            return true;
        }

        GameMode mode = parse(args[0]);
        if (mode == null) {
            Msg.error(sender, "Unknown gamemode &f" + args[0]
                    + "&c - use a number &f0&7-&f3&c or a name such as &fcreative&c.");
            return true;
        }

        // The general node allows every mode; otherwise only the node that
        // belongs to the mode that was asked for.
        if (!plugin.permissions().has(sender, ALL)
                && !plugin.permissions().has(sender, MODE_PREFIX + key(mode))) {
            Msg.noPermission(sender);
            return true;
        }

        if (args.length < 2) {
            player.setGameMode(mode);
            Msg.success(sender, "Your gamemode is now &f" + display(mode) + "&a.");
            return true;
        }

        // Changing someone else is a step further than changing yourself, so a
        // per-mode node alone is not enough for it.
        if (!plugin.permissions().has(sender, ALL)
                && !plugin.permissions().has(sender, OTHERS)) {
            Msg.noPermission(sender);
            return true;
        }
        Player target = Targets.online(sender, args[1]);
        if (target == null) {
            return true;
        }
        target.setGameMode(mode);
        Msg.success(sender, "Set the gamemode of &f" + Targets.displayName(target)
                + "&a to &f" + display(mode) + "&a.");
        if (!target.equals(player)) {
            Msg.send(target, "&7Your gamemode was set to &f" + display(mode) + "&7.");
        }
        return true;
    }

    private static GameMode parse(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "0", "survival", "s" -> GameMode.SURVIVAL;
            case "1", "creative", "c" -> GameMode.CREATIVE;
            case "2", "adventure", "a" -> GameMode.ADVENTURE;
            case "3", "spectator", "sp" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    public static String key(GameMode mode) {
        return mode.name().toLowerCase(Locale.ROOT);
    }

    private static String display(GameMode mode) {
        return key(mode);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> completions = new ArrayList<>();
            for (String choice : CHOICES) {
                if (choice.startsWith(prefix) && mayUse(sender, choice)) {
                    completions.add(choice);
                }
            }
            return completions;
        }
        if (args.length == 2) {
            if (!plugin.permissions().has(sender, ALL)
                    && !plugin.permissions().has(sender, OTHERS)) {
                return Collections.emptyList();
            }
            return Targets.complete(args[1]);
        }
        return Collections.emptyList();
    }

    private boolean mayUse(CommandSender sender, String choice) {
        if (plugin.permissions().has(sender, ALL)) {
            return true;
        }
        GameMode mode = parse(choice);
        return mode != null && plugin.permissions().has(sender, MODE_PREFIX + key(mode));
    }
}
