package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Repairs the held item, or the whole inventory with {@code /repair all}. */
public class Repair_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        boolean all = args.length >= 1
                && (args[0].equalsIgnoreCase("all") || args[0].equalsIgnoreCase("everything"));

        int repaired = 0;
        if (all) {
            for (ItemStack stack : player.getInventory().getContents()) {
                if (repair(stack)) {
                    repaired++;
                }
            }
            for (ItemStack stack : player.getInventory().getArmorContents()) {
                if (repair(stack)) {
                    repaired++;
                }
            }
            if (repair(player.getInventory().getItemInOffHand())) {
                repaired++;
            }
            Msg.success(player, repaired == 0 ? "Nothing needed repairing."
                    : "Repaired " + repaired + " item(s).");
            return true;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR) {
            Msg.error(player, "You have to hold an item to repair it.");
            return true;
        }
        if (!repair(held)) {
            Msg.error(player, "The item in your hand is not damaged.");
            return true;
        }
        Msg.success(player, "Your held item was repaired.");
        return true;
    }

    private boolean repair(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return false;
        }
        if (damageable.getDamage() <= 0) {
            return false;
        }
        damageable.setDamage(0);
        stack.setItemMeta(meta);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], "hand", "all");
        }
        return Collections.emptyList();
    }
}
