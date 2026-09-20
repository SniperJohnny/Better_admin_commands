package io.sniperjohnny.github.better_admin_commands.commands.admin;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Gives an item to a player. */
public class Give_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 2) {
            Msg.usage(sender, command);
            return true;
        }
        Player target = Targets.online(sender, args[0]);
        if (target == null) {
            return true;
        }
        Material material = Material.matchMaterial(args[1].toUpperCase(Locale.ROOT));
        if (material == null || material.isAir()) {
            Msg.error(sender, "There is no item called " + args[1] + ".");
            return true;
        }
        int amount = 1;
        if (args.length >= 3) {
            Integer parsed = Targets.parseInt(args[2]);
            if (parsed == null || parsed < 1) {
                Msg.error(sender, "The amount has to be a positive number.");
                return true;
            }
            amount = Math.min(parsed, material.getMaxStackSize() * 36);
        }

        int remaining = amount;
        int maxStack = material.getMaxStackSize();
        while (remaining > 0) {
            int stackSize = Math.min(remaining, maxStack);
            ItemStack stack = new ItemStack(material, stackSize);
            target.getInventory().addItem(stack).forEach((index, leftover) ->
                    target.getWorld().dropItemNaturally(target.getLocation(), leftover));
            remaining -= stackSize;
        }

        Msg.success(sender, "Gave " + amount + "x " + material.name().toLowerCase(Locale.ROOT)
                + " to " + target.getName() + ".");
        if (!target.equals(sender)) {
            Msg.send(target, "&7You received &f" + amount + "x "
                    + material.name().toLowerCase(Locale.ROOT) + "&7.");
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
            List<String> names = new ArrayList<>();
            for (Material material : Material.values()) {
                if (!material.isAir() && material.isItem()) {
                    names.add(material.name().toLowerCase(Locale.ROOT));
                }
            }
            return Targets.completeFrom(args[1].toLowerCase(Locale.ROOT), names);
        }
        if (args.length == 3) {
            return Targets.completeFrom(args[2], "1", "8", "16", "32", "64");
        }
        return Collections.emptyList();
    }
}
