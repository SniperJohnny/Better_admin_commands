package io.sniperjohnny.github.better_admin_commands.commands.items;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
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

/** Toggles placing blocks and consuming items without using them up. */
public class Unlimited_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Unlimited_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        String action = args.length >= 1 ? args[0].toLowerCase(Locale.ROOT) : "toggle";

        if (action.equals("list")) {
            List<String> names = plugin.unlimited().activePlayers();
            if (names.isEmpty()) {
                Msg.send(sender, "&7Nobody has unlimited items right now.");
                return true;
            }
            Msg.raw(sender, "&6Unlimited items enabled for:");
            for (String name : names) {
                Msg.raw(sender, " &8- &f" + name);
            }
            return true;
        }
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (action.equals("clear") || action.equals("off")) {
            plugin.unlimited().clear(player);
            Msg.success(player, "Unlimited items disabled.");
            return true;
        }
        boolean enabled = plugin.unlimited().toggle(player);
        Msg.success(player, enabled
                ? "Unlimited items enabled - placed blocks and eaten items are kept."
                : "Unlimited items disabled.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1 && sender.hasPermission("betteradmincommands.unlimited.list")) {
            return Targets.completeFrom(args[0], "toggle", "list", "clear");
        }
        return Collections.emptyList();
    }
}
