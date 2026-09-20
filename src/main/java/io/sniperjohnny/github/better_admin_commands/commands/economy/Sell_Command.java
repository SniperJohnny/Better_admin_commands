package io.sniperjohnny.github.better_admin_commands.commands.economy;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Sells items for the prices defined in config.yml. */
public class Sell_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Sell_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        String mode = args.length >= 1 ? args[0].toLowerCase(Locale.ROOT) : "hand";

        double total = 0;
        int sold = 0;

        if (mode.equals("hand")) {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType().isAir()) {
                Msg.error(player, "You have to hold an item.");
                return true;
            }
            double price = plugin.worth().price(held.getType());
            if (price <= 0) {
                Msg.error(player, plugin.worth().nameOf(held.getType()) + " cannot be sold.");
                return true;
            }
            sold = held.getAmount();
            total = price * sold;
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else if (mode.equals("all")) {
            ItemStack[] contents = player.getInventory().getStorageContents();
            for (int index = 0; index < contents.length; index++) {
                ItemStack stack = contents[index];
                if (stack == null || stack.getType().isAir()) {
                    continue;
                }
                double price = plugin.worth().price(stack.getType());
                if (price <= 0) {
                    continue;
                }
                total += price * stack.getAmount();
                sold += stack.getAmount();
                contents[index] = new ItemStack(Material.AIR);
            }
            player.getInventory().setStorageContents(contents);
        } else {
            Integer amount = Targets.parseInt(mode);
            if (amount == null || amount < 1) {
                Msg.error(player, "Use /sell <hand|all|amount>.");
                return true;
            }
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType().isAir()) {
                Msg.error(player, "You have to hold an item.");
                return true;
            }
            double price = plugin.worth().price(held.getType());
            if (price <= 0) {
                Msg.error(player, plugin.worth().nameOf(held.getType()) + " cannot be sold.");
                return true;
            }
            int toSell = Math.min(amount, held.getAmount());
            sold = toSell;
            total = price * toSell;
            held.setAmount(held.getAmount() - toSell);
        }

        if (sold == 0 || total <= 0) {
            Msg.error(player, "You have nothing that can be sold.");
            return true;
        }
        plugin.economy().deposit(player.getUniqueId(), total);
        plugin.economy().saveAsync();
        Msg.success(player, "Sold " + sold + " item(s) for " + plugin.worth().format(total)
                + ". New balance: " + plugin.worth().format(plugin.economy().getBalance(player.getUniqueId())) + ".");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0].toLowerCase(Locale.ROOT), "hand", "all", "1", "64");
        }
        return Collections.emptyList();
    }
}
