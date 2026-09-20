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
import java.util.Comparator;
import java.util.List;

/** Sorts the inventory by material name. */
public class Sort_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.getType() != Material.AIR) {
                items.add(stack);
            }
        }
        items.sort(Comparator.comparing(stack -> stack.getType().name()));

        ItemStack[] contents = player.getInventory().getStorageContents();
        ItemStack[] result = new ItemStack[contents.length];
        for (int index = 0; index < result.length; index++) {
            result[index] = index < items.size() ? items.get(index) : new ItemStack(Material.AIR);
        }
        player.getInventory().setStorageContents(result);
        Msg.success(player, "Inventory sorted (" + items.size() + " stack(s)).");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
