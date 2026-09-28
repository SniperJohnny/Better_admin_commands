package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Nametags_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Nametags_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        boolean hidden = plugin.preferences().hideNameTags();
        if (args.length < 1) {
            Msg.usage(sender, command);
            status(sender, hidden);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "enable", "on", "show", "shown" -> set(sender, false);
            case "disable", "off", "hide", "hidden" -> set(sender, true);
            case "toggle" -> set(sender, !hidden);
            case "status" -> status(sender, hidden);
            default -> {
                Msg.error(sender, "Unknown option &f" + args[0]
                        + "&c - use &fenable&c, &fdisable&c or &ftoggle&c.");
                Msg.usage(sender, command);
            }
        }
        return true;
    }

    private void set(CommandSender sender, boolean hidden) {
        plugin.preferences().setNameTagsHidden(hidden);
        Msg.success(sender, hidden
                ? "Name tags are now hidden for every player."
                : "Name tags are now shown above every player.");
    }

    private void status(CommandSender sender, boolean hidden) {
        Msg.send(sender, hidden
                ? "&7Name tags are currently &chidden&7 for every player."
                : "&7Name tags are currently &ashown&7 above every player.");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> options = List.of("enable", "disable", "toggle", "status");
        List<String> matches = new java.util.ArrayList<>();
        for (String option : options) {
            if (option.startsWith(prefix)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
