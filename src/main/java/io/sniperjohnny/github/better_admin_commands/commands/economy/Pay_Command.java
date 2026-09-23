package io.sniperjohnny.github.better_admin_commands.commands.economy;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.economy.EconomyService;
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

/**
 * Sends money from the sender to another player:
 *
 * <ul>
 *   <li>{@code /pay <player> <amount>} - send straight away</li>
 *   <li>{@code /pay <player>} - ask for the amount in chat</li>
 * </ul>
 */
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
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }
        OfflinePlayer target = Targets.offline(sender, args[0]);
        if (target == null) {
            return true;
        }

        if (args.length < 2) {
            // No amount yet: ask for it in a dialog, so the command works with just a name.
            plugin.dialogs().number(self, "Send money",
                    "&7How much do you want to send to &f"
                            + target.getName() + "&7? &8(you have &f"
                            + plugin.economy().format(plugin.economy().getBalance(self.getUniqueId())) + "&8)",
                    "Amount", "", 16, answer -> {
                        if (answer.equalsIgnoreCase("cancel")) {
                            Msg.send(self, "&7Payment cancelled.");
                            return;
                        }
                        Double amount = Targets.parseDouble(answer);
                        if (amount == null) {
                            Msg.error(self, "That is not a valid amount.");
                            return;
                        }
                        pay(self, target, amount);
                    });
            return true;
        }

        Double amount = Targets.parseDouble(args[1]);
        if (amount == null) {
            Msg.error(self, "That is not a valid amount.");
            return true;
        }
        pay(self, target, amount);
        return true;
    }

    /** Moves the money and reports the outcome to both sides. */
    private void pay(Player self, OfflinePlayer target, double amount) {
        // The rules live in the economy service, so /pay and the balance menu
        // behave identically.
        EconomyService.TransferResult result = plugin.economy().transfer(self, target.getUniqueId(), amount);
        switch (result) {
            case SUCCESS -> {
                Msg.success(self, "You paid " + plugin.economy().format(amount) + " to " + target.getName() + ".");
                if (target.isOnline() && target.getPlayer() != null) {
                    Msg.send(target.getPlayer(), "&7You received &a" + plugin.economy().format(amount)
                            + " &7from &f" + self.getName() + "&7.");
                }
            }
            case PAYMENTS_DISABLED -> Msg.error(self, "Payments are disabled on this server.");
            case SELF -> Msg.error(self, "You cannot pay yourself.");
            case TOO_SMALL -> Msg.error(self, "The amount has to be at least "
                    + plugin.economy().format(plugin.economy().minimumPayment()) + ".");
            case TOO_POOR -> Msg.error(self, "You do not have enough money. Your balance is "
                    + plugin.economy().format(plugin.economy().getBalance(self.getUniqueId())) + ".");
        }
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
