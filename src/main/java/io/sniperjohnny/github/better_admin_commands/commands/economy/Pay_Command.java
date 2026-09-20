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

/** Sends money from the sender to another player. */
public class Pay_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Pay_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player self)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (!plugin.getConfig().getBoolean("economy.allow-payments", true)) {
            Msg.error(self, "Payments are disabled on this server.");
            return true;
        }
        if (args.length < 2) {
            Msg.usage(sender, command);
            return true;
        }
        OfflinePlayer target = Targets.offline(sender, args[0]);
        if (target == null) {
            return true;
        }
        if (target.getUniqueId().equals(self.getUniqueId())) {
            Msg.error(self, "You cannot pay yourself.");
            return true;
        }
        Double amount = Targets.parseDouble(args[1]);
        double minimum = plugin.getConfig().getDouble("economy.minimum-payment", 0.01);
        if (amount == null || amount < minimum) {
            Msg.error(self, "The amount has to be at least " + plugin.economy().format(minimum) + ".");
            return true;
        }

        if (!plugin.economy().withdraw(self.getUniqueId(), amount)) {
            Msg.error(self, "You do not have enough money. Your balance is "
                    + plugin.economy().format(plugin.economy().getBalance(self.getUniqueId())) + ".");
            return true;
        }
        plugin.economy().deposit(target.getUniqueId(), amount);
        plugin.economy().saveAsync();

        Msg.success(self, "You paid " + plugin.economy().format(amount) + " to " + target.getName() + ".");
        if (target.isOnline() && target.getPlayer() != null) {
            Msg.send(target.getPlayer(), "&7You received &a" + plugin.economy().format(amount)
                    + " &7from &f" + self.getName() + "&7.");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.complete(args[0]);
        }
        if (args.length == 2) {
            return Targets.completeFrom(args[1], "10", "100", "1000");
        }
        return Collections.emptyList();
    }
}
