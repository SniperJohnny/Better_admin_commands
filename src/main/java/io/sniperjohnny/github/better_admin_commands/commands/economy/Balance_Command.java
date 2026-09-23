package io.sniperjohnny.github.better_admin_commands.commands.economy;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Shows the balance of the sender or of another player. */
public class Balance_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Balance_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length >= 1) {
            // /money and /balance double as the economy admin command, so
            // /money set <player> <amount> works like it does on XConomy. A first
            // argument that is not an action is treated as a player name below.
            if (plugin.ecoCommand() != null
                    && plugin.ecoCommand().admin(sender, command, args)) {
                return true;
            }
            if (!plugin.permissions().has(sender, "betteradmincommands.balance.others")) {
                Msg.noPermission(sender);
                return true;
            }
            OfflinePlayer target = Targets.offline(sender, args[0]);
            if (target == null) {
                return true;
            }
            Msg.send(sender, "&7Balance of &f" + target.getName() + "&7: &a"
                    + plugin.economy().format(plugin.economy().getBalance(target.getUniqueId())));
            return true;
        }

        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        // The amount still goes to chat - a single number is what /balance is for -
        // and the menu behind it adds sending money and the leaderboard.
        Msg.send(player, "&7Your balance: &a" + plugin.economy().format(plugin.economy().getBalance(player.getUniqueId())));
        plugin.balanceGui().open(player);
        return true;
    }

    /** True when the first argument names an economy action (for tab completion). */
    private static boolean isAction(String value) {
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "give", "add", "take", "remove", "reduce", "set", "reset", "resetall",
                 "balance", "bal", "top", "help" -> true;
            default -> false;
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            List<String> choices = new java.util.ArrayList<>(Targets.complete(args[0]));
            if (plugin.permissions().has(sender, "betteradmincommands.eco")) {
                choices.addAll(Targets.completeFrom(args[0], "give", "take", "set", "reset", "resetall",
                        "balance", "top", "help"));
            }
            return choices;
        }
        if (isAction(args[0]) && args.length == 2) {
            return Targets.complete(args[1]);
        }
        if (isAction(args[0]) && args.length == 3) {
            return Targets.completeFrom(args[2], "100", "1000", "10000");
        }
        return Collections.emptyList();
    }
}
