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
        // Without an argument this opens the menu, which shows the current state
        // instead of making the player remember whether the mode is on.
        if (args.length < 1) {
            if (!(sender instanceof Player player)) {
                Msg.playerOnly(sender);
                return true;
            }
            plugin.unlimitedGui().open(player);
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);

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
        switch (action) {
            case "clear", "off" -> {
                plugin.unlimited().clear(player);
                Msg.success(player, "Unlimited items disabled.");
            }
            case "on" -> {
                if (plugin.unlimited().isUnlimited(player)) {
                    Msg.send(player, "&7Unlimited items are already on.");
                    return true;
                }
                plugin.unlimited().toggle(player);
                Msg.success(player, "Unlimited items enabled - placed blocks and eaten items are kept.");
            }
            case "toggle" -> {
                boolean enabled = plugin.unlimited().toggle(player);
                Msg.success(player, enabled
                        ? "Unlimited items enabled - placed blocks and eaten items are kept."
                        : "Unlimited items disabled.");
            }
            default -> Msg.error(player, "Use /unlimited [toggle|on|off|list|clear].");
        }
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
