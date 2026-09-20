package io.sniperjohnny.github.better_admin_commands.commands.economy;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Administrative economy command: /eco &lt;give|take|set|reset|balance&gt; &lt;player&gt; [amount]. */
public class Eco_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Eco_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 2) {
            Msg.usage(sender, command);
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        OfflinePlayer target = Targets.offline(sender, args[1]);
        if (target == null) {
            return true;
        }
        var service = plugin.economy();

        switch (action) {
            case "balance", "bal" -> Msg.send(sender, "&7Balance of &f" + target.getName() + "&7: &a"
                    + plugin.economy().format(service.getBalance(target.getUniqueId())));

            case "reset" -> {
                service.setBalance(target.getUniqueId(), plugin.getConfig()
                        .getDouble("economy.starting-balance", 100.0));
                service.saveAsync();
                Msg.success(sender, "Reset the balance of " + target.getName() + " to "
                        + plugin.economy().format(service.getBalance(target.getUniqueId())) + ".");
            }

            case "give", "add" -> {
                Double amount = amount(sender, args);
                if (amount == null) {
                    return true;
                }
                service.deposit(target.getUniqueId(), amount);
                service.saveAsync();
                Msg.success(sender, "Gave " + plugin.economy().format(amount) + " to " + target.getName()
                        + ". New balance: " + plugin.economy().format(service.getBalance(target.getUniqueId())) + ".");
            }

            case "take", "remove" -> {
                Double amount = amount(sender, args);
                if (amount == null) {
                    return true;
                }
                service.withdraw(target.getUniqueId(), amount);
                service.saveAsync();
                Msg.success(sender, "Took " + plugin.economy().format(amount) + " from " + target.getName()
                        + ". New balance: " + plugin.economy().format(service.getBalance(target.getUniqueId())) + ".");
            }

            case "set" -> {
                Double amount = amount(sender, args);
                if (amount == null) {
                    return true;
                }
                service.setBalance(target.getUniqueId(), amount);
                service.saveAsync();
                Msg.success(sender, "Set the balance of " + target.getName() + " to "
                        + plugin.economy().format(service.getBalance(target.getUniqueId())) + ".");
            }

            default -> Msg.usage(sender, command);
        }
        return true;
    }

    private Double amount(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Msg.error(sender, "You have to give an amount.");
            return null;
        }
        Double amount = Targets.parseDouble(args[2]);
        if (amount == null || amount < 0) {
            Msg.error(sender, "The amount has to be a positive number.");
            return null;
        }
        return amount;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], "give", "take", "set", "reset", "balance");
        }
        if (args.length == 2) {
            return Targets.complete(args[1]);
        }
        if (args.length == 3) {
            return Targets.completeFrom(args[2], "100", "1000", "10000");
        }
        return Collections.emptyList();
    }
}
