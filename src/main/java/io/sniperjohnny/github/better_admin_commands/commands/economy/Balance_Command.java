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
            if (!sender.hasPermission("betteradmincommands.balance.others")) {
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
        Msg.send(player, "&7Your balance: &a" + plugin.economy().format(plugin.economy().getBalance(player.getUniqueId())));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1 && sender.hasPermission("betteradmincommands.balance.others")) {
            return Targets.complete(args[0]);
        }
        return Collections.emptyList();
    }
}
