package io.sniperjohnny.github.better_admin_commands.commands.items;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
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

/** Merges identical items in the inventory into full stacks. */
public class Stack_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        ItemStack[] contents = player.getInventory().getStorageContents();
        List<ItemStack> merged = new ArrayList<>();

        for (ItemStack stack : contents) {
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            int remaining = stack.getAmount();
            for (ItemStack existing : merged) {
                if (remaining <= 0) {
                    break;
                }
                if (!existing.isSimilar(stack)) {
                    continue;
                }
                int space = existing.getMaxStackSize() - existing.getAmount();
                if (space <= 0) {
                    continue;
                }
                int move = Math.min(space, remaining);
                existing.setAmount(existing.getAmount() + move);
                remaining -= move;
            }
            if (remaining > 0) {
                ItemStack copy = stack.clone();
                copy.setAmount(remaining);
                merged.add(copy);
            }
        }

        ItemStack[] result = new ItemStack[contents.length];
        for (int index = 0; index < result.length; index++) {
            if (index < merged.size()) {
                result[index] = merged.get(index);
            } else {
                result[index] = new ItemStack(Material.AIR);
            }
        }
        // Anything that no longer fits is dropped so nothing is lost.
        for (int index = contents.length; index < merged.size(); index++) {
            player.getWorld().dropItemNaturally(player.getLocation(), merged.get(index));
        }
        player.getInventory().setStorageContents(result);
        Msg.success(player, "Stacks merged.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
